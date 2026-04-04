package com.nipponhub.nipponhubv0.Services;

import com.nipponhub.nipponhubv0.DTO.ReqRes;
import com.nipponhub.nipponhubv0.Mappers.UserMapper;
import com.nipponhub.nipponhubv0.Models.Enum.UserRole;
import com.nipponhub.nipponhubv0.Models.OurUsers;
import com.nipponhub.nipponhubv0.Repositories.mysql.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service handling all user lifecycle operations:
 * registration, authentication, profile management, and deletion.
 *
 * Profile images are stored in MongoDB GridFS via FileStorageService.
 * The returned GridFS ObjectId is persisted in MySQL (users.images column).
 * This means MySQL only holds a reference (the id), not the binary data.
 */
@Slf4j
@Service
@RequiredArgsConstructor  // generates a constructor for all final fields (no @Autowired needed)
public class UsersManagementService {

    private final UserRepository        usersRepo;
    private final JWTUtils              jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder       passwordEncoder;
    private final FileStorageService    fileStorageService; // ← GridFS image operations

    /** Injected from application.properties: app.base-url=http://localhost:8080 */
    @Value("${app.base-url}")
    private String baseUrl;

    // ─── REGISTER ────────────────────────────────────────────────────────────────

    /**
     * Register a new user account with an optional profile picture.
     *
     * Flow:
     *  1. Reject if the email is already taken.
     *  2. Assign default role USER if none was provided.
     *  3. Upload the profile picture to GridFS → get back a hex ObjectId.
     *  4. Save the user to MySQL with the GridFS id in the images column.
     *  5. Return the saved user data plus the full image URL.
     *
     * @param reqres registration payload (name, email, password, telephone, role)
     * @param file   optional profile picture (any image/* MIME type)
     * @return ReqRes with user info on success, error details on failure
     * @throws IOException if the file upload to GridFS fails at the stream level
     */
    public ReqRes register(ReqRes reqres, MultipartFile file) throws IOException {
        ReqRes resp = new ReqRes();

        // ── Guard: reject duplicate emails ────────────────────────────────────
        if (usersRepo.findByEmail(reqres.getEmail()).isPresent()) {
            throw new RuntimeException(
                "A user with email [" + reqres.getEmail() + "] already exists."
            );
        }

        // ── Default role to USER if the caller did not provide one ────────────
        if (reqres.getRole() == null) {
            reqres.setRole(UserRole.USER);
        }

        try {
            // ── Upload profile picture to GridFS (optional) ───────────────────
            // fileStorageService.uploadFile() validates that the file is an image,
            // stores it in GridFS, saves metadata in MongoDB, and returns the ObjectId.
            String gridFsId = null;
            if (file != null && !file.isEmpty()) {
                gridFsId = fileStorageService.uploadFile(file);
                log.info("Profile picture uploaded — gridFsId: {}", gridFsId);
            }

            // ── Build the user entity ─────────────────────────────────────────
            OurUsers newUser = new OurUsers();
            newUser.setEmail(reqres.getEmail());
            newUser.setName(reqres.getName());
            newUser.setTelephone(reqres.getTelephone());
            newUser.setRole(reqres.getRole());
            newUser.setPassword(passwordEncoder.encode(reqres.getPassword())); // never store plain text
            newUser.setImages(gridFsId); // null if no picture was provided

            // ── Persist to MySQL ──────────────────────────────────────────────
            OurUsers savedUser = usersRepo.save(newUser);

            // ── Build the success response using the shared mapper ────────────
            resp = UserMapper.toResponse(savedUser, baseUrl);
            resp.setMessage("User registered successfully");

        } catch (IOException e) {
            // Let IO exceptions propagate — the controller can decide how to respond
            throw e;
        } catch (Exception e) {
            log.error("Registration failed for [{}]: {}", reqres.getEmail(), e.getMessage(), e);
            resp.setStatusCode(500);
            resp.setError(e.getMessage());
        }

        return resp;
    }

    // ─── LOGIN ───────────────────────────────────────────────────────────────────

    /**
     * Authenticate a user with email + password and return JWT tokens.
     *
     * Spring Security's AuthenticationManager handles credential verification.
     * On success, we issue an access token (short-lived) and a refresh token.
     *
     * @param loginRequest must contain `email` and `password`
     * @return ReqRes with tokens and role on success, error message on failure
     */
    public ReqRes login(ReqRes loginRequest) {
        ReqRes response = new ReqRes();
        try {
            // Delegates to Spring Security — throws BadCredentialsException if wrong
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
                )
            );

