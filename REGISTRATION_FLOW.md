# Registration Flow Documentation

## 🎯 Overview

This document explains how user registration works for **both Basic Authentication and OAuth2**, ensuring consistent user data structure regardless of the registration method.

## 📊 User Entity Structure

Both registration methods populate the same `User` entity:

```java
User {
    UUID id;                    // Unique identifier
    String firstname;           // First name
    String lastname;            // Last name
    String name;                // Full name (firstname + lastname)
    String email;               // Email address (unique)
    String username;            // Username (uses email for consistency)
    String password;            // Encrypted password (null for OAuth2)
    String picture;             // Profile picture URL (mainly for OAuth2)
    String provider;            // "local", "google", "facebook", "github"
    String providerId;          // Provider's user ID (null for local)
    Role role;                  // USER or ADMIN
    boolean enabled;            // Account enabled status
    boolean accountNonExpired;
    boolean accountNonLocked;
    boolean credentialsNonExpired;
}
```

## 🔄 Flow 1: Basic Authentication Registration

### Frontend (login.html)

**Step 1: User fills registration form**
```html
<form id="register-form" onsubmit="handleRegister(event)">
    <input id="reg-firstname" name="firstname" placeholder="First Name" required>
    <input id="reg-lastname" name="lastname" placeholder="Last Name" required>
    <input id="reg-email" name="email" type="email" placeholder="Email" required>
    <input id="reg-password" name="password" type="password" placeholder="Password" required>
    <button type="submit">Sign Up</button>
</form>
```

**Step 2: JavaScript sends POST request**
```javascript
async function handleRegister(event) {
    event.preventDefault();
    
    const registerData = {
        firstname: document.getElementById('reg-firstname').value,
        lastname: document.getElementById('reg-lastname').value,
        email: document.getElementById('reg-email').value,
        password: document.getElementById('reg-password').value
    };
    
    const response = await fetch('/api/v1/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(registerData)
    });
    
    const data = await response.json();
    
    if (response.ok) {
        // Server sets JWT tokens in HTTP-only cookies automatically
        // Redirect to dashboard
        window.location.href = '/';
    }
}
```

### Backend Flow

**Step 3: AuthenticationController receives request**
```java
@PostMapping("/register")
public ResponseEntity<AuthenticationResponse> register(
        @RequestBody RegisterRequest request,  // Maps JSON to RegisterRequest DTO
        HttpServletResponse response) {
    
    // RegisterRequest contains:
    // - firstname: "John"
    // - lastname: "Doe"
    // - email: "john.doe@example.com"
    // - password: "password123"
    
    AuthenticationResponse authResponse = authenticationService.registerUser(request);
    
    // Add JWT tokens to cookies
    addTokenCookies(response, authResponse.getAccessToken(), authResponse.getRefreshToken());
    
    return ResponseEntity.ok(authResponse);
}
```

**Step 4: AuthenticationService creates User**
```java
public AuthenticationResponse registerUser(RegisterRequest request) {
    User user = User.builder()
        .id(UUID.randomUUID())                          // Generate unique ID
        .firstname(request.getFirstname())              // "John"
        .lastname(request.getLastname())                // "Doe"
        .name(request.getFirstname() + " " + request.getLastname())  // "John Doe"
        .email(request.getEmail())                      // "john.doe@example.com"
        .username(request.getEmail())                   // "john.doe@example.com"
        .password(passwordEncoder.encode(request.getPassword()))  // Encrypted
        .role(Role.USER)                                // Default role
        .enabled(true)
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .provider("local")                              // Mark as basic auth
        .build();
    
    return register(user);  // Save and generate JWT tokens
}
```

**Step 5: Generate JWT and return**
```java
public AuthenticationResponse register(User user) {
    User savedUser = repository.save(user);                    // Save to database
    String jwtToken = jwtService.generateToken(user);          // Generate access token
    String refreshToken = jwtService.generateRefreshToken(user); // Generate refresh token
    saveUserToken(savedUser, jwtToken);                        // Store token in DB
    
    return AuthenticationResponse.builder()
        .accessToken(jwtToken)
        .refreshToken(refreshToken)
        .build();
}
```

