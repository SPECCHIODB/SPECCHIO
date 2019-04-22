
-- Algorithm attribute for sun angle: this will need refining in future versions for proper provenance info
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `description`, `cardinality`) values('Solar Angle Computation', (select category_id from category where name = 'Processing'), 'string_val', 'Notes produced by the SPECCHIO solar angle computation routine', NULL);



INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Illumination Sources'), 'Dedolight Halogen DLH 4 150W', '', '');

INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `description`, `default_unit_id`) VALUES ('Sampling Area', (select category_id from `specchio`.category where name = 'Sampling Design'), 'double_val', 'Area covered by sampling scheme, e.g. area covered during a sweep.', (select unit_id from unit where short_name like 'm2'));

-- Metadata on hierarchy level and campaign level
CREATE TABLE `hierarchy_x_eav` (
  `hierarchy_level_id` int(11) NOT NULL,
  `eav_id` int(11) NOT NULL,
  `campaign_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`hierarchy_level_id`,`eav_id`),
  KEY `hierarchy_x_eav_fk1` (`hierarchy_level_id`),
  KEY `hierarchy_x_eav_fk2` (`eav_id`),
  KEY `FK_hierarchy_x_eav_campaign_id` (`campaign_id`),
  CONSTRAINT `FK_hierarchy_x_eav_campaign_id` FOREIGN KEY (`campaign_id`) REFERENCES `campaign` (`campaign_id`),
  CONSTRAINT `hierarchy_x_eav_fk1` FOREIGN KEY (`hierarchy_level_id`) REFERENCES `hierarchy_level` (`hierarchy_level_id`) ON UPDATE NO ACTION,
  CONSTRAINT `hierarchy_x_eav_fk2` FOREIGN KEY (`eav_id`) REFERENCES `eav` (`eav_id`) ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE VIEW `specchio`.`hierarchy_x_eav_view` AS
	SELECT `hierarchy_x_eav`.*
	FROM `specchio`.`hierarchy_x_eav`
	WHERE `hierarchy_x_eav`.`campaign_id` IN (
		SELECT `campaign`.`campaign_id`
		FROM `specchio`.`campaign`, `specchio`.`research_group_members`, `specchio`.`specchio_user`
		WHERE
			`campaign`.`research_group_id` = `research_group_members`.`research_group_id`
			AND
			`research_group_members`.`member_id` = `specchio_user`.`user_id`
			AND
			`specchio_user`.`user` = SUBSTRING_INDEX((select user()), '@', 1)
	);

	
CREATE TRIGGER `specchio`.`hierarchy_x_eav_tr`
	BEFORE INSERT ON `specchio`.`hierarchy_x_eav`
	FOR EACH ROW SET new.`campaign_id` = (
		SELECT `campaign_id` FROM `specchio`.`hierarchy_level` WHERE `hierarchy_level`.`hierarchy_level_id` = new.`hierarchy_level_id`
);


CREATE TABLE `campaign_x_eav` (
  `eav_id` int(11) NOT NULL,
  `campaign_id` int(11) NOT NULL,
  PRIMARY KEY (`campaign_id`,`eav_id`),
  KEY `campaign_x_eav_fk2` (`eav_id`),
  KEY `FK_campaign_x_eav_campaign_id` (`campaign_id`),
  CONSTRAINT `FK_campaign_x_eav_campaign_id` FOREIGN KEY (`campaign_id`) REFERENCES `campaign` (`campaign_id`),
  CONSTRAINT `campaign_x_eav_fk2` FOREIGN KEY (`eav_id`) REFERENCES `eav` (`eav_id`) ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE VIEW `specchio`.`campaign_x_eav_view` AS
	SELECT `campaign_x_eav`.*
	FROM `specchio`.`campaign_x_eav`
	WHERE `campaign_x_eav`.`campaign_id` IN (
		SELECT `campaign`.`campaign_id`
		FROM `specchio`.`campaign`, `specchio`.`research_group_members`, `specchio`.`specchio_user`
		WHERE
			`campaign`.`research_group_id` = `research_group_members`.`research_group_id`
			AND
			`research_group_members`.`member_id` = `specchio_user`.`user_id`
			AND
			`specchio_user`.`user` = SUBSTRING_INDEX((select user()), '@', 1)
	);


-- vegetation pigment updates
update attribute set name = 'Chlorophyll A+B', description = 'Combined Chlorophyll A and B Content' where name like 'Chlorophyll Content';

INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `description`, `default_unit_id`) VALUES ('Chlorophyll A', (select category_id from `specchio`.category where name = 'Vegetation Biophysical Variables'), 'double_val', 'Chlorophyll A pigment', (select unit_id from unit where short_name like 'ugrams/cm2'));
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `description`, `default_unit_id`) VALUES ('Chlorophyll B', (select category_id from `specchio`.category where name = 'Vegetation Biophysical Variables'), 'double_val', 'Chlorophyll B pigment', (select unit_id from unit where short_name like 'ugrams/cm2'));

-- correct missing delete from previous spatial upgrade
delete from attribute where name = 'Latitude' OR name = 'Longitude';

-- remove empty file comments
CREATE TEMPORARY TABLE IF NOT EXISTS `specchio_temp`.`temp_spectrum_x_eav_table` AS (select distinct eav_id from spectrum_x_eav where eav_id in (select sxe.eav_id from eav eav, spectrum_x_eav sxe where eav.eav_id = sxe.eav_id and attribute_id = (select attribute_id from attribute where name = 'File Comments') and (string_val is null or CHAR_LENGTH(string_val) = 0)));
delete from spectrum_x_eav where eav_id in (select eav_id from `specchio_temp`.`temp_spectrum_x_eav_table`);

CREATE TEMPORARY TABLE IF NOT EXISTS `specchio_temp`.`temp_eav_table` AS (select eav_id from eav where  eav_id in (select eav_id from eav eav where attribute_id = (select attribute_id from attribute where name = 'File Comments') and (string_val is null or CHAR_LENGTH(string_val) = 0)));
delete from eav where  eav_id in (select eav_id from `specchio_temp`.`temp_eav_table`);



-- db version
INSERT INTO `specchio`.`schema_info` (`version`, `date`) VALUES ('3.32', CURDATE());
