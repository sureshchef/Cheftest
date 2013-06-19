# --- !Ups

ALTER TABLE incident ADD address VARCHAR(256) AFTER longitude;
CREATE TABLE incident_details (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  address_json text,
  incident_id bigint(20) NOT NULL,
  PRIMARY KEY (id),
  KEY FK1D36EBD5F8656B9E (incident_id),
  CONSTRAINT FK1D36EBD5F8656B9E FOREIGN KEY (incident_id) REFERENCES incident (id)
);

# --- !Downs

DROP TABLE incident_details;
ALTER TABLE incident DROP COLUMN address;
