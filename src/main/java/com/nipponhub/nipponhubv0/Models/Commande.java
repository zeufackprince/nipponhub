package com.nipponhub.nipponhubv0.Models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.nipponhub.nipponhubv0.Models.Enum.CommandeStatus;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A client order.  Stock is NOT touched until the admin marks it DELIVERED.
 * At that point SalesService creates the matching Sales and decrements stock.
 */
@Entity
@Table(name = "commande")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Commande {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    /** The authenticated client who placed the order. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private OurUsers client;
 
    /** Items requested. */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "commande_id")
    private List<CommandeItem> items = new ArrayList<>();
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommandeStatus status = CommandeStatus.PENDING;
 
    /** Optional note from the client (delivery address, remarks…). */
    @Column(length = 1000)
    private String note;
 
    /** Email of the admin / owner who last changed the status. */
    @Column(name = "confirmed_by")
    private String confirmedBy;
 
    /** Populated once status = DELIVERED. */
    @OneToOne(mappedBy = "commande", fetch = FetchType.LAZY)
    private Sales Sales;
 
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
 
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
 
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt  = LocalDateTime.now();
    }
 
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
