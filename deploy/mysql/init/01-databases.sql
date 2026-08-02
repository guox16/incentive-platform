CREATE DATABASE IF NOT EXISTS user_db;
CREATE DATABASE IF NOT EXISTS points_db;
CREATE DATABASE IF NOT EXISTS incentive_db;
CREATE DATABASE IF NOT EXISTS award_db;
GRANT ALL PRIVILEGES ON user_db.* TO 'incentive'@'%';
GRANT ALL PRIVILEGES ON points_db.* TO 'incentive'@'%';
GRANT ALL PRIVILEGES ON incentive_db.* TO 'incentive'@'%';
GRANT ALL PRIVILEGES ON award_db.* TO 'incentive'@'%';
FLUSH PRIVILEGES;