**Step 6: Set cookies and respond**
```java
private void addTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
    Cookie accessTokenCookie = new Cookie("access_token", accessToken);
    accessTokenCookie.setHttpOnly(true);
    accessTokenCookie.setMaxAge(24 * 60 * 60); // 24 hours
    response.addCookie(accessTokenCookie);
    
    Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
    refreshTokenCookie.setHttpOnly(true);
    refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
    response.addCookie(refreshTokenCookie);
}
```

## 🌐 Flow 2: OAuth2 Registration (Google/Facebook/GitHub)

### Frontend (login.html)

**Step 1: User clicks OAuth2 button**
```html
<a href="/oauth2/authorization/google" class="login-btn google-btn">
    Continue with Google
</a>
<a href="/oauth2/authorization/facebook" class="login-btn facebook-btn">
    Continue with Facebook
</a>
<a href="/oauth2/authorization/github" class="login-btn github-btn">
    Continue with GitHub
</a>
```

**Step 2: Redirect to OAuth2 provider**
- User is redirected to Google/Facebook/GitHub
- User authorizes the application
- Provider redirects back with authorization code

### Backend Flow

**Step 3: OAuth2 callback** (`/login/oauth2/code/{provider}`)
Spring Security automatically:
1. Exchanges authorization code for access token
2. Calls `OAuth2UserService.loadUser()` to fetch user info

**Step 4: OAuth2UserService extracts user info**
```java
@Override
public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) {
    // Get user info from provider (Google/Facebook/GitHub)
    OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
    
    // Process and register/update user
    return processOAuth2User(oAuth2UserRequest, oAuth2User);
}

private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
    String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
    // registrationId: "google", "facebook", or "github"
    
    // Extract user info using provider-specific logic
    OAuth2Provider provider = OAuth2ProviderFactory.getProvider(registrationId);
    Oauth2UserInfoDto userInfoDto = provider.extractUserInfo(oAuth2User.getAttributes());
    
    // userInfoDto contains:
    // - id: "123456789" (provider's user ID)
    // - name: "John Doe" (full name from provider)
    // - email: "john.doe@gmail.com"
    // - picture: "https://lh3.googleusercontent.com/..."
    
    // Check if user already exists
    Optional<User> userOptional = userRepository.findByUsername(userInfoDto.getEmail());
    
    User user = userOptional
        .map(existingUser -> updateExistingUser(existingUser, userInfoDto))
        .orElseGet(() -> registerNewUser(oAuth2UserRequest, userInfoDto));
    
    return UserPrincipal.create(user, oAuth2User.getAttributes());
}
```

**Step 5: Register new OAuth2 user**
```java
private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, Oauth2UserInfoDto userInfoDto) {
    String providerName = oAuth2UserRequest.getClientRegistration().getRegistrationId();
    
    // Split full name into first and last name
    String fullName = userInfoDto.getName();  // "John Doe"
    String[] nameParts = fullName.split("\\s+", 2);
    String firstname = nameParts[0];           // "John"
    String lastname = nameParts.length > 1 ? nameParts[1] : "";  // "Doe"
    
    // Use email from OAuth2 provider
    String email = userInfoDto.getEmail();     // "john.doe@gmail.com"
    String username = email;                   // Use email as username
    
    // Build user with same structure as basic auth
    User user = User.builder()
        .id(UUID.randomUUID())
        .firstname(firstname)                  // "John"
        .lastname(lastname)                    // "Doe"
        .name(fullName)                        // "John Doe"
        .email(email)                          // "john.doe@gmail.com"
        .username(username)                    // "john.doe@gmail.com"
        .picture(userInfoDto.getPicture())     // Profile picture URL
        .provider(providerName)                // "google", "facebook", "github"
        .providerId(userInfoDto.getId())       // "123456789"
        .role(Role.USER)                       // Default role
        .password(null)                        // No password for OAuth2
        .enabled(true)
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .build();
    
    return userRepository.save(user);
}
```

