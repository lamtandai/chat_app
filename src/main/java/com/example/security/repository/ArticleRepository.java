package com.example.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.security.model.Article;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Integer> {
}
