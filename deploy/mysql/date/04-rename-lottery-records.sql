USE incentive_db;

-- 保留全部历史数据，只调整抽奖业务记录的表名。
RENAME TABLE lottery_participations TO lottery_records;
