-- Sprints table
CREATE TABLE sprint (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    goal TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNED', -- PLANNED, ACTIVE, COMPLETED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    CHECK(end_date > start_date)
);

CREATE INDEX idx_sprint_project ON sprint(project_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_sprint_status ON sprint(project_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_sprint_dates ON sprint(project_id, start_date, end_date) WHERE deleted_at IS NULL;

-- Sprint-Issue many-to-many relationship
CREATE TABLE sprint_issue (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sprint_id UUID NOT NULL REFERENCES sprint(id) ON DELETE CASCADE,
    issue_id UUID NOT NULL REFERENCES issue(id) ON DELETE CASCADE,
    added_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(sprint_id, issue_id)
);

CREATE INDEX idx_sprint_issue_sprint ON sprint_issue(sprint_id);
CREATE INDEX idx_sprint_issue_issue ON sprint_issue(issue_id);
