-- add integration times for SVC and other instruments having different integration times per detector
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`) values('Integration Time VNIR', (select category_id from `specchio`.category where name = 'Instrument Settings'), 'int_val');
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`) values('Integration Time SWIR1', (select category_id from `specchio`.category where name = 'Instrument Settings'), 'int_val');
INSERT INTO `specchio`.`attribute`(`name`, `category_id`, `default_storage_field`) values('Integration Time SWIR2', (select category_id from `specchio`.category where name = 'Instrument Settings'), 'int_val');


INSERT INTO `specchio`.`measurement_unit` (`name`, `ASD_Coding`) VALUES ('AOT',102);


-- db version
INSERT INTO `specchio`.`schema_info` (`version`, `date`) VALUES ('3.37', CURDATE());

