-- SQL UPDATEs for GA Specchio build

-- Adding metadata fields to suit the Aquatic substrate library, "Aquatic Ecosystem Biophysical variables" category added
-- Adding metadata fields to suit RPAS (drone) acquisitions
-- Updating existing metadata categories with new fields, altering current fields for wider application


-- Environmental Conditions additions:

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Density of Cover', '% or decimal 
coverage of plant matter', (select category_id from `specchio`.
`category` WHERE name = 'Environmental Conditions'), 'double_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Sampling Notes', 'Relating to field conditions on the day of data collection, 
 any interruptions, causes of human error or instrument failure etc', (select category_id from `specchio`.
`category` WHERE name = 'Environmental Conditions'), 'string_val');


-- Taxonomy inserts:

INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, 
`description`) VALUES ((select `attribute_id` from `specchio`.`attribute` 
WHERE name = 'Water Type'), 'Wetland', '', '');

INSERT INTO `specchio`.`taxonomy` (`attribute_id`, `name`, `code`, 
`description`) VALUES ((select `attribute_id` from `specchio`.`attribute` 
WHERE name = 'Water Type'), 'Coastal Floodplain', '', '');

-- Sampling Geometry additions:
INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Height AGL', 'Height above ground level
 (ft.) of measurement', (select category_id from `specchio`.
`category` WHERE name = 'Sampling Geometry'), 'double_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Acquisition Time(str)', '', 
(select category_id from `specchio`.
`category` WHERE name = 'General'), 'string_val');


-- adding a new category - Aquatic Ecosystem Biophysical Variables:

INSERT INTO `specchio`.`category` (`category_id`, `name`, `string_val`) VALUES ('27', 
'Aquatic Ecosystem Biophysical Variables', '');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Water Flow Status', 'e.g. Ebb, Neap, King, High, Rate of flow: In or out; Downstream  etc', 
(select category_id from `specchio`.
`category` WHERE name = 'Aquatic Ecosystem Biophysical Variables'), 'string_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Water Transparency (Secchi)', 'Positive integers', 
(select category_id from `specchio`.
`category` WHERE name = 'Aquatic Ecosystem Biophysical Variables'), 'double_val');
(done)

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Water Samples Taken', '', 
(select category_id from `specchio`.
`category` WHERE name = 'Aquatic Ecosystem Biophysical Variables'), 'int_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Depositional Environment', 'e.g. Intertidal, Supratidal, Subtidal, Foredune, Backdune, Nearshore, Offshore, Reef flat, Reef lagoon, Reef crest, Reef slope etc.', 
(select category_id from `specchio`.
`category` WHERE name = 'Aquatic Ecosystem Biophysical Variables'), 'string_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Water Condition', '', 
(select category_id from `specchio`.
`category` WHERE name = 'Aquatic Ecosystem Biophysical Variables'), 'string_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Water Colour', '', 
(select category_id from `specchio`.
`category` WHERE name = 'Aquatic Ecosystem Biophysical Variables'), 'string_val');

UPDATE `specchio`.`attribute` SET default_storage_field = 'string_val' WHERE name = 
'Sampling Environment';

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Water Type', '', 
(select category_id from `specchio`.
`category` WHERE name = 'Aquatic Ecosystem Biophysical Variables'), 'string_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Water Temperature', 'Water temperature in degrees Celcius', 
(select category_id from `specchio`.
`category` WHERE name = 'Aquatic Ecosystem Biophysical Variables'), 'double_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Underwater Housing Type', '', 
(select category_id from `specchio`.
`category` WHERE name = 'Instrument'), 'string_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
`default_storage_field`) VALUES ('Spectrum Suitability', 'Suitable for 
Validation, Parametrisation, Intraspecies variability, Interspecies variability,
Substratum type variability.', 
(select category_id from `specchio`.
`category` WHERE name = 'Keywords'), 'string_val');


-- add descriptions:

UPDATE `specchio`.`attribute` SET description = 'Suitable for 
Validation, Parametrisation, Intraspecies variability, Interspecies variability,
Substratum type variability.' WHERE name = 'Spectrum Suitability';


