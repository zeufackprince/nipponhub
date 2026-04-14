package com.nipponhub.nipponhubv0.Services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nipponhub.nipponhubv0.DTO.SalesDto;
import com.nipponhub.nipponhubv0.Models.Commande;
import com.nipponhub.nipponhubv0.Models.CommandeItem;
import com.nipponhub.nipponhubv0.Models.Product;
import com.nipponhub.nipponhubv0.Models.Sales;
import com.nipponhub.nipponhubv0.Models.SalesItem;
import com.nipponhub.nipponhubv0.Repositories.mysql.ProductRepository;
import com.nipponhub.nipponhubv0.Repositories.mysql.SalesRepository;
import com.nipponhub.nipponhubv0.Mappers.SalesMapper;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class SalesService {

    private final ProductService productService;

    private final ProductRepository productRepository;
    
    private final SalesRepository SalesRepo;

    private final SalesMapper SalesMapper;


    @Transactional
    public SalesDto registerSales(Sales Sales) {
        Sales.setDate(LocalDate.now());

        for (SalesItem item : Sales.getItems()) {
            // ✅ Handle case where product is null but productId is provided (from JSON deserialization)
            Long productId;
            if (item.getProduct() != null) {
                productId = item.getProduct().getIdProd();
            } else if (item.getProductId() != null) {
                productId = item.getProductId();
            } else {
                throw new IllegalArgumentException("SalesItem must have either product object or productId");
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
            item.setSales(Sales);
        }

        Sales saved = SalesRepo.save(Sales);
        return SalesMapper.toDto(saved, "Sales enregistrée avec succès");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Called by CommandeService when an order is DELIVERED
    //  This is the ONLY path that decrements stock for client orders.
    // ══════════════════════════════════════════════════════════════════════
 
    /**
     * Create a Sales from a delivered Commande.
     *  - Decrements product stock (rolls back the whole transaction on failure)
     *  - Links the Sales to the client and commande
     */
    @Transactional
    public Sales createFromCommande(Commande commande, String adminEmail, String adminRole) {
        Sales Sales = new Sales();
        Sales.setDate(LocalDate.now());
        Sales.setClient(commande.getClient());
        Sales.setCommande(commande);
 
        List<SalesItem> SalesItems = new ArrayList<>();
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
 
            SalesItem vi = new SalesItem();
            vi.setProduct(product);
            vi.setQuantite(qty);
            vi.setPrixVendu(prixVendu);
            vi.setPrix(totalLigne);
            vi.setGain(gainLigne);
            vi.setSales(Sales);  // ✅ Set the Sales reference for proper cascading
            SalesItems.add(vi);
 
            // Decrement stock
            product.setProdQty(product.getProdQty() - qty);
            productRepository.save(product);
        }
 
        Sales.setItems(SalesItems);
        return SalesRepo.save(Sales);
    }
 
    // ══════════════════════════════════════════════════════════════════════
    //  Direct admin / owner sale (no commande — e.g. in-store / B2B)
    //  Kept from original implementation; ADMIN & OWNER only via controller.
    // ══════════════════════════════════════════════════════════════════════
 
    @Transactional
    public SalesDto registerDirectSales(Sales request) {
        Sales Sales = new Sales();
        Sales.setDate(LocalDate.now());
        // client and commande are null for direct sales
 
        List<SalesItem> items = new ArrayList<>();
        for (SalesItem reqItem : request.getItems()) {
            // ✅ Handle case where product is null but productId is provided (from JSON deserialization)
            Long productId;
            if (reqItem.getProduct() != null) {
                productId = reqItem.getProduct().getIdProd();
            } else if (reqItem.getProductId() != null) {
                productId = reqItem.getProductId();
            } else {
                throw new IllegalArgumentException("SalesItem must have either product object or productId");
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
 
            SalesItem vi = new SalesItem();
            vi.setProduct(product);
            vi.setQuantite(qty);
            vi.setPrixVendu(prixVendu);
            vi.setPrix(total);
            vi.setGain(gain);
            vi.setSales(Sales);  // ✅ Set Sales reference
            items.add(vi);
 
            product.setProdQty(product.getProdQty() - qty);
            productRepository.save(product);
        }
 
        Sales.setItems(items);
        return SalesMapper.toDto(SalesRepo.save(Sales), "Sales enregistrée avec succès");
    }

    @Transactional(readOnly = true)
    public List<SalesDto> getAllSales() {
        List<Sales> allSales = this.SalesRepo.findAll();
        if(allSales.isEmpty()){
            throw new EntityNotFoundException("No sales data found in DB!!");
        }
        return allSales.stream()
                .map(Sales -> SalesMapper.toDto(Sales, "Saless fetched successfully"))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SalesDto getSalesById(Long id) {
        Sales Sales = SalesRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Sales with ID " + id + " not found"));
        return SalesMapper.toDto(Sales, "Sales fetched successfully");
    }

    @Transactional(readOnly = true)
    public List<SalesDto> getSalessByDate(LocalDate date) {
        List<Sales> Saless = SalesRepo.findByDate(date);
        if (Saless.isEmpty()) {
            throw new EntityNotFoundException("No Saless found for date " + date);
        }
        return Saless.stream()
                .map(Sales -> SalesMapper.toDto(Sales, "Saless fetched successfully"))
                .collect(Collectors.toList());
    }

}