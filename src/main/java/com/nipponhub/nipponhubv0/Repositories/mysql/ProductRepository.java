package com.nipponhub.nipponhubv0.Repositories.mysql;

import com.nipponhub.nipponhubv0.Models.City;
import com.nipponhub.nipponhubv0.Models.Country;
import com.nipponhub.nipponhubv0.Models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ─── STANDARD FINDERS ────────────────────────────────────────────────────────

    Optional<Product> findByProdName(String prodName);

    Optional<Product> findByProdNameIgnoreCase(String prodName);

    // ─── FETCH JOIN QUERIES ───────────────────────────────────────────────────────

    /**
     * FIX: Replaces findAll() for use in ProductService.getAllProducts().
     *
     * Plain findAll() returns Products with LAZY relations. Even with
     * @Transactional on the service, calling stream().map(mapper) after
     * findAll() can still hit proxy issues for @ManyToOne and @ManyToMany
     * fields because Spring may create sub-transactions internally.
     *
     * This JPQL query uses LEFT JOIN FETCH to eagerly load categoriesProd,
     * franchiseProd, and countries in a single SQL query — no proxy needed.
     *
     * Note: JOIN FETCH on a collection (@ManyToMany countries) requires
     * DISTINCT to avoid duplicate Product rows in the result.
     */
    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.categoriesProd
        LEFT JOIN FETCH p.franchiseProd
        LEFT JOIN FETCH p.countries
        """)
    List<Product> findAllWithRelations();

    /**
     * FIX: Fetch join version of findById — loads all lazy relations
     * in a single query so the mapper never needs a live session.
     *
     * @param id the product primary key
     * @return Optional<Product> with all relations populated
     */
    @Query("""
        SELECT p FROM Product p
        LEFT JOIN FETCH p.categoriesProd
        LEFT JOIN FETCH p.franchiseProd
        LEFT JOIN FETCH p.countries
        WHERE p.idProd = :id
        """)
    Optional<Product> findByIdWithRelations(@Param("id") Long id);

    /**
     * Fetch join version of findByProdName (case-insensitive).
     *
     * @param name the product name to search (case-insensitive)
     * @return Optional<Product> with all relations populated
     */
    @Query("""
        SELECT p FROM Product p
        LEFT JOIN FETCH p.categoriesProd
        LEFT JOIN FETCH p.franchiseProd
        LEFT JOIN FETCH p.countries
        WHERE LOWER(p.prodName) = LOWER(:name)
        """)
    Optional<Product> findByProdNameIgnoreCaseWithRelations(@Param("name") String name);

    List<Product> findByCountriesContaining(Country country);

     /**
     * NEW — used by the public searchByCategory endpoint.
     * Matches on the category's name field, case-insensitive.
     */
    List<Product> findByCategoriesProdCatProdNameIgnoreCase(String categoryName);

    // ─── LOCATION-BASED QUERIES ──────────────────────────────────────────────────

    /**
     * Find all products in a specific city (location-based filtering).
     * Useful for displaying products available in a selected city.
     *
     * @param city the City entity
     * @return list of products in the city
     */
    @Query("""
        SELECT p FROM Product p
        LEFT JOIN FETCH p.categoriesProd
        LEFT JOIN FETCH p.franchiseProd
        LEFT JOIN FETCH p.countries
        WHERE p.city = :city
        """)
    List<Product> findByCity(@Param("city") City city);

    /**
     * Find products in a city by city name and country name.
     * Convenience method to avoid needing City entity ID.
     *
     * @param cityName the city name
     * @param countryName the country name
     * @return list of products in the specified city
     */
    @Query("""
        SELECT p FROM Product p
        LEFT JOIN FETCH p.categoriesProd
        LEFT JOIN FETCH p.franchiseProd
        LEFT JOIN FETCH p.countries
        WHERE p.city.cityName = :cityName AND p.city.country.countryName = :countryName
        """)
    List<Product> findByCityAndCountry(
        @Param("cityName") String cityName,
        @Param("countryName") String countryName
    );

    /**
     * Find all products available in cities within a country.
     * Broader than city-specific query — returns all city products in country.
     *
     * @param country the Country entity
     * @return list of products in any city of the country
     */
    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.categoriesProd
        LEFT JOIN FETCH p.franchiseProd
        LEFT JOIN FETCH p.countries
        WHERE p.city.country = :country
        """)
    List<Product> findByCityCountry(@Param("country") Country country);

    /**
     * Find cities available for a product (which countries/cities it ships to).
     * Returns distinct cities that a product can be sold in.
     *
     * @param productId the product ID
     * @return list of cities where product is available
     */
    @Query("""
        SELECT DISTINCT p.city FROM Product p WHERE p.idProd = :productId
        """)
    List<City> findCitiesByProductId(@Param("productId") Long productId);

    }
