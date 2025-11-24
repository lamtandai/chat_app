# Unified Authentication System - Complete Guide

## 🎯 System Overview

This system provides a unified authentication approach supporting **BOTH**:
1. **Basic Authentication** (Username/Password via API)
2. **OAuth2 Social Login** (Google, Facebook, GitHub)

**After authentication via either method**, the server generates JWT tokens and stores them in HTTP-only cookies for all subsequent requests.

## 🔄 Complete Authentication Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      UNIFIED AUTHENTICATION FLOW                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Method 1: Basic Authentication (Username/Password)                     │
│  ┌──────────────────────────────────────────────────────────┐          │
│  │ 1. User fills login form on /login page                   │          │
│  │ 2. JavaScript sends POST to /api/v1/auth/authenticate     │          │
│  │ 3. Server validates credentials                            │          │
│  │ 4. Server generates JWT access + refresh tokens           │          │
│  │ 5. Server sets tokens in HTTP-only cookies                │          │
│  │ 6. Server returns tokens in response body                 │          │
│  │ 7. JavaScript redirects to dashboard                      │          │
│  └──────────────────────────────────────────────────────────┘          │
│                                                                          │
│  Method 2: OAuth2 Social Login                                          │
│  ┌──────────────────────────────────────────────────────────┐          │
│  │ 1. User clicks "Login with Google/Facebook/GitHub"        │          │
│  │ 2. Redirects to OAuth2 provider                           │          │
│  │ 3. User authorizes the application                        │          │
│  │ 4. OAuth2 callback to /login/oauth2/code/{provider}       │          │
│  │ 5. OAuth2UserService processes user info                  │          │
│  │ 6. OAuth2LoginSuccessHandler generates JWT tokens         │          │
│  │ 7. Server sets tokens in HTTP-only cookies                │          │
│  │ 8. Server redirects to dashboard                          │          │
│  └──────────────────────────────────────────────────────────┘          │
│                                                                          │
│  Subsequent Requests (Both Methods)                                     │
│  ┌──────────────────────────────────────────────────────────┐          │
│  │ 1. Browser automatically sends cookies with requests      │          │
│  │ 2. JwtAuthenticationFilter extracts token from cookie     │          │
│  │ 3. Server validates JWT token                             │          │
│  │ 4. If expired: Check refresh token                        │          │
│  │    - If refresh token valid: Generate new access token    │          │
│  │    - Update cookie with new access token                  │          │
│  │ 5. Set authentication in SecurityContext                  │          │
│  │ 6. Process request                                        │          │
│  └──────────────────────────────────────────────────────────┘          │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## 📝 Updated login.html Features

The unified `/login` page now supports:

### ✅ Basic Authentication Form
- Email and password fields
- Registration form (toggle between sign in/sign up)
- Client-side validation
- Loading states
- Error/success messages
- Automatic token storage in cookies
- Auto-redirect to dashboard after success

### ✅ OAuth2 Social Login Buttons
- Google OAuth2
- Facebook OAuth2
- GitHub OAuth2
- One-click authentication
- Automatic JWT generation
- Cookie storage handled by server

## 🔐 Token Management

### Token Storage (Cookies)
All tokens are stored in **HTTP-only cookies** for security:

```javascript
// Access Token Cookie
Name: access_token
HttpOnly: true
Secure: false (true in production)
Path: /
Max-Age: 86400 seconds (24 hours)

// Refresh Token Cookie
Name: refresh_token
HttpOnly: true
Secure: false (true in production)
Path: /
Max-Age: 604800 seconds (7 days)
```

### Automatic Token Refresh
The system automatically handles token refresh:

1. **Request comes in** with expired access token
2. **JwtAuthenticationFilter** catches expiration exception
3. **Checks refresh token** from cookie
4. **If refresh token valid**:
   - Generates new access token
   - Updates cookie with new token
   - Continues with request
5. **If refresh token invalid**:
   - Redirects to login

## 🧪 Testing the System

### Test Basic Login

1. **Open browser**: `http://localhost:8080/login`

2. **Register new user**:
   - Click "Sign Up"
   - Fill in: First Name, Last Name, Email, Password
   - Click "Sign Up" button
   - Should redirect to dashboard with tokens in cookies

3. **Login existing user**:
   - Enter email and password
   - Click "Sign In" button
   - Should redirect to dashboard with tokens in cookies

### Test OAuth2 Login

1. **Open browser**: `http://localhost:8080/login`

2. **Click any social login button**:
   - "Continue with Google"
   - "Continue with Facebook"
   - "Continue with GitHub"

3. **Authorize on provider's page**

4. **Redirected back** with JWT tokens in cookies

### Test Protected Endpoints

After login (either method), test protected endpoints:

```bash
# Get user info (cookies sent automatically by browser)
curl http://localhost:8080/user-info \
  --cookie "access_token=YOUR_TOKEN"

# Get token info
curl http://localhost:8080/api/token/info \
  --cookie "access_token=YOUR_TOKEN"

# Access dashboard
Open: http://localhost:8080/
```

### Test Token Refresh

1. Wait for access token to expire (24 hours) OR manually expire it
2. Make a request to any protected endpoint
3. System should automatically:
   - Detect expired access token
   - Use refresh token to generate new access token
   - Update cookie
   - Continue with request

## 🔧 Key Components

### 1. **login.html**
- **Location**: `src/main/resources/static/login.html`
- **Features**:
  - Unified form for basic auth + OAuth2
  - Client-side form validation
  - JWT token storage in cookies
  - Auto-redirect after login

