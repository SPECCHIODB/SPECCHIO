
-- linking standard deviations to spectra: intermediate 

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`, `default_storage_field`, `cardinality`) VALUES ('Standard Deviation Data Link', 'Points to a spectrum representing a standard deviation.', (select category_id from `specchio`.`category` where name = 'Data Links'), 'spectrum_id', NULL);


-- new foreign key to mainly support automatic export of campaign path information
ALTER TABLE `specchio`.`campaign_path` ADD CONSTRAINT `campaign_path_campaign_id_fk` FOREIGN KEY `campaign_path_campaign_id_fk` (`campaign_id`) REFERENCES `campaign` (`campaign_id`);
ALTER TABLE `specchio`.`campaign_path` ADD column `campaign_path_id` int(10) unsigned primary KEY AUTO_INCREMENT;


-- new roofing material
CREATE TEMPORARY TABLE IF NOT EXISTS `specchio_temp`.`temp_tax_table` AS (SELECT * FROM `specchio`.`taxonomy`);
insert into `specchio_temp`.`temp_tax_table`  (SELECT * FROM `specchio`.`taxonomy`);
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `description`, `parent_id`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Basic Target Type'), 'Roof (Roofing Cardboard)', 'Tar coated roofing paper', (select taxonomy_id from `specchio_temp`.`temp_tax_table` where attribute_id = (select `attribute_id` from `specchio`.`attribute` where name = 'Basic Target Type') and name like 'Roof' and parent_id is not null));


-- tarps
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `description`, `parent_id`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Basic Target Type'), 'Black Tarp', 'Black reference tarp', (select taxonomy_id from `specchio_temp`.`temp_tax_table` where attribute_id = (select `attribute_id` from `specchio`.`attribute` where name = 'Basic Target Type') and name like 'Reference Material' ));
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `description`, `parent_id`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Basic Target Type'), 'Grey Tarp', 'Grey reference tarp', (select taxonomy_id from `specchio_temp`.`temp_tax_table` where attribute_id = (select `attribute_id` from `specchio`.`attribute` where name = 'Basic Target Type') and name like 'Reference Material' ));
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `description`, `parent_id`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Basic Target Type'), 'White Tarp', 'White reference tarp', (select taxonomy_id from `specchio_temp`.`temp_tax_table` where attribute_id = (select `attribute_id` from `specchio`.`attribute` where name = 'Basic Target Type') and name like 'Reference Material' ));


-- CAL VAL position
INSERT INTO `attribute`(`name`, `category_id`, `default_storage_field`, `description`, `cardinality`) VALUES ('CAL/VAL Spatial Position', (select category_id from category where name = 'Location'), 'spatial_val', 'Spatial location of a spectrum in 2D space as latitude and longitude, optimised for CAL/VAL to correct for image misregistrations', 1);


-- Dropbox support
ALTER TABLE `specchio`.`campaign_path` ADD column `data_source_type` VARCHAR(50) DEFAULT NULL;
ALTER TABLE `specchio`.`campaign_path` ADD column `data_source_details` VARCHAR(100) DEFAULT NULL;

-- 

INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `description`, default_unit_id) values('Time since last WR', (select category_id from `specchio`.category where name = 'Instrument Settings'), 'int_val', 'Time since last white reference measurement', (select unit_id from `specchio`.unit where short_name like 's'));


-- CAL and Characterisation Support
ALTER TABLE category MODIFY string_val VARCHAR(100);
INSERT INTO `specchio`.`category` (`name`, `string_val`) VALUES ('Calibration and Characterisation', 'Settings used for instrument calibration and characterisation');

INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `description`, `cardinality`) VALUES ('Integrating sphere light attenuation', (select category_id from `specchio`.category where name = 'Calibration and Characterisation'), 'double_val', 'Attenuation of light via diaphragms or similar', 1);

INSERT INTO `specchio`.`attribute` (`name`, `category_id`, `default_storage_field`) VALUES ('ASD Spectral Scanning Configuration', (select category_id from `specchio`.`category` where name = 'Calibration and Characterisation'), 'taxonomy_id');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'ASD Spectral Scanning Configuration'), 'A only', '', '');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'ASD Spectral Scanning Configuration'), 'AB even', '', '');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'ASD Spectral Scanning Configuration'), 'A or B', '', '');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'ASD Spectral Scanning Configuration'), 'B only', '', '');

INSERT INTO `specchio`.`attribute` (`name`, `category_id`, `default_storage_field`) VALUES ('Spectral Interpolation', (select category_id from `specchio`.`category` where name = 'Calibration and Characterisation'), 'taxonomy_id');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Spectral Interpolation'), 'ON', '', '');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Spectral Interpolation'), 'OFF', '', '');

INSERT INTO `specchio`.`attribute` (`name`, `category_id`, `default_storage_field`) VALUES ('Spectral Line Lamp', (select category_id from `specchio`.`category` where name = 'Calibration and Characterisation'), 'taxonomy_id');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Spectral Line Lamp'), 'Hg-Ar', 'Mercury Argon Combination', '');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Spectral Line Lamp'), 'Ne', 'Neon', '');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Spectral Line Lamp'), 'Xe', 'Xenon', '');



-- Authentication and Salting upgrades
ALTER TABLE `specchio`.`specchio_user` ADD salt mediumblob;
ALTER TABLE `specchio`.`specchio_user` MODIFY user varchar(255);
ALTER TABLE `specchio`.`specchio_user` MODIFY password mediumblob;


-- Ocean Waste category and new attributes

INSERT INTO `specchio`.`category` (`name`, `string_val`) VALUES ('Ocean Waste', 'Ocean waste specific metadata');

INSERT INTO `specchio`.`attribute` (`name`, `category_id`, `default_storage_field`) VALUES ('Type of Ocean Waste Observation', (select category_id from `specchio`.`category` where name = 'Ocean Waste'), 'taxonomy_id');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Type of Ocean Waste Observation'), 'No litter present', '', '');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Type of Ocean Waste Observation'), 'Single litter item', '', '');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Type of Ocean Waste Observation'), 'Small group (<1 m2)', '', '');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Type of Ocean Waste Observation'), 'Patch (>=1 m2)', '', '');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Type of Ocean Waste Observation'), 'Filament (>= 1m2)', '', '');

INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `description`, default_unit_id) values('Patch area', (select category_id from `specchio`.category where name = 'Ocean Waste'), 'double_val', 'Size of a waste patch in m2', (select unit_id from `specchio`.unit where short_name like 'm2'));


-- db version
INSERT INTO `specchio`.`schema_info` (`version`, `date`) VALUES ('3.35', CURDATE());

