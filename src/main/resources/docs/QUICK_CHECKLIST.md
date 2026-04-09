# ⚡ NipponHub: Priority Checklist (Print This!)

## 🔴 CRITICAL FIXES (DO FIRST - 1 Hour)

### ☐ Fix #1: Hardcoded JWT Secret
**File**: `JWTUtils.java`
**Status**: 🔴 BLOCKS DEPLOYMENT
```
Current: String secret = "843567893696976453275974432697R...";
Fix: Use ${JWT_SECRET} from environment
Timeline: 30 minutes
Difficulty: Easy
```

### ☐ Fix #2: Empty MySQL Password
**File**: `application.properties`
**Status**: 🔴 BLOCKS DEPLOYMENT
```
Current: spring.datasource.password=
Fix: Use ${DB_PASSWORD} from environment
Timeline: 30 minutes
Difficulty: Easy
```

---

## 🟠 HIGH PRIORITY (1-2 Week Timeline)

### ☐ Issue #1: CORS Security
**File**: `CorsConfig.java`
- Current: `.allowedOrigins("*")`
- Fix: `.allowedOrigins("${app.cors.allowed-origins}")`
- Timeline: 45 minutes
- Difficulty: Easy

### ☐ Issue #2: MongoDB Authentication
**File**: `application.properties`
- Current: `mongodb://localhost:27017/nipponhubfiles`
- Fix: Add credentials to URI
- Timeline: 30 minutes
- Difficulty: Easy

### ☐ Issue #3: Password Complexity
**File**: `UsersManagementService.java`
- Current: No validation
- Fix: Add PasswordValidator (see analysis document)
- Timeline: 1 hour
- Difficulty: Easy

### ☐ Issue #4: Token Revocation (Logout)
**File**: `JWTAuthFilter.java`
- Current: No blacklist mechanism
- Fix: Add TokenBlacklist table + repository
- Timeline: 2 hours
- Difficulty: Medium

### ☐ Issue #5: File Upload Security
**File**: `FileStorageService.java`
- Current: Only MIME type check (spoofable)
- Fix: Add magic bytes + size validation
- Timeline: 1.5 hours
- Difficulty: Medium

---

## 🟡 MEDIUM PRIORITY (2-3 Week Timeline)

### ☐ Input Validation (JSR-303)
- Add @NotBlank, @Email, @Size to DTOs
- Timeline: 2 hours
- Difficulty: Easy

### ☐ Global Exception Handler
- Create @RestControllerAdvice class
- Timeline: 1 hour
- Difficulty: Easy

### ☐ Rate Limiting
- Add bucket4j library
- Limit login endpoints
- Timeline: 1.5 hours
- Difficulty: Medium

### ☐ Connection Pooling
- Configure HikariCP in application.properties
- Timeline: 30 minutes
- Difficulty: Easy

### ☐ Structured Logging
- Configure SLF4J + JSON output
- Timeline: 1 hour
- Difficulty: Easy

### ☐ API Pagination
- Add Pageable to GET endpoints
- Timeline: 1 hour
- Difficulty: Easy

### ☐ Database Indexes
- Add @Index annotations to models
- Timeline: 1 hour
- Difficulty: Easy

---

## 🚀 DEPLOYMENT PREPARATION

### ☐ Create Dockerfile
```dockerfile
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```
Timeline: 30 minutes

### ☐ Create docker-compose.yml
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - JWT_SECRET=${JWT_SECRET}
      - DB_PASSWORD=${DB_PASSWORD}
  mysql:
    image: mysql:8
    environment:
      - MYSQL_ROOT_PASSWORD=${DB_PASSWORD}
  mongodb:
    image: mongo:7
    environment:
      - MONGO_INITDB_ROOT_USERNAME=${MONGO_USER}
      - MONGO_INITDB_ROOT_PASSWORD=${MONGO_PASSWORD}
```
Timeline: 30 minutes

### ☐ Create .env.prod
```env
JWT_SECRET=<base64-32-byte-key>
DB_PASSWORD=<strong-16-char-password>
DB_USERNAME=nipponhub_user
CORS_ALLOWED_ORIGINS=https://yourdomain.com
MONGO_URI=mongodb+srv://user:pass@cluster.mongodb.net/db
```
Timeline: 30 minutes

### ☐ Set up Health Check Endpoint
```java
@GetMapping("/health")
public ResponseEntity<?> health() {
    return ResponseEntity.ok(Map.of(
        "status", "UP",
        "version", "1.0.0",
        "timestamp", new Date()
    ));
}
```
Timeline: 15 minutes

### ☐ Enable Spring Boot Actuator
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```
Timeline: 15 minutes

### ☐ Set up Database Migrations (Flyway)
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```
Timeline: 1 hour

### ☐ Configure Render.com Environment Variables
```
JWT_SECRET = <generated-value>
DB_PASSWORD = <generated-value>
CORS_ALLOWED_ORIGINS = https://yourdomain.com
MONGO_URI = <mongodb-atlas-connection-string>
SPRING_PROFILES_ACTIVE = prod
```
Timeline: 30 minutes

---

## 📊 Progress Tracking

| Phase | Checklist | Assigned To | Due Date | Status |
|-------|-----------|-------------|----------|--------|
| Critical | Fix #1, #2 | DevOps | Week 1 | ☐ |
| High-1 | Issues #1-3 | Backend | Week 1 | ☐ |
| High-2 | Issues #4-5 | Backend | Week 2 | ☐ |
| Medium | 7 improvements | Backend | Week 2-3 | ☐ |
| Deploy | Docker + env | DevOps | Week 3 | ☐ |
| Launch | Final testing | QA | Week 3 | ☐ |

---

## 🎯 Success Criteria

- [ ] All 2 critical issues fixed
- [ ] All 5 high-priority issues fixed  
- [ ] At least 5 medium issues fixed
- [ ] Dockerfile created & tested
- [ ] All secrets in environment (not code)
- [ ] Health check endpoint working
- [ ] Local docker-compose runs successfully
- [ ] Database migrations working
- [ ] Render.com env variables configured
- [ ] Successfully deployed to Render.com

---

## 📞 Quick Reference

**Full analysis**: See `SECURITY_AND_CODE_ANALYSIS.md`  
**Implementation roadmap**: See `IMPLEMENTATION_ROADMAP.md`  
**Code snippets**: All provided in main analysis document

**Timeline**: 
- Critical fixes: 1-2 hours
- High priority: 1 week  
- Medium + deployment: 2-3 weeks
- **Total to launch**: 3-4 weeks

---

## ⚠️ DEPLOYMENT BLOCKERS (FIX BEFORE LAUNCHING)

### CRITICAL - Blocks everything:
- [ ] JWT secret extracted
- [ ] DB password secured  
- [ ] CORS configured correctly

### HIGH - Blocks production:
- [ ] MongoDB authenticated
- [ ] Password validation added
- [ ] Token revocation implemented
- [ ] File uploads secured

### Required for live:
- [ ] Dockerfile working
- [ ] Environment variables set
- [ ] Database migrations tested
- [ ] Health check endpoint live
- [ ] Render.com configured

---

## 🚨 DO NOT DEPLOY UNTIL:

1. ✅ All 2 CRITICAL issues are fixed
2. ✅ All 5 HIGH issues are fixed
3. ✅ Secrets are environment variables
4. ✅ CORS is restricted to your domain
5. ✅ File uploads are validated
6. ✅ Docker image builds successfully
7. ✅ All tests pass locally
8. ✅ Staging environment tested

---

**Last Updated**: April 6, 2026  
**Print this page** for quick team reference

