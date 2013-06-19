# --- !Ups

INSERT INTO system_setting (key_, type_id, value) VALUES ("redact.enabled", 2, "false");

# --- !Downs

DELETE FROM system_setting WHERE key_ = "redact.enabled";