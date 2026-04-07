package com.nipponhub.nipponhubv0.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.nipponhub.nipponhubv0.DTO.CommandeDto;
import com.nipponhub.nipponhubv0.Models.Enum.CommandeStatus;
import com.nipponhub.nipponhubv0.Services.CommandeService;

import java.util.List;

@RestController
@RequestMapping("/api/v0/commande")
public class CommandeController {

    @Autowired
    private CommandeService commandeService;

    // ══════════════════════════════════════════════════════════════════════
    //  CLIENT endpoints — any authenticated user
    // ══════════════════════════════════════════════════════════════════════

    /**
     * POST /api/v0/commande/new
     * Place a new order.  No stock change — just records intent.
     *
     * Body: { "items": [{ "productId": 1, "quantite": 2, "prixVendu": 20.00 }], "note": "..." }
     */
    @PostMapping("/new")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommandeDto> placeCommande(
            @RequestBody CommandeDto request) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.status(201)
                .body(commandeService.placeCommande(request, username));
    }

    /**
     * GET /api/v0/commande/my-orders
     * Client sees their own order history.
     */
    @GetMapping("/my-orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CommandeDto>> getMyOrders() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(commandeService.getMyOrders(username));
    }

    /**
     * GET /api/v0/commande/my-orders/{id}
     * Client fetches one of their own orders.  Returns 403 if not theirs.
     */
    @GetMapping("/my-orders/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommandeDto> getMyOrder(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(commandeService.getMyOrderById(id, username));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ADMIN / OWNER endpoints
    // ══════════════════════════════════════════════════════════════════════

    /**
     * GET /api/v0/commande/all
     * All orders with full client info.
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<CommandeDto>> getAllCommandes() {
        return ResponseEntity.ok(commandeService.getAllCommandes());
    }

    /**
     * GET /api/v0/commande/status/{status}
     * Filter by status (PENDING, CONFIRMED, DELIVERED, CANCELLED).
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<CommandeDto>> getByStatus(@PathVariable CommandeStatus status) {
        return ResponseEntity.ok(commandeService.getCommandesByStatus(status));
    }

    /**
     * GET /api/v0/commande/{id}
     * Admin fetches any specific order (includes client details).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<CommandeDto> getCommande(@PathVariable Long id) {
        return ResponseEntity.ok(commandeService.getCommandeById(id));
    }

    /**
     * PUT /api/v0/commande/{id}/status?status=CONFIRMED
     * Generic status update.  Use DELIVERED to trigger stock decrement + Vente creation.
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<CommandeDto> updateStatus(
            @PathVariable Long id,
            @RequestParam CommandeStatus status) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(commandeService.updateStatus(id, status, username));
    }

    /**
     * PUT /api/v0/commande/{id}/deliver
     * Shortcut: mark an order as DELIVERED.
     * → Creates the Vente in DB, decrements stock, logs activity.
     */
    @PutMapping("/{id}/deliver")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<CommandeDto> deliver(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(
                commandeService.updateStatus(id, CommandeStatus.DELIVERED, username));
    }

    /**
     * PUT /api/v0/commande/{id}/confirm
     * Move to CONFIRMED (order acknowledged, not yet shipped).
     */
    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<CommandeDto> confirm(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(
                commandeService.updateStatus(id, CommandeStatus.CONFIRMED, username));
    }

    /**
     * PUT /api/v0/commande/{id}/cancel
     * Cancel an order (no stock impact).
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<CommandeDto> cancel(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(
                commandeService.updateStatus(id, CommandeStatus.CANCELLED, username));
    }
}

