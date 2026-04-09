# 🔍 NipponHub: Quick Reference Guide

**Keep this open while implementing fixes!**

---

## 📍 File Locations of Issues

### CRITICAL (Fix First)

| Issue | File | Line | Problem | Fix Ref |
|-------|------|------|---------|---------|
| Hardcoded JWT Secret | `JWTUtils.java` | 24 | String constant | Sec Analysis §1.1 |
| Empty MySQL Password | `application.properties` | 8 | No password set | Sec Analysis §1.2 |

### HIGH PRIORITY

| Issue | File | Problem | Fix Ref |
|-------|------|---------|---------|
| CORS Too Permissive | `CorsConfig.java` | `.allowedOrigins("*")` | Sec Analysis §2.1 |
| MongoDB Not Secured | `application.properties` | No credentials in URI | Sec Analysis §2.2 |
| No Password Validation | `UsersManagementService.java` | No complexity checks | Sec Analysis §2.3 |
| No Token Revocation | `JWTAuthFilter.java` | No blacklist check | Sec Analysis §2.4 |
| File Upload Weak | `FileStorageService.java` | MIME type only | Sec Analysis §2.5 |

---

## 🔐 Secrets to Generate

### JWT Secret (Required)
```bash
# Generate base64-encoded 32-byte secret:
openssl rand -base64 32

# Example output:
# aB3xYp9mL2nK8vQ5rT7wS4zU1vB2cX6yJ8zC9iD0eF1pG2qH3rI4sJ5tK6uL7vM=

# Store in:
- Render.com ENV: JWT_SECRET
- .env.prod: JWT_SECRET=<generated-value>
```

### Database Password (Required)
```bash
# Generate strong random password:
openssl rand -base64 24

# Example output:
# nF7pK2mQ9rJ3sT8vL4wX1yB5cD6eG7hI9kL2nM4oP5qR6sT7uV8wX9yZ0aB1cC=

# Store in:
- Render.com ENV: DB_PASSWORD
- .env.prod: DB_PASSWORD=<generated-value>
```

### MongoDB Connection String (If Using Atlas)
```bash
# Format:
mongodb+srv://username:password@cluster-name.mongodb.net/database?retryWrites=true&w=majority

# Example:
mongodb+srv://nipponhub_user:SecurePassword123@cluster0.abc123.mongodb.net/nipponhubfiles?retryWrites=true&w=majority

# Store in:
- Render.com ENV: MONGO_URI
- .env.prod: MONGO_URI=<connection-string>
```

---

## 🛠️ Quick Fix Commands

### 1. Extract JWT Secret from Code
```bash
# Run this to generate and note your secret:
openssl rand -base64 32 | tee jwt_secret.txt

# Then update JWTUtils.java to read from @Value annotation
```

### 2. Update application.properties for Secrets
```properties
# BEFORE (bad):
spring.datasource.password=

# AFTER (good):
spring.datasource.password=${DB_PASSWORD}
app.jwt.secret=${JWT_SECRET}
```

### 3. Fix CORS in One Minute
```bash
# Old code:
.allowedOrigins("*")

# New code:
.allowedOrigins("${app.cors.allowed-origins}")

# Then add to application.properties:
app.cors.allowed-origins=https://yourdomain.com,https://admin.yourdomain.com
```

### 4. Secure MongoDB
```bash
# Old URI:
mongodb://localhost:27017/nipponhubfiles

# New URI (with Auth):
mongodb+srv://user:password@cluster.mongodb.net/nipponhubfiles?retryWrites=true&w=majority
```

---

## 📋 Daily Standup Template

**Use this in team meetings:**

```
DAILY SECURITY FIX STANDUP
Date: [DATE]
Sprint: [1/2/3]

COMPLETED YESTERDAY:
☐ Item 1 - Owner: [Name]
☐ Item 2 - Owner: [Name]

IN PROGRESS TODAY:
☐ Item 3 - Owner: [Name] - ETA: [TIME]
☐ Item 4 - Owner: [Name] - ETA: [TIME]

BLOCKERS:
[ ] None / [ ] List below:
  - [Description]

NEXT 24 HOURS:
☐ Complete Item 3
☐ Start Item 5
☐ Code review PR #XXX

METRICS:
- Issues Fixed: X/10
- Lines of Code Changed: ~X
- Test Coverage: X%
- Security Score: X%
```

---

## 🚨 Troubleshooting Quick Fixes

### "No environment variable found for JWT_SECRET"

**Problem**: JWTUtils.java can't find `${JWT_SECRET}`

**Solution**:
```java
// In JWTUtils.java:
@Value("${app.jwt.secret:fallback-for-dev-only}")
private String secretString;

// Add to application.properties:
app.jwt.secret=${JWT_SECRET}

// Then set environment variable:
export JWT_SECRET="<your-generated-secret>"
```

### "CORS still allowing all origins"

**Problem**: .allowedOrigins("*") is still in code

**Solution**:
```bash
# Search for the problem:
grep -r "allowedOrigins.*\*" src/

# Fix all occurrences with the environment variable approach
```

### "MongoDB connection refused"

**Problem**: No authentication credentials

**Solution**:
```properties
# OLD (doesn't work):
spring.data.mongodb.uri=mongodb://localhost:27017/nipponhubfiles

# NEW (works):
spring.data.mongodb.uri=mongodb+srv://nipponhub_user:password@cluster.mongodb.net/nipponhubfiles?retryWrites=true
```

### "Password validation not working"

**Problem**: PasswordValidator class not imported or registered

**Solution**:
```java
// Ensure this is in UsersManagementService.java:
if (!PasswordValidator.isValid(reqres.getPassword())) {
    resp.setStatusCode(400);
    resp.setError(PasswordValidator.getErrorMessage());
    return resp;
}

// And run test:
assertEquals(false, PasswordValidator.isValid("123"));
assertEquals(true, PasswordValidator.isValid("MyP@ssw0rd2024"));
```

---

## ✅ Testing Each Fix

### Test JWT Secret Extraction
```bash
# After fixing JWTUtils.java:
java -jar target/nipponhub-0.0.1-SNAPSHOT.jar \
  --JWT_SECRET="test-secret-here"

# If it starts, JWT secret fix is working
```

### Test CORS Configuration
```bash
# Test with curl from different origin:
curl -H "Origin: https://evil.com" \
     -H "Access-Control-Request-Method: POST" \
     http://localhost:8080/auth/login -v

# Should NOT have "Access-Control-Allow-Origin: *" in response
```

### Test Password Validation
```bash
# Run integration test:
POST /auth/register
{
  "name": "Test User",
  "email": "test@example.com",
  "password": "weak123",  # This should fail
  "telephone": "+1234567890"
}

# Expected: 400 Bad Request
# Message: "Password must be at least 12 characters..."
```

### Test File Upload Validation
```bash
# Try uploading non-image file:
curl -F "file=@document.pdf" http://localhost:8080/file/upload

# Expected: 400 Bad Request
# Message: "Only image files are allowed"
```

---

## 📊 Progress Metrics

**Track your improvement:**

```
WEEK 1 METRICS:
- Critical Issues Fixed: 2/2 ✓
- High Priority Fixed: 5/5 ✓
- Test Coverage: 40% → 60%
- Security Score: 20% → 60%
- Lines of Code Changed: ~500

WEEK 2 METRICS:
- Medium Issues Fixed: 7/7 ✓
- Test Coverage: 60% → 80%
- Security Score: 60% → 85%
- Performance Score: 30% → 70%
- Lines of Code Changed: ~800

WEEK 3 METRICS:
- Deployment Ready: YES ✓
- Docker Image: Building ✓
- Tests Passing: 100% ✓
- Security Score: 85% → 95%
- Ready for Production: YES ✓
```

---

## 🏗️ Environment Variables Checklist

**Copy this and fill in values:**

```env
# ==================== CRITICAL ====================
JWT_SECRET=___________________________________
DB_PASSWORD=____________________________________

# ==================== DATABASE ====================
DB_URL=jdbc:mysql://localhost:3306/nipponhub_DB
DB_USERNAME=root
MONGO_URI=mongodb+srv://user:pass@cluster.mongodb.net/db

# ==================== SECURITY ====================
CORS_ALLOWED_ORIGINS=https://yourdomain.com
SPRING_PROFILES_ACTIVE=prod

# ==================== FILE UPLOAD ====================
SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=5MB
SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=25MB

# ==================== LOGGING ====================
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_APP=INFO

# ==================== SERVER ====================
SERVER_PORT=8080
SERVER_ERROR_INCLUDE_MESSAGE=never
```

---

## 🔗 Cross-Reference: Where to Find Things

| What You Need | See Document | Section |
|---------------|--------------|---------|
| Overall plan | IMPLEMENTATION_ROADMAP.md | - |
| Task checklist | QUICK_CHECKLIST.md | - |
| Code examples | SECURITY_AND_CODE_ANALYSIS.md | 1-5 |
| Detailed fixes | SECURITY_AND_CODE_ANALYSIS.md | 1-5 |
| Timeline | EXECUTIVE_SUMMARY.md | Implementation Path |
| Architecture | IMPLEMENTATION_ROADMAP.md | Deployment Strategy |

---

## 🎯 Success Indicators

**You'll know each fix is working when:**

| Fix | Success Indicator |
|-----|-------------------|
| JWT Secret | App starts with ENV variable |
| DB Password | MySQL connects with new password |
| CORS Fix | Only allowed origins get CORS headers |
| MongoDB Auth | GridFS operations authenticate |
| Password Validation | /auth/register rejects weak passwords |
| Token Revocation | /auth/logout works, token unusable |
| File Validation | Non-images rejected at upload |
| Input Validation | Invalid data rejected with 400 |
| Error Handler | No stack traces in responses |
| Rate Limiting | 6th login attempt in 1 min blocked |

---

## 📞 Help Resources

**Stuck?** Try these in order:

1. **First**: Search this Quick Reference (`Ctrl+F`)
2. **Second**: Read relevant section of SECURITY_AND_CODE_ANALYSIS.md
3. **Third**: Check code example provided
4. **Fourth**: Look at your test output/logs
5. **Fifth**: Ask team member or search Stack Overflow

---

## 🏁 Final Validation

**Before marking a fix as DONE:**

```checklist
☐ Code change committed to feature branch
☐ Tests written and passing
☐ No console errors during startup
☐ Feature works as expected locally
☐ Code review approved by peer
☐ Merged to develop branch
☐ Marked COMPLETE in QUICK_CHECKLIST.md
☐ Updated team in standup
```

---

**Bookmark this page for quick reference during implementation!**

Print → Post on wall → Reference during daily work

---

**Last Updated**: April 6, 2026  
**For**: NipponHub Development Team

