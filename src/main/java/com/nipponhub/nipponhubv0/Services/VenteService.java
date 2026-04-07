package com.nipponhub.nipponhubv0.Services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nipponhub.nipponhubv0.DTO.VenteDto;
import com.nipponhub.nipponhubv0.Models.Commande;
import com.nipponhub.nipponhubv0.Models.CommandeItem;
import com.nipponhub.nipponhubv0.Models.Product;
import com.nipponhub.nipponhubv0.Models.Vente;
import com.nipponhub.nipponhubv0.Models.VenteItem;
import com.nipponhub.nipponhubv0.Repositories.mysql.ProductRepository;
import com.nipponhub.nipponhubv0.Repositories.mysql.VenteRepository;
import com.nipponhub.nipponhubv0.Mappers.VenteMapper;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class VenteService {

    private final ProductService productService;

    private final ProductRepository productRepository;
    
    private final VenteRepository venteRepo;

    private final VenteMapper venteMapper;


    @Transactional
    public VenteDto registerVente(Vente vente) {
        vente.setDate(LocalDate.now());

        for (VenteItem item : vente.getItems()) {
            // ✅ Handle case where product is null but productId is provided (from JSON deserialization)
            Long productId;
            if (item.getProduct() != null) {
                productId = item.getProduct().getIdProd();
            } else if (item.getProductId() != null) {
                productId = item.getProductId();
            } else {
                throw new IllegalArgumentException("VenteItem must have either product object or productId");
            }
            
            Product prod = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Produit non trouvé avec ID: " + productId));

            BigDecimal qte = BigDecimal.valueOf(item.getQuantite());
            BigDecimal itemTotal;
            BigDecimal itemGain;

            // Handle default selling price
            if (item.getPrixVendu() == null || item.getPrixVendu().compareTo(BigDecimal.ZERO) == 0) {
                itemTotal = prod.getUnitPrice().multiply(qte);
                itemGain = BigDecimal.ZERO;
                item.setPrixVendu(prod.getUnitPrice()); // Update the item with actual selling price used
            } else {
                itemTotal = item.getPrixVendu().multiply(qte);
                itemGain = item.getPrixVendu().subtract(prod.getUnitPrice()).multiply(qte);
            }

            // Update DB Stock
            productService.retirerQuantite(prod.getIdProd(), item.getQuantite());

            item.setPrix(itemTotal);
            item.setGain(itemGain);
            item.setProduct(prod);  // ✅ Ensure product is set
            item.setVente(vente);
        }

        Vente saved = venteRepo.save(vente);
        return venteMapper.toDto(saved, "Vente enregistrée avec succès");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Called by CommandeService when an order is DELIVERED
    //  This is the ONLY path that decrements stock for client orders.
    // ══════════════════════════════════════════════════════════════════════
 
    /**
     * Create a Vente from a delivered Commande.
     *  - Decrements product stock (rolls back the whole transaction on failure)
     *  - Links the Vente to the client and commande
     */
    @Transactional
    public Vente createFromCommande(Commande commande, String adminEmail, String adminRole) {
        Vente vente = new Vente();
        vente.setDate(LocalDate.now());
        vente.setClient(commande.getClient());
        vente.setCommande(commande);
 
        List<VenteItem> venteItems = new ArrayList<>();
        for (CommandeItem ci : commande.getItems()) {
            Product product = productRepository.findById(ci.getProduct().getIdProd())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Produit introuvable : " + ci.getProduct().getIdProd()));
 
            int qty = ci.getQuantite();
            if (product.getProdQty() < qty) {
                throw new IllegalStateException(
                        "Stock insuffisant pour « " + product.getProdName() + " ». "
                        + "Disponible : " + product.getProdQty() + ", demandé : " + qty);
            }
 
            // Determine actual sale price
            BigDecimal prixVendu = (ci.getPrixVendu() != null
                    && ci.getPrixVendu().compareTo(BigDecimal.ZERO) > 0)
                    ? ci.getPrixVendu()
                    : product.getSoldPrice();
 
            BigDecimal coutLigne  = product.getUnitPrice().multiply(BigDecimal.valueOf(qty));
            BigDecimal totalLigne = prixVendu.multiply(BigDecimal.valueOf(qty));
            BigDecimal gainLigne  = totalLigne.subtract(coutLigne);
 
            VenteItem vi = new VenteItem();
            vi.setProduct(product);
            vi.setQuantite(qty);
            vi.setPrixVendu(prixVendu);
            vi.setPrix(totalLigne);
            vi.setGain(gainLigne);
            vi.setVente(vente);  // ✅ Set the vente reference for proper cascading
            venteItems.add(vi);
 
            // Decrement stock
            product.setProdQty(product.getProdQty() - qty);
            productRepository.save(product);
        }
 
        vente.setItems(venteItems);
        return venteRepo.save(vente);
    }
 
    // ══════════════════════════════════════════════════════════════════════
    //  Direct admin / owner sale (no commande — e.g. in-store / B2B)
    //  Kept from original implementation; ADMIN & OWNER only via controller.
    // ══════════════════════════════════════════════════════════════════════
 
    @Transactional
    public VenteDto registerDirectVente(Vente request) {
        Vente vente = new Vente();
        vente.setDate(LocalDate.now());
        // client and commande are null for direct sales
 
        List<VenteItem> items = new ArrayList<>();
        for (VenteItem reqItem : request.getItems()) {
            // ✅ Handle case where product is null but productId is provided (from JSON deserialization)
            Long productId;
            if (reqItem.getProduct() != null) {
                productId = reqItem.getProduct().getIdProd();
            } else if (reqItem.getProductId() != null) {
                productId = reqItem.getProductId();
            } else {
                throw new IllegalArgumentException("VenteItem must have either product object or productId");
            }
            
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Produit non trouvé avec ID : " + productId));
 
            int qty = reqItem.getQuantite();
            if (product.getProdQty() < qty) {
                throw new IllegalStateException(
                        "Insufficient stock. Current: " + product.getProdQty()
                        + ", requested: " + qty);
            }
 
            BigDecimal prixVendu = (reqItem.getPrixVendu() == null
                    || reqItem.getPrixVendu().compareTo(BigDecimal.ZERO) == 0)
                    ? product.getUnitPrice()
                    : reqItem.getPrixVendu();
 
            BigDecimal cout  = product.getUnitPrice().multiply(BigDecimal.valueOf(qty));
            BigDecimal total = prixVendu.multiply(BigDecimal.valueOf(qty));
            BigDecimal gain  = total.subtract(cout);
 
            VenteItem vi = new VenteItem();
            vi.setProduct(product);
            vi.setQuantite(qty);
            vi.setPrixVendu(prixVendu);
            vi.setPrix(total);
            vi.setGain(gain);
            vi.setVente(vente);  // ✅ Set vente reference
            items.add(vi);
 
            product.setProdQty(product.getProdQty() - qty);
            productRepository.save(product);
        }
 
        vente.setItems(items);
        return venteMapper.toDto(venteRepo.save(vente), "Vente enregistrée avec succès");
    }

    @Transactional(readOnly = true)
    public List<VenteDto> getAllVente() {
        List<Vente> allVente = this.venteRepo.findAll();
        if(allVente.isEmpty()){
            throw new EntityNotFoundException("No sales data found in DB!!");
        }
        return allVente.stream()
                .map(vente -> venteMapper.toDto(vente, "Ventes fetched successfully"))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VenteDto getVenteById(Long id) {
        Vente vente = venteRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Vente with ID " + id + " not found"));
        return venteMapper.toDto(vente, "Vente fetched successfully");
    }

    @Transactional(readOnly = true)
    public List<VenteDto> getVentesByDate(LocalDate date) {
        List<Vente> ventes = venteRepo.findByDate(date);
        if (ventes.isEmpty()) {
            throw new EntityNotFoundException("No ventes found for date " + date);
        }
        return ventes.stream()
                .map(vente -> venteMapper.toDto(vente, "Ventes fetched successfully"))
                .collect(Collectors.toList());
    }

}