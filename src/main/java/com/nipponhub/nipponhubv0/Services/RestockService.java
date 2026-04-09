package com.nipponhub.nipponhubv0.Services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nipponhub.nipponhubv0.DTO.RestockDto;
import com.nipponhub.nipponhubv0.DTO.RestockItemDetailDto;
import com.nipponhub.nipponhubv0.Models.RestockItem;
import com.nipponhub.nipponhubv0.Models.Restocks;
import com.nipponhub.nipponhubv0.Models.Product;
import com.nipponhub.nipponhubv0.Models.ProductActivity;
import com.nipponhub.nipponhubv0.Repositories.mysql.RestockRepository;
import com.nipponhub.nipponhubv0.Repositories.mysql.ProductActivityRepository;
import com.nipponhub.nipponhubv0.Repositories.mysql.ProductRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class RestockService {

    private final RestockRepository RestockRepository;
    private final ProductActivityRepository activityRepos;
    private final ProductRepository productRepository;

    @Transactional
    public RestockDto createRestock(Restocks request, String performedBy, String performedByRole) {
        Restocks Restock = new Restocks();
        Restock.setDate(LocalDate.now());  // ignore client-supplied date

        List<RestockItem> items = new ArrayList<>();
        for (RestockItem reqItem : request.getItems()) {
            Product product = productRepository.findById(reqItem.getProduct().getIdProd())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Produit non trouvé avec ID : " + reqItem.getProduct().getIdProd()));

            RestockItem item = new RestockItem();
            item.setProduct(product);
            item.setQuantite(reqItem.getQuantite());
            item.setRestock(Restock);
            items.add(item);

            // Increase stock
            product.setProdQty(product.getProdQty() + reqItem.getQuantite());
            productRepository.save(product);

            // Audit log
            ProductActivity log = new ProductActivity();
            log.setProduct(product);
            log.setActionType("Restock");
            log.setPerformedBy(performedBy);
            log.setPerformedByRole(performedByRole);
            log.setDetails("qty+=" + reqItem.getQuantite()
                    + " | unitPrice=" + product.getUnitPrice()
                    + " | newStock=" + product.getProdQty());
            activityRepos.save(log);
        }

        Restock.setItems(items);
        Restock = RestockRepository.save(Restock);
        return toDto(Restock, "Restock enregistré avec succès");
    }

    public List<RestockDto> getAllRestocks() {
        return RestockRepository.findAll().stream()
                .map(a -> toDto(a, null))
                .collect(Collectors.toList());
    }

    // ── DTO mapping ───────────────────────────────────────────────────────
    private RestockDto toDto(Restocks a, String message) {
        RestockDto dto = new RestockDto();
        dto.setId(a.getId());
        dto.setDate(a.getDate());
        dto.setMessage(message);

        BigDecimal total = BigDecimal.ZERO;
        int totalItem = 0;
        List<RestockItemDetailDto> itemDtos = new ArrayList<>();

        for (RestockItem item : a.getItems()) {
            RestockItemDetailDto i = new RestockItemDetailDto();
            i.setProductId(item.getProduct().getIdProd());
            i.setProductName(item.getProduct().getProdName());
            i.setQuantite(item.getQuantite());
            i.setPrixUnitaire(item.getProduct().getUnitPrice());
            i.setTotal(item.getProduct().getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantite())));
            total = total.add(i.getTotal());
            totalItem += item.getQuantite();
            itemDtos.add(i);
        }

        dto.setCoutTotal(total);
        dto.setTotalItem(totalItem);
        dto.setItems(itemDtos);
        return dto;
    }
}