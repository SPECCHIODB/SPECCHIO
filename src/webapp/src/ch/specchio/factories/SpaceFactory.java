package ch.specchio.factories;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.ListIterator;

import ch.specchio.eav_db.SQL_StatementBuilder;
import ch.specchio.plots.GonioPosition;
import ch.specchio.plots.GonioSamplingPoints;
import ch.specchio.spaces.*;
import ch.specchio.types.Capabilities;
import ch.specchio.types.Instrument;
import org.ujmp.core.Matrix;
import org.ujmp.core.util.SerializationUtil;

/**
 * Class for creating and manipulating Space objects.
 */
public class SpaceFactory extends SPECCHIOFactory {
	
	private boolean match_only_sensor;
	private boolean match_only_sensor_and_unit;
	
	/** field by which to order lists of spaces */
	private String order_by = "date";

	/**
	 * Construct a factory using a specific user's connection to the database.
	 *
	 * @param db_user		database account user name
	 * @param db_password	database account password
	 * @param is_admin	is the user an administrator?
	 * @param capabilities		server capabilities and configurations
	 *
	 * @throws SPECCHIOFactoryException	could not establish initial context
	 */
	public SpaceFactory(String db_user, String db_password, String ds_name, boolean is_admin, Capabilities capabilities) throws SPECCHIOFactoryException {

		this(db_user, db_password, ds_name, is_admin);

		this.capabilities = capabilities;

	}
	
	/**
	 * Constructor. 
	 * 
	 * @param db_user		database account user name
	 * @param db_password	database account password
	 * @param ds_name		data source name
	 * @param is_admin		is the user an administrator? 
	 * 
	 * @throws SPECCHIOFactoryException	could not establish initial context
	 */
	public SpaceFactory(String db_user, String db_password, String ds_name, boolean is_admin) throws SPECCHIOFactoryException {

		super(db_user, db_password, ds_name, is_admin);
		
	}
	
	
	/**
	 * Copy constructor. Construct a new factory that uses the same database connection
	 * as an existing factory.
	 * 
	 * @param factory	the existing factory
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public SpaceFactory(SPECCHIOFactory factory) throws SPECCHIOFactoryException {
		
		super(factory);
		
	}
	
	
	/**
	 * Add a spectrum to a space
	 */
	private void addSpectrumToSpace(
			ArrayList<Space> spaces,
			int instrumentation_factors_id,
			int sensor_id,
			int measurement_type_id
		) throws SPECCHIOFactoryException {
		
		boolean space_exists = false;
		Space ss = null;
		
		// check if there is already a space that matches the sensor, instrument and calibration
		ListIterator<Space> li = spaces.listIterator();
		while(li.hasNext() && space_exists == false)
		{
			ss = li.next();
			if(((RefPanelCalSpace)ss).matches(sensor_id, measurement_type_id))
			{
				space_exists = true;
			}	
		}
		
		if(space_exists == false)
		{
			// create a new space
			MeasurementUnit mu = getMeasurementUnit(measurement_type_id);
			ss = createRefPanelCalSpace(sensor_id, mu);
			
			// add new space to space list
			spaces.add(ss);
		}
		
		ss.add_unique_spectrum_id(instrumentation_factors_id);
		
	}
	
	
	/**
	 * Add a spectrum to a space.
	 */
	private void addSpectrumToSpace(
			ArrayList<Space> spaces,
			int spectrum_id,
			int sensor_id,
			int instrument_id,
			int calibration_id,
			int measurement_type_id,
			boolean storage_format
		) throws SPECCHIOFactoryException {
		
		boolean space_exists = false;
		Space ss = null;
		
		// check if there is already a space that matches the sensor, instrument and calibration
		ListIterator<Space> li = spaces.listIterator();
		while(li.hasNext() && space_exists == false)
		{
			if (match_only_sensor_and_unit)
			{
				ss = li.next();
				
				if(((SensorAndInstrumentSpace)ss).matches(sensor_id, measurement_type_id))
				{
					space_exists = true;
				}					
				
			}
			else if (match_only_sensor)
			{
				ss = li.next();
				
				if(((SensorAndInstrumentSpace)ss).matches(sensor_id))
				{
					space_exists = true;
				}					
				
			}
			else
			{
				ss = li.next();
				if(((SensorAndInstrumentSpace)ss).matches(instrument_id, sensor_id, calibration_id, measurement_type_id))
				{
					space_exists = true;
				}	
			}
		}
		
		if(space_exists == false)
		{
			// create a new space
			MeasurementUnit mu = getMeasurementUnit(measurement_type_id);
			ss = createSensorAndInstrumentSpace(sensor_id, instrument_id, calibration_id, mu, storage_format);
			
			// add new space to space list
			spaces.add(ss);
		}
		
		ss.add_unique_spectrum_id(spectrum_id);
		
	}

	/**
	 * Add an uncertainty vector to a space.
	 */
	private void addSpectrumToSpace(
			ArrayList<Space> spaces,
			int spectrum_id,
			int sensor_id,
			int instrument_id,
			int calibration_id,
			int measurement_type_id,
			boolean storage_format,
			int uncertainty_set_id
	) throws SPECCHIOFactoryException {

		boolean space_exists = false;
		Space ss = null;

		// check if there is already a space that matches the sensor, instrument and calibration, the measurement unit and the uncertainty set
		ListIterator<Space> li = spaces.listIterator();
		while(li.hasNext() && space_exists == false)
		{
			ss = li.next();
			if(((UncertaintySpace)ss).matches(instrument_id, sensor_id, calibration_id, measurement_type_id, uncertainty_set_id))
			{
				space_exists = true;
			}
		}

		if(space_exists == false)
		{
			// create a new space
			MeasurementUnit mu = getMeasurementUnit(measurement_type_id);
			ss = this.createUncertaintySpace(sensor_id, instrument_id, calibration_id, mu, uncertainty_set_id);

			// add new space to space list
			spaces.add(ss);
		}

		ss.add_unique_spectrum_id(spectrum_id);

	}
	
	
	/**
	 * Helper method for getSpaces() and loadSpace().
	 * 
	 * @param table		the table to be queried
	 * @param id_column	the primary key of the table
	 * @param columns	the columns to return (not including the id column)
	 * @param ids		the ids for which to return data
	 * @param order_by	the attribute to order by
	 *
	 * @return an SQL query that will return the data described by the input parameters
	 */
	private String buildSpaceQuery(String table, String id_column, String columns[], ArrayList<Integer> ids, String order_by) {
		
		// start with an empty string
		StringBuffer query = new StringBuffer();
		
		//System.out.println(ids);
		
		// select the appropriate columns from the table
		query.append("select ");
		for (int i = 0; i < columns.length; i++) {
			if (i > 0) {
				query.append(", ");
			}
			query.append(getStatementBuilder().prefix(table, columns[i]));
		}
		query.append(" from " + table);
		
		int order_by_attribute_id = 0;
		String order_by_storage_field = null;
		if (order_by != null) {
			// join to the attribute by which ordering will be done
			order_by_attribute_id = getAttributes().get_attribute_id(order_by);
			order_by_storage_field = getAttributes().get_default_storage_field(order_by_attribute_id);
			query.append(
				" left join (" +
					"select spectrum_x_eav.spectrum_id, eav.eav_id, eav." + order_by_storage_field + " " +
					"from spectrum_x_eav, eav " +
					"where spectrum_x_eav.eav_id = eav.eav_id " +
						"and eav.attribute_id = " + order_by_attribute_id +
				") t " +
				"on " +
					getStatementBuilder().prefix("t", id_column) +
					"=" +
					getStatementBuilder().prefix(table, id_column)
			);
		}
		
		// add id restriction
		query.append(" where " + getStatementBuilder().prefix(table, id_column) + " in (" + getStatementBuilder().conc_ids(ids) + ")");
		
		if (order_by != null) {
			// add order-by clause
			query.append(" order by " + getStatementBuilder().prefix("t", order_by_storage_field));
		}
		
		return query.toString();
		
	}
	
	
	/**
	 * Create a reference panel calibration space.
	 * 
	 * @param sensor_id			the sensor identifier
	 * @param mu				the measurement unit
	 * 
	 * @return a new RefPanelCalSpace object
	 *
	 * @throws SPECCHIOFactoryException	database error
	 */
	private RefPanelCalSpace createRefPanelCalSpace(int sensor_id, MeasurementUnit mu) throws SPECCHIOFactoryException {
		
		RefPanelCalSpace rpcs = new RefPanelCalSpace(sensor_id, mu);
		setSensorAndInstrumentInSpace(rpcs);
		return rpcs;		
	}


