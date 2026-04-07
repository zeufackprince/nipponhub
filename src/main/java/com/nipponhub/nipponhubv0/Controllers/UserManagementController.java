package com.nipponhub.nipponhubv0.Controllers;

import com.nipponhub.nipponhubv0.DTO.ReqRes;
import com.nipponhub.nipponhubv0.Models.OurUsers;
import com.nipponhub.nipponhubv0.Models.Enum.UserRole;
import com.nipponhub.nipponhubv0.Repositories.mysql.UserRepository;
import com.nipponhub.nipponhubv0.Services.UsersManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * User Management Controller — updated to match use-case rules:
 *
 *  ┌─────────────────────────────────────────────────────────────────┐
 *  │  Action                       │ Who can do it                  │
 *  ├─────────────────────────────────────────────────────────────────┤
 *  │  Create users with any role   │ ADMIN only                     │
 *  │  (ADMIN, OWNER, PARTNER…)     │                                │
 *  │  View all users               │ ADMIN, OWNER                   │
 *  │  Filter users by role         │ ADMIN, OWNER                   │
 *  │  Get user by ID               │ ADMIN, OWNER                   │
 *  │  Delete user                  │ ADMIN only                     │
 *  │  View own profile             │ Any authenticated user         │
 *  │  Update own profile           │ Any authenticated user         │
 *  └─────────────────────────────────────────────────────────────────┘
 *
 *  Note: public registration (POST /auth/register) always assigns the
 *  CLIENT role regardless of what the caller sends — enforced in
 *  AuthController/Service, not here.
 */
@RestController
@RequiredArgsConstructor 
public class UserManagementController {

    private final UsersManagementService usersManagementService;

    /**
     * Used only to resolve the authenticated user's id from their email.
     * This avoids exposing the id in the JWT or request body.
     */
    private final UserRepository userRepository;

    // ─── PUBLIC AUTH ─────────────────────────────────────────────────────────────

    /**
     * POST /auth/register
     *
     * Register a new user account. All fields are passed as @RequestParam
     * so the request can be sent as multipart/form-data (required for the file).
     *
     * @param file optional profile picture (must be an image/* MIME type)
     */
    @PostMapping("/api/admin/create-user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReqRes> createPrivilegedUser(
        @RequestParam(required = false) String     name,
        @RequestParam(required = false) String     email,
        @RequestParam(required = false) String     password,
        @RequestParam(required = false) String     telephone,
        @RequestParam(required = false) UserRole   role,
        @RequestParam(required = false) MultipartFile profileImg
    ) throws IOException {
        ReqRes reg = new ReqRes(name, email, telephone, password, role);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(usersManagementService.register(reg, profileImg));
    }


    // ══════════════════════════════════════════════════════════════════════
    //  ADMIN / OWNER: read user data
    // ══════════════════════════════════════════════════════════════════════

    /**
     * GET /api/admin/get-all-users
     *
     * Returns every user in the system. Restricted to ADMINs.
     */
    @GetMapping("/api/admin/get-all-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReqRes>> getAllUsers() {
        return ResponseEntity.ok(usersManagementService.getAllUsers());
    }

    /**
     * GET /api/admin/users/{role}
     *
     * Returns all users with a specific role (e.g. ADMIN, USER, PARTNER …).
     * The role value must match a UserRole enum constant exactly.
     *
     * @param role one of: ADMIN, USER, CUSTOMER, PARTNER, GUEST
     */
    @GetMapping("/api/admin/users/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<ReqRes>> getUsersByRole(@PathVariable UserRole role) {
        return usersManagementService.getUsersByRole(role);
    }

    /**
     * DELETE /api/admin/delete/{userId}
     *
     * Permanently delete a user and their profile picture. Restricted to ADMINs.
     *
     * @param userId the target user's MySQL id
     */
    @DeleteMapping("/api/admin/delete/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReqRes> deleteUser(@PathVariable Long userId) {
        return ResponseEntity.ok(usersManagementService.deleteUser(userId));
    }

    // ─── USER ENDPOINTS ──────────────────────────────────────────────────────────

    /**
     * GET /api/user/get-users/{userId}
     *
     * Fetch a specific user by id. Restricted to ADMINs.
     * (Kept under /api/user/ to match the original URL structure.)
     *
     * @param userId the target user's MySQL id
     */
    @GetMapping("/api/user/get-users/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ReqRes> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(usersManagementService.getUsersById(userId));
    }

    /**
     * PUT /api/admin/users/{userId}/role
     * ADMIN-only: change the role of any user.
     */
    @PutMapping("/api/admin/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReqRes> changeUserRole(
            @PathVariable Long userId,
            @RequestParam String role) {
        return ResponseEntity.ok(usersManagementService.changeRole(userId, role));
    }    


    /**
     * PUT /api/user/update
     *
     * Update the currently authenticated user's own profile.
     * Only the fields provided (non-null/non-empty) will be changed.
     *
     * The user's id is resolved server-side from the JWT — they cannot
     * supply an arbitrary userId in the request.
     *
     * @param file optional new profile picture (replaces the old one in GridFS)
     */
    @PutMapping("/api/user/update")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReqRes> updateUser(
        @RequestParam(required = false) String     name,
        @RequestParam(required = false) String     email,
        @RequestParam(required = false) String     password,
        @RequestParam(required = false) String     telephone,
        @RequestParam(required = false) MultipartFile file
    ) throws IOException {

        // ── Resolve the caller's identity from the security context ────────────
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        // Look up the user by email (JWT subject) to get their MySQL id and role
        OurUsers userdb = userRepository
            .findByEmail(auth.getName())
            .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
        
        Long userId = userdb.getUserid();

        ReqRes reqres = new ReqRes(name, email, telephone, password, userdb.getRole());
        return ResponseEntity.ok(usersManagementService.updateUser(userId, reqres, file));
    }

    /**
     * GET /api/adminuser/get-profile
     *
     * Return the profile of the currently authenticated user.
     * Accessible by any authenticated user (both ADMIN and USER roles).
     */
    @GetMapping("/api/adminuser/get-profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReqRes> getMyProfile() {
        Authentication auth  = SecurityContextHolder.getContext().getAuthentication();
        ReqRes response      = usersManagementService.getMyInfo(auth.getName());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
