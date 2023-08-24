
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Illumination Sources'), 'RASTA', '', 'DLR Traceable Radiometric Standard');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Illumination Sources'), 'Heliosphere', '', 'Integrating sphere by Labsphere');

-- upgrade towards storage of vectors as UJMP
ALTER TABLE `specchio`.`spectrum` ADD COLUMN `storage_format` INT UNSIGNED NULL DEFAULT 0 AFTER `calibration_id`;
ALTER TABLE `specchio`.`spectrum` MODIFY COLUMN `measurement` LONGBLOB;

USE `specchio`;
CREATE
OR REPLACE ALGORITHM = UNDEFINED
    DEFINER = `root`@`localhost`
    SQL SECURITY DEFINER
VIEW `spectrum_view` AS
select
    `spectrum`.`spectrum_id` AS `spectrum_id`,
    `spectrum`.`measurement_unit_id` AS `measurement_unit_id`,
    `spectrum`.`measurement` AS `measurement`,
    `spectrum`.`hierarchy_level_id` AS `hierarchy_level_id`,
    `spectrum`.`sensor_id` AS `sensor_id`,
    `spectrum`.`file_format_id` AS `file_format_id`,
    `spectrum`.`campaign_id` AS `campaign_id`,
    `spectrum`.`instrument_id` AS `instrument_id`,
    `spectrum`.`reference_id` AS `reference_id`,
    `spectrum`.`calibration_id` AS `calibration_id`,
    `spectrum`.`storage_format` AS `storage_format`
from
    `spectrum`
where
        `spectrum`.`campaign_id` in (select
                                         `campaign`.`campaign_id`
                                     from
                                         ((`campaign`
                                             join `research_group_members`)
                                             join `specchio_user`)
                                     where
                                         ((`campaign`.`research_group_id` = `research_group_members`.`research_group_id`)
                                             and (`research_group_members`.`member_id` = `specchio_user`.`user_id`)
                                             and (`specchio_user`.`user` = substring_index((select user()), '@', 1))));

-- calibration metadata support via EAV
CREATE TABLE `specchio`.`calibration_x_eav` (
                                                `eav_id` int(11) NOT NULL,
                                                `calibration_id` int(10) unsigned NOT NULL,
                                                PRIMARY KEY (`calibration_id`,`eav_id`),
                                                KEY `calibration_x_eav_fk` (`eav_id`),
                                                KEY `FK_calibration_x_eav_calibration_id` (`calibration_id`),
                                                CONSTRAINT `FK_calibration_x_eav_calibration_id` FOREIGN KEY (`calibration_id`) REFERENCES `calibration` (`calibration_id`),
                                                CONSTRAINT `calibration_x_eav_fk` FOREIGN KEY (`eav_id`) REFERENCES `eav` (`eav_id`) ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



-- db version
INSERT INTO `specchio`.`schema_info` (`version`, `date`) VALUES ('3.36', CURDATE());

