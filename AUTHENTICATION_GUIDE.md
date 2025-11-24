# OAuth2 + JWT Unified Authentication Guide

## Architecture Overview

This system combines OAuth2 social login with JWT token-based authentication for stateless API requests.

### Authentication Flows

```
┌─────────────────────────────────────────────────────────────────┐
│                    AUTHENTICATION FLOWS                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. OAuth2 Login (Google/Facebook/GitHub)                       │
│     ├─ User clicks "Login with Google"                          │
│     ├─ OAuth2 flow redirects to provider                        │
│     ├─ User authorizes                                           │
│     ├─ OAuth2UserService processes user info                    │
│     ├─ OAuth2LoginSuccessHandler generates JWT                  │
│     └─ JWT stored in HTTP-only cookie                           │
│                                                                  │
│  2. Basic Login (Username/Password) - API Based                 │
│     POST /api/v1/auth/authenticate                              │
│     Body: {"email": "user@example.com", "password": "pass"}     │
│     Response: {"accessToken": "jwt...", "refreshToken": "..."}  │
│                                                                  │
│  3. Subsequent API Requests                                     │
│     Authorization: Bearer <JWT_TOKEN>                           │
│     OR Cookie: access_token=<JWT_TOKEN>                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## API Endpoints

### 1. OAuth2 Login (Browser-based)
```
GET /login
GET /oauth2/authorization/google
GET /oauth2/authorization/facebook
GET /oauth2/authorization/github
```

### 2. Basic Authentication (API-based)

#### Register User
```bash
POST /api/v1/auth/register
Content-Type: application/json

{
  "firstname": "John",
  "lastname": "Doe",
  "email": "john@example.com",
  "password": "password123"
}

Response:
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc..."
}
```

#### Login
```bash
POST /api/v1/auth/authenticate
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "password123"
}

Response:
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc..."
}
```

#### Refresh Token
```bash
POST /api/v1/auth/refresh-token
Authorization: Bearer <REFRESH_TOKEN>

Response:
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc..."
}
```

### 3. Protected Endpoints

#### Using Authorization Header (Recommended for APIs)
```bash
GET /api/v1/users
Authorization: Bearer <ACCESS_TOKEN>
```

#### Using Cookie (Automatically sent by browser after OAuth2 login)
```bash
GET /user-info
Cookie: access_token=<JWT_TOKEN>
```

#### Token Info (Debug endpoint)
```bash
GET /api/token/info
Authorization: Bearer <ACCESS_TOKEN>

Response:
{
  "authenticated": true,
  "username": "john@example.com",
  "userId": "uuid...",
  "authorities": ["ROLE_USER"],
  "tokenInHeader": true,
  "tokenInCookie": false
}
```

## Testing the Flows

### Test OAuth2 Login
1. Open browser: http://localhost:8080/login
2. Click "Continue with Google/Facebook/GitHub"
3. After successful login, JWT is stored in cookie
4. Access protected pages automatically

### Test Basic Login (API)
```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstname": "Test",
    "lastname": "User",
    "email": "test@example.com",
    "password": "password123"
  }'

# Login
curl -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'

# Use the returned access_token
curl http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

### Test Token Info
```bash
curl http://localhost:8080/api/token/info \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

## Security Configuration

### Token Storage Options

1. **HTTP-Only Cookies (Used for OAuth2)**
   - Pros: XSS protection, browser handles automatically
   - Cons: CSRF concerns (use CSRF tokens in production)
   - Use case: Traditional web applications

2. **Authorization Header (Used for API)**
   - Pros: Stateless, no CSRF issues, mobile-friendly
   - Cons: Vulnerable to XSS if stored in localStorage
   - Use case: SPAs, Mobile apps, Microservices

### Token Lifetimes
- Access Token: 24 hours (86400000 ms)
- Refresh Token: 7 days (604800000 ms)

## Key Components

### 1. OAuth2LoginSuccessHandler
- Intercepts successful OAuth2 login
- Generates JWT tokens
- Stores in HTTP-only cookies
- Path: `handler/OAuth2LoginSuccessHandler.java`

### 2. JwtAuthenticationFilter
- Validates JWT from header OR cookie
- Sets security context
- Path: `filter/JwtAuthenticationFilter.java`

### 3. SecurityConfig
- Unified security configuration
- Combines OAuth2 + JWT
- Path: `config/SecurityConfig.java`

### 4. AuthenticationService
- Handles username/password authentication
- Generates JWT tokens
- Path: `service/AuthenticationService.java`

## Important Notes

1. **CSRF Protection**: Currently disabled for stateless JWT. Enable for cookie-based auth in production.

2. **HTTPS**: Set `cookie.setSecure(true)` in production with HTTPS.

3. **Token Revocation**: Tokens are stored in database and can be revoked via logout endpoint.

4. **Refresh Tokens**: Use refresh tokens to get new access tokens without re-authentication.

5. **CORS**: Configure CORS if frontend is on different domain.

## Production Checklist

- [ ] Enable CSRF protection for cookie-based auth
- [ ] Set secure cookies (HTTPS only)
- [ ] Configure proper CORS policies
- [ ] Use environment variables for secrets
- [ ] Implement rate limiting
- [ ] Add token blacklist for logout
- [ ] Set up monitoring for failed auth attempts
- [ ] Use shorter token expiration times
- [ ] Implement refresh token rotation
