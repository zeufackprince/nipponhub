package com.nipponhub.nipponhubv0.Controllers;

import com.nipponhub.nipponhubv0.DTO.CityDto;
import com.nipponhub.nipponhubv0.Services.CityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for City management and location-based queries.
 * Endpoints for CRUD operations and city-country hierarchy navigation.
 */
@Slf4j
@RestController
@RequestMapping("/api/v0/city")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    // ─── CREATE ──────────────────────────────────────────────────────────────────

    /**
     * POST /api/v0/city/create
     * Create a new city within a country (ADMIN only).
     * 
     * Example request:
     * POST /api/v0/city/create?cityName=Tokyo&cityCode=JP-TYO&countryName=Japan
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCity(
            @RequestParam String cityName,
            @RequestParam String cityCode,
            @RequestParam String countryName) {
        try {
            CityDto city = cityService.createCity(cityName, cityCode, countryName);
            return ResponseEntity.status(HttpStatus.CREATED).body(city);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Validation error: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating city: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error creating city: " + e.getMessage()));
        }
    }

    // ─── READ ────────────────────────────────────────────────────────────────────

    /**
     * GET /api/v0/city/all
     * Get all cities (public endpoint).
     */
    @GetMapping("/all")
    public ResponseEntity<List<CityDto>> getAllCities() {
        List<CityDto> cities = cityService.getAllCities();
        return ResponseEntity.ok(cities);
    }

    /**
     * GET /api/v0/city/{idCity}
     * Get a specific city by ID (public endpoint).
     */
    @GetMapping("/{idCity}")
    public ResponseEntity<?> getCityById(@PathVariable Long idCity) {
        Optional<CityDto> city = cityService.getCityById(idCity);
        if (city.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("City not found with id: " + idCity));
        }
        return ResponseEntity.ok(city.get());
    }

    /**
     * GET /api/v0/city/search?country=...&city=...
     * Get a specific city by name and country (public endpoint).
     */
    
    @GetMapping("/byCityCountry")
    public ResponseEntity<?> getCity(
            @RequestParam String cityName,
            @RequestParam String countryName) {
        Optional<CityDto> city = cityService.getCity(cityName, countryName);
        if (city.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("City not found: " + cityName + " in " + countryName));
        }
        return ResponseEntity.ok(city.get());
    }

    /**
     * GET /api/v0/city/byCountry?country=...
     * Get all cities in a country (public endpoint).
     * Useful for populating city dropdown after country selection.
     * 
     * Example: GET /api/v0/city/byCountry?country=Japan
     */
    @GetMapping("/searchbyCountry")
    public ResponseEntity<List<CityDto>> getCitiesByCountry(@RequestParam String countryName) {
        List<CityDto> cities = cityService.getCitiesByCountry(countryName);
        return ResponseEntity.ok(cities);
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────────

    /**
     * PUT /api/v0/city/{idCity}
     * Update city details (ADMIN only).
     * 
     * Example request:
     * PUT /api/v0/city/1?cityName=Edo&cityCode=JP-EDO
     */
    @PutMapping("/{idCity}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCity(
            @PathVariable Long idCity,
            @RequestParam(required = false) String cityName,
            @RequestParam(required = false) String cityCode) {
        try {
            CityDto city = cityService.updateCity(idCity, cityName, cityCode);
            return ResponseEntity.ok(city);
        } catch (Exception e) {
            log.error("Error updating city {}: {}", idCity, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error updating city: " + e.getMessage()));
        }
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────────

    /**
     * DELETE /api/v0/city/{idCity}
     * Delete a city (ADMIN only).
     * WARNING: Products in this city will have their city reference set to NULL.
     */
    @DeleteMapping("/{idCity}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCity(@PathVariable Long idCity) {
        try {
            cityService.deleteCity(idCity);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting city {}: {}", idCity, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error deleting city: " + e.getMessage()));
        }
    }

    // ─── HELPER ───────────────────────────────────────────────────────────────────

    /**
     * Simple error response object.
     */
    public static class ErrorResponse {
        public String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
