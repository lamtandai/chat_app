package com.example.security.service;

import org.springframework.stereotype.Service;

import com.example.security.model.Chatting.Channel;
import com.example.security.repository.ChatRepo.ChannelRepository;


import java.util.Optional;


/**
 * Local sequence number generator for message IDs.
 * Generates unique, time-sortable IDs within each conversation/channel.
 * 
 * This implementation ensures:
 * 1. IDs are unique within a channel
 * 2. IDs are sortable by time (new messages have higher IDs)
 * 3. Thread-safe generation for concurrent message creation
 */
@Service
public class MessageSequenceGenerator {

    // Map to store sequence counters for each channel
    private final ChannelRepository channelRepository;

    public MessageSequenceGenerator(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    /**
     * Generates the next message ID for a specific channel.
     * 
     * @param channelId The unique identifier for the conversation/channel
     * @return The next sequence number for this channel
     */

    public boolean updateMessageCounter(long channelId){
        int updateMessageId = this.channelRepository.updateCounter(channelId);
        return updateMessageId != 0 ? true: false;
    }  

    public long generateMessageId(long channelId) {
        // Get or create an atomic counter for this channel
        boolean isUpdate =  updateMessageCounter(channelId);

        if (isUpdate) {
            Optional<Channel> channel = this.channelRepository.findById(channelId);
            if (channel.isPresent()){
                return channel.get().getCounter();
            }
            throw new RuntimeException("Channel not found!");
        }

        throw new RuntimeException("Can not generate new messageId!");
    }

    /**
     * Generates a composite message ID that includes both timestamp and sequence.
     * Format: timestamp (41 bits) + sequence (22 bits) = 63 bits total
     * 
     * This provides better time-sortability and allows ~4 million messages per
     * channel per millisecond.
     * 
     * @param channelId The unique identifier for the conversation/channel
     * @return A composite ID combining timestamp and sequence number
     */
    public long generateCompositeMessageId(long channelId, long sequence) {
        long timestamp = System.currentTimeMillis();

        // Shift timestamp left by 22 bits and add sequence (masked to 22 bits)
        return (timestamp << 22) | (sequence & 0x3FFFFF);
    }

    /**
     * Generates a channel ID for a one-on-one conversation.
     * Uses lexicographic ordering to ensure the same channel ID regardless of who
     * initiates.
     * 
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return A consistent channel ID for this pair of users
     */
    public String generateOneOnOneChannelId(String userId1, String userId2) {
        // Sort user IDs to ensure consistency
        if (userId1.compareTo(userId2) < 0) {
            return "1on1:" + userId1 + ":" + userId2;
        } else {
            return "1on1:" + userId2 + ":" + userId1;
        }
    }

    /**
     * Generates a channel ID for a group conversation.
     * 
     * @param groupId The unique group identifier
     * @return A channel ID for this group
     */
    public String generateGroupChannelId(String groupId) {
        return "group:" + groupId;
    }

    /**
     * Resets the sequence for a specific channel.
     * Use with caution - mainly for testing or maintenance purposes.
     * 
     * @param channelId The channel whose sequence should be reset
     */

    /**
     * Gets the current sequence number for a channel without incrementing.
     * 
     * @param channelId The channel ID
     * @return The current sequence number, or 0 if channel doesn't exist
     */

}
