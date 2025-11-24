package com.example.security.config;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;


@Configuration
public class RabbitMqConfig {
    private RedisTemplate<String, String> redisTemplate;

    public static final String queue_name = "queue_1";
    public static final String queue_json = "queue_json";
    public static final String exchange_name = "exchange_1";
    public static final String exchange_json = "exchange_json";
    public static final String routing_key = "routing_key_1_to_q1";
    public static final String routing_key_json = "routing_key_1_to_q_json";

    @Bean
    public Queue queue() {
        return QueueBuilder.durable(queue_name).build();
    }

    @Bean
    public Queue jsQueue(){
        return QueueBuilder.durable(queue_json).build();
    }

    @Bean
    public Queue wsQueue(){
        return QueueBuilder.durable("/topic/rankings").build();
    }

    @Bean
    public Queue wsChatQueue(){
        return QueueBuilder.durable("/topic/chat").build();
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(exchange_name);
    }

    @Bean
    public TopicExchange topicExchangeJson() {
        return new TopicExchange(exchange_json);
    }

    @Bean
    public Binding binding () {
        return BindingBuilder.bind(queue()).to(topicExchange()).with(routing_key);
    }

    @Bean
    public Binding jsobBinding() {
        return BindingBuilder.bind(jsQueue()).to(topicExchangeJson()).with(routing_key_json);
    }
    

    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate template(ConnectionFactory con){
        RabbitTemplate template = new RabbitTemplate(con);
        template.setMessageConverter(messageConverter());
        return template;
    }

}
