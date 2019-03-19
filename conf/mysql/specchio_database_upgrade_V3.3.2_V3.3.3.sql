

INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Vegetation Sampling Scale'), 'Bark', '', 'Measurements at single leaf scale');


INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`, `default_storage_field`) VALUES ('Vegetation Stratification', '', (select category_id from `specchio`.`category` where name = 'Sampling Design'), 'taxonomy_id');

INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Vegetation Stratification'), 'Tree Layer', '', '');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Vegetation Stratification'), 'Shrub Layer', '', '');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Vegetation Stratification'), 'Herbaceous Layer', '', '');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Vegetation Stratification'), 'Forest Floor', '', '');

DROP TABLE IF EXISTS `specchio_temp`.`temp_tax_table`;
CREATE TEMPORARY TABLE IF NOT EXISTS `specchio_temp`.`temp_tax_table` AS (SELECT * FROM `specchio`.`taxonomy`);

INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `description`, `parent_id`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Vegetation Stratification'), 'Canopy', '', (select taxonomy_id from `specchio_temp`.`temp_tax_table` where attribute_id = (select `attribute_id` from `specchio`.`attribute` where name = 'Vegetation Stratification') and name like 'Tree Layer'));
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `description`, `parent_id`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Vegetation Stratification'), 'Understory', '', (select taxonomy_id from `specchio_temp`.`temp_tax_table` where attribute_id = (select `attribute_id` from `specchio`.`attribute` where name = 'Vegetation Stratification') and name like 'Tree Layer'));
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `description`, `parent_id`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Vegetation Stratification'), 'Moss Layer', '', (select taxonomy_id from `specchio_temp`.`temp_tax_table` where attribute_id = (select `attribute_id` from `specchio`.`attribute` where name = 'Vegetation Stratification') and name like 'Forest Floor'));
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `description`, `parent_id`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Vegetation Stratification'), 'Root Layer', '', (select taxonomy_id from `specchio_temp`.`temp_tax_table` where attribute_id = (select `attribute_id` from `specchio`.`attribute` where name = 'Vegetation Stratification') and name like 'Forest Floor'));


-- Leaf clips
INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`, `default_storage_field`) VALUES ('Leaf Clip', '', (select category_id from `specchio`.`category` where name = 'Instrumentation'), 'taxonomy_id');

INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Leaf Clip'), 'ASD Leaf Clip', '', 'Analytical Spectral Devices Leaf Clip');

-- Soil state (in field)
INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`, `default_storage_field`) VALUES ('Tillage State', '', (select category_id from `specchio`.`category` where name = 'Soil Parameters'), 'taxonomy_id');

INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Tillage State'), 'Ploughed', '', '');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Tillage State'), 'Harrowed', '', '');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Tillage State'), 'Unmanaged', '', '');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Tillage State'), 'Sowed', '', '');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Tillage State'), 'Rolled', '', '');


-- change 'Ploughed Field' in Basic Target Type to 'Bare Field'
update taxonomy set name = 'Bare Field' where name = 'Ploughed Field';

-- Soil parameters

INSERT INTO `specchio`.`attribute` (`name`, `category_id`, `default_storage_field`, `default_unit_id`) VALUES ('Organic Matter', (select category_id from `specchio`.`category` where name = 'Soil Parameters'), 'double_val', (select unit_id from `specchio`.`unit` where short_name = '%'));
INSERT INTO `specchio`.`attribute` (`name`, `category_id`, `default_storage_field`, `default_unit_id`, `description`) VALUES ('Volumetric Water Content', (select category_id from `specchio`.`category` where name = 'Soil Parameters'), 'double_val', (select unit_id from `specchio`.`unit` where short_name = '%'), '= volume of water / volume of total wet material');
INSERT INTO `specchio`.`attribute` (`name`, `category_id`, `default_storage_field`, `default_unit_id`, `description`) VALUES ('Gravimetric Water Content', (select category_id from `specchio`.`category` where name = 'Soil Parameters'), 'double_val', (select unit_id from `specchio`.`unit` where short_name = '%'), '= mass of water / mass of total wet material');


INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`, `default_storage_field`) VALUES ('Soil Textural Class', '', (select category_id from `specchio`.`category` where name = 'Soil Parameters'), 'taxonomy_id');

INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Soil Textural Class'), 'Sand', '', 'Based on FAO textural class (http://www.fao.org/fishery/static/FAO_Training/FAO_Training/General/x6706e/x6706e06.htm)');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Soil Textural Class'), 'Loamy Sand', '', 'Based on FAO textural class (http://www.fao.org/fishery/static/FAO_Training/FAO_Training/General/x6706e/x6706e06.htm)');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Soil Textural Class'), 'Sandy Loam', '', 'Based on FAO textural class (http://www.fao.org/fishery/static/FAO_Training/FAO_Training/General/x6706e/x6706e06.htm)');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Soil Textural Class'), 'Loam', '', 'Based on FAO textural class (http://www.fao.org/fishery/static/FAO_Training/FAO_Training/General/x6706e/x6706e06.htm)');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Soil Textural Class'), 'Silty Loam', '', 'Based on FAO textural class (http://www.fao.org/fishery/static/FAO_Training/FAO_Training/General/x6706e/x6706e06.htm)');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Soil Textural Class'), 'Silt', '', 'Based on FAO textural class (http://www.fao.org/fishery/static/FAO_Training/FAO_Training/General/x6706e/x6706e06.htm)');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Soil Textural Class'), 'Clay Loam', '', 'Based on FAO textural class (http://www.fao.org/fishery/static/FAO_Training/FAO_Training/General/x6706e/x6706e06.htm)');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Soil Textural Class'), 'Sandy Clay Loam', '', 'Based on FAO textural class (http://www.fao.org/fishery/static/FAO_Training/FAO_Training/General/x6706e/x6706e06.htm)');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Soil Textural Class'), 'Silty Clay Loam', '', 'Based on FAO textural class (http://www.fao.org/fishery/static/FAO_Training/FAO_Training/General/x6706e/x6706e06.htm)');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Soil Textural Class'), 'Sandy Clay', '', 'Based on FAO textural class (http://www.fao.org/fishery/static/FAO_Training/FAO_Training/General/x6706e/x6706e06.htm)');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Soil Textural Class'), 'Silty Clay', '', 'Based on FAO textural class (http://www.fao.org/fishery/static/FAO_Training/FAO_Training/General/x6706e/x6706e06.htm)');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Soil Textural Class'), 'Clay', '', 'Based on FAO textural class (http://www.fao.org/fishery/static/FAO_Training/FAO_Training/General/x6706e/x6706e06.htm)');

INSERT INTO `specchio`.`attribute` (`name`, `category_id`, `default_storage_field`) VALUES ('Munsell Hue', (select category_id from `specchio`.`category` where name = 'Soil Parameters'), 'string_val');


INSERT INTO `specchio`.`attribute` (`name`, `category_id`, `default_storage_field`, `description`) VALUES ('Surface Roughness Method', (select category_id from `specchio`.`category` where name = 'Generic Target Properties'), 'taxonomy_id', 'https://doi.org/10.1016/j.catena.2005.08.005, doi:10.5194/soil-1-399-2015');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Surface Roughness Method'), 'Roller Chain', '', '');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Surface Roughness Method'), 'Pin Meter', '', '');



INSERT INTO `specchio`.`attribute` (`name`, `category_id`, `default_storage_field`, `description`) VALUES ('Surface Saleh Roughness Factor', (select category_id from `specchio`.`category` where name = 'Generic Target Properties'), 'double_val', 'Equivalent to Chain Roughness Index (https://doi.org/10.1016/j.catena.2005.08.005)');
INSERT INTO `specchio`.`attribute` (`name`, `category_id`, `default_storage_field`, `default_unit_id`, `description`) VALUES ('Surface Random Roughness Factor', (select category_id from `specchio`.`category` where name = 'Generic Target Properties'), 'double_val', (select unit_id from `specchio`.`unit` where short_name = 'mm'), 'Defined as standard deviation of elevations (https://doi.org/10.1016/j.catena.2005.08.005)');