	/**
	 * Create a new uncertainty space.
	 *
	 * @param sensor_id			the sensor identifier
	 * @param instrument_id		the instrument identifier (0 to create a reference panel calibration space)
	 * @param calibration_id	the calibration identifier
	 * @param mu				the measurement unit
	 * @param uncertainty_set_id	the identifier of the uncertainty set
	 *
	 * @return a new UncertaintySpace object
	 *
	 * @throws SPECCHIOFactoryException	database error
	 */
	private UncertaintySpace createUncertaintySpace(
			int sensor_id,
			int instrument_id,
			int calibration_id,
			MeasurementUnit mu,
			int uncertainty_set_id) throws SPECCHIOFactoryException {

		UncertaintySpace s;

		s = new UncertaintySpace(sensor_id, instrument_id, calibration_id, mu, uncertainty_set_id);
		s.setOrderBy(this.order_by);

		setSensorAndInstrumentInSpace(s);

		return s;

	}
	
	
	/**
	 * Create a new sensor and instrument space.
	 * 
	 * @param sensor_id            the sensor identifier
	 * @param instrument_id        the instrument identifier (0 to create a reference panel calibration space)
	 * @param calibration_id    the calibration identifier
	 * @param mu                the measurement unit
	 *
	 * @param storage_format
	 * @return a new SensorAndInstrumentSpace or RefPanelCalSpace object
	 *
	 * @throws SPECCHIOFactoryException	database error
	 */
	private SensorAndInstrumentSpace createSensorAndInstrumentSpace(
			int sensor_id,
			int instrument_id,
			int calibration_id,
			MeasurementUnit mu, boolean storage_format) throws SPECCHIOFactoryException {
		
		SensorAndInstrumentSpace sais;

		sais = new SensorAndInstrumentSpace(sensor_id, instrument_id, calibration_id, mu);
		sais.setOrderBy(this.order_by);
		sais.setUJMP_storage(storage_format);
		setSensorAndInstrumentInSpace(sais);
		
		return sais;
	
	}
	
	
	/**
	 * Fill sensor and instrument information for a space.
	 * 
	 * @param sais	sensor and instrument space	
	 */	
	private void setSensorAndInstrumentInSpace(SensorAndInstrumentSpace sais)
	{
		
		try {
			sais.setSensor(getDataCache().get_sensor(sais.getSensorid()));
			sais.setInstrument(getDataCache().get_instrument(sais.getInstrumentId(), sais.getCalibrationId()));
			
			if(sais.getInstrument() == null)
			{
				// try again without calibration id
				sais.setInstrument(getDataCache().get_instrument(sais.getInstrumentId()));
			}
			
			if(sais.getInstrument() == null)
			{
				// create a dummy instrument, only existing to provide empty attributes for reports
				sais.setInstrument(new Instrument()); 
				
				// set sensor so we can have a uniform call during the get_channels when plotting			
				sais.getInstrument().setSensor(sais.getSensor());
			}
			
			if(sais.getSensor() != null)
			{
				sais.setWvlsAreKnown(true);
				sais.setDimensionality(sais.getSensor().getNumberOfChannels().get_value());
//				sais.setSensorId(sensor_id);
//				sais.setInstrumentId(instrument_id);
//				sais.setCalibrationId(calibration_id);	
				sais.setAverageWavelengths(sais.getInstrument().getCentreWavelengths());
				
			} else {
				sais.setWvlsAreKnown(false);
			}
		} catch (SPECCHIOFactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
	}
	
	
	/**
	 * Get calibration identifiers for a given set of spectrum identifiers
	 * 
	 * @param ssi_list
	 * @param ids		the spectrum identifiers
	 * 
	 * @return a list of space_sorting_ident_struct objects
	 * 
	 * @throws SPECCHIOFactoryException	could not connect to the database
	 */
	public ArrayList<space_sorting_ident_struct> getCalibrationIds(
		ArrayList<space_sorting_ident_struct> ssi_list,
		ArrayList<Integer> ids) throws SPECCHIOFactoryException
	{
		
		int cal_id = 0;
		
		int cnt = 0;
	
		try {
			ResultSet rs;
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = getStatementBuilder().createStatement();
			String query =
					"select spectrum_id, calibration_id from (" +
						"select s.spectrum_id, s.instrument_id, c.calibration_id " +
						"from " +
							"spectrum_x_eav sxe, eav, attribute a, " +
							"spectrum s left outer join calibration c on c.instrument_id = s.instrument_id " + 
						"where " +
							"s.spectrum_id = sxe.spectrum_id and " +
							"sxe.eav_id = eav.eav_id and " +
							"a.attribute_id = eav.attribute_id and " + 
							"a.name = " + SQL.quote_string("Acquisition Time") + " and " +
							"eav.datetime_val >= c.calibration_date and " +
							"s.spectrum_id in (" + SQL.conc_ids(ids) + ") " +
						"order by (TIMEDIFF(eav.datetime_val, c.calibration_date)) asc " +
					") as closest_cal group by spectrum_id";

						
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				
				int spectrum_id = rs.getInt(1);
				cal_id = rs.getInt(2);
				
				// find the right entry to store it
				// this is needed as the order of spectrum ids can change due to different order-by conditions
				boolean found = false;
				cnt = 0;
				
				while(!found && cnt < ssi_list.size())
				{
					if(ssi_list.get(cnt).spectrum_id == spectrum_id)
					{
						ssi_list.get(cnt).calibration_id = cal_id;
						found = true;
					}
					
					cnt++;
					
				}
			
			}
											
			stmt.close();
			rs.close();

			}catch (SQLException ex) {
				// database error
				throw new SPECCHIOFactoryException(ex);
			}
		
		return ssi_list;
		
	}
	
	
	/**
	 * Get an array of Space objects representing calibration factors.
	 * 
	 * @param spectrum_ids	the identifiers of the spectra to use
	 * 
	 * @return a list of Space objects representing the calibration factors corresponding to the input identifiers
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public ArrayList<Space> getCalibrationSpaces(ArrayList<Integer> spectrum_ids) throws SPECCHIOFactoryException {
		
		ArrayList<Integer> cal_factor_ids = new ArrayList<Integer>();
		ArrayList<Integer> handled_spectrum_ids = new ArrayList<Integer>();
		ArrayList<Space> cal_spaces = new ArrayList<Space>();
		
		try {
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			
			// output dimension is equal to the sensor wavelengths of the sensor
			// attached to the panel corr factors.
			// Therefore:
			// get the spectrum_ids of the corr factors that are referenced by the
			// correct calibration of the panel used for the spectra in the input space
			// 
			String query = "select s.spectrum_id, c.cal_factors from spectrum s, calibration c, reference r, spectrum_x_eav sxe, eav eav where " +
				"s.spectrum_id in (" + SQL.conc_ids(spectrum_ids) + ") and s.reference_id = r. reference_id and c.reference_id = r. reference_id " +
				" and s.spectrum_id = sxe.spectrum_id and eav.eav_id = sxe.eav_id and eav.attribute_id = (select attribute_id from attribute where name = 'Acquisition Time') "  +
				"and eav.datetime_val >= c.calibration_date order by s.spectrum_id, abs(TIMEDIFF(c.calibration_date, eav.datetime_val)) asc";
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				int spectrum_id = rs.getInt(1);
				
				// only store the closest reference calibration data relative to spectrum acquisition date
				if(!handled_spectrum_ids.contains(spectrum_id))
				{					
					int cal_id = rs.getInt(2);
					if(!cal_factor_ids.contains(cal_id)) cal_factor_ids.add(cal_id);	
					handled_spectrum_ids.add(spectrum_id);
				}
				
				
			}	
			
			rs.close();		
			
			// create the spaces
			query = "SELECT instrumentation_factors_id, sensor_id, measurement_unit_id " +
					"from instrumentation_factors where instrumentation_factors_id in (" + SQL.conc_ids(cal_factor_ids) + ")";
			rs = stmt.executeQuery(query);
			while (rs.next()) {				
				addSpectrumToSpace(cal_spaces, rs.getInt(1), rs.getInt(2), rs.getInt(3));
			}	
			rs.close();
			
			stmt.close();
			
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
		return cal_spaces;
		
	}
	
	
	/**
	 * Get the Goniometer sampling points for a space.
	 * 
	 * @param space	the space
	 * 
	 * @return a GonioSamplingPoints object
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public GonioSamplingPoints getGonioSamplingPoints(SpectralSpace space) throws SPECCHIOFactoryException {
		
		GonioSamplingPoints sampling_points = new GonioSamplingPoints(space);
		
		try {
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			String query;
			
			// get attribute identifiers for sensor zenith and sensor azimuth
			int zenith_id = getAttributes().get_attribute_id("Sensor Zenith");
			int azimuth_id = getAttributes().get_attribute_id("Sensor Azimuth");
			if (zenith_id == 0 || azimuth_id == 0) {
				// database does not support these attributes
				throw new SPECCHIOFactoryException("The sensor zenith and azimuth attributes are not available.");
			}
			

			// create a temporary table with one row for each spectrum
			String tempname = SQL.prefix(getTempDatabaseName(), "sampling_geometry_" + getDatabaseUserName());
			query = "create temporary table if not exists " + tempname + " (" +
						"spectrum_id INT(10) PRIMARY KEY NOT NULL, " +
						"zenith DOUBLE, " +
						"azimuth DOUBLE " +
					")";
			stmt.executeUpdate(query);
			query = "delete from " + tempname;
			stmt.executeUpdate(query);
			query = "insert into " + tempname + "(spectrum_id, zenith, azimuth) values(" +
						"?, " +
						"(select double_val from spectrum_x_eav, eav where " +
							"spectrum_x_eav.spectrum_id=? and " +
							"spectrum_x_eav.eav_id=eav.eav_id and " +
							"eav.attribute_id=" + Integer.toString(zenith_id) +
						"), " +
						"(select double_val from spectrum_x_eav, eav where " +
							"spectrum_x_eav.spectrum_id=? and " +
							"spectrum_x_eav.eav_id=eav.eav_id and " +
							"eav.attribute_id=" + Integer.toString(azimuth_id) +
						")" +
					")";
			PreparedStatement pstmt = SQL.prepareStatement(query);
			for (Integer spectrum_id : space.getSpectrumIds()) {
				pstmt.setInt(1, spectrum_id);
				pstmt.setInt(2, spectrum_id);
				pstmt.setInt(3, spectrum_id);
				pstmt.executeUpdate();
			}
			pstmt.close();
			
			// build the list of sensor angles from the temporary table
			query = "select spectrum_id, zenith, azimuth from " + tempname;
			ResultSet rs = stmt.executeQuery(query);
			int s = 0;
			while (rs.next()) 
			{	
				int spectrum_id = rs.getInt(1);
				float sensor_zenith = rs.getFloat(2);
				float sensor_azimuth  = rs.getFloat(3);
				float sensor_elevation = 90 - sensor_zenith;
					
				double alpha = sensor_azimuth*(Math.PI/180);
				double beta = sensor_elevation*(Math.PI/180); 
								
				double x = Math.sin(alpha)*Math.cos(beta);
				double y = Math.cos(alpha)*Math.cos(beta);
					
				sampling_points.setAngle(s, new GonioPosition(sensor_azimuth, sensor_zenith, x, y));
					
				// store spectrum_ids in the correct order to match them with the internal index later
				sampling_points.setSpectrumId(s, spectrum_id);
					
				s++;
			}
			rs.close();
			
			// clean up
			stmt.close();

		} catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
		return sampling_points;
	}
	
	
	/**
	 * Get a measurement unit.
	 * 
	 * @param measurement_unit_id	the measurement unit identifier
	 * 
	 * @return a MeasurementUnit object corresponding to measurement_unit_id
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public MeasurementUnit getMeasurementUnit(int measurement_unit_id) throws SPECCHIOFactoryException {
		
		MeasurementUnit mu = new MeasurementUnit(measurement_unit_id);

		if (measurement_unit_id != 0) {
			SQL_StatementBuilder SQL = getStatementBuilder();	
			mu.setUnitNumber(Integer.parseInt(SQL.get_field_value("ASD_coding", "measurement_unit", measurement_unit_id)));
			mu.setUnitName(SQL.get_field_value("name", "measurement_unit", measurement_unit_id));
		}
			
		return mu;
	}
	
	
	/**
	 * Get a reference space.
	 * 
	 * @param input_ids		the identifiers of the input spectra
	 * @return a ReferenceSpaceStruct object
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public ReferenceSpaceStruct getReferenceSpace(ArrayList<Integer> input_ids) throws SPECCHIOFactoryException {
		
		ReferenceSpaceStruct rss = new ReferenceSpaceStruct();
		
		SQL_StatementBuilder SQL = getStatementBuilder();
//		String query;
		ResultSet rs;
		
//
//			// check if we are linking to spectralon (radiance) or irradiance (cosine receptor) data
//			query = "select counet(*)" +
//					" from spectrum_datalink where spectrum_id in (" + SQL.conc_ids(input_ids) + ") " +
//					"and datalink_type_id = (select datalink_type_id from datalink_type where name = ?)";
//			PreparedStatement pstmt = SQL.prepareStatement(query);
//
//			int spectralon_no_of_rows = 0;	
//			pstmt.setString(1, "Spectralon data");
//			rs = pstmt.executeQuery();
//			while (rs.next()) {
//				spectralon_no_of_rows = rs.getInt(1);
//			}
//
//			int cosine_no_of_rows = 0;
//			pstmt.setString(1, "Cosine receptor data");
//			rs = pstmt.executeQuery();
//			while (rs.next()) {
//				cosine_no_of_rows = rs.getInt(1);
//			}
//			
//			rss.is_spectralon = spectralon_no_of_rows > 0 && cosine_no_of_rows == 0;
//			
//			pstmt.close();
//			
//			// build the reference table
//			ArrayList<Integer> spectralon_spectra_ids = new ArrayList<Integer>();
//			Statement stmt = SQL.createStatement();
//			String datalink = (rss.is_spectralon)? "Spectralon data"  : "Cosine receptor data";
//			query = "select linked_spectrum_id, spectrum_id" +
//					" from spectrum_datalink where spectrum_id in (" + SQL.conc_ids(input_ids) + ") " +
//					"and datalink_type_id = (select datalink_type_id from datalink_type where name = '" + datalink + "')";
//			rs = stmt.executeQuery(query);
//			while (rs.next()) 
//			{
//				Integer linked_id = rs.getInt(1);
//				Integer spectrum_id = rs.getInt(2);
//
//				// build hash table
//				rss.spectrum_reference_table.put(spectrum_id, linked_id);
//				rss.spectrum_ids.add(spectrum_id);
//				spectralon_spectra_ids.add(linked_id);	
//				
//			}
//			rs.close();
//			stmt.close();
		
		ArrayList<Integer> ref_ids = new ArrayList<Integer>();
		
		// get eavs of all reference links
		MetadataFactory MF = new MetadataFactory(this);
		
		
		String query = "select sxe.spectrum_id, eav.spectrum_id from eav eav, spectrum_x_eav sxe where eav.attribute_id = " + getAttributes().get_attribute_id("Reference Data Link") +
				" and sxe.spectrum_id in " + "(" + SQL.conc_ids(input_ids) + ") and eav.eav_id = sxe.eav_id";
		
		Statement stmt;
		try {
			stmt = SQL.createStatement();

			rs = stmt.executeQuery(query);
			while (rs.next()) 
			{
				Integer spectrum_id = rs.getInt(1);
				Integer linked_id = rs.getInt(2);
	
				// build hash table
				rss.spectrum_reference_table.put(spectrum_id, linked_id);
				rss.spectrum_ids.add(spectrum_id);
				ref_ids.add(linked_id);	
				
			}
			rs.close();		
			
			Integer measurement_unit_id = 0;
			
			// check if this is cosine or equivalent to target geometry
			query = "select measurement_unit_id from spectrum where spectrum_id in " + "(" + SQL.conc_ids(input_ids.get(0)) + ") ";			
			rs = stmt.executeQuery(query);
			while (rs.next()) 
			{
				measurement_unit_id = rs.getInt(1);

			}
			rs.close();
			stmt.close();	
			
			if (measurement_unit_id == MeasurementUnit.Irradiance)
			{
				rss.is_spectralon = false;
			}
			else
			{
				rss.is_spectralon = true;
			}
			
//			
//			ArrayList<MetaParameter> mps = MF.getMetaParameterValues(input_ids, getAttributes().get_attribute_id("Reference Data Link"), false);
//			
//			for(MetaParameter mp : mps)
//			{
//				ref_ids.add((Integer) mp.getValue());				
//			}
//			
	
			rss.reference_space = null;
			if (ref_ids.size() > 0) {
				ArrayList<Space> spaces = getSpaces(ref_ids);
				if (spaces.size() == 1) {
					rss.reference_space = (SpectralSpace)spaces.get(0);
					loadSpace(rss.reference_space);
				}
			}
		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		return rss;
		
	}
	
	
	/**
	 * Get Space objects for a given set of spectra.
	 * 
	 * @param spectrum_ids	a list of spectrum identifiers
	 * 
	 * @return an array of Space objects corresponding to the given identifiers
	 * 
	 * @throws SPECCHIOFactoryException	could not access the database
	 */
	public ArrayList<Space> getSpaces(ArrayList<Integer> spectrum_ids) throws SPECCHIOFactoryException {
		
		ArrayList<Space> spaces = new ArrayList<Space>();
		ArrayList<space_sorting_ident_struct> ssi_list = new ArrayList<space_sorting_ident_struct>();
		
		try {
			
			SQL_StatementBuilder SQL = getStatementBuilder();
			
			// create a query string
			String columns[] = new String[] {
				"spectrum_id",
				"sensor_id",
				"instrument_id",
				"measurement_unit_id",
				"calibration_id"
			};

			if(capabilities.getBooleanCapability(Capabilities.MATRIX_STORAGE))
			{
				columns = new String[] {
						"spectrum_id",
						"sensor_id",
						"instrument_id",
						"measurement_unit_id",
						"calibration_id", "storage_format"
				};
			}

			String query = buildSpaceQuery("spectrum", "spectrum_id", columns, spectrum_ids, this.order_by);
			
			// get the spectra from the database
			Statement stmt = SQL.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				space_sorting_ident_struct ssi = new space_sorting_ident_struct();
				ssi.spectrum_id = rs.getInt(1);
				ssi.sensor_id = rs.getInt(2);
				ssi.instrument_id = rs.getInt(3);
				ssi.measurement_unit_id = rs.getInt(4);		
				ssi.calibration_id = rs.getInt(5);
				if(capabilities.getBooleanCapability(Capabilities.MATRIX_STORAGE)){
					ssi.storage_format = rs.getBoolean(6); }
				else
					ssi.storage_format = false;
				ssi_list.add(ssi);
			}	
			rs.close();
			stmt.close();
			
			
		} catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
		//ssi_list = getCalibrationIds(ssi_list, spectrum_ids);
		
