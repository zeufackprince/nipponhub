package com.nipponhub.nipponhubv0.Repositories.mysql;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nipponhub.nipponhubv0.Models.Restocks;

public interface RestockRepository extends JpaRepository<Restocks, Long>{
    
}
