package com.nipponhub.nipponhubv0.Controllers;

import com.nipponhub.nipponhubv0.DTO.ProductDto;
import com.nipponhub.nipponhubv0.Models.City;
import com.nipponhub.nipponhubv0.Models.ProductActivity;
import com.nipponhub.nipponhubv0.Services.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v0/product")
@RequiredArgsConstructor  
public class ProductController {

    private final ProductService productServices;

    // ─── CREATE ──────────────────────────────────────────────────────────────────

    /**
     * POST /api/v0/product/newProduct
     *
     * FIX: Returns ResponseEntity<ProductDto> with HTTP 201 CREATED instead of raw DTO.
     * FIX: Added throws IOException — no longer silently swallowed.
     * FIX: Removed unnecessary null initialization of reqProductDto.
     * FIX: Added error handling for IllegalArgumentException (validation errors).
     */
    @PostMapping("/newProduct")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<?> newProduct(
        @RequestParam(required = false, name = "ProdName")    String ProdName,
        @RequestParam(required = false, name = "UnitPrice")   BigDecimal UnitPrice,
        @RequestParam(required = false, name = "SoldPrice")   BigDecimal SoldPrice,
        @RequestParam(required = false, name = "ProdQty")     Integer ProdQty,
        @RequestParam(required = false, name = "ProdUrl")     List<MultipartFile> ProdUrl,
        @RequestParam(required = false, name = "Country")     List<String> Country,
        @RequestParam(required = false, name = "category")    String category,
        @RequestParam(required = false, name = "prodDescription")    String prodDescription
    ) throws IOException {

        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            String role = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                        .iterator().next().getAuthority().replace("ROLE_", "");

            if (!role.equals("ADMIN") && !role.equals("OWNER")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied: insufficient permissions"));
            }

            ProductDto result = productServices.createProduct(
                ProdName, UnitPrice, SoldPrice, ProdQty, ProdUrl, Country, category, prodDescription, username
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────────

    /**
     * POST /api/v0/product/updateProduct/{idProd}
     *
     * FIX: Added throws IOException — previously swallowed file upload errors silently.
     * FIX: Removed unnecessary null initialization of reqProductDto.
     * FIX: Returns ResponseEntity with proper HTTP status.
     */
    @PutMapping("/updateProduct/{idProd}")
    public ResponseEntity<?> updateProduct(
        @PathVariable Long idProd,
        @RequestParam(required = false, name = "ProdName")    String ProdName,
        @RequestParam(required = false, name = "UnitPrice")   BigDecimal UnitPrice,
        @RequestParam(required = false, name = "SoldPrice")   BigDecimal SoldPrice,
        @RequestParam(required = false, name = "ProdQty")     Integer ProdQty,
        @RequestParam(required = false, name = "ProdUrl")     List<MultipartFile> ProdUrl,
        @RequestParam(required = false, name = "Country")     List<String> Country,
        @RequestParam(required = false, name = "category")    String category,
        @RequestParam(required = false, name = "prodDescription")    String prodDescription
    ) throws IOException {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        ProductDto result = productServices.updateProduct(
            idProd, ProdName, UnitPrice, SoldPrice, ProdQty, ProdUrl, Country, category, prodDescription, username
        );

        if (result.getMessage() != null) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
        }

        return ResponseEntity.ok(result);
    }

    // ─── READ ────────────────────────────────────────────────────────────────────

    /**
     * GET /api/v0/product/all
     * Returns all products.
     */
    @GetMapping("/all")
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        List<ProductDto> products = productServices.getAllProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * GET /api/v0/product/{idProd}
     * Returns a product by its MySQL id.
     */
    @GetMapping("/{idProd}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long idProd) {
        ProductDto product = productServices.getProductById(idProd);

        if (product.getMessage() != null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(product);
        }
        return ResponseEntity.ok(product);
    }

    /**
     * GET /api/v0/product/search?prodName=...
     * Returns a product by name (case-insensitive).
     */
    @GetMapping("/searchByName")
    public ResponseEntity<ProductDto> getProductByName(@RequestParam String prodName) {
        ProductDto product = productServices.getProductByName(prodName);

        if (product.getMessage() != null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(product);
        }
        return ResponseEntity.ok(product);
    }


    /**
     * NEW  GET /api/v0/product/searchByCategory?category=...
     * Visitors can filter the catalogue by category.
     */
    @GetMapping("/searchByCategory")
    public ResponseEntity<List<ProductDto>> searchByCategory(@RequestParam String category) {
        return ResponseEntity.ok(productServices.searchByCategory(category));
    }

    @GetMapping("/searchByCountry")
    public ResponseEntity<List<ProductDto>> getProductsByCountry(@RequestParam String country) {
        List<ProductDto> products = productServices.getProductByCountry(country);
        return ResponseEntity.ok(products);
    }

    /**
     * NEW — GET /api/v0/product/searchByCity?country=...&city=...
     * Filter products by city within a country (location-based filtering).
     * Combination of country and city uniquely identifies location.
     * Public endpoint — visitors can use.
     */
    @GetMapping("/searchByCity")
    public ResponseEntity<List<ProductDto>> getProductsByCity(
            @RequestParam String country,
            @RequestParam String city) {
        List<ProductDto> products = productServices.getProductByCity(city, country);
        return ResponseEntity.ok(products);
    }

    /**
     * NEW — GET /api/v0/product/cities?country=...
     * Fetch all cities available in a specific country.
     * Useful for populating city dropdown during product search.
     * Public endpoint — visitors can use.
     */
    @GetMapping("/cities")
    public ResponseEntity<List<City>> getCitiesByCountry(@RequestParam String country) {
        List<City> cities = productServices.getCitiesByCountry(country);
        return ResponseEntity.ok(cities);
    }

    /** 
          * NEW  DELETE /api/v0/product/{idProd}
     * Permanently deletes a product and its GridFS images.
     */
    @DeleteMapping("/{idProd}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long idProd) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String role = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                        .iterator().next().getAuthority().replace("ROLE_", "");
        productServices.deleteProduct(idProd, username, role);
        return ResponseEntity.noContent().build();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Audit trail  (ADMIN / OWNER)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * NEW  GET /api/v0/product/{idProd}/activity
     * Full activity log for one product (who created, updated, ordered, delivered…).
     */
    @GetMapping("/{idProd}/activity")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<ProductActivity>> getProductActivity(@PathVariable Long idProd) {
        return ResponseEntity.ok(productServices.getActivityForProduct(idProd));
    }

    /**
     * NEW  GET /api/v0/product/activity/user?email=...
     * All actions performed by a specific user across all products.
     */
    @GetMapping("/activity/user")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<ProductActivity>> getActivityByUser(@RequestParam String email) {
        return ResponseEntity.ok(productServices.getActivityByUser(email));
    }

    // ─── HEALTH CHECK ────────────────────────────────────────────────────────────

    @GetMapping("/greetings")
    public ResponseEntity<String> greeting() {
        return ResponseEntity.ok("Hello prince, I'm a visible EndPoint");
    }
}
