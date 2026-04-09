#!/bin/bash
# ============================================================================
# API Testing Guide: Location-Based Product Filtering
# ============================================================================
# This file contains curl commands to test all location-based product 
# filtering endpoints. Run these after starting the NipponHub server.
#
# Prerequisites:
# 1. Spring Boot application running on http://localhost:8080
# 2. Valid JWT token for ADMIN operations
# 3. Sample data populated (cities and products)
# ============================================================================

# ─── AUTHENTICATION ──────────────────────────────────────────────────────
# Get JWT token (replace with actual credentials)
# This token will be used in subsequent authenticated requests

TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@nipponhub.com","password":"admin123"}' \
  | jq -r '.access_token')

echo "JWT Token: $TOKEN"

# ─── 1. CITY MANAGEMENT ENDPOINTS ────────────────────────────────────────

# 1.1 Create a new city (ADMIN only)
echo "=========================================="
echo "1.1 Create City: Tokyo"
echo "=========================================="
curl -X POST "http://localhost:8080/api/v0/city/create?cityName=Tokyo&cityCode=JP-TYO&countryName=Japan" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  | jq .

# 1.2 Get all cities (public)
echo "=========================================="
echo "1.2 Get All Cities"
echo "=========================================="
curl -X GET "http://localhost:8080/api/v0/city/all" \
  -H "Content-Type: application/json" \
  | jq .

# 1.3 Get city by ID (public)
echo "=========================================="
echo "1.3 Get City by ID (id=1)"
echo "=========================================="
curl -X GET "http://localhost:8080/api/v0/city/1" \
  -H "Content-Type: application/json" \
  | jq .

# 1.4 Search city by name (public)
echo "=========================================="
echo "1.4 Search City by Name (Tokyo, Japan)"
echo "=========================================="
curl -X GET "http://localhost:8080/api/v0/city/search?cityName=Tokyo&countryName=Japan" \
  -H "Content-Type: application/json" \
  | jq .

# 1.5 Get cities by country (public)
echo "=========================================="
echo "1.5 Get Cities in Japan"
echo "=========================================="
curl -X GET "http://localhost:8080/api/v0/city/byCountry?countryName=Japan" \
  -H "Content-Type: application/json" \
  | jq .

# 1.6 Update city (ADMIN only)
echo "=========================================="
echo "1.6 Update City (id=1, rename to Edo)"
echo "=========================================="
curl -X PUT "http://localhost:8080/api/v0/city/1?cityName=Edo&cityCode=JP-EDO" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  | jq .

# 1.7 Delete city (ADMIN only)
echo "=========================================="
echo "1.7 Delete City (id=999)"
echo "=========================================="
curl -X DELETE "http://localhost:8080/api/v0/city/999" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n"

# ─── 2. PRODUCT SEARCH BY LOCATION ──────────────────────────────────────

# 2.1 Get cities for country dropdown (public)
echo "=========================================="
echo "2.1 Get Cities in Japan (for dropdown)"
echo "=========================================="
curl -X GET "http://localhost:8080/api/v0/product/cities?country=Japan" \
  -H "Content-Type: application/json" \
  | jq .

# 2.2 Search products by city (public)
echo "=========================================="
echo "2.2 Search Products in Tokyo, Japan"
echo "=========================================="
curl -X GET "http://localhost:8080/api/v0/product/searchByCity?country=Japan&city=Tokyo" \
  -H "Content-Type: application/json" \
  | jq .

# 2.3 Search products by country (existing endpoint)
echo "=========================================="
echo "2.3 Search Products in Japan"
echo "=========================================="
curl -X GET "http://localhost:8080/api/v0/product/searchByCountry?country=Japan" \
  -H "Content-Type: application/json" \
  | jq .

# ─── 3. TESTING FILTERS & ERROR CASES ────────────────────────────────────

# 3.1 Test missing country parameter
echo "=========================================="
echo "3.1 Test: Missing country parameter"
echo "=========================================="
curl -X GET "http://localhost:8080/api/v0/product/searchByCity?city=Tokyo" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n"

# 3.2 Test invalid country
echo "=========================================="
echo "3.2 Test: Invalid country (NonExistent)"
echo "=========================================="
curl -X GET "http://localhost:8080/api/v0/product/searchByCity?country=NonExistent&city=Tokyo" \
  -H "Content-Type: application/json" \
  | jq .

# 3.3 Test city not found
echo "=========================================="
echo "3.3 Test: Valid country, invalid city"
echo "=========================================="
curl -X GET "http://localhost:8080/api/v0/product/searchByCity?country=Japan&city=NonExistentCity" \
  -H "Content-Type: application/json" \
  | jq .

# 3.4 Test unauthorized city creation (non-ADMIN)
echo "=========================================="
echo "3.4 Test: Unauthorized city creation (non-ADMIN)"
echo "=========================================="
# Assume we have a USER token (not ADMIN)
curl -X POST "http://localhost:8080/api/v0/city/create?cityName=Test&cityCode=TST&countryName=Japan" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n"

