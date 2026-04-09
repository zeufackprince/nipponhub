# Location-Based Product Filtering Implementation

## Overview
This document describes the Country → City → Products location hierarchy system implemented in NipponHub for geographic-based product filtering.

## Architecture

### Data Model Hierarchy
```
Country (1)
  ├── Cities (Many)
  │   ├── Products (Many per City)
  │   └── Product Details
  └── Products (via @ManyToMany)
```

### Entity Relationships

#### Country ↔ City (1:Many)
```
Country
  @OneToMany(mappedBy = "country", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  List<City> cities

City
  @ManyToOne(fetch = FetchType.EAGER)
  Country country
```

#### Product ↔ City (Many:1)
```
Product
  @ManyToOne(fetch = FetchType.LAZY)
  City city

City
  @OneToMany(mappedBy = "city", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  List<Product> products
```

#### Country ↔ Product (Many:Many)
```
Product (maintains join table)
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "product_country", ...)
  List<Country> countries

Country
  @ManyToMany(mappedBy = "countries")
  List<Product> products
```

## Database Schema

### City Table
```sql
CREATE TABLE city (
    id_city BIGINT PRIMARY KEY AUTO_INCREMENT,
    city_name VARCHAR(255) NOT NULL,
    city_code VARCHAR(20) NOT NULL,
    country_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (country_id) REFERENCES country(id_country),
    INDEX idx_city_country (country_id),
    INDEX idx_city_name (city_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Product Table Update
```sql
ALTER TABLE product ADD COLUMN city_id BIGINT AFTER country_id;
ALTER TABLE product ADD FOREIGN KEY (city_id) REFERENCES city(id_city);
```

## API Endpoints

### 1. Product Search Endpoints

#### Search Products by City
```
GET /api/v0/product/searchByCity?country=Japan&city=Tokyo
```

**Description**: Filter products available in a specific city within a country.

**Parameters**:
- `country` (required): Country name
- `city` (required): City name

**Response**:
```json
[
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
]
```

#### Get Cities in Country
```
GET /api/v0/product/cities?country=Japan
```

**Description**: Fetch all cities available in a country (for dropdown population).

**Response**:
```json
[
  {
    "idCity": 1,
    "cityName": "Tokyo",
    "cityCode": "JP-TYO",
    "countryId": 1,
    "countryName": "Japan",
    "createdAt": "2026-04-09T10:00:00"
  },
  {
    "idCity": 2,
    "cityName": "Osaka",
    "cityCode": "JP-OSA",
    "countryId": 1,
    "countryName": "Japan",
    "createdAt": "2026-04-09T10:00:00"
  }
]
```

### 2. City Management Endpoints

#### Create City (ADMIN only)
```
POST /api/v0/city/create?cityName=Tokyo&cityCode=JP-TYO&countryName=Japan
```

**Response**:
```json
{
  "idCity": 1,
  "cityName": "Tokyo",
  "cityCode": "JP-TYO",
  "countryId": 1,
  "countryName": "Japan",
  "createdAt": "2026-04-09T10:00:00"
}
```

#### Get All Cities
```
GET /api/v0/city/all
```

**Response**: List of all cities with country info.

#### Get Specific City
```
GET /api/v0/city/1
```

**Response**: Single city object.

#### Search City by Name
```
GET /api/v0/city/search?cityName=Tokyo&countryName=Japan
```

**Response**: Single city object.

#### Get Cities by Country
```
GET /api/v0/city/byCountry?countryName=Japan
```

**Response**: List of cities in the country.

#### Update City (ADMIN only)
```
PUT /api/v0/city/1?cityName=Edo&cityCode=JP-EDO
```

**Parameters**:
- `cityName` (optional): New city name
- `cityCode` (optional): New city code

**Response**: Updated city object.

#### Delete City (ADMIN only)
```
DELETE /api/v0/city/1
```

**Response**: 204 No Content

## Service Layer

### ProductService Methods

#### getProductByCity(String cityName, String countryName): List<ProductDto>
- Returns all products available in a specific city
- Used by searchByCity endpoint
- Includes eager loading of product relations

#### getCitiesByCountry(String countryName): List<City>
- Returns all cities in a country
- Used for dropdown population
- Useful for UI city selection

#### getCity(String cityName, String countryName): Optional<City>
- Get specific city information
- Returns empty Optional if not found

### CityService Methods

#### createCity(String cityName, String cityCode, String countryName): CityDto
- Creates new city within a country
- Validates inputs (name, code, country existence)
- Prevents duplicate cities in same country

#### getCitiesByCountry(String countryName): List<CityDto>
- Returns all cities in country
- Maps entity to DTO to prevent circular refs

#### getCity(String cityName, String countryName): Optional<CityDto>
- Single city lookup
- Returns mapped DTO

#### getCityById(Long idCity): Optional<CityDto>
- Get city by primary key

#### getAllCities(): List<CityDto>
- Returns all cities system-wide

#### updateCity(Long idCity, String cityName, String cityCode): CityDto
- Partial update (name and/or code)

#### deleteCity(Long idCity): void
- Delete city (ADMIN operation)
- Products in deleted city have city_id set to NULL (due to LAZY foreign key)

## Repository Layer

### CityRepository Methods

- `findByCountry(Country)` - Get all cities in country
- `findByCityNameAndCountry(String, Country)` - Get city by name + country entity
- `findByCountryName(String)` - Get cities by country name (JPQL)
- `findByCityNameAndCountryName(String, String)` - Get city by names (JPQL)
- `findAll()` - Get all cities
- `findByCityCode(String)` - Get city by code

### ProductRepository Methods (New)

- `findByCity(City)` - Products in city (FETCH JOIN)
- `findByCityAndCountry(String, String)` - Products by city + country names (FETCH JOIN)
- `findByCityCountry(Country)` - All city products in country (FETCH JOIN DISTINCT)
- `findCitiesByProductId(Long)` - Cities available for product

## Usage Workflow

### Client-Side Flow

1. **User Selects Country**:
   ```javascript
   GET /api/v0/product/cities?country=Japan
   // Response: List of cities in Japan
   ```

2. **UI Populates City Dropdown** with city names/codes

3. **User Selects City**:
   ```javascript
   GET /api/v0/product/searchByCity?country=Japan&city=Tokyo
   // Response: Products available in Tokyo, Japan
   ```

4. **UI Displays Products** by location

### Server-Side Flow

1. Request validation (country + city parameters)
2. Query cities by country name: `CityRepository.findByCountryName()`
3. Query products by city: `ProductRepository.findByCityAndCountry()`
4. Eager load product relations (category, franchises, countries)
5. Map to DTO (prevents circular JSON serialization)
6. Return response to client

## Error Handling

### Validation Errors
- 400 BAD_REQUEST: Invalid/missing parameters
- 400 BAD_REQUEST: City already exists in country

### Not Found Errors
- 404 NOT_FOUND: Country doesn't exist
- 404 NOT_FOUND: City not found
- 404 NOT_FOUND: No products in city

### Authorization Errors
- 403 FORBIDDEN: Non-ADMIN users cannot create/update/delete cities
- 401 UNAUTHORIZED: User not authenticated

### Server Errors
- 500 INTERNAL_SERVER_ERROR: Database operation failed
- Detailed error messages logged to console/logs

## Performance Considerations

### Indexes
- `idx_city_country` on `city(country_id)` - Fast country lookups
- `idx_city_name` on `city(city_name)` - Fast city name searches

### Fetch Strategies
- City → Country: **EAGER** (small set, always needed)
- City → Products: **LAZY** (large set, fetched on demand)
- Product → City: **LAZY** (not always needed)
- Product → Country: **LAZY** (explicit use case)

### Query Optimization
- Use FETCH JOIN for product queries to avoid N+1 problems
- Eager load product relations in single query
- DISTINCT on Many-to-Many joins to prevent duplicates

## Sample Data Insertion

### Create Cities for Japan
```sql
INSERT INTO city (city_name, city_code, country_id) VALUES
  ('Tokyo', 'JP-TYO', 1),
  ('Osaka', 'JP-OSA', 1),
  ('Kyoto', 'JP-KYO', 1),
  ('Hiroshima', 'JP-HRO', 1),
  ('Sapporo', 'JP-SAP', 1);

