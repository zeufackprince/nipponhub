package com.nipponhub.nipponhubv0.DTO;

import java.math.BigDecimal;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CommandeItemDto {
 
    /** Required on POST — the product to order. */
    private Long productId;
 
    /** Populated on GET responses. */
    private String productName;
 
    private int quantite;
 
    /** Optional — proposed sale price per unit.  Null → product's soldPrice is used. */
    private BigDecimal prixVendu;
 
    /** Populated on GET: prixVendu × quantite. */
    private BigDecimal subtotal;
}
