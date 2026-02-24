# Board Column Evolution Summary

## Overview
Evolved the board_column system from basic columns to a Jira-like implementation with soft delete, semantic categories, WIP limits, and comprehensive audit tracking.

---

## Files Created

### 1. **ColumnCategory.java** (Enum)
`ai.planmate.agile.entity.ColumnCategory`

Semantic categories for board columns:
- `BACKLOG` - Backlog/Icebox items
- `TODO` - Ready to start
- `IN_PROGRESS` - Currently being worked on
- `DONE` - Completed work
- `CUSTOM` - User-defined categories

### 2. **UpdateColumnRequest.java** (DTO)
`ai.planmate.agile.dto.UpdateColumnRequest`

Request DTO for updating board columns with validation:
- Optional name (1-60 chars)
- Optional category
- Optional WIP limit (min 1)
- Optional isDefault flag

### 3. **BoardColumnServiceTest.java** (Tests)
`ai.planmate.agile.service.BoardColumnServiceTest`

Comprehensive integration tests covering:
- Soft delete behavior
- Default column constraints
- Column in-use protection
- Reordering with deleted columns
- WIP limit validation
- Category assignment
- Name trimming and length validation

### 4. **V27__enhance_board_column_with_soft_delete_and_semantics.sql** (Migration)
`src/main/resources/db/migration/V27__enhance_board_column_with_soft_delete_and_semantics.sql`

Database migration that:
- Adds 5 new columns (deleted_at, category, wip_limit, created_by, updated_by)
- Backfills category based on name patterns
- Creates partial unique indexes for position and default columns
- Adds performance indexes
- Includes constraints and documentation

---

## Files Modified

### 1. **BoardColumn.java** (Entity)
`ai.planmate.agile.entity.BoardColumn`

**Added Fields:**
- `category` (ColumnCategory) - Semantic category [default: CUSTOM]
- `wipLimit` (Integer) - Work-in-progress limit (null = unlimited)
- `createdBy` (UUID) - User who created the column
- `updatedBy` (UUID) - User who last updated the column
- `deletedAt` (Instant) - Soft delete timestamp

**Added Methods:**
- `isDeleted()` - Check if column is soft-deleted
- `onCreate()` - @PrePersist callback for timestamps
- `onUpdate()` - @PreUpdate callback for updatedAt
- `setName()` - Override to trim names automatically

**Validation:**
- Name: 1-60 characters (changed from 100)
- WIP limit: >= 1 if present
- Category: Required, defaults to CUSTOM

### 2. **BoardColumnRepository.java** (Repository)
`ai.planmate.agile.repository.BoardColumnRepository`

**All Queries Updated for Soft Delete:**
- `findByProjectIdOrderByPosition()` - Added `deletedAt IS NULL`
- `findByIdAndNotDeleted()` - NEW - Safe fetch by ID
- `findDefaultByProjectId()` - Added `deletedAt IS NULL`
- `existsDefaultByProjectId()` - NEW - Check default exists
- `countByProjectId()` - Added `deletedAt IS NULL`
- `findMaxPositionByProjectId()` - NEW - Get max position
- `existsByProjectIdAndNameIgnoreCaseAndDeletedAtIsNull()` - NEW - Name uniqueness check
- `existsByProjectIdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot()` - NEW - Name uniqueness excluding self

### 3. **BoardService.java** (Service)
`ai.planmate.agile.service.BoardService`

**New Methods:**
- `createColumn(projectId, name, category, wipLimit, isDefault)` - Full parameter version
- `updateColumn(columnId, name, wipLimit, category, isDefault)` - Update with validation
- `getCurrentUserId()` - Extract user ID from SecurityContext
- `determineCategoryFromName(name)` - Auto-determine category from name

**Updated Methods:**
- `createDefaultColumns()` - Sets categories and audit fields
- `createColumn(projectId, name)` - Now delegates to full version
- `deleteColumn()` - Soft delete with conflict checks
- `renameColumn()` - Delegates to updateColumn()
- `reorderColumns()` - Validates against soft-deleted columns
- `moveIssue()` - Uses findByIdAndNotDeleted()
- `getBoard()` - Returns category and wipLimit in response

**Business Rules Implemented:**
- ✅ Only one default column per project
- ✅ Cannot delete default column (409 CONFLICT)
- ✅ Cannot delete column with issues (409 CONFLICT)
- ✅ WIP limit >= 1 validation
- ✅ Name trimming and length validation
- ✅ Position auto-calculation
- ✅ Audit field tracking (createdBy, updatedBy)
- ✅ Structured logging with context

