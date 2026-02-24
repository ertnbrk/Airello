# DTO Guidelines - Quick Reference

## ❌ DON'T - Never Do This

### Controllers Returning Entities:
```java
// ❌ WRONG - Will cause Hibernate proxy errors
@GetMapping("/items")
public List<Item> getItems() {
    return itemRepository.findAll();  // Returns entities with lazy proxies
}

// ❌ WRONG - Will cause 500 errors
@PostMapping("/items")
public Item createItem(@RequestBody CreateItemRequest request) {
    return itemService.save(request);  // Returns entity
}
```

### WebSocket Broadcasting Entities:
```java
// ❌ WRONG - Will crash WebSocket
Item item = itemRepository.save(newItem);
realtimeEventService.broadcast(
    RealtimeEvent.of("ITEM_CREATED", item)  // Entity with proxies!
);
```

---

## ✅ DO - Always Do This

### Controllers Returning DTOs:
```java
// ✅ CORRECT - Returns safe DTO
@GetMapping("/items")
public List<ItemDto> getItems() {
    return itemRepository.findAll().stream()
        .map(ItemDto::fromEntity)
        .toList();
}

// ✅ CORRECT - Maps entity to DTO
@PostMapping("/items")
public ItemDto createItem(@RequestBody CreateItemRequest request) {
    Item item = itemService.save(request);
    return ItemDto.fromEntity(item);
}
```

### WebSocket Broadcasting DTOs:
```java
// ✅ CORRECT - Broadcasts safe DTO
Item item = itemRepository.save(newItem);
realtimeEventService.broadcast(
    RealtimeEvent.of("ITEM_CREATED", ItemDto.fromEntity(item))
);
```

---

## Creating a New DTO

### Template:
```java
package com.example.dto;

import java.time.Instant;
import java.util.UUID;
import com.example.entity.YourEntity;

/**
 * DTO for YourEntity - safe for REST/WebSocket serialization.
 * Contains only primitive/simple fields, no entity references.
 */
public record YourEntityDto(
        UUID id,
        String name,
        UUID foreignKeyId,  // NOT the entity, just the ID
        Instant createdAt) {

    /**
     * Creates a DTO from an entity.
     * Extracts only IDs without triggering lazy proxy initialization.
     */
    public static YourEntityDto fromEntity(YourEntity entity) {
        return new YourEntityDto(
                entity.getId(),
                entity.getName(),
                entity.getRelation().getId(),  // Safe: getId() doesn't trigger proxy
                entity.getCreatedAt());
    }
}
```

### Rules:
1. Use Java `record` (immutable)
2. Only primitive types, String, UUID, Instant, enums
3. **Never** include entity references (Project, User, etc.)
4. Extract IDs from relationships: `entity.getProject().getId()`
5. Add static `fromEntity()` factory method
6. Convert backend enums to frontend strings if needed

---

## Common Patterns

### Pattern 1: Simple Entity with Foreign Keys
```java
// Entity
@Entity
class Comment {
    @Id private UUID id;
    private String text;
    @ManyToOne(lazy) private Post post;
    @ManyToOne(lazy) private User author;
}

// DTO
public record CommentDto(
    UUID id,
    String text,
    UUID postId,      // NOT Post entity
    UUID authorId) {  // NOT User entity

    public static CommentDto fromEntity(Comment comment) {
        return new CommentDto(
            comment.getId(),
            comment.getText(),
            comment.getPost().getId(),    // Extract ID only
            comment.getAuthor().getId()); // Extract ID only
    }
}
```

### Pattern 2: Entity with Enums
```java
// Entity
@Entity
class Task {
    @Id private UUID id;
    private String title;
    @Enumerated(STRING) private TaskStatus status;
    @Enumerated(STRING) private Priority priority;
}

// DTO
public record TaskDto(
    UUID id,
    String title,
    String status,   // Frontend format: "in-progress"
    String priority  // Frontend format: "high"
) {
    public static TaskDto fromEntity(Task task) {
        return new TaskDto(
            task.getId(),
            task.getTitle(),
            EnumMapper.toFrontendStatus(task.getStatus()),
            EnumMapper.toFrontendPriority(task.getPriority()));
    }
}
```

### Pattern 3: Collections
```java
// Controller
@GetMapping("/posts")
public List<PostDto> getPosts() {
    return postRepository.findAll().stream()
        .map(PostDto::fromEntity)
        .toList();
}
```

---

## Testing Your DTO

### Required Test:
```java
@Test
void testJsonSerializationNoNestedEntities() throws Exception {
    // Given
    YourEntityDto dto = new YourEntityDto(...);

    // When
    ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());
    String json = mapper.writeValueAsString(dto);

    // Then - verify no nested objects
    assertFalse(json.contains("\"entity\":{"));
    assertFalse(json.contains("hibernateLazyInitializer"));
    assertFalse(json.contains("ByteBuddyInterceptor"));
}
```

---

## Checklist Before Committing

- [ ] Created DTO record in `*.dto` package
- [ ] DTO only contains primitives/String/UUID/Instant
- [ ] No entity references, only IDs
- [ ] Added `fromEntity()` static method
- [ ] Controller returns DTO, not entity
- [ ] WebSocket broadcasts DTO, not entity
- [ ] Written serialization test
- [ ] Test passes with no Hibernate proxy errors
- [ ] Code formatted with `./gradlew spotlessApply`

---

## When You See This Error:

```
No serializer found for class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor
```

**Cause:** You're returning or broadcasting an entity with lazy relationships.

**Fix:**
1. Create a DTO for the entity
2. Map entity to DTO in controller: `return EntityDto.fromEntity(entity)`
3. For WebSocket: `RealtimeEvent.of("EVENT", EntityDto.fromEntity(entity))`

---

## Quick Commands

```bash
# Format code
./gradlew spotlessApply

# Run DTO tests
./gradlew test --tests "*.dto.*DtoTest"

# Compile
./gradlew compileJava

# Full build
./gradlew build
```

---

## Examples in Codebase

### ✅ Good Examples:
- `BoardColumnDto` - Board column for REST and WebSocket
- `EpicDto` - Epic with enum mapping
- `IssueResponse` - Issue with full details
- `UserDto` - User without sensitive fields
- `ChatMessageResponse` - Chat message DTO

### 📚 Refer To:
- `BoardController.java` - Controller using DTOs
- `AgileController.java` - Controller with enum mapping
- `BoardService.java` - WebSocket broadcast with DTOs
- `BoardColumnDtoTest.java` - DTO test example
- `EpicDtoTest.java` - DTO test with enums

---

**Last Updated:** 2024-01-15
