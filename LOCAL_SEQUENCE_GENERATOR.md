# Local Sequence Generator for Message IDs

## Overview

This implementation provides a **local sequence number generator** for message IDs in the chat application. Unlike global ID generators (like Snowflake), this approach generates IDs that are **unique within each conversation/channel** rather than globally unique across the entire system.

## Why Local Sequence Generator?

### Advantages

1. **Simpler Implementation**: No need for distributed coordination or centralized ID services
2. **Better Performance**: Thread-safe local atomic operations are faster than distributed systems
3. **Sufficient Uniqueness**: For chat applications, message order only matters within a conversation
4. **Natural Partitioning**: Each channel maintains its own sequence, making it easier to scale

### Requirements Met

✅ **Unique IDs**: Each message in a channel has a unique sequence number  
✅ **Time Sortable**: New messages have higher IDs than older ones  
✅ **Thread Safe**: Uses ConcurrentHashMap and AtomicLong for concurrent access  
✅ **Channel Isolation**: Each conversation maintains independent sequences

## Architecture

### Components

1. **MessageSequenceGenerator**: Core service that generates sequence numbers
2. **Message Model**: Updated to include `channelId`, `messageId`, and `sequenceNumber`
3. **ChatService**: Integrated with sequence generator for message creation
4. **ChatRepository**: Enhanced with channel-based queries

### Message ID Structure

#### Option 1: Simple Sequence (Basic)
```
messageId = sequenceNumber (within channel)
Example: 1, 2, 3, 4, ...
```

#### Option 2: Composite ID (Recommended)
```
messageId = (timestamp << 22) | (sequenceNumber & 0x3FFFFF)
        
Format: [Timestamp: 41 bits][Sequence: 22 bits]
Example: 4503599627370497 (timestamp: 1073741824ms, sequence: 1)
```

**Benefits of Composite ID**:
- Better time-sortability across channels
- Allows up to ~4 million messages per channel per millisecond
- 63-bit total (fits in Long type)
- Can extract timestamp: `messageId >> 22`
- Can extract sequence: `messageId & 0x3FFFFF`

## Channel ID Patterns

### One-on-One Conversations
```java
channelId = "1on1:user1:user2"
```
- User IDs are sorted lexicographically for consistency
- `generateOneOnOneChannelId("alice", "bob")` = `generateOneOnOneChannelId("bob", "alice")`

### Group Conversations
```java
channelId = "group:groupId"
```
- Uses the group's unique identifier
- Example: `"group:team-alpha"`

### Public/Broadcast
```java
channelId = "group:public"
```
- For messages sent to all users

## Usage Examples

### Creating a Message

```java
@Autowired
private MessageSequenceGenerator sequenceGenerator;

@Autowired
private ChatRepository chatRepository;

public void sendMessage(String senderId, String receiverId, String content) {
    // Generate channel ID
    String channelId = sequenceGenerator.generateOneOnOneChannelId(senderId, receiverId);
    
    // Generate sequence number
    long sequenceNumber = sequenceGenerator.generateMessageId(channelId);
    
    // Generate composite ID
    long messageId = sequenceGenerator.generateCompositeMessageId(channelId);
    
    // Create message
    Message message = Message.builder()
        .messageId(messageId)
        .channelId(channelId)
        .sequenceNumber(sequenceNumber)
        .sender(senderId)
        .receiver(receiverId)
        .content(content)
        .createdAt(Instant.now())
        .build();
    
    chatRepository.save(message);
}
```

### Querying Messages in Order

```java
// Get all messages in a conversation, ordered by sequence
List<Message> messages = chatRepository.findByChannelIdOrderBySequenceNumberAsc(channelId);

// Get new messages after a specific sequence (for pagination/sync)
List<Message> newMessages = chatRepository
    .findByChannelIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
        channelId, 
        lastReceivedSequence
    );
```

### Client-Side Message Synchronization

```java
// Client stores the last received sequence number per channel
Long lastSequence = clientStorage.get(channelId);

// Request only new messages
List<Message> updates = chatRepository
    .findByChannelIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
        channelId,
        lastSequence
    );

// Update local storage
if (!updates.isEmpty()) {
    Long newLastSequence = updates.get(updates.size() - 1).getSequenceNumber();
    clientStorage.put(channelId, newLastSequence);
}
```

