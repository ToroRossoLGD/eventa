CREATE TABLE organizers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(180) NOT NULL
);

CREATE TABLE event_organizers (
    event_id BIGINT NOT NULL,
    organizer_id BIGINT NOT NULL,
    PRIMARY KEY (event_id, organizer_id),
    FOREIGN KEY (event_id) REFERENCES events(id),
    FOREIGN KEY (organizer_id) REFERENCES organizers(id)
);

CREATE INDEX idx_event_organizers_organizer ON event_organizers(organizer_id);
