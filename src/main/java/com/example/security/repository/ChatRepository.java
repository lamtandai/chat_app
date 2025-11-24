package com.example.security.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.security.model.Message;

@Repository
public interface ChatRepository extends JpaRepository<Message, Long >{
    List<Message> findAll();
}
