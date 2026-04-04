package com.nipponhub.nipponhubv0.Mappers;

import com.nipponhub.nipponhubv0.DTO.ReqRes;
import com.nipponhub.nipponhubv0.Models.OurUsers;

/**
 * Utility class for mapping between OurUsers (entity) and ReqRes (DTO).
 *
 * Centralising mapping here removes the repeated 8-line field-assignment
 * blocks that previously appeared in every service method.
 *
 * Usage:
 *   ReqRes resp = UserMapper.toResponse(user, baseUrl);
 *   resp.setMessage("...");
 */
public class UserMapper {

    // ── Prevent instantiation — all methods are static ──────────────────────────
    private UserMapper() {}

    // ─── ENTITY → DTO ────────────────────────────────────────────────────────────

    /**
     * Convert an OurUsers entity into a safe ReqRes response DTO.
     *
     * Fields intentionally excluded from the response:
     *   • password  — never sent over the wire
     *   • ourUsers / ourUsersList — internal use only
     *
     * Image fields:
     *   • poster    — the raw GridFS ObjectId stored in MySQL (users.images)
     *   • posterUrl — the full URL the client can hit to load the image
     *
     * @param user    the user entity fetched from MySQL
     * @param baseUrl the application base URL, e.g. "http://localhost:8080"
     * @return a populated ReqRes ready to be returned in a ResponseEntity
     */
    public static ReqRes toResponse(OurUsers user, String baseUrl) {
        ReqRes resp = new ReqRes();

        resp.setStatusCode(200);
        resp.setUserId(user.getUserid());
        resp.setName(user.getName());
        resp.setEmail(user.getEmail());
        resp.setTelephone(user.getTelephone());
        resp.setRole(user.getRole());

        // poster = raw GridFS ObjectId (kept so the client can reference it directly)
        resp.setPoster(user.getImages());

        // posterUrl = the full /file/{gridFsId} URL streamed by FileController
        if (user.getImages() != null) {
            resp.setPosterUrl(baseUrl + "/file/" + user.getImages());
        }

        return resp;
    }
}
