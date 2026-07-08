-- ============================================================
-- 智慧路灯 — 测试数据插入脚本
-- 数据库: PostgreSQL
-- 说明: 本脚本依赖于 schema.sql 建表，执行前请确保表已存在
-- 密码明文 → BCrypt 哈希（由 Hutool BCrypt.hashpw 生成，10 轮）:
--   admin123  → $2a$10$rNgAYknaIvMHYpT18sKd7Ob0AvHvoWwk.6gb.oBiaZsMgzW9Q2idC
--   123456    → $2a$10$oDZI6djgYk86X9PEhdPWuuAZ9NhUL69GyCORiUw.Vnv8vd5JUfxg.
--   staff123  → $2a$10$llHBmc6QnS/HiiioooPyt.Rk2Ml7VGIWRZKv5VnAyE9O4c9hA3Y7a
-- ============================================================

-- 清空旧测试数据（按依赖顺序，先删子表再删主表，BIGSERIAL 不会自动复位）
DELETE FROM control_logs;
DELETE FROM light_readings;
DELETE FROM alarm_logs;
DELETE FROM threshold_config;
DELETE FROM devices;
DELETE FROM users;

-- 复位序列（让 ID 重新从 1 开始）
ALTER SEQUENCE users_id_seq RESTART WITH 1;
ALTER SEQUENCE devices_id_seq RESTART WITH 1;
ALTER SEQUENCE light_readings_id_seq RESTART WITH 1;
ALTER SEQUENCE control_logs_id_seq RESTART WITH 1;
ALTER SEQUENCE alarm_logs_id_seq RESTART WITH 1;
ALTER SEQUENCE threshold_config_id_seq RESTART WITH 1;

-- ============================================================
-- 1. 用户表（4 人）
-- ============================================================
INSERT INTO users (username, password, role) VALUES
    ('admin',   '$2a$10$rNgAYknaIvMHYpT18sKd7Ob0AvHvoWwk.6gb.oBiaZsMgzW9Q2idC', 'ADMIN'),
    ('zhangsan','$2a$10$oDZI6djgYk86X9PEhdPWuuAZ9NhUL69GyCORiUw.Vnv8vd5JUfxg.', 'MUNICIPAL_STAFF'),
    ('lisi',    '$2a$10$oDZI6djgYk86X9PEhdPWuuAZ9NhUL69GyCORiUw.Vnv8vd5JUfxg.', 'MUNICIPAL_STAFF'),
    ('wangwu',  '$2a$10$oDZI6djgYk86X9PEhdPWuuAZ9NhUL69GyCORiUw.Vnv8vd5JUfxg.', 'MUNICIPAL_STAFF');

-- ============================================================
-- 2. 路灯设备表（6 台）
--   覆盖场景: 在线+开灯 / 在线+关灯 / 离线+关灯 / 离线+开灯(卡死)
-- ============================================================
INSERT INTO devices (device_name, device_sn, status, online_status, last_heartbeat_time, created_at) VALUES
    ('人民路001号路灯', 'SN-RM-001', 'ON',  'ONLINE',  CURRENT_TIMESTAMP - INTERVAL  '2 minutes', CURRENT_TIMESTAMP - INTERVAL '30 days'),
    ('人民路002号路灯', 'SN-RM-002', 'OFF', 'ONLINE',  CURRENT_TIMESTAMP - INTERVAL  '5 minutes', CURRENT_TIMESTAMP - INTERVAL '30 days'),
    ('解放路001号路灯', 'SN-JF-001', 'OFF', 'OFFLINE', CURRENT_TIMESTAMP - INTERVAL '90 seconds', CURRENT_TIMESTAMP - INTERVAL '25 days'), -- 心跳90秒前，超时阈值60秒 → 已离线
    ('解放路002号路灯', 'SN-JF-002', 'ON',  'ONLINE',  CURRENT_TIMESTAMP - INTERVAL  '8 minutes', CURRENT_TIMESTAMP - INTERVAL '20 days'),
    ('中山路001号路灯', 'SN-ZS-001', 'OFF', 'ONLINE',  CURRENT_TIMESTAMP - INTERVAL  '1 minute',  CURRENT_TIMESTAMP - INTERVAL '15 days'),
    ('中山路002号路灯', 'SN-ZS-002', 'ON',  'OFFLINE', CURRENT_TIMESTAMP - INTERVAL '120 seconds', CURRENT_TIMESTAMP - INTERVAL '15 days'); -- 心跳2分钟前，已离线但灯卡在ON状态