**Step 6: OAuth2LoginSuccessHandler generates JWT**
```java
@Override
public void onAuthenticationSuccess(
        HttpServletRequest request, 
        HttpServletResponse response,
        Authentication authentication) throws IOException {
    
    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
    
    // Find the actual User entity
    User user = userRepository.findByUsername(userPrincipal.getUsername())
        .orElseThrow(() -> new IllegalStateException("User not found"));
    
    // Generate JWT tokens (same as basic auth)
    String accessToken = jwtService.generateToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);
    
    // Revoke old tokens and save new one
    revokeAllUserTokens(user);
    saveUserToken(user, accessToken);
    
    // Set tokens in HTTP-only cookies
    Cookie accessTokenCookie = new Cookie("access_token", accessToken);
    accessTokenCookie.setHttpOnly(true);
    accessTokenCookie.setMaxAge(24 * 60 * 60);
    response.addCookie(accessTokenCookie);
    
    Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
    refreshTokenCookie.setHttpOnly(true);
    refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60);
    response.addCookie(refreshTokenCookie);
    
    // Redirect to dashboard
    response.sendRedirect("/");
}
```

## 🔍 Data Mapping Comparison

### Basic Auth Registration
| Input Field (login.html) | RegisterRequest DTO | User Entity Field | Value Example |
|---------------------------|---------------------|-------------------|---------------|
| `reg-firstname` | `firstname` | `firstname` | "John" |
| `reg-lastname` | `lastname` | `lastname` | "Doe" |
| `reg-email` | `email` | `email` | "john@example.com" |
| `reg-email` | - | `username` | "john@example.com" |
| `reg-password` | `password` | `password` | (encrypted) |
| - | - | `name` | "John Doe" |
| - | - | `provider` | "local" |
| - | - | `role` | USER |

### OAuth2 Registration (Google)
| Google API Field | Oauth2UserInfoDto | User Entity Field | Value Example |
|------------------|-------------------|-------------------|---------------|
| `sub` | `id` | `providerId` | "123456789" |
| `name` | `name` | `name` | "John Doe" |
| `name` (split) | - | `firstname` | "John" |
| `name` (split) | - | `lastname` | "Doe" |
| `email` | `email` | `email` | "john@gmail.com" |
| `email` | - | `username` | "john@gmail.com" |
| `picture` | `picture` | `picture` | "https://..." |
| - | - | `password` | null |
| - | - | `provider` | "google" |
| - | - | `role` | USER |

## ✅ Consistency Achieved

Both registration methods now create users with:
1. ✅ **firstname** and **lastname** fields properly populated
2. ✅ **email** and **username** using the same value (email)
3. ✅ **name** containing full name
4. ✅ **role** set to USER by default
5. ✅ **provider** indicating registration source ("local" vs "google")
6. ✅ All boolean flags properly initialized
7. ✅ JWT tokens generated and stored in HTTP-only cookies

## 🧪 Testing

### Test Basic Registration
1. Open `http://localhost:8080/login`
2. Click "Sign Up"
3. Fill in:
   - First Name: John
   - Last Name: Doe
   - Email: john@example.com
   - Password: password123
4. Click "Sign Up"
5. Check database for user with provider="local"

### Test OAuth2 Registration
1. Open `http://localhost:8080/login`
2. Click "Continue with Google"
3. Authorize with Google account
4. Check database for user with:
   - firstname from Google name
   - email from Google
   - provider="google"
   - password=null

### Verify Data
```sql
SELECT 
    id, 
    firstname, 
    lastname, 
    name,
    email, 
    username, 
    provider, 
    role,
    password IS NULL as is_oauth2
FROM user;
```

Expected results:
- Basic auth users have password
- OAuth2 users have provider="google/facebook/github" and password=NULL
- All users have firstname, lastname, email, username populated

## 🎉 Summary

✅ **Basic Auth**: Form data → RegisterRequest → AuthenticationService → User entity with all fields
✅ **OAuth2**: Provider data → Oauth2UserInfoDto → OAuth2UserService → User entity with all fields  
✅ **Both methods**: Generate JWT tokens → Store in cookies → Redirect to dashboard
✅ **Data consistency**: All users have same structure regardless of registration method