		//int spectrum_id, int sensor_id, int instrument_id, int calibration_id, int measurement_type_id
		
		ListIterator<space_sorting_ident_struct> li = ssi_list.listIterator();
		while(li.hasNext())
		{
			space_sorting_ident_struct ssi = li.next();
			addSpectrumToSpace(spaces, ssi.spectrum_id, ssi.sensor_id, ssi.instrument_id, ssi.calibration_id, ssi.measurement_unit_id, ssi.storage_format);
		}
		
		return spaces;
		
	}



	/**
	 * Get Space objects for a given set of spectra and uncertainty_sets: these are collections of uncertainty vectors
	 *
	 * @param spectrum_ids	a list of spectrum identifiers
	 * @param uncertainty_set_ids	a list of uncertainty_set identifiers
	 *
	 * @return an array of Space objects corresponding to the given identifiers, grouped by sensor, instrument, measurement unit and uncertainty set id
	 *
	 * @throws SPECCHIOFactoryException	could not access the database
	 */
	public ArrayList<Space> getUncertaintySpaces(ArrayList<Integer> spectrum_ids, ArrayList<Integer> uncertainty_set_ids) throws SPECCHIOFactoryException {

		ArrayList<Space> spaces = new ArrayList<Space>();
		ArrayList<space_sorting_ident_struct> ssi_list = new ArrayList<space_sorting_ident_struct>();

		try {

			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();

			// create a query string
			String columns[] = new String[] {
					"spectrum_id",
					"sensor_id",
					"instrument_id",
					"measurement_unit_id",
					"calibration_id"
			};
			
			// Removing rows from uc_spectrum_temp, spectrum_temp

			String tmp_uc_spectrum_tablename = getStatementBuilder().prefix(getTempDatabaseName(), "uc_spectrum");
			
			// create temporary table
			String ddl_string = "CREATE TEMPORARY TABLE IF NOT EXISTS " + tmp_uc_spectrum_tablename + " " +
					"(uc_set_id INT NOT NULL, " +
					"spectrum_id INT NOT NULL," +
					"abs_rel varchar(3))";
			stmt.executeUpdate(ddl_string);

			// ensure table is empty
			String query = "delete from " + tmp_uc_spectrum_tablename;
			stmt.executeUpdate(query);

			String tmp_spectrum_tablename = getStatementBuilder().prefix(getTempDatabaseName(), "spectrum");
			
			// create temporary table
			ddl_string = "CREATE TEMPORARY TABLE IF NOT EXISTS " + tmp_spectrum_tablename + " " +
					"(spectrum_id INT NOT NULL, " +
					"sensor_id INT," +
					"instrument_id INT," +
					"spectrum_measurement_unit_id INT," +
					"calibration_id INT)";
			stmt.executeUpdate(ddl_string);


			// ensure table is empty
			 query = "delete from " + tmp_spectrum_tablename;
			stmt.executeUpdate(query);


//			String rm_uc_query = "DELETE FROM uc_spectrum_temp;";
//
//			PreparedStatement rm_uc_pstmt = SQL.prepareStatement(rm_uc_query);
//
//			rm_uc_pstmt.executeUpdate();
			
//			String rm_spectrum_query = "DELETE FROM spectrum_temp;";
//
//			PreparedStatement rm_spectrum_pstmt = SQL.prepareStatement(rm_spectrum_query);
//
//			rm_spectrum_pstmt.executeUpdate();
			
			// Selecting uncertainty information for spectrum_ids
			
			
		
			String uc_select_query = "SELECT DISTINCT ucs.uncertainty_set_id, ssbm.spectrum_id, sn.abs_rel\n" + 
					"FROM spectrum_subset_map ssbm\n" + 
					"INNER JOIN spectrum_node sn ON sn.spectrum_node_id = ssbm.spectrum_node_id\n" +
					"INNER JOIN spectrum_set_map ssm ON ssm.spectrum_subset_id = ssbm.spectrum_subset_id\n" + 
					"INNER JOIN uncertainty_node un ON un.spectrum_set_id = ssm.spectrum_set_id\n" + 
					"INNER JOIN uncertainty_node_set uns ON uns.node_id = un.node_id\n" + 
					"INNER JOIN uncertainty_set ucs ON ucs.node_set_id = uns.node_set_id\n" + 
					"WHERE ssbm.spectrum_id in ("  + getStatementBuilder().conc_ids(spectrum_ids) + ")\n" + 
					"AND ucs.uncertainty_set_id in (" + getStatementBuilder().conc_ids(uncertainty_set_ids) + ");";
			
			PreparedStatement uc_select_pstmt = SQL.prepareStatement(uc_select_query);
			
			System.out.println("Uc query:" + uc_select_query);
			ResultSet rsc = uc_select_pstmt.executeQuery();
			
			String insert_uc_spectrum_query = "insert into " + tmp_uc_spectrum_tablename +"(uc_set_id, spectrum_id, abs_rel) " +
					" values (?, ?, ?)";
			
			PreparedStatement insert_uc_spectrum_pstmt = SQL.prepareStatement(insert_uc_spectrum_query);
			
			while (rsc.next()) {
				
				int uc_set_id = rsc.getInt(1);
				int spectrum_id = rsc.getInt(2);
				String abs_rel = rsc.getString(3);

				// Now inserting these values in SQL
				
				insert_uc_spectrum_pstmt.setInt(1, uc_set_id);
				insert_uc_spectrum_pstmt.setInt(2, spectrum_id);
				insert_uc_spectrum_pstmt.setString(3, abs_rel);

				insert_uc_spectrum_pstmt.executeUpdate();
				
			}
			
			// Selecting spectrum information
			
			String spectrum_query = buildSpaceQuery("spectrum", "spectrum_id", columns, spectrum_ids, this.order_by);
			
			PreparedStatement select_spectrum_pstmt = SQL.prepareStatement(spectrum_query);
			
			ResultSet rss = select_spectrum_pstmt.executeQuery();
			
			String insert_spectrum_query = "insert into "+ tmp_spectrum_tablename +"(spectrum_id, sensor_id, instrument_id, spectrum_measurement_unit_id, calibration_id) " +
					" values (?, ?, ?, ?, ?)";
			
			PreparedStatement insert_spectrum_pstmt = SQL.prepareStatement(insert_spectrum_query);
			
			while (rss.next()) {
				
				int spectrum_id = rss.getInt(1);
				int sensor_id = rss.getInt(2);
				int instrument_id = rss.getInt(3);
				int spectrum_measurement_unit_id = rss.getInt(4);
				int calibration_id = rss.getInt(5);
				
				// Now inserting these values into temporary table in SQL
				
				insert_spectrum_pstmt.setInt(1, spectrum_id);
				insert_spectrum_pstmt.setInt(2, sensor_id);
				insert_spectrum_pstmt.setInt(3, instrument_id);
				insert_spectrum_pstmt.setInt(4, spectrum_measurement_unit_id);
				insert_spectrum_pstmt.setInt(5, calibration_id);
				
				insert_spectrum_pstmt.executeUpdate();
				
			}
			
			// Left join of tables to retrieve spectrum and uncertainty information in one place
			
			String join_query = "SELECT uc.uc_set_id, uc.spectrum_id, uc.abs_rel, sp.sensor_id, sp.instrument_id, sp.spectrum_measurement_unit_id, sp.calibration_id \n" + 
					"FROM " + tmp_uc_spectrum_tablename + " uc\n" +
					"LEFT JOIN " + tmp_spectrum_tablename + " sp\n" +
					"ON uc.spectrum_id = sp.spectrum_id;";
			
			PreparedStatement join_pstmt = SQL.prepareStatement(join_query);
			
			// Then get results back out
			ResultSet rsj = join_pstmt.executeQuery();
			
			while (rsj.next()) {
				
				// Now we populate the structure needed below:
				space_sorting_ident_struct ssi = new space_sorting_ident_struct();
				
				ssi.uncertainty_set_id = rsj.getInt(1);
				ssi.spectrum_id  = rsj.getInt(2);
				ssi.sensor_id = rsj.getInt(4);
				ssi.instrument_id = rsj.getInt(5);
				ssi.calibration_id = rsj.getInt(7);
		
				String uncertainty_unit = rsj.getString(3);
				int spectrum_measurement_unit_id = rsj.getInt(6);

				// this is the unit of the uncertainty (absolute: in this case it should be equivalent to spectrum_measurement_unit_id | relative: in this case it should be percentage)
				if(uncertainty_unit.equals("abs"))
				{
					ssi.measurement_unit_id = spectrum_measurement_unit_id;
				}
				else
				{
					MeasurementUnit mu = getDataCache().get_measurement_unit("Percent");
					ssi.measurement_unit_id = mu.getUnitId();
				}
				
				
				ssi_list.add(ssi);
			}
			

		} catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}

		ListIterator<space_sorting_ident_struct> li = ssi_list.listIterator();
		while(li.hasNext())
		{
			space_sorting_ident_struct ssi = li.next();
			addSpectrumToSpace(spaces, ssi.spectrum_id, ssi.sensor_id, ssi.instrument_id, ssi.calibration_id, ssi.measurement_unit_id, ssi.storage_format, ssi.uncertainty_set_id);

		}

		return spaces;

	}


	/**
	 * Get a set of spaces corresponding to instrumentation factors.
	 * 
	 * @param instrumentation_factor_ids	the instrumentation factor identifiers
	 * 
	 * @return a list of Space objects corresponding to the given identifiers
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public ArrayList<Space> getSpacesForInstrumentationFactors(ArrayList<Integer> instrumentation_factor_ids)
		throws SPECCHIOFactoryException {
		
		ArrayList<Space> spaces = new ArrayList<Space>(instrumentation_factor_ids.size());
		
		try {
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			String query = "SELECT instrumentation_factors_id, sensor_id, measurement_unit_id from instrumentation_factors " +
					"where instrumentation_factors_id in (" + SQL.conc_ids(instrumentation_factor_ids) + ")";
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				addSpectrumToSpace(spaces, rs.getInt(1), rs.getInt(2), rs.getInt(3));
			}
			rs.close();
			stmt.close();
		} 
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
		return spaces;
		
	}
	

    /**
     * Load a Space object from the database.
     *
     * @param space	a partially-filled Space object
     *
     * @throws SPECCHIOFactoryException	database error
     */
    public void loadSpace(Space space) throws SPECCHIOFactoryException {
        Instant start = Instant.now();
        // clear existing data vectors
        Instant startclearDataVectors = Instant.now();
        space.clearDataVectors();

        int curr_id = 0;
        Instant endclearDataVectors = Instant.now();
        long msecondsclearDataVectors = Duration.between(startclearDataVectors, endclearDataVectors).toMillis();

        int uncertainty_set_id = 0;
        
        try {
            // create SQL-building objects
            Instant startexecuteQuery = Instant.now();

            SQL_StatementBuilder SQL = getStatementBuilder();
            Statement stmt = SQL.createStatement();

            String quicker_order_by = "";

            // build query
            String table;
            String id_column = "";
            String order_by;
            String conc_ids = SQL.conc_ids(space.getSpectrumIds());
            
            
            
            if (space instanceof RefPanelCalSpace) {
                // load instrumentation calibration factors
                table = "instrumentation_factors";
                id_column = "instrumentation_factors_id";
                order_by = null;
            }
			else if (space instanceof UncertaintySpace) {
				
				// Downcasting to UncertaintySpace in order to access uncertainty set id which is needed for query
				
				UncertaintySpace uc_space = (UncertaintySpace) space; 
				
				uncertainty_set_id = uc_space.uncertainty_set_id;
				
				// load uncertainty vectors
				table = "uncertainty";
				
				// Do we have an id column here?
				// Check normal spectrum/wvl space!
				
				id_column = "spectrum_id";
				quicker_order_by = "order by FIELD (spectrum_id, "+ conc_ids +")";
			}
			else {
                // load spectral data
                table = "spectrum";
                id_column = "spectrum_id";
                order_by = null;
                quicker_order_by = "order by FIELD (spectrum_id, "+ conc_ids +")";
            }
			String query = " ";
			if(space.getSelectedBand() != null){
				// SELECT THE SUBSET OF THE BLOB THAT CORRESPONDS TO THE GIVEN BAND (LIMIT 1 if more than 1)
				String getBand = "SELECT wvl, sid - ( SELECT sensor_element_id FROM sensor_element " +
						" WHERE sensor_id = "+ ((SensorAndInstrumentSpace) space).getSensor().getSensorId() + " LIMIT 1) " +
						" AS x FROM ( SELECT se.avg_wavelength AS wvl, se.sensor_element_id AS sid" +
						" , sen.* FROM sensor_element AS " +
						" se JOIN sensor AS sen ON se.sensor_id = sen.sensor_id " +
						" WHERE sen.sensor_id = " + ((SensorAndInstrumentSpace) space).getSensor().getSensorId() + " HAVING se.avg_wavelength " +
						" >= " + space.getSelectedBand() + " AND se.avg_wavelength <= " + (space.getSelectedBand() + 1) + ") AS r LIMIT 1";

				// Execute query to get the substring index for the blob
				ResultSet bandNr = stmt.executeQuery(getBand);
				bandNr.next();
				float bandWvl = Float.parseFloat(bandNr.getString(1));
				space.setSelectedWavelength(bandWvl);
				int bandIndex = Integer.parseInt(bandNr.getString(2));
				// The formula for the index is 4n-3
				String substringIndex = Integer.toString((bandIndex * 4)-3);


				// Define and run the query that will return the subset of the blob (1 element, 4 bytes)
				 query = "SELECT substring(sp.measurement, "+ substringIndex + " , 4), sp.spectrum_id FROM spectrum AS sp " +
						" WHERE sp.spectrum_id IN ( " + conc_ids + " )"  + quicker_order_by;
			} else if(table.equals("spectrum")){
				// SELECT THE WHOLE BLOB
				String columns[] = new String[] { "measurement", id_column };
				//query = buildSpaceQuery(table, id_column, columns, space.getSpectrumIds(), order_by) + quicker_order_by;
				query = "SELECT sp.measurement, sp.spectrum_id FROM spectrum AS sp " +
							" WHERE sp.spectrum_id IN ( " + conc_ids + " )"  + quicker_order_by;
			}
			else if(table.equals("instrumentation_factors")) {
				
				query = "SELECT measurement, instrumentation_factors_id FROM instrumentation_factors WHERE instrumentation_factors_id = " + conc_ids;
				System.out.println("instrumentaton_factors query" + query);
				
			}
			else if(table.equals("uncertainty")) {


				// TODO: why do we need distinct here? Without distinct it duplicates entries .... but this was apparenlty due to the JAXB unmarshalling
				query = "SELECT distinct sn.u_vector, ss.spectrum_id\n" +
						"FROM spectrum_node sn\n" + 
						"INNER JOIN spectrum_subset_map ss ON ss.spectrum_node_id = sn.spectrum_node_id\n" + 
						"INNER JOIN spectrum_set_map ssm ON ssm.spectrum_subset_id = ss.spectrum_subset_id\n" +
						"INNER JOIN uncertainty_node un ON un.spectrum_set_id = ssm.spectrum_set_id\n" +
						"INNER JOIN uncertainty_node_set uns ON uns.node_id = un.node_id\n" +
						"INNER JOIN uncertainty_set ucs ON ucs.node_set_id = uns.node_set_id\n" +
						"WHERE ucs.uncertainty_set_id = " + uncertainty_set_id + "\n" +
						"AND ss.spectrum_id IN (" + conc_ids + ")"  + quicker_order_by;
				System.out.println("uncertainty vectors query" + query);
				
				
			}
			
			ResultSet rs = stmt.executeQuery(query);
            Instant endexecuteQuery = Instant.now();
            long msecondsexecuteQuery = Duration.between(startexecuteQuery, endexecuteQuery).toMillis();


//				float[][] vectors =  new float[space.getSpectrumIds().size()][space.getDimensionality()];

            long msecondsTocreateMatrix = Duration.between(endexecuteQuery, Instant.now()).toMillis();


            int cnt = 0;
            Instant startReadBlobs = Instant.now();



            while (rs.next())
            {
				Blob measurement = rs.getBlob(1);
				InputStream binstream = measurement.getBinaryStream();

                curr_id = rs.getInt(2);

                if(curr_id != space.getSpectrumIds().get(cnt))
                {
                    int buggerit = 1;
                }



				if(space.isUJMP_storage())
				{
					//String m_hex = rs.getString(1);

					byte[] b =  measurement.getBytes(1l, (int)measurement.length());

					String hex = javax.xml.bind.DatatypeConverter.printHexBinary(b);

					space.addMeasurementMatrixSerialised(hex);

					// Test of conversion to matrix from hex
//					byte[] b_ = javax.xml.bind.DatatypeConverter.parseHexBinary(hex);
//					space.addMeasurementMatrix((Matrix) SerializationUtil.deserialize(b_));

					int x = 1;

					// Direct creation of matrices; not needed as they get pushed through JAXB anyway, hence, it is easier to keep them as HEX string
					// space.addMeasurementMatrix((Matrix) SerializationUtil.deserialize(binstream));
				}
				else {

					int nElements = (int) measurement.length() / 4;

					if(space.getSelectedBand() != null){
						space.setDimensionality(1);
					} else{
						if(!space.getWvlsAreKnown() && space.getDimensionalityIsSet() == false)
						{
							try {
								space.setDimensionality(binstream.available() / 4);
							} catch (IOException e) {
								// dont't know what would cause this
								e.printStackTrace();
							}
						}
					}

					DataInput dis = new DataInputStream(binstream);
					double[] vector = new double[nElements];
//				ArrayList<Double> vectorArr = new ArrayList<>();

					for (int i = 0; i < nElements; i++) {
						try {
							Float f = dis.readFloat();

//							vectors[cnt][i]=f;
							vector[i] = f.doubleValue();
//						vectorArr.add(f.doubleValue());
						} catch (IOException e) {
							// don't know what would cause this
							e.printStackTrace();
						}
					}

					space.addVector(vector);

					try {
						binstream.close();
					} catch (IOException e) {
						// don't know what would cause this
						e.printStackTrace();
					}

				}

                cnt++;


//                double[] vector = new double[vectorArr.size()];
//
//                for(int i=0;i<vectorArr.size();i++){
//                	vector[i] = vectorArr.get(i);
//				}



            }

            Instant endReadBlobs = Instant.now();
            long msecondsReadBlobs = Duration.between(startReadBlobs, endReadBlobs).toMillis();

            Instant end = Instant.now();
            long mseconds = Duration.between(start, end).toMillis();

            rs.close();
            stmt.close();

        } catch (SQLException ex) {
            // database error
            throw new SPECCHIOFactoryException(ex);
        }
        catch (java.lang.NullPointerException ex)
        {
            throw new SPECCHIOFactoryException("Found spectrum with a zero binary entry: please delete or reload spectrum with id = " + curr_id);
        }
//        catch (IOException e) {
//			e.printStackTrace();
//		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
//		}
	}
	
	
	public void setMatchOnlySensor(boolean match_only_sensor) {
		
		this.match_only_sensor = match_only_sensor;
		
	}
	
	
	public void setMatchOnlySensorAndUnit(boolean match_only_sensor_and_unit) {
		
		this.match_only_sensor_and_unit = match_only_sensor_and_unit;
		
	}
	
	
	/**
	 * Set the attribute by which spaces will be ordered.
	 * 
	 * @param order_by	the name of the attribute by which results will be ordered
	 */
	public void setOrderByAttribute(String order_by) {
		
		this.order_by = order_by;
		
	}

}
