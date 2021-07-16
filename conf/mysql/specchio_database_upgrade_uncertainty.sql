-- Author: kmason
-- Description: script to create tables needed for 'Uncertainty in SPECCHIO' test environment with new SPECCHIO schema v4.01

START TRANSACTION;

-- Creating spectrum_set
CREATE TABLE IF NOT EXISTS `spectrum_set` (
  `spectrum_set_id` int(11) NOT NULL AUTO_INCREMENT,
  `spectrum_set_description` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`spectrum_set_id`)
);

-- Creating uncertainty_edge
CREATE TABLE IF NOT EXISTS `uncertainty_edge` (
  `edge_id` int(10) NOT NULL AUTO_INCREMENT,
  `edge_value` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`edge_id`)
);

-- Creating spectrum_node
CREATE TABLE IF NOT EXISTS `spectrum_node` (
  `spectrum_node_id` int(11) NOT NULL AUTO_INCREMENT,
  `node_type` varchar(100) DEFAULT NULL,
  `u_vector` blob DEFAULT NULL,
  `confidence_level` double(3,2),
  `abs_rel` varchar(3),
  `unit_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`spectrum_node_id`),
  KEY `unit_id` (`unit_id`),
  CONSTRAINT `spectrum_node_ibfk_1` FOREIGN KEY (`unit_id`) REFERENCES `unit` (`unit_id`)
);

-- Creating spectrum_subset
-- Does not include foreign key constraint on spectrum_id to spectrum table
CREATE TABLE IF NOT EXISTS `spectrum_subset` (
  `spectrum_subset_id` int(11) NOT NULL AUTO_INCREMENT,
  `spectrum_node_id` int(11) DEFAULT NULL,
  `spectrum_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`spectrum_subset_id`),
  KEY `spectrum_node_id` (`spectrum_node_id`),
  CONSTRAINT `spectrum_subset_ibfk_1` FOREIGN KEY (`spectrum_node_id`) REFERENCES `spectrum_node` (`spectrum_node_id`)
);

-- Creating spectrum_set_map
CREATE TABLE IF NOT EXISTS `spectrum_set_map` (
  `spectrum_set_id` int(11) NOT NULL,
  `spectrum_subset_id` int(11) NOT NULL,
  PRIMARY KEY (`spectrum_set_id`,`spectrum_subset_id`),
  KEY `spectrum_subset_id` (`spectrum_subset_id`),
  KEY `spectrum_set_id` (`spectrum_set_id`),
  CONSTRAINT `spectrum_set_map_ibfk_1` FOREIGN KEY (`spectrum_set_id`) REFERENCES `spectrum_set` (`spectrum_set_id`),
  CONSTRAINT `spectrum_set_map_ibfk_2` FOREIGN KEY (`spectrum_subset_id`) REFERENCES `spectrum_subset` (`spectrum_subset_id`)
);

-- Creating instrument_node
CREATE TABLE IF NOT EXISTS `instrument_node` (
  `instrument_node_id` int(11) NOT NULL AUTO_INCREMENT,
  `node_type` varchar(100) DEFAULT NULL,
  `u_vector` blob DEFAULT NULL,
  `confidence_level` double(3,2),
  `abs_rel` varchar(3),
  `unit_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`instrument_node_id`),
  KEY `unit_id` (`unit_id`),
  CONSTRAINT `instrument_node_ibfk_1` FOREIGN KEY (`unit_id`) REFERENCES `unit` (`unit_id`)
);

-- Creating uncertainty_node
CREATE TABLE IF NOT EXISTS `uncertainty_node` (
  `node_id` int(11) NOT NULL AUTO_INCREMENT,
  `is_spectrum` tinyint(1) NOT NULL,
  `instrument_node_id` int(11) DEFAULT NULL,
  `spectrum_set_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`node_id`),
  KEY `instrument_node_id` (`instrument_node_id`),
  KEY `spectrum_set_id` (`spectrum_set_id`),
  CONSTRAINT `uncertainty_node_ibfk_1` FOREIGN KEY (`instrument_node_id`) REFERENCES `instrument_node` (`instrument_node_id`),
  CONSTRAINT `uncertainty_node_ibfk_2` FOREIGN KEY (`spectrum_set_id`) REFERENCES `spectrum_set` (`spectrum_set_id`)
);

-- Creating uncertainty_node_set
CREATE TABLE IF NOT EXISTS `uncertainty_node_set` (
  `node_set_id` int(11) NOT NULL,
  `node_num` int(11),
  `node_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`node_set_id`,`node_num`),
  KEY `node_id` (`node_id`),
  CONSTRAINT `uncertainty_node_set_ibfk_1` FOREIGN KEY (`node_id`) REFERENCES `uncertainty_node` (`node_id`)
);

-- Creating uncertainty_set
CREATE TABLE IF NOT EXISTS `uncertainty_set` (
  `uncertainty_set_id` int(11) NOT NULL AUTO_INCREMENT,
  `adjacency_matrix` blob DEFAULT NULL,
  `node_set_id` int(11) DEFAULT NULL,
  `uncertainty_set_description` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`uncertainty_set_id`),
  KEY `node_set_id` (`node_set_id`)
);

INSERT INTO `specchio`.`schema_info` (`version`, `date`) VALUES ('3.36', CURDATE());

-- Still to add: links to spectrum and calibration tables

COMMIT;


