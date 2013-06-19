# --- !Ups

ALTER TABLE webuser ADD COLUMN pw_reset_token VARCHAR(255) NULL  AFTER role_id;

# --- !Downs

ALTER TABLE webuser DROP COLUMN pw_reset_token;
