package com.example.security.message;

import java.util.HashMap;
import java.util.Map;

import com.example.security.model.Article;
import com.example.security.model.Category;


public class ArticleMessage {

    private Category category;
    private Article article;
    
    public ArticleMessage() {
    }

    public ArticleMessage(Category category, Article article) {
        this.category = category;
        this.article = article;
    }

    public Map<String, Object> setContent() {
        HashMap<String, Object> content = new HashMap<>();

        return content;

    }

    public Category getCategory() {
        return category;
    }

    public Article getArticle() {
        return article;
    }

    @Override
    public String toString() {
        return "ArticleMessage [category=" + category + ", article=" + article + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((category == null) ? 0 : category.hashCode());
        result = prime * result + ((article == null) ? 0 : article.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ArticleMessage other = (ArticleMessage) obj;
        if (category == null) {
            if (other.category != null)
                return false;
        } else if (!category.equals(other.category))
            return false;
        if (article == null) {
            if (other.article != null)
                return false;
        } else if (!article.equals(other.article))
            return false;
        return true;
    }

}
