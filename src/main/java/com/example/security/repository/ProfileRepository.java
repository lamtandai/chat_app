package com.example.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.security.model.Profile;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Integer> {
}
