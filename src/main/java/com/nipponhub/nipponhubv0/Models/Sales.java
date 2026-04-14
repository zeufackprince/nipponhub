package com.nipponhub.nipponhubv0.Models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table(name = "sales")
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Sales {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(name = "dateSales", nullable = false)
    private LocalDate date;

    @OneToMany(mappedBy = "Sales", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<SalesItem> items = new ArrayList<>();  // ✅ Initialize to empty list to avoid null

     /**
     * The client whose order triggered this sale.
     * Null for direct admin sales (no commande).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private OurUsers client;
 
    /**
     * The commande this Sales was created from.
     * Null for direct admin sales.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id", unique = true)
    private Commande commande;

}