-- Assign products to cities
UPDATE product SET city_id = 1 WHERE idProd IN (1, 3, 5, 7, 9);    -- Tokyo
UPDATE product SET city_id = 2 WHERE idProd IN (2, 4, 6, 8, 10);   -- Osaka
-- etc.
```

### Populate All Countries with Cities
```bash
# For each country, create representative cities
PUT /api/v0/city/create?cityName=London&cityCode=GB-LON&countryName=United%20Kingdom
PUT /api/v0/city/create?cityName=Paris&cityCode=FR-PAR&countryName=France
# ... continue for all 10 countries
```

## Testing Checklist

- [ ] Create city endpoint works (POST)
- [ ] Get all cities endpoint (GET /api/v0/city/all)
- [ ] Get cities by country (GET /api/v0/city/byCountry)
- [ ] Search products by city (GET /api/v0/product/searchByCity)
- [ ] Get cities for product dropdown (GET /api/v0/product/cities)
- [ ] Update city endpoint (PUT /api/v0/city/{id})
- [ ] Delete city endpoint (DELETE /api/v0/city/{id})
- [ ] Verify products correctly mapped to cities
- [ ] Verify no N+1 query problems
- [ ] Verify FETCH JOIN reduces SQL queries
- [ ] Test with missing/invalid parameters
- [ ] Test ADMIN vs non-ADMIN access
- [ ] Test circular reference prevention in JSON

## Next Steps

1. **Populate Sample Data**:
   - Create cities for each of the 10 countries
   - Assign existing products to cities
   - Test filtering works correctly

2. **Update ProductActivity**:
   - Track city-level operations (CITY_ADDED, CITY_REMOVED)
   - Include city info in audit trail

3. **Frontend Integration**:
   - Add country dropdown
   - Add cascading city dropdown
   - Add product search by location
   - Display city info on product detail page

4. **Analytics**:
   - Track products popular by city
   - Regional sales insights
   - City-based inventory management

## Files Modified/Created

### Created
- `Models/City.java` - City entity
- `DTO/CityDto.java` - City data transfer object
- `Mappers/CityMapper.java` - City mapper
- `Repositories/mysql/CityRepository.java` - City repository
- `Services/CityService.java` - City business logic
- `Controllers/CityController.java` - City REST API

### Updated
- `Models/Country.java` - Added cities relationship
- `Models/Product.java` - Added city relationship
- `Repositories/mysql/ProductRepository.java` - Added city-based queries
- `Services/ProductService.java` - Added city filtering methods
- `Controllers/ProductController.java` - Added city search endpoints

## Database Migration

When deploying to production:

1. Hibernate will auto-create `city` table (ddl-auto=update)
2. Add `city_id` column to `product` table
3. Optionally populate sample cities
4. Update existing products to assign cities

```sql
-- If manual schema update needed:
CREATE TABLE IF NOT EXISTS city (
    id_city BIGINT AUTO_INCREMENT PRIMARY KEY,
    city_name VARCHAR(255) NOT NULL,
    city_code VARCHAR(20) NOT NULL,
    country_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (country_id) REFERENCES country(id_country),
    INDEX idx_city_country (country_id),
    INDEX idx_city_name (city_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```
