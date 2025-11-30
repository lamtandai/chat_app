package com.example.security.model.Chatting;

import java.io.Serializable;
import java.time.Instant;

import java.util.UUID;

import com.example.security.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "channels",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "channel_name")
    }
)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Channel implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "name")
    private String name;

    @ManyToOne()
    @JoinColumn(name = "creator_id")
    private User creatorId;
    
    @Column(name = "counter")
    private long counter;

    @Column(name = "created_at")
    private Instant createdAt;

}
