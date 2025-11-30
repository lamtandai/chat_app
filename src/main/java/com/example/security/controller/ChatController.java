package com.example.security.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.security.dto.MessageResponse;
import com.example.security.dto.SendingMessageRequest;
import com.example.security.dto.ChannelSearchResponse;
import com.example.security.dto.UserSearchResponse;
import com.example.security.dto.CreateChannelRequest;
import com.example.security.dto.UserPrincipal;
import com.example.security.model.Chatting.Channel;
import com.example.security.model.User;
import com.example.security.service.ChatService;
import java.security.Principal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * REST Controller for chat/messaging functionality.
 * Supports both HTTP REST endpoints and WebSocket messaging.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    /**
     * Send a message via HTTP POST.
     * 
     * @param request Message details (content, sender, receiver)
     * @return Success response
     */
    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestBody SendingMessageRequest request) {
        chatService.addMessage(request);
        return ResponseEntity.ok("Message sent successfully");
    }

    /**
     * Get all messages for a specific channel.
     * 
     * @param channelId The channel identifier
     * @return List of messages in the channel
     */
    @GetMapping("/channel/{channelId}")
    public ResponseEntity<List<MessageResponse>> getChannelMessages(
            @PathVariable("channelId") Long channelId) {
        List<MessageResponse> messages = chatService.getChannelMessages(channelId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get messages in a one-on-one conversation between two users.
     * 
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return List of messages in the conversation
     */
    @GetMapping("/conversation")
    public ResponseEntity<List<MessageResponse>> getConversation(
            @RequestParam("userId1") String userId1,
            @RequestParam("userId2") String userId2) {
        List<MessageResponse> messages = chatService.getConversationMessages(userId1, userId2);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get new messages in a channel after a specific sequence number.
     * Useful for message synchronization and real-time updates.
     * 
     * @param channelId     The channel identifier
     * @param afterSequence The sequence number after which to fetch messages
     * @return List of new messages
     */
    @GetMapping("/channel/{channelId}/updates")
    public ResponseEntity<List<MessageResponse>> getNewMessages(
            @PathVariable("channelId") Long channelId,
            @RequestParam("afterSequence") Long afterSequence) {
        List<MessageResponse> messages = chatService.getNewMessages(channelId, afterSequence);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get initial messages (latest 15) for a channel.
     * 
     * @param channelId The channel identifier
     * @return List of latest messages
     */
    @GetMapping("/channel/{channelId}/initial")
    public ResponseEntity<List<MessageResponse>> getInitialMessages(
            @PathVariable("channelId") Long channelId) {
        List<MessageResponse> messages = chatService.getInitialMessages(channelId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get older messages before a specific sequence number.
     * 
     * @param channelId      The channel identifier
     * @param beforeSequence The sequence number before which to fetch messages
     * @return List of older messages
     */
    @GetMapping("/channel/{channelId}/history")
    public ResponseEntity<List<MessageResponse>> getOlderMessages(
            @PathVariable("channelId") Long channelId,
            @RequestParam("beforeSequence") Long beforeSequence) {
        List<MessageResponse> messages = chatService.getOlderMessages(channelId, beforeSequence);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/channels/search")
    public ResponseEntity<List<ChannelSearchResponse>> searchChannels(@RequestParam("query") String query) {
        return ResponseEntity.ok(chatService.searchChannels(query));
    }

    @PostMapping("/channel/create")
    public ResponseEntity<Channel> createChannel(@RequestBody CreateChannelRequest request) {
        Channel channel = chatService.createChannel(request);
        return ResponseEntity.ok(channel);
    }

    @PostMapping("/channel/{channelId}/add-members")
    public ResponseEntity<String> addMembers(@PathVariable("channelId") Long channelId,
            @RequestBody List<String> memberIds) {
        chatService.addMembersToChannel(channelId, memberIds);
        return ResponseEntity.ok("Members added successfully");
    }

    @GetMapping("/user/channels")
    public ResponseEntity<List<ChannelSearchResponse>> getUserChannels(Principal connectedUser) {
        UserPrincipal user = (UserPrincipal) ((UsernamePasswordAuthenticationToken) connectedUser).getPrincipal();
        return ResponseEntity.ok(chatService.getUserChannels(user.getId()));
    }

    @GetMapping("/user/conversations")
    public ResponseEntity<List<UserSearchResponse>> getUserConversations(Principal connectedUser) {
        UserPrincipal user = (UserPrincipal) ((UsernamePasswordAuthenticationToken) connectedUser).getPrincipal();
        return ResponseEntity.ok(chatService.getUserConversations(user.getId()));
    }

    @PostMapping("/channel/{channelId}/leave")
    public ResponseEntity<String> leaveChannel(@PathVariable("channelId") Long channelId, Principal connectedUser) {
        UserPrincipal user = (UserPrincipal) ((UsernamePasswordAuthenticationToken) connectedUser).getPrincipal();
        chatService.leaveChannel(channelId, user.getId());
        return ResponseEntity.ok("Left channel successfully");
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/channel/{channelId}")
    public ResponseEntity<String> deleteChannel(@PathVariable("channelId") Long channelId, Principal connectedUser) {
        UserPrincipal user = (UserPrincipal) ((UsernamePasswordAuthenticationToken) connectedUser).getPrincipal();
        chatService.deleteChannel(channelId, user.getId());
        return ResponseEntity.ok("Channel deleted successfully");
    }
}
