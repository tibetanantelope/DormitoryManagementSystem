-- ============================================================
-- 宿舍管理系统 最终版数据库
-- 核心改动：
--   1. dorm_build 以 dormbuild_id 为主键，新增 dormbuild_type 字段
--   2. dorm_room 删除 first/second/third/fourth_bed 字段
--   3. 新增 dorm_bed 表，作为 dorm_room 和 student 的桥接
--   4. 各表补充外键约束
--   5. apply_time / finish_time 改为 DATETIME 类型
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `visitor`;
DROP TABLE IF EXISTS `notice`;
DROP TABLE IF EXISTS `repair`;
DROP TABLE IF EXISTS `adjust_room`;
DROP TABLE IF EXISTS `dorm_bed`;
DROP TABLE IF EXISTS `dorm_room`;
DROP TABLE IF EXISTS `student`;
DROP TABLE IF EXISTS `dorm_manager`;
DROP TABLE IF EXISTS `dorm_build`;
DROP TABLE IF EXISTS `admin`;

CREATE TABLE `admin` (
  `username` varchar(255) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '密码（BCrypt）',
  `name` varchar(255) NOT NULL COMMENT '姓名',
  `gender` enum('男','女') NOT NULL DEFAULT '男' COMMENT '性别',
  `age` int NOT NULL COMMENT '年龄',
  `phone_num` varchar(11) DEFAULT NULL COMMENT '手机号',
  `email` varchar(255) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='系统管理员';

CREATE TABLE `dorm_build` (
  `dormbuild_id` int NOT NULL COMMENT '楼栋编号',
  `dormbuild_name` varchar(255) NOT NULL COMMENT '楼栋名称',
  `dormbuild_type` enum('男','女') NOT NULL DEFAULT '男' COMMENT '楼栋类型（男宿/女宿）',
  `dormbuild_detail` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dormbuild_id`),
  UNIQUE KEY `uk_dormbuild_name` (`dormbuild_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='楼宇信息';

CREATE TABLE `dorm_manager` (
  `username` varchar(255) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '密码（BCrypt）',
  `dormbuild_id` int NOT NULL COMMENT '所负责楼栋',
  `name` varchar(255) NOT NULL COMMENT '姓名',
  `gender` enum('男','女') NOT NULL DEFAULT '男' COMMENT '性别',
  `age` int NOT NULL COMMENT '年龄',
  `phone_num` varchar(11) DEFAULT NULL COMMENT '手机号',
  `email` varchar(255) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  PRIMARY KEY (`username`),
  CONSTRAINT `fk_manager_build` FOREIGN KEY (`dormbuild_id`)
    REFERENCES `dorm_build` (`dormbuild_id`) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='宿舍管理员';

CREATE TABLE `student` (
  `username` varchar(255) NOT NULL COMMENT '学号',
  `password` varchar(255) NOT NULL COMMENT '密码（BCrypt）',
  `name` varchar(255) NOT NULL COMMENT '姓名',
  `gender` enum('男','女') NOT NULL DEFAULT '男' COMMENT '性别',
  `age` int unsigned NOT NULL COMMENT '年龄',
  `phone_num` varchar(11) DEFAULT NULL COMMENT '手机号',
  `email` varchar(255) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='学生信息';

CREATE TABLE `dorm_room` (
  `dormroom_id` int NOT NULL COMMENT '房间号',
  `dormbuild_id` int NOT NULL COMMENT '所属楼栋',
  `floor_num` int NOT NULL COMMENT '楼层',
  `max_capacity` int NOT NULL DEFAULT 4 COMMENT '最大入住人数',
  PRIMARY KEY (`dormroom_id`),
  CONSTRAINT `fk_room_build` FOREIGN KEY (`dormbuild_id`)
    REFERENCES `dorm_build` (`dormbuild_id`) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='宿舍房间';

CREATE TABLE `dorm_bed` (
  `bed_id` int NOT NULL AUTO_INCREMENT COMMENT '床位主键',
  `dormroom_id` int NOT NULL COMMENT '所属房间号',
  `bed_no` tinyint NOT NULL COMMENT '床位编号（1-4）',
  `username` varchar(255) DEFAULT NULL COMMENT '入住学生学号，NULL 表示空床',
  PRIMARY KEY (`bed_id`),
  UNIQUE KEY `uk_bed` (`dormroom_id`,`bed_no`),
  UNIQUE KEY `uk_bed_student` (`username`),
  CONSTRAINT `fk_bed_room` FOREIGN KEY (`dormroom_id`)
    REFERENCES `dorm_room` (`dormroom_id`) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_bed_student` FOREIGN KEY (`username`)
    REFERENCES `student` (`username`) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='床位信息（dorm_room 与 student 的桥接表）';

CREATE TABLE `adjust_room` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '申请编号',
  `username` varchar(255) NOT NULL COMMENT '申请学生学号',
  `name` varchar(255) NOT NULL COMMENT '申请学生姓名',
  `currentroom_id` int NOT NULL COMMENT '当前房间号',
  `currentbed_id` int NOT NULL COMMENT '当前床位编号',
  `towardsroom_id` int NOT NULL COMMENT '目标房间号',
  `towardsbed_id` int NOT NULL COMMENT '目标床位编号',
  `state` enum('未处理','通过','驳回','处理中','已完成','拒绝执行') NOT NULL DEFAULT '未处理' COMMENT '申请状态',
  `apply_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `finish_time` datetime DEFAULT NULL COMMENT '处理时间',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_adjust_student` FOREIGN KEY (`username`)
    REFERENCES `student` (`username`) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_adjust_currentroom` FOREIGN KEY (`currentroom_id`)
    REFERENCES `dorm_room` (`dormroom_id`) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_adjust_towardsroom` FOREIGN KEY (`towardsroom_id`)
    REFERENCES `dorm_room` (`dormroom_id`) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='调宿申请';

CREATE TABLE `repair` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '报修单编号',
  `repairer` varchar(255) NOT NULL COMMENT '报修人学号',
  `dormbuild_id` int NOT NULL COMMENT '报修楼栋',
  `dormroom_id` int NOT NULL COMMENT '报修房间',
  `title` varchar(255) NOT NULL COMMENT '报修标题',
  `content` longtext NOT NULL COMMENT '报修描述',
  `state` enum('pending','approved','rejected','in_progress','completed') NOT NULL DEFAULT 'pending' COMMENT '报修状态',
  `order_buildtime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `order_finishtime` datetime DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_repair_student` FOREIGN KEY (`repairer`)
    REFERENCES `student` (`username`) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_repair_build` FOREIGN KEY (`dormbuild_id`)
    REFERENCES `dorm_build` (`dormbuild_id`) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_repair_room` FOREIGN KEY (`dormroom_id`)
    REFERENCES `dorm_room` (`dormroom_id`) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报修申请';

CREATE TABLE `notice` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '公告编号',
  `title` varchar(255) NOT NULL COMMENT '标题',
  `content` longtext NOT NULL COMMENT '内容',
  `author` varchar(255) NOT NULL COMMENT '发布人（admin.username）',
  `release_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_notice_admin` FOREIGN KEY (`author`)
    REFERENCES `admin` (`username`) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='公告';

CREATE TABLE `visitor` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '记录编号',
  `name` varchar(255) NOT NULL COMMENT '访客姓名',
  `gender` enum('男','女') NOT NULL DEFAULT '男' COMMENT '性别',
  `phone_num` varchar(255) NOT NULL COMMENT '访客手机号',
  `origin_city` varchar(255) NOT NULL COMMENT '来源城市',
  `visit_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '来访时间',
  `content` varchar(255) NOT NULL COMMENT '来访事由',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='访客记录';

DROP TRIGGER IF EXISTS `trg_bed_before_insert`;
DELIMITER ;;
CREATE TRIGGER `trg_bed_before_insert`
BEFORE INSERT ON `dorm_bed`
FOR EACH ROW
BEGIN
  DECLARE v_count int DEFAULT 0;
  IF NEW.username IS NOT NULL THEN
    SELECT COUNT(*) INTO v_count FROM dorm_bed WHERE username = NEW.username;
    IF v_count > 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '该学生已有床位，不能重复分配';
    END IF;
  END IF;
END;;
DELIMITER ;

DROP TRIGGER IF EXISTS `trg_bed_before_update`;
DELIMITER ;;
CREATE TRIGGER `trg_bed_before_update`
BEFORE UPDATE ON `dorm_bed`
FOR EACH ROW
BEGIN
  DECLARE v_count int DEFAULT 0;
  IF NEW.username IS NOT NULL AND (OLD.username IS NULL OR NEW.username <> OLD.username) THEN
    SELECT COUNT(*) INTO v_count FROM dorm_bed WHERE username = NEW.username AND bed_id <> NEW.bed_id;
    IF v_count > 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '该学生已有床位，不能重复分配';
    END IF;
  END IF;
END;;
DELIMITER ;

DROP PROCEDURE IF EXISTS `sp_execute_adjust_room`;
DELIMITER ;;
CREATE PROCEDURE `sp_execute_adjust_room`(
  IN p_adjust_id int,
  OUT p_result_code int,
  OUT p_result_msg varchar(255)
)
proc: BEGIN
  DECLARE v_username varchar(255);
  DECLARE v_current_room_id int;
  DECLARE v_current_bed_id int;
  DECLARE v_towards_room_id int;
  DECLARE v_towards_bed_id int;
  DECLARE v_state varchar(20);
  DECLARE v_current_bed_key int;
  DECLARE v_target_bed_key int;
  DECLARE v_target_username varchar(255);

  SET p_result_code = -5;
  SET p_result_msg = '执行调宿失败';

  SELECT username, currentroom_id, currentbed_id, towardsroom_id, towardsbed_id, state
    INTO v_username, v_current_room_id, v_current_bed_id, v_towards_room_id, v_towards_bed_id, v_state
  FROM adjust_room WHERE id = p_adjust_id LIMIT 1;

  IF v_username IS NULL THEN
    SET p_result_code = -1;
    SET p_result_msg = '调宿申请不存在';
    LEAVE proc;
  END IF;

  IF v_state <> '通过' THEN
    SET p_result_code = -2;
    SET p_result_msg = '只有审核通过的申请才能执行调宿';
    LEAVE proc;
  END IF;

  SELECT bed_id INTO v_current_bed_key
  FROM dorm_bed
  WHERE dormroom_id = v_current_room_id AND bed_no = v_current_bed_id AND username = v_username
  LIMIT 1;

  IF v_current_bed_key IS NULL THEN
    SET p_result_code = -4;
    SET p_result_msg = '原床位信息不匹配，无法执行调宿';
    LEAVE proc;
  END IF;

  SELECT bed_id, username INTO v_target_bed_key, v_target_username
  FROM dorm_bed
  WHERE dormroom_id = v_towards_room_id AND bed_no = v_towards_bed_id
  LIMIT 1;

  IF v_target_bed_key IS NULL THEN
    SET p_result_code = -3;
    SET p_result_msg = '目标床位不存在';
    LEAVE proc;
  END IF;

  IF v_target_username IS NOT NULL THEN
    SET p_result_code = -3;
    SET p_result_msg = '目标床位已有人';
    LEAVE proc;
  END IF;

  UPDATE dorm_bed SET username = NULL WHERE bed_id = v_current_bed_key;
  UPDATE dorm_bed SET username = v_username WHERE bed_id = v_target_bed_key AND username IS NULL;

  IF ROW_COUNT() <> 1 THEN
    UPDATE dorm_bed SET username = v_username WHERE bed_id = v_current_bed_key;
    SET p_result_code = -3;
    SET p_result_msg = '目标床位写入失败';
    LEAVE proc;
  END IF;

  UPDATE adjust_room SET state = '处理中', finish_time = NOW() WHERE id = p_adjust_id;

  SET p_result_code = 1;
  SET p_result_msg = '调宿执行成功';
END;;
DELIMITER ;

INSERT INTO `admin` VALUES
  ('admin', '$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG', '大强', '男', 18, '14785412478', NULL, 'c4063718784b4e259a61c3e56f2ba01d.png');

INSERT INTO `dorm_build` VALUES
  (1, '一号楼', '男', '男宿舍'),
  (2, '二号楼', '女', '女宿舍'),
  (3, '三号楼', '男', '男宿舍'),
  (4, '四号楼', '女', '女宿舍');

INSERT INTO `dorm_manager` VALUES
  ('dorm1', '$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG', 1, '张三', '男', 35, '15222223333', '12@email.com', NULL),
  ('dorm2', '$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG', 2, '李四', '女', 55, '15333332222', NULL, NULL),
  ('dorm3', '$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG', 3, '王五', '男', 38, '15855552222', NULL, NULL),
  ('dorm4', '$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG', 4, '赵花', '女', 40, '15877776666', NULL, NULL);

INSERT INTO `student` VALUES
  ('stu1', '$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG', '张三', '男', 18, '15833332222', '123@qq.com', NULL),
  ('stu2', '$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG', '田田', '男', 18, '15875359641', NULL, NULL),
  ('stu3', '$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG', '吉安', '男', 18, '15798657350', NULL, NULL),
  ('stu4', '$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG', '力力', '男', 22, '15878965874', NULL, NULL),
  ('stu5', '$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG', '哦哦', '男', 19, '15897535478', NULL, NULL);

INSERT INTO `dorm_room` VALUES
  (1101, 1, 1, 4),
  (1104, 1, 1, 4),
  (1201, 1, 2, 3),
  (2101, 2, 1, 4);

INSERT INTO `dorm_bed` (`dormroom_id`, `bed_no`, `username`) VALUES
  (1101, 1, NULL), (1101, 2, NULL), (1101, 3, NULL), (1101, 4, 'stu4'),
  (1104, 1, 'stu2'), (1104, 2, 'stu3'), (1104, 3, NULL), (1104, 4, NULL),
  (1201, 1, 'stu1'), (1201, 2, 'stu5'), (1201, 3, NULL),
  (2101, 1, NULL), (2101, 2, NULL), (2101, 3, NULL), (2101, 4, NULL);

SET FOREIGN_KEY_CHECKS = 1;
