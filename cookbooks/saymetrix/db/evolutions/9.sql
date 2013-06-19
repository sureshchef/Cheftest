# --- !Ups

CREATE TABLE rpt_region_scheme (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  name varchar(32) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY name (name)
);

CREATE TABLE rpt_region (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  scheme_id bigint(20) NOT NULL,
  name varchar(32) NOT NULL,
  query varchar(32) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY scheme_id (scheme_id,query),
  UNIQUE KEY scheme_id_2 (scheme_id,name),
  KEY FK2265695D26EA30D2 (scheme_id),
  CONSTRAINT FK2265695D26EA30D2 FOREIGN KEY (scheme_id) REFERENCES rpt_region_scheme (id)
);

INSERT INTO rpt_region_scheme (id, name) VALUES (1, "Ireland");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (1, 1, "Louth", "%Co. Louth%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (2, 1, "Meath", "%Co. Meath%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (3, 1, "Dublin", "%Co. Dublin%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (4, 1, "Wicklow", "%Co. Wicklow%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (5, 1, "Wexford", "%Co. Wexford%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (6, 1, "Carlow", "%Co. Carlow%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (7, 1, "Kildare", "%Co. Kildare%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (8, 1, "Kilkenny", "%Co. Kilkenny%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (9, 1, "Laois", "%Co. Laois%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (10, 1, "Offaly", "%Co. Offaly%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (11, 1, "Westmeath", "%Co. Westmeath%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (12, 1, "Longford", "%Co. Longford%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (13, 1, "Cork", "%Co. Cork%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (14, 1, "Kerry", "%Co. Kerry%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (15, 1, "Clare", "%Co. Clare%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (16, 1, "Limerick", "%Co. Limerick%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (17, 1, "Tipperary", "%Co. Tipperary%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (18, 1, "Waterford", "%Co. Waterford%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (19, 1, "Galway", "%Co. Galway%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (20, 1, "Mayo", "%Co. Mayo%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (21, 1, "Sligo", "%Co. Sligo%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (22, 1, "Leitrim", "%Co. Leitrim%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (23, 1, "Roscommon", "%Co. Roscommon%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (24, 1, "Donegal", "%Co. Donegal%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (25, 1, "Derry", "%Co. Derry%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (26, 1, "Antrim", "%Co. Antrim%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (27, 1, "Down", "%Co. Down%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (28, 1, "Armagh", "%Co. Armagh%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (29, 1, "Tyrone", "%Co. Tyrone%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (30, 1, "Monaghan", "%Co. Monaghan%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (31, 1, "Fermanagh", "%Co. Fermanagh%");
INSERT INTO rpt_region(id, scheme_id, name, query) VALUES (32, 1, "Cavan", "%Co. Cavan%");

# --- !Downs

DROP TABLE rpt_region;
DROP TABLE rpt_region_scheme;
