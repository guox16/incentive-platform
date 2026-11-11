USE xxl_job;

INSERT IGNORE INTO xxl_job_info (
    id, job_group, job_desc, add_time, update_time, author, alarm_email,
    schedule_type, schedule_conf, misfire_strategy, executor_route_strategy,
    executor_handler, executor_param, executor_block_strategy, executor_timeout,
    executor_fail_retry_count, glue_type, glue_source, glue_remark, glue_updatetime,
    child_jobid, trigger_status, trigger_last_time, trigger_next_time)
SELECT 1004, id, '签到积分补发', NOW(), NOW(), 'system', '',
       'CRON', '0/15 * * * * ?', 'DO_NOTHING', 'SHARDING_BROADCAST',
       'dailyCheckInRewardJob', '', 'SERIAL_EXECUTION', 180,
       0, 'BEAN', '', '初始化', NOW(), '', 1, 0, 0
FROM xxl_job_group
WHERE app_name = 'incentive-lottery-service'
ORDER BY id
LIMIT 1;
