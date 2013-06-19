# --- !Ups

CREATE TABLE role (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  name varchar(255) NOT NULL,
  PRIMARY KEY (id)
);

ALTER TABLE webuser ADD role_id BIGINT;
ALTER TABLE webuser ADD CONSTRAINT FK48FAED1FDB0F53C6 FOREIGN KEY (role_id) REFERENCES role (id);

INSERT INTO role (id,name) VALUES (1, 'admin');
INSERT INTO role (id,name) VALUES (2, 'support');
INSERT INTO role (id,name) VALUES (3, 'kam');

UPDATE webuser SET role_id = 1 WHERE role='ADMINISTRATOR';
UPDATE webuser SET role_id = 3 WHERE role='USER';

ALTER TABLE webuser DROP role;

# --- !Downs

ALTER TABLE webuser ADD role VARCHAR(255);

UPDATE webuser SET role = 'ADMINISTRATOR' WHERE role_id=1;
UPDATE webuser SET role = 'USER' WHERE role_id=3;

ALTER TABLE webuser DROP role_id;

DROP TABLE role;
