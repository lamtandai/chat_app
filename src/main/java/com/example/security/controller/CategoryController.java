package com.example.security.controller;

import java.io.IOException;
import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.security.dto.GetCategoryArticlesRequest;
import com.example.security.dto.GetCategoryArticlesResponse;
import com.example.security.exception.CategoryNotFoundException;
import com.example.security.service.CategoryService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(path = "/category")
public class CategoryController {
  @Autowired
  private CategoryService categoryService;

  @GetMapping("/{id}/articles")
  public ResponseEntity<GetCategoryArticlesResponse> getArticles(@PathVariable("id") Integer id, Principal principal)
      throws IOException {
   
    log.info("request id={}, auth={}", id, principal.getName());
    GetCategoryArticlesResponse response;
    try {
      response = categoryService.getArticles(GetCategoryArticlesRequest.builder().categoryId(id).build());
    } catch (CategoryNotFoundException e) {
      return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.ok(response);
  }

}
