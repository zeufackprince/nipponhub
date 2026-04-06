package com.nipponhub.nipponhubv0.Mappers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.nipponhub.nipponhubv0.DTO.VenteDto;
import com.nipponhub.nipponhubv0.DTO.VenteItemDetailDto;
import com.nipponhub.nipponhubv0.Models.Product;
import com.nipponhub.nipponhubv0.Models.Vente;
import com.nipponhub.nipponhubv0.Models.VenteItem;
import com.nipponhub.nipponhubv0.Repositories.mysql.ProductRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;


@AllArgsConstructor
@Component
public class VenteMapper {

    private final ProductRepository productRepository;
    
    public VenteDto mapVenteToDto(Vente vente, String message) {

        VenteDto dto = new VenteDto();
        dto.setId(vente.getId());
        dto.setDate(vente.getDate());
        dto.setMessage(message);

        BigDecimal coutTotal = BigDecimal.ZERO;
        BigDecimal prixVenduTotal = BigDecimal.ZERO;
        BigDecimal totalGain = BigDecimal.ZERO;
        int totalItem = 0;

        List<VenteItemDetailDto> itemDetails = vente.getItems().stream().map(item -> {
            Product prod = productRepository.findById(item.getProduct().getIdProd())
                .orElseThrow(() -> new EntityNotFoundException("Produit non trouvé"));

            BigDecimal qte = BigDecimal.valueOf(item.getQuantite());
            BigDecimal itemTotal;
            BigDecimal itemGain;
            BigDecimal appliedPrixVendu = item.getPrixVendu() != null ? item.getPrixVendu() : BigDecimal.ZERO;

            if (appliedPrixVendu.compareTo(BigDecimal.ZERO) == 0) {
                itemTotal = prod.getUnitPrice().multiply(qte);
                itemGain = BigDecimal.ZERO;
                appliedPrixVendu = prod.getUnitPrice();
            } else {
                itemTotal = appliedPrixVendu.multiply(qte);
                itemGain = appliedPrixVendu.subtract(prod.getUnitPrice()).multiply(qte);
            }

            return new VenteItemDetailDto(
                prod.getIdProd(),
                prod.getProdName(),
                item.getQuantite(),
                prod.getUnitPrice(),
                appliedPrixVendu,
                itemTotal,
                itemGain
            );
        }).collect(Collectors.toList());

        for (VenteItemDetailDto detail : itemDetails) {
            // Calculating the base cost (Unit Price * Qty)
            BigDecimal baseCost = detail.getPrixUnitaire().multiply(BigDecimal.valueOf(detail.getQuantite()));
            coutTotal = coutTotal.add(baseCost);
            
            prixVenduTotal = prixVenduTotal.add(detail.getTotal());
            totalGain = totalGain.add(detail.getGain());
            totalItem += detail.getQuantite();
        }

        dto.setCoutTotal(coutTotal);
        dto.setPrixVendu(prixVenduTotal);
        dto.setGain(totalGain);
        dto.setTotalItem(totalItem);
        dto.setItems(itemDetails);

        return dto;
    }

    public VenteDto toDto(Vente v, String message) {
        VenteDto dto = new VenteDto();
        dto.setId(v.getId());
        dto.setDate(v.getDate());
        dto.setMessage(message);
 
        // Client info (null for direct admin sales)
        if (v.getClient() != null) {
            dto.setClientId(v.getClient().getUserid());
            dto.setClientName(v.getClient().getName());
            dto.setClientEmail(v.getClient().getEmail());
        }
        if (v.getCommande() != null) {
            dto.setCommandeId(v.getCommande().getId());
        }
 
        BigDecimal coutTotal  = BigDecimal.ZERO;
        BigDecimal prixVendu  = BigDecimal.ZERO;
        BigDecimal gain       = BigDecimal.ZERO;
        int totalItem         = 0;
        List<VenteItemDetailDto> itemDtos = new ArrayList<>();
 
        for (VenteItem item : v.getItems()) {
            VenteItemDetailDto i = new VenteItemDetailDto();
            i.setProductId(item.getProduct().getIdProd());
            i.setProductName(item.getProduct().getProdName());
            i.setQuantite(item.getQuantite());
            i.setPrixUnitaire(item.getProduct().getUnitPrice());
            i.setPrixVendu(item.getPrixVendu());
            i.setTotal(item.getPrix());
            i.setGain(item.getGain());
 
            coutTotal  = coutTotal.add(item.getProduct().getUnitPrice()
                                        .multiply(BigDecimal.valueOf(item.getQuantite())));
            prixVendu  = prixVendu.add(item.getPrix() != null ? item.getPrix() : BigDecimal.ZERO);
            gain       = gain.add(item.getGain() != null ? item.getGain() : BigDecimal.ZERO);
            totalItem += item.getQuantite();
            itemDtos.add(i);
        }
 
        dto.setCoutTotal(coutTotal);
        dto.setPrixVendu(prixVendu);
        dto.setGain(gain);
        dto.setTotalItem(totalItem);
        dto.setItems(itemDtos);
        return dto;
    }

    
}
