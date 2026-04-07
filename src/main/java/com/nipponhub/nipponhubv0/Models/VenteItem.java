package com.nipponhub.nipponhubv0.Models;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "vente_item")
public class VenteItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int quantite;

    private BigDecimal prix;

    @Column(nullable = false)
    private BigDecimal prixVendu;

    private BigDecimal gain;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vente_id", nullable = false)
    @JsonIgnore  // ✅ Prevent infinite recursion when serializing
    private Vente vente;
    
    // ✅ Transient field for incoming JSON deserialization (not persisted)
    @Transient
    private Long productId;
}

