package com.nipponhub.nipponhubv0.Models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Vente {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(name = "dateVente", nullable = false)
    private LocalDate date;

    @OneToMany(mappedBy = "vente", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<VenteItem> items = new ArrayList<>();  // ✅ Initialize to empty list to avoid null

     /**
     * The client whose order triggered this sale.
     * Null for direct admin sales (no commande).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private OurUsers client;
 
    /**
     * The commande this vente was created from.
     * Null for direct admin sales.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id", unique = true)
    private Commande commande;

}