### 2. **AuthenticationController**
- **Location**: `controller/AuthenticationController.java`
- **Updates**:
  - `/register` endpoint sets JWT in cookies
  - `/authenticate` endpoint sets JWT in cookies
  - Returns tokens in both response body AND cookies

### 3. **OAuth2LoginSuccessHandler**
- **Location**: `handler/OAuth2LoginSuccessHandler.java`
- **Function**:
  - Intercepts successful OAuth2 login
  - Generates JWT tokens
  - Sets tokens in HTTP-only cookies
  - Redirects to dashboard

### 4. **JwtAuthenticationFilter**
- **Location**: `filter/JwtAuthenticationFilter.java`
- **Updates**:
  - Reads JWT from cookies (not just header)
  - Automatically refreshes expired tokens
  - Uses refresh token from cookies
  - Updates cookie with new access token

### 5. **SecurityConfig**
- **Location**: `config/SecurityConfig.java`
- **Configuration**:
  - Unified filter chain for OAuth2 + JWT
  - Stateless session management
  - Whitelisted `/login` and `/api/v1/auth/**`

## 📊 API Endpoints

### Public Endpoints (No Auth Required)

```
GET  /login                     - Login page (HTML)
POST /api/v1/auth/register      - Register new user
POST /api/v1/auth/authenticate  - Login with email/password
GET  /oauth2/authorization/*    - OAuth2 redirect endpoints
```

### Protected Endpoints (JWT Required)

```
GET  /                          - Dashboard
GET  /user-info                 - Get current user info
GET  /api/token/info            - Debug token information
GET  /api/v1/users              - User management
GET  /api/v1/admin/**           - Admin endpoints
POST /api/v1/auth/logout        - Logout and invalidate tokens
```

## 🔄 Request Flow Example

### Basic Login Flow
```javascript
// 1. User submits login form
POST /api/v1/auth/authenticate
Content-Type: application/json
{
  "email": "user@example.com",
  "password": "password123"
}

// 2. Server Response
Status: 200 OK
Set-Cookie: access_token=eyJhbGc...; HttpOnly; Path=/; Max-Age=86400
Set-Cookie: refresh_token=eyJhbGc...; HttpOnly; Path=/; Max-Age=604800
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc..."
}

// 3. JavaScript stores tokens and redirects
document.cookie = `access_token=${data.accessToken}; ...`;
window.location.href = '/';
```

### Subsequent Request with Cookie
```javascript
// Browser automatically sends cookies
GET /user-info
Cookie: access_token=eyJhbGc...; refresh_token=eyJhbGc...

// Server automatically:
// 1. Extracts token from cookie
// 2. Validates token
// 3. Sets authentication context
// 4. Returns user data
```

### Auto Refresh Flow
```javascript
// 1. Request with expired access token
GET /user-info
Cookie: access_token=EXPIRED_TOKEN; refresh_token=VALID_REFRESH_TOKEN

// 2. JwtAuthenticationFilter detects expiration
// 3. Checks refresh token
// 4. Generates new access token
// 5. Updates cookie
Set-Cookie: access_token=NEW_TOKEN; HttpOnly; Path=/; Max-Age=86400

// 6. Continues with request
Status: 200 OK
{ "user": "..." }
```

## 🛡️ Security Features

### ✅ HTTP-Only Cookies
- Prevents XSS attacks (JavaScript cannot access tokens)
- Automatically sent by browser
- Secure flag in production (HTTPS only)

### ✅ Automatic Token Refresh
- No need for manual refresh calls
- Seamless user experience
- Refresh token rotation (optional)

### ✅ Token Revocation
- Tokens stored in database
- Can be revoked via logout
- Checked on every request

### ✅ CSRF Protection
- Currently disabled for stateless API
- Should be enabled in production for cookie-based auth
- Use double-submit cookie pattern

## 🚀 Production Checklist

- [ ] Enable HTTPS and set `cookie.setSecure(true)`
- [ ] Enable CSRF protection for cookie-based auth
- [ ] Set proper CORS policies
- [ ] Use environment variables for JWT secrets
- [ ] Implement rate limiting on auth endpoints
- [ ] Add account lockout after failed attempts
- [ ] Implement refresh token rotation
- [ ] Set up monitoring for auth failures
- [ ] Use shorter token expiration times
- [ ] Add 2FA (Two-Factor Authentication)
- [ ] Implement logout from all devices
- [ ] Add token blacklist for immediate revocation

## 📱 Client Integration

### Web Application (Browser)
- Cookies handled automatically
- No need to manually manage tokens
- Just make fetch/axios requests normally

### Mobile/SPA Application
- Use Authorization header for API requests
- Store tokens in secure storage
- Manually handle token refresh

```javascript
// Example: Fetch with cookies (automatic)
fetch('/user-info', {
  credentials: 'include' // Send cookies
})

// Example: Fetch with Authorization header
fetch('/user-info', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
})
```

## 🎉 Benefits of This Approach

1. **Unified Experience**: One login page for all methods
2. **Seamless**: Users don't see token management
3. **Secure**: HTTP-only cookies prevent XSS
4. **Stateless**: JWT enables horizontal scaling
5. **Auto-Refresh**: No interruption from token expiration
6. **Flexible**: Supports both browser and API clients
7. **Standard**: OAuth2 + JWT industry best practices

## 📞 Support

For issues or questions:
1. Check browser console for errors
2. Check server logs for authentication flow
3. Use `/api/token/info` endpoint to debug token state
4. Verify cookies are being set and sent
