package ch.specchio.eav_db;

/**
 * Definitions of table and view names.
 */
public class TableNames {
	
	/** all table names */
	public static final String[] TABLES = new String[] {
		"campaign",
		"campaign_path",
		"hierarchy_level",
		"spectrum",
		"specchio_user",
		"specchio_user_group",
		"institute",
		"file_format",
		"measurement_unit",
		"sensor",
		"sensor_element",
		"sensor_element_type",
		"instrument",
		"calibration",
		"manufacturer",
		"instrument_x_picture",
	  	"reference_x_picture",  
		"reference",
		"reference_brand",
		"reference_type",
		"schema_info",	
		"country",
		"instrumentation_picture",
		"instrumentation_factors",
		"unit",
		"category",
		"attribute",
		"eav",
		"spectrum_x_eav",
		"hierarchy_level_x_spectrum",
		"taxonomy",
		"research_group",
		"research_group_members",
		"hierarchy_x_eav",
		"campaign_x_eav"
	};
	
	/** view names */
	public static final String[] VIEWS = new String[] {
		"campaign_view",
		"campaign_path_view",
		"hierarchy_level_view",
		"hierarchy_level_x_spectrum_view",
		"spectrum_x_eav_view",
		"eav_view",
		"research_group_view",
		"research_group_members_view",
		"hierarchy_x_eav_view",
		"campaign_x_eav_view"
	};
	

	/** user-updateable columns of the spectrum_view table */
	public static final String[] SPECTRUM_VIEW_COLS = new String[] {
		"spectrum_id",
		"measurement_unit_id",
		"measurement",
		"hierarchy_level_id",
		"sensor_id",
		"file_format_id",
		"campaign_id",
		"instrument_id",
		"reference_id",
		"calibration_id"
	};
	
	
	/** user-updateable user information tables */
	public static final String[] USER_TABLES = new String[] {
		"specchio_user",
		"specchio_user_group",
		"institute"
	};

}
