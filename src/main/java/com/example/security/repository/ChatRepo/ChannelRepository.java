package com.example.security.repository.ChatRepo;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.security.model.Chatting.Channel;
import com.example.security.model.Chatting.ChatMemmbers;

import jakarta.transaction.Transactional;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long> {
    Optional<Channel> findByName(String name);

    List<Channel> findByNameContainingIgnoreCase(String name);

    @Query("SELECT cm FROM ChatMemmbers cm WHERE cm.channel = :channelId")
    List<ChatMemmbers> findMembersByChannelId(@Param("channelId") Long channelId);

    @Query("SELECT c FROM Channel c WHERE c.id IN (SELECT cm.channel FROM ChatMemmbers cm WHERE cm.user = :userId)")
    List<Channel> findChannelsByUserId(@Param("userId") java.util.UUID userId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE channels SET counter = counter + 1 where id = id", nativeQuery = true)
    int updateCounter(@Param("id") Long channelId);
}
