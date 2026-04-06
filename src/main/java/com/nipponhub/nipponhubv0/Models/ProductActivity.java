package com.nipponhub.nipponhubv0.Models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
 
/**
 * Immutable audit record written every time a product is touched.
 *
 * actionType values:
 *   CREATED          – product first saved
 *   UPDATED          – product fields / images changed
 *   DELETED          – product removed
 *   ACHAT            – admin / owner registered a purchase (stock ↑)
 *   COMMANDE_PLACED  – a client added this product to an order
 *   CONFIRMED        – admin confirmed the order containing this product
 *   DELIVERED        – order delivered → stock ↓, vente created
 *   CANCELLED        – order containing this product was cancelled
 */
@Entity
@Table(name = "product_activity",
       indexes = {
           @Index(name = "idx_pa_product",      columnList = "product_id"),
           @Index(name = "idx_pa_performed_by", columnList = "performed_by")
       })
@Data
public class ProductActivity {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "action_type", nullable = false, length = 30)
    private String actionType;
 
    /** Email of the user who triggered this action. */
    @Column(name = "performed_by", nullable = false)
    private String performedBy;
 
    @Column(name = "performed_by_role", length = 20)
    private String performedByRole;
 
    /** Free-text details: qty, prices, commande id, etc. */
    @Column(columnDefinition = "TEXT")
    private String details;
 
    @Column(name = "timestamp", updatable = false)
    private LocalDateTime timestamp;
 
    @PrePersist
    protected void onCreate() { timestamp = LocalDateTime.now(); }
}
