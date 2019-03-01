-- Irradiance related metadata
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `description`, `default_unit_id`) VALUES ('Irradiance Stability (CV)', (select category_id from `specchio`.category where name = 'Illumination'), 'double_val', 'Illumination condition stability indicated as coefficient of variation in irradiance: median(std/mean)', (select unit_id from `specchio`.`unit` where name like 'Percent'));

-- leaf clip measurement designators for spectrum tagging
INSERT INTO `specchio`.`attribute` (`name`, `category_id`, `default_storage_field`) VALUES ('Leafclip Measurement Designator', (select category_id from `specchio`.`category` where name = 'Generic Target Properties'), 'taxonomy_id');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Leafclip Measurement Designator'), 'Black', '', 'Black reference');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Leafclip Measurement Designator'), 'Leaf-Black', '', 'Leaf and Black reference');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Leafclip Measurement Designator'), 'Leaf-White', '', 'Leaf and White reference');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Leafclip Measurement Designator'), 'White', '', 'White reference');

-- spectral indices support
INSERT INTO `specchio`.`category` (`name`, `string_val`) VALUES ('Spectral Indices', 'Indices calculated from the spectral data');
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `default_unit_id`, `description`, `cardinality`) VALUES ('SR', (select category_id from `specchio`.category where name = 'Spectral Indices'), 'double_val', (select unit_id from `specchio`.unit where name = 'RAW'),'', null);
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `default_unit_id`, `description`, `cardinality`) VALUES ('NDVI', (select category_id from `specchio`.category where name = 'Spectral Indices'), 'double_val', (select unit_id from `specchio`.unit where name = 'RAW'),'', null);
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `default_unit_id`, `description`, `cardinality`) VALUES ('NDVIre', (select category_id from `specchio`.category where name = 'Spectral Indices'), 'double_val', (select unit_id from `specchio`.unit where name = 'RAW'),'', null);
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `default_unit_id`, `description`, `cardinality`) VALUES ('EVI', (select category_id from `specchio`.category where name = 'Spectral Indices'), 'double_val', (select unit_id from `specchio`.unit where name = 'RAW'),'', null);
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `default_unit_id`, `description`, `cardinality`) VALUES ('REP', (select category_id from `specchio`.category where name = 'Spectral Indices'), 'double_val', (select unit_id from `specchio`.unit where name = 'RAW'),'', null);
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `default_unit_id`, `description`, `cardinality`) VALUES ('MTCI', (select category_id from `specchio`.category where name = 'Spectral Indices'), 'double_val', (select unit_id from `specchio`.unit where name = 'RAW'),'', null);
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `default_unit_id`, `description`, `cardinality`) VALUES ('TCARI', (select category_id from `specchio`.category where name = 'Spectral Indices'), 'double_val', (select unit_id from `specchio`.unit where name = 'RAW'),'', null);
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `default_unit_id`, `description`, `cardinality`) VALUES ('PRI', (select category_id from `specchio`.category where name = 'Spectral Indices'), 'double_val', (select unit_id from `specchio`.unit where name = 'RAW'),'', null);
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `default_unit_id`, `description`, `cardinality`) VALUES ('cPRI', (select category_id from `specchio`.category where name = 'Spectral Indices'), 'double_val', (select unit_id from `specchio`.unit where name = 'RAW'),'', null);
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `default_unit_id`, `description`, `cardinality`) VALUES ('WBI', (select category_id from `specchio`.category where name = 'Spectral Indices'), 'double_val', (select unit_id from `specchio`.unit where name = 'RAW'),'', null);

-- db version
INSERT INTO `specchio`.`schema_info` (`version`, `date`) VALUES ('3.34', CURDATE());