INSERT INTO `specchio`.`attribute` (`name`, `category_id`, `default_storage_field`) VALUES ('Surface Moisture Description', (select category_id from `specchio`.`category` where name = 'Generic Target Properties'), 'taxonomy_id');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Surface Moisture Description'), 'Dry', '', '');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Surface Moisture Description'), 'Moist', '', '');
INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, `description`) VALUES ((select `attribute_id` from `specchio`.`attribute` where name = 'Surface Moisture Description'), 'Wet', '', '');


INSERT INTO `specchio`.`attribute` (`name`, `category_id`, `default_storage_field`) VALUES ('Water Content Method', (select category_id from `specchio`.`category` where name = 'Soil Parameters'), 'string_val');
INSERT INTO `specchio`.`attribute` (`name`, `category_id`, `default_storage_field`, `default_unit_id`) VALUES ('Water Content', (select category_id from `specchio`.`category` where name = 'Soil Parameters'), 'double_val', (select unit_id from `specchio`.`unit` where name like 'Percent'));

-- update soil attribute units
update attribute set default_unit_id  = (select unit_id from unit where name like 'Percent') where name like 'Clay';
update attribute set default_unit_id  = (select unit_id from unit where name like 'Percent') where name like 'Fine Sand';
update attribute set default_unit_id  = (select unit_id from unit where name like 'Percent') where name like 'Silt';

-- instrument settings
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `description`, `default_unit_id`) VALUES ('Spectrometer Frame Temperature', (select category_id from `specchio`.category where name = 'Instrument Settings'), 'double_val', 'Temperature of the structure holding the actual spectrometer', (select unit_id from `specchio`.`unit` where short_name = 'Degrees C'));
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `description`, `default_unit_id`) VALUES ('Detector Temperature', (select category_id from `specchio`.category where name = 'Instrument Settings'), 'double_val', 'Temperature of the opto-electronic chip', (select unit_id from `specchio`.`unit` where short_name = 'Degrees C'));
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `description`, `default_unit_id`) VALUES ('PCB Temperature', (select category_id from `specchio`.category where name = 'Instrument Settings'), 'double_val', 'Temperature of the printed circuit board, i.e. electronics board', (select unit_id from `specchio`.`unit` where short_name = 'Degrees C'));
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `description`, `default_unit_id`) VALUES ('Optical Compartment Temperature', (select category_id from `specchio`.category where name = 'Instrument Settings'), 'double_val', 'Temperature of the chamber containing the spectrometer(s)', (select unit_id from `specchio`.`unit` where short_name = 'Degrees C'));
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `description`, `default_unit_id`) VALUES ('PCB Humidity', (select category_id from `specchio`.category where name = 'Instrument Settings'), 'double_val', 'Relative humidity of air next to the printed circuit board, i.e. electronics board', (select unit_id from `specchio`.`unit` where name like 'Percent'));
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `description`, `default_unit_id`) VALUES ('Optical Compartment Humidity', (select category_id from `specchio`.category where name = 'Instrument Settings'), 'double_val', 'Relative humidity of the chamber containing the spectrometer(s)', (select unit_id from `specchio`.`unit` where name like 'Percent'));

-- calibration table update: add type of calibration. Coding is done in Java: 0=spectral, 1=radiometric
ALTER TABLE `specchio`.`calibration` ADD COLUMN `calibration_type` INT(10);

update `specchio`.calibration set calibration_type = 0 where comments like '%Wavelength%';
update `specchio`.calibration set calibration_type = 1 where comments not like '%Wavelength%';

insert into `specchio`.measurement_unit (name, ASD_coding) values ('DN/Radiance', 101);

-- FLoX related metadata
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `description`) VALUES ('Saturation Count', (select category_id from `specchio`.category where name = 'Processing'), 'int_val', 'Count of saturated pixels');
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`, `description`, `default_unit_id`) VALUES ('Irradiance Instability', (select category_id from `specchio`.category where name = 'Illumination'), 'double_val', 'Illumination condition stability indicated as percent change in irradiance', (select unit_id from `specchio`.`unit` where name like 'Percent'));



-- db version
INSERT INTO `specchio`.`schema_info` (`version`, `date`) VALUES ('3.33', CURDATE());

