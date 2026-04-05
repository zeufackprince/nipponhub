package com.nipponhub.nipponhubv0.Models;

import jakarta.persistence.*;
import java.math.BigDecimal;
 
/**
 * One line in a client order.
 * prixVendu is optional — the client may propose a price;
 * if null/0 the product's soldPrice is used at delivery time.
 */
@Entity
@Table(name = "commande_item")
public class CommandeItem {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
 
    @Column(nullable = false)
    private int quantite;
 
    /** Client-proposed selling price per unit.  Nullable — admin can override at delivery. */
    @Column(name = "prix_vendu", precision = 12, scale = 2)
    private BigDecimal prixVendu;
}
