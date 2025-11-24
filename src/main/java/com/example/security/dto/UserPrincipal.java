package com.example.security.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.example.security.model.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPrincipal implements OAuth2User, UserDetails {

    private UUID id;
    private String username;
    private String password;
    private String name;
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private String provider;
    private String providerId;
    private String picture;
    private boolean enabled;
    private Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;

    public UserPrincipal(UUID id, String username, String password,
            Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
    }

    public static UserPrincipal create(User user) {
        String roleName = user.getRole() != null ? user.getRole().name() : "USER";
        List<GrantedAuthority> authorities = Collections
                .singletonList(new SimpleGrantedAuthority("ROLE_" + roleName));

        UserPrincipal principal = new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                authorities);

        principal.setName(user.getName());
        principal.setAccountNonExpired(user.isAccountNonExpired());
        principal.setAccountNonLocked(user.isAccountNonLocked());
        principal.setCredentialsNonExpired(user.isCredentialsNonExpired());
        principal.setEnabled(user.isEnabled());
        principal.setProvider(user.getProvider());
        principal.setProviderId(user.getProviderId());
        principal.setPicture(user.getPicture());

        return principal;
    }

    public static UserPrincipal create(User user, Map<String, Object> attributes) {
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        userPrincipal.setAttributes(attributes);
        return userPrincipal;
    }

    @Override
    public String getName() {
        // Return username as the principal name for OAuth2User interface
        // Provide fallbacks to ensure this is never null or empty
        if (this.username != null && !this.username.trim().isEmpty()) {
            return this.username;
        }
        if (this.name != null && !this.name.trim().isEmpty()) {
            return this.name;
        }
        if (this.id != null) {
            return this.id.toString();
        }
        // Last resort - should never reach here
        return "unknown-user";
    }

}
