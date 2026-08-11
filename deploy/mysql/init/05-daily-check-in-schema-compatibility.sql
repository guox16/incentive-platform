USE incentive_db;

-- The service owns a lightweight daily-check-in aggregate.  The earlier
-- activity/rule snapshot columns were never populated by that aggregate and
-- therefore made every first check-in fail before points could be credited.
ALTER TABLE daily_check_ins
    DROP FOREIGN KEY fk_daily_check_ins_activity,
    DROP FOREIGN KEY fk_daily_check_ins_rule,
    DROP INDEX uk_daily_check_ins_activity_user_date,
    DROP COLUMN activity_id,
    DROP COLUMN rule_id,
    DROP COLUMN rule_version;
