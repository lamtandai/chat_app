package com.example.security.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.example.security.dto.MessageResponse;
import com.example.security.dto.SendingMessageRequest;
import com.example.security.dto.ChannelSearchResponse;
import com.example.security.dto.UserSearchResponse;
import com.example.security.dto.CreateChannelRequest;
import com.example.security.model.User;
import com.example.security.model.Chatting.Channel;
import com.example.security.model.Chatting.Message;
import com.example.security.repository.UserRepository;
import com.example.security.repository.ChatRepo.ChannelRepository;
import com.example.security.repository.ChatRepo.ChatRepository;
import com.example.security.model.Chatting.ChatMemmbers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.UUID;
import java.util.Optional;
import java.util.Collections;

import java.util.Map;
import java.util.HashMap;

@Service
public class ChatService {

    private ChatRepository chatRepository;
    private UserRepository userRepository;
    private SimpMessagingTemplate messagingTemplate;
    private MessageSequenceGenerator sequenceGenerator;
    private ChannelRepository channelRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String CHANNEL_PREFIX = "/group";
    private static final String PUBLIC_CHANNEL = CHANNEL_PREFIX + "/public";

    public ChatService(ChatRepository chatRepository, UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate, MessageSequenceGenerator sequenceGenerator,
            ChannelRepository channelRepository) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.sequenceGenerator = sequenceGenerator;
        this.channelRepository = channelRepository;
    }

    @Transactional
    public void addMessage(SendingMessageRequest messString) {
        // Extract values from the new nested structure
        String senderId = messString.getSender().getUserId();
        String content = messString.getSender().getContent();

        String receiverId = null;
        Long channelIdParam = null;

        if (messString.getReceiver() != null) {
            receiverId = messString.getReceiver().getUserId();
            channelIdParam = messString.getReceiver().getChannelId();
        }

        // Determine the channel ID based on sender and receiver
        long channelId = determineChannelId(channelIdParam, senderId, receiverId);

        // Generate the local sequence number for this channel
        long sequenceNumber = sequenceGenerator.generateMessageId(channelId);

        // Generate composite ID (timestamp + sequence) for better time-sortability
        long messageId = sequenceGenerator.generateCompositeMessageId(channelId, sequenceNumber);

        Message newMess = Message.builder()
                .messageId(messageId)
                .channelId(channelId)
                .sequenceNumber(sequenceNumber)
                .content(content)
                .sender(senderId)
                .receiver(receiverId)
                .createdAt(Instant.now())
                .build();

        chatRepository.save(newMess);

        showMessage(channelId);
    }

    /**
     * Determines the channel ID based on sender and receiver.
     * For one-on-one chats, generates a consistent channel ID.
     * For group chats, would use the group ID.
     */
    private long determineChannelId(Long ChannelId, String sender, String receiver) {
        if (ChannelId == null && (receiver == null || receiver.isEmpty())) {
            throw new RuntimeException("This group chat or receiver is not found!");
        }
        if (ChannelId != null) {
            Optional<Channel> private_group_channelOpt = channelRepository.findById(ChannelId);

            if (private_group_channelOpt.isPresent()) {
                return ChannelId;
            }
        }
        if (receiver != null && !receiver.isEmpty()) {
            String oneOnOneChannel = sequenceGenerator.generateOneOnOneChannelId(sender, receiver);
            Optional<Channel> private_group_channelOpt = channelRepository.findByName(oneOnOneChannel);
            if (private_group_channelOpt.isPresent()) {
                Channel existingChannel = private_group_channelOpt.get();
                // Self-healing: Ensure members exist
                return existingChannel.getId();
            } else {
                // Create new 1-on-1 channel
                Channel newChannel = Channel.builder()
                        .name(oneOnOneChannel)
                        .counter(0)
                        .createdAt(Instant.now())
                        .build();

                newChannel = channelRepository.save(newChannel);

                // Add members to the channel
                ChatMemmbers member1 = ChatMemmbers.builder()
                        .channel(newChannel.getId())
                        .user(UUID.fromString(sender))
                        .build();

                ChatMemmbers member2 = ChatMemmbers.builder()
                        .channel(newChannel.getId())
                        .user(UUID.fromString(receiver))
                        .build();

                entityManager.persist(member1);
                entityManager.persist(member2);
                // Ensure members are visible to subsequent queries
                entityManager.flush();
                return newChannel.getId();
            }
        }

        throw new RuntimeException("This group chat or receiver is not found!");
    }

    public void showMessage(long channelId) {
        // Fetch latest 15 messages for the channel
        Pageable pageable = PageRequest.of(0, 15);
        List<Message> messages = chatRepository.findByChannelIdOrderBySequenceNumberDesc(channelId, pageable);

        // Reverse to return in chronological order (oldest -> newest)
        Collections.reverse(messages);

        List<MessageResponse> responseMessages = messages.stream()
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList());

        // Find all members in the channel
        List<ChatMemmbers> members = channelRepository.findMembersByChannelId(channelId);

        // Send to each member
        for (ChatMemmbers member : members) {
            messagingTemplate.convertAndSend("/queue/chat-user-" + member.getUser(), responseMessages);
        }
    }

    /**
     * Get messages for a specific channel/conversation, ordered by sequence.
     * 
     * @param channelId The channel identifier
     * @return List of messages ordered by sequence number
     */
    public List<MessageResponse> getChannelMessages(Long channelId) {
        return chatRepository.findByChannelIdOrderBySequenceNumberAsc(channelId)
                .stream()
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get messages for a one-on-one conversation between two users.
     * 
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return List of messages ordered by sequence number
     */
    @Transactional
    public List<MessageResponse> getConversationMessages(String userId1, String userId2) {
        long channelId = determineChannelId(null, userId1, userId2);
        return getInitialMessages(channelId);
    }

    /**
     * Get new messages in a channel after a specific sequence number.
     * Useful for message synchronization and pagination.
     * 
     * @param channelId     The channel identifier
     * @param afterSequence The sequence number after which to fetch messages
     * @return List of new messages
     */
    public List<MessageResponse> getNewMessages(Long channelId, Long afterSequence) {
        return chatRepository.findByChannelIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
                channelId, afterSequence)
                .stream()
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get initial messages (latest 15) for a channel.
     */
    public List<MessageResponse> getInitialMessages(Long channelId) {
        Pageable pageable = PageRequest.of(0, 15);
        List<Message> messages = chatRepository.findByChannelIdOrderBySequenceNumberDesc(channelId, pageable);

        // Reverse to return in chronological order (oldest -> newest)
        java.util.Collections.reverse(messages);

        return messages.stream()
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get older messages before a specific sequence number.
     */
    public List<MessageResponse> getOlderMessages(Long channelId, Long beforeSequence) {
        Pageable pageable = PageRequest.of(0, 15);
        List<Message> messages = chatRepository.findByChannelIdAndSequenceNumberLessThanOrderBySequenceNumberDesc(
                channelId, beforeSequence, pageable);

        // Reverse to return in chronological order (oldest -> newest)
        java.util.Collections.reverse(messages);

        return messages.stream()
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Helper method to convert Message entity to MessageResponse DTO.
     */
    private MessageResponse convertToMessageResponse(Message mess) {
        String senderId = mess.getSender();
        String senderName = "Unknown";
        String senderAvatar = "";

        if (senderId != null) {
            try {
                Optional<User> userOpt = userRepository.findById(UUID.fromString(senderId));
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    senderName = user.getName() != null ? user.getName() : user.getUsername();
                    senderAvatar = user.getPicture();
                }
            } catch (IllegalArgumentException e) {
                // Handle invalid UUID string if necessary
            }
        }

        return new MessageResponse(
                mess.getMessageId(),
                mess.getContent(),
                senderId,
                senderName,
                senderAvatar,
                mess.getSequenceNumber(),
                mess.getCreatedAt() != null ? mess.getCreatedAt().toString() : "",
                mess.getChannelId());
    }

    public List<ChannelSearchResponse> searchChannels(String query) {
        return channelRepository.findByNameContainingIgnoreCase(query)
                .stream()
                .map(channel -> ChannelSearchResponse.builder()
                        .id(channel.getId())
                        .name(channel.getName())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public Channel createChannel(CreateChannelRequest request) {
        User creator = userRepository.findById(UUID.fromString(request.getCreatorId()))
                .orElseThrow(() -> new RuntimeException("Creator not found"));

        Channel newChannel = Channel.builder()
                .name(request.getName())
                .creatorId(creator)
                .counter(0)
                .createdAt(Instant.now())
                .build();

        Channel savedChannel = channelRepository.save(newChannel);

        // Add creator as a member
        ChatMemmbers creatorMember = ChatMemmbers.builder()
                .channel(savedChannel.getId())
                .user(creator.getId())
                .build();
        entityManager.persist(creatorMember);

        // Add other members
        if (request.getMemberIds() != null) {
            for (String memberId : request.getMemberIds()) {
                try {
                    UUID userId = UUID.fromString(memberId);
                    // Skip if creator is already in the list
                    if (userId.equals(creator.getId()))
                        continue;

                    ChatMemmbers member = ChatMemmbers.builder()
                            .channel(savedChannel.getId())
                            .user(userId)
                            .build();
                    entityManager.persist(member);
                } catch (IllegalArgumentException e) {
                    // Ignore invalid UUIDs
                }
            }
        }

        entityManager.flush();

        // Notify all members (creator + added members)
        ChannelSearchResponse channelInfo = ChannelSearchResponse.builder()
                .id(savedChannel.getId())
                .name(savedChannel.getName())
                .build();

        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "CHANNEL_ADDED");
        notification.put("channel", channelInfo);

        // Notify creator
        messagingTemplate.convertAndSend("/queue/updates-user-" + creator.getId(), notification);

        // Notify other members
        if (request.getMemberIds() != null) {
            for (String memberId : request.getMemberIds()) {
                try {
                    UUID userId = UUID.fromString(memberId);
                    if (!userId.equals(creator.getId())) {
                        messagingTemplate.convertAndSend("/queue/updates-user-" + userId, notification);
                    }
                } catch (IllegalArgumentException e) {
                    // Ignore
                }
            }
        }

        return savedChannel;
    }

    public List<ChannelSearchResponse> getUserChannels(UUID userId) {
        return channelRepository.findChannelsByUserId(userId).stream()
                .filter(c -> !c.getName().startsWith("1on1:"))
                .map(c -> ChannelSearchResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .build())
                .collect(Collectors.toList());
    }

    public List<UserSearchResponse> getUserConversations(UUID userId) {
        List<Channel> allChannels = channelRepository.findChannelsByUserId(userId);
        List<UserSearchResponse> conversations = new java.util.ArrayList<>();

        for (Channel channel : allChannels) {
            if (channel.getName().startsWith("1on1:")) {
                // Extract the other user ID from the channel name
                // Format: "1on1:userId1:userId2"
                String[] parts = channel.getName().split(":");
                if (parts.length == 3) {
                    String otherUserIdStr = parts[1].equals(userId.toString()) ? parts[2] : parts[1];
                    try {
                        UUID otherUserId = UUID.fromString(otherUserIdStr);
                        userRepository.findById(otherUserId).ifPresent(user -> {
                            conversations.add(UserSearchResponse.builder()
                                    .id(user.getId())
                                    .username(user.getUsername())
                                    .name(user.getName())
                                    .picture(user.getPicture())
                                    .build());
                        });
                    } catch (IllegalArgumentException e) {
                        // Ignore invalid UUIDs
                    }
                }
            }
        }
        return conversations;
    }

    @Transactional
    public void addMembersToChannel(Long channelId, List<String> memberIds) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found"));

        for (String memberId : memberIds) {
            try {
                UUID userId = UUID.fromString(memberId);

                // Check if already a member
                ChatMemmbers existingMember = entityManager.find(ChatMemmbers.class,
                        new com.example.security.model.Chatting.ChatMemberId(channelId, userId));

                if (existingMember == null) {
                    ChatMemmbers member = ChatMemmbers.builder()
                            .channel(channel.getId())
                            .user(userId)
                            .build();
                    entityManager.persist(member);
                }
            } catch (IllegalArgumentException e) {
                // Ignore invalid UUIDs
            }
        }
        entityManager.flush();

        // Notify new members
        ChannelSearchResponse channelInfo = ChannelSearchResponse.builder()
                .id(channel.getId())
                .name(channel.getName())
                .build();

        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "CHANNEL_ADDED");
        notification.put("channel", channelInfo);

        for (String memberId : memberIds) {
            try {
                UUID userId = UUID.fromString(memberId);
                // Only notify if they were actually added (or just notify anyway to be
                // safe/idempotent)
                // Ideally we track who was added, but notifying existing members is harmless
                // (they just ignore or update)
                messagingTemplate.convertAndSend("/queue/updates-user-" + userId, notification);
            } catch (IllegalArgumentException e) {
                // Ignore
            }
        }
    }

    @Transactional
    public void leaveChannel(Long channelId, UUID userId) {
        // We need to delete the member record.
        // Since ChatMemmbers has a composite key, we can try to find and remove it.
        // Or use a JPQL delete query.

        // Option 1: JPQL Delete (requires adding method to repository or using
        // entityManager)
        // Let's use entityManager for flexibility or add to repo.
        // Since we don't have a direct delete method in repo yet, let's try to fetch
        // and remove.

        // Actually, let's add a delete method to ChannelRepository or just use native
        // query here if needed.
        // But cleaner to use JPA.

        ChatMemmbers member = entityManager.find(ChatMemmbers.class,
                new com.example.security.model.Chatting.ChatMemberId(channelId, userId));
        if (member != null) {
            entityManager.remove(member);
            entityManager.flush();

            // Notify user to remove from sidebar
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "CHANNEL_REMOVED");
            notification.put("channelId", channelId);

            messagingTemplate.convertAndSend("/queue/updates-user-" + userId, notification);
        }
    }

    @Transactional
    public void deleteChannel(Long channelId, UUID requesterId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found"));

        // Check if requester is creator
        if (channel.getCreatorId() == null || !channel.getCreatorId().getId().equals(requesterId)) {
            throw new RuntimeException("Only the creator can delete this channel");
        }

        // Get all members to notify them later
        List<ChatMemmbers> members = channelRepository.findMembersByChannelId(channelId);

        // Delete all members first (if cascade is not set)
        // JPQL: DELETE FROM ChatMemmbers c WHERE c.channel = :id
        // Since we are in a transaction, we can just delete the channel if
        // CascadeType.ALL is set on members list in Channel entity.
        // But Channel entity doesn't seem to have the OneToMany list mapped.
        // So we must delete members manually.

        for (ChatMemmbers m : members) {
            entityManager.remove(m);
        }

        // Delete channel
        channelRepository.delete(channel);
        entityManager.flush();

        // Notify all members
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "CHANNEL_REMOVED");
        notification.put("channelId", channelId);

        for (ChatMemmbers member : members) {
            messagingTemplate.convertAndSend("/queue/updates-user-" + member.getUser(), notification);
        }
    }
}
