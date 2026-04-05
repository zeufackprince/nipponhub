package com.nipponhub.nipponhubv0.Repositories.mysql;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nipponhub.nipponhubv0.Models.Product;
import com.nipponhub.nipponhubv0.Models.ProductActivity;

import java.util.List;
 
public interface ProductActivityRepository extends JpaRepository<ProductActivity, Long> {
 
    /** Full audit trail for one product, newest first. */
    List<ProductActivity> findByProductOrderByTimestampDesc(Product product);
 
    /** All actions performed by a specific user. */
    List<ProductActivity> findByPerformedByOrderByTimestampDesc(String email);
 
    /** All actions of a specific type across all products. */
    List<ProductActivity> findByActionTypeOrderByTimestampDesc(String actionType);
}
