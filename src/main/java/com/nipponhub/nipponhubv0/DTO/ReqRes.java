package com.nipponhub.nipponhubv0.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nipponhub.nipponhubv0.Models.Enum.UserRole;

import jakarta.validation.constraints.*;

import com.nipponhub.nipponhubv0.Models.OurUsers;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Dual-purpose DTO used for both inbound requests and outbound responses.
 *
 * When used as a REQUEST:
 *   - Fields like name, email, password, role, telephone are populated by the caller.
 *   - statusCode / error / message / token fields are ignored.
 *
 * When used as a RESPONSE:
 *   - statusCode reflects the operation result (200, 404, 500 …).
 *   - password is NEVER populated in responses (excluded by UserMapper).
 *   - poster     = raw GridFS ObjectId (stored in MySQL as users.images).
 *   - posterUrl  = full URL to load the image via GET /file/{gridFsId}.
 *
 * @JsonInclude(NON_NULL) ensures null fields are omitted from JSON output,
 * keeping responses clean regardless of which use-case is active.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqRes {

    // ── Response metadata ────────────────────────────────────────────────────────
    private int    statusCode;
    private String error;
    private String message;

    // ── Auth tokens (login / refresh responses) ──────────────────────────────────
    private String token;
    private String refreshToken;
    private String expirationTime;

    // ── User fields ──────────────────────────────────────────────────────────────
    private Long     userId;
   @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 12, message = "Password must be at least 12 characters")
    private String password;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
    private String telephone;
    private UserRole role;

    // ── Profile image ─────────────────────────────────────────────────────────────
    private String images;          // kept for legacy compatibility
    private String poster;          // raw GridFS ObjectId (same as users.images in MySQL)
    private String posterUrl;       // full accessible URL: baseUrl + "/file/" + gridFsId

    // ── Embedded objects (admin / internal use) ───────────────────────────────────
    private OurUsers       ourUsers;
    private List<OurUsers> ourUsersList;

    // ─── Constructors ─────────────────────────────────────────────────────────────

    /**
     * Request constructor — used by the controller to package @RequestParam values
     * into a single object before passing them to the service.
     */
    public ReqRes(String name, String email, String telephone, String password, UserRole role) {
        this.name      = name;
        this.email     = email;
        this.telephone = telephone;
        this.password  = password;
        this.role      = role;
    }
}
