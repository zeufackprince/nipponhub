package com.nipponhub.nipponhubv0.Services;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nipponhub.nipponhubv0.DTO.CommandeDto;
import com.nipponhub.nipponhubv0.DTO.CommandeItemDto;
import com.nipponhub.nipponhubv0.Models.Commande;
import com.nipponhub.nipponhubv0.Models.CommandeItem;
import com.nipponhub.nipponhubv0.Models.OurUsers;
import com.nipponhub.nipponhubv0.Models.Product;
import com.nipponhub.nipponhubv0.Models.ProductActivity;
import com.nipponhub.nipponhubv0.Models.Sales;
import com.nipponhub.nipponhubv0.Models.Enum.CommandeStatus;
import com.nipponhub.nipponhubv0.Repositories.mysql.CommandeRepository;
import com.nipponhub.nipponhubv0.Repositories.mysql.ProductActivityRepository;
import com.nipponhub.nipponhubv0.Repositories.mysql.ProductRepository;
import com.nipponhub.nipponhubv0.Repositories.mysql.UserRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
 
@Service
public class CommandeService {
 
    @Autowired private CommandeRepository    commandeRepo;
    @Autowired private ProductRepository     productRepo;
    @Autowired private UserRepository     userRepo;          // your existing user repo
    @Autowired private SalesService          SalesService;
    @Autowired private ProductActivityRepository activityRepo;
 
    // ══════════════════════════════════════════════════════════════════════
    //  CLIENT operations
    // ══════════════════════════════════════════════════════════════════════
 
    /**
     * Place a new order.  Called by an authenticated CLIENT / USER.
     * No stock change here — only when admin marks it DELIVERED.
     */
    @Transactional
    public CommandeDto placeCommande(CommandeDto request, String clientEmail) {
        OurUsers client = requireUser(clientEmail);
 
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("La commande doit contenir au moins un article.");
        }
 
        Commande commande = new Commande();
        commande.setClient(client);
        commande.setNote(request.getNote());
        commande.setStatus(CommandeStatus.PENDING);
 
        List<CommandeItem> items = new ArrayList<>();
        for (CommandeItemDto dto : request.getItems()) {
            Product product = requireProduct(dto.getProductId());
 
            CommandeItem item = new CommandeItem();
            item.setProduct(product);
            item.setQuantite(dto.getQuantite());
            item.setPrixVendu(dto.getPrixVendu()); // may be null — resolved at delivery
            items.add(item);
 
            log(product, "COMMANDE_PLACED", clientEmail, client.getRole().name(),
                    "commandeId=pending | qty=" + dto.getQuantite());
        }
        commande.setItems(items);
        commande = commandeRepo.save(commande);
 
        // Update log entries with the real commande id
        final Long cid = commande.getId();
        activityRepo.findByActionTypeOrderByTimestampDesc("COMMANDE_PLACED").stream()
                .filter(a -> a.getDetails() != null && a.getDetails().contains("commandeId=pending")
                          && a.getPerformedBy().equals(clientEmail))
                .forEach(a -> {
                    a.setDetails(a.getDetails().replace("commandeId=pending", "commandeId=" + cid));
                    activityRepo.save(a);
                });
 