### 4. **BoardColumnDto.java** (DTO)
`ai.planmate.agile.dto.BoardColumnDto`

**Added Fields:**
- `category` (ColumnCategory)
- `wipLimit` (Integer)

### 5. **BoardColumnResponse.java** (DTO)
`ai.planmate.agile.dto.BoardColumnResponse`

**Added Fields:**
- `category` (ColumnCategory)
- `wipLimit` (Integer)

### 6. **CreateColumnRequest.java** (DTO)
`ai.planmate.agile.dto.CreateColumnRequest`

**Added Fields:**
- `category` (ColumnCategory) - Optional
- `wipLimit` (Integer) - Optional, min 1
- `isDefault` (Boolean) - Optional

**Updated Validation:**
- Name length: 1-60 characters (was 1-100)

### 7. **BoardController.java** (Controller)
`ai.planmate.agile.controller.BoardController`

**Updated Endpoints:**
- `POST /columns` - Now accepts category, wipLimit, isDefault
- `PUT /columns/{columnId}` - New endpoint using UpdateColumnRequest

**Error Responses:**
- 404 - Column not found (including soft-deleted)
- 409 - Conflict errors (default exists, column in use, etc.)

### 8. **GlobalExceptionHandler.java** (Exception Handler)
`ai.planmate.shared.exception.GlobalExceptionHandler`

**Added Handlers:**
- `uq_board_column_project_position` → 409 COLUMN_POSITION_CONFLICT
- `uq_board_column_project_default` → 409 DEFAULT_COLUMN_EXISTS

---

## Database Changes (V27 Migration)

### New Columns
```sql
deleted_at         TIMESTAMP WITH TIME ZONE  -- Soft delete timestamp
category           VARCHAR(20) NOT NULL DEFAULT 'CUSTOM'
wip_limit          INTEGER                   -- NULL = unlimited, else >= 1
created_by         UUID REFERENCES app_user(id)
updated_by         UUID REFERENCES app_user(id)
```

### Constraints
- `chk_board_column_wip_limit` - Ensures WIP limit >= 1 if not null
- Name column reduced from VARCHAR(100) to VARCHAR(60)

### Partial Unique Indexes (Soft-Delete Aware)
```sql
-- Ensures unique position per project among non-deleted columns
CREATE UNIQUE INDEX uq_board_column_project_position
ON board_column(project_id, position)
WHERE deleted_at IS NULL;

-- Ensures at most one default column per project among non-deleted columns
CREATE UNIQUE INDEX uq_board_column_project_default
ON board_column(project_id)
WHERE is_default = true AND deleted_at IS NULL;
```

### Performance Indexes
```sql
idx_board_column_project_deleted_position  -- Fast board loading
idx_board_column_deleted_at                -- Soft delete queries
idx_board_column_category                  -- Category filtering
idx_board_column_created_by                -- Audit tracking
idx_board_column_updated_by                -- Audit tracking
```

### Data Backfill
Existing columns automatically categorized based on name patterns:
- "backlog" → BACKLOG
- "done" → DONE
- "progress" → IN_PROGRESS
- "to do" / "todo" → TODO
- Others → CUSTOM

---

## API Examples

### Create Column with Full Options
```http
POST /v1/projects/{projectId}/board/columns
Content-Type: application/json

{
  "name": "In Review",
  "category": "IN_PROGRESS",
  "wipLimit": 3,
  "isDefault": false
}
```

**Response:**
```json
{
  "id": "uuid",
  "name": "In Review",
  "position": 2,
  "isDefault": false,
  "category": "IN_PROGRESS",
  "wipLimit": 3,
  "projectId": "uuid"
}
```

### Update Column
```http
PUT /v1/projects/{projectId}/board/columns/{columnId}
Content-Type: application/json

{
  "wipLimit": 5,
  "category": "DONE"
}
```

### Delete Column (Soft Delete)
```http
DELETE /v1/projects/{projectId}/board/columns/{columnId}
```

**Possible Errors:**
- 404 - Column not found
- 409 - Cannot delete default column (CANNOT_DELETE_DEFAULT_COLUMN)
- 409 - Column has issues (COLUMN_IN_USE)

---

## Error Codes

| Code | HTTP | Description |
|------|------|-------------|
| `DEFAULT_COLUMN_EXISTS` | 409 | Attempted to create/set second default column |
| `CANNOT_DELETE_DEFAULT_COLUMN` | 409 | Attempted to delete the default column |
| `COLUMN_IN_USE` | 409 | Attempted to delete column with issues |
| `COLUMN_POSITION_CONFLICT` | 409 | Position conflict (database constraint) |

