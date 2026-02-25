package ai.planmate.agile.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ai.planmate.agile.entity.Epic;
import ai.planmate.agile.entity.IssuePriority;
import ai.planmate.agile.entity.IssueStatus;
import ai.planmate.auth.entity.AppUser;
import ai.planmate.projects.entity.Project;

/**
 * Unit test for EpicDto to ensure safe REST/WebSocket serialization. Verifies that the DTO doesn't
 * contain nested entity references or Hibernate proxies.
 */
class EpicDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void testFromEntityMapsAllFields() {
        // Given
        UUID projectId = UUID.randomUUID();
        UUID epicId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Project project = new Project();
        project.setId(projectId);

        AppUser user = new AppUser();
        user.setId(userId);

        Epic epic = new Epic();
        epic.setId(epicId);
        epic.setProject(project);
        epic.setTitle("User Authentication Epic");
        epic.setDescription("Complete user auth system");
        epic.setKey("AUTH-1");
        epic.setPriority(IssuePriority.HIGH);
        epic.setStatus(IssueStatus.IN_PROGRESS);
        epic.setCreatedBy(user);
        epic.setCreatedAt(Instant.now());
        epic.setUpdatedAt(Instant.now());

        // When
        EpicDto dto = EpicDto.fromEntity(epic);

        // Then
        assertNotNull(dto);
        assertEquals(epicId, dto.id());
        assertEquals(projectId, dto.projectId());
        assertEquals("User Authentication Epic", dto.title());
        assertEquals("Complete user auth system", dto.description());
        assertEquals("AUTH-1", dto.key());
        assertEquals("high", dto.priority());
        assertEquals("in-progress", dto.status());
        assertEquals(userId, dto.createdBy());
        assertNotNull(dto.createdAt());
        assertNotNull(dto.updatedAt());
    }

    @Test
    void testJsonSerializationNoNestedEntities() throws Exception {
        // Given
        UUID projectId = UUID.randomUUID();
        UUID epicId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        EpicDto dto =
                new EpicDto(
                        epicId,
                        projectId,
                        "Epic Title",
                        "Epic Description",
                        "EPIC-1",
                        "high",
                        "in-progress",
                        userId,
                        Instant.now(),
                        Instant.now());

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("\"id\":\"" + epicId + "\""));
        assertTrue(json.contains("\"projectId\":\"" + projectId + "\""));
        assertTrue(json.contains("\"title\":\"Epic Title\""));
        assertTrue(json.contains("\"key\":\"EPIC-1\""));
        assertTrue(json.contains("\"priority\":\"high\""));
        assertTrue(json.contains("\"status\":\"in-progress\""));
        assertTrue(json.contains("\"createdBy\":\"" + userId + "\""));

        // Verify no nested objects (no "project": {}, "createdBy": {})
        assertFalse(json.contains("\"project\":{"));
        assertFalse(json.contains("\"workspace\""));
        assertFalse(json.contains("\"owner\""));
        assertFalse(json.contains("hibernateLazyInitializer"));
        assertFalse(json.contains("ByteBuddyInterceptor"));
    }

    @Test
    void testJsonDeserializationRoundTrip() throws Exception {
        // Given
        UUID projectId = UUID.randomUUID();
        UUID epicId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        EpicDto original =
                new EpicDto(
                        epicId,
                        projectId,
                        "Test Epic",
                        "Description",
                        "TEST-1",
                        "medium",
                        "backlog",
                        userId,
                        now,
                        now);

        // When
        String json = objectMapper.writeValueAsString(original);
        EpicDto deserialized = objectMapper.readValue(json, EpicDto.class);

        // Then
        assertEquals(original.id(), deserialized.id());
        assertEquals(original.projectId(), deserialized.projectId());
        assertEquals(original.title(), deserialized.title());
        assertEquals(original.description(), deserialized.description());
        assertEquals(original.key(), deserialized.key());
        assertEquals(original.priority(), deserialized.priority());
        assertEquals(original.status(), deserialized.status());
        assertEquals(original.createdBy(), deserialized.createdBy());
    }

    @Test
    void testRecordImmutability() {
        // Given
        UUID projectId = UUID.randomUUID();
        UUID epicId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // When
        EpicDto dto =
                new EpicDto(
                        epicId,
                        projectId,
                        "Title",
                        "Desc",
                        "KEY-1",
                        "low",
                        "done",
                        userId,
                        Instant.now(),
                        Instant.now());

        // Then - verify all fields are accessible
        assertNotNull(dto.id());
        assertNotNull(dto.projectId());
        assertNotNull(dto.title());
        assertNotNull(dto.description());
        assertNotNull(dto.key());
        assertNotNull(dto.priority());
        assertNotNull(dto.status());
        assertNotNull(dto.createdBy());
        assertNotNull(dto.createdAt());
        assertNotNull(dto.updatedAt());

        // Records are immutable - no setters should exist
        // This is enforced by the Java compiler
    }
}
