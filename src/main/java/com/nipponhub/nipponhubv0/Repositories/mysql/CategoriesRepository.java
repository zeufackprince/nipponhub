package com.nipponhub.nipponhubv0.Repositories.mysql;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nipponhub.nipponhubv0.Models.CategoriesProd;
import com.nipponhub.nipponhubv0.Models.Product;

public interface CategoriesRepository extends JpaRepository<CategoriesProd, Long>{

    Optional<CategoriesProd> findByCatProdName(String catProdName);

    Optional<Product> findByCatProdNameIgnoreCase(String categoryName);
    
}
