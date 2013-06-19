# --- !Ups

DROP TABLE mobilesub_incident;

# --- !Downs

CREATE TABLE mobilesub_incident (
  mobilesub_id bigint(20) NOT NULL,
  incidents_id bigint(20) NOT NULL,
  UNIQUE KEY incidents_id (incidents_id),
  KEY FK53E16D93B541106A (mobilesub_id),
  KEY FK53E16D93405C8C2F (incidents_id),
  CONSTRAINT FK53E16D93405C8C2F FOREIGN KEY (incidents_id) REFERENCES incident (id),
  CONSTRAINT FK53E16D93B541106A FOREIGN KEY (mobilesub_id) REFERENCES mobilesub (id)
);