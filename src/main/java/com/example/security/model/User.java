package com.example.security.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;

import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.security.model.Authority.Authority;
import com.example.security.model.Authority.Token;
import com.example.security.model.Chatting.Channel;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    private UUID id;

    @OneToMany
    private Set<Authority> authorities;

    private String password;

    private String firstname;

    private String lastname;

    private String name;

    private String picture;

    @Column(unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    private boolean accountNonExpired;

    private boolean accountNonLocked;

    private boolean credentialsNonExpired;

    private String provider;

    private String providerId;

    private boolean enabled;

    @Enumerated(EnumType.STRING)
    private com.example.security.model.Authority.Role role;

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
    @JsonIgnore
    @ToString.Exclude
    private List<Token> tokens;

    @OneToMany(mappedBy = "creatorId",   fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Channel> channels;


}