-- name UPDATEs

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('WORMS AlphaID', 'Latin or scientific name', 
(select category_id from `specchio`.
`category` WHERE name = 'Names'), 'string_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('CAAB code', 'Latin or scientific name', 
(select category_id from `specchio`.
`category` WHERE name = 'Names'), 'string_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('CATAMI code', 'Latin or scientific name', 
(select category_id from `specchio`.
`category` WHERE name = 'Names'), 'string_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Epithetic/Epibiont cover and type', 'Latin or scientific name', 
(select category_id from `specchio`.
`category` WHERE name = 'Names'), 'string_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Processing Algorithm', 'Includes smoothing method.', 
(select category_id from `specchio`.
`category` WHERE name = 'Processing'), 'string_val');


-- adding missing descriptions to new attributes

UPDATE `specchio`.`attribute` SET description = 'River, Lake, Farm Dam, Flood Plain, Reservoir, Delta, Estuary, Beach, 
Rock Platform; Intertidal, Coastal, Coral etc.' WHERE name = 'Water Type';

UPDATE `specchio`.`attribute` SET description = 'e.g Wave Height, Foam, Surface slicks, Surface scums etc'
WHERE name = 'Water Condition';

UPDATE `specchio`.`attribute` SET description = 'Blue, Blue-Green, Green, Green-Yellow, Yellow-Orange, Orange-Brown, Brown, etc.
' WHERE name = 'Water Colour';

UPDATE `specchio`.`attribute` SET description = 'same as solar angle, angle between celestial body / illumination source and North' WHERE name = 'Illumination Azimuth';

UPDATE `specchio`.`attribute` SET description = '(seconds)' WHERE name = 'Integration Time';
UPDATE `specchio`.`attribute` SET default_storage_field = 'double_val' WHERE name = 'Integration Time';

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Shading Target and/or Panel', 'To avoid wave induced glint or surface glint (in field or lab)', 
(select category_id from `specchio`.
`category` WHERE name = 'Instrumentation'), 'string_val');


-- add cal info to PDFs

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Calibration Information', '', 
(select category_id from `specchio`.
`category` WHERE name = 'PDFs'), 'binary_val');


-- add drone metadata - new category = "RPA variables", description = 'RPA / drone metadata'

INSERT INTO `specchio`.`category`(`category_id`, `name`, `string_val`) values ('35', 'RPA Variables', 'RPA / drone metadata');