-- ============================================================
-- 3. 光照记录表（33 条，覆盖最近 6 小时 + 少量历史）
--   强度范围: 低光照(<30 应触发开灯) / 正常(30-80) / 高光照(>80 应触发关灯)
-- ============================================================
INSERT INTO light_readings (device_id, light_intensity, created_at) VALUES
    -- 设备1（人民路001号, 在线+开灯）: 早晚交替，最近一次为低光照
    (1, 28.50, CURRENT_TIMESTAMP - INTERVAL  '5 minutes'),  -- 低于30，触发AUTO ON
    (1, 35.20, CURRENT_TIMESTAMP - INTERVAL '35 minutes'),
    (1, 72.80, CURRENT_TIMESTAMP - INTERVAL '65 minutes'),
    (1, 88.10, CURRENT_TIMESTAMP - INTERVAL '95 minutes'),  -- 高于80，触发AUTO OFF
    (1, 45.60, CURRENT_TIMESTAMP - INTERVAL '125 minutes'),
    (1, 22.30, CURRENT_TIMESTAMP - INTERVAL '155 minutes'),  -- 低于30

    -- 设备2（人民路002号, 在线+关灯）: 正常范围内
    (2, 55.00, CURRENT_TIMESTAMP - INTERVAL  '3 minutes'),
    (2, 60.30, CURRENT_TIMESTAMP - INTERVAL '33 minutes'),
    (2, 48.70, CURRENT_TIMESTAMP - INTERVAL '63 minutes'),
    (2, 51.90, CURRENT_TIMESTAMP - INTERVAL '93 minutes'),
    (2, 65.40, CURRENT_TIMESTAMP - INTERVAL '123 minutes'),

    -- 设备3（解放路001号, 离线+关灯）: 离线前有数据，离线后无新数据
    (3, 33.10, CURRENT_TIMESTAMP - INTERVAL  '3 minutes'),  -- 最近一次（虽然离线，但可能最后一次上报）
    (3, 41.50, CURRENT_TIMESTAMP - INTERVAL '30 minutes'),
    (3, 39.80, CURRENT_TIMESTAMP - INTERVAL '60 minutes'),
    (3, 76.20, CURRENT_TIMESTAMP - INTERVAL '90 minutes'),

    -- 设备4（解放路002号, 在线+开灯）: 光照很高但灯开着（可能是手动覆盖）
    (4, 92.00, CURRENT_TIMESTAMP - INTERVAL  '7 minutes'),  -- 高于80，但灯仍为ON（手动控制）
    (4, 87.50, CURRENT_TIMESTAMP - INTERVAL '37 minutes'),
    (4, 45.30, CURRENT_TIMESTAMP - INTERVAL '67 minutes'),
    (4, 26.80, CURRENT_TIMESTAMP - INTERVAL '97 minutes'),  -- 低于30，触发AUTO ON
    (4, 50.10, CURRENT_TIMESTAMP - INTERVAL '127 minutes'),

    -- 设备5（中山路001号, 在线+关灯）: 黄昏场景，光照逐渐降低
    (5, 70.00, CURRENT_TIMESTAMP - INTERVAL  '4 minutes'),
    (5, 75.50, CURRENT_TIMESTAMP - INTERVAL '19 minutes'),
    (5, 82.30, CURRENT_TIMESTAMP - INTERVAL '34 minutes'),
    (5, 85.00, CURRENT_TIMESTAMP - INTERVAL '49 minutes'),
    (5, 90.20, CURRENT_TIMESTAMP - INTERVAL '64 minutes'),
    (5, 95.60, CURRENT_TIMESTAMP - INTERVAL '79 minutes'),

    -- 设备6（中山路002号, 离线+开灯）: 离线前最后几条数据
    (6, 18.90, CURRENT_TIMESTAMP - INTERVAL  '7 minutes'),
    (6, 20.40, CURRENT_TIMESTAMP - INTERVAL '22 minutes'),
    (6, 25.10, CURRENT_TIMESTAMP - INTERVAL '37 minutes'),

    -- 历史数据（几天前，用于趋势查询）
    (1, 42.00, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (2, 55.50, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (4, 38.00, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (5, 78.00, CURRENT_TIMESTAMP - INTERVAL '1 day');

-- ============================================================
-- 4. 控制日志表（14 条）
--   source: AUTO=光照联动自动, MANUAL=手动远程
--   command: ON | OFF
--   result: SUCCESS | FAIL
--   operator_id: MANUAL 时有值，AUTO 时为 NULL
-- ============================================================
INSERT INTO control_logs (device_id, operator_id, command, source, result, created_at) VALUES
    -- 自动控制：设备1 低光照自动开灯
    (1, NULL, 'ON',  'AUTO', 'SUCCESS', CURRENT_TIMESTAMP - INTERVAL  '5 minutes'),
    -- 自动控制：设备1 之前高光照自动关灯
    (1, NULL, 'OFF', 'AUTO', 'SUCCESS', CURRENT_TIMESTAMP - INTERVAL '95 minutes'),
    -- 自动控制：设备4 低光照自动开灯
    (4, NULL, 'ON',  'AUTO', 'SUCCESS', CURRENT_TIMESTAMP - INTERVAL '97 minutes'),
    -- 自动控制：设备6 低光照自动开灯（但随后离线，灯卡在ON）
    (6, NULL, 'ON',  'AUTO', 'SUCCESS', CURRENT_TIMESTAMP - INTERVAL '37 minutes'),

    -- 手动控制：zhangsan 远程关闭设备4（覆盖自动，但模拟中设备4仍为ON）
    (4, 2,   'OFF', 'MANUAL', 'FAIL',    CURRENT_TIMESTAMP - INTERVAL '10 minutes'), -- 手动关灯失败了
    -- 手动控制：admin 远程打开设备2
    (2, 1,   'ON',  'MANUAL', 'SUCCESS', CURRENT_TIMESTAMP - INTERVAL '45 minutes'),
    -- 手动控制：admin 远程关闭设备2
    (2, 1,   'OFF', 'MANUAL', 'SUCCESS', CURRENT_TIMESTAMP - INTERVAL '60 minutes'),
    -- 手动控制：lisi 远程打开设备5
    (5, 3,   'ON',  'MANUAL', 'SUCCESS', CURRENT_TIMESTAMP - INTERVAL '80 minutes'),
    -- 手动控制：lisi 远程关闭设备5
    (5, 3,   'OFF', 'MANUAL', 'SUCCESS', CURRENT_TIMESTAMP - INTERVAL '110 minutes'),

    -- 手动控制：wangwu 尝试远程打开离线设备3（预期失败或成功取决于逻辑）
    (3, 4,   'ON',  'MANUAL', 'FAIL',    CURRENT_TIMESTAMP - INTERVAL '15 minutes'),
    -- 手动控制：admin 远程关闭设备1
    (1, 1,   'OFF', 'MANUAL', 'SUCCESS', CURRENT_TIMESTAMP - INTERVAL '120 minutes'),
    -- 手动控制：admin 远程打开设备1
    (1, 1,   'ON',  'MANUAL', 'SUCCESS', CURRENT_TIMESTAMP - INTERVAL '150 minutes'),

    -- 历史数据
    (2, 1,   'OFF', 'MANUAL', 'SUCCESS', CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (4, 2,   'ON',  'MANUAL', 'SUCCESS', CURRENT_TIMESTAMP - INTERVAL '1 day');

-- ============================================================
-- 5. 阈值配置表（当前有效配置）
-- ============================================================
INSERT INTO threshold_config (light_threshold_on, light_threshold_off, heartbeat_timeout)
VALUES (30, 80, 60);  -- 光照<30自动开灯，>80自动关灯，心跳超时60秒

-- ============================================================
-- 6. 告警记录表（8 条）
--   alarm_type: OFFLINE | LIGHT_ABNORMAL | HEARTBEAT_TIMEOUT
--   status: ACTIVE | RESOLVED
-- ============================================================
INSERT INTO alarm_logs (device_id, alarm_type, message, status, created_at, resolved_at) VALUES
    -- 设备3 离线告警（活跃中）
    (3, 'OFFLINE', '设备解放路001号路灯心跳超时，已自动标记为离线', 'ACTIVE',
     CURRENT_TIMESTAMP - INTERVAL '30 seconds', NULL),

    -- 设备6 离线告警（活跃中）
    (6, 'OFFLINE', '设备中山路002号路灯心跳超时，已自动标记为离线', 'ACTIVE',
     CURRENT_TIMESTAMP - INTERVAL '60 seconds', NULL),

    -- 设备4 光照异常告警（活跃中）— 户外很亮但灯还开着
    (4, 'LIGHT_ABNORMAL', '设备解放路002号路灯在高光照(92.00)下仍处于开灯状态，可能存在故障', 'ACTIVE',
     CURRENT_TIMESTAMP - INTERVAL '7 minutes', NULL),

    -- 设备1 历史离线告警（已解决）
    (1, 'OFFLINE', '设备人民路001号路灯心跳超时，已自动标记为离线', 'RESOLVED',
     CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '1 day'),

    -- 设备2 历史离线告警（已解决）
    (2, 'OFFLINE', '设备人民路002号路灯心跳超时，已自动标记为离线', 'RESOLVED',
     CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),

    -- 设备5 历史心跳异常（已解决）
    (5, 'HEARTBEAT_TIMEOUT', '设备中山路001号路灯心跳间隔异常，距上次心跳已超过120秒', 'RESOLVED',
     CURRENT_TIMESTAMP - INTERVAL '1 day',  CURRENT_TIMESTAMP - INTERVAL '23 hours'),

    -- 设备4 历史离线告警（已解决）
    (4, 'OFFLINE', '设备解放路002号路灯心跳超时，已自动标记为离线', 'RESOLVED',
     CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '4 days'),

    -- 设备1 历史光照异常告警（已解决）
    (1, 'LIGHT_ABNORMAL', '设备人民路001号路灯在低光照下持续关灯超过30分钟', 'RESOLVED',
     CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '3 days');

-- ============================================================
-- 数据统计验证
-- ============================================================
-- 统计: 各表行数
SELECT 'users'            AS table_name, COUNT(*) AS rows FROM users
UNION ALL SELECT 'devices',         COUNT(*) FROM devices
UNION ALL SELECT 'light_readings',  COUNT(*) FROM light_readings
UNION ALL SELECT 'control_logs',    COUNT(*) FROM control_logs
UNION ALL SELECT 'threshold_config',COUNT(*) FROM threshold_config
UNION ALL SELECT 'alarm_logs',      COUNT(*) FROM alarm_logs
ORDER BY table_name;
