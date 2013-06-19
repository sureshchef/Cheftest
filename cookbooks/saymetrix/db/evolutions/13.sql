# --- !Ups

ALTER TABLE filter_account DROP INDEX `accounts_id`;

# --- !Downs

ALTER TABLE filter_account ADD UNIQUE INDEX `accounts_id` (`accounts_id` ASC);
