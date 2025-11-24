package com.example.security.service;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.security.dto.GetCategoryArticlesRequest;
import com.example.security.dto.GetCategoryArticlesResponse;
import com.example.security.exception.CategoryNotFoundException;
import com.example.security.model.Category;
import com.example.security.redisSchema.RedisSchema;
import com.example.security.repository.CategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CategoryServiceImpl implements CategoryService {
  private static final int CACHE_TIME_IN_MINUTE = 5;
  @Autowired
  private CategoryRepository categoryRepository;

  @Autowired
  private RedisTemplate<String, String> redisTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Override
  public GetCategoryArticlesResponse getArticles(GetCategoryArticlesRequest request)
      throws CategoryNotFoundException, IOException {

    String cacheKey = RedisSchema.getArticlesOfCategoryId(String.valueOf(request.getCategoryId()));
    String cachedData = redisTemplate.opsForValue().get(cacheKey);
    GetCategoryArticlesResponse response;
    if (Objects.isNull(cachedData)) {
      log.info("cache miss, get from mysql");
      Optional<Category> optionalCategory = categoryRepository.findById(request.getCategoryId());
      if (optionalCategory.isEmpty()) {
        throw new CategoryNotFoundException();
      }
      Category category = optionalCategory.get();
      response = GetCategoryArticlesResponse.builder().articles(category.getArticles())
          .build();
      redisTemplate.opsForValue().set(cacheKey,
          objectMapper.writeValueAsString(response),
          Duration.ofMinutes(CACHE_TIME_IN_MINUTE));
    } else {
      log.info("cache hit");
      response = objectMapper.readValue(cachedData,
          GetCategoryArticlesResponse.class);
    }
    return response;
  }

}
