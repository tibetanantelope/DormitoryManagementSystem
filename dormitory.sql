-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: localhost    Database: dormitory
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `adjust_room`
--

DROP TABLE IF EXISTS `adjust_room`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `adjust_room` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '账号',
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '姓名',
  `currentroom_id` int NOT NULL COMMENT '当前房间',
  `currentbed_id` int NOT NULL COMMENT '当前床位号',
  `towardsroom_id` int NOT NULL COMMENT '目标房间',
  `towardsbed_id` int NOT NULL COMMENT '目标床位号',
  `state` enum('未处理','通过','驳回','处理中','已完成','拒绝执行') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '未处理' COMMENT '申请状态',
  `apply_time` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '申请时间',
  `finish_time` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '处理时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Stored procedures
--

DROP PROCEDURE IF EXISTS `sp_execute_adjust_room`;
DELIMITER ;;
CREATE PROCEDURE `sp_execute_adjust_room`(
  IN p_adjust_id INT,
  OUT p_result_code INT,
  OUT p_result_msg VARCHAR(255)
)
proc: BEGIN
  DECLARE v_username VARCHAR(255);
  DECLARE v_current_room_id INT;
  DECLARE v_current_bed_id INT;
  DECLARE v_towards_room_id INT;
  DECLARE v_towards_bed_id INT;
  DECLARE v_state VARCHAR(20);
  DECLARE v_current_bed_value VARCHAR(255);
  DECLARE v_target_bed_value VARCHAR(255);
  DECLARE v_current_room_count INT DEFAULT 0;
  DECLARE v_target_room_count INT DEFAULT 0;

  SET p_result_code = -5;
  SET p_result_msg = '执行调宿失败';

  SELECT username, currentroom_id, currentbed_id, towardsroom_id, towardsbed_id, state
    INTO v_username, v_current_room_id, v_current_bed_id, v_towards_room_id, v_towards_bed_id, v_state
  FROM adjust_room
  WHERE id = p_adjust_id
  LIMIT 1;

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

  SELECT COUNT(*) INTO v_current_room_count
  FROM dorm_room
  WHERE dormroom_id = v_current_room_id;

  IF v_current_room_count = 0 THEN
    SET p_result_code = -4;
    SET p_result_msg = '原床位信息不匹配，无法执行调宿';
    LEAVE proc;
  END IF;

  SELECT CASE v_current_bed_id
           WHEN 1 THEN first_bed
           WHEN 2 THEN second_bed
           WHEN 3 THEN third_bed
           WHEN 4 THEN fourth_bed
           ELSE NULL
         END
    INTO v_current_bed_value
  FROM dorm_room
  WHERE dormroom_id = v_current_room_id
  LIMIT 1;

  IF v_current_bed_value IS NULL OR v_current_bed_value <> v_username THEN
    SET p_result_code = -4;
    SET p_result_msg = '原床位信息不匹配，无法执行调宿';
    LEAVE proc;
  END IF;

  SELECT COUNT(*) INTO v_target_room_count
  FROM dorm_room
  WHERE dormroom_id = v_towards_room_id;

  IF v_target_room_count = 0 THEN
    SET p_result_code = -3;
    SET p_result_msg = '目标床位已有人';
    LEAVE proc;
  END IF;

  SELECT CASE v_towards_bed_id
           WHEN 1 THEN first_bed
           WHEN 2 THEN second_bed
           WHEN 3 THEN third_bed
           WHEN 4 THEN fourth_bed
           ELSE NULL
         END
    INTO v_target_bed_value
  FROM dorm_room
  WHERE dormroom_id = v_towards_room_id
  LIMIT 1;

  IF v_target_bed_value IS NOT NULL AND v_target_bed_value <> '' THEN
    SET p_result_code = -3;
    SET p_result_msg = '目标床位已有人';
    LEAVE proc;
  END IF;

  IF v_current_bed_id = 1 THEN
    UPDATE dorm_room SET first_bed = NULL WHERE dormroom_id = v_current_room_id AND first_bed = v_username;
  ELSEIF v_current_bed_id = 2 THEN
    UPDATE dorm_room SET second_bed = NULL WHERE dormroom_id = v_current_room_id AND second_bed = v_username;
  ELSEIF v_current_bed_id = 3 THEN
    UPDATE dorm_room SET third_bed = NULL WHERE dormroom_id = v_current_room_id AND third_bed = v_username;
  ELSEIF v_current_bed_id = 4 THEN
    UPDATE dorm_room SET fourth_bed = NULL WHERE dormroom_id = v_current_room_id AND fourth_bed = v_username;
  ELSE
    SET p_result_code = -4;
    SET p_result_msg = '原床位信息不匹配，无法执行调宿';
    LEAVE proc;
  END IF;

  IF ROW_COUNT() <> 1 THEN
    SET p_result_code = -4;
    SET p_result_msg = '原床位信息不匹配，无法执行调宿';
    LEAVE proc;
  END IF;

  IF v_towards_bed_id = 1 THEN
    UPDATE dorm_room SET first_bed = v_username WHERE dormroom_id = v_towards_room_id AND first_bed IS NULL;
  ELSEIF v_towards_bed_id = 2 THEN
    UPDATE dorm_room SET second_bed = v_username WHERE dormroom_id = v_towards_room_id AND second_bed IS NULL;
  ELSEIF v_towards_bed_id = 3 THEN
    UPDATE dorm_room SET third_bed = v_username WHERE dormroom_id = v_towards_room_id AND third_bed IS NULL;
  ELSEIF v_towards_bed_id = 4 THEN
    UPDATE dorm_room SET fourth_bed = v_username WHERE dormroom_id = v_towards_room_id AND fourth_bed IS NULL;
  ELSE
    SET p_result_code = -3;
    SET p_result_msg = '目标床位已有人';
    LEAVE proc;
  END IF;

  IF ROW_COUNT() <> 1 THEN
    IF v_current_bed_id = 1 THEN
      UPDATE dorm_room SET first_bed = v_username WHERE dormroom_id = v_current_room_id AND first_bed IS NULL;
    ELSEIF v_current_bed_id = 2 THEN
      UPDATE dorm_room SET second_bed = v_username WHERE dormroom_id = v_current_room_id AND second_bed IS NULL;
    ELSEIF v_current_bed_id = 3 THEN
      UPDATE dorm_room SET third_bed = v_username WHERE dormroom_id = v_current_room_id AND third_bed IS NULL;
    ELSEIF v_current_bed_id = 4 THEN
      UPDATE dorm_room SET fourth_bed = v_username WHERE dormroom_id = v_current_room_id AND fourth_bed IS NULL;
    END IF;
    SET p_result_code = -3;
    SET p_result_msg = '目标床位已有人';
    LEAVE proc;
  END IF;

  UPDATE adjust_room
  SET state = '处理中',
      finish_time = DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
  WHERE id = p_adjust_id;

  SET p_result_code = 1;
  SET p_result_msg = '调宿执行成功';
