package com.nipponhub.nipponhubv0.Models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * City entity: represents cities within a country.
 * Hierarchy: Country → Cities → Products (by city)
 */
@Entity
@Table(name = "city", indexes = {
    @Index(name = "idx_city_country", columnList = "country_id"),
    @Index(name = "idx_city_name", columnList = "city_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCity;

    @Column(name = "city_name", nullable = false)
    private String cityName;

    @Column(name = "city_code", nullable = false)
    private String cityCode;  // e.g., "JP-TYO" for Tokyo, Japan

    /**
     * Many-to-One: each City belongs to exactly one Country.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    /**
     * One-to-Many: a city can have many products.
     * Products can be filtered by city.
     */
    @OneToMany(mappedBy = "city", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore  // Prevent infinite recursion in JSON
    private List<Product> products = new ArrayList<>();

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof City)) return false;
        City city = (City) o;
        return idCity != null && idCity.equals(city.idCity);
    }

    @Override
    public int hashCode() {
        return idCity != null ? idCity.hashCode() : 0;
    }
}
