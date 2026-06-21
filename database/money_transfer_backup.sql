-- MariaDB dump 10.19  Distrib 10.4.32-MariaDB, for Win64 (AMD64)
--
-- Host: localhost    Database: money_transfer_db
-- ------------------------------------------------------
-- Server version	10.4.32-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `accounts`
--

DROP TABLE IF EXISTS `accounts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `accounts` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `holder_name` varchar(100) NOT NULL,
  `email` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `password_hash` varchar(255) NOT NULL,
  `min_balance` decimal(15,2) NOT NULL,
  `balance` decimal(15,2) NOT NULL DEFAULT 0.00,
  `status` varchar(20) NOT NULL DEFAULT 'LOCKED',
  `approved` tinyint(1) NOT NULL DEFAULT 0,
  `role` varchar(20) NOT NULL DEFAULT 'ROLE_USER',
  `account_type` varchar(20) NOT NULL,
  `version` int(11) NOT NULL DEFAULT 0,
  `last_updated` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_email_account_type` (`email`,`account_type`),
  KEY `idx_email` (`email`),
  KEY `idx_status` (`status`),
  KEY `idx_approved` (`approved`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `accounts`
--

LOCK TABLES `accounts` WRITE;
/*!40000 ALTER TABLE `accounts` DISABLE KEYS */;
INSERT INTO `accounts` VALUES (1,'System Administrator','admin@system.com',NULL,NULL,NULL,'$2a$10$vrtzoIuf0vecGR8sgO.cq.T8e99UPlbFU5uacGOFb5kyA7GhnnYb.',0.00,0.00,'ACTIVE',1,'ROLE_ADMIN','ADMIN',0,'2026-06-20 21:59:45','2026-06-20 21:59:45'),(2,'Yuvaraj Karunakaran','user1@email.com','7550099095','Address 1','2004-09-21','$2a$10$r3cfA7ZnMZWGXBaIlxGkaOw13YGwtdIgM1ZyrOp.7wBPq2rpGRsJe',1000.00,92500.00,'ACTIVE',1,'ROLE_USER','CURRENT',6,'2026-06-21 06:47:46','2026-06-20 22:04:36'),(3,'Pranoy P Jyothiraj','user2@email.com','7550005801','Address 2','2004-08-02','$2a$10$/KEdiRyAn9a5VaYpTd0I0OhVpc3J62VZmMTCoUzcqZChuZjAxjz7a',5000.00,22500.00,'ACTIVE',1,'ROLE_USER','SAVINGS',5,'2026-06-21 06:47:46','2026-06-20 22:05:37'),(4,'Yuvaraj Karunakaran','user1@email.com','7550099095','Address 1','2004-09-21','$2a$10$y3ZRrjJ50hB9QcbFHR73DetGTQSX5mLQhrS6.4Sk7WkPt5ssedDcS',10000.00,61000.00,'ACTIVE',1,'ROLE_USER','BUSINESS',3,'2026-06-21 06:46:49','2026-06-20 22:06:14');
/*!40000 ALTER TABLE `accounts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `linked_accounts`
--

DROP TABLE IF EXISTS `linked_accounts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `linked_accounts` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `group_id` varchar(36) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `linked_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_account_link` (`account_id`),
  KEY `idx_group_id` (`group_id`),
  CONSTRAINT `fk_linked_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `linked_accounts`
--

LOCK TABLES `linked_accounts` WRITE;
/*!40000 ALTER TABLE `linked_accounts` DISABLE KEYS */;
INSERT INTO `linked_accounts` VALUES (1,'ddb8fe20-d17d-4ceb-8274-8030339335b9',4,'2026-06-20 22:06:38'),(2,'ddb8fe20-d17d-4ceb-8274-8030339335b9',2,'2026-06-20 22:06:38');
/*!40000 ALTER TABLE `linked_accounts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reward_points`
--

DROP TABLE IF EXISTS `reward_points`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `reward_points` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) NOT NULL,
  `transaction_id` varchar(36) NOT NULL,
  `points` int(11) NOT NULL,
  `revoked` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `revoked_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_reward_account` (`account_id`),
  KEY `idx_reward_transaction` (`transaction_id`),
  CONSTRAINT `fk_reward_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`),
  CONSTRAINT `fk_reward_transaction` FOREIGN KEY (`transaction_id`) REFERENCES `transaction_logs` (`transaction_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reward_points`
--

LOCK TABLES `reward_points` WRITE;
/*!40000 ALTER TABLE `reward_points` DISABLE KEYS */;
INSERT INTO `reward_points` VALUES (1,2,'e05163b0-e629-4d6c-aa1c-03a2fd6e4f73',75,0,'2026-06-21 06:47:05',NULL),(2,2,'4a338a1e-6738-41bb-8a12-dd0f9c9091cb',5,1,'2026-06-21 06:47:18','2026-06-21 06:47:46');
/*!40000 ALTER TABLE `reward_points` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transaction_logs`
--

DROP TABLE IF EXISTS `transaction_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `transaction_logs` (
  `transaction_id` varchar(36) NOT NULL,
  `from_account_id` bigint(20) DEFAULT NULL,
  `to_account_id` bigint(20) DEFAULT NULL,
  `amount` decimal(15,2) NOT NULL,
  `transaction_type` enum('DEBIT','CREDIT','DEPOSIT','TRANSFER','REVERSAL','SELF_TRANSFER') NOT NULL,
  `status` enum('SUCCESS','FAILED','ROLLBACK_REQUESTED','ROLLBACK_REJECTED','ROLLED_BACK') NOT NULL DEFAULT 'SUCCESS',
  `failure_reason` varchar(500) DEFAULT NULL,
  `idempotency_key` varchar(255) DEFAULT NULL,
  `created_on` timestamp NOT NULL DEFAULT current_timestamp(),
  `rollback_requested_at` datetime DEFAULT NULL,
  `rollback_processed_at` datetime DEFAULT NULL,
  `rollback_processed_by` bigint(20) DEFAULT NULL,
  `original_transaction_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`transaction_id`),
  UNIQUE KEY `idempotency_key` (`idempotency_key`),
  KEY `idx_from_account` (`from_account_id`),
  KEY `idx_to_account` (`to_account_id`),
  KEY `idx_idempotency` (`idempotency_key`),
  KEY `idx_created_on` (`created_on`),
  CONSTRAINT `transaction_logs_ibfk_1` FOREIGN KEY (`from_account_id`) REFERENCES `accounts` (`id`) ON DELETE SET NULL,
  CONSTRAINT `transaction_logs_ibfk_2` FOREIGN KEY (`to_account_id`) REFERENCES `accounts` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transaction_logs`
--

LOCK TABLES `transaction_logs` WRITE;
/*!40000 ALTER TABLE `transaction_logs` DISABLE KEYS */;
INSERT INTO `transaction_logs` VALUES ('01e84102-3397-4e48-bf39-400ae3ae197f',2,4,1000.00,'SELF_TRANSFER','SUCCESS',NULL,'a909cb17-7e49-4d4f-a163-41ba934433b2-CREDIT','2026-06-21 06:46:49',NULL,NULL,NULL,NULL),('2e493fc2-86ef-45c4-86d8-6e4d576e15f4',2,3,7500.00,'CREDIT','SUCCESS',NULL,'508eeeab-587e-4d3a-8d8e-5a5b8c6040af-CREDIT','2026-06-21 06:47:05',NULL,NULL,NULL,NULL),('319b907a-4a45-479d-be37-8d1e286a0842',NULL,2,100000.00,'DEPOSIT','SUCCESS',NULL,NULL,'2026-06-20 22:09:08',NULL,NULL,NULL,NULL),('4a338a1e-6738-41bb-8a12-dd0f9c9091cb',2,3,500.00,'DEBIT','ROLLED_BACK',NULL,'89d5f46a-7572-4853-b6ab-ba245bdfcc23-DEBIT','2026-06-21 06:47:18','2026-06-21 12:17:29','2026-06-21 12:17:46',1,NULL),('c027131c-e7fe-4923-906e-c2b3b90c909d',NULL,3,10000.00,'DEPOSIT','SUCCESS',NULL,NULL,'2026-06-20 22:09:16',NULL,NULL,NULL,NULL),('cd4864dc-e87f-4dd0-ac3e-ac49d81bfdf5',3,2,500.00,'REVERSAL','SUCCESS',NULL,'4a338a1e-6738-41bb-8a12-dd0f9c9091cb-REVERSAL','2026-06-21 06:47:46',NULL,NULL,NULL,'4a338a1e-6738-41bb-8a12-dd0f9c9091cb'),('d0d942f8-177a-4e6c-8bc6-a7f0a80a60c8',NULL,4,50000.00,'DEPOSIT','SUCCESS',NULL,NULL,'2026-06-20 22:09:34',NULL,NULL,NULL,NULL),('d9bb7e72-953a-46c6-91aa-77aaf208a455',2,3,500.00,'CREDIT','SUCCESS',NULL,'89d5f46a-7572-4853-b6ab-ba245bdfcc23-CREDIT','2026-06-21 06:47:18',NULL,NULL,NULL,NULL),('e05163b0-e629-4d6c-aa1c-03a2fd6e4f73',2,3,7500.00,'DEBIT','SUCCESS',NULL,'508eeeab-587e-4d3a-8d8e-5a5b8c6040af-DEBIT','2026-06-21 06:47:05',NULL,NULL,NULL,NULL),('ef27e64d-921e-4ba4-855f-1a7a32e0a81a',2,4,1000.00,'SELF_TRANSFER','SUCCESS',NULL,'a909cb17-7e49-4d4f-a163-41ba934433b2-DEBIT','2026-06-21 06:46:49',NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `transaction_logs` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-21 17:51:59
