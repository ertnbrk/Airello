package ai.planmate.agile.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.planmate.agile.entity.BoardColumn;
import ai.planmate.projects.entity.Project;

/**
 * Unit test for BoardColumnDto to ensure safe WebSocket/STOMP serialization. Verifies that the DTO
 * doesn't contain nested entity references or Hibernate proxies.
 */
class BoardColumnDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testFromEntityMapsAllFields() {
        // Given
        UUID projectId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        Project project = new Project();
        project.setId(projectId);

        BoardColumn column = new BoardColumn();
        column.setId(columnId);
        column.setProject(project);
        column.setName("In Progress");
        column.setPosition(2);
        column.setIsDefault(false);
        column.setCreatedAt(Instant.now());
        column.setUpdatedAt(Instant.now());

        // When
        BoardColumnDto dto = BoardColumnDto.fromEntity(column);

        // Then
        assertNotNull(dto);
        assertEquals(columnId, dto.id());
        assertEquals("In Progress", dto.name());
        assertEquals(2, dto.position());
        assertFalse(dto.isDefault());
        assertEquals(projectId, dto.projectId());
    }

    @Test
    void testFromEntityHandlesDefaultColumn() {
        // Given
        UUID projectId = UUID.randomUUID();

        Project project = new Project();
        project.setId(projectId);

        BoardColumn column = new BoardColumn();
        column.setId(UUID.randomUUID());
        column.setProject(project);
        column.setName("Backlog");
        column.setPosition(0);
        column.setIsDefault(true);

        // When
        BoardColumnDto dto = BoardColumnDto.fromEntity(column);

        // Then
        assertTrue(dto.isDefault());
        assertEquals("Backlog", dto.name());
        assertEquals(0, dto.position());
    }

    @Test
    void testJsonSerializationNoNestedEntities() throws Exception {
        // Given
        UUID projectId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        BoardColumnDto dto = new BoardColumnDto(columnId, "To Do", 1, false, projectId);

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("\"id\":\"" + columnId + "\""));
        assertTrue(json.contains("\"name\":\"To Do\""));
        assertTrue(json.contains("\"position\":1"));
        assertTrue(json.contains("\"isDefault\":false"));
        assertTrue(json.contains("\"projectId\":\"" + projectId + "\""));

        // Verify no nested objects (no "project": { })
        assertFalse(json.contains("\"project\":{"));
        assertFalse(json.contains("\"workspace\""));
        assertFalse(json.contains("\"owner\""));
        assertFalse(json.contains("hibernateLazyInitializer"));
    }

    @Test
    void testJsonDeserializationRoundTrip() throws Exception {
        // Given
        UUID projectId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();
        BoardColumnDto original = new BoardColumnDto(columnId, "Review", 3, false, projectId);

        // When
        String json = objectMapper.writeValueAsString(original);
        BoardColumnDto deserialized = objectMapper.readValue(json, BoardColumnDto.class);

        // Then
        assertEquals(original.id(), deserialized.id());
        assertEquals(original.name(), deserialized.name());
        assertEquals(original.position(), deserialized.position());
        assertEquals(original.isDefault(), deserialized.isDefault());
        assertEquals(original.projectId(), deserialized.projectId());
    }

    @Test
    void testRecordImmutability() {
        // Given
        UUID projectId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        // When
        BoardColumnDto dto = new BoardColumnDto(columnId, "Done", 4, false, projectId);

        // Then - verify all fields are accessible
        assertNotNull(dto.id());
        assertNotNull(dto.name());
        assertNotNull(dto.position());
        assertNotNull(dto.isDefault());
        assertNotNull(dto.projectId());

        // Records are immutable - no setters should exist
        // This is enforced by the Java compiler
    }
}