            OurUsers user = usersRepo.findByEmail(loginRequest.getEmail()).orElseThrow();

            String jwt          = jwtUtils.generateToken(user);
            String refreshToken = jwtUtils.generateRefreshToken(new HashMap<>(), user);

            response.setStatusCode(200);
            response.setToken(jwt);
            response.setRefreshToken(refreshToken);
            response.setRole(user.getRole());
            response.setExpirationTime("24Hrs");
            response.setMessage("Successfully logged in");

        } catch (Exception e) {
            log.warn("Login failed for [{}]: {}", loginRequest.getEmail(), e.getMessage());
            response.setStatusCode(500);
            response.setMessage(e.getMessage());
        }
        return response;
    }

    // ─── REFRESH TOKEN ───────────────────────────────────────────────────────────

    /**
     * Issue a new access token using a still-valid refresh token.
     * The refresh token itself is not replaced — the client keeps it until it expires.
     *
     * @param request must contain `token` (the refresh token string)
     * @return ReqRes with the new access token, or 401 if the token is invalid
     */
    public ReqRes refreshToken(ReqRes request) {
        ReqRes response = new ReqRes();
        try {
            String email = jwtUtils.extractUsername(request.getToken());
            OurUsers user = usersRepo.findByEmail(email).orElseThrow();

            if (jwtUtils.isTokenValid(request.getToken(), user)) {
                String newJwt = jwtUtils.generateToken(user);

                response.setStatusCode(200);
                response.setToken(newJwt);
                response.setRefreshToken(request.getToken()); // same refresh token echoed back
                response.setExpirationTime("24Hrs");
                response.setMessage("Token refreshed successfully");
            } else {
                response.setStatusCode(401);
                response.setMessage("Refresh token is invalid or expired");
            }

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage(), e);
            response.setStatusCode(500);
            response.setMessage(e.getMessage());
        }
        return response;
    }

    // ─── READ ────────────────────────────────────────────────────────────────────

    /**
     * Return all users in the system.
     *
     * BUG FIX: the original code reused a single `resp` object inside the loop,
     * causing every entry in the list to point to the same mutated object.
     * Fixed by using Stream.map() which creates a new ReqRes per user.
     *
     * @return list of ReqRes, one per user; empty list if no users exist
     */
    public List<ReqRes> getAllUsers() {
        return usersRepo.findAll()
            .stream()
            .map(user -> {
                ReqRes r = UserMapper.toResponse(user, baseUrl);
                r.setMessage("User listed successfully");
                return r;
            })
            .collect(Collectors.toList());
    }

    /**
     * Return a single user by their MySQL primary key.
     *
     * @param userId the user's id in the MySQL table
     * @return ReqRes with user data, or a 500 response if not found
     */
    public ReqRes getUsersById(Long userId) {
        ReqRes resp = new ReqRes();
        try {
            OurUsers user = usersRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

            resp = UserMapper.toResponse(user, baseUrl);
            resp.setMessage("User fetched successfully");

        } catch (Exception e) {
            log.error("Failed to fetch user {}: {}", userId, e.getMessage());
            resp.setStatusCode(500);
            resp.setMessage("Error: " + e.getMessage());
        }
        return resp;
    }

    /**
     * Return all users that have a specific role.
     *
     * REFACTOR: the original had a switch statement with three near-identical
     * copy-pasted loops for USER / AGENT / ADMIN. Replaced with a single
     * repository call + stream that works for any role in the enum.
     *
     * @param role the target UserRole (ADMIN, USER, CUSTOMER, PARTNER, GUEST)
     * @return ResponseEntity wrapping the matching users list
     */
    public ResponseEntity<List<ReqRes>> getUsersByRole(UserRole role) {
        List<ReqRes> result = usersRepo.findByRole(role)
            .stream()
            .map(user -> {
                ReqRes r = UserMapper.toResponse(user, baseUrl);
                r.setMessage("Users with role [" + role.name() + "] listed successfully");
                return r;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Return the profile of the currently authenticated user.
     * The email is extracted from the JWT by the controller via SecurityContextHolder.
     *
     * @param email the authenticated user's email (from the JWT subject)
     * @return ReqRes with profile data
     */
    public ReqRes getMyInfo(String email) {
        ReqRes resp = new ReqRes();
        try {
            OurUsers user = usersRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

            resp = UserMapper.toResponse(user, baseUrl);
            resp.setMessage("Profile fetched successfully");

        } catch (Exception e) {
            log.error("Failed to fetch profile for [{}]: {}", email, e.getMessage());
            resp.setStatusCode(500);
            resp.setMessage("Error: " + e.getMessage());
        }
        return resp;
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────────

    /**
     * Partially update a user's profile.
     *
     * Only non-null / non-blank fields from the request are applied —
     * this means the client can update just the phone number without
     * touching the name, email, or role.
     *
     * Image update flow:
     *  1. If a new file is provided, delete the old GridFS file (avoids orphans).
     *  2. Upload the new file to GridFS.
     *  3. Replace users.images with the new ObjectId.
     *
     * REFACTOR vs original:
     *  - Old code used Files.deleteIfExists(Paths.get(...)) — local filesystem.
     *  - New code calls fileStorageService.deleteFile() — GridFS via MongoDB.
     *  - Added null/blank checks on every field (partial update support).
     *
     * @param userId the authenticated user's id (resolved by the controller)
     * @param reqres fields to update; null or blank values are skipped
     * @param file   optional new profile picture
     * @return ReqRes with the fully updated user data
     * @throws IOException if the new file upload to GridFS fails
     */
    public ReqRes updateUser(Long userId, ReqRes reqres, MultipartFile file) throws IOException {
        ReqRes resp = new ReqRes();

        try {
            OurUsers user = usersRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

            // ── Profile picture replacement ────────────────────────────────────
            if (file != null && !file.isEmpty()) {

                // Delete the current picture from GridFS to keep storage clean
                if (StringUtils.hasText(user.getImages())) {
                    fileStorageService.deleteFile(user.getImages());
                    log.info("Old profile picture removed — gridFsId: {}", user.getImages());
                }

                // Upload the new picture and store its ObjectId
                String newGridFsId = fileStorageService.uploadFile(file);
                user.setImages(newGridFsId);
                log.info("New profile picture stored — gridFsId: {}", newGridFsId);
            }

            // ── Partial field updates — skip null/blank values ─────────────────
            if (StringUtils.hasText(reqres.getName()))      user.setName(reqres.getName());
            if (StringUtils.hasText(reqres.getEmail()))     user.setEmail(reqres.getEmail());
            if (StringUtils.hasText(reqres.getTelephone())) user.setTelephone(reqres.getTelephone());
            if (StringUtils.hasText(reqres.getPassword()))  user.setPassword(passwordEncoder.encode(reqres.getPassword()));
            if (reqres.getRole() != null)                   user.setRole(reqres.getRole());

            OurUsers savedUser = usersRepo.save(user);

            resp = UserMapper.toResponse(savedUser, baseUrl);
            resp.setMessage("User updated successfully");

        } catch (IOException e) {
            throw e; // surface GridFS upload errors to the controller
        } catch (Exception e) {
            log.error("Failed to update user {}: {}", userId, e.getMessage(), e);
            resp.setStatusCode(500);
            resp.setError("Update failed: " + e.getMessage());
        }

        return resp;
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────────

    /**
     * Delete a user and clean up their profile picture from GridFS.
     *
     * IMPROVEMENT vs original: the old code only called usersRepo.deleteById(),
     * leaving the image as an orphan in GridFS forever. Now we explicitly delete
     * the GridFS file before removing the user row.
     *
     * @param userId the user's MySQL primary key
     * @return ReqRes confirming deletion or describing the error
     */
    public ReqRes deleteUser(Long userId) {
        ReqRes resp = new ReqRes();
        try {
            OurUsers user = usersRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

            // ── Remove profile picture from GridFS first ───────────────────────
            if (StringUtils.hasText(user.getImages())) {
                fileStorageService.deleteFile(user.getImages());
                log.info("Profile picture deleted — gridFsId: {} (user: {})", user.getImages(), userId);
            }

            // ── Remove user record from MySQL ──────────────────────────────────
            usersRepo.deleteById(userId);

            resp.setStatusCode(200);
            resp.setMessage("User deleted successfully");

        } catch (Exception e) {
            log.error("Failed to delete user {}: {}", userId, e.getMessage(), e);
            resp.setStatusCode(500);
            resp.setMessage("Error: " + e.getMessage());
        }
        return resp;
    }
}
