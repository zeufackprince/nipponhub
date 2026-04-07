package com.nipponhub.nipponhubv0.Controllers;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.nipponhub.nipponhubv0.DTO.AchatDto;
import com.nipponhub.nipponhubv0.Models.Achats;
import com.nipponhub.nipponhubv0.Services.AchatService;

import lombok.AllArgsConstructor;


/**
 * Achat (purchase from supplier) — ADMIN and OWNER only.
 * Increases product stock and writes a ProductActivity audit entry.
 */
@RestController
@RequestMapping("/api/v0/achat")
@AllArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
public class AchatController {

    private final AchatService achatService;

        /** POST /api/v0/achat/new-achat */
    @PostMapping("/new-achat")
    public ResponseEntity<AchatDto> newAchat(@RequestBody Achats request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String role = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .iterator().next().getAuthority().replace("ROLE_", "");
        return ResponseEntity.status(201)
                .body(achatService.createAchat(request, username, role));
    }

    /**
     * GET /api/v0/achat/get-all-achat
     * Récupère la liste de tous les achats avec les détails structurés.
     */
    @GetMapping("/get-all-achat")
    public ResponseEntity<List<AchatDto>> getAllAchat() {
        List<AchatDto> res = this.achatService.getAllAchats();
        return ResponseEntity.ok(res);
    }
}