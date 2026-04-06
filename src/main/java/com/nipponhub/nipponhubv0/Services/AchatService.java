package com.nipponhub.nipponhubv0.Services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nipponhub.nipponhubv0.DTO.AchatDto;
import com.nipponhub.nipponhubv0.DTO.AchatItemDetailDto;
import com.nipponhub.nipponhubv0.Models.AchatItem;
import com.nipponhub.nipponhubv0.Models.Achats;
import com.nipponhub.nipponhubv0.Models.Product;
import com.nipponhub.nipponhubv0.Models.ProductActivity;
import com.nipponhub.nipponhubv0.Repositories.mysql.AchatRepository;
import com.nipponhub.nipponhubv0.Repositories.mysql.ProductActivityRepository;
import com.nipponhub.nipponhubv0.Repositories.mysql.ProductRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AchatService {

    private final AchatRepository achatRepository;
    private final ProductService productService;
    private final ProductActivityRepository activityRepos;
    private final ProductRepository productRepository;

    @Transactional
    public AchatDto createAchat(Achats request, String performedBy, String performedByRole) {
        Achats achat = new Achats();
        achat.setDate(LocalDate.now());  // ignore client-supplied date

        List<AchatItem> items = new ArrayList<>();
        for (AchatItem reqItem : request.getItems()) {
            Product product = productRepository.findById(reqItem.getProduct().getIdProd())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Produit non trouvé avec ID : " + reqItem.getProduct().getIdProd()));

            AchatItem item = new AchatItem();
            item.setProduct(product);
            item.setQuantite(reqItem.getQuantite());
            item.setAchat(achat);
            items.add(item);

            // Increase stock
            product.setProdQty(product.getProdQty() + reqItem.getQuantite());
            productRepository.save(product);

            // Audit log
            ProductActivity log = new ProductActivity();
            log.setProduct(product);
            log.setActionType("ACHAT");
            log.setPerformedBy(performedBy);
            log.setPerformedByRole(performedByRole);
            log.setDetails("qty+=" + reqItem.getQuantite()
                    + " | unitPrice=" + product.getUnitPrice()
                    + " | newStock=" + product.getProdQty());
            activityRepos.save(log);
        }

        achat.setItems(items);
        achat = achatRepository.save(achat);
        return toDto(achat, "Achat enregistré avec succès");
    }

    public List<AchatDto> getAllAchats() {
        return achatRepository.findAll().stream()
                .map(a -> toDto(a, null))
                .collect(Collectors.toList());
    }

    // ── DTO mapping ───────────────────────────────────────────────────────
    private AchatDto toDto(Achats a, String message) {
        AchatDto dto = new AchatDto();
        dto.setId(a.getId());
        dto.setDate(a.getDate());
        dto.setMessage(message);

        BigDecimal total = BigDecimal.ZERO;
        int totalItem = 0;
        List<AchatItemDetailDto> itemDtos = new ArrayList<>();

        for (AchatItem item : a.getItems()) {
            AchatItemDetailDto i = new AchatItemDetailDto();
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