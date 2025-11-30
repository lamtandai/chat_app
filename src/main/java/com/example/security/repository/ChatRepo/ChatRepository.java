package com.example.security.repository.ChatRepo;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.security.model.Chatting.Message;

@Repository
public interface ChatRepository extends JpaRepository<Message, Long> {
    List<Message> findAll();

    // Find all messages in a specific channel, ordered by sequence number
    List<Message> findByChannelIdOrderBySequenceNumberAsc(Long channelId);

    // Find messages in a channel after a specific sequence number (for
    // pagination/sync)
    List<Message> findByChannelIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
            Long channelId, Long sequenceNumber);

    // Fetch latest messages (for initial load)
    List<Message> findByChannelIdOrderBySequenceNumberDesc(Long channelId, Pageable pageable);

    // Fetch older messages (for scrolling up)
    List<Message> findByChannelIdAndSequenceNumberLessThanOrderBySequenceNumberDesc(
            Long channelId, Long sequenceNumber, Pageable pageable);
}
