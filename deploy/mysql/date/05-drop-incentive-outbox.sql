USE incentive_db;

-- 当前业务没有写入或消费该事务发件箱，删除闲置表。
DROP TABLE IF EXISTS incentive_outbox;