-- add metadata fields

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Speed', 'RPA speed at time of measurement', 
(select category_id from `specchio`.
`category` WHERE name = 'RPA Variables'), 'double_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,`default_storage_field`) 
VALUES ('Distance', 'RPA distance from home location at time of measurement', 
(select category_id from `specchio`.`category` WHERE name = 'RPA Variables'), 'double_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,`default_storage_field`) 
VALUES ('Compass Heading', 'RPA heading at time of measurement', 
(select category_id from `specchio`.`category` WHERE name = 'RPA Variables'), 'double_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,`default_storage_field`) 
VALUES ('Pitch', 'RPA pitch at time of measurement', 
(select category_id from `specchio`.`category` WHERE name = 'RPA Variables'), 'double_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,`default_storage_field`) 
VALUES ('Roll', 'RPA roll at time of measurement', 
(select category_id from `specchio`.`category` WHERE name = 'RPA Variables'), 'double_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,`default_storage_field`) 
VALUES ('Gimbal Heading', 'RPA gimbal heading at time of measurement', 
(select category_id from `specchio`.`category` WHERE name = 'RPA Variables'), 'double_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,`default_storage_field`) 
VALUES ('Gimbal Pitch', 'RPA gimbal pitch at time of measurement, -90 degrees is approximately NADIR / stright down', 
(select category_id from `specchio`.`category` WHERE name = 'RPA Variables'), 'double_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,`default_storage_field`) 
VALUES ('Panel Serial Number', 'Reference Panel Serial Number', 
(select category_id from `specchio`.`category` WHERE name = 'Instrumentation'), 'string_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,`default_storage_field`) 
VALUES ('Link to Workflow', 'Provides extra information on the process to generate reflectance from raw data', 
(select category_id from `specchio`.`category` WHERE name = 'Data Links'), 'string_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,`default_storage_field`) 
VALUES ('Yaw', 'RPA yaw at time of measurement', 
(select category_id from `specchio`.`category` WHERE name = 'RPA Variables'), 'double_val');


-- UPDATE processing level descriptions

UPDATE `specchio`.`attribute` SET description = '0.0 DN: Digital numbers; 
1.0 Radiance Spectra, as provided by the instrument; 
1.x Radiance Spectra, corrected; 
2.0 Reflectance Spectra, as provided by the instrument; 
2.x Reflectance Spectra, corrected' WHERE name = 'processing level';


-- further inserts / additions to metadata fields

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Data Provider', 'Name and contact email of the provider of the dataSET', 
(select category_id from `specchio`.
`category` WHERE name = 'Personnel'), 'string_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Staff', 'Names of staff involved in the project', 
(select category_id from `specchio`.
`category` WHERE name = 'Personnel'), 'string_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Aquatic Measurement Unit', 'e.g: Irradiance Reflectance (Ed/Eu), Radiance Reflectance (Lu/Ld), Remote Sensing Reflectance ( Ed/Lu); Downwelling Radiance (Ld), Downwelling Irradiance (Ed), Upwelling Radiance (Lu), Upwelling Irradiance (Eu).', 
(select category_id from `specchio`.
`category` WHERE name = 'General'), 'string_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Study Description', 'Project-level description', 
(select category_id from `specchio`.
`category` WHERE name = 'Campaign Details'), 'string_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Extended Instrument Name', 'Instrument descriptor or name', 
(select category_id from `specchio`.
`category` WHERE name = 'Instrument'), 'string_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('State/Territory', 'State or Territory WHERE the measurement was taken', 
(select category_id from `specchio`.
`category` WHERE name = 'Location'), 'string_val');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`, `default_unit_id`) VALUES ('Altitude', 'Height (metres) above mean sea level', 
(select category_id from `specchio`.
`category` WHERE name = 'Location'), 'double_val', '15');

INSERT INTO `specchio`.`attribute`(`name`, `description`, `category_id`,
 `default_storage_field`) VALUES ('Illumination Sources', 'Alternate field for Illumination Source, allowing for the entry of strng values', (select category_id from `specchio`.
`category` WHERE name = 'Instrumentation'), 'string_val');

-- UPDATE "density of cover" field -> string val, description.
UPDATE `specchio`.`attribute` SET default_storage_field = 'string_val' WHERE attribute_id = '434';
UPDATE `specchio`.`attribute` SET description = 'Density of cover in decimal % i.e (0.45 = 45%), 
percentage, or descriptor (low, med, high etc.)' WHERE attribute_id = '434';

-- UPDATE "wind speed" field -> string val, description.
UPDATE `specchio`.`attribute` SET default_storage_field = 'string_val' WHERE attribute_id = '3';
UPDATE `specchio`.`attribute` SET description = 'Wind speed, either in m/s or as a description (low, med, high etc.)' 
WHERE attribute_id = '3';

-- UPDATE "Number of internal Scans" to "Number of Internal Scans"
UPDATE `specchio`.`attribute` SET name = 'Number of Internal Scans' WHERE name = 'Number of internal Scans';

-- ####################################################
-- Metadata additions summary
-- (Category - Field added. *Indicates new category)

-- *Aquatic Ecosystem Biophysical Variables - Depositional Environment, Water Colour, Water Condition, Water Flow Status, Water Samples Taken, Water Temperature, Water Transparency (Secchi), Water Type
-- Campaign Details - Study Description
-- Data Links - Link to Workflow
-- Instrument - Underwater Housing Type
-- Instrumentation - Panel Serial Number
-- Keywords - Spectrum Suitability
-- Location - State/Territory, Altitude
-- PDFs - Calibration Information
-- Personnel - Staff, Data Provider
-- *RPA Variables - Compass Heading, Distance, Gimbal Heading, Gimbal Pitch, Pitch, Roll, Yaw, Speed
-- Sampling Geometry - Height AGL

