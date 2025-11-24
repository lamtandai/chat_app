package com.example.security.dto;

import lombok.Data;

@Data
public class RankingUpdateRequest {
  private String user;
  private double score;

}
