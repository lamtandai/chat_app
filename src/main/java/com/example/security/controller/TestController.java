package com.example.security.controller;

import java.util.List;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.example.security.dto.MessageRequest;
import com.example.security.dto.RankingUpdateRequest;
import com.example.security.message.ArticleMessage;
import com.example.security.model.Article;
import com.example.security.model.Category;
import com.example.security.model.Ranking;
import com.example.security.rabbitmq.Producer;
import com.example.security.rabbitmq.JsonProducer;
import com.example.security.repository.ArticleRepository;
import com.example.security.repository.CategoryRepository;
import com.example.security.service.ChatService;
import com.example.security.service.RankingService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.security.core.Authentication;
import com.example.security.dto.UserPrincipal;

@RestController

public class TestController {
    @Autowired
    private Producer producer;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JsonProducer JsonProducer;

    @Autowired
    private RankingService raningService;

    @Autowired
    private ChatService chatService;

    static CategoryRepository categoryRepository;
    static ArticleRepository articleRepository;

    @GetMapping("/test/normal")
    public ResponseEntity<Object> test() {
        rabbitTemplate.convertAndSend("exchange_1", "routing_key_1_to_q1", "test message");
        return ResponseEntity.ok("test message");
    }

    @GetMapping("/test/json")
    public ResponseEntity<Object> jsonTest() {
        Category category = Category.builder().name("Entertainment").build();
        Article article;
        article = Article.builder().url("spider-man-live-action")
                .title("Spider-Man: Live-Action")
                .content("...").category(category).build();

        ArticleMessage test = new ArticleMessage(category, article);
        JsonProducer.sendMessage(test);
        return ResponseEntity.ok(test);
    }

    @GetMapping("/test/websocket")
    public String testWebSoket() {
        return "index.html";
    }

    @PostMapping("/test/update-ranking")
    public void jsonTestUpdateRanking(@RequestBody RankingUpdateRequest request) {
        raningService.updateRanking(request.getUser(), request.getScore());
    }

    @GetMapping("/test/ranking/{count}")
    public ResponseEntity<List<Ranking>> testRanking(@RequestBody Integer count) {
        List<Ranking> ranks = raningService.getTopRankings(count);
        return ResponseEntity.ok(ranks);
    }

    @PostMapping("/test/send-message")
    public void getMethodName(@RequestBody MessageRequest mess, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            mess.setSender(String.valueOf(userPrincipal.getId()));
        }
        chatService.addMessage(mess);
        chatService.showMessage();
    }
}
