ALTER TABLE teams_conversation
    ADD COLUMN aad_object_id VARCHAR(255) NOT NULL;

ALTER TABLE teams_conversation
    ADD CONSTRAINT uk_teams_conversation_aad_object_id UNIQUE (aad_object_id);