# ─── 4. PERFORMANCE & DATA VERIFICATION ─────────────────────────────────

# 4.1 Get all cities count
echo "=========================================="
echo "4.1 Count Cities in Database"
echo "=========================================="
curl -X GET "http://localhost:8080/api/v0/city/all" \
  -H "Content-Type: application/json" \
  | jq 'length'

# 4.2 Get all products count (should be higher than city products)
echo "=========================================="
echo "4.2 Count Products in Tokyo"
echo "=========================================="
curl -X GET "http://localhost:8080/api/v0/product/searchByCity?country=Japan&city=Tokyo" \
  -H "Content-Type: application/json" \
  | jq 'length'

# 4.3 Get product details to verify city assignment
echo "=========================================="
echo "4.3 Get Product #1 Details"
echo "=========================================="
curl -X GET "http://localhost:8080/api/v0/product/1" \
  -H "Content-Type: application/json" \
  | jq '.city'

# ─── 5. INTEGRATION TEST SCENARIO ───────────────────────────────────────

# Complete workflow: User searching for products in a location
echo "=========================================="
echo "5.1 USER: Select country (Japan)"
echo "=========================================="
CITIES=$(curl -s -X GET "http://localhost:8080/api/v0/product/cities?country=Japan" \
  -H "Content-Type: application/json")
echo "$CITIES" | jq '.[] | {cityName, cityCode}'

echo ""
echo "=========================================="
echo "5.2 USER: Select city (first city from above)"
echo "=========================================="
FIRST_CITY=$(echo "$CITIES" | jq -r '.[0].cityName')
echo "Selected city: $FIRST_CITY"

echo ""
echo "=========================================="
echo "5.3 USER: Search products in selected city"
echo "=========================================="
curl -X GET "http://localhost:8080/api/v0/product/searchByCity?country=Japan&city=$FIRST_CITY" \
  -H "Content-Type: application/json" \
  | jq '.[] | {idProd, prodName, unitPrice, soldPrice}'

# ─── 6. API RESPONSE EXAMPLES ────────────────────────────────────────────

# Examples of successful responses

echo "=========================================="
echo "6.1 Sample City Response"
echo "=========================================="
cat << 'EOF'
{
  "idCity": 1,
  "cityName": "Tokyo",
  "cityCode": "JP-TYO",
  "countryId": 1,
  "countryName": "Japan",
  "createdAt": "2026-04-09T10:00:00"
}
EOF

echo ""
echo "=========================================="
echo "6.2 Sample Product Response"
echo "=========================================="
cat << 'EOF'
{
  "idProd": 1,
  "prodName": "Anime Figure Set",
  "unitPrice": 29.99,
  "soldPrice": 34.99,
  "prodQty": 50,
  "prodDescription": "Limited edition anime figures",
  "categoryName": "Collectibles",
  "countries": ["Japan"],
  "createdAt": "2026-04-09T10:00:00",
  "prodUrl": ["gridfs-id-1", "gridfs-id-2"]
}
EOF

echo ""
echo "=========================================="
echo "6.3 Sample Error Response"
echo "=========================================="
cat << 'EOF'
{
  "message": "City not found: NonExistentCity in Japan"
}
EOF

# ─── 7. SHELL FUNCTION FOR REUSABLE COMMANDS ───────────────────────────

# Function to search products by city
search_products_by_city() {
  local country=$1
  local city=$2
  curl -X GET "http://localhost:8080/api/v0/product/searchByCity?country=$country&city=$city" \
    -H "Content-Type: application/json" | jq .
}

# Function to get cities in country
get_cities_in_country() {
  local country=$1
  curl -X GET "http://localhost:8080/api/v0/city/byCountry?countryName=$country" \
    -H "Content-Type: application/json" | jq .
}

# Function to create city
create_city() {
  local city=$1
  local code=$2
  local country=$3
  local token=$4
  curl -X POST "http://localhost:8080/api/v0/city/create?cityName=$city&cityCode=$code&countryName=$country" \
    -H "Authorization: Bearer $token" \
    -H "Content-Type: application/json" | jq .
}

# Example usage of functions:
# search_products_by_city "Japan" "Tokyo"
# get_cities_in_country "Japan"
# create_city "Kyoto" "JP-KYO" "Japan" "$TOKEN"

# ═══════════════════════════════════════════════════════════════════════════
# TESTING CHECKLIST
# ═══════════════════════════════════════════════════════════════════════════
# [ ] City creation works (ADMIN only)
# [ ] City retrieval works (public)
# [ ] City search by name works
# [ ] Get cities by country works
# [ ] City update works (ADMIN only)
# [ ] City deletion works (ADMIN only)
# [ ] Product search by city works
# [ ] Product search by city+country works
# [ ] City dropdown population works
# [ ] Error handling for invalid inputs
# [ ] Error handling for unauthorized access
# [ ] No N+1 query problems (check logs)
# [ ] JSON responses are properly formatted
# [ ] City codes work with special characters
# [ ] Multiple cities per country works
# [ ] No circular reference issues in JSON
# ═══════════════════════════════════════════════════════════════════════════
