package com.nipponhub.nipponhubv0.Services;

import com.nipponhub.nipponhubv0.DTO.CityDto;
import com.nipponhub.nipponhubv0.Mappers.CityMapper;
import com.nipponhub.nipponhubv0.Models.City;
import com.nipponhub.nipponhubv0.Models.Country;
import com.nipponhub.nipponhubv0.Repositories.mysql.CityRepository;
import com.nipponhub.nipponhubv0.Repositories.mysql.CountryRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for City operations.
 * Handles location-based queries and city management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;
    private final CountryRepository countryRepository;
    private final CityMapper cityMapper;

    // ─── CREATE ──────────────────────────────────────────────────────────────────

    /**
     * Create a new city in a country.
     * ADMIN only operation for location hierarchy management.
     *
     * @param cityName the city name
     * @param cityCode the city code (e.g., "JP-TYO" for Tokyo, Japan)
     * @param countryName the country that contains this city
     * @return CityDto with the newly created city
     * @throws RuntimeException if country not found or validation fails
     */
    @Transactional
    public CityDto createCity(String cityName, String cityCode, String countryName) {
        if (cityName == null || cityName.isBlank()) {
            throw new IllegalArgumentException("City name is required");
        }
        if (cityCode == null || cityCode.isBlank()) {
            throw new IllegalArgumentException("City code is required");
        }
        if (countryName == null || countryName.isBlank()) {
            throw new IllegalArgumentException("Country name is required");
        }

        Country country = countryRepository.getByCountryName(countryName)
            .orElseThrow(() -> new EntityNotFoundException("Country not found: " + countryName));

        // Check if city already exists
        Optional<City> existing = cityRepository.findByCityNameAndCountry(cityName, country);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("City already exists: " + cityName + " in " + countryName);
        }

        City city = new City();
        city.setCityName(cityName);
        city.setCityCode(cityCode);
        city.setCountry(country);

        City saved = cityRepository.save(city);
        log.info("City created — id: {}, name: {}, country: {}", saved.getIdCity(), cityName, countryName);

        return cityMapper.cityToDto(saved);
    }

    // ─── READ ────────────────────────────────────────────────────────────────────

    /**
     * Get all cities in a country.
     * Public endpoint — useful for populating city dropdown.
     *
     * @param countryName the country name
     * @return list of CityDto objects
     */
    @Transactional
    public List<CityDto> getCitiesByCountry(String countryName) {
        List<CityDto> res = new ArrayList<>();
        try {
            List<City> cities = cityRepository.findByCountryName(countryName);
            if (cities.isEmpty()) {
                log.info("No cities found for country: {}", countryName);
                return res;
            }
            res = cities.stream()
                .map(cityMapper::cityToDto)
                .collect(Collectors.toList());
            log.info("Found {} city/cities in country: {}", res.size(), countryName);
        } catch (Exception e) {
            log.error("Error fetching cities for country '{}': {}", countryName, e.getMessage(), e);
        }
        return res;
    }

    /**
     * Get a specific city by name and country.
     *
     * @param cityName the city name
     * @param countryName the country name
     * @return CityDto if found, empty Optional otherwise
     */
    @Transactional
    public Optional<CityDto> getCity(String cityName, String countryName) {
        try {
            Optional<City> city = cityRepository.findByCityNameAndCountryName(cityName, countryName);
            return city.map(cityMapper::cityToDto);
        } catch (Exception e) {
            log.error("Error fetching city '{}' in country '{}': {}", cityName, countryName, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Get a city by ID.
     *
     * @param idCity the city ID
     * @return CityDto if found, empty Optional otherwise
     */
    @Transactional
    public Optional<CityDto> getCityById(Long idCity) {
        try {
            Optional<City> city = cityRepository.findById(idCity);
            return city.map(cityMapper::cityToDto);
        } catch (Exception e) {
            log.error("Error fetching city with id {}: {}", idCity, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Get all cities.
     *
     * @return list of all CityDto objects
     */
    @Transactional
    public List<CityDto> getAllCities() {
        try {
            List<City> cities = cityRepository.findAll();
            return cities.stream()
                .map(cityMapper::cityToDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching all cities: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────────

    /**
     * Update city details.
     * ADMIN only operation.
     *
     * @param idCity the city ID
     * @param cityName the new city name (optional)
     * @param cityCode the new city code (optional)
     * @return CityDto with updated city
     * @throws EntityNotFoundException if city not found
     */
    @Transactional
    public CityDto updateCity(Long idCity, String cityName, String cityCode) {
        City city = cityRepository.findById(idCity)
            .orElseThrow(() -> new EntityNotFoundException("City not found: " + idCity));

        if (cityName != null && !cityName.isBlank()) {
            city.setCityName(cityName);
        }
        if (cityCode != null && !cityCode.isBlank()) {
            city.setCityCode(cityCode);
        }

        City updated = cityRepository.save(city);
        log.info("City updated — id: {}", idCity);

        return cityMapper.cityToDto(updated);
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────────

    /**
     * Delete a city.
     * ADMIN only operation.
     * WARNING: All products in this city will have their city reference set to NULL.
     *
     * @param idCity the city ID
     * @throws EntityNotFoundException if city not found
     */
    @Transactional
    public void deleteCity(Long idCity) {
        City city = cityRepository.findById(idCity)
            .orElseThrow(() -> new EntityNotFoundException("City not found: " + idCity));

        cityRepository.delete(city);
        log.info("City deleted — id: {}", idCity);
    }
}
