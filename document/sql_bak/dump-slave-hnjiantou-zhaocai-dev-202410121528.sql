-- MySQL dump 10.13  Distrib 5.7.34, for Win64 (x86_64)
--
-- Host: 172.168.100.199    Database: hnjiantou-zhaocai-dev
-- ------------------------------------------------------
-- Server version	8.4.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `act_evt_log`
--

DROP TABLE IF EXISTS `act_evt_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_evt_log` (
  `LOG_NR_` bigint NOT NULL AUTO_INCREMENT,
  `TYPE_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TIME_STAMP_` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `USER_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DATA_` longblob,
  `LOCK_OWNER_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `IS_PROCESSED_` tinyint DEFAULT '0',
  PRIMARY KEY (`LOG_NR_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_expand_info`
--

DROP TABLE IF EXISTS `act_expand_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_expand_info` (
  `proinst_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '实例id',
  `business_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务类型',
  `business_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务id',
  `business_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务名称',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '备注',
  PRIMARY KEY (`proinst_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ge_bytearray`
--

DROP TABLE IF EXISTS `act_ge_bytearray`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_ge_bytearray` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTES_` longblob,
  `GENERATED_` tinyint DEFAULT NULL,
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `ACT_FK_BYTEARR_DEPL` (`DEPLOYMENT_ID_`) USING BTREE,
  CONSTRAINT `ACT_FK_BYTEARR_DEPL` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_re_deployment` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ge_property`
--

DROP TABLE IF EXISTS `act_ge_property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_ge_property` (
  `NAME_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `VALUE_` varchar(300) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  PRIMARY KEY (`NAME_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_hi_actinst`
--

DROP TABLE IF EXISTS `act_hi_actinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_hi_actinst` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `ACT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `TASK_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CALL_PROC_INST_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `ASSIGNEE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `TRANSACTION_ORDER_` int DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `ACT_IDX_HI_ACT_INST_START` (`START_TIME_`) USING BTREE,
  KEY `ACT_IDX_HI_ACT_INST_END` (`END_TIME_`) USING BTREE,
  KEY `ACT_IDX_HI_ACT_INST_PROCINST` (`PROC_INST_ID_`,`ACT_ID_`) USING BTREE,
  KEY `ACT_IDX_HI_ACT_INST_EXEC` (`EXECUTION_ID_`,`ACT_ID_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_hi_attachment`
--

DROP TABLE IF EXISTS `act_hi_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_hi_attachment` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `USER_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `URL_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CONTENT_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_hi_comment`
--

DROP TABLE IF EXISTS `act_hi_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_hi_comment` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TIME_` datetime(3) NOT NULL,
  `USER_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ACTION_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `MESSAGE_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `FULL_MSG_` longblob,
  PRIMARY KEY (`ID_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_hi_detail`
--

DROP TABLE IF EXISTS `act_hi_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_hi_detail` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_INST_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `VAR_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  `TIME_` datetime(3) NOT NULL,
  `BYTEARRAY_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `ACT_IDX_HI_DETAIL_PROC_INST` (`PROC_INST_ID_`) USING BTREE,
  KEY `ACT_IDX_HI_DETAIL_ACT_INST` (`ACT_INST_ID_`) USING BTREE,
  KEY `ACT_IDX_HI_DETAIL_TIME` (`TIME_`) USING BTREE,
  KEY `ACT_IDX_HI_DETAIL_NAME` (`NAME_`) USING BTREE,
  KEY `ACT_IDX_HI_DETAIL_TASK_ID` (`TASK_ID_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_hi_entitylink`
--

DROP TABLE IF EXISTS `act_hi_entitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_hi_entitylink` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `LINK_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_ELEMENT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_DEFINITION_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `HIERARCHY_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `ACT_IDX_HI_ENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`) USING BTREE,
  KEY `ACT_IDX_HI_ENT_LNK_REF_SCOPE` (`REF_SCOPE_ID_`,`REF_SCOPE_TYPE_`,`LINK_TYPE_`) USING BTREE,
  KEY `ACT_IDX_HI_ENT_LNK_ROOT_SCOPE` (`ROOT_SCOPE_ID_`,`ROOT_SCOPE_TYPE_`,`LINK_TYPE_`) USING BTREE,
  KEY `ACT_IDX_HI_ENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_hi_identitylink`
--

DROP TABLE IF EXISTS `act_hi_identitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_hi_identitylink` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `GROUP_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `ACT_IDX_HI_IDENT_LNK_USER` (`USER_ID_`) USING BTREE,
  KEY `ACT_IDX_HI_IDENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_HI_IDENT_LNK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_HI_IDENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_HI_IDENT_LNK_TASK` (`TASK_ID_`) USING BTREE,
  KEY `ACT_IDX_HI_IDENT_LNK_PROCINST` (`PROC_INST_ID_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_hi_procinst`
--

DROP TABLE IF EXISTS `act_hi_procinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_hi_procinst` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_INST_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `BUSINESS_KEY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `START_USER_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `START_ACT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `END_ACT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SUPER_PROCESS_INSTANCE_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '',
  `NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`) USING BTREE,
  UNIQUE KEY `PROC_INST_ID_` (`PROC_INST_ID_`) USING BTREE,
  KEY `ACT_IDX_HI_PRO_INST_END` (`END_TIME_`) USING BTREE,
  KEY `ACT_IDX_HI_PRO_I_BUSKEY` (`BUSINESS_KEY_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_hi_taskinst`
--

DROP TABLE IF EXISTS `act_hi_taskinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_hi_taskinst` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_KEY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_TASK_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `OWNER_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ASSIGNEE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `CLAIM_TIME_` datetime(3) DEFAULT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PRIORITY_` int DEFAULT NULL,
  `DUE_DATE_` datetime(3) DEFAULT NULL,
  `FORM_KEY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '',
  `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `ACT_IDX_HI_TASK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_HI_TASK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_HI_TASK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_HI_TASK_INST_PROCINST` (`PROC_INST_ID_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_hi_tsk_log`
--

DROP TABLE IF EXISTS `act_hi_tsk_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_hi_tsk_log` (
  `ID_` bigint NOT NULL AUTO_INCREMENT,
  `TYPE_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `TIME_STAMP_` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `USER_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DATA_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_hi_varinst`
--

DROP TABLE IF EXISTS `act_hi_varinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_hi_varinst` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_INST_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `VAR_TYPE_` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTEARRAY_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `ACT_IDX_HI_PROCVAR_NAME_TYPE` (`NAME_`,`VAR_TYPE_`) USING BTREE,
  KEY `ACT_IDX_HI_VAR_SCOPE_ID_TYPE` (`SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_HI_VAR_SUB_ID_TYPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_HI_PROCVAR_PROC_INST` (`PROC_INST_ID_`) USING BTREE,
  KEY `ACT_IDX_HI_PROCVAR_TASK_ID` (`TASK_ID_`) USING BTREE,
  KEY `ACT_IDX_HI_PROCVAR_EXE` (`EXECUTION_ID_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_id_bytearray`
--

DROP TABLE IF EXISTS `act_id_bytearray`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_id_bytearray` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTES_` longblob,
  PRIMARY KEY (`ID_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_id_group`
--

DROP TABLE IF EXISTS `act_id_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_id_group` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_id_info`
--

DROP TABLE IF EXISTS `act_id_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_id_info` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `USER_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `VALUE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PASSWORD_` longblob,
  `PARENT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_id_membership`
--

DROP TABLE IF EXISTS `act_id_membership`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_id_membership` (
  `USER_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `GROUP_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`USER_ID_`,`GROUP_ID_`) USING BTREE,
  KEY `ACT_FK_MEMB_GROUP` (`GROUP_ID_`) USING BTREE,
  CONSTRAINT `ACT_FK_MEMB_GROUP` FOREIGN KEY (`GROUP_ID_`) REFERENCES `act_id_group` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_MEMB_USER` FOREIGN KEY (`USER_ID_`) REFERENCES `act_id_user` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_id_priv`
--

DROP TABLE IF EXISTS `act_id_priv`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_id_priv` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`ID_`) USING BTREE,
  UNIQUE KEY `ACT_UNIQ_PRIV_NAME` (`NAME_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_id_priv_mapping`
--

DROP TABLE IF EXISTS `act_id_priv_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_id_priv_mapping` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `PRIV_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `USER_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `GROUP_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `ACT_FK_PRIV_MAPPING` (`PRIV_ID_`) USING BTREE,
  KEY `ACT_IDX_PRIV_USER` (`USER_ID_`) USING BTREE,
  KEY `ACT_IDX_PRIV_GROUP` (`GROUP_ID_`) USING BTREE,
  CONSTRAINT `ACT_FK_PRIV_MAPPING` FOREIGN KEY (`PRIV_ID_`) REFERENCES `act_id_priv` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_id_property`
--

DROP TABLE IF EXISTS `act_id_property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_id_property` (
  `NAME_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `VALUE_` varchar(300) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `REV_` int DEFAULT NULL,
  PRIMARY KEY (`NAME_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_id_token`
--

DROP TABLE IF EXISTS `act_id_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_id_token` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TOKEN_VALUE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TOKEN_DATE_` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `IP_ADDRESS_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_AGENT_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TOKEN_DATA_` varchar(2000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_id_user`
--

DROP TABLE IF EXISTS `act_id_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_id_user` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `FIRST_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `LAST_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DISPLAY_NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `EMAIL_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PWD_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PICTURE_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_procdef_info`
--

DROP TABLE IF EXISTS `act_procdef_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_procdef_info` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `PROC_DEF_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `INFO_JSON_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`) USING BTREE,
  UNIQUE KEY `ACT_UNIQ_INFO_PROCDEF` (`PROC_DEF_ID_`) USING BTREE,
  KEY `ACT_IDX_INFO_PROCDEF` (`PROC_DEF_ID_`) USING BTREE,
  KEY `ACT_FK_INFO_JSON_BA` (`INFO_JSON_ID_`) USING BTREE,
  CONSTRAINT `ACT_FK_INFO_JSON_BA` FOREIGN KEY (`INFO_JSON_ID_`) REFERENCES `act_ge_bytearray` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_INFO_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_re_deployment`
--

DROP TABLE IF EXISTS `act_re_deployment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_re_deployment` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '',
  `DEPLOY_TIME_` timestamp(3) NULL DEFAULT NULL,
  `DERIVED_FROM_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_ROOT_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_DEPLOYMENT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ENGINE_VERSION_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_re_model`
--

DROP TABLE IF EXISTS `act_re_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_re_model` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LAST_UPDATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `META_INFO_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `EDITOR_SOURCE_VALUE_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `EDITOR_SOURCE_EXTRA_VALUE_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `ACT_FK_MODEL_SOURCE` (`EDITOR_SOURCE_VALUE_ID_`) USING BTREE,
  KEY `ACT_FK_MODEL_SOURCE_EXTRA` (`EDITOR_SOURCE_EXTRA_VALUE_ID_`) USING BTREE,
  KEY `ACT_FK_MODEL_DEPLOYMENT` (`DEPLOYMENT_ID_`) USING BTREE,
  CONSTRAINT `ACT_FK_MODEL_DEPLOYMENT` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_re_deployment` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_MODEL_SOURCE` FOREIGN KEY (`EDITOR_SOURCE_VALUE_ID_`) REFERENCES `act_ge_bytearray` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_MODEL_SOURCE_EXTRA` FOREIGN KEY (`EDITOR_SOURCE_EXTRA_VALUE_ID_`) REFERENCES `act_ge_bytearray` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_re_procdef`
--

DROP TABLE IF EXISTS `act_re_procdef`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_re_procdef` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `KEY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `VERSION_` int NOT NULL,
  `DEPLOYMENT_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `RESOURCE_NAME_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DGRM_RESOURCE_NAME_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `HAS_START_FORM_KEY_` tinyint DEFAULT NULL,
  `HAS_GRAPHICAL_NOTATION_` tinyint DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '',
  `ENGINE_VERSION_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_FROM_ROOT_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DERIVED_VERSION_` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID_`) USING BTREE,
  UNIQUE KEY `ACT_UNIQ_PROCDEF` (`KEY_`,`VERSION_`,`DERIVED_VERSION_`,`TENANT_ID_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_actinst`
--

DROP TABLE IF EXISTS `act_ru_actinst`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_ru_actinst` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT '1',
  `PROC_DEF_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `ACT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `TASK_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CALL_PROC_INST_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `ASSIGNEE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `DURATION_` bigint DEFAULT NULL,
  `TRANSACTION_ORDER_` int DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `ACT_IDX_RU_ACTI_START` (`START_TIME_`) USING BTREE,
  KEY `ACT_IDX_RU_ACTI_END` (`END_TIME_`) USING BTREE,
  KEY `ACT_IDX_RU_ACTI_PROC` (`PROC_INST_ID_`) USING BTREE,
  KEY `ACT_IDX_RU_ACTI_PROC_ACT` (`PROC_INST_ID_`,`ACT_ID_`) USING BTREE,
  KEY `ACT_IDX_RU_ACTI_EXEC` (`EXECUTION_ID_`) USING BTREE,
  KEY `ACT_IDX_RU_ACTI_EXEC_ACT` (`EXECUTION_ID_`,`ACT_ID_`) USING BTREE,
  KEY `ACT_IDX_RU_ACTI_TASK` (`TASK_ID_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_deadletter_job`
--

DROP TABLE IF EXISTS `act_ru_deadletter_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_ru_deadletter_job` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `ACT_IDX_DEADLETTER_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`) USING BTREE,
  KEY `ACT_IDX_DEADLETTER_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`) USING BTREE,
  KEY `ACT_IDX_DEADLETTER_JOB_CORRELATION_ID` (`CORRELATION_ID_`) USING BTREE,
  KEY `ACT_IDX_DJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_DJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_DJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_FK_DEADLETTER_JOB_EXECUTION` (`EXECUTION_ID_`) USING BTREE,
  KEY `ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`) USING BTREE,
  KEY `ACT_FK_DEADLETTER_JOB_PROC_DEF` (`PROC_DEF_ID_`) USING BTREE,
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_entitylink`
--

DROP TABLE IF EXISTS `act_ru_entitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_ru_entitylink` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `LINK_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_ELEMENT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `REF_SCOPE_DEFINITION_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_SCOPE_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `HIERARCHY_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `ACT_IDX_ENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`) USING BTREE,
  KEY `ACT_IDX_ENT_LNK_REF_SCOPE` (`REF_SCOPE_ID_`,`REF_SCOPE_TYPE_`,`LINK_TYPE_`) USING BTREE,
  KEY `ACT_IDX_ENT_LNK_ROOT_SCOPE` (`ROOT_SCOPE_ID_`,`ROOT_SCOPE_TYPE_`,`LINK_TYPE_`) USING BTREE,
  KEY `ACT_IDX_ENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`,`LINK_TYPE_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_event_subscr`
--

DROP TABLE IF EXISTS `act_ru_event_subscr`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_ru_event_subscr` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `EVENT_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `EVENT_NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ACTIVITY_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CONFIGURATION_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATED_` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `PROC_DEF_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `ACT_IDX_EVENT_SUBSCR_CONFIG_` (`CONFIGURATION_`) USING BTREE,
  KEY `ACT_FK_EVENT_EXEC` (`EXECUTION_ID_`) USING BTREE,
  CONSTRAINT `ACT_FK_EVENT_EXEC` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_execution`
--

DROP TABLE IF EXISTS `act_ru_execution`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_ru_execution` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `BUSINESS_KEY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SUPER_EXEC_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ROOT_PROC_INST_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ACT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `IS_ACTIVE_` tinyint DEFAULT NULL,
  `IS_CONCURRENT_` tinyint DEFAULT NULL,
  `IS_SCOPE_` tinyint DEFAULT NULL,
  `IS_EVENT_SCOPE_` tinyint DEFAULT NULL,
  `IS_MI_ROOT_` tinyint DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `CACHED_ENT_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '',
  `NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `START_ACT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `START_TIME_` datetime(3) DEFAULT NULL,
  `START_USER_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `IS_COUNT_ENABLED_` tinyint DEFAULT NULL,
  `EVT_SUBSCR_COUNT_` int DEFAULT NULL,
  `TASK_COUNT_` int DEFAULT NULL,
  `JOB_COUNT_` int DEFAULT NULL,
  `TIMER_JOB_COUNT_` int DEFAULT NULL,
  `SUSP_JOB_COUNT_` int DEFAULT NULL,
  `DEADLETTER_JOB_COUNT_` int DEFAULT NULL,
  `EXTERNAL_WORKER_JOB_COUNT_` int DEFAULT NULL,
  `VAR_COUNT_` int DEFAULT NULL,
  `ID_LINK_COUNT_` int DEFAULT NULL,
  `CALLBACK_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CALLBACK_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `REFERENCE_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `BUSINESS_STATUS_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `ACT_IDX_EXEC_BUSKEY` (`BUSINESS_KEY_`) USING BTREE,
  KEY `ACT_IDC_EXEC_ROOT` (`ROOT_PROC_INST_ID_`) USING BTREE,
  KEY `ACT_IDX_EXEC_REF_ID_` (`REFERENCE_ID_`) USING BTREE,
  KEY `ACT_FK_EXE_PROCINST` (`PROC_INST_ID_`) USING BTREE,
  KEY `ACT_FK_EXE_PARENT` (`PARENT_ID_`) USING BTREE,
  KEY `ACT_FK_EXE_SUPER` (`SUPER_EXEC_`) USING BTREE,
  KEY `ACT_FK_EXE_PROCDEF` (`PROC_DEF_ID_`) USING BTREE,
  CONSTRAINT `ACT_FK_EXE_PARENT` FOREIGN KEY (`PARENT_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_EXE_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_EXE_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `ACT_FK_EXE_SUPER` FOREIGN KEY (`SUPER_EXEC_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_external_job`
--

DROP TABLE IF EXISTS `act_ru_external_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_ru_external_job` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `ACT_IDX_EXTERNAL_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`) USING BTREE,
  KEY `ACT_IDX_EXTERNAL_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`) USING BTREE,
  KEY `ACT_IDX_EXTERNAL_JOB_CORRELATION_ID` (`CORRELATION_ID_`) USING BTREE,
  KEY `ACT_IDX_EJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_EJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_EJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`) USING BTREE,
  CONSTRAINT `ACT_FK_EXTERNAL_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_EXTERNAL_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_history_job`
--

DROP TABLE IF EXISTS `act_ru_history_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_ru_history_job` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ADV_HANDLER_CFG_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_identitylink`
--

DROP TABLE IF EXISTS `act_ru_identitylink`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_ru_identitylink` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `GROUP_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `USER_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `ACT_IDX_IDENT_LNK_USER` (`USER_ID_`) USING BTREE,
  KEY `ACT_IDX_IDENT_LNK_GROUP` (`GROUP_ID_`) USING BTREE,
  KEY `ACT_IDX_IDENT_LNK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_IDENT_LNK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_IDENT_LNK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_ATHRZ_PROCEDEF` (`PROC_DEF_ID_`) USING BTREE,
  KEY `ACT_FK_TSKASS_TASK` (`TASK_ID_`) USING BTREE,
  KEY `ACT_FK_IDL_PROCINST` (`PROC_INST_ID_`) USING BTREE,
  CONSTRAINT `ACT_FK_ATHRZ_PROCEDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_IDL_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_TSKASS_TASK` FOREIGN KEY (`TASK_ID_`) REFERENCES `act_ru_task` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_job`
--

DROP TABLE IF EXISTS `act_ru_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_ru_job` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `ACT_IDX_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`) USING BTREE,
  KEY `ACT_IDX_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`) USING BTREE,
  KEY `ACT_IDX_JOB_CORRELATION_ID` (`CORRELATION_ID_`) USING BTREE,
  KEY `ACT_IDX_JOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_JOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_JOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_FK_JOB_EXECUTION` (`EXECUTION_ID_`) USING BTREE,
  KEY `ACT_FK_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`) USING BTREE,
  KEY `ACT_FK_JOB_PROC_DEF` (`PROC_DEF_ID_`) USING BTREE,
  CONSTRAINT `ACT_FK_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_suspended_job`
--

DROP TABLE IF EXISTS `act_ru_suspended_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_ru_suspended_job` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `ACT_IDX_SUSPENDED_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`) USING BTREE,
  KEY `ACT_IDX_SUSPENDED_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`) USING BTREE,
  KEY `ACT_IDX_SUSPENDED_JOB_CORRELATION_ID` (`CORRELATION_ID_`) USING BTREE,
  KEY `ACT_IDX_SJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_SJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_SJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_FK_SUSPENDED_JOB_EXECUTION` (`EXECUTION_ID_`) USING BTREE,
  KEY `ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`) USING BTREE,
  KEY `ACT_FK_SUSPENDED_JOB_PROC_DEF` (`PROC_DEF_ID_`) USING BTREE,
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_task`
--

DROP TABLE IF EXISTS `act_ru_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_ru_task` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROPAGATED_STAGE_INST_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PARENT_TASK_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_DEF_KEY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `OWNER_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ASSIGNEE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DELEGATION_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PRIORITY_` int DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `DUE_DATE_` datetime(3) DEFAULT NULL,
  `CATEGORY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SUSPENSION_STATE_` int DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '',
  `FORM_KEY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CLAIM_TIME_` datetime(3) DEFAULT NULL,
  `IS_COUNT_ENABLED_` tinyint DEFAULT NULL,
  `VAR_COUNT_` int DEFAULT NULL,
  `ID_LINK_COUNT_` int DEFAULT NULL,
  `SUB_TASK_COUNT_` int DEFAULT NULL,
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `ACT_IDX_TASK_CREATE` (`CREATE_TIME_`) USING BTREE,
  KEY `ACT_IDX_TASK_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_TASK_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_TASK_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_FK_TASK_EXE` (`EXECUTION_ID_`) USING BTREE,
  KEY `ACT_FK_TASK_PROCINST` (`PROC_INST_ID_`) USING BTREE,
  KEY `ACT_FK_TASK_PROCDEF` (`PROC_DEF_ID_`) USING BTREE,
  CONSTRAINT `ACT_FK_TASK_EXE` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_TASK_PROCDEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_TASK_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_timer_job`
--

DROP TABLE IF EXISTS `act_ru_timer_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_ru_timer_job` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `CATEGORY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `ELEMENT_NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_DEFINITION_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CORRELATION_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `RETRIES_` int DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CUSTOM_VALUES_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `ACT_IDX_TIMER_JOB_EXCEPTION_STACK_ID` (`EXCEPTION_STACK_ID_`) USING BTREE,
  KEY `ACT_IDX_TIMER_JOB_CUSTOM_VALUES_ID` (`CUSTOM_VALUES_ID_`) USING BTREE,
  KEY `ACT_IDX_TIMER_JOB_CORRELATION_ID` (`CORRELATION_ID_`) USING BTREE,
  KEY `ACT_IDX_TIMER_JOB_DUEDATE` (`DUEDATE_`) USING BTREE,
  KEY `ACT_IDX_TJOB_SCOPE` (`SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_TJOB_SUB_SCOPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_TJOB_SCOPE_DEF` (`SCOPE_DEFINITION_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_FK_TIMER_JOB_EXECUTION` (`EXECUTION_ID_`) USING BTREE,
  KEY `ACT_FK_TIMER_JOB_PROCESS_INSTANCE` (`PROCESS_INSTANCE_ID_`) USING BTREE,
  KEY `ACT_FK_TIMER_JOB_PROC_DEF` (`PROC_DEF_ID_`) USING BTREE,
  CONSTRAINT `ACT_FK_TIMER_JOB_CUSTOM_VALUES` FOREIGN KEY (`CUSTOM_VALUES_ID_`) REFERENCES `act_ge_bytearray` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_TIMER_JOB_EXCEPTION` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `act_ge_bytearray` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_TIMER_JOB_EXECUTION` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_TIMER_JOB_PROC_DEF` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `act_re_procdef` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_TIMER_JOB_PROCESS_INSTANCE` FOREIGN KEY (`PROCESS_INSTANCE_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `act_ru_variable`
--

DROP TABLE IF EXISTS `act_ru_variable`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `act_ru_variable` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `NAME_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `BYTEARRAY_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint DEFAULT NULL,
  `TEXT_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `ACT_IDX_RU_VAR_SCOPE_ID_TYPE` (`SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_IDX_RU_VAR_SUB_ID_TYPE` (`SUB_SCOPE_ID_`,`SCOPE_TYPE_`) USING BTREE,
  KEY `ACT_FK_VAR_BYTEARRAY` (`BYTEARRAY_ID_`) USING BTREE,
  KEY `ACT_IDX_VARIABLE_TASK_ID` (`TASK_ID_`) USING BTREE,
  KEY `ACT_FK_VAR_EXE` (`EXECUTION_ID_`) USING BTREE,
  KEY `ACT_FK_VAR_PROCINST` (`PROC_INST_ID_`) USING BTREE,
  CONSTRAINT `ACT_FK_VAR_BYTEARRAY` FOREIGN KEY (`BYTEARRAY_ID_`) REFERENCES `act_ge_bytearray` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_VAR_EXE` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ACT_FK_VAR_PROCINST` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `act_ru_execution` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `contract_base`
--

DROP TABLE IF EXISTS `contract_base`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `contract_base` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '主键',
  `unique_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '唯一id',
  `introduce_flag` tinyint(1) DEFAULT '0' COMMENT '是否引入',
  `pre_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '上一版本id',
  `version` int DEFAULT NULL COMMENT '版本号',
  `change_mode` tinyint(1) DEFAULT NULL COMMENT '变更方式（1-变更 2-签证 3-索赔 4-补充协议 5-其他）',
  `change_reason_remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '0' COMMENT '变更原因',
  `last_version_flag` tinyint(1) DEFAULT '1' COMMENT '是否最新版',
  `con_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '合同编码',
  `Internal_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '单位内部合同管理编码，文本50',
  `con_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '合同名称',
  `project_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '项目Id',
  `transaction_subject_matter` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '交易标的物',
  `program_archives_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '合同类型(合约规划档案id)',
  `program_archives_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '合同类型(合约规划档案名称)',
  `program_archives_tax_amount` decimal(16,4) DEFAULT '0.0000' COMMENT '合约规划，规划金额（含税），快照',
  `program_archives_used_amount` decimal(16,4) DEFAULT '0.0000' COMMENT '合约规划，已发生规划金额（含税）, 快照',
  `program_archives_surplus_amount` decimal(16,4) DEFAULT '0.0000' COMMENT '合约规划，剩余金额（含税）, 快照',
  `parta_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '合同甲方,可配置',
  `parta_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '合同甲方名称',
  `partb_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '合同乙方,可配置',
  `partb_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '合同乙方名称',
  `partb_legal_person` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '乙方法人代表',
  `partb_legal_person_id_card` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '乙方法人代表身份证',
  `partb_legal_person_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '乙方法人代表电话',
  `partb_site_manager` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '乙方现场实际履职负责人',
  `partb_site_manager_id_card` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '乙方现场实际履职负责人身份证',
  `partb_site_manager_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '乙方现场实际履职负责人电话',
  `con_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '0' COMMENT '支出业务分类( Z-其他 A-劳务分包 B-专业分包 C-购买材料 D-租赁材料 G-设备租赁（机械）)',
  `rent_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '计租方式 1算头又算尾  2算头不算尾  3算尾不算头  4头尾都不算',
  `payment_cycle` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '1' COMMENT '支付周期（1-按月支付 2-按进度节点支付 3-按节日节点支付）',
  `payment_type` char(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '1' COMMENT '支付方式（1-银行转账 2-商业汇票 3-供应链金融产品  4-固定资产抵扣）',
  `start_date` datetime DEFAULT NULL COMMENT '进场日期（年月日）',
  `end_date` datetime DEFAULT NULL COMMENT '完工日期（年月日）',
  `duration` decimal(10,0) DEFAULT NULL COMMENT '工期（完工日-进场日，单位：天）',
  `con_status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT 'A' COMMENT '合同状态（A-签署中 B-已签署 C-已到期、未续签 D-已终中止 E-合同履行完毕 F-合同解除）',
  `settle_status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '0' COMMENT '结算状态（0-未结算，1-部分结算，2-已完工结算）',
  `sign_date` datetime DEFAULT NULL COMMENT '合同签订日期（年月日）',
  `valid_date` datetime DEFAULT NULL COMMENT '合同生效日期（年月日）',
  `price_form` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '0' COMMENT '价格形式(1-固定单价2-总价下浮3-固定总价)',
  `down_ratio` decimal(5,2) DEFAULT '0.00' COMMENT '下浮比例(%)',
  `work_scope_content` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '工程范围及工作内容',
  `tax_sign_amount` decimal(16,4) DEFAULT '0.0000' COMMENT '合同签订金额（含税）（元）',
  `ntax_sign_amount` decimal(16,4) DEFAULT '0.0000' COMMENT '合同签订金额（不含税）（元）',
  `sign_tax` decimal(16,4) DEFAULT '0.0000' COMMENT '签订税额(初始签订金额)',
  `tax_change_amount` decimal(16,4) DEFAULT '0.0000' COMMENT '合同变更金额（含税）（元）（合同变更回写）',
  `tax_changed_amount` decimal(16,4) DEFAULT '0.0000' COMMENT '合同变更后金额（元）（含税）（合同签订金额+合同变更金额）',
  `ntax_changed_amount` decimal(16,4) DEFAULT '0.0000' COMMENT '合同变更后金额（元）（不含税）',
  `total_tax_settle_amount` decimal(16,4) DEFAULT '0.0000' COMMENT '累计结算(元) （结算单回写）',
  `total_ntax_settle_amount` decimal(16,4) DEFAULT '0.0000' COMMENT '累计结算金额（无税）（结算单回写）',
  `total_tax_out_con_settle_amount` decimal(16,4) DEFAULT '0.0000' COMMENT '合同外累计结算金额（结算单回写）',
  `total_ntax_out_con_settle_amount` decimal(16,4) DEFAULT '0.0000' COMMENT '累计合同外结算金额（不含税）（结算单回写）',
  `remain_tax_con_amount` decimal(16,4) DEFAULT '0.0000' COMMENT '剩余合同金额\r\n\r\n(含税)(元)（=合同变更后金额 - 累计结算金额）',
  `remain_ntax_con_amount` decimal(16,4) DEFAULT '0.0000' COMMENT '合同剩余金额（不含税）（元）',
  `total_actual_payment_amount` decimal(16,4) DEFAULT '0.0000' COMMENT '实付付款金额（含税）(元)(累计实付付款金额)',
  `settle_owed_amount` decimal(16,4) DEFAULT '0.0000' COMMENT '按结算金额欠付金额(含税)(元)（结算金额-实付付款金额）',
  `whether_complete_settle` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '0' COMMENT '完工结算状态（0-否，1-是）',
  `money_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT 'CNY' COMMENT '币种：引用GB/T12406-2022表示货币的代码',
  `invoice_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '1' COMMENT '发票类型（1-专票 2-普票 3-数电票）',
  `tax_rate` decimal(3,0) DEFAULT '0' COMMENT '合同税率(%)',
  `imprest_ratio` decimal(5,2) DEFAULT '0.00' COMMENT '约定预付款比例(%）',
  `imprest_amount` decimal(16,4) DEFAULT '0.0000' COMMENT '约定预付款金额（元）',
  `imprest_deduct_condition` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '预付款扣回条件',
  `imprest_deduct_point` decimal(10,0) DEFAULT NULL COMMENT '预付款全部扣回截止点(%)',
  `out_con_settle_ratio` decimal(10,2) DEFAULT '0.00' COMMENT '允许合同外结算占合同比例(%)',
  `whether_associate_steel_price` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '是否关联我的钢铁网价格（0-否，1-是）',
  `market_short_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '市场简称（是在关联我的钢铁网价格为“是”的情况下才有值）',
  `steel_price_float` decimal(10,2) DEFAULT NULL COMMENT '我的钢铁网价格浮动值',
  `stagnate_machine_settle_ratio` decimal(5,2) DEFAULT NULL COMMENT '停滞台班结算比例(%)',
  `change_field` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '可变更字段(JSON)',
  `is_exceed` tinyint unsigned DEFAULT '0' COMMENT '是否超量',
  `proc_status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '0' COMMENT '流程状态 0-自由态 1-审核中 2-驳回 3-弃审 4-完成',
  `proc_complete_time` datetime DEFAULT NULL COMMENT '审批完成时间',
  `last_proc_approver` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '最后审批人',
  `next_proc_approver` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '下一审批人',
  `proc_org_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '审批流程组织id',
  `supplier_status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '供应商状态 0未发送、1已发送、2已签收、3已确认',
  `create_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '创建人id',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `attach_business_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '附件id',
  `valid` int NOT NULL DEFAULT '0' COMMENT '删除状态（0-生效 其他删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='合同基础信息表-主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `contract_materials_list`
--

DROP TABLE IF EXISTS `contract_materials_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `contract_materials_list` (
  `id` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `unique_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '唯一id',
  `con_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同ID',
  `subject_code` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本子目编码',
  `subject_name` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本子目名称',
  `specs` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '特征描述(规格型号)',
  `measure_unit` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量单位',
  `metrological_rules` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量规则',
  `basic_job` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工作内容',
  `quantity` decimal(10,2) DEFAULT NULL COMMENT '工程量',
  `tax_rate` decimal(16,4) DEFAULT NULL COMMENT '税率',
  `ntax_price` decimal(16,4) DEFAULT NULL COMMENT '单价（不含税）',
  `tax_price` decimal(16,4) DEFAULT NULL COMMENT '单价（含税）,=单价（不含税）*（1+增值税率）=单价（含税）',
  `ntax_amount` decimal(16,4) DEFAULT NULL COMMENT '金额（不含税）',
  `tax_amount` decimal(16,4) DEFAULT NULL COMMENT '金额（含税）',
  `tax` decimal(16,4) DEFAULT NULL COMMENT '税额，单价（不含税）*税率',
  `settle_status` char(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结算状态',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人id',
  `create_by` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `valid` int DEFAULT NULL COMMENT '删除状态（0-生效 其他删除）',
  `brand` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '品牌',
  `rent_mode` char(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租赁方式(1-日 2-月租 3-工作量)',
  `rent_unit` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计租单位',
  `number` decimal(12,2) DEFAULT NULL COMMENT '数量',
  `rent_time` decimal(12,2) DEFAULT NULL COMMENT '租赁时间',
  `used_quantity` decimal(10,2) DEFAULT NULL COMMENT '已进场工程量',
  `cost_type` char(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '价款类型(1-固定价 2-浮动价)',
  `order_quantity` decimal(12,2) DEFAULT NULL COMMENT '已下订单数量（工程量）',
  `con_type` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同类型',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='支出合同-合同清单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `da_road_line`
--

DROP TABLE IF EXISTS `da_road_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `da_road_line` (
  `id` bigint NOT NULL COMMENT '主键',
  `dept_id` bigint DEFAULT NULL,
  `ancestors` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `road_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路线编码',
  `road_full_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路线全码',
  `road_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路线类型 1 高速 2 非高速',
  `road_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路线名称',
  `begin_milestone` decimal(8,3) DEFAULT NULL COMMENT '起点',
  `end_milestone` decimal(8,3) DEFAULT NULL COMMENT '止点',
  `gy_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '管养类型 1 正常管养 2 政府管养',
  `direction` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '道路方向  1 东西横线  2 南北纵线',
  `road_full_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路线全称',
  `order_no` int DEFAULT '0' COMMENT '排序号',
  `attachments` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '附件',
  `del_flag` tinyint DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='路线';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dw_mm_asset_inf`
--

DROP TABLE IF EXISTS `dw_mm_asset_inf`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dw_mm_asset_inf` (
  `id` bigint NOT NULL COMMENT 'id',
  `asset_class_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '资产分类编码',
  `asset_class_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '资产分类名称',
  `asset_class_level_ext` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类级次_扩展',
  `asset_class_level` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类级次',
  `asset_class_level_cd` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类级次cd',
  `belg_pre_asset_class_ext` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '所属上级资产分类_扩展',
  `belg_pre_asset_class_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '所属上级资产分类名称',
  `belg_pre_asset_class_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '所属上级资产分类id',
  `belg_pre_asset_class_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '所属上级资产分类编码',
  `is_subject_matter_ext` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否标的物_扩展',
  `is_subject_matter` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否标的物',
  `is_subject_matter_cd` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否标的物cd',
  `source_remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '来源备注',
  `standard_tax_rate` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '标准税率(%)',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '其他备注',
  `ext1` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '扩展字段',
  `report_status` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '上报状态',
  `report_time` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '上报时间',
  `internal_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '内部ID',
  `internal_parent_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '内部父级ID',
  `instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程实例ID',
  `approval_status` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程审批状态',
  `dept_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '组织ID',
  `crypto_field` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '加密存储字段',
  `measure_unit` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '单位',
  `map_asset_class_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '映射资产分类编码',
  `map_asset_class_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '映射资产分类名称',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '由谁创建',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='资产 dm0731/材料分类';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dw_mm_service_inf`
--

DROP TABLE IF EXISTS `dw_mm_service_inf`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dw_mm_service_inf` (
  `id` bigint NOT NULL COMMENT 'id',
  `service_class_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '资产分类编码',
  `service_class_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '资产分类名称',
  `service_class_level_ext` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类级次_扩展',
  `service_class_level` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类级次',
  `service_class_level_cd` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类级次cd',
  `belg_pre_service_class_ext` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '所属上级资产分类_扩展',
  `belg_pre_service_class_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '所属上级资产分类名称',
  `belg_pre_service_class_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '所属上级资产分类id',
  `belg_pre_service_class_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '所属上级资产分类编码',
  `is_subject_matter_ext` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否标的物_扩展',
  `is_subject_matter` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否标的物',
  `is_subject_matter_cd` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否标的物cd',
  `source_remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '来源备注',
  `standard_tax_rate` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '标准税率(%)',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '其他备注',
  `ext1` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '扩展字段',
  `report_status` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '上报状态',
  `report_time` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '上报时间',
  `internal_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '内部ID',
  `internal_parent_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '内部父级ID',
  `instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程实例ID',
  `approval_status` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程审批状态',
  `dept_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '组织ID',
  `crypto_field` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '加密存储字段',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '由谁创建',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='服务 dm073';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flw_channel_definition`
--

DROP TABLE IF EXISTS `flw_channel_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `flw_channel_definition` (
  `ID_` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `NAME_` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `VERSION_` int DEFAULT NULL,
  `KEY_` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `RESOURCE_NAME_` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DESCRIPTION_` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `TYPE_` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `IMPLEMENTATION_` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`) USING BTREE,
  UNIQUE KEY `ACT_IDX_CHANNEL_DEF_UNIQ` (`KEY_`,`VERSION_`,`TENANT_ID_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flw_ev_databasechangelog`
--

DROP TABLE IF EXISTS `flw_ev_databasechangelog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `flw_ev_databasechangelog` (
  `ID` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `AUTHOR` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `FILENAME` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `DATEEXECUTED` datetime NOT NULL,
  `ORDEREXECUTED` int NOT NULL,
  `EXECTYPE` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `MD5SUM` varchar(35) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DESCRIPTION` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `COMMENTS` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `TAG` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `LIQUIBASE` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `CONTEXTS` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `LABELS` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DEPLOYMENT_ID` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flw_ev_databasechangeloglock`
--

DROP TABLE IF EXISTS `flw_ev_databasechangeloglock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `flw_ev_databasechangeloglock` (
  `ID` int NOT NULL,
  `LOCKED` bit(1) NOT NULL,
  `LOCKGRANTED` datetime DEFAULT NULL,
  `LOCKEDBY` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flw_event_deployment`
--

DROP TABLE IF EXISTS `flw_event_deployment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `flw_event_deployment` (
  `ID_` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `NAME_` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `CATEGORY_` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DEPLOY_TIME_` datetime(3) DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `PARENT_DEPLOYMENT_ID_` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flw_ru_batch`
--

DROP TABLE IF EXISTS `flw_ru_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `flw_ru_batch` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `TYPE_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `SEARCH_KEY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SEARCH_KEY2_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) NOT NULL,
  `COMPLETE_TIME_` datetime(3) DEFAULT NULL,
  `STATUS_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `BATCH_DOC_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flw_ru_batch_part`
--

DROP TABLE IF EXISTS `flw_ru_batch_part`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `flw_ru_batch_part` (
  `ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `REV_` int DEFAULT NULL,
  `BATCH_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TYPE_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `SCOPE_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SUB_SCOPE_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SCOPE_TYPE_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SEARCH_KEY_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `SEARCH_KEY2_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) NOT NULL,
  `COMPLETE_TIME_` datetime(3) DEFAULT NULL,
  `STATUS_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `RESULT_DOC_ID_` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '',
  PRIMARY KEY (`ID_`) USING BTREE,
  KEY `FLW_IDX_BATCH_PART` (`BATCH_ID_`) USING BTREE,
  CONSTRAINT `FLW_FK_BATCH_PART_PARENT` FOREIGN KEY (`BATCH_ID_`) REFERENCES `flw_ru_batch` (`ID_`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gen_table`
--

DROP TABLE IF EXISTS `gen_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `gen_table` (
  `table_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '表描述',
  `sub_table_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联子表的表名',
  `sub_table_fk_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '子表关联的外键名',
  `class_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '实体类名称',
  `tpl_category` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
  `tpl_web_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '前端模板类型（element-ui模版 element-plus模版）',
  `package_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生成功能作者',
  `gen_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  `gen_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
  `options` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '其它生成选项',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=110 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='代码生成业务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gen_table_column`
--

DROP TABLE IF EXISTS `gen_table_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `gen_table_column` (
  `column_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_id` bigint DEFAULT NULL COMMENT '归属表编号',
  `column_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '列名称',
  `column_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '列描述',
  `column_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '列类型',
  `java_type` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'JAVA类型',
  `java_field` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'JAVA字段名',
  `is_pk` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否主键（1是）',
  `is_increment` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否自增（1是）',
  `is_required` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否必填（1是）',
  `is_insert` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否为插入字段（1是）',
  `is_edit` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否编辑字段（1是）',
  `is_list` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否列表字段（1是）',
  `is_query` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否查询字段（1是）',
  `query_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
  `html_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  `dict_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典类型',
  `sort` int DEFAULT NULL COMMENT '排序',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`column_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1836 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='代码生成业务表字段';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_config`
--

DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_config` (
  `config_id` int NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `config_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='参数配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_deploy_form`
--

DROP TABLE IF EXISTS `sys_deploy_form`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_deploy_form` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `form_id` bigint DEFAULT NULL COMMENT '表单主键',
  `deploy_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例主键',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `form_id` (`form_id`) USING BTREE,
  KEY `deploy_id` (`deploy_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='流程实例关联表单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dept`
--

DROP TABLE IF EXISTS `sys_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_dept` (
  `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门id',
  `ancestors` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '部门名称',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `leader` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `origin` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '来源',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `thrid_dept_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方部门id',
  `thrid_parent_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方父部门id',
  `thrid_org_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方组织类型',
  `thrid_org_level` int DEFAULT NULL COMMENT '第三方组织层次（一级组织|二级组织|.....）',
  PRIMARY KEY (`dept_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1831566955480993798 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dept_copy1`
--

DROP TABLE IF EXISTS `sys_dept_copy1`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_dept_copy1` (
  `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门id',
  `ancestors` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '部门名称',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `leader` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `origin` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '来源',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `thrid_dept_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方部门id',
  `thrid_parent_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方父部门id',
  PRIMARY KEY (`dept_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1812684578352136194 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dept_copy1_copy1`
--

DROP TABLE IF EXISTS `sys_dept_copy1_copy1`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_dept_copy1_copy1` (
  `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门id',
  `ancestors` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '部门名称',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `leader` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `origin` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '来源',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `thrid_dept_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方部门id',
  `thrid_parent_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方父部门id',
  `thrid_org_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方组织类型',
  PRIMARY KEY (`dept_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1813458195602133049 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dict_data`
--

DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_dict_data` (
  `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort` int DEFAULT '0' COMMENT '字典排序',
  `dict_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=346 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dict_type`
--

DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_dict_type` (
  `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典类型',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`) USING BTREE,
  UNIQUE KEY `dict_type` (`dict_type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=146 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_expression`
--

DROP TABLE IF EXISTS `sys_expression`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_expression` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '表单主键',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表达式名称',
  `expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表达式内容',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人员',
  `update_by` bigint DEFAULT NULL COMMENT '更新人员',
  `status` tinyint DEFAULT '0' COMMENT '状态',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='流程表达式';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_form`
--

DROP TABLE IF EXISTS `sys_form`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_form` (
  `form_id` bigint NOT NULL AUTO_INCREMENT COMMENT '表单主键',
  `form_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表单名称',
  `form_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '表单内容',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人员',
  `update_by` bigint DEFAULT NULL COMMENT '更新人员',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`form_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='流程表单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_job`
--

DROP TABLE IF EXISTS `sys_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_job` (
  `job_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调用目标字符串',
  `cron_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT 'cron执行表达式',
  `misfire_policy` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
  `concurrent` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1暂停）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '备注信息',
  PRIMARY KEY (`job_id`,`job_name`,`job_group`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=111 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='定时任务调度表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_job_log`
--

DROP TABLE IF EXISTS `sys_job_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_job_log` (
  `job_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调用目标字符串',
  `job_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '日志信息',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
  `exception_info` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '异常信息',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`job_log_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=527210 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='定时任务调度日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_listener`
--

DROP TABLE IF EXISTS `sys_listener`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_listener` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '表单主键',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `type` char(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '监听类型',
  `event_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '事件类型',
  `value_type` char(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '值类型',
  `value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行内容',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人员',
  `update_by` bigint DEFAULT NULL COMMENT '更新人员',
  `status` tinyint DEFAULT '0' COMMENT '状态',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='流程监听';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_logininfor`
--

DROP TABLE IF EXISTS `sys_logininfor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_logininfor` (
  `info_id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '用户账号',
  `ipaddr` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '登录IP地址',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '提示信息',
  `access_time` datetime DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`info_id`) USING BTREE,
  KEY `idx_sys_logininfor_s` (`status`) USING BTREE,
  KEY `idx_sys_logininfor_lt` (`access_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=88427 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='系统访问记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_menu` (
  `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组件路径',
  `query` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路由参数',
  `is_frame` int DEFAULT '1' COMMENT '是否为外链（0是 1否）',
  `is_cache` int DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '#' COMMENT '菜单图标',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`menu_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2040 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='菜单权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_notice`
--

DROP TABLE IF EXISTS `sys_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_notice` (
  `notice_id` int NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `notice_title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告标题',
  `notice_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告类型（1通知 2公告）',
  `notice_content` longblob COMMENT '公告内容',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`notice_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='通知公告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_oper_log`
--

DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_oper_log` (
  `oper_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '模块标题',
  `business_type` int DEFAULT '0' COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '请求方式',
  `operator_type` int DEFAULT '0' COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '操作地点',
  `oper_param` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '请求参数',
  `json_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '返回参数',
  `status` int DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint DEFAULT '0' COMMENT '消耗时间',
  PRIMARY KEY (`oper_id`) USING BTREE,
  KEY `idx_sys_oper_log_bt` (`business_type`) USING BTREE,
  KEY `idx_sys_oper_log_s` (`status`) USING BTREE,
  KEY `idx_sys_oper_log_ot` (`oper_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3246 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='操作日志记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_post`
--

DROP TABLE IF EXISTS `sys_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_post` (
  `post_id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `post_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位名称',
  `post_sort` int NOT NULL COMMENT '显示顺序',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`post_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='岗位信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_process_title`
--

DROP TABLE IF EXISTS `sys_process_title`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_process_title` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `proc_ins_id` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '流程实例ID',
  `process_title` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '流程标题',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=686 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色权限字符串',
  `role_sort` int NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  `menu_check_strictly` tinyint(1) DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(1) DEFAULT '1' COMMENT '部门树选择项是否关联显示',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=108 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='角色信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role_dept`
--

DROP TABLE IF EXISTS `sys_role_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role_dept` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`,`dept_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='角色和部门关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`,`menu_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='角色和菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_task_form`
--

DROP TABLE IF EXISTS `sys_task_form`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_task_form` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `form_id` bigint DEFAULT NULL COMMENT '表单主键',
  `task_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属任务',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='流程任务关联表单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户昵称',
  `user_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'purchase' COMMENT '用户类型（purchase系统用户）',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '手机号码',
  `sex` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '头像地址',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '密码',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `thrid_user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方用户id',
  `thrid_org_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方组织名称',
  `thrid_org_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属第三方组织id',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `login_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1838409436017188867 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='用户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_post`
--

DROP TABLE IF EXISTS `sys_user_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user_post` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`,`post_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='用户与岗位关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='用户和角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_agreement`
--

DROP TABLE IF EXISTS `tb_agreement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_agreement` (
  `id` bigint DEFAULT NULL COMMENT 'id',
  `scheme_id` bigint DEFAULT NULL COMMENT '采购方案id',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商 id',
  `contract_split_id` bigint DEFAULT NULL COMMENT '合约规划拆分id',
  `belong_organization_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属本级组织id',
  `belong_organization_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属本级组织名称',
  `belong_accounting_item` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属最小核算项目',
  `belong_accounting_item_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属最小核算项目编码',
  `agreement_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同名称',
  `agreement_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同编号',
  `total_amount_inc_tax` decimal(24,4) DEFAULT NULL COMMENT '合同签订金额(含税)',
  `total_amount_exc_tax` decimal(24,4) DEFAULT NULL COMMENT '合同签订金额(不含税)',
  `inner_agreement_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '单位内部合同管理编码',
  `party_a_org_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '甲方组织机构 id',
  `party_a_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '甲方名称',
  `party_b_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方名称',
  `expenditure_business_type` int DEFAULT NULL COMMENT '支出业务分类',
  `subject_matter_code` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '交易标的物编码',
  `subject_matter_name` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '交易标的物',
  `payment_cycle` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '支付周期',
  `payment_way` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '支付方式',
  `reporter_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '填报人',
  `party_a_contact_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '甲方联系人名称',
  `party_a_contact_phone` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '甲方联系电话',
  `party_b_legal_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方法人代表',
  `party_b_legal_id_card` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方法人代表身份证',
  `party_b_legal_phone` varchar(22) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方法人代表联系方式',
  `party_b_responsible_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方现场实际履职负责人',
  `party_b_responsible_id_card` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方现场实际履职负责人身份证',
  `party_b_responsible_phone` varchar(22) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方现场实际履职负责人联系方式',
  `agreement_state` int DEFAULT '0' COMMENT '合同状态',
  `agreement_sign_state` int DEFAULT '0' COMMENT '合同签约状态',
  `agreement_sign_date` datetime DEFAULT NULL COMMENT '合同签订日期',
  `agreement_effective_date` datetime DEFAULT NULL COMMENT '合同生效日期',
  `agreement_perform_address` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同履行地',
  `agreement_perform_country` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '国家地区代码(履行地)',
  `agreement_perform_district` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '行政区划代码(履行地)',
  `rental_method` int DEFAULT NULL COMMENT '计租方式',
  `entry_date` datetime DEFAULT NULL COMMENT '进场日期',
  `finish_date` datetime DEFAULT NULL COMMENT '完工日期',
  `duration` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工期',
  `price_form` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '价格形式',
  `discount_ratio` decimal(24,4) DEFAULT NULL COMMENT '下浮比例(%)',
  `scope_of_work` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '工程范围及工作内容',
  `contract_start_date` datetime DEFAULT NULL COMMENT '合同履行开始日期',
  `contract_end_date` datetime DEFAULT NULL COMMENT '合同履行结束日期',
  `attachment_id` bigint DEFAULT NULL COMMENT '合同附件id',
  `label_attachment_id` bigint DEFAULT NULL COMMENT '合同标签附件 id',
  `watermark_attachment_id` bigint DEFAULT NULL COMMENT '合同水印附件 id',
  `convert_pdf_attachment_id` bigint DEFAULT NULL COMMENT 'pdf 附件id',
  `sign_attachment_id` bigint DEFAULT NULL COMMENT '电子签章附件 id',
  `signature_user_id` bigint DEFAULT NULL COMMENT '合同签署人用户 id',
  `cancelled_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '作废原因',
  `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='合同基本信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_agreement_daily_wage`
--

DROP TABLE IF EXISTS `tb_agreement_daily_wage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_agreement_daily_wage` (
  `id` bigint NOT NULL COMMENT '主键 id',
  `agreement_id` bigint NOT NULL COMMENT '合同id',
  `job_title_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工种名称编码',
  `unit_measurement` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量单位',
  `tax_rate` decimal(24,4) DEFAULT NULL COMMENT '税率',
  `unit_price_exc_tax` decimal(24,4) DEFAULT NULL COMMENT '单价(不含税)',
  `unit_price_inc_tax` decimal(24,4) DEFAULT NULL COMMENT '单价(含税)',
  `remark` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='合同-计日工';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_agreement_deposit`
--

DROP TABLE IF EXISTS `tb_agreement_deposit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_agreement_deposit` (
  `id` bigint DEFAULT NULL COMMENT 'id',
  `agreement_id` bigint DEFAULT NULL COMMENT '合同 id',
  `deposit_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '保证金类型',
  `deposit_way` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '保证金方式',
  `deposit_base_amount` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '保证金基数',
  `payment_amount` decimal(24,4) DEFAULT NULL COMMENT '缴纳金额',
  `deposit_ratio` decimal(20,4) DEFAULT NULL COMMENT '约定保证金比例',
  `deposit_amount` decimal(20,4) DEFAULT NULL COMMENT '约定保证金金额',
  `return_condition` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '返还条件',
  `remark` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='合同保证金';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_agreement_equipment_supply`
--

DROP TABLE IF EXISTS `tb_agreement_equipment_supply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_agreement_equipment_supply` (
  `id` bigint DEFAULT NULL COMMENT 'id',
  `agreement_id` bigint DEFAULT NULL COMMENT '合同id',
  `equipment_name_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备名称编码',
  `equipment_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备名称',
  `specification` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格型号',
  `unit_measurement` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量单位',
  `estimated_count` decimal(24,4) DEFAULT NULL COMMENT '预估数量',
  `estimated_tax_rate` decimal(24,4) DEFAULT NULL COMMENT '预估税率',
  `estimated_unit_price_exc_tax` decimal(24,4) DEFAULT NULL COMMENT '预估单价(不含税)',
  `estimated_unit_price_inc_tax` decimal(24,4) DEFAULT NULL COMMENT '预估单价(含税)',
  `estimated_amount_exc_tax` decimal(24,4) DEFAULT NULL COMMENT '预估金额(不含税)',
  `estimated_amount_inc_tax` decimal(24,4) DEFAULT NULL COMMENT '预估金额(含税)',
  `estimated_tax_amount` decimal(24,4) DEFAULT NULL COMMENT '预估税额',
  `remark` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='合同-甲供设备清单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_agreement_machine_shift`
--

DROP TABLE IF EXISTS `tb_agreement_machine_shift`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_agreement_machine_shift` (
  `id` bigint NOT NULL COMMENT '主键 id',
  `agreement_id` bigint NOT NULL COMMENT '合同id',
  `equipment_name_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备名称编码',
  `equipment_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备名称',
  `specification` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格型号',
  `unit_measurement` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量单位',
  `tax_rate` decimal(24,4) DEFAULT NULL COMMENT '税率',
  `unit_price_exc_tax` decimal(24,4) DEFAULT NULL COMMENT '单价(不含税)',
  `unit_price_inc_tax` decimal(24,4) DEFAULT NULL COMMENT '单价(含税)',
  `remark` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='合同-机械台班';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_agreement_material_supply`
--

DROP TABLE IF EXISTS `tb_agreement_material_supply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_agreement_material_supply` (
  `id` bigint DEFAULT NULL COMMENT 'id',
  `agreement_id` bigint DEFAULT NULL COMMENT '合同id',
  `material_name_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物资名称编码',
  `material_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物资名称',
  `specification` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格型号',
  `unit_measurement` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量单位',
  `estimated_count` decimal(24,4) DEFAULT NULL COMMENT '预估数量',
  `estimated_tax_rate` decimal(24,4) DEFAULT NULL COMMENT '预估税率',
  `estimated_unit_price_exc_tax` decimal(24,4) DEFAULT NULL COMMENT '预估单价(不含税)',
  `estimated_unit_price_inc_tax` decimal(24,4) DEFAULT NULL COMMENT '预估单价(含税)',
  `estimated_amount_exc_tax` decimal(24,4) DEFAULT NULL COMMENT '预估金额(不含税)',
  `estimated_amount_inc_tax` decimal(24,4) DEFAULT NULL COMMENT '预估金额(含税)',
  `estimated_tax_amount` decimal(24,4) DEFAULT NULL COMMENT '预估税额',
  `remark` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='合同-甲供材料清单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_agreement_materials_list`
--

DROP TABLE IF EXISTS `tb_agreement_materials_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_agreement_materials_list` (
  `id` bigint NOT NULL COMMENT 'id',
  `agreement_id` bigint DEFAULT NULL COMMENT '合同id',
  `materials_list_id` bigint DEFAULT NULL COMMENT '物料清单 id',
  `bidding_list_quotation_id` bigint DEFAULT NULL COMMENT '投标清单报价 id',
  `contract_split_id` bigint DEFAULT NULL COMMENT '合约拆分 id',
  `materials_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物料编码',
  `materials_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物料名称',
  `brand` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '品牌',
  `payment_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '价款类型',
  `rental_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租赁方式',
  `rental_unit` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计租单位',
  `rental_duration` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租赁时间',
  `workload` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工作量',
  `sign_count` decimal(24,4) DEFAULT NULL COMMENT '签订数量',
  `sign_tax_rate` decimal(24,4) DEFAULT NULL COMMENT '签订税率',
  `sign_unit_price_incl_tax` decimal(24,4) DEFAULT NULL COMMENT '签订单价（含税）',
  `sign_unit_price_excl_tax` decimal(24,4) DEFAULT NULL COMMENT '签订单价（不含税）',
  `sign_amount_incl_tax` decimal(24,4) DEFAULT NULL COMMENT '签订金额（含税）',
  `sign_amount_excl_tax` decimal(24,4) DEFAULT NULL COMMENT '签订金额（不含税）',
  `vendor_tax_rate` decimal(24,4) DEFAULT NULL COMMENT '供应商签订税率',
  `vendor_unit_price_incl_tax` decimal(24,4) DEFAULT NULL COMMENT '供应商招标单价（含税）',
  `vendor_unit_price_excl_tax` decimal(24,4) DEFAULT NULL COMMENT '供应商招标单价（不含税）',
  `vendor_amount_incl_tax` decimal(24,4) DEFAULT NULL COMMENT '供应商招标金额（含税）',
  `vendor_amount_excl_tax` decimal(24,4) DEFAULT NULL COMMENT '供应商招标金额（不含税）',
  `remark` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='合同物料清单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_agreement_payment_item`
--

DROP TABLE IF EXISTS `tb_agreement_payment_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_agreement_payment_item` (
  `id` bigint NOT NULL COMMENT 'id',
  `agreement_id` bigint DEFAULT NULL COMMENT '合同id',
  `currency` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '币种',
  `invoice_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发票类型',
  `contract_tax_rate` decimal(24,4) DEFAULT NULL COMMENT '合同税率',
  `prepayment_ratio` decimal(24,4) DEFAULT NULL COMMENT '约定预付款比例',
  `prepayment_amount` decimal(24,4) DEFAULT NULL COMMENT '约定预付款金额',
  `prepayment_deduction_conditions` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '预付款扣回条件',
  `prepayment_deduction_deadline` decimal(24,4) DEFAULT NULL COMMENT '预付款全部扣回截止点',
  `out_of_settlement_ratio` decimal(24,4) DEFAULT NULL COMMENT '允许合同外结算占合同比例',
  `is_related_my_steel` int DEFAULT NULL COMMENT '是否关联我的钢铁网价格',
  `my_steel_price_fluctuation` decimal(24,4) DEFAULT NULL COMMENT '我的钢铁网价格浮动值',
  `stagnation_ratio` decimal(24,4) DEFAULT NULL COMMENT '停滞台班结算比例 ',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='合同款项信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_agreement_payment_list`
--

DROP TABLE IF EXISTS `tb_agreement_payment_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_agreement_payment_list` (
  `id` bigint NOT NULL COMMENT 'id',
  `agreement_id` bigint DEFAULT NULL COMMENT '合同id',
  `payment_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '付款阶段名称',
  `payment_basis` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '付款基数',
  `payment_ratio` decimal(24,4) DEFAULT NULL COMMENT '约定付款比例(%)',
  `payment_amount` decimal(24,4) DEFAULT NULL COMMENT '约定付款金额',
  `payment_remark` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '付款说明',
  `current_payment_point` int DEFAULT NULL COMMENT '当前付款节点',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='合同结算与付款节点信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_agreement_sign`
--

DROP TABLE IF EXISTS `tb_agreement_sign`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_agreement_sign` (
  `id` bigint NOT NULL COMMENT 'id',
  `business_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务编码',
  `business_id` bigint NOT NULL COMMENT '业务 Id',
  `sign_state` int DEFAULT NULL COMMENT '签署状态',
  `sign_step` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '签署步骤',
  `sign_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '签订信息',
  `tenant_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '签发主体 id',
  `tenant_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '签章发起主体',
  `creator_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '发起人名称',
  `creator_contact` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '发起人联系方式',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='合同签章信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_agreement_sign_qys`
--

DROP TABLE IF EXISTS `tb_agreement_sign_qys`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_agreement_sign_qys` (
  `id` bigint NOT NULL COMMENT 'id',
  `business_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务编码',
  `business_id` bigint NOT NULL COMMENT '业务 Id',
  `sign_id` bigint NOT NULL COMMENT '合同签章id',
  `document_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '合同文档 id',
  `contract_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '电子签约文件id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='合同签章信息-契约锁';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_agreement_sign_qys_request_log`
--

DROP TABLE IF EXISTS `tb_agreement_sign_qys_request_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_agreement_sign_qys_request_log` (
  `id` bigint NOT NULL COMMENT 'id',
  `bizId` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '交易流水号',
  `business_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务编码',
  `business_id` bigint NOT NULL COMMENT '业务 id',
  `applicant_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '申请人名称',
  `applicant_mobile` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '申请人联系方式',
  `request_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '请求接口名称',
  `request_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '请求url',
  `request_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '请求参数',
  `request_time` datetime DEFAULT NULL COMMENT '请求时间',
  `response_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '响应码',
  `response_message` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '响应消息',
  `response_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '响应数据',
  `response_time` datetime DEFAULT NULL COMMENT '响应时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='合同签章-契约锁请求日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_agreement_sign_stamper`
--

DROP TABLE IF EXISTS `tb_agreement_sign_stamper`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_agreement_sign_stamper` (
  `id` bigint NOT NULL COMMENT 'id',
  `type` int NOT NULL COMMENT '类型（1：发起方  2：接收方）',
  `template_id` bigint DEFAULT NULL COMMENT '模板 id',
  `sign_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '签署类型',
  `key_word` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关键词',
  `sign_page` int DEFAULT NULL COMMENT '签署坐标页码',
  `offset_x` decimal(10,4) DEFAULT NULL COMMENT '签署 X 轴坐标',
  `offset_y` decimal(10,4) DEFAULT NULL COMMENT '签署 Y 轴坐标',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='合同签章签署位置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_agreement_signature`
--

DROP TABLE IF EXISTS `tb_agreement_signature`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_agreement_signature` (
  `id` bigint NOT NULL COMMENT 'id',
  `sign_id` bigint NOT NULL COMMENT '合同签章id',
  `signature_type` int NOT NULL COMMENT '签署方类型',
  `signature_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '签署方 id',
  `signature_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '签署方名称',
  `signature_contact_id` bigint NOT NULL COMMENT '签署方联系人 id',
  `signature_contact_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '签署方联系人名称',
  `signature_contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '签署方联系人联系方式',
  `sign_state` int DEFAULT NULL COMMENT '签署状态',
  `sign_callback_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '签约回调信息',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='合同签订方签章信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_area_division`
--

DROP TABLE IF EXISTS `tb_area_division`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_area_division` (
  `id` bigint NOT NULL COMMENT '主键id',
  `area_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '区划名称',
  `area_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '行政区划编码',
  `parent_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父级行政区划编码',
  `parent_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父级区划名称',
  `area_level` int DEFAULT NULL COMMENT '层级',
  `level_alias` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '层级名称',
  `order_num` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '显示排序',
  `origin` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '来源',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='行政区划表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_attachment`
--

DROP TABLE IF EXISTS `tb_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_attachment` (
  `id` bigint DEFAULT NULL COMMENT '主键 id',
  `business_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务类型',
  `business_id` bigint DEFAULT NULL COMMENT '业务id',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文件路径',
  `file_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文件名',
  `doc_meta` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '文件元数据',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='附件';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_bidding_abandon_record`
--

DROP TABLE IF EXISTS `tb_bidding_abandon_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_bidding_abandon_record` (
  `id` bigint NOT NULL COMMENT '主键id',
  `project_id` bigint DEFAULT NULL COMMENT '招标项目id',
  `project_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '招标项目名称',
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '废标原因',
  `attach_id` bigint DEFAULT NULL COMMENT '废标附件id',
  `notice_id` bigint DEFAULT NULL COMMENT '招标公告id',
  `scheme_id` bigint DEFAULT NULL COMMENT '采购方案id',
  `bidding_info_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '投标单id集合',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='投标单废标记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_bidding_evaluat_expert`
--

DROP TABLE IF EXISTS `tb_bidding_evaluat_expert`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_bidding_evaluat_expert` (
  `id` bigint NOT NULL COMMENT '主键id',
  `notice_id` bigint DEFAULT NULL COMMENT '招标公告id',
  `scheme_id` bigint DEFAULT NULL COMMENT '采购方案id',
  `expert_id` bigint DEFAULT NULL COMMENT '专家人员id',
  `expert_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '专家人员姓名',
  `deadline` datetime DEFAULT NULL COMMENT '允许评标截止时间（保留）',
  `eval_status` int DEFAULT '0' COMMENT '专家是否完成评标（0未评标 1已评标）',
  `is_eval` int DEFAULT '0' COMMENT '专家是否需要评标（0是 1否）',
  `expert_type` int DEFAULT NULL COMMENT '专家类别（1技术类 2经济类）',
  `is_join` int DEFAULT NULL COMMENT '是否参加评标（1是 2否）',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='评标专家人员信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_bidding_info`
--

DROP TABLE IF EXISTS `tb_bidding_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_bidding_info` (
  `id` bigint NOT NULL COMMENT '主键id',
  `parent_id` bigint DEFAULT NULL COMMENT '父主键id',
  `project_id` bigint DEFAULT NULL COMMENT '招标项目id',
  `scheme_id` bigint DEFAULT NULL COMMENT '采购方案id',
  `notice_id` bigint DEFAULT NULL COMMENT '招标公告id',
  `tax_price` decimal(17,5) DEFAULT NULL COMMENT '含税总价',
  `not_tax_price` decimal(17,5) DEFAULT NULL COMMENT '不含税总价',
  `contact` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系人',
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系电话',
  `submit_status` int DEFAULT NULL COMMENT '提交状态（0草稿 1已提交）',
  `bidding_status` int DEFAULT NULL COMMENT '投标状态（0未投标/ 1已投标（对供应商端）|已回标（对采购端）/ 2已撤回 / 3已废标）',
  `ip_address` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作人ip',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商id',
  `vendor_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应商名称',
  `collect_deposit` int DEFAULT '2' COMMENT '是否收取保证金（为2则不需收取 0未收取 1已收取）',
  `twice_quot` int DEFAULT '0' COMMENT '二次报价设置（默认0关 1开）',
  `twice_time` datetime DEFAULT NULL COMMENT '二次报价截止时间',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `twice_quot_version` int DEFAULT NULL COMMENT '二次报价版本号 从1开始',
  `price_change_state` int DEFAULT NULL COMMENT '供应商调价状态 0未调价 1已调价 2放弃调价',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='投标单信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_bidding_item_grade`
--

DROP TABLE IF EXISTS `tb_bidding_item_grade`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_bidding_item_grade` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `score` decimal(10,2) DEFAULT NULL COMMENT '分数',
  `item_id` bigint DEFAULT NULL COMMENT '评分项id',
  `expert_id` bigint DEFAULT NULL COMMENT '专家用户id',
  `vendor_id` bigint DEFAULT NULL COMMENT '评标的供应商id',
  `schema_id` bigint DEFAULT NULL COMMENT '采购方案id',
  `expert_score_id` bigint DEFAULT NULL COMMENT '专家评分表id',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1843152076399165443 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='评分项分数表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_bidding_list_quotation`
--

DROP TABLE IF EXISTS `tb_bidding_list_quotation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_bidding_list_quotation` (
  `id` bigint NOT NULL COMMENT '主键id',
  `materials_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '清单编码',
  `bidding_info_id` bigint DEFAULT NULL COMMENT '投标单信息id',
  `tax_unit_price` decimal(17,5) DEFAULT NULL COMMENT '含税单价(元)',
  `not_tax_unit_price` decimal(17,5) DEFAULT NULL COMMENT '不含税单价(元)',
  `tax_price` decimal(17,5) DEFAULT NULL COMMENT '含税总价(元)',
  `not_tax_price` decimal(17,5) DEFAULT NULL COMMENT '不含税总价(元)',
  `tax_rate` decimal(10,2) DEFAULT NULL COMMENT '税率',
  `bill_type` int DEFAULT NULL COMMENT '发票类型（1增值税专用发票/2增值税普通发票）',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `floating_price` decimal(17,5) DEFAULT NULL COMMENT '浮动价',
  `unloading_fee` decimal(17,5) DEFAULT NULL COMMENT '卸费',
  `base_price` decimal(17,5) DEFAULT NULL COMMENT '基价',
  `split_id` bigint DEFAULT NULL COMMENT '拆分id',
  `materials_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物料id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='投标清单报价';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_bidding_mark_category`
--

DROP TABLE IF EXISTS `tb_bidding_mark_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_bidding_mark_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `item_type` int DEFAULT NULL COMMENT '评分模板项类型',
  `total_score` int DEFAULT NULL COMMENT '项总分',
  `template_id` bigint DEFAULT NULL COMMENT '模板id',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1838487512094646274 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='评分模板项';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_bidding_mark_item`
--

DROP TABLE IF EXISTS `tb_bidding_mark_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_bidding_mark_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '评分项名称',
  `low_range` int DEFAULT NULL COMMENT '分值范围-低',
  `high_range` int DEFAULT NULL COMMENT '分值范围-高',
  `category_id` bigint DEFAULT NULL COMMENT '模板项id',
  `parent_id` bigint DEFAULT NULL COMMENT '父项id',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1838487512136589315 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='模板评分项';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_bidding_mark_template`
--

DROP TABLE IF EXISTS `tb_bidding_mark_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_bidding_mark_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '评分模板名称',
  `state` int DEFAULT '0' COMMENT '启用状态（默认0未启用 1启用）',
  `create_user` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '维护人',
  `use_unit` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '使用单位',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `use_unit_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1835672055492288514 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='评分模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_bidding_open_people`
--

DROP TABLE IF EXISTS `tb_bidding_open_people`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_bidding_open_people` (
  `id` bigint NOT NULL COMMENT '主键id',
  `notice_id` bigint DEFAULT NULL COMMENT '招标公告id',
  `scheme_id` bigint DEFAULT NULL COMMENT '采购方案id',
  `user_id` bigint DEFAULT NULL COMMENT '人员id',
  `user_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '人员姓名',
  `is_open` int DEFAULT '0' COMMENT '是否开标（0未开标 1已开标）',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `redirect_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '第三方待办跳转地址',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='开标人员信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_bidding_result`
--

DROP TABLE IF EXISTS `tb_bidding_result`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_bidding_result` (
  `id` bigint NOT NULL COMMENT '主键id',
  `scheme_id` bigint DEFAULT NULL COMMENT '采购方案id',
  `notice_id` bigint DEFAULT NULL COMMENT '招标公告id',
  `bidding_info_id` bigint DEFAULT NULL COMMENT '投标单信息id',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商id',
  `vendor_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应商名称',
  `candidate` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中标候选人（第一、第二、第三）',
  `total_score` decimal(10,2) DEFAULT NULL COMMENT '综合分',
  `rank` int DEFAULT NULL COMMENT '综合排名',
  `publicity_start_time` datetime DEFAULT NULL COMMENT '公示期时间-起',
  `publicity_end_time` datetime DEFAULT NULL COMMENT '公示期时间-止',
  `bid_result` int DEFAULT NULL COMMENT '中标结果（0未中标 1已中标）',
  `send_notified` int DEFAULT NULL COMMENT '是否发送通知书（0未发送 1已发送）',
  `notifi_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '中标通知书内容',
  `notifi_time` datetime DEFAULT NULL COMMENT '中标通知发布时间',
  `contact` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系人',
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系电话',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `sure_bid` int DEFAULT NULL COMMENT '确定中标（1：确定中标人 其它：不确定中标人）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='投标结果信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_bidding_schema_template`
--

DROP TABLE IF EXISTS `tb_bidding_schema_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_bidding_schema_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `scheme_id` bigint DEFAULT NULL COMMENT '采购方案id',
  `template_id` bigint DEFAULT NULL COMMENT '评分模板id',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='采购方案评分模板关联关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_business_code`
--

DROP TABLE IF EXISTS `tb_business_code`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_business_code` (
  `id` bigint NOT NULL COMMENT 'id',
  `business_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务编码',
  `business_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务名称',
  `now_date_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '当前日期类型',
  `now_date` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '当前日期',
  `prefix` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '前缀',
  `code_number` bigint NOT NULL COMMENT '编码数值',
  `code_number_length` int DEFAULT NULL COMMENT '编码长度',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `tb_index_business_code` (`business_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='系统业务编号规则';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_contract_planning`
--

DROP TABLE IF EXISTS `tb_contract_planning`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_contract_planning` (
  `id` bigint NOT NULL COMMENT '主键 id',
  `plan_id` bigint NOT NULL COMMENT '采购计划 id',
  `bid_responsible_org` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '招标责任单位',
  `bid_responsible_org_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '招标责任单位名称',
  `contract_planning_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合约规划id',
  `contract_planning_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合约规划编码',
  `contract_planning_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合约规划名称',
  `project_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目 id',
  `project_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目编号',
  `project_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目名称',
  `contract_planning_category` int DEFAULT NULL COMMENT '合约规划类别',
  `contract_planning_category_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目合约类别名称',
  `planned_amount_incl_tax` decimal(24,4) DEFAULT NULL COMMENT '规划金额（含税）',
  `incurred_planned_amount` decimal(24,4) DEFAULT NULL COMMENT '已发生规划金额',
  `planning_balance` decimal(24,4) DEFAULT NULL COMMENT '规划余量',
  `bidding_method_code` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '拟定招标方式编码',
  `bidding_method_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '拟定招标方式名称',
  `subject_matter` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '交易标的物',
  `brand` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='合约规划表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_contract_planning_push_record`
--

DROP TABLE IF EXISTS `tb_contract_planning_push_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_contract_planning_push_record` (
  `id` bigint NOT NULL COMMENT '主键 id',
  `contract_planning_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合约规划id',
  `contract_planning_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目合约编码',
  `push_status` int DEFAULT NULL COMMENT '推送状态（0未推送 1已推送）',
  `push_obj` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '推送对象',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='第三方数据合约规划推送记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_contract_planning_split`
--

DROP TABLE IF EXISTS `tb_contract_planning_split`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_contract_planning_split` (
  `id` bigint NOT NULL COMMENT '主键 id',
  `procurement_plan_id` bigint DEFAULT NULL COMMENT '采购计划 id',
  `split_contract_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '拆分合约规划名称',
  `contract_planning_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合约规划编码',
  `contract_planning_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合约规划名称',
  `contract_scope` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '拟签约合同拆包范围',
  `total_plan_amount` decimal(24,4) DEFAULT NULL COMMENT '计划金额',
  `total_used_amount` decimal(24,4) DEFAULT NULL COMMENT '使用金额',
  `is_use_up` int DEFAULT NULL COMMENT '是否用尽',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='合约规划拆分表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_country`
--

DROP TABLE IF EXISTS `tb_country`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_country` (
  `id` bigint NOT NULL,
  `arabic_numeral_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '阿拉伯数字代码',
  `chinese_as_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '中文简称',
  `chinese_full_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '中文全称',
  `english_as_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '英文简称',
  `english_full_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '英文全称',
  `thrid_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '第三方id',
  `three_char_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '三字符拉丁字母代码',
  `two_char_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '两字符拉丁字母代码',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_expert`
--

DROP TABLE IF EXISTS `tb_expert`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_expert` (
  `id` bigint DEFAULT NULL COMMENT 'id',
  `expert_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '专家姓名',
  `expert_phone` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '专家手机号码',
  `belong_organization` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属组织机构',
  `department` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工作部门',
  `education_degree` int DEFAULT NULL COMMENT '学历',
  `major` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '专业',
  `business_type` int DEFAULT NULL COMMENT '业态',
  `expert_type` int DEFAULT NULL COMMENT '专家类别（1技术类 2经济类）',
  `expert_state` int DEFAULT '0' COMMENT '专家状态(0账号审批中|1启用|2禁用)',
  `user_id` bigint DEFAULT NULL COMMENT '对应用户id',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `registered_certificate` int DEFAULT NULL COMMENT '执业资格证',
  `registered_certificate_date` datetime DEFAULT NULL COMMENT '执业资格证取得时间',
  `present_job` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '现从事专业工作',
  `present_job_date` datetime DEFAULT NULL COMMENT '本专业参加工作时间',
  `technical_titles` int DEFAULT NULL COMMENT '技术职称（初级、中级、副高、正高、教授级高工、研究员）',
  `technical_titles_date` datetime DEFAULT NULL COMMENT '技术职称获取时间',
  `present_unit_dept` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '现工作单位或者部门',
  `email` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
  `profession_resume` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '相关专业工作简历'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='专家';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_expert_score`
--

DROP TABLE IF EXISTS `tb_expert_score`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_expert_score` (
  `id` bigint NOT NULL COMMENT '主键id',
  `scheme_id` bigint DEFAULT NULL COMMENT '采购方案id',
  `notice_id` bigint DEFAULT NULL COMMENT '招标公告id',
  `bidding_info_id` bigint DEFAULT NULL COMMENT '投标单信息id',
  `vendor_id` bigint DEFAULT NULL COMMENT '投标供应商id',
  `expert_id` bigint DEFAULT NULL COMMENT '专家id',
  `bus_score` decimal(10,2) DEFAULT NULL COMMENT '商务评分',
  `tech_score` decimal(10,2) DEFAULT NULL COMMENT '技术评分',
  `eva_time` datetime DEFAULT NULL COMMENT '评标时间',
  `eva_opinion` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '评标意见',
  `eval_status` int DEFAULT NULL COMMENT '评标状态（0未评标 1已评标）',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='专家评分表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_expert_score_detail`
--

DROP TABLE IF EXISTS `tb_expert_score_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_expert_score_detail` (
  `id` bigint NOT NULL COMMENT '主键id',
  `expert_score_id` bigint DEFAULT NULL COMMENT '专家评分主表id',
  `type` int DEFAULT NULL COMMENT '评分评分类型（1商务评分 2技术评分 ）',
  `number` int DEFAULT NULL COMMENT '序号',
  `items` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '评分项',
  `range` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分值范围',
  `score` decimal(10,2) DEFAULT NULL COMMENT '得分',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='专家评分明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_file_z_task`
--

DROP TABLE IF EXISTS `tb_file_z_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_file_z_task` (
  `id` bigint DEFAULT NULL COMMENT 'id',
  `attachment_id` bigint DEFAULT NULL COMMENT '附件id',
  `business_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务编码',
  `business_id` bigint DEFAULT NULL COMMENT '业务 id',
  `task_state` int DEFAULT '0' COMMENT '任务状态 0:执行中 1:执行成功  2:执行失败',
  `request_time` datetime DEFAULT NULL COMMENT '请求时间',
  `request_type_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求类型编码',
  `request_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求类型',
  `request_url` varchar(520) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求路径',
  `request_body` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '请求参数',
  `task_id` varchar(320) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '任务 id',
  `response_time` datetime DEFAULT NULL COMMENT '响应时间',
  `response_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '响应状态',
  `response_body` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '响应体',
  `call_back_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '回调状态',
  `call_back_time` datetime DEFAULT NULL COMMENT '请求回调时间',
  `call_back_body` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '回调数据',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  UNIQUE KEY `index_uk_attachment_id` (`attachment_id`) USING BTREE COMMENT '附件唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='FileZ文档任务';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_index_statistic`
--

DROP TABLE IF EXISTS `tb_index_statistic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_index_statistic` (
  `id` bigint DEFAULT NULL COMMENT 'id',
  `total_procurement_amout` decimal(20,2) DEFAULT NULL COMMENT '累计采购金额',
  `total_vendor` bigint DEFAULT NULL COMMENT '合作供应商数',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `total_project` int DEFAULT NULL COMMENT '上线项目数',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='首页统计数据';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_materials_list`
--

DROP TABLE IF EXISTS `tb_materials_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_materials_list` (
  `id` bigint NOT NULL COMMENT 'id',
  `plan_id` bigint DEFAULT NULL COMMENT '采购计划id',
  `contract_split_id` bigint DEFAULT NULL COMMENT '合约采购拆分id',
  `materials_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物料 id',
  `materials_unique_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物料清单唯一 id',
  `materials_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物料清单编码',
  `materials_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物料清单名称',
  `subject_matter_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '交易标的物编码',
  `subject_matter_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '交易标的物名称',
  `subject_matter_flag` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '交易标的物标识',
  `specification` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格型号',
  `measurement_rules` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量规则',
  `unit_measurement` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量单位',
  `work_content` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '基本工作内容',
  `cost_account_Id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本科目档案 id',
  `cost_account_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本科目编码',
  `cost_account_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本科目名称',
  `count` decimal(20,5) DEFAULT NULL COMMENT '数量',
  `used_count` decimal(20,5) DEFAULT NULL COMMENT '已使用数量',
  `tax_rate` decimal(24,5) DEFAULT NULL COMMENT '税率',
  `unit_price_excl_tax` decimal(24,5) DEFAULT NULL COMMENT '单价(不含税)',
  `unit_price_incl_tax` decimal(24,5) DEFAULT NULL COMMENT '单价(含税) ',
  `amount_excl_tax` decimal(24,5) DEFAULT NULL COMMENT '金额(不含税)',
  `amount_incl_tax` decimal(24,5) DEFAULT NULL COMMENT '金额(含税)',
  `tax_amount` decimal(24,5) DEFAULT NULL COMMENT '税额',
  `floating_price` decimal(20,5) DEFAULT NULL COMMENT '浮动价',
  `unloading_fee` decimal(24,5) DEFAULT NULL COMMENT '卸费',
  `landing_charges` decimal(20,5) DEFAULT NULL COMMENT '卸费',
  `base_price` decimal(20,5) DEFAULT NULL COMMENT '基价',
  `rent_mode` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租赁方式',
  `rent_time` decimal(24,5) DEFAULT NULL COMMENT '租赁时间',
  `rent_quantity` decimal(24,5) DEFAULT NULL COMMENT '租赁数量',
  `remark` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='采购物料清单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_materials_list_old`
--

DROP TABLE IF EXISTS `tb_materials_list_old`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_materials_list_old` (
  `id` bigint NOT NULL COMMENT 'id',
  `plan_id` bigint DEFAULT NULL COMMENT '采购计划id',
  `contract_split_id` bigint DEFAULT NULL COMMENT '合约采购拆分id',
  `materials_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物料编码',
  `materials_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物料名称',
  `specification` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格型号',
  `feature_description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '特征描述',
  `measurement_rules` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量规则',
  `unit_measurement` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量单位',
  `work_content` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '基本工作内容',
  `count` decimal(20,2) DEFAULT NULL COMMENT '数量',
  `price_including_tax` decimal(20,4) DEFAULT NULL COMMENT '单价(含税)',
  `unit_price_excl_tax` decimal(20,4) DEFAULT NULL COMMENT '单价(不含税)',
  `unit_price_incl_tax` decimal(20,4) DEFAULT NULL COMMENT '单价(含税) ',
  `amount_excl_tax` decimal(20,4) DEFAULT NULL COMMENT '金额(不含税)',
  `amount_incl_tax` decimal(20,4) DEFAULT NULL COMMENT '金额(含税)',
  `tax_amount` decimal(20,4) DEFAULT NULL COMMENT '税额',
  `remark` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='采购物料清单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_message`
--

DROP TABLE IF EXISTS `tb_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_message` (
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键id',
  `message_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息标题',
  `message_type` int NOT NULL COMMENT '消息类型',
  `message_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息内容',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='消息中心';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_min_project`
--

DROP TABLE IF EXISTS `tb_min_project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_min_project` (
  `id` bigint DEFAULT NULL COMMENT 'id',
  `parent_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父项目编码',
  `parent_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父项目全称',
  `parent_simple_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父项目简称',
  `project_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目编码',
  `min_account_code` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最小核算项目编号',
  `min_account_full_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目简称',
  `project_department` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属项目部',
  `prj_state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目业态',
  `prg_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工程类型',
  `money_sec` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目资金来源',
  `prj_manage_model` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目管理模式',
  `contracting_model` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '承包模式',
  `busi_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务分类',
  `zi_zhi` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '资质',
  `qualified_unit` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '资质所属单位',
  `duty_unit` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '责任单位',
  `constrution_unit` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'construtionUnit',
  `project_leader` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目负责人',
  `project_leader_phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目负责人电话',
  `technical_director` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '技术负责人',
  `technical_director_phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '技术负责人电话',
  `zb_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '承揽方式（招标方式）',
  `control` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '招标控制价',
  `bqcb_rate` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标前成本测算利润率',
  `bidding_price` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中标价（含税）',
  `provisional_sum` decimal(24,4) DEFAULT NULL COMMENT '其中暂列金额（含税）',
  `winning_time` date DEFAULT NULL COMMENT '中标时间',
  `cost_reduction_rate` decimal(24,4) DEFAULT NULL COMMENT '全费用下浮率',
  `lx_date` date DEFAULT NULL COMMENT '立项时间',
  `all_area` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '总建筑面积(㎡)',
  `down_area` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '地下总建筑面积(㎡)',
  `up_area` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '地上总建筑面积(㎡)',
  `total_full` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最高层数',
  `top_eaves` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最高建筑高度（檐口）(m)',
  `jg_type` varchar(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结构类型',
  `base_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '基础形式',
  `country` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '国家地区',
  `prj_addr` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '行政区划',
  `prj_addr_province` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '行政区划-省',
  `prj_addr_city` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '行政区划-市',
  `prj_addr_region` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '行政区划-区',
  `prj_addr_info` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '详细地址',
  `actual_begin_date` date DEFAULT NULL COMMENT '实际开工日期',
  `actual_end_date` date DEFAULT NULL COMMENT '实际竣工日期',
  `actual_construction_period` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '实际工期',
  `taxpayer` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'taxpayer',
  `taxpayer_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '纳税识别号',
  `tax` varchar(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计税方式',
  `management_org_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属管理组织',
  `belonging_org_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属本级组织',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
  `state` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目状态'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='最小核算项目信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_procurement_plan`
--

DROP TABLE IF EXISTS `tb_procurement_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_procurement_plan` (
  `id` bigint NOT NULL COMMENT '主键 id',
  `procurement_plan_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '采购计划编号',
  `procurement_plan_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '采购计划名称',
  `procurement_plan_type` int DEFAULT NULL COMMENT '采购计划分类',
  `procurement_type` int DEFAULT NULL COMMENT '采购方式',
  `project_hierarchy` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目采购层级',
  `begin_date` datetime DEFAULT NULL COMMENT '计划开始时间',
  `end_date` datetime DEFAULT NULL COMMENT '计划完成时间',
  `arrival_date` datetime DEFAULT NULL COMMENT '计划进场时间',
  `procurement_reporter` bigint DEFAULT NULL COMMENT '采购填报人',
  `procurement_reporter_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '采购填报人名称',
  `procurement_officer` bigint NOT NULL COMMENT '采购经办人',
  `procurement_officer_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '采购经办人名称',
  `subject_matter` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '交易标的物',
  `subject_matter_name` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '交易标的物名称',
  `subject_matter_type` int DEFAULT NULL COMMENT '交易标的物类型',
  `price_type` int DEFAULT NULL COMMENT '价格类型',
  `region_province_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '区域-省',
  `region_city_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '区域-市',
  `counting_type` int DEFAULT NULL COMMENT '计数方式',
  `payment_type` int DEFAULT NULL COMMENT '付款方式',
  `guidance_price` decimal(24,5) DEFAULT NULL COMMENT '指导价',
  `state` int NOT NULL COMMENT '状态',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='采购计划';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_procurement_scheme`
--

DROP TABLE IF EXISTS `tb_procurement_scheme`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_procurement_scheme` (
  `id` bigint NOT NULL COMMENT 'id',
  `project_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目编号',
  `procurement_scheme_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '采购方案编号',
  `procurement_scheme_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '采购方案名称',
  `procurement_officer` bigint DEFAULT NULL COMMENT '采购经办人',
  `procurement_officer_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '采购经办人名称',
  `procurement_plan_type` int DEFAULT NULL COMMENT '采购计划类别',
  `procurement_type` int DEFAULT NULL COMMENT '采购方式',
  `ceiling_price` decimal(20,2) DEFAULT NULL COMMENT '上限价',
  `is_receive_deposit` int DEFAULT NULL COMMENT '是否收取保证金',
  `security_deposit` decimal(20,2) DEFAULT NULL COMMENT '保证金',
  `finance_confirm_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '财务确认人员id',
  `finance_confirm_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '财务确认人员名称',
  `counting_type` int DEFAULT NULL COMMENT '计数方式 ',
  `payment_type` int DEFAULT NULL COMMENT '付款方式',
  `price_type` int DEFAULT NULL COMMENT '价格类型',
  `subject_matter_code` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '交易标的物编码',
  `subject_matter_name` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '交易标的物名称',
  `subject_matter_type` int DEFAULT NULL COMMENT '交易标的物类型',
  `state` int DEFAULT NULL COMMENT '状态',
  `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `tb_procurement_scheme_project_code_IDX` (`project_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='采购方案';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_procurement_scheme_bidding`
--

DROP TABLE IF EXISTS `tb_procurement_scheme_bidding`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_procurement_scheme_bidding` (
  `id` bigint NOT NULL COMMENT '主键id',
  `scheme_id` bigint NOT NULL COMMENT '采购方案id',
  `bid_deadline` datetime DEFAULT NULL COMMENT '投标截止时间',
  `bid_contact_person` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '投标联系人',
  `bid_contact_phone` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '投标联系电话',
  `bid_contact_email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '投标联系邮箱',
  `evaluation_template_id` bigint DEFAULT NULL COMMENT '评分模板id',
  `bidding_attachment_id` bigint DEFAULT NULL COMMENT '招标文件附件 id',
  `bidding_template_id` bigint DEFAULT NULL COMMENT '招标文件模板id',
  `contract_template_id` bigint DEFAULT NULL COMMENT '合同模板id',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='采购方案-招标信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_procurement_scheme_plan_relate`
--

DROP TABLE IF EXISTS `tb_procurement_scheme_plan_relate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_procurement_scheme_plan_relate` (
  `id` bigint NOT NULL COMMENT '主键 id',
  `procurement_scheme_id` bigint NOT NULL COMMENT '采购方案id',
  `procurement_plan_id` bigint NOT NULL COMMENT '采购计划id',
  `contract_split_id` bigint DEFAULT NULL COMMENT '拆分合约 id',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='采购方案-采购计划对应关系';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_project`
--

DROP TABLE IF EXISTS `tb_project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_project` (
  `id` bigint NOT NULL COMMENT 'id',
  `third_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '采购订单id',
  `abnormal_status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '异常状态 字典key=ABNORMAL_STATUS',
  `actual_end_time` datetime DEFAULT NULL COMMENT '实际竣工日期',
  `actual_start_time` datetime DEFAULT NULL COMMENT '实际开工日期',
  `actual_work` decimal(24,5) DEFAULT NULL COMMENT '实际工期',
  `basic_form` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '基础形式 字典key=BASIC_FORM',
  `city_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '行政区划代码（市）',
  `city_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '行政区划代码（市）',
  `construction_high` decimal(24,5) DEFAULT NULL COMMENT '最高建筑高度檐口(m)',
  `construction_unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '建设单位',
  `construction_unit_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '建设单位Id',
  `contract_mode` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '承包模式 字典key=CONTRACT_MODE',
  `contract_total_amount` decimal(24,5) DEFAULT NULL COMMENT '合同金额',
  `country_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '国家和地区代码',
  `country_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '国家和地区代码',
  `county_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '行政区划代码（县）',
  `county_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '行政区划代码（县）',
  `design_unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设计单位',
  `design_unit_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设计单位Id',
  `down_total_construction_area` decimal(24,5) DEFAULT NULL COMMENT '地下总建筑面积(㎡)',
  `finance_leader_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '财务负责人id',
  `finance_leader_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '财务负责人名称',
  `finance_org_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属财务组织',
  `floor_number` decimal(24,5) DEFAULT NULL COMMENT '最高层数',
  `full_drop_rate` decimal(24,5) DEFAULT NULL COMMENT '全费用下浮率',
  `manage_org_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属管理组织id',
  `manage_org_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属管理组织名称',
  `org_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属本级组织id',
  `org_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属本级组织名称',
  `parent_project_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父项目编码',
  `parent_project_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父项目名称',
  `plan_start_time` datetime DEFAULT NULL COMMENT '计划开工日期',
  `plan_work` decimal(24,5) DEFAULT NULL COMMENT '计划工期（天）',
  `project_approval_time` datetime DEFAULT NULL COMMENT '立项时间',
  `project_as_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最小核算项目简称',
  `project_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最小核算项目编码',
  `project_department_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属项目部id',
  `project_department_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属项目部名称',
  `project_format` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目业态',
  `project_funds_source` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目资金来源 字典key=PROJECT_FUNDS_SOURCE',
  `project_leader_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目负责人id',
  `project_leader_mobile` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目负责人电话',
  `project_leader_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目负责人名称',
  `project_manage_model` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目管理模式 字段key=PROJECT_MANAGE_MODEL',
  `project_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最小核算项目全称',
  `project_site` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目地点',
  `project_status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目状态 字典key=PROJECT_STATUS',
  `project_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工程类型 字典key=PROJECT_TYPE',
  `province_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '行政区划代码（省）',
  `province_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '行政区划代码（省）',
  `provisional_amount` decimal(24,5) DEFAULT NULL COMMENT '其中暂列金额（含税）',
  `qualifications_unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '资质所属单位',
  `qualifications_unit_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '资质所属单位Id',
  `responsible_unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '责任单位',
  `responsible_unit_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '责任单位Id',
  `social_credit_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '纳税识别号',
  `structure_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结构类型 字典key=STRUCTURE_TYPE',
  `tax_mode` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计税方式 字典key=TAX_MODE',
  `tax_payers` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '纳税主体',
  `technology_leader_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '技术负责人id',
  `technology_leader_mobile` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '技术负责人电话',
  `technology_leader_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '技术负责人名称',
  `total_construction_area` decimal(24,5) DEFAULT NULL COMMENT '总建筑面积(㎡)',
  `up_total_construction_area` decimal(24,5) DEFAULT NULL COMMENT '地上总建筑面积(㎡)',
  `winning_bid_amount` decimal(24,5) DEFAULT NULL COMMENT '中标价（含税）',
  `winning_bid_time` datetime DEFAULT NULL COMMENT '中标时间',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='项目表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_project_work`
--

DROP TABLE IF EXISTS `tb_project_work`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_project_work` (
  `id` bigint NOT NULL COMMENT 'id',
  `third_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '采购订单id',
  `parnt_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父级id',
  `work_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工程承包范围类型',
  `work_type_value` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工程承包范围类型选项值',
  `create_dept` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='项目明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_receipt_contract_settle_datall`
--

DROP TABLE IF EXISTS `tb_receipt_contract_settle_datall`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_receipt_contract_settle_datall` (
  `id` bigint NOT NULL COMMENT 'id',
  `third_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方id',
  `cost_collection_mode` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本归集方式（1-工程施工合同履约成本 2-研发开支-资本化 3-研发开支-费用化）',
  `current_ntax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '本期结算金额(不含税)',
  `current_settle_quantity` decimal(11,0) DEFAULT NULL COMMENT '本期结算工程量',
  `current_tax` decimal(24,5) DEFAULT NULL COMMENT '本期结算税额',
  `current_tax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '本期结算金额(含税)',
  `current_total_ntax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '本期末累计结算金额(不含税)',
  `current_total_settle_quantity` decimal(24,1) DEFAULT NULL COMMENT '本期末累计结算工程量',
  `current_total_tax` decimal(24,5) DEFAULT NULL COMMENT '本期末累计结算税额',
  `current_total_tax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '本期末累计结算金额(含税)',
  `dataller_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '结算明细表关联id(合同计日工明细表id)',
  `dataller_unique_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '明细表唯一id',
  `measure_unit_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '单位id',
  `measure_unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '单位',
  `ntax_price` decimal(24,5) DEFAULT NULL COMMENT '单价（不含税）',
  `pre_total_ntax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '上期末结算金额(不含税)',
  `pre_total_settle_quantity` decimal(11,1) DEFAULT NULL COMMENT '上期末结算工程量',
  `pre_total_tax` decimal(24,5) DEFAULT NULL COMMENT '上期末结算税额',
  `pre_total_tax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '上期末结算金额(含税)',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `research_project` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '研发项目(成本归集方式非“工程施工合同履约成本” 选择)',
  `settle_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结算单ID,是contract_settlement的id',
  `subject_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本科目编码',
  `subject_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本科目档案ID',
  `subject_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本科目名称',
  `tax_price` decimal(24,5) DEFAULT NULL COMMENT '单价（含税）',
  `tax_rate` decimal(24,5) DEFAULT NULL COMMENT '税率(%)',
  `work_type` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工种名称（1-架子工 2 -钢筋工）',
  `parnt_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父级id',
  `create_dept` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='结算单计日工明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_receipt_contract_settle_deposit`
--

DROP TABLE IF EXISTS `tb_receipt_contract_settle_deposit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_receipt_contract_settle_deposit` (
  `id` bigint NOT NULL COMMENT 'id',
  `third_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方id',
  `base_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '押金、保证金基数（1-签订金额 2-结算金额 3-一次性缴纳）',
  `current_deposit_amount` decimal(24,5) DEFAULT NULL COMMENT '本期约定押金/保证金金额',
  `deposit_amount` decimal(24,5) DEFAULT NULL COMMENT '约定押金/保证金金额',
  `deposit_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结算明细表关联id',
  `deposit_mode` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '押金、保证金方式（1-现金 2-保函 3-担保）',
  `deposit_ratio` decimal(24,5) DEFAULT NULL COMMENT '约定押金/保证金比例(%)',
  `deposit_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '押金、保证金类型（1-履约保证金 2-质量保证金）',
  `deposit_unique_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '明细表唯一id',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `settle_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结算单ID,是contract_settlement的id',
  `parnt_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父级id',
  `create_dept` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='结算单押金、保证金明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_receipt_contract_settle_evaluate`
--

DROP TABLE IF EXISTS `tb_receipt_contract_settle_evaluate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_receipt_contract_settle_evaluate` (
  `id` bigint NOT NULL COMMENT 'id',
  `third_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方id',
  `bad_num` int DEFAULT NULL COMMENT '差',
  `excellent_num` int DEFAULT NULL COMMENT '优',
  `good_num` int DEFAULT NULL COMMENT '良',
  `qualified_num` int DEFAULT NULL COMMENT '合格',
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '评价维度',
  `parnt_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父级id',
  `create_dept` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='结算单供应商评价明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_receipt_contract_settle_list`
--

DROP TABLE IF EXISTS `tb_receipt_contract_settle_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_receipt_contract_settle_list` (
  `id` bigint NOT NULL COMMENT 'id',
  `third_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方id',
  `basic_job` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工作内容',
  `cost_collection_mode` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本归集方式（1-工程施工合同履约成本 2-研发开支-资本化 3-研发开支-费用化）',
  `current_ntax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '本期结算金额(不含税)',
  `current_settle_quantity` decimal(24,5) DEFAULT NULL COMMENT '本期结算工程量',
  `current_tax` decimal(24,5) DEFAULT NULL COMMENT '本期结算税额',
  `current_tax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '本期结算金额(含税)',
  `current_total_ntax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '本期末累计结算金额(不含税)',
  `current_total_settle_quantity` decimal(24,0) DEFAULT NULL COMMENT '本期末累计结算工程量',
  `current_total_tax` decimal(24,5) DEFAULT NULL COMMENT '本期末累计结算税额',
  `current_total_tax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '本期末累计结算金额(含税)',
  `item_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结算明细表关联id',
  `item_unique_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '明细表唯一id',
  `ledger_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '台账主表id',
  `measure_unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量单位，如m³',
  `metrological_rules` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量规则，如按体积计算',
  `ntax_price` decimal(24,5) DEFAULT NULL COMMENT '单价（不含税）',
  `pre_total_ntax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '上期末结算金额(不含税)',
  `pre_total_settle_quantity` decimal(24,0) DEFAULT NULL COMMENT '上期末结算工程量',
  `pre_total_tax` decimal(24,5) DEFAULT NULL COMMENT '上期末结算税额',
  `pre_total_tax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '上期末结算金额(含税)',
  `quantity` decimal(24,0) DEFAULT NULL COMMENT '合同工程量',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `rent_mode` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租赁方式(1-日 2-月租 3-工作量)',
  `research_project` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '研发项目(成本归集方式非“工程施工合同履约成本” 选择)',
  `settle_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结算单ID,是contract_settlement的id',
  `specs` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格型号',
  `subject_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本科目编码',
  `subject_dtl_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本子目编码（物资编码）',
  `subject_dtl_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本子目名称（物资名称）',
  `subject_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本科目档案ID,关联xxx表',
  `subject_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本科目名称',
  `tax_price` decimal(24,5) DEFAULT NULL COMMENT '单价（含税）,=单价（不含税）*（1+增值税率）=单价（含税）',
  `tax_rate` decimal(24,5) DEFAULT NULL COMMENT '税率',
  `parnt_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父级id',
  `create_dept` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='结算单清单结算明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_receipt_contract_settle_mechanical`
--

DROP TABLE IF EXISTS `tb_receipt_contract_settle_mechanical`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_receipt_contract_settle_mechanical` (
  `id` bigint NOT NULL COMMENT 'id',
  `third_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方id',
  `cost_collection_mode` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本归集方式（1-工程施工合同履约成本 2-研发开支-资本化 3-研发开支-费用化）',
  `current_ntax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '本期结算金额(不含税)',
  `current_settle_quantity` decimal(24,0) DEFAULT NULL COMMENT '本期结算工程量',
  `current_tax` decimal(24,5) DEFAULT NULL COMMENT '本期结算税额',
  `current_tax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '本期结算金额(含税)',
  `current_total_ntax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '本期末累计结算金额(不含税)',
  `current_total_settle_quantity` decimal(24,0) DEFAULT NULL COMMENT '本期末累计结算工程量',
  `current_total_tax` decimal(24,5) DEFAULT NULL COMMENT '本期末累计结算税额',
  `current_total_tax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '本期末累计结算金额(含税)',
  `device_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备编码',
  `device_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备名称',
  `measure_unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量单位',
  `ntax_price` decimal(24,5) DEFAULT NULL COMMENT '单价（不含税）',
  `pre_total_ntax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '上期末结算金额(不含税)',
  `pre_total_settle_quantity` decimal(24,0) DEFAULT NULL COMMENT '上期末结算工程量',
  `pre_total_tax` decimal(24,5) DEFAULT NULL COMMENT '上期末结算税额',
  `pre_total_tax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '上期末结算金额(含税)',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `research_project` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '研发项目(成本归集方式非“工程施工合同履约成本” 选择)',
  `settle_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结算单ID,是contract_settlement的id',
  `specs` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格型号',
  `subject_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本科目编码',
  `subject_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本科目档案ID,关联xxx表',
  `subject_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本科目名称',
  `table_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结算明细表关联id',
  `table_unique_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '明细表唯一id',
  `tax_price` decimal(24,5) DEFAULT NULL COMMENT '单价（含税）',
  `tax_rate` decimal(24,5) DEFAULT NULL COMMENT '税率(%)',
  `parnt_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父级id',
  `create_dept` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='结算单机械台班明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_receipt_contract_settle_other_payment`
--

DROP TABLE IF EXISTS `tb_receipt_contract_settle_other_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_receipt_contract_settle_other_payment` (
  `id` bigint NOT NULL COMMENT 'id',
  `third_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方id',
  `cost_collection_mode` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本归集方式（1-工程施工合同履约成本 2-研发开支-资本化 3-研发开支-费用化）',
  `ntax_amount` decimal(24,5) DEFAULT NULL COMMENT '金额（不含税）',
  `payment_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '清单名称',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `research_project` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '研发项目(成本归集方式非“工程施工合同履约成本” 选择)',
  `settle_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结算单ID,是contract_settlement的id',
  `subject_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本科目编码',
  `subject_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本科目档案ID',
  `subject_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本科目名称',
  `tax` decimal(24,5) DEFAULT NULL COMMENT '税额',
  `tax_amount` decimal(24,5) DEFAULT NULL COMMENT '金额（含税）',
  `tax_rate` decimal(24,5) DEFAULT NULL COMMENT '税率(%)',
  `parnt_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父级id',
  `create_dept` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='结算单其他款项结算明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_receipt_contract_settle_sporadic_work`
--

DROP TABLE IF EXISTS `tb_receipt_contract_settle_sporadic_work`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_receipt_contract_settle_sporadic_work` (
  `id` bigint NOT NULL COMMENT 'id',
  `third_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方id',
  `cost_collection_mode` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本归集方式（1-工程施工合同履约成本 2-研发开支-资本化 3-研发开支-费用化）',
  `ntax_amount` decimal(24,5) DEFAULT NULL COMMENT '金额（不含税）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `research_project` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '研发项目(成本归集方式非“工程施工合同履约成本” 选择)',
  `settle_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结算单ID',
  `subject_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本科目编码',
  `subject_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本科目档案ID',
  `subject_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本科目名称',
  `tax` decimal(24,5) DEFAULT NULL COMMENT '税额',
  `tax_amount` decimal(24,5) DEFAULT NULL COMMENT '金额（含税）',
  `tax_rate` decimal(24,5) DEFAULT NULL COMMENT '税率(%)',
  `work_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '清单名称',
  `parnt_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父级id',
  `create_dept` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='结算单零星包工明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_receipt_contract_settlement`
--

DROP TABLE IF EXISTS `tb_receipt_contract_settlement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_receipt_contract_settlement` (
  `id` bigint NOT NULL COMMENT 'id',
  `third_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方id',
  `state` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '2' COMMENT '2待确认 3已确认',
  `attach_business_id` bigint DEFAULT NULL COMMENT '附件id',
  `bill_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结算单编码',
  `con_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同编码',
  `con_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同id',
  `con_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同名称',
  `con_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '支出业务分类( 0-其他 1-劳务分包 2-专业分包 3-购买材料 4-租赁材料 5-设备租赁（机械）)',
  `con_unique_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同唯一id',
  `current_imprest_deduct_amount` decimal(24,5) DEFAULT NULL COMMENT '本期应扣回预付款',
  `current_ntax_out_con_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '本期合同外结算金额（不含税）(元)',
  `current_ntax_payment_obligation` decimal(24,5) DEFAULT NULL COMMENT '本期付款义务（不含税）( = 本期结算金额（含税）*当前付款比例（%）)',
  `current_ntax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '本期结算金额（不含税）（元）',
  `current_out_con_settle_ratio` decimal(24,5) DEFAULT NULL COMMENT '本期合同外结算占合同金额比例（%）（=其中本期合同外结算金额(含税）/合同金额（含税））',
  `current_payment_ratio` decimal(24,5) DEFAULT NULL COMMENT '当前付款比例（%）',
  `current_settle_amount_ratio` decimal(24,5) DEFAULT NULL COMMENT '本期结算比例（%）',
  `current_tax_out_con_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '本期合同外结算金额（含税）(元)',
  `current_tax_payment_obligation` decimal(24,5) DEFAULT NULL COMMENT '本期付款义务（含税）( = 本期结算金额（不含税）*当前付款比例（%）)',
  `current_tax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '本期结算金额（含税）（元）',
  `current_total_ntax_out_con_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '本期末累计合同外结算金额（不含税）(元)',
  `current_total_ntax_payment_obligation` decimal(24,5) DEFAULT NULL COMMENT '本期末累计付款义务（不含税）',
  `current_total_ntax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '本期末累计结算金额（不含税）（元）',
  `current_total_out_con_settle_ratio` decimal(24,5) DEFAULT NULL COMMENT '本期末累计合同外结算占合同金额比例（%）',
  `current_total_settle_amount_ratio` decimal(24,5) DEFAULT NULL COMMENT '本期末累计结算比例（%）',
  `current_total_tax_out_con_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '本期末累计合同外结算金额（含税）(元)',
  `current_total_tax_payment_obligation` decimal(24,5) DEFAULT NULL COMMENT '本期末累计付款义务（含税）',
  `current_total_tax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '本期末累计结算金额（含税）（元）',
  `invoice_type` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发票类型（1-专票 2-普票 3-数电票）',
  `last_proc_approver` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最后审批人',
  `next_proc_approver` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '下一审批人',
  `org_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属本级组织id',
  `org_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属本级组织名称',
  `parta_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同甲方名称',
  `partb_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同乙方名称',
  `per_total_ntax_out_con_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '上期末累计合同外结算金额（不含税）(元)',
  `per_total_ntax_payment_obligation` decimal(24,5) DEFAULT NULL COMMENT '上期末累计付款义务（不含税）',
  `per_total_ntax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '上期末累计结算金额（不含税）（元）',
  `per_total_out_con_settle_ratio` decimal(24,5) DEFAULT NULL COMMENT '上期末累计合同外结算占合同金额比例（%）',
  `per_total_settle_amount_ratio` decimal(24,5) DEFAULT NULL COMMENT '上期末累计结算比例（%）',
  `per_total_tax_out_con_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '上期末累计合同外结算金额（含税）(元)',
  `per_total_tax_payment_obligation` decimal(24,5) DEFAULT NULL COMMENT '上期末累计付款义务（含税）',
  `per_total_tax_settle_amount` decimal(24,5) DEFAULT NULL COMMENT '上期末累计结算金额（含税）（元）',
  `proc_complete_time` datetime DEFAULT NULL COMMENT '审批完成时间',
  `proc_status` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程状态 0- 自由态； 1- 审批中； 2- 被驳回； 3- 已撤销； 4- 已完成',
  `project_as_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属最小核算项目简称',
  `project_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属最小核算项目项目编码',
  `project_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最小核算项目id',
  `project_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属最小核算项目全称',
  `settle_date` datetime DEFAULT NULL COMMENT '结算日期（年月日）：默认制单日期',
  `settle_end_date` datetime DEFAULT NULL COMMENT '结算结束日期（年月日）',
  `settle_period` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结算期数（流水号）',
  `settle_start_date` datetime DEFAULT NULL COMMENT '结算开始日期（年月日）',
  `settle_type` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结算类型(1-过程结算 2-完工结算)',
  `tax_changed_amount` decimal(24,5) DEFAULT NULL COMMENT '合同金额（含税）=合同变更后金额',
  `tax_obligation_owed_amount` decimal(24,5) DEFAULT NULL COMMENT '按付款义务欠付金额(含税)（本期末累计付款义务(含税)-累计已付金额）',
  `tax_settle_owed_amount` decimal(24,5) DEFAULT NULL COMMENT '按结算金额欠付金额(含税)(元)（本期末累计结算金额(含税)-累计已付金额）',
  `total_actual_payment_amount` decimal(24,5) DEFAULT NULL COMMENT '累计已付金额(累计实付付款金额)',
  `total_imprest_deduct_amount` decimal(24,5) DEFAULT NULL COMMENT '累计应扣回的预付款金额（本期应扣回预付款:+累计已扣回的预付款实际金额）',
  `total_imprest_deducted_amount` decimal(24,5) DEFAULT NULL COMMENT '累计已扣回的预付款实际金额',
  `supplier_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应商id',
  `supplier_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应商名称',
  `create_dept` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '制单人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '制单时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='结算单主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_receipt_materials_device_ledger`
--

DROP TABLE IF EXISTS `tb_receipt_materials_device_ledger`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_receipt_materials_device_ledger` (
  `id` bigint NOT NULL COMMENT 'id',
  `third_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方id',
  `state` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '2' COMMENT '操作状态(2待确认 3已确认)',
  `con_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同编码',
  `con_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同名称',
  `con_unique_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同uninque',
  `end_date` datetime DEFAULT NULL COMMENT '对账结束日期',
  `ledger_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备台账编码',
  `month` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '对账月',
  `month_rent_include` decimal(24,5) DEFAULT NULL COMMENT '本月租金(含税)',
  `month_rent_notax` decimal(24,5) DEFAULT NULL COMMENT '本月租金(不含税)',
  `month_rent_taxmoney` decimal(24,5) DEFAULT NULL COMMENT '本月租金税额',
  `month_rent_total` decimal(24,5) DEFAULT NULL COMMENT '本期累计租金',
  `org_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属本级组织',
  `period` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '对账周期',
  `project_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属最小核算项目编码',
  `project_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目id',
  `project_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属最小核算项目',
  `rent_type` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计租方式(数据字典：RENT_TYPE) 1算头又算尾 2算头不算尾 3算尾不算头 4头尾都不算',
  `settle_date` datetime DEFAULT NULL COMMENT '结算日期',
  `settle_status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结算状态,数据字典：SETTLE_STATUS',
  `start_date` datetime DEFAULT NULL COMMENT '对账开始日期',
  `supplier_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应商id',
  `supplier_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应商名称',
  `create_dept` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '制单人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '制单时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='设备租赁台账单据';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_receipt_materials_device_ledger_dtl`
--

DROP TABLE IF EXISTS `tb_receipt_materials_device_ledger_dtl`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_receipt_materials_device_ledger_dtl` (
  `id` bigint NOT NULL COMMENT 'id',
  `third_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方id',
  `amt_include_tax` decimal(24,5) DEFAULT NULL COMMENT '金额(含税)',
  `amt_no_tax` decimal(24,5) DEFAULT NULL COMMENT '金额(不含税)',
  `amt_tax_money` decimal(24,5) DEFAULT NULL COMMENT '金额税额',
  `device_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备编码',
  `device_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备名称',
  `end_datetime` datetime DEFAULT NULL COMMENT '施工结束',
  `ledger_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备台账id',
  `machine_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '台班类型,数据字典：MTR_MACH_TYPE',
  `material_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物资编码',
  `material_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物资名称',
  `specification_model` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格型号',
  `stagnate_machine_settle_ratio` decimal(24,0) DEFAULT NULL COMMENT '停滞台班结算比例',
  `start_datetime` datetime DEFAULT NULL COMMENT '施工开始',
  `tax_rate` decimal(24,5) DEFAULT NULL COMMENT '税率',
  `unit_include_tax` decimal(24,5) DEFAULT NULL COMMENT '单价(含税)',
  `unit_no_tax` decimal(24,5) DEFAULT NULL COMMENT '单价(不含税)',
  `unit_tax_money` decimal(24,5) DEFAULT NULL COMMENT '单价税额',
  `work_hours` decimal(24,5) DEFAULT NULL COMMENT '工作时长',
  `workload` decimal(24,5) DEFAULT NULL COMMENT '工作量',
  `parnt_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父级id',
  `create_dept` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '制单人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '制单时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='设备租赁台账详情';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_receipt_materials_device_ledger_dtl_dtl`
--

DROP TABLE IF EXISTS `tb_receipt_materials_device_ledger_dtl_dtl`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_receipt_materials_device_ledger_dtl_dtl` (
  `id` bigint NOT NULL COMMENT 'id',
  `third_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方id',
  `end_datetime` datetime DEFAULT NULL COMMENT '台班结束时间',
  `ledger_dtl_id` varbinary(64) DEFAULT NULL COMMENT '设备台账详情id',
  `ledger_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备台账id',
  `start_datetime` datetime DEFAULT NULL COMMENT '台班开始时间',
  `work_hours` decimal(24,5) DEFAULT NULL COMMENT '工作时长',
  `workload` decimal(24,5) DEFAULT NULL COMMENT '工作量',
  `parnt_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父级id',
  `create_dept` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '制单人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '制单时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='设备租赁台账详情';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_receipt_materials_turn_ledger`
--

DROP TABLE IF EXISTS `tb_receipt_materials_turn_ledger`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_receipt_materials_turn_ledger` (
  `id` bigint NOT NULL COMMENT 'id',
  `third_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方id',
  `state` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '2' COMMENT '操作状态(2待确认 3已确认)',
  `con_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同编码',
  `con_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同名称',
  `con_unique_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同uninque',
  `end_date` datetime DEFAULT NULL COMMENT '对账结束日期',
  `ledger_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '周材台账编码',
  `month` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '对账月',
  `month_rent_include` decimal(24,5) DEFAULT NULL COMMENT '本月租金(含税)',
  `month_rent_notax` decimal(24,5) DEFAULT NULL COMMENT '本月租金(不含税)',
  `month_rent_taxmoney` decimal(24,5) DEFAULT NULL COMMENT '本月租金税额',
  `month_rent_total` decimal(24,5) DEFAULT NULL COMMENT '本期累计租金',
  `org_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属本级组织',
  `period` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '对账周期',
  `project_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属最小核算项目编码',
  `project_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目id',
  `project_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属最小核算项目',
  `rent_type` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计租方式(数据字典：RENT_TYPE) 1算头又算尾 2算头不算尾 3算尾不算头 4头尾都不算',
  `settle_date` datetime DEFAULT NULL COMMENT '结算日期',
  `settle_status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '结算状态,数据字典：SETTLE_STATUS',
  `start_date` datetime DEFAULT NULL COMMENT '对账开始日期',
  `supplier_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应商id',
  `supplier_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应商名称',
  `create_dept` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '制单人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '制单时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='周材租赁台账单据';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_receipt_materials_turn_ledger_dtl`
--

DROP TABLE IF EXISTS `tb_receipt_materials_turn_ledger_dtl`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_receipt_materials_turn_ledger_dtl` (
  `id` bigint NOT NULL COMMENT 'id',
  `third_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方id',
  `amt_include_tax` decimal(24,5) DEFAULT NULL COMMENT '金额(含税)',
  `amt_no_tax` decimal(24,5) DEFAULT NULL COMMENT '金额(不含税)',
  `amt_tax_money` decimal(24,5) DEFAULT NULL COMMENT '金额税额',
  `ledger_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '周转材台账id',
  `ledger_rent_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '周材台账租金类型,数据字典：MTR_LEDGER_RENT_TYPE',
  `in_num` decimal(11,0) DEFAULT NULL COMMENT '进场数量',
  `machine_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '台班类型,数据字典：MTR_MACH_TYPE',
  `material_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物资编码',
  `material_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物资名称',
  `measurement_unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量单位',
  `rent_days` int DEFAULT NULL COMMENT '本期租赁天数',
  `specification_model` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格型号',
  `stagnate_machine_settle_ratio` decimal(24,0) DEFAULT NULL COMMENT '停滞台班结算比例',
  `start_rent_date` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '开始计租日期',
  `start_rent_date_flag` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '开始计租日期类型(false-承上期,true-日期)',
  `tax_rate` decimal(24,5) DEFAULT NULL COMMENT '税率',
  `unit_include_tax` decimal(24,5) DEFAULT NULL COMMENT '单价(含税)',
  `unit_no_tax` decimal(24,5) DEFAULT NULL COMMENT '单价(不含税)',
  `unit_tax_money` decimal(24,5) DEFAULT NULL COMMENT '单价税额',
  `workload` decimal(24,5) DEFAULT NULL COMMENT '工作量',
  `parnt_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父级id',
  `create_dept` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '制单人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '制单时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='周材租赁台账详情';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_receipt_materials_turn_ledger_dtl_dtl`
--

DROP TABLE IF EXISTS `tb_receipt_materials_turn_ledger_dtl_dtl`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_receipt_materials_turn_ledger_dtl_dtl` (
  `id` bigint NOT NULL COMMENT 'id',
  `third_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方id',
  `amt_include_tax` decimal(24,5) DEFAULT NULL COMMENT '金额(含税)',
  `amt_no_tax` decimal(24,5) DEFAULT NULL COMMENT '金额(不含税)',
  `amt_tax_money` decimal(24,5) DEFAULT NULL COMMENT '金额税额',
  `in_date` datetime DEFAULT NULL COMMENT '进场日期',
  `ledger_dtl_id` varbinary(64) DEFAULT NULL COMMENT '周转材台账详情id',
  `ledger_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '周转材台账id',
  `in_num` decimal(24,5) DEFAULT NULL COMMENT '进场数量',
  `material_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物资编码',
  `tax_rate` decimal(24,5) DEFAULT NULL COMMENT '税率',
  `unit_include_tax` decimal(24,5) DEFAULT NULL COMMENT '单价(含税)',
  `unit_no_tax` decimal(24,5) DEFAULT NULL COMMENT '单价(不含税)',
  `unit_tax_money` decimal(24,5) DEFAULT NULL COMMENT '单价税额',
  `parnt_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父级id',
  `create_dept` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '制单人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '制单时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='周材租赁台账详情';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_receipt_purchase`
--

DROP TABLE IF EXISTS `tb_receipt_purchase`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_receipt_purchase` (
  `id` bigint NOT NULL COMMENT 'id',
  `third_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '采购订单id',
  `state` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '2' COMMENT '2待确认 3已确认',
  `acceptance_phone` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '验收人联系方式',
  `acceptance_user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '验收人id',
  `acceptance_user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '提货验收人名称',
  `attach_business_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `con_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '支出合同编码',
  `con_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '支出合同id',
  `con_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '支出合同名称',
  `delivery_method` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '提货方式(字典表)',
  `detail_address` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '详细地址',
  `order_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '采购订单编码',
  `order_date` datetime DEFAULT NULL COMMENT '采购日期',
  `order_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '采购订单名称',
  `order_status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '订单状态',
  `org_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属本级组织id',
  `org_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属本级组织名称',
  `plan_entry_time` datetime DEFAULT NULL COMMENT '计划进场时间',
  `proc_status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批状态',
  `process_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `project_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最小核算项目编码',
  `project_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最小核算项目id',
  `project_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最小核算项目名称',
  `purchase_money` decimal(24,5) DEFAULT '0.00000' COMMENT '采购金额',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `supplier_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应商id',
  `supplier_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应商名称',
  `create_dept` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='采购订单主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_receipt_purchase_detail`
--

DROP TABLE IF EXISTS `tb_receipt_purchase_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_receipt_purchase_detail` (
  `id` bigint NOT NULL COMMENT 'id',
  `brand` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '品牌',
  `shipment_quantity` decimal(24,5) DEFAULT '0.00000' COMMENT '本次发货量',
  `can_order_number` decimal(11,0) DEFAULT NULL COMMENT '可下订单数量',
  `residue_order_number` decimal(11,0) DEFAULT '0' COMMENT '剩余订单数量',
  `con_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '支出合同主键',
  `contract_list_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '支出合同清单主键',
  `contract_number` decimal(11,0) DEFAULT NULL COMMENT '合同数量',
  `is_all_storage` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否全部入库',
  `material_class_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分类编码',
  `material_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物资编码',
  `material_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物资名称',
  `subject_dtl_unique_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本子目唯一id',
  `material_storage_number` decimal(11,0) DEFAULT NULL COMMENT '已入库数量',
  `measurement_unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量单位',
  `measurement_unit_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量单位id',
  `price_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '价格类型',
  `purchase_number` decimal(11,0) DEFAULT NULL COMMENT '本次采购数量',
  `specification_model` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格型号',
  `tax_rate` decimal(24,5) DEFAULT NULL COMMENT '税率',
  `total_include_tax` decimal(24,5) DEFAULT NULL COMMENT '含税总价',
  `total_no_tax` decimal(24,5) DEFAULT NULL COMMENT '不含税总价',
  `total_tax_money` decimal(24,5) DEFAULT NULL COMMENT '总价税额',
  `unit_include_tax` decimal(24,5) DEFAULT NULL COMMENT '含税单价',
  `unit_no_tax` decimal(24,5) DEFAULT NULL COMMENT '不含税单价',
  `parnt_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父级id',
  `create_dept` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='采购订单详情';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_receipt_purchase_invoice`
--

DROP TABLE IF EXISTS `tb_receipt_purchase_invoice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_receipt_purchase_invoice` (
  `id` bigint NOT NULL COMMENT 'id',
  `brand_material_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发货单名称',
  `shipment_order_money` decimal(24,5) DEFAULT NULL COMMENT '本次发货单金额',
  `invoice_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发货单编号',
  `code_serial_number` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0001' COMMENT '编码序号',
  `invoice_date` datetime DEFAULT NULL COMMENT '发货单日期',
  `expected_delivery_date` datetime DEFAULT NULL COMMENT '预计到货日期',
  `receipt_purchase_id` bigint DEFAULT NULL COMMENT '采购订单id',
  `order_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '采购订单名称',
  `order_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '采购订单编码',
  `order_date` datetime DEFAULT NULL COMMENT '采购日期',
  `plan_entry_time` datetime DEFAULT NULL COMMENT '计划进场时间',
  `create_dept` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `purchase_money` decimal(24,5) DEFAULT NULL COMMENT '采购金额',
  `acceptance_user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '提货验收人名称',
  `acceptance_phone` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '验收人联系方式',
  `con_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同编码',
  `con_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同名称',
  `project_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最小核算项目名称',
  `org_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属本级组织id',
  `org_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属本级组织名称',
  `or_code` blob COMMENT '二维码',
  `supplier_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应商id',
  `supplier_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应商名称',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='发货单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_receipt_purchase_invoice_detail`
--

DROP TABLE IF EXISTS `tb_receipt_purchase_invoice_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_receipt_purchase_invoice_detail` (
  `id` bigint NOT NULL COMMENT 'id',
  `detail_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '采购订单明细id',
  `brand` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '品牌',
  `shipment_quantity` decimal(24,5) DEFAULT '0.00000' COMMENT '本次发货量',
  `can_order_number` decimal(24,0) DEFAULT NULL COMMENT '可下订单数量',
  `con_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '支出合同主键',
  `contract_list_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '支出合同清单主键',
  `contract_number` decimal(24,0) DEFAULT '0' COMMENT '合同数量',
  `is_all_storage` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否全部入库',
  `material_class_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分类编码',
  `material_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物资编码',
  `material_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物资名称',
  `subject_dtl_unique_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '成本子目唯一id',
  `material_storage_number` decimal(24,0) DEFAULT '0' COMMENT '已入库数量',
  `measurement_unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量单位',
  `measurement_unit_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量单位id',
  `price_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '价格类型',
  `purchase_number` decimal(11,0) DEFAULT '0' COMMENT '本次采购数量',
  `specification_model` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格型号',
  `tax_rate` decimal(24,5) DEFAULT '0.00000' COMMENT '税率',
  `total_include_tax` decimal(24,5) DEFAULT '0.00000' COMMENT '含税总价',
  `total_no_tax` decimal(24,5) DEFAULT '0.00000' COMMENT '不含税总价',
  `total_tax_money` decimal(24,5) DEFAULT '0.00000' COMMENT '总价税额',
  `unit_include_tax` decimal(24,5) DEFAULT '0.00000' COMMENT '含税单价',
  `unit_no_tax` decimal(24,5) DEFAULT '0.00000' COMMENT '不含税单价',
  `tax_money` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '税额',
  `this_time_total_no_tax` decimal(24,5) DEFAULT '0.00000' COMMENT '本次发货金额不含税',
  `this_time_unit_include` decimal(24,5) DEFAULT '0.00000' COMMENT '本次发货金额含税',
  `this_time_tax_money` decimal(24,5) DEFAULT '0.00000' COMMENT '本次发货金额税额',
  `parnt_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父级id',
  `create_dept` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='发货单详情';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_receipt_reconciliation`
--

DROP TABLE IF EXISTS `tb_receipt_reconciliation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_receipt_reconciliation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '材料对账单id',
  `third_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '材料对账单第三方id',
  `order_status` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '单据状态',
  `org_id` bigint DEFAULT NULL COMMENT '归属本级组织id',
  `org_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '归属本级组织名称',
  `plan_b_reconciler_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方对账人',
  `proc_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '单据状态',
  `project_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最小核算项目编码',
  `project_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最小核算项目id',
  `project_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最小核算项目名称',
  `reconciliation_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '对账单编码',
  `reconciliation_date` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '对账日期',
  `reconciliation_end_date` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '对账周期-结束时间',
  `reconciliation_start_date` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '对账周期-开始时间',
  `supplier_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应商id',
  `supplier_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应商名称',
  `attach_business_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '附件业务id',
  `con_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '支出合同编码',
  `con_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '支出合同id',
  `con_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '支出合同名称',
  `third_create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方创建人',
  `third_create_time` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方创建时间',
  `create_by` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志',
  `supplier_status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '2' COMMENT '确认状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1839584823231459330 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='材料对账单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_receipt_reconciliation_detail`
--

DROP TABLE IF EXISTS `tb_receipt_reconciliation_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_receipt_reconciliation_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '对账单详情id',
  `actual_entry_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '入库日期',
  `con_dtl_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '支出合同清单主键',
  `doc_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `material_class_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物资类编码',
  `material_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物资编码',
  `material_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '物资名称',
  `materials_storage_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '入库单编码',
  `materials_storage_dtl_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '入库单主键',
  `materials_storage_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '入库单主键',
  `measurement_unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量单位',
  `price_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '价格类型',
  `reconciliation_include_tax` decimal(25,4) DEFAULT NULL COMMENT '本次对账金额(含税)',
  `reconciliation_no_tax` decimal(25,4) DEFAULT NULL COMMENT '本次对账金额(不含税)',
  `reconciliation_tax_money` decimal(25,4) DEFAULT NULL COMMENT '本次对账金额(税额)',
  `specification_model` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格型号',
  `storage_number` decimal(25,2) DEFAULT NULL COMMENT '入库数量',
  `tax_rate` decimal(25,4) DEFAULT NULL COMMENT '税率',
  `unit_include_tax` decimal(25,4) DEFAULT NULL COMMENT '含税单价',
  `unit_no_tax` decimal(25,4) DEFAULT NULL COMMENT '不含税单价',
  `unit_tax_money` decimal(25,4) DEFAULT NULL COMMENT '单价税额',
  `create_by` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` int DEFAULT NULL COMMENT '创建人id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` int DEFAULT NULL COMMENT '修改人id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志(0代表存在 2代表删除)',
  `reconciliation_id` bigint DEFAULT NULL COMMENT '材料对账单id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1839584823374065667 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='材料对账单详情';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_template`
--

DROP TABLE IF EXISTS `tb_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_template` (
  `id` bigint NOT NULL COMMENT '主键 id',
  `template_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板名称',
  `template_type` int NOT NULL COMMENT '模板分类',
  `using_unit_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '使用单位编码',
  `using_unit_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '使用单位名称',
  `is_editable` int DEFAULT NULL COMMENT '是否允许编辑',
  `attachment_id` bigint DEFAULT NULL COMMENT '附件id',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `contract_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同类型',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_tender_apply`
--

DROP TABLE IF EXISTS `tb_tender_apply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_tender_apply` (
  `id` bigint NOT NULL COMMENT '主键',
  `notice_id` bigint DEFAULT NULL COMMENT '招标公告id',
  `contact` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系人',
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系电话',
  `attach_id` bigint DEFAULT NULL COMMENT '附件',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商id',
  `vendor_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应商名称',
  `ip_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作者ip',
  `approve_result` int DEFAULT NULL COMMENT '资审结果（0不通过 1通过）',
  `scheme_id` bigint DEFAULT NULL COMMENT '采购方案id',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='招标报名（已弃用）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_tender_content`
--

DROP TABLE IF EXISTS `tb_tender_content`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_tender_content` (
  `id` bigint NOT NULL COMMENT '主键id',
  `bidding_time` datetime DEFAULT NULL COMMENT '投标截止时间',
  `contact` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系人',
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系电话',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系邮箱',
  `tender_attach_id` bigint DEFAULT NULL COMMENT '招标文件附件id',
  `evaluat_attach_id` bigint DEFAULT NULL COMMENT '评标规则附件id',
  `contract_attach_id` bigint DEFAULT NULL COMMENT '合同文件附件id',
  `project_id` bigint DEFAULT NULL COMMENT '招标项目id',
  `scheme_id` bigint DEFAULT NULL COMMENT '采购方案id',
  `twice_bidding_time` datetime DEFAULT NULL COMMENT '二次报价截止时间',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='招标内容（已弃用）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_tender_notice`
--

DROP TABLE IF EXISTS `tb_tender_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_tender_notice` (
  `id` bigint NOT NULL COMMENT '主键',
  `project_id` bigint DEFAULT NULL COMMENT '招标项目id',
  `scheme_id` bigint DEFAULT NULL COMMENT '采购方案id',
  `apply_time` datetime DEFAULT NULL COMMENT '报名截止时间',
  `contact` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系人',
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系电话',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系邮箱',
  `template_id` bigint DEFAULT NULL COMMENT '招标公告模板id',
  `attach_id` bigint DEFAULT NULL COMMENT '招标公告附件id',
  `vendor_range` int DEFAULT NULL COMMENT '是否设置供应商范围（0不设置 1设置）',
  `notice_status` int DEFAULT NULL COMMENT '招标公告状态（招标阶段流程状态）0已发布/1已回标/2已开标/3已评标/4已定标/5已公示/6结果已发布',
  `is_open_people` int DEFAULT '0' COMMENT '是否设置开标人员（0未设置 1已设置）',
  `is_eval` int DEFAULT '0' COMMENT '专家是否开启评标（0否-默认 1是）',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
  `contact_notice` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发布公告联系人',
  `phone_notice` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发布公告联系电话',
  `email_notice` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发布公告联系邮箱',
  `apply_time_notice` datetime DEFAULT NULL COMMENT '发布公告报名截止时间',
  `attach_id_notice` bigint DEFAULT NULL COMMENT '发布公告附件id',
  `twice_quot_version` int DEFAULT NULL COMMENT '二次报价版本号 从1开始',
  `twice_quot_state` int DEFAULT NULL COMMENT '二次报价状态 0关闭报价 1开放报价',
  `twice_time` datetime DEFAULT NULL COMMENT '二次报价截止时间',
  `payment_type` int DEFAULT NULL COMMENT '付款方式 现付1旬付2月付3资金利息4',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `tb_tender_notice_scheme_id_IDX` (`scheme_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='招标公告';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_tender_notice_answer`
--

DROP TABLE IF EXISTS `tb_tender_notice_answer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_tender_notice_answer` (
  `id` bigint NOT NULL COMMENT '主键',
  `bus_id` bigint DEFAULT NULL COMMENT '关联业务id（招标公告id/招标内容id）',
  `bus_type` int DEFAULT NULL COMMENT '业务类型（招标公告/招标内容）',
  `question` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '提问内容',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '答疑内容',
  `question_user` bigint DEFAULT NULL COMMENT '提问用户',
  `answer_user` bigint DEFAULT NULL COMMENT '答疑用户',
  `vendor_id` bigint DEFAULT NULL COMMENT '提问供应商id',
  `vendor_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '提问供应商名称',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `notice_status` int DEFAULT NULL COMMENT '招标公告状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='招标公告/招标内容 答疑';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_tender_notice_change_record`
--

DROP TABLE IF EXISTS `tb_tender_notice_change_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_tender_notice_change_record` (
  `id` bigint NOT NULL COMMENT '主键',
  `notice_id` bigint DEFAULT NULL COMMENT '招标公告id',
  `type` int DEFAULT NULL COMMENT '变更类型（1变更时间 2变更内容）',
  `update_before` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '变更前信息',
  `update_after` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '变更后信息',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `notice_status` int DEFAULT NULL COMMENT '招标公告状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='招标公告变更记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_tender_notice_range`
--

DROP TABLE IF EXISTS `tb_tender_notice_range`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_tender_notice_range` (
  `id` bigint NOT NULL COMMENT '主键',
  `notice_id` bigint DEFAULT NULL COMMENT '招标公告id',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商id',
  `vendor_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应商名称',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `type` int DEFAULT NULL COMMENT '类型（0设置供应商范围 1推荐供应商）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='招标公告供应商范围';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_vendor`
--

DROP TABLE IF EXISTS `tb_vendor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_vendor` (
  `id` bigint NOT NULL COMMENT '主键 id',
  `enterprise_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '企业名称',
  `social_credit_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '统一社会信用代码',
  `registered_capital` decimal(20,2) DEFAULT NULL COMMENT '注册资金',
  `currency_code` int DEFAULT NULL COMMENT '币种',
  `account_branch` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '开户支行',
  `bank_account` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '银行账号',
  `legal_representative` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '法定代表人',
  `legal_phone` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '法人联系方式',
  `legal_id_card` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '法人身份证',
  `enterprise_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '企业分类',
  `taxpayer_type` int DEFAULT NULL COMMENT '增值税纳税人类型',
  `invoice_type` int DEFAULT NULL COMMENT '发票类型',
  `enterprise_nature` int DEFAULT NULL COMMENT '企业性质',
  `enterprise_province_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '企业所在省/市编码',
  `enterprise_province_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '企业所在省/市名称',
  `enterprise_city_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '企业所在地市编码',
  `enterprise_city_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '企业所在地市名称',
  `enterprise_address` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '详细地址',
  `contact_phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '企业联系电话',
  `business_scope` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '经营范围',
  `enterprise_profile` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '企业简介',
  `first_cooperation_company_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '首次合作单位',
  `first_cooperation_company_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '首次合作单位名称',
  `vendor_class` int DEFAULT NULL COMMENT '供应商类别',
  `vendor_level` int DEFAULT NULL COMMENT '供应商等级',
  `state` int DEFAULT NULL COMMENT '状态',
  `sign_state` int DEFAULT NULL COMMENT '签章状态',
  `sign_auth_fail_reason` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '签章认证失败原因',
  `is_black` int DEFAULT '0' COMMENT '是否为黑名单',
  `black_begin_date` date DEFAULT NULL COMMENT '黑名单限制期-开始日期',
  `black_end_date` date DEFAULT NULL COMMENT '黑名单限制期-结束日期',
  `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
  `process_type` int DEFAULT NULL COMMENT '流程类型',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `supplier_sourse` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '供应商注册来源',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='供应商';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_vendor_certification`
--

DROP TABLE IF EXISTS `tb_vendor_certification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_vendor_certification` (
  `id` bigint NOT NULL COMMENT '主键 id',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商id',
  `business_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务编码',
  `business_id` bigint DEFAULT NULL COMMENT '业务id',
  `attachment_file_url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '附件文件路径',
  `attachment_file_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '附件文件名',
  `effective_begin_date` datetime DEFAULT NULL COMMENT '有效期开始时间',
  `effective_end_date` datetime DEFAULT NULL COMMENT '有效期结束时间',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='供应商资质';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_vendor_certification_change`
--

DROP TABLE IF EXISTS `tb_vendor_certification_change`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_vendor_certification_change` (
  `certification_id` bigint DEFAULT NULL COMMENT '供应商资质id',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商id',
  `business_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务编码',
  `business_id` bigint DEFAULT NULL COMMENT '业务id',
  `attachment_file_url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '附件文件路径',
  `attachment_file_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '附件文件名',
  `effective_begin_date` datetime DEFAULT NULL COMMENT '有效期开始时间',
  `effective_end_date` datetime DEFAULT NULL COMMENT '有效期结束时间',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `id` bigint NOT NULL COMMENT '主键 id',
  `version` int NOT NULL COMMENT '版本号',
  `change_status` int DEFAULT NULL COMMENT '变更状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='供应商资质变更';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_vendor_change`
--

DROP TABLE IF EXISTS `tb_vendor_change`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_vendor_change` (
  `vendor_id` bigint NOT NULL COMMENT '供应商id',
  `enterprise_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '企业名称',
  `social_credit_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '统一社会信用代码',
  `registered_capital` decimal(20,2) DEFAULT NULL COMMENT '注册资金',
  `currency_code` int DEFAULT NULL COMMENT '币种',
  `account_branch` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '开户支行',
  `bank_account` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '银行账号',
  `legal_representative` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '法定代表人',
  `legal_phone` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '法人联系方式',
  `legal_id_card` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '法人身份证',
  `enterprise_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '企业分类',
  `taxpayer_type` int DEFAULT NULL COMMENT '增值税纳税人类型',
  `invoice_type` int DEFAULT NULL COMMENT '发票类型',
  `enterprise_nature` int DEFAULT NULL COMMENT '企业性质',
  `enterprise_province_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '企业所在省/市编码',
  `enterprise_province_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '企业所在省/市名称',
  `enterprise_city_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '企业所在地市编码',
  `enterprise_city_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '企业所在地市名称',
  `enterprise_address` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '详细地址',
  `contact_phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '企业联系电话',
  `business_scope` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '经营范围',
  `enterprise_profile` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '企业简介',
  `first_cooperation_company_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '首次合作单位',
  `first_cooperation_company_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '首次合作单位名称',
  `vendor_class` int DEFAULT NULL COMMENT '供应商类别',
  `vendor_level` int DEFAULT NULL COMMENT '供应商等级',
  `state` int DEFAULT NULL COMMENT '状态',
  `is_black` int DEFAULT '0' COMMENT '是否为黑名单',
  `black_begin_date` date DEFAULT NULL COMMENT '黑名单限制期-开始日期',
  `black_end_date` date DEFAULT NULL COMMENT '黑名单限制期-结束日期',
  `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `id` bigint NOT NULL COMMENT '变更 id',
  `version` int NOT NULL COMMENT '版本号',
  `change_status` int DEFAULT NULL COMMENT '变更状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='供应商变更';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_vendor_classify`
--

DROP TABLE IF EXISTS `tb_vendor_classify`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_vendor_classify` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `parent_id` bigint DEFAULT NULL COMMENT '父id',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分类名称',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分类编码',
  `level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '层级',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=123 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='供应商分类';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_vendor_contact`
--

DROP TABLE IF EXISTS `tb_vendor_contact`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_vendor_contact` (
  `id` bigint NOT NULL COMMENT '主键 id',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商id',
  `contact_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系人名称',
  `contact_id_card` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系人身份证',
  `contact_phone` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系人电话',
  `contact_email` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系人邮箱',
  `is_manager` int DEFAULT '0' COMMENT '是否为管理员',
  `is_legal` int DEFAULT '0' COMMENT '是否为法人',
  `state` int DEFAULT '0' COMMENT '状态',
  `sign_state` int DEFAULT '0' COMMENT '签订状态',
  `sign_auth_fail_reason` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '签章认证失败原因',
  `is_main_contact` int DEFAULT '0' COMMENT '是否为主要联系人',
  `login_user_id` bigint DEFAULT NULL COMMENT '登录用户 id',
  `certification_id` bigint DEFAULT NULL COMMENT '授权书id',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='供应商联系人';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_vendor_contact_change`
--

DROP TABLE IF EXISTS `tb_vendor_contact_change`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_vendor_contact_change` (
  `contact_id` bigint NOT NULL COMMENT '联系人 id',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商id',
  `contact_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系人名称',
  `contact_id_card` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系人身份证',
  `contact_phone` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系人电话',
  `contact_email` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系人邮箱',
  `is_manager` int DEFAULT '0' COMMENT '是否为管理员',
  `is_legal` int DEFAULT '0' COMMENT '是否为法人',
  `state` int DEFAULT '0' COMMENT '状态',
  `is_main_contact` int DEFAULT '0' COMMENT '是否为主要联系人',
  `login_user_id` bigint DEFAULT NULL COMMENT '登录用户 id',
  `certification_id` bigint DEFAULT NULL COMMENT '授权书id',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
  `id` bigint NOT NULL COMMENT '变更 id',
  `version` int NOT NULL COMMENT '版本号',
  `change_status` int DEFAULT NULL COMMENT '变更状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='供应商联系人变更';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_vendor_operate_log`
--

DROP TABLE IF EXISTS `tb_vendor_operate_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_vendor_operate_log` (
  `id` bigint NOT NULL COMMENT '主键 id',
  `vendor_id` bigint NOT NULL COMMENT '供应商 id',
  `business_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作业务类型',
  `state` int DEFAULT '0' COMMENT '操作状态',
  `wf_process_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程实例 id',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='供应商操作日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_vendor_performance_evaluation`
--

DROP TABLE IF EXISTS `tb_vendor_performance_evaluation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_vendor_performance_evaluation` (
  `id` bigint NOT NULL COMMENT '主键id',
  `project_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '项目id',
  `vendor_id` bigint NOT NULL COMMENT '供应商 id',
  `agreement_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '合同编号',
  `agreement_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同名称',
  `party_a_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '甲方id',
  `party_a_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '甲方名称',
  `party_b_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方id',
  `party_b_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '乙方名称',
  `excellent_num` decimal(20,4) DEFAULT NULL COMMENT '优-数量',
  `good_num` decimal(20,4) DEFAULT NULL COMMENT '良-数量',
  `qualified_num` decimal(20,4) DEFAULT NULL COMMENT '合格-数量',
  `bad_num` decimal(20,4) DEFAULT NULL COMMENT '差-数量',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_id` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='供应商履约评价';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_winning_bid_ranking`
--

DROP TABLE IF EXISTS `tb_winning_bid_ranking`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tb_winning_bid_ranking` (
  `id` bigint DEFAULT NULL COMMENT 'id',
  `procurement_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '采购类型',
  `vendor_id` bigint DEFAULT NULL COMMENT '中标供应商id',
  `vendor_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '中标供应商名称',
  `winning_bid_count` int DEFAULT NULL COMMENT '中标次数',
  `total_agreement_amount` decimal(20,2) DEFAULT NULL COMMENT '合同总金额（元）',
  `total_agreement_count` decimal(20,2) DEFAULT NULL COMMENT '合同总次数',
  `total_settled_amount` decimal(20,2) DEFAULT NULL COMMENT '已结算金额（元）',
  `total_paid_amount` decimal(20,2) DEFAULT NULL COMMENT '已付款金额（元）',
  `total_unpaid_amount` decimal(20,2) DEFAULT NULL COMMENT '未付款金额（元）',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_id` bigint DEFAULT NULL COMMENT '创建人 id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_id` bigint DEFAULT NULL COMMENT '修改人 id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='中标排行榜';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `undo_log`
--

DROP TABLE IF EXISTS `undo_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `undo_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `branch_id` bigint NOT NULL,
  `xid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `context` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int NOT NULL,
  `log_created` datetime NOT NULL,
  `log_modified` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary table structure for view `v_bid_count`
--

DROP TABLE IF EXISTS `v_bid_count`;
/*!50001 DROP VIEW IF EXISTS `v_bid_count`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE VIEW `v_bid_count` AS SELECT 
 1 AS `min_account_full_name`,
 1 AS `min_account_code`,
 1 AS `prj_state`,
 1 AS `duty_unit`,
 1 AS `management_org_id`,
 1 AS `belonging_org_id`,
 1 AS `cg_num`,
 1 AS `gk_num`,
 1 AS `yq_num`,
 1 AS `xj_num`,
 1 AS `dy_num`,
 1 AS `gk_total_num`,
 1 AS `ngk_total_num`,
 1 AS `n_bid_total_num`,
 1 AS `gk_ratio`*/;
SET character_set_client = @saved_cs_client;

--
-- Dumping routines for database 'hnjiantou-zhaocai-dev'
--

--
-- Final view structure for view `v_bid_count`
--

/*!50001 DROP VIEW IF EXISTS `v_bid_count`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `v_bid_count` AS select `tmp`.`min_account_full_name` AS `min_account_full_name`,`tmp`.`min_account_code` AS `min_account_code`,`tmp`.`prj_state` AS `prj_state`,`tmp`.`duty_unit` AS `duty_unit`,`tmp`.`management_org_id` AS `management_org_id`,`tmp`.`belonging_org_id` AS `belonging_org_id`,`tps`.`cg_num` AS `cg_num`,`tps`.`gk_num` AS `gk_num`,`tps`.`yq_num` AS `yq_num`,`tps`.`xj_num` AS `xj_num`,`tps`.`dy_num` AS `dy_num`,(`tps`.`gk_num` + `tps`.`yq_num`) AS `gk_total_num`,(`tps`.`xj_num` + `tps`.`dy_num`) AS `ngk_total_num`,ifnull(`contract`.`num`,0) AS `n_bid_total_num`,coalesce(round(((`tps`.`gk_num` / nullif(`tps`.`cg_num`,0)) * 100),2),'') AS `gk_ratio` from (((select `tps`.`project_code` AS `project_code`,sum((case when (`tps`.`procurement_type` = '1') then 1 else 0 end)) AS `gk_num`,sum((case when (`tps`.`procurement_type` = '2') then 1 else 0 end)) AS `yq_num`,sum((case when (`tps`.`procurement_type` = '3') then 1 else 0 end)) AS `xj_num`,sum((case when (`tps`.`procurement_type` = '4') then 1 else 0 end)) AS `dy_num`,(((sum((case when (`tps`.`procurement_type` = '1') then 1 else 0 end)) + sum((case when (`tps`.`procurement_type` = '2') then 1 else 0 end))) + sum((case when (`tps`.`procurement_type` = '3') then 1 else 0 end))) + sum((case when (`tps`.`procurement_type` = '4') then 1 else 0 end))) AS `cg_num` from (`tb_procurement_scheme` `tps` left join `tb_tender_notice` `tn` on(((`tn`.`scheme_id` = `tps`.`id`) and (`tn`.`del_flag` = 0)))) where ((`tps`.`del_flag` = 0) and (`tps`.`state` = 3) and (`tn`.`notice_status` = 8)) group by `tps`.`project_code`) `tps` left join `tb_min_project` `tmp` on((`tmp`.`min_account_code` = `tps`.`project_code`))) left join (select `contract_base`.`project_id` AS `project_id`,count(1) AS `num`,'' AS `management_org_id`,'' AS `belonging_org_id` from `contract_base` where ((`contract_base`.`introduce_flag` = 0) and (`contract_base`.`proc_status` = 4) and (`contract_base`.`last_version_flag` = 1)) group by `contract_base`.`project_id`,`management_org_id`,`belonging_org_id`) `contract` on(((`tmp`.`id` = `contract`.`project_id`) and (`tmp`.`management_org_id` = `contract`.`management_org_id`) and (`tmp`.`belonging_org_id` = `contract`.`belonging_org_id`)))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2024-10-12 15:29:04
