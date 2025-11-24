package com.example.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.example.security.model.Article;
import com.example.security.model.Category;
import com.example.security.rabbitmq.Producer;
import com.example.security.repository.ArticleRepository;
import com.example.security.repository.CategoryRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class SecurityApplication {
	static CategoryRepository categoryRepository;
	static ArticleRepository articleRepository;
	static Producer producer;
	public record TestMessage(Object category, Object article) {}

	static Category createCategory(String name) {
		Category category = Category.builder().name(name).build();
		categoryRepository.save(category);
		log.info("created category {}={}", category.getId(), category.getName());
		return category;
	}

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(SecurityApplication.class, args);
		categoryRepository = context.getBean(CategoryRepository.class);
		articleRepository = context.getBean(ArticleRepository.class);
		producer = context.getBean(Producer.class);

		try {
			Category entertainmentCategory = createCategory("Entertainment");
			Category sportCategory = createCategory("Sport");
			Article article;
			article = Article.builder().url("spider-man-live-action")
					.title("Spider-Man: Live-Action")
					.content("...").category(entertainmentCategory).build();
			articleRepository
					.save(article);

			var test = new TestMessage(entertainmentCategory, article);

			producer.sendMessage(test);
			Article article2 = Article.builder().url("superman")
					.title("Superman")
					.content("...").category(entertainmentCategory).build();
			var test2 = new TestMessage(entertainmentCategory, article);

			articleRepository
					.save(article2);
			producer.sendMessage(test2);

			Article article3 = Article.builder().url("maxpetent winner winner chicken dinner")
					.title("F1 race")
					.content("...").category(sportCategory).build();
			articleRepository
					.save(article3);
			var test3 = new TestMessage(entertainmentCategory, article);
			producer.sendMessage(test3);
			
		} catch (Exception e) {
			log.error("Cannot create seed data");
			log.info("exception: {}", e);
		}
	}

}