---

## Logging Examples

### Column Creation
```
INFO  Creating column: projectId=abc, name=In Progress, category=IN_PROGRESS, wipLimit=3, isDefault=false, userId=xyz
INFO  Column created: id=def, projectId=abc, name=In Progress, position=2, category=IN_PROGRESS, userId=xyz
```

### Column Deletion
```
INFO  Deleting column: id=def, userId=xyz
INFO  Column soft-deleted: id=def, projectId=abc, issuesCount=0, userId=xyz
```

### Column Reordering
```
INFO  Reordering columns: projectId=abc, columnCount=5, userId=xyz
INFO  Columns reordered: projectId=abc, count=5, userId=xyz
```

---

## Testing

Run tests:
```bash
./gradlew test --tests BoardColumnServiceTest
```

### Test Coverage
✅ Soft deleted columns not returned in list
✅ Cannot create second default column
✅ Cannot delete default column
✅ Cannot delete column with issues
✅ Reorder ignores deleted columns
✅ WIP limit validation
✅ Category determination
✅ Column name trimming
✅ Column name length validation
✅ Position calculation
✅ Update column sets isDefault correctly
✅ Find by ID returns empty for deleted column

---

## Migration Rollback (If Needed)

**⚠️ Warning:** This will lose soft-delete data and new fields.

```sql
-- Remove new columns
ALTER TABLE board_column DROP COLUMN IF EXISTS deleted_at;
ALTER TABLE board_column DROP COLUMN IF EXISTS category;
ALTER TABLE board_column DROP COLUMN IF EXISTS wip_limit;
ALTER TABLE board_column DROP COLUMN IF EXISTS created_by;
ALTER TABLE board_column DROP COLUMN IF EXISTS updated_by;

-- Drop new indexes
DROP INDEX IF EXISTS uq_board_column_project_position;
DROP INDEX IF EXISTS uq_board_column_project_default;
DROP INDEX IF EXISTS idx_board_column_project_deleted_position;
DROP INDEX IF EXISTS idx_board_column_deleted_at;
DROP INDEX IF EXISTS idx_board_column_category;
DROP INDEX IF EXISTS idx_board_column_created_by;
DROP INDEX IF EXISTS idx_board_column_updated_by;

-- Restore original index
CREATE INDEX idx_board_column_position ON board_column(project_id, position);

-- Restore name length
ALTER TABLE board_column ALTER COLUMN name TYPE VARCHAR(100);
```

---

## Production Deployment Checklist

- [ ] Review migration script V27
- [ ] Backup database before applying migration
- [ ] Test migration on staging environment
- [ ] Verify all unit tests pass
- [ ] Verify integration tests pass
- [ ] Update API documentation with new fields
- [ ] Notify frontend team of new fields (category, wipLimit)
- [ ] Monitor error rates after deployment
- [ ] Monitor database performance (new indexes)
- [ ] Update monitoring alerts for new error codes

---

## Performance Notes

### Indexing Strategy
- **Partial unique indexes** prevent conflicts only among active (non-deleted) columns
- **Composite index** (project_id, deleted_at, position) optimizes board loading queries
- **Category index** supports future filtering/analytics features

### Query Optimization
All repository methods use `deletedAt IS NULL` filters, leveraging the composite index for fast board loads.

### Scalability
Soft delete approach allows:
- Column recreation with same name (after deletion)
- Audit trail of deleted columns
- Recovery of accidentally deleted columns
- Historical analysis

---

## Future Enhancements

**Potential Additions:**
1. Column color customization
2. Column icons
3. Automated transitions between columns
4. Column-level permissions
5. Analytics per column (cycle time, throughput)
6. Column templates
7. Bulk column operations
8. Column archiving (vs soft delete)
9. Column dependencies/ordering rules
10. WIP limit warnings/enforcement UI

---

## References

- **Entity**: `ai.planmate.agile.entity.BoardColumn`
- **Repository**: `ai.planmate.agile.repository.BoardColumnRepository`
- **Service**: `ai.planmate.agile.service.BoardService`
- **Controller**: `ai.planmate.agile.controller.BoardController`
- **Migration**: `V27__enhance_board_column_with_soft_delete_and_semantics.sql`
- **Tests**: `ai.planmate.agile.service.BoardColumnServiceTest`
