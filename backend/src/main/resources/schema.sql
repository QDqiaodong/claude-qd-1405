SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET character_set_results = utf8mb4;

-- ---------------------------------------------------------------------
-- 农机合作社 · 4 模块业务库
-- 库名 farm_machine；application.yml 里 ddl-auto: none，表结构全部由本文件在 MySQL 镜像构建期导入。
-- 注意：mysql 容器是 UTC，时间戳一律写 NOW()，日期用 DATE_SUB(CURDATE(), INTERVAL N DAY)。
-- 两处 schema（backend/src/main/resources/schema.sql 与 mysql/schema.sql）内容必须完全一致。
-- 外键顺序：machine / field 先建，workorder 与 maintain 后建。

CREATE DATABASE IF NOT EXISTS farm_machine
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE farm_machine;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS workorder;
DROP TABLE IF EXISTS maintain;
DROP TABLE IF EXISTS `field`;
DROP TABLE IF EXISTS machine;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------- 1. 农机 machine（被 workorder / maintain 引用，须先建） ----------------
CREATE TABLE machine (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    machine_no    VARCHAR(32) COMMENT '机号，唯一',
    machine_type  VARCHAR(16) COMMENT '农机类型：拖拉机 / 收割机 / 播种机 / 旋耕机 / 植保机',
    brand         VARCHAR(32) COMMENT '品牌',
    plate_no      VARCHAR(32) COMMENT '牌号（农机牌照）',
    work_width    DECIMAL(10,2) COMMENT '作业幅宽（米），必须大于 0',
    purchase_date DATE COMMENT '购入日期',
    status        VARCHAR(16) COMMENT '状态：待命 / 作业中 / 维修 / 报废（报废为终态）',
    created_at    DATETIME COMMENT '创建时间',
    updated_at    DATETIME COMMENT '更新时间',
    UNIQUE KEY uk_machine_no (machine_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- 2. 地块 field（被 workorder 引用，须先建） ----------------
CREATE TABLE `field` (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    field_no   VARCHAR(32) COMMENT '地块号，唯一',
    field_name VARCHAR(64) COMMENT '地块名',
    area       DECIMAL(10,2) COMMENT '面积（亩），必须大于 0',
    crop_type  VARCHAR(16) COMMENT '作物类型：小麦 / 玉米 / 水稻 / 大豆 / 油菜',
    soil_type  VARCHAR(16) COMMENT '土壤类型：壤土 / 砂土 / 黏土',
    status     VARCHAR(16) COMMENT '状态：待耕 / 已耕 / 已播 / 已收 / 休耕（休耕不可派作业）',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    UNIQUE KEY uk_field_no (field_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- 3. 作业订单 workorder（依赖 machine + field） ----------------
CREATE TABLE workorder (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    machine_id BIGINT COMMENT '农机 id（须为待命农机才可派工）',
    field_id   BIGINT COMMENT '地块 id（休耕地块不可派作业）',
    operator   VARCHAR(32) COMMENT '农机手',
    work_type  VARCHAR(16) COMMENT '作业类型：耕地 / 播种 / 施肥 / 植保 / 收割',
    plan_date  DATE COMMENT '计划作业日期（同一农机同一日期不可重复派工）',
    work_area  DECIMAL(10,2) COMMENT '作业亩数，大于 0 且不得超过地块面积',
    fuel       DECIMAL(10,2) COMMENT '油耗（升）',
    status     VARCHAR(16) COMMENT '状态：待派工 / 作业中 / 已完成 / 已取消（后两者为终态）',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    CONSTRAINT fk_wo_machine FOREIGN KEY (machine_id) REFERENCES machine (id),
    CONSTRAINT fk_wo_field FOREIGN KEY (field_id) REFERENCES `field` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- 4. 维修保养 maintain（依赖 machine） ----------------
CREATE TABLE maintain (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    machine_id    BIGINT COMMENT '农机 id（报废农机不可再维修）',
    maintain_type VARCHAR(16) COMMENT '维修类型：日常保养 / 故障维修 / 年检 / 季前检修',
    start_date    DATE COMMENT '开始日期',
    end_date      DATE COMMENT '结束日期，可空，不得早于开始日期',
    cost          DECIMAL(10,2) COMMENT '维修费用（元），不得为负',
    mechanic      VARCHAR(32) COMMENT '维修人',
    status        VARCHAR(16) COMMENT '状态：待修 / 维修中 / 已完成（已完成为终态）',
    created_at    DATETIME COMMENT '创建时间',
    updated_at    DATETIME COMMENT '更新时间',
    CONSTRAINT fk_mt_machine FOREIGN KEY (machine_id) REFERENCES machine (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ---------------------------------------------------------------------
-- 种子数据（含异常态，用于真实触发业务拒绝分支）
--   异常态 1：M4 状态「作业中」→ 派工会被拒绝（只有待命农机可派工）
--   异常态 2：M5 状态「维修」  → 派工会被拒绝，且已有维修中记录
--   异常态 3：M6 状态「报废」  → 终态，不可派工、不可送修、不可修改
--   异常态 4：F5 状态「已收」  → 只能回到待耕开新一轮；直接派播种 / 收割会触发跳级拒绝
--   异常态 5：F6 状态「休耕」  → 休耕地块不可派作业
--   异常态 6：W2 状态「作业中」（挂在作业中农机 M4 上）→ 不可重复开工
--   异常态 7：W7 状态「已取消」、W8 挂在报废农机 M6 上 → 终态不可再改
--   异常态 8：W1 已完成 → 终态不可再改；W3~W6 待派工可开工
--   异常态 9：T1 维修中（对应维修状态的农机 M5）；T2/T4/T5 已完成不可再改
--   异常态 10：T5 挂在报废农机 M6 上（历史记录），再建维修单会被拒绝
-- ---------------------------------------------------------------------

-- ---------------- 农机 machine（8 行） ----------------
INSERT IGNORE INTO machine (id, machine_no, machine_type, brand, plate_no, work_width, purchase_date, status, created_at, updated_at) VALUES
(1, 'NJ-001', '拖拉机', '东方红',  '冀A·NJ001', 2.50, DATE_SUB(CURDATE(), INTERVAL 900 DAY), '待命',   NOW(), NOW()),
(2, 'NJ-002', '收割机', '雷沃谷神', '冀A·NJ002', 4.20, DATE_SUB(CURDATE(), INTERVAL 700 DAY), '待命',   NOW(), NOW()),
(3, 'NJ-003', '播种机', '大华宝来', '冀A·NJ003', 3.60, DATE_SUB(CURDATE(), INTERVAL 500 DAY), '待命',   NOW(), NOW()),
(4, 'NJ-004', '旋耕机', '神耕',    '冀A·NJ004', 2.30, DATE_SUB(CURDATE(), INTERVAL 400 DAY), '作业中', NOW(), NOW()),
(5, 'NJ-005', '植保机', '丰疆智能', '冀A·NJ005', 12.00, DATE_SUB(CURDATE(), INTERVAL 300 DAY), '维修',  NOW(), NOW()),
(6, 'NJ-006', '拖拉机', '约翰迪尔', '冀A·NJ006', 3.00, DATE_SUB(CURDATE(), INTERVAL 1500 DAY), '报废',  NOW(), NOW()),
(7, 'NJ-007', '收割机', '沃得',    '冀A·NJ007', 4.50, DATE_SUB(CURDATE(), INTERVAL 200 DAY), '待命',   NOW(), NOW()),
(8, 'NJ-008', '播种机', '马斯奇奥', '冀A·NJ008', 3.20, DATE_SUB(CURDATE(), INTERVAL 120 DAY), '待命',   NOW(), NOW());

-- ---------------- 地块 field（8 行） ----------------
INSERT IGNORE INTO `field` (id, field_no, field_name, area, crop_type, soil_type, status, created_at, updated_at) VALUES
(1, 'DK-A01', '东河地',   120.00, '小麦', '壤土', '已耕', NOW(), NOW()),
(2, 'DK-A02', '南岗地',    80.00, '玉米', '砂土', '待耕', NOW(), NOW()),
(3, 'DK-B01', '西洼地',   150.00, '水稻', '黏土', '已耕', NOW(), NOW()),
(4, 'DK-B02', '北坡地',    60.00, '大豆', '壤土', '已播', NOW(), NOW()),
(5, 'DK-C01', '老河滩地',  45.00, '油菜', '砂土', '已收', NOW(), NOW()),
(6, 'DK-C02', '荒草地',    30.00, '小麦', '黏土', '休耕', NOW(), NOW()),
(7, 'DK-D01', '新村东地', 200.00, '玉米', '壤土', '待耕', NOW(), NOW()),
(8, 'DK-D02', '果园边地',  90.00, '大豆', '砂土', '已耕', NOW(), NOW());

-- ---------------- 作业订单 workorder（8 行） ----------------
INSERT IGNORE INTO workorder (id, machine_id, field_id, operator, work_type, plan_date, work_area, fuel, status, created_at, updated_at) VALUES
(1, 1, 1, '张建国', '耕地', DATE_SUB(CURDATE(), INTERVAL 6 DAY),  100.00, 85.50, '已完成', NOW(), NOW()),
(2, 4, 3, '李振海', '播种', DATE_SUB(CURDATE(), INTERVAL 2 DAY),  150.00, 62.00, '作业中', NOW(), NOW()),
(3, 7, 4, '王守田', '收割', DATE_SUB(CURDATE(), INTERVAL 1 DAY),   60.00, 40.00, '待派工', NOW(), NOW()),
(4, 2, 7, '赵大川', '耕地', DATE_SUB(CURDATE(), INTERVAL -1 DAY), 180.00, NULL,  '待派工', NOW(), NOW()),
(5, 3, 8, '孙有福', '播种', DATE_SUB(CURDATE(), INTERVAL -2 DAY),  90.00, NULL,  '待派工', NOW(), NOW()),
(6, 8, 2, '周立春', '耕地', DATE_SUB(CURDATE(), INTERVAL -3 DAY),  80.00, NULL,  '待派工', NOW(), NOW()),
(7, 1, 5, '吴满仓', '施肥', CURDATE(),                             45.00, NULL,  '已取消', NOW(), NOW()),
(8, 6, 1, '郑老栓', '播种', DATE_SUB(CURDATE(), INTERVAL 20 DAY), 100.00, 0.00,  '已取消', NOW(), NOW());

-- ---------------- 维修保养 maintain（6 行） ----------------
INSERT IGNORE INTO maintain (id, machine_id, maintain_type, start_date, end_date, cost, mechanic, status, created_at, updated_at) VALUES
(1, 5, '故障维修', DATE_SUB(CURDATE(), INTERVAL 3 DAY),  NULL,                                   1200.00, '王师傅', '维修中', NOW(), NOW()),
(2, 1, '日常保养', DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_SUB(CURDATE(), INTERVAL 9 DAY),      300.00, '李师傅', '已完成', NOW(), NOW()),
(3, 2, '季前检修', DATE_SUB(CURDATE(), INTERVAL 8 DAY),  NULL,                                       0.00, '待定',   '待修',   NOW(), NOW()),
(4, 3, '年检',     DATE_SUB(CURDATE(), INTERVAL 5 DAY),  DATE_SUB(CURDATE(), INTERVAL 4 DAY),      800.00, '赵师傅', '已完成', NOW(), NOW()),
(5, 6, '故障维修', DATE_SUB(CURDATE(), INTERVAL 25 DAY), DATE_SUB(CURDATE(), INTERVAL 22 DAY),    5200.00, '孙师傅', '已完成', NOW(), NOW()),
(6, 8, '日常保养', DATE_SUB(CURDATE(), INTERVAL -1 DAY), NULL,                                       0.00, '待定',   '待修',   NOW(), NOW());