        CommandeDto result = toDto(commande);
        result.setMessage("Commande enregistrée avec succès");
        return result;
    }
 
    /** Client's own order history. */
    public List<CommandeDto> getMyOrders(String clientEmail) {
        OurUsers client = requireUser(clientEmail);
        return commandeRepo.findByClientOrderByCreatedAtDesc(client)
                .stream().map(this::toDto).collect(Collectors.toList());
    }
 
    /** Client fetches one specific order — ownership is enforced. */
    public CommandeDto getMyOrderById(Long id, String clientEmail) {
        Commande c = requireCommande(id);
        if (!c.getClient().getEmail().equals(clientEmail)) {
            throw new AccessDeniedException("Vous n'avez pas accès à cette commande.");
        }
        return toDto(c);
    }
 
    // ══════════════════════════════════════════════════════════════════════
    //  ADMIN / OWNER operations
    // ══════════════════════════════════════════════════════════════════════
 
    public List<CommandeDto> getAllCommandes() {
        return commandeRepo.findAllWithDetails().stream().map(this::toDto).collect(Collectors.toList());
    }
 
    public List<CommandeDto> getCommandesByStatus(CommandeStatus status) {
        return commandeRepo.findByStatusOrderByCreatedAtDesc(status)
                .stream().map(this::toDto).collect(Collectors.toList());
    }
 
    public CommandeDto getCommandeById(Long id) {
        return toDto(requireCommande(id));
    }
 
    /**
     * Update the status of a commande (CONFIRMED, CANCELLED, DELIVERED).
     *
     * When status = DELIVERED:
     *   1. SalesService creates a Sales linked to this commande + client
     *   2. Stock is decremented for each product
     *   3. Activity is logged for every product
     */
    @Transactional
    public CommandeDto updateStatus(Long id, CommandeStatus newStatus, String adminEmail) {
        Commande commande = requireCommande(id);
 
        if (commande.getStatus() == CommandeStatus.DELIVERED) {
            throw new IllegalStateException("Cette commande est déjà livrée. Aucune modification possible.");
        }
        if (commande.getStatus() == CommandeStatus.CANCELLED) {
            throw new IllegalStateException("Impossible de modifier une commande annulée.");
        }
 
        OurUsers admin = requireUser(adminEmail);
 
        commande.setStatus(newStatus);
        commande.setConfirmedBy(adminEmail);
 
        if (newStatus == CommandeStatus.DELIVERED) {
            // This is the moment stock is touched and Sales is created
            Sales Sales = SalesService.createFromCommande(commande, adminEmail, admin.getRole().name());
            commande.setSales(Sales);
 
            // Log activity for every product in the commande
            for (CommandeItem item : commande.getItems()) {
                log(item.getProduct(), "DELIVERED", adminEmail, admin.getRole().name(),
                        "commandeId=" + id + " | clientId=" + commande.getClient().getUserid()
                        + " | qty=" + item.getQuantite()
                        + " | SalesId=" + Sales.getId());
            }
        }
 
        if (newStatus == CommandeStatus.CONFIRMED) {
            for (CommandeItem item : commande.getItems()) {
                log(item.getProduct(), "CONFIRMED", adminEmail, admin.getRole().name(),
                        "commandeId=" + id);
            }
        }
 
        if (newStatus == CommandeStatus.CANCELLED) {
            for (CommandeItem item : commande.getItems()) {
                log(item.getProduct(), "CANCELLED", adminEmail, admin.getRole().name(),
                        "commandeId=" + id);
            }
        }
 
        commande = commandeRepo.save(commande);
        CommandeDto result = toDto(commande);
        result.setMessage("Statut mis à jour : " + newStatus);
        return result;
    }
 
    // ══════════════════════════════════════════════════════════════════════
    //  Private helpers
    // ══════════════════════════════════════════════════════════════════════
 
    private Commande requireCommande(Long id) {
        return commandeRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Commande introuvable : " + id));
    }
 
    private OurUsers requireUser(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable : " + email));
    }
 
    private Product requireProduct(Long id) {
        return productRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable : " + id));
    }
 
    private void log(Product product, String action, String email, String role, String details) {
        ProductActivity a = new ProductActivity();
        a.setProduct(product);
        a.setActionType(action);
        a.setPerformedBy(email);
        a.setPerformedByRole(role);
        a.setDetails(details);
        activityRepo.save(a);
    }
 
    // ── DTO mapping ───────────────────────────────────────────────────────
 
    public CommandeDto toDto(Commande c) {
        CommandeDto dto = new CommandeDto();
        dto.setId(c.getId());
        dto.setStatus(c.getStatus());
        dto.setNote(c.getNote());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());
        dto.setConfirmedBy(c.getConfirmedBy());
 
        if (c.getClient() != null) {
            dto.setClientId(c.getClient().getUserid());
            dto.setClientName(c.getClient().getName());
            dto.setClientEmail(c.getClient().getEmail());
        }
        if (c.getSales() != null) {
            dto.setId(c.getSales().getId());
        }
 
        List<CommandeItemDto> itemDtos = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        if (c.getItems() != null) {
            for (CommandeItem item : c.getItems()) {
                CommandeItemDto i = new CommandeItemDto();
                i.setProductId(item.getProduct().getIdProd());
                i.setProductName(item.getProduct().getProdName());
                i.setQuantite(item.getQuantite());
                BigDecimal price = (item.getPrixVendu() != null
                        && item.getPrixVendu().compareTo(BigDecimal.ZERO) > 0)
                        ? item.getPrixVendu()
                        : item.getProduct().getSoldPrice();
                i.setPrixVendu(price);
                i.setSubtotal(price.multiply(BigDecimal.valueOf(item.getQuantite())));
                total = total.add(i.getSubtotal());
                itemDtos.add(i);
            }
        }
        dto.setItems(itemDtos);
        dto.setTotal(total);
        return dto;
    }
}
 
