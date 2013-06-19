# --- !Ups

CREATE  TABLE network_event_type (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  tkey varchar(255) NOT NULL,
  name varchar(255) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY tkey (tkey)
);

CREATE TABLE network_event (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  created datetime DEFAULT NULL,
  description longtext,
  start datetime DEFAULT NULL,
  end datetime DEFAULT NULL,
  subject varchar(255) DEFAULT NULL,
  creator_id bigint(20) DEFAULT NULL,
  event_type_id bigint(20) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY FKDB375549700BDF0 (event_type_id),
  KEY FKDB375549A8695BA9 (creator_id),
  CONSTRAINT FKDB375549A8695BA9 FOREIGN KEY (creator_id) REFERENCES webuser (id),
  CONSTRAINT FKDB375549700BDF0 FOREIGN KEY (event_type_id) REFERENCES network_event_type (id)
);

CREATE  TABLE site (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    skey VARCHAR(255) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    street_address VARCHAR(255) NULL,
    technologies VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY skey (skey)
);

CREATE TABLE network_event_site (
  network_event_id bigint(20) NOT NULL,
  sites_id bigint(20) NOT NULL,
  KEY FK1D18287DFBD55201 (network_event_id),
  KEY FK1D18287D7824B099 (sites_id),
  CONSTRAINT FK1D18287D7824B099 FOREIGN KEY (sites_id) REFERENCES site (id),
  CONSTRAINT FK1D18287DFBD55201 FOREIGN KEY (network_event_id) REFERENCES network_event (id)
);

ALTER TABLE incident ADD COLUMN network_event_id BIGINT(20) NULL AFTER source;
ALTER TABLE incident ADD INDEX FK52F44D2506D333E (network_event_id);
ALTER TABLE incident ADD CONSTRAINT FK52F44D2506D333E FOREIGN KEY (network_event_id) REFERENCES network_event (id);

INSERT INTO network_event_type (tkey, name) VALUES ('outage', 'Outage');

ALTER TABLE incident ADD CONSTRAINT FK52F44D21EC3E5D6 FOREIGN KEY (webuser_id) REFERENCES webuser (id);

# --- !Downs

ALTER TABLE incident DROP FOREIGN KEY FK52F44D21EC3E5D6;

ALTER TABLE incident DROP FOREIGN KEY FK52F44D2506D333E;
ALTER TABLE incident DROP COLUMN network_event_id;
ALTER TABLE network_event_site DROP FOREIGN KEY FK1D18287D7824B099;
ALTER TABLE network_event_site DROP FOREIGN KEY FK1D18287DFBD55201;
DROP TABLE site;
DROP TABLE network_event;
DROP TABLE network_event_site;
DROP TABLE network_event_type;
