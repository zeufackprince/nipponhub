package com.nipponhub.nipponhubv0.Models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "country")
@AllArgsConstructor
@Setter
@Getter
@NoArgsConstructor
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCountry;

    @Column(name = "country_name")
    private String countryName;

    @Column(name = "country_code", unique = true)
    private String countryCode;

    // ✅ mappedBy = the field name in Product
    @ManyToMany(mappedBy = "countries")
    @JsonIgnore  // prevents infinite recursion in JSON responses
    private List<Product> products = new ArrayList<>();

    /**
     * One-to-Many: a country can have many cities.
     * Bidirectional: inverse of @ManyToOne in City.
     */
    @OneToMany(mappedBy = "country", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnore  // prevents infinite recursion in JSON responses
    private List<City> cities = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
