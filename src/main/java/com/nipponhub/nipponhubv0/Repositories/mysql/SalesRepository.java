package com.nipponhub.nipponhubv0.Repositories.mysql;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nipponhub.nipponhubv0.Models.OurUsers;
import com.nipponhub.nipponhubv0.Models.Sales;

public interface SalesRepository extends JpaRepository<Sales, Long> {

    List<Sales> findByDate(LocalDate date);

    /** All sales tied to a client (came from a commande). */
    List<Sales> findByClientOrderByDateDesc(OurUsers client);

    /** Look up the Sales produced by a given commande. */
    Optional<Sales> findByCommandeId(Long commandeId);
}
