#!/bin/bash
# Schema Validation Verification Script
# Run this after starting the application to verify all fixes are working

set -e

echo "================================================"
echo "Schema Validation Verification"
echo "================================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if PostgreSQL connection details are provided
if [ -z "$DB_URL" ]; then
    echo -e "${YELLOW}⚠ DB_URL not set. Using default: jdbc:postgresql://localhost:5432/planmate${NC}"
    DB_URL="jdbc:postgresql://localhost:5432/planmate"
fi

if [ -z "$DB_USER" ]; then
    echo -e "${YELLOW}⚠ DB_USER not set. Using default: postgres${NC}"
    DB_USER="postgres"
fi

if [ -z "$DB_PASSWORD" ]; then
    echo -e "${YELLOW}⚠ DB_PASSWORD not set. Using default: postgres${NC}"
    DB_PASSWORD="postgres"
fi

# Extract host, port, and database from JDBC URL
DB_HOST=$(echo $DB_URL | sed -n 's/.*:\/\/\([^:]*\):.*/\1/p')
DB_PORT=$(echo $DB_URL | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
DB_NAME=$(echo $DB_URL | sed -n 's/.*\/\([^?]*\).*/\1/p')

echo "Connecting to: $DB_HOST:$DB_PORT/$DB_NAME"
echo ""

# Function to run SQL and check result
check_sql() {
    local description=$1
    local sql=$2
    local expected=$3

    echo -n "Checking: $description ... "

    result=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -A -c "$sql" 2>&1)

    if echo "$result" | grep -q "$expected"; then
        echo -e "${GREEN}✓ PASS${NC}"
        return 0
    else
        echo -e "${RED}✗ FAIL${NC}"
        echo "  Expected: $expected"
        echo "  Got: $result"
        return 1
    fi
}

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "1. FLYWAY MIGRATION STATUS"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check_sql "Flyway schema_history table exists" \
    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name='flyway_schema_history'" \
    "1"

check_sql "All 22 migrations applied" \
    "SELECT COUNT(*) FROM flyway_schema_history WHERE success=true" \
    "22"

check_sql "V22 migration applied" \
    "SELECT COUNT(*) FROM flyway_schema_history WHERE version='22'" \
    "1"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "2. ISSUE TABLE SCHEMA VALIDATION"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check_sql "order_index is NUMERIC(15,2)" \
    "SELECT data_type || '(' || numeric_precision || ',' || numeric_scale || ')' FROM information_schema.columns WHERE table_name='issue' AND column_name='order_index'" \
    "numeric(15,2)"

check_sql "order_index default is 1000" \
    "SELECT column_default FROM information_schema.columns WHERE table_name='issue' AND column_name='order_index'" \
    "1000"

check_sql "order_index is NOT NULL" \
    "SELECT is_nullable FROM information_schema.columns WHERE table_name='issue' AND column_name='order_index'" \
    "NO"

check_sql "board_column_id exists and is nullable" \
    "SELECT is_nullable FROM information_schema.columns WHERE table_name='issue' AND column_name='board_column_id'" \
    "YES"

check_sql "parent_issue_id exists and is nullable" \
    "SELECT is_nullable FROM information_schema.columns WHERE table_name='issue' AND column_name='parent_issue_id'" \
    "YES"

check_sql "reporter_id is nullable" \
    "SELECT is_nullable FROM information_schema.columns WHERE table_name='issue' AND column_name='reporter_id'" \
    "YES"

check_sql "version column exists" \
    "SELECT COUNT(*) FROM information_schema.columns WHERE table_name='issue' AND column_name='version'" \
    "1"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "3. ENUM TYPES VALIDATION"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check_sql "sprint_status enum exists" \
    "SELECT COUNT(*) FROM pg_type WHERE typname='sprint_status'" \
    "1"

check_sql "sprint_status enum values" \
    "SELECT string_agg(enumlabel, ',' ORDER BY enumsortorder) FROM pg_enum WHERE enumtypid=(SELECT oid FROM pg_type WHERE typname='sprint_status')" \
    "PLANNED,ACTIVE,COMPLETED"

check_sql "sprint.status column uses sprint_status enum" \
    "SELECT data_type FROM information_schema.columns WHERE table_name='sprint' AND column_name='status'" \
    "USER-DEFINED"

check_sql "sprint.status default value" \
    "SELECT column_default FROM information_schema.columns WHERE table_name='sprint' AND column_name='status'" \
    "'PLANNED'::sprint_status"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "4. APP_USER TABLE VALIDATION"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check_sql "plan is VARCHAR" \
    "SELECT data_type FROM information_schema.columns WHERE table_name='app_user' AND column_name='plan'" \
    "character varying"

check_sql "user_type column exists" \
    "SELECT COUNT(*) FROM information_schema.columns WHERE table_name='app_user' AND column_name='user_type'" \
    "1"

check_sql "provider column exists" \
    "SELECT COUNT(*) FROM information_schema.columns WHERE table_name='app_user' AND column_name='provider'" \
    "1"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "5. BOARD_COLUMN TABLE VALIDATION"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check_sql "board_column table exists" \
    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name='board_column'" \
    "1"

check_sql "position is INTEGER" \
    "SELECT data_type FROM information_schema.columns WHERE table_name='board_column' AND column_name='position'" \
    "integer"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "6. CHAT & AI TABLES VALIDATION"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check_sql "chat_thread table exists" \
    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name='chat_thread'" \
    "1"

check_sql "chat_message table exists" \
    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name='chat_message'" \
    "1"

check_sql "ai_usage table exists" \
    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name='ai_usage'" \
    "1"

check_sql "diagram table exists" \
    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name='diagram'" \
    "1"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "7. INDEX VALIDATION"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check_sql "idx_issue_order_index exists" \
    "SELECT COUNT(*) FROM pg_indexes WHERE tablename='issue' AND indexname='idx_issue_order_index'" \
    "1"

check_sql "idx_issue_board_column exists" \
    "SELECT COUNT(*) FROM pg_indexes WHERE tablename='issue' AND indexname='idx_issue_board_column'" \
    "1"

check_sql "idx_issue_parent exists" \
    "SELECT COUNT(*) FROM pg_indexes WHERE tablename='issue' AND indexname='idx_issue_parent'" \
    "1"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "8. FOREIGN KEY VALIDATION"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check_sql "issue → board_column FK exists" \
    "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_name='issue' AND constraint_type='FOREIGN KEY' AND constraint_name LIKE '%board_column%'" \
    "1"

check_sql "issue → parent_issue FK exists" \
    "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_name='issue' AND constraint_type='FOREIGN KEY' AND constraint_name LIKE '%parent%'" \
    "1"

echo ""
echo "================================================"
echo "Verification Complete!"
echo "================================================"
echo ""
echo -e "${GREEN}If all checks passed, your schema is ready for Hibernate validation.${NC}"
echo ""
echo "Next steps:"
echo "1. Start the application: ./gradlew bootRun"
echo "2. Check logs for 'Started PlanmateApiApplication'"
echo "3. Verify no Hibernate validation errors"
echo ""
