package com.nipponhub.nipponhubv0.Repositories.mysql;

import com.nipponhub.nipponhubv0.Models.City;
import com.nipponhub.nipponhubv0.Models.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for City entity (location hierarchy).
 * Handles database operations for cities and city-based queries.
 */
@Repository
public interface CityRepository extends JpaRepository<City, Long> {

    /**
     * Find cities by their country.
     * Useful for populating city dropdown once a country is selected.
     *
     * @param country the Country entity
     * @return list of cities in the country
     */
    List<City> findByCountry(Country country);

    /**
     * Find a specific city by name within a country.
     *
     * @param cityName the city name
     * @param country the Country entity
     * @return Optional city with matching name and country
     */
    Optional<City> findByCityNameAndCountry(String cityName, Country country);

    /**
     * Find cities by country name (convenience method).
     * Joins Country table to filter by country name.
     *
     * @param countryName the country name
     * @return list of cities in the country
     */
    @Query("SELECT c FROM City c WHERE c.country.countryName = :countryName")
    List<City> findByCountryName(@Param("countryName") String countryName);

    /**
     * Find a city by city name and country name (convenience method).
     * Useful for application without knowing the Country entity ID.
     *
     * @param cityName the city name
     * @param countryName the country name
     * @return Optional city matching both criteria
     */
    @Query("SELECT c FROM City c WHERE c.cityName = :cityName AND c.country.countryName = :countryName")
    Optional<City> findByCityNameAndCountryName(@Param("cityName") String cityName, @Param("countryName") String countryName);

    /**
     * Find all cities.
     * Useful for admin operations or generating city lists.
     *
     * @return list of all cities
     */
    List<City> findAll();

    /**
     * Find cities by city code.
     * Useful for API operations using city codes (e.g., "JP-TYO").
     *
     * @param cityCode the city code
     * @return Optional city with matching code
     */
    Optional<City> findByCityCode(String cityCode);
}
