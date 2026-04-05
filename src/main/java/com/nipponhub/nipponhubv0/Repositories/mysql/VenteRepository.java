package com.nipponhub.nipponhubv0.Repositories.mysql;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nipponhub.nipponhubv0.Models.OurUsers;
import com.nipponhub.nipponhubv0.Models.Vente;

public interface VenteRepository extends JpaRepository<Vente, Long> {

    List<Vente> findByDate(LocalDate date);

    /** All sales tied to a client (came from a commande). */
    List<Vente> findByClientOrderByDateDesc(OurUsers client);

    /** Look up the vente produced by a given commande. */
    Optional<Vente> findByCommandeId(Long commandeId);
}
