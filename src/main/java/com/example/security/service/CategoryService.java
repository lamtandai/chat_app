package com.example.security.service;

import java.io.IOException;

import org.springframework.stereotype.Service;

import com.example.security.dto.GetCategoryArticlesRequest;
import com.example.security.dto.GetCategoryArticlesResponse;
import com.example.security.exception.CategoryNotFoundException;

@Service
public interface CategoryService {
  GetCategoryArticlesResponse getArticles(GetCategoryArticlesRequest request)
      throws CategoryNotFoundException, IOException;
}