END;;
DELIMITER ;

--
-- Dumping data for table `adjust_room`
--

LOCK TABLES `adjust_room` WRITE;
/*!40000 ALTER TABLE `adjust_room` DISABLE KEYS */;
/*!40000 ALTER TABLE `adjust_room` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `admin`
--

DROP TABLE IF EXISTS `admin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin` (
  `username` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '密码',
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '姓名',
  `gender` enum('男','女') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '男' COMMENT '性别',
  `age` int NOT NULL COMMENT '年龄',
  `phone_num` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '手机号',
  `email` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '头像',
  PRIMARY KEY (`username`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin`
--

LOCK TABLES `admin` WRITE;
/*!40000 ALTER TABLE `admin` DISABLE KEYS */;
INSERT INTO `admin` VALUES ('admin','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG','大强','男',18,'14785412478',NULL,'c4063718784b4e259a61c3e56f2ba01d.png'),('Atest','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG','测试管理员','男',22,'14785412478',NULL,NULL);
/*!40000 ALTER TABLE `admin` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dorm_build`
--

DROP TABLE IF EXISTS `dorm_build`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dorm_build` (
  `dormbuild_id` int NOT NULL COMMENT '宿舍楼号码',
  `dormbuild_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '宿舍楼名称',
  `dormbuild_detail` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '宿舍楼备注',
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dorm_build`
--

LOCK TABLES `dorm_build` WRITE;
/*!40000 ALTER TABLE `dorm_build` DISABLE KEYS */;
INSERT INTO `dorm_build` VALUES (1,'一号楼','男宿舍',1),(2,'二号楼','女宿舍',2),(3,'三号楼','男宿舍',3),(4,'四号楼','女宿舍',4);
/*!40000 ALTER TABLE `dorm_build` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dorm_manager`
--

DROP TABLE IF EXISTS `dorm_manager`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dorm_manager` (
  `username` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '密码',
  `dormbuild_id` int NOT NULL COMMENT '所管理的宿舍楼栋号',
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '名字',
  `gender` enum('男','女') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '男' COMMENT '性别',
  `age` int NOT NULL COMMENT '年龄',
  `phone_num` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '手机号',
  `email` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '头像',
  PRIMARY KEY (`username`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dorm_manager`
--

LOCK TABLES `dorm_manager` WRITE;
/*!40000 ALTER TABLE `dorm_manager` DISABLE KEYS */;
INSERT INTO `dorm_manager` VALUES ('dorm1','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',1,'张三','男',35,'15222223333','12@email.com',NULL),('dorm2','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',2,'李四','女',55,'15333332222',NULL,NULL),('dorm3','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',3,'王五','男',38,'15855552222',NULL,NULL),('dorm4','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',4,'赵花','女',40,'15877776666',NULL,NULL),('Mtest','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',2,'宿管测试','男',22,'15899999999',NULL,NULL);
/*!40000 ALTER TABLE `dorm_manager` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dorm_room`
--

DROP TABLE IF EXISTS `dorm_room`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dorm_room` (
  `dormroom_id` int NOT NULL COMMENT '宿舍房间号',
  `dormbuild_id` int NOT NULL COMMENT '宿舍楼号',
  `floor_num` int NOT NULL COMMENT '楼层',
  `max_capacity` int NOT NULL DEFAULT '4' COMMENT '房间最大入住人数',
  `current_capacity` int NOT NULL DEFAULT '0' COMMENT '当前房间入住人数',
  `first_bed` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '一号床位',
  `second_bed` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '二号床位',
  `third_bed` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '三号床位',
  `fourth_bed` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '四号床位',
  PRIMARY KEY (`dormroom_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Triggers for table `dorm_room`
--

DROP TRIGGER IF EXISTS `trg_dorm_room_before_insert`;
DELIMITER ;;
CREATE TRIGGER `trg_dorm_room_before_insert`
BEFORE INSERT ON `dorm_room`
FOR EACH ROW
BEGIN
  SET NEW.current_capacity =
      (CASE WHEN NEW.first_bed IS NULL OR NEW.first_bed = '' THEN 0 ELSE 1 END) +
      (CASE WHEN NEW.second_bed IS NULL OR NEW.second_bed = '' THEN 0 ELSE 1 END) +
      (CASE WHEN NEW.third_bed IS NULL OR NEW.third_bed = '' THEN 0 ELSE 1 END) +
      (CASE WHEN NEW.fourth_bed IS NULL OR NEW.fourth_bed = '' THEN 0 ELSE 1 END);
END;;
DELIMITER ;

DROP TRIGGER IF EXISTS `trg_dorm_room_before_update`;
DELIMITER ;;
CREATE TRIGGER `trg_dorm_room_before_update`
BEFORE UPDATE ON `dorm_room`
FOR EACH ROW
BEGIN
  SET NEW.current_capacity =
      (CASE WHEN NEW.first_bed IS NULL OR NEW.first_bed = '' THEN 0 ELSE 1 END) +
      (CASE WHEN NEW.second_bed IS NULL OR NEW.second_bed = '' THEN 0 ELSE 1 END) +
      (CASE WHEN NEW.third_bed IS NULL OR NEW.third_bed = '' THEN 0 ELSE 1 END) +
      (CASE WHEN NEW.fourth_bed IS NULL OR NEW.fourth_bed = '' THEN 0 ELSE 1 END);
END;;
DELIMITER ;

--
-- Dumping data for table `dorm_room`
--

LOCK TABLES `dorm_room` WRITE;
/*!40000 ALTER TABLE `dorm_room` DISABLE KEYS */;
INSERT INTO `dorm_room` VALUES (1101,1,1,4,2,'stu22',NULL,NULL,'stu4'),(1103,1,1,4,4,'stu8','stu9','stu10','stu11'),(1104,1,1,4,2,'stu2','stu3',NULL,NULL),(1201,1,2,3,2,'stu1','stu5',NULL,NULL),(2101,2,1,4,3,'stu12','stu13','stu14',NULL),(3101,3,1,4,3,'stu15','stu6','stu16',NULL),(4102,4,1,4,3,'stu17','stu18','stu19',NULL);
/*!40000 ALTER TABLE `dorm_room` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notice`
--

DROP TABLE IF EXISTS `notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notice` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主题',
  `content` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '内容',
  `author` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '作者',
  `release_time` datetime NOT NULL COMMENT '发布时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notice`
--

LOCK TABLES `notice` WRITE;
/*!40000 ALTER TABLE `notice` DISABLE KEYS */;
INSERT INTO `notice` VALUES (3,'天气降温，注意保暖','<p>明天上海即将大降温，并伴随大降雨，请同学们注意增添衣物注意保暖，并且关好门窗，注意安全！</p>','大强','2026-05-02 15:32:27');
/*!40000 ALTER TABLE `notice` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `repair`
--

DROP TABLE IF EXISTS `repair`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `repair` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '订单编号',
  `repairer` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '报修人',
  `dormbuild_id` int NOT NULL COMMENT '报修宿舍楼',
  `dormroom_id` int NOT NULL COMMENT '报修宿舍房间号',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '表单标题',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '表单内容',
  `state` enum('pending','approved','rejected','in_progress','completed') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '订单状态',
  `order_buildtime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '订单创建时间',
  `order_finishtime` datetime DEFAULT NULL COMMENT '订单完成时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1645228035 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `repair`
--

LOCK TABLES `repair` WRITE;
/*!40000 ALTER TABLE `repair` DISABLE KEYS */;
/*!40000 ALTER TABLE `repair` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student`
--

DROP TABLE IF EXISTS `student`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student` (
  `username` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '学号',
  `password` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '密码',
  `age` int unsigned NOT NULL COMMENT '年龄',
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '姓名',
  `gender` enum('男','女') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '男' COMMENT '性别',
  `phone_num` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '手机号',
  `email` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '头像',
  PRIMARY KEY (`username`) USING BTREE,
  UNIQUE KEY `stu_num` (`username`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student`
--

LOCK TABLES `student` WRITE;
/*!40000 ALTER TABLE `student` DISABLE KEYS */;
INSERT INTO `student` VALUES ('Stest','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',20,'学生测试','男','13233332222',NULL,NULL),('stu1','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',18,'张三','男','15833332222','123@qq.com','c4063718784b4e259a61c3e56f2ba01d.png'),('stu10','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',19,'马克','女','15833333333',NULL,NULL),('stu11','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',16,'巧巧','女','18922223333',NULL,NULL),('stu12','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',17,'丽丽','女','17922222222',NULL,NULL),('stu13','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',18,'美美','女','15822222222',NULL,NULL),('stu14','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',20,'拉拉','女','13355556666',NULL,NULL),('stu15','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',18,'贝贝','男','15899999999',NULL,NULL),('stu16','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',18,'力力','男','14596475257',NULL,NULL),('stu17','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',18,'阿成','男','15896542147',NULL,NULL),('stu18','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',19,'阿达','女','14785635874','akk@akkmail.com',NULL),('stu19','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',19,'帕森斯','男','15889658475',NULL,NULL),('stu2','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',18,'田田','男','15875359641',NULL,NULL),('stu20','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',21,'柠檬','男','15874563558',NULL,NULL),('stu21','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',21,'面对','男','15889635874',NULL,NULL),('stu22','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',25,'等等','男','13412341234','akkk@kkk.com',NULL),('stu3','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',18,'吉安','男','15798657350',NULL,NULL),('stu4','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',22,'力力','男','15878965874',NULL,NULL),('stu5','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',19,'哦哦','男','15897535478',NULL,NULL),('stu6','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',18,'泡泡','男','18987554765',NULL,NULL),('stu7','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',15,'刚刚','男','15897543854',NULL,NULL),('stu8','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',18,'七七','男','12332143215',NULL,NULL),('stu9','$2a$10$zCiQjjGjSO0WWqSqYAzrneft4tNKaz1VENnM8EYG7FkZu1HnAkAXG',20,'德萨','男','15889658741',NULL,NULL);
/*!40000 ALTER TABLE `student` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `visitor`
--

DROP TABLE IF EXISTS `visitor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `visitor` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '姓名',
  `gender` enum('男','女') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '男' COMMENT '性别',
  `phone_num` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '电话',
  `origin_city` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '来源城市',
  `visit_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '来访时间',
  `content` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '事情',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `visitor`
--

LOCK TABLES `visitor` WRITE;
/*!40000 ALTER TABLE `visitor` DISABLE KEYS */;
/*!40000 ALTER TABLE `visitor` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-02 18:05:39
