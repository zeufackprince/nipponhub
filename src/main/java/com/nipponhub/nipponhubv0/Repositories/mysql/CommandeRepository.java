package com.nipponhub.nipponhubv0.Repositories.mysql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.nipponhub.nipponhubv0.Models.Commande;
import com.nipponhub.nipponhubv0.Models.OurUsers;
import com.nipponhub.nipponhubv0.Models.Enum.CommandeStatus;

import java.util.List;
 
public interface CommandeRepository extends JpaRepository<Commande, Long> {
 
    /** All orders for a specific client, newest first. */
    List<Commande> findByClientOrderByCreatedAtDesc(OurUsers client);
 
    /** All orders with a given status. */
    List<Commande> findByStatusOrderByCreatedAtDesc(CommandeStatus status);
 
    /** All orders, newest first — used by admin/owner. */
    List<Commande> findAllByOrderByCreatedAtDesc();
 
    /** All orders for a client with a given status. */
    List<Commande> findByClientAndStatusOrderByCreatedAtDesc(OurUsers client, CommandeStatus status);
 
    /**
     * Admin view: commandes with eager-loaded client and items
     * (avoids N+1 on the admin "all orders" page).
     */
    @Query("SELECT DISTINCT c FROM Commande c " +
           "LEFT JOIN FETCH c.client " +
           "LEFT JOIN FETCH c.items i " +
           "LEFT JOIN FETCH i.product " +
           "ORDER BY c.createdAt DESC")
    List<Commande> findAllWithDetails();
    
}
