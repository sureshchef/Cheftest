# --- !Ups

ALTER TABLE incident ADD webuser_id BIGINT;
ALTER TABLE incident ADD source INT NOT NULL DEFAULT 1;
ALTER TABLE mobilesub CHANGE COLUMN email email VARCHAR(255) NULL;
INSERT INTO account (akey, name, manager_id) VALUES ('NONE', 'None', '1');

# --- !Downs

ALTER TABLE incident DROP webuser_id;
ALTER TABLE incident DROP source;
ALTER TABLE mobilesub CHANGE COLUMN email email VARCHAR(255) NOT NULL;
DELETE FROM account WHERE akey='NONE';
