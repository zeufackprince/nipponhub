package com.nipponhub.nipponhubv0.Controllers;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.nipponhub.nipponhubv0.DTO.RestockDto;
import com.nipponhub.nipponhubv0.Models.Restocks;
import com.nipponhub.nipponhubv0.Services.RestockService;

import lombok.AllArgsConstructor;


/**
 * Restock (purchase from supplier) — ADMIN and OWNER only.
 * Increases product stock and writes a ProductActivity audit entry.
 */
@RestController
@RequestMapping("/api/v0/Restock")
@AllArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
public class RestockController {

    private final RestockService RestockService;

        /** POST /api/v0/Restock/new-Restock */
    @PostMapping("/new-Restock")
    public ResponseEntity<RestockDto> newRestock(@RequestBody Restocks request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String role = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .iterator().next().getAuthority().replace("ROLE_", "");
        return ResponseEntity.status(201)
                .body(RestockService.createRestock(request, username, role));
    }

    /**
     * GET /api/v0/Restock/get-all-Restock
     * Récupère la liste de tous les Restocks avec les détails structurés.
     */
    @GetMapping("/get-all-Restock")
    public ResponseEntity<List<RestockDto>> getAllRestock() {
        List<RestockDto> res = this.RestockService.getAllRestocks();
        return ResponseEntity.ok(res);
    }
}