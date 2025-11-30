# Message ID Local Sequence Generator - Usage Examples

## API Endpoints

### 1. Send a Message (One-on-One)

**HTTP Request:**
```http
POST /api/chat/send
Content-Type: application/json

{
  "content": "Hello, how are you?",
  "sender": "alice-user-id",
  "receiver": "bob-user-id"
}
```

**What Happens:**
1. System generates channelId: `"1on1:alice-user-id:bob-user-id"`
2. Generates sequence number: `1` (or next available)
3. Generates composite messageId: `4503599627370497` (timestamp + sequence)
4. Stores message with all IDs
5. Broadcasts to WebSocket subscribers

### 2. Send a Group/Public Message

**HTTP Request:**
```http
POST /api/chat/send
Content-Type: application/json

{
  "content": "Hello everyone!",
  "sender": "alice-user-id",
  "receiver": null
}
```

**What Happens:**
1. System generates channelId: `"group:public"` (since receiver is null)
2. Sequence is maintained per group channel
3. All group members can see messages in order

### 3. Get Conversation History

**HTTP Request:**
```http
GET /api/chat/conversation?userId1=alice-user-id&userId2=bob-user-id
```

**Response:**
```json
[
  {
    "content": "Hello!",
    "senderId": "alice-user-id",
    "senderName": "Alice Smith",
    "senderAvatar": "https://example.com/alice.jpg"
  },
  {
    "content": "Hi Alice!",
    "senderId": "bob-user-id",
    "senderName": "Bob Jones",
    "senderAvatar": "https://example.com/bob.jpg"
  }
]
```

**Note:** Messages are automatically ordered by sequence number.

### 4. Get Messages by Channel ID

**HTTP Request:**
```http
GET /api/chat/channel/1on1:alice-user-id:bob-user-id
```

**Use Case:** When you already know the channel ID.

### 5. Sync New Messages (Polling)

**HTTP Request:**
```http
GET /api/chat/channel/1on1:alice-user-id:bob-user-id/updates?afterSequence=42
```

**Response:** Returns only messages with sequence > 42

**Use Case:** Client periodically polls for new messages:
```javascript
let lastSequence = localStorage.getItem('lastSequence') || 0;

async function syncMessages() {
  const response = await fetch(
    `/api/chat/channel/${channelId}/updates?afterSequence=${lastSequence}`
  );
  const newMessages = await response.json();
  
  if (newMessages.length > 0) {
    displayMessages(newMessages);
    lastSequence = newMessages[newMessages.length - 1].sequenceNumber;
    localStorage.setItem('lastSequence', lastSequence);
  }
}

// Poll every 2 seconds
setInterval(syncMessages, 2000);
```

## WebSocket Usage

### Connect and Subscribe

```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
  console.log('Connected: ' + frame);
  
  // Subscribe to chat topic
  stompClient.subscribe('/topic/chat', function(message) {
    const messages = JSON.parse(message.body);
    displayMessages(messages);
  });
});
```

### Send Message via WebSocket

```javascript
function sendMessage() {
  const message = {
    content: document.getElementById('messageInput').value,
    sender: currentUserId,
    receiver: recipientUserId
  };
  
  stompClient.send('/app/chat.send', {}, JSON.stringify(message));
}
```

## Java/Spring Service Usage

### In Your Service or Component

```java
@Service
public class MyService {
    
    @Autowired
    private MessageSequenceGenerator sequenceGenerator;
    
    @Autowired
    private ChatRepository chatRepository;
    
    public void sendDirectMessage(String fromUserId, String toUserId, String content) {
        // Generate channel ID
        String channelId = sequenceGenerator.generateOneOnOneChannelId(fromUserId, toUserId);
        
        // Generate IDs
        long sequenceNumber = sequenceGenerator.generateMessageId(channelId);
        long messageId = sequenceGenerator.generateCompositeMessageId(channelId);
        
        // Create and save message
        Message message = Message.builder()
            .messageId(messageId)
            .channelId(channelId)
            .sequenceNumber(sequenceNumber)
            .sender(fromUserId)
            .receiver(toUserId)
            .content(content)
            .createdAt(Instant.now())
            .build();
        
        chatRepository.save(message);
    }
    
    public void sendGroupMessage(String groupId, String senderId, String content) {
        String channelId = sequenceGenerator.generateGroupChannelId(groupId);
        
        long sequenceNumber = sequenceGenerator.generateMessageId(channelId);
        long messageId = sequenceGenerator.generateCompositeMessageId(channelId);
        
        Message message = Message.builder()
            .messageId(messageId)
            .channelId(channelId)
            .sequenceNumber(sequenceNumber)
            .sender(senderId)
            .content(content)
            .createdAt(Instant.now())
            .build();
        
        chatRepository.save(message);
    }
}
```

## Frontend Integration Examples

### React Component

```javascript
import React, { useState, useEffect } from 'react';
import axios from 'axios';

function ChatWindow({ userId, recipientId }) {
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  
  // Load conversation on mount
  useEffect(() => {
    loadConversation();
  }, [userId, recipientId]);
  
  const loadConversation = async () => {
    const response = await axios.get('/api/chat/conversation', {
      params: { userId1: userId, userId2: recipientId }
    });
    setMessages(response.data);
  };
  
  const sendMessage = async () => {
    await axios.post('/api/chat/send', {
      content: newMessage,
      sender: userId,
      receiver: recipientId
    });
    setNewMessage('');
    loadConversation(); // Refresh messages
  };
  
  return (
    <div>
      <div className="messages">
        {messages.map((msg, index) => (
          <div key={index} className={msg.senderId === userId ? 'sent' : 'received'}>
            <strong>{msg.senderName}:</strong> {msg.content}
          </div>
        ))}
      </div>
      <input 
        value={newMessage} 
        onChange={(e) => setNewMessage(e.target.value)}
        placeholder="Type a message..."
      />
      <button onClick={sendMessage}>Send</button>
    </div>
  );
}
```

### Vue.js Component

```vue
<template>
  <div class="chat-window">
    <div class="messages">
      <div 
        v-for="(msg, index) in messages" 
        :key="index"
        :class="msg.senderId === userId ? 'sent' : 'received'"
      >
        <strong>{{ msg.senderName }}:</strong> {{ msg.content }}
      </div>
    </div>
    <input 
      v-model="newMessage" 
      @keyup.enter="sendMessage"
      placeholder="Type a message..."
    />
    <button @click="sendMessage">Send</button>
  </div>
</template>

<script>
export default {
  props: ['userId', 'recipientId'],
  data() {
    return {
      messages: [],
      newMessage: ''
    };
  },
  mounted() {
    this.loadConversation();
  },
  methods: {
    async loadConversation() {
      const response = await this.$http.get('/api/chat/conversation', {
        params: { 
          userId1: this.userId, 
          userId2: this.recipientId 
        }
      });
      this.messages = response.data;
    },
    async sendMessage() {
      await this.$http.post('/api/chat/send', {
        content: this.newMessage,
        sender: this.userId,
        receiver: this.recipientId
      });
      this.newMessage = '';
      this.loadConversation();
    }
  }
};
</script>
```

## Advanced Usage Patterns

### 1. Message Pagination

```java
@GetMapping("/channel/{channelId}/paginated")
public ResponseEntity<List<MessageResponse>> getPaginatedMessages(
        @PathVariable String channelId,
        @RequestParam(defaultValue = "0") Long fromSequence,
        @RequestParam(defaultValue = "50") int limit) {
    
    List<Message> messages = chatRepository
        .findByChannelIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
            channelId, fromSequence);
    
    // Limit results
    List<MessageResponse> limited = messages.stream()
        .limit(limit)
        .map(this::convertToMessageResponse)
        .collect(Collectors.toList());
    
    return ResponseEntity.ok(limited);
}
```

### 2. Message Gap Detection

```java
public List<Long> detectMissingSequences(String channelId) {
    List<Message> messages = chatRepository
        .findByChannelIdOrderBySequenceNumberAsc(channelId);
    
    List<Long> missingSequences = new ArrayList<>();
    
    for (int i = 1; i < messages.size(); i++) {
        long current = messages.get(i).getSequenceNumber();
        long previous = messages.get(i - 1).getSequenceNumber();
        
        if (current - previous > 1) {
            // Gap detected
            for (long seq = previous + 1; seq < current; seq++) {
                missingSequences.add(seq);
            }
        }
    }
    
    return missingSequences;
}
```

### 3. Bulk Message Insert

```java
public void sendBulkMessages(String channelId, List<String> contents, String senderId) {
    List<Message> messages = contents.stream()
        .map(content -> {
            long seq = sequenceGenerator.generateMessageId(channelId);
            long msgId = sequenceGenerator.generateCompositeMessageId(channelId);
            
            return Message.builder()
                .messageId(msgId)
                .channelId(channelId)
                .sequenceNumber(seq)
                .sender(senderId)
                .content(content)
                .createdAt(Instant.now())
                .build();
        })
        .collect(Collectors.toList());
    
    chatRepository.saveAll(messages);
}
```

## Testing Examples

### Unit Test

```java
@Test
public void testMessageCreationWithSequence() {
    String senderId = "user1";
    String receiverId = "user2";
    String content = "Test message";
    
    // Send first message
    MessageRequest request1 = MessageRequest.builder()
        .sender(senderId)
        .receiver(receiverId)
        .content(content)
        .build();
    chatService.addMessage(request1);
    
    // Send second message
    MessageRequest request2 = MessageRequest.builder()
        .sender(senderId)
        .receiver(receiverId)
        .content("Second message")
        .build();
    chatService.addMessage(request2);
    
    // Verify messages have sequential IDs
    List<MessageResponse> messages = chatService.getConversationMessages(senderId, receiverId);
    assertEquals(2, messages.size());
}
```

### Integration Test with REST

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ChatControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testSendAndRetrieveMessage() throws Exception {
        String messageJson = """
            {
                "content": "Hello World",
                "sender": "user1",
                "receiver": "user2"
            }
            """;
        
        // Send message
        mockMvc.perform(post("/api/chat/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(messageJson))
            .andExpect(status().isOk());
        
        // Retrieve messages
        mockMvc.perform(get("/api/chat/conversation")
                .param("userId1", "user1")
                .param("userId2", "user2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].content").value("Hello World"));
    }
}
```

## Troubleshooting

### Issue: Sequence numbers reset after restart

**Cause:** In-memory storage resets on restart.

**Solution:** Implement persistent storage (Redis or Database):

```java
@Service
public class PersistentSequenceGenerator {
    
    @Autowired
    private RedisTemplate<String, Long> redisTemplate;
    
    public long generateMessageId(String channelId) {
        String key = "channel:sequence:" + channelId;
        return redisTemplate.opsForValue().increment(key);
    }
}
```

### Issue: Sequence gaps in messages

**Cause:** Transaction rollbacks or failed saves.

**Solution:** This is normal and expected. Messages are ordered by sequence, gaps don't affect functionality.

### Issue: Messages out of order

**Cause:** Not using `sequenceNumber` for ordering.

**Solution:** Always order by `sequenceNumber`, not by `messageId` or `createdAt`:

```java
// ❌ Wrong
messages.sort(Comparator.comparing(Message::getCreatedAt));

// ✅ Correct
messages.sort(Comparator.comparing(Message::getSequenceNumber));

// ✅ Better: Use database ordering
repository.findByChannelIdOrderBySequenceNumberAsc(channelId);
```