## Database Schema

```sql
CREATE TABLE messages (
    message_id BIGINT PRIMARY KEY,
    channel_id VARCHAR(255) NOT NULL,
    sequence_number BIGINT NOT NULL,
    content TEXT,
    sender VARCHAR(255),
    receiver VARCHAR(255),
    created_at TIMESTAMP,
    
    INDEX idx_channel_sequence (channel_id, sequence_number),
    UNIQUE KEY uk_channel_sequence (channel_id, sequence_number)
);
```

**Key Indexes**:
- `(channel_id, sequence_number)`: For ordered retrieval within channels
- Unique constraint ensures no duplicate sequences within a channel

## Thread Safety

The implementation uses:
- `ConcurrentHashMap`: Thread-safe map for storing channel sequences
- `AtomicLong`: Atomic increment operations for each sequence counter
- `computeIfAbsent`: Atomic "get-or-create" operation

This ensures correct behavior even with:
- Multiple users sending messages to the same channel simultaneously
- High-concurrency scenarios with many concurrent requests

## Scalability Considerations

### Horizontal Scaling

For distributed deployments (multiple application instances):

1. **Database-backed sequences** (current approach with some modification):
   ```sql
   -- Use database to store and increment sequences
   UPDATE channel_sequences 
   SET current_sequence = current_sequence + 1 
   WHERE channel_id = ?
   RETURNING current_sequence;
   ```

2. **Redis-backed sequences**:
   ```java
   Long sequence = redisTemplate.opsForValue()
       .increment("channel:sequence:" + channelId);
   ```

3. **Sticky sessions**: Route all messages for a channel to the same instance

### Current In-Memory Limitations

The current implementation stores sequences in memory (`ConcurrentHashMap`):
- ✅ Fast and simple
- ✅ Perfect for single-instance deployments
- ⚠️ Sequences reset on application restart
- ⚠️ Not shared across multiple instances

**For production with multiple instances**, consider using Redis or database-backed sequences.

## Migration from Auto-Increment IDs

If migrating from the old `AUTO_INCREMENT` IDs:

```java
// Option 1: Initialize sequence from max existing ID
Long maxSequence = chatRepository.findMaxSequenceByChannelId(channelId);
if (maxSequence != null) {
    sequenceGenerator.initializeSequence(channelId, maxSequence);
}

// Option 2: Dual-write during migration
Message message = Message.builder()
    .id(legacyAutoIncrementId)      // Old global ID
    .messageId(compositeId)          // New composite ID
    .sequenceNumber(sequenceNumber)  // New sequence
    // ...
    .build();
```

## Testing

Run the test suite:
```bash
./gradlew test --tests MessageSequenceGeneratorTest
```

Tests cover:
- Sequential ID generation
- Channel isolation
- One-on-one channel ID consistency
- Time-ordered composite IDs
- Group channel IDs

## Comparison with Alternatives

| Approach | Uniqueness | Time-Sortable | Implementation | Scalability |
|----------|-----------|---------------|----------------|-------------|
| **Local Sequence** | ✅ Per-channel | ✅ Yes | ⭐⭐⭐⭐⭐ Simple | ⭐⭐⭐ Good |
| **Snowflake** | ✅ Global | ✅ Yes | ⭐⭐⭐ Complex | ⭐⭐⭐⭐⭐ Excellent |
| **UUID** | ✅ Global | ❌ No | ⭐⭐⭐⭐⭐ Simple | ⭐⭐⭐⭐⭐ Excellent |
| **Auto-Increment** | ✅ Global | ✅ Yes | ⭐⭐⭐⭐ Easy | ⭐⭐ Limited |

## Best Practices

1. **Always use channelId**: Never compare message IDs across different channels
2. **Use sequenceNumber for ordering**: It's simpler and guaranteed sequential
3. **Use messageId for uniqueness**: Composite ID ensures global uniqueness if needed
4. **Index properly**: Always index `(channel_id, sequence_number)` together
5. **Handle gaps**: Sequence gaps are normal (failed transactions, rollbacks)

## Future Enhancements

- [ ] Persistent sequence storage (Redis/Database)
- [ ] Sequence gap detection and filling
- [ ] Message acknowledgment tracking
- [ ] Batch ID generation for bulk inserts
- [ ] Sequence compaction for archived channels
