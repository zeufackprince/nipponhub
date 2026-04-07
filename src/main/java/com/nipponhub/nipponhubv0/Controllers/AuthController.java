package com.nipponhub.nipponhubv0.Controllers;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.nipponhub.nipponhubv0.DTO.ReqRes;
import com.nipponhub.nipponhubv0.Models.Enum.UserRole;
import com.nipponhub.nipponhubv0.Services.UsersManagementService;


/**
 * Public authentication endpoints — no token required.
 *
 *  KEY RULE: POST /auth/register always creates a CLIENT account.
 *  The `role` field in the request body is intentionally ignored here.
 *  Only an ADMIN can create accounts with elevated roles via
 *  POST /api/admin/create-user.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UsersManagementService usersManagementService;

    /**
     * POST /auth/register
     * Public registration — always results in a CLIENT account.
     * Consumes: multipart/form-data
     * @throws IOException 
     */
    @PostMapping("/register")
    public ResponseEntity<ReqRes> register(
            @RequestParam("name")      String name,
            @RequestParam("email")     String email,
            @RequestParam("password")  String password,
            @RequestParam(value = "telephone",  required = false) String telephone,
            @RequestParam(value = "profileImg", required = false) MultipartFile profileImg
            // NOTE: `role` param is intentionally NOT accepted here.
    ) throws IOException {
        ReqRes req = new ReqRes();
        req.setName(name);
        req.setEmail(email);
        req.setPassword(password);
        req.setTelephone(telephone);
        req.setRole(UserRole.CLIENT); // always CLIENT — cannot be overridden

        ReqRes result = usersManagementService.register(req, profileImg);
        int status = result.getStatusCode() != 0 ? result.getStatusCode() : 201;
        return ResponseEntity.status(status).body(result);
    }

    /**
     * POST /auth/login
     * Returns JWT access token + refresh token.
     * Consumes: application/json  { "email": "...", "password": "..." }
     */
    @PostMapping("/login")
    public ResponseEntity<ReqRes> login(@RequestBody ReqRes loginRequest) {
        return ResponseEntity.ok(usersManagementService.login(loginRequest));
    }

    /**
     * POST /auth/refresh
     * Exchange a still-valid refresh token for a new access token.
     * Consumes: application/json  { "token": "<refresh_token>" }
     */
    @PostMapping("/refresh")
    public ResponseEntity<ReqRes> refresh(@RequestBody ReqRes refreshRequest) {
        return ResponseEntity.ok(usersManagementService.refreshToken(refreshRequest));
    }
}

