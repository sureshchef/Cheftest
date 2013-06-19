# --- !Ups

INSERT INTO system_setting (key_, type_id, value)
    VALUES ("msg.register.failure", 1, "It looks like you're not registered for SayMetrix. Please get in touch with your account manager.");
INSERT INTO system_setting (key_, type_id, value)
    VALUES ("msg.register.success", 1, "Thank you for registering for SayMetrix.");
INSERT INTO system_setting (key_, type_id, value)
    VALUES ("msg.report.failure", 1, "It looks like you no longer have access to SayMetrix. Please get in touch with your account manager.");
INSERT INTO system_setting (key_, type_id, value)
    VALUES ("msg.report.success", 1, "${firstname}, thanks for your feedback and helping us to improve our network.");

# --- !Downs

DELETE FROM system_setting WHERE key_ = "msg.register.failure";
DELETE FROM system_setting WHERE key_ = "msg.register.success";
DELETE FROM system_setting WHERE key_ = "msg.report.failure";
DELETE FROM system_setting WHERE key_ = "msg.report.success";