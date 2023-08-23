package ch.specchio.factories;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ch.specchio.types.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;

import ch.specchio.eav_db.SQL_StatementBuilder;
import ch.specchio.eav_db.id_and_op_struct;
import ch.specchio.spaces.Space;
import ch.specchio.spaces.SpectralSpace;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


/**
 * Class for manipulating instruments, references and sensors in the database.
 */
public class InstrumentationFactory extends SPECCHIOFactory {
	
	/** the prefix of instrument table and column names */
	private static final String INSTRUMENT = "instrument";
	
	/** the prefix of reference table and column names */
	private static final String REFERENCE = "reference";

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
	public InstrumentationFactory(String db_user, String db_password, String ds_name, boolean is_admin, Capabilities capabilities) throws SPECCHIOFactoryException {

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
	private InstrumentationFactory(String db_user, String db_password, String ds_name, boolean is_admin) throws SPECCHIOFactoryException {

		super(db_user, db_password, ds_name, is_admin);

	}

	/**
	 * Construct a factory using the default connection to the database.
	 * 
	 * @param ds_name		database connection
	 * @throws SPECCHIOFactoryException	could not establish initial context
	 */	
	public InstrumentationFactory(String ds_name) throws SPECCHIOFactoryException {
		super(ds_name);
	}


	/**
	 * Delete an instrument calibration.
	 * 
	 * @param calibration_id	the identifier of the calibration to be deleted
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public void deleteCalibration(int calibration_id) throws SPECCHIOFactoryException {
		
		try {
			Statement stmt = getStatementBuilder().createStatement();
			
			String query = "delete from calibration where calibration_id = " + calibration_id;
			stmt.executeUpdate(query);
			
			query = "delete from instrumentation_factors USING instrumentation_factors, calibration where (instrumentation_factors_id = cal_factors OR instrumentation_factors_id = uncertainty) and calibration_id = " + calibration_id;
			
			stmt.executeUpdate(query);						

			stmt.close();
		} catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		} 
		
	}
	
	
	/**
	 * Delete an instrument from the database.
	 * 
	 * @param instrument_id	the identifier of the instrument to be deleted
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public void deleteInstrument(int instrument_id) throws SPECCHIOFactoryException {
		
		try {
			Statement stmt = getStatementBuilder().createStatement();
			String query;
			int calibration_id = 0;
			
			// delete the instrument's calibration data
			query = "select calibration_id from calibration where instrument_id = " + instrument_id;
			
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				calibration_id = rs.getInt(1);
				deleteCalibration(calibration_id);
				getDataCache().deleteCalibration(calibration_id);
			}
			rs.close();						
			
			// delete the instrument's pictures
			query = "select instrumentation_picture_id from instrument_x_picture where instrument_id = " + instrument_id;
			
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				int picture_id = rs.getInt(1);
				deletePicture(picture_id);
			}
			rs.close();				
			
			query = "delete FROM specchio.instrumentation_factors where instrumentation_factors_id in (select cal_factors from calibration where instrument_id = " + instrument_id + ")";
			stmt.executeUpdate(query);	
			
			query = "delete from calibration where instrument_id = " + instrument_id;
			stmt.executeUpdate(query);			
			
			// delete the instrument from the database
			query = "delete from instrument where instrument_id = " + instrument_id;
			stmt.executeUpdate(query);
			
			// clean up
			stmt.close();
			
			// delete the instrument and calibration from the cache
			getDataCache().delete_instrument(instrument_id);
			
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	
	
	/**
	 * Delete a picture from the database.
	 * 
	 * @param picture_id	the identifier of the picture to be deleted
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public void deletePicture(int picture_id) throws SPECCHIOFactoryException {
		
		try {
			String query;
			Statement stmt = getStatementBuilder().createStatement();
			
			// delete the picture from the join tables
			for (String table : new String[] {INSTRUMENT, REFERENCE}) {
				query = "delete from " + table + "_x_picture where instrumentation_picture_id=" + Integer.toString(picture_id);
				stmt.executeUpdate(query);
			}
			
			// delete the picture from the instrumentation_picture table
			query = "delete from instrumentation_picture where instrumentation_picture_id=" + Integer.toString(picture_id);
			stmt.executeUpdate(query);
			
			// clean up
			stmt.close();
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	
	
	/**
	 * Delete a reference from the database.
	 * 
	 * @param reference_id	the identifier of the reference to be deleted
	 * 
	 * @throws SPECCHIOFactoryException	the reference does not exist
	 */
	public void deleteReference(int reference_id) throws SPECCHIOFactoryException {
		
		try {	
			// get SQL-building objects
			Statement stmt = getStatementBuilder().createStatement();
			String query;
			
			// delete the references's pictures
			query = "select instrumentation_picture_id from reference_x_picture where reference_id = " + reference_id;
			
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				int picture_id = rs.getInt(1);
				deletePicture(picture_id);
			}
			rs.close();					
			
			// delete the references's calibration data
			query = "delete from calibration where reference_id = " + Integer.toString(reference_id);
			stmt.executeUpdate(query);
			
			// delete the reference itself
			query = "delete from reference where reference_id = " + Integer.toString(reference_id);
			stmt.executeUpdate(query);
			stmt.close();
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	
	/**
	 * Get a calibrated instrument object.
	 * 
	 * @param calibration_id	the calibration identifier
	 * 
	 * @return a new Instrument object, or null if the calibrated instrument does not exist
	 * 
	 * @throws SPECCHIOFactoryException	the instrument does not exist
	 */
	public Instrument getCalibratedInstrument(int calibration_id) throws SPECCHIOFactoryException {
		
		
		CalibrationMetadata cm = getCalibrationMetadata(calibration_id);
		
		Instrument i = this.getInstrument(cm.getInstrumentId());
		
		// fill wavelength calibration for instrument if there is a wavelength calibration
		// TODO: should check if this is really a wavelength calibration
		i.setAverageWavelengths(cm.getFactors());
		
		
		return i;
	}

	/**
	 * Get an array of CalibrationMetadata based on an instrument id and a calibration number (typically an ASD instrument).
	 *
	 * @param instrument_id			the instrument_id
	 * @param calibration_number	the calibration number is given in the Metadata field "Calibration Number"
	 *
	 * @return an array of CalibrationMetadata
	 *
	 * @throws SPECCHIOFactoryException	the instrument does not exist
	 */
	public CalibrationMetadata[] getCalibrationMetadataByCalibrationNumber(int instrument_id, int calibration_number) throws SPECCHIOFactoryException {

		String query = "select calibration_id,calibration_no,calibration_date,comments, cal_factors, uncertainty, reference_id, instrument_id, fov, name from calibration " +
				" where instrument_id =" + Integer.toString(instrument_id) + " AND calibration_no = " + Integer.toString(calibration_number);

		List<CalibrationMetadata> cmlist = getCalibrationMetadata(query);

		return cmlist.toArray(new CalibrationMetadata[cmlist.size()]);
	}

	
	/**
	 * Get the calibration metadata for an instrument or reference.
	 * 
	 * @param calibration_id	calibration_id
	 * 
	 * @return an array of calibration metatdata objects associated with the instrument
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	private CalibrationMetadata getCalibrationMetadata(int calibration_id) throws SPECCHIOFactoryException {
				
		String query = "select calibration_id,calibration_no,calibration_date,comments, cal_factors, uncertainty, reference_id, instrument_id from calibration " +
				" where calibration_id " + "=" + Integer.toString(calibration_id);

		List<CalibrationMetadata> cmlist = getCalibrationMetadata(query);

		return cmlist.get(0); // there can be only one per calibration id!
		
	}	
	
	
	/**
	 * Get the calibration metadata for an instrument or reference.
	 * 
	 * @param object_type	"instrument" or "reference"
	 * @param object_id		the instrument or reference identifier
	 * 
	 * @return an array of calibration metatdata objects associated with the instrument
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	private CalibrationMetadata[] getCalibrationMetadata(String object_type, int object_id) throws SPECCHIOFactoryException {
				
		// work out column name
		String id_column = object_type + "_id";

		String query = "select calibration_id,calibration_no,calibration_date,comments, cal_factors, uncertainty, reference_id, instrument_id, fov, name from calibration " +
				" where " + id_column + "=" + Integer.toString(object_id);

		List<CalibrationMetadata> cmlist = getCalibrationMetadata(query);

		return cmlist.toArray(new CalibrationMetadata[cmlist.size()]);
		
	}
	
	
	/**
	 * Get the calibration metadata for a defined query.
	 * 
	 * @param 	query
	 * 
	 * @return an list of calibration metatdata objects
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	private List<CalibrationMetadata> getCalibrationMetadata(String query) throws SPECCHIOFactoryException {
		
		List<CalibrationMetadata> cmlist = new ArrayList<CalibrationMetadata>();
		
		
		try {
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			ResultSet rs;

			// check if the eav metadata for calibrations exists on this system

			rs = stmt.executeQuery(query);

			while (rs.next()) {
				int col_no = 1;
				CalibrationMetadata cm = new CalibrationMetadata(rs.getInt(col_no++));
				cm.setCalibrationNumber(rs.getInt(col_no++));
				
				DateTimeFormatter formatter;
				String date_str = rs.getString(col_no++);
				

				
				
				if(date_str != null)
				{	
					if(date_str.length()==10)
					{
						formatter = DateTimeFormat.forPattern("yyyy-MM-dd").withZoneUTC();
					}
					else
					{
						formatter = DateTimeFormat.forPattern(MetaDate.DEFAULT_DATE_FORMAT + ".S").withZoneUTC();
					}
					
					DateTime d = formatter.parseDateTime(date_str); 
					cm.setCalibrationDate(d);
				}
				
				cm.setComments(rs.getString(col_no++));
				cm.setCalFactorsId(rs.getInt(col_no++));
				cm.setCalibrationFactorsPlot(getCalibrationPlotsMetadata(cm.getCalFactorsId(), "cal_factors"));
				cm.setUncertainty_id(rs.getInt(col_no++));
				cm.setCalibrationUncertaintyPlot(getCalibrationPlotsMetadata(cm.getUncertainty_id(), "uncertainty"));				
				cm.setReferenceId(rs.getInt(col_no++));
				cm.setInstrumentId(rs.getInt(col_no++));
				if(query.contains("fov"))  cm.setField_of_view(rs.getInt(col_no++)); // dirty hack to avoid errors with different queries
				if(query.contains("name"))  cm.setName(rs.getString(col_no++));
				
				// fill factors from space
				cm.setFactors(cm.getCalibrationFactorsPlot().getSpace().getVectors().get(0));

				// get additional metadata if existing in the eav table
				if(this.capabilities.getBooleanCapability(Capabilities.EAV_FOR_CALIBRATION)){
					MetadataFactory mf = new MetadataFactory(this);
					Metadata md = mf.getMetadataForCalibration(cm.getCalibration_id());
					cm.setMetadata(md);
				}


				cmlist.add(cm);
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
		return cmlist;
		
	}	


	/**
	 * Get calibration plot metadata.
	 * 
	 * @param calibration_id	the calibration identifier
	 * @param plot_type			"cal_factors" or "uncertainty"
	 * 
	 * @return a new calibration plots metadata object
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	private CalibrationPlotsMetadata getCalibrationPlotsMetadata(Integer calibration_id, String plot_type)
			throws SPECCHIOFactoryException {
		
		// get spaces for this calibration identifier
		SpaceFactory sf = new SpaceFactory(this);
		ArrayList<Integer> calibration_id_list = new ArrayList<Integer>();
		calibration_id_list.add(calibration_id);
		ArrayList<Space> spaces = sf.getSpacesForInstrumentationFactors(calibration_id_list);
		
		// create the calibration plots metadata object
		CalibrationPlotsMetadata cpm = new CalibrationPlotsMetadata();
		if (spaces.size() > 0) {
			SpectralSpace ss = (SpectralSpace)spaces.get(0);
			sf.loadSpace(ss);
			cpm.setSpace(ss);
		}
		
		// clean up
		sf.dispose();
		
		return cpm;
		
	}
	
	
	/**
	 * Get an Instrument object from the database.
	 * 
	 * @param instrument_id	the identifier of the desired instrument
	 * 
	 * @return an Instrument object corresponding to instrument_id
	 * 
	 * @trhows SPECCHIOFactoryException	the instrument does not exist
	 */
	public Instrument getInstrument(int instrument_id) throws SPECCHIOFactoryException {
		
		Instrument instrument = new Instrument(instrument_id);

		try {
			
			// set default values
			int institute_id = 0;
			
			// read information from database
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();			
			String[] tables = new String[]{"instrument"};
			String[] attr = new String[]{"name", "institute_id", "serial_number", "sensor_id"};
			String query = SQL.assemble_sql_select_query(
						SQL.conc_attributes(attr),
						SQL.conc_tables(tables),
						"instrument.instrument_id = " + Integer.toString(instrument_id));
			
						
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				instrument.getInstrumentName().value = rs.getString(1);
				institute_id = rs.getInt(2);
				instrument.getInstrumentNumber().value = rs.getString(3);
				instrument.setSensorId(rs.getInt(4));
			}
			rs.close();		
			
			// load sensor
			instrument.setSensor(getDataCache().get_sensor(instrument.getSensorId()));
			
			Institute inst = getDataCache().get_institute_by_id(institute_id);
			if (inst != null) {
				instrument.getInstrumentOwner().value = inst.toString();
			}
			
			stmt.close();
			
		} catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
		instrument.setValid(true);
		
		return instrument;
		
	}
	

	/**
	 * Get an instrument that matches the passed object or creates a new instrument and returns it
	 * 
	 * @param instrument instance
	 * 
	 * @return an Instrument object matching the passed object, or a new instrument
	 * 
	 * @throws SPECCHIOFactoryException	on error
	 */	
	public Instrument getOrCreateInstrument(Instrument instrument) {
		
		
		// create dummy spectral file with instrument info and re-use existing method
		SpectralFile spec_file = new SpectralFile();
		
		spec_file.setInstrumentNumber(instrument.getInstrumentNumber().get_value());
		
		Float[] wvls =  new Float[instrument.getCentreWavelengths().length];
		
		for (int i = 0 ; i < instrument.getCentreWavelengths().length; i++)
		{
			wvls[i] = (float) instrument.getCentreWavelengths()[i];
		}		
		
		spec_file.addWvls(wvls);
		
		spec_file.setCompany(instrument.getSensor().getManufacturerShortName().get_value());
		
		
		Instrument new_instrument = getInstrumentForSpectralFile(spec_file);
		
		
		
		return new_instrument;
	}


	
	
	/**
	 * Get the calibration metadata for an instrument.
	 * 
	 * @param instrument_id	the instrument identifier
	 * 
	 * @return an array of calibration metadata objects for the instrument
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public CalibrationMetadata[] getInstrumentCalibrationMetadata(int instrument_id) throws SPECCHIOFactoryException {
		
		return getCalibrationMetadata(INSTRUMENT, instrument_id);
		
	}
	

	
	/**
	 * Get the descriptors of every instrument in the database.
	 * 
	 * @return an array of InstrumentDescriptors identifying every instrument in the database
	 * 
	 * @throws SPECCHIOFactoryException	could not access the database
	 */
	public InstrumentDescriptor[] getInstrumentDescriptors() throws SPECCHIOFactoryException {
		
		List<InstrumentDescriptor> instruments = new ArrayList<InstrumentDescriptor>();
		
		try {
			Statement stmt = getStatementBuilder().createStatement();
			String query = "select instrument_id, name from instrument";
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				InstrumentDescriptor instrument = new InstrumentDescriptor(rs.getInt(1), rs.getString(2));
				instruments.add(instrument);
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
		return instruments.toArray(new InstrumentDescriptor[instruments.size()]);
	
	}
	
	
	/**
	 * Get a instrument object for a given spectral file object.
	 * 
	 * @param spec_file		the spectral file
	 * 
	 * @return a new Instrument object
	 * 
	 * @throws SPECCHIOFactoryException	the instrument does not exist
	 */	
	public Instrument getInstrumentForSpectralFile(SpectralFile spec_file) throws SPECCHIOFactoryException {
		
		try {
			return this.getDataCache().get_instrument_id_for_file(spec_file, 0, new SpecchioMessage());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	
	
	/**
	 * Get the picture associated with an instrument.
	 * 
	 * @param instrument_id	the instrument identifier
	 * 
	 * @return a table of pictures asscoiated with the instrument
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public PictureTable getInstrumentPictures(int instrument_id) throws SPECCHIOFactoryException {
		
		return getPictures(INSTRUMENT, instrument_id);
		
	}
	
	
	/**
	 * Get the pictures associated with an instrument or reference.
	 * 
	 * @param object_type	"instrument" or "reference"
	 * @param object_id		the instrument or reference identifier
	 * 
	 * @return a table of pictures associated with the instrument or reference
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */

	private PictureTable getPictures(String object_type, int object_id) throws SPECCHIOFactoryException {
		
		PictureTable pictures = new PictureTable();
		
		// work out table and column names
		String join_table = object_type + "_x_picture";
		String id_column = object_type + "_id";
		
		try {
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			String query = "select instrumentation_picture.instrumentation_picture_id,instrumentation_picture.caption,instrumentation_picture.image_data" +
					" from instrumentation_picture," + join_table + 
					" where instrumentation_picture.instrumentation_picture_id=" + SQL.prefix(join_table, "instrumentation_picture_id") +
					" and " + SQL.prefix(join_table, id_column) + "=" + Integer.toString(object_id);
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				Integer picture_id = rs.getInt(1);
				String caption = rs.getString(2);
				Blob image_data = rs.getBlob(3);
				Picture picture = new Picture(picture_id, object_id, caption, image_data.getBytes(1, (int)image_data.length()));
				pictures.put(picture_id, picture);
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
		return pictures;
		
	}
	
	
	/**
	 * Get the calibration metadata for an reference.
	 * 
	 * @param reference_id	the reference identifier
	 * 
	 * @return an array of calibration metadata objects for the reference
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public CalibrationMetadata[] getReferenceCalibrationMetadata(int reference_id) throws SPECCHIOFactoryException {
		
		return getCalibrationMetadata(REFERENCE, reference_id);
		
	}
	
	
	/**
	 * Get the picture associated with a reference.
	 * 
	 * @param reference_id	the reference identifier
	 * 
	 * @return a table of pictures asscoiated with the reference
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public PictureTable getReferencePictures(int reference_id) throws SPECCHIOFactoryException {
		
		return getPictures(REFERENCE, reference_id);
		
	}
	

	/**
	 * Get a Reference object from the database.
	 * 
	 * @param reference_id	the identifier of the reference to get
	 * 
	 * @return a new Reference object corresponding to reference_id
	 * 
	 * @throws SPECCHIOFactoryException	the reference does not exist
	 */
	public Reference getReference(int reference_id) throws SPECCHIOFactoryException {
		
		Reference ref = new Reference(reference_id);
		
		try {
			Statement stmt = getStatementBuilder().createStatement();
						
			// read information from database

			String query = "select r.name, r.serial_number, r.comments, rb.name, rt.name, r.institute_id, m.name " +
					"from reference r, reference_brand rb, manufacturer m, reference_type rt " +
					"where r.reference_brand_id = rb.reference_brand_id " +
					"and rb.manufacturer_id = m.manufacturer_id and rb.reference_type_id = rt.reference_type_id " +
					"and r.reference_id = " + Integer.toString(reference_id);
			
						
			int pos = 1;
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				ref.getReferenceName().value = rs.getString(pos++);
				ref.getSerialNumber().value = rs.getString(pos++);
				ref.getComments().value = rs.getString(pos++);
				ref.getBrandName().value = rs.getString(pos++);
				ref.getTypeName().value = rs.getString(pos++);
				ref.getReferenceOwner().value = getDataCache().get_institute_by_id(rs.getInt(pos++)).toString();
				ref.getManufacturer().value = rs.getString(pos++);
			}
			rs.close();		
			
			stmt.close();		
			
		} catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
		return ref;
	
	}
	
	
	/**
	 * Get all of the reference brands in the database.
	 * 
	 * @return an array of ReferenceBrands objects describing every brand in the database
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public ReferenceBrand[] getReferenceBrands() throws SPECCHIOFactoryException {
		
		List<ReferenceBrand> brands = new ArrayList<ReferenceBrand>();
		
		try {
			Statement stmt = getStatementBuilder().createStatement();
			String query = "select reference_brand_id, reference_type_id, manufacturer_id, name from reference_brand";
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				ReferenceBrand reference = new ReferenceBrand(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4));
				brands.add(reference);
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
		return brands.toArray(new ReferenceBrand[brands.size()]);
	
	}
	
	
	/**
	 * Get the descriptors of every reference in the database.
	 * 
	 * @return an array of ReferenceDescriptors identifying every reference in the database
	 * 
	 * @throws SPECCHIOFactoryException	could not access the database
	 */
	public ReferenceDescriptor[] getReferenceDescriptors() throws SPECCHIOFactoryException {
		
		List<ReferenceDescriptor> references = new ArrayList<ReferenceDescriptor>();
		
		try {
			Statement stmt = getStatementBuilder().createStatement();
			String query = "select reference_id, name from reference";
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				ReferenceDescriptor reference = new ReferenceDescriptor(rs.getInt(1), rs.getString(2));
				references.add(reference);
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
		return references.toArray(new ReferenceDescriptor[references.size()]);
	
	}
	
	
	/**
	 * Get a Sensor object from the database.
	 * 
	 * @param sensor_id	the identifier of the desired sensor
	 * 
	 * @return a Sensor object corresponding to sensor_id
	 * 
	 * @throws SPECCHIOFactoryException	the sensor does not exist
	 */
	public Sensor getSensor(int sensor_id) throws SPECCHIOFactoryException {
		
		return getDataCache().get_sensor(sensor_id);
		
	}
	
	
	/**
	 * Get descriptors of all of the sensors in the database. Note that this method
	 * does not load the full sensor definition; just enough information to
	 * identify the sensor. Use getSensor() to load the full definition.
	 * 
	 * @return an array of Sensor objects representing every sensor in the database
	 * 
	 * @throws SPECCHIOFactoryException	could not access the database
	 */
	public Sensor[] getSensorDescriptors() throws SPECCHIOFactoryException {
		
		List<Sensor> sensors = getDataCache().get_sensors();
		
		return sensors.toArray(new Sensor[sensors.size()]);
		
	}
	
	

	/**
	 * Insert an instrument or reference calibration into the database.
	 * 
	 * @param object_type	the type of object with which this calibration is associated ("instrument" or "reference")
	 * @param cal			the calibration object to be inserted
	 * 
	 * @return the identifier of the new calibration, or zero
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public int insertCalibration(String object_type, Calibration cal) throws SPECCHIOFactoryException {
		
		int cal_id = 0;
		String id_column = object_type + "_id";
		
		try {
			
			// insert the spectral file, if any, into the database
			
			Integer cal_factor_id = 0;
			Integer uncert_id = 0;
			SpectralFile spec_file = cal.getSpectralFile();
			if (spec_file != null) {
				cal_factor_id = insertInstrumentationFactors(spec_file, 0);
				
				if (cal.getIncludesUncertainty() && spec_file.getNumberOfSpectra() == 2)
				{
					uncert_id = insertInstrumentationFactors(spec_file, 1);
				}			
			}
			else
			{
				cal_factor_id = this.insertCalibrationFactors(cal);
			}
			
			int referenced_id = 0;
			
			if(object_type.equals("reference"))
			{
				referenced_id = cal.getReferenceId();
			}
			else
			{
				referenced_id = cal.getInstrumentId();
			}
			
			

			// update the calibration table
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			id_and_op_struct cal_factor = SQL.is_null_key_get_val_and_op(cal_factor_id);
			id_and_op_struct cal_uncert = SQL.is_null_key_get_val_and_op(uncert_id);
			String fov = (cal.getField_of_view() != 0)? Integer.toString(cal.getField_of_view()) : "null";
			String cal_type = (cal.getCalibration_type() != 0)? Integer.toString(cal.getCalibration_type()) : "null";

			
			String date = "null";
			if(cal.getCalibrationDate() != null)
			{
				DateTimeFormatter fmt = DateTimeFormat.forPattern(MetaDate.DEFAULT_DATE_FORMAT);
				date =  "'" + fmt.print((DateTime)cal.getCalibrationDate()) + "'";
			}
			
			String cal_no = (cal.getCalibrationNumber() != -1)? (new Integer(cal.getCalibrationNumber())).toString() : "null";
			
			
			String query = "insert into calibration (" + id_column + ", cal_factors, uncertainty, comments, calibration_no, fov, name, calibration_date, calibration_type) " +
					"values (" + referenced_id + "," + cal_factor.id + "," + cal_uncert.id + ", '" 
					+ cal.getComments() + "'," + cal_no + "," + fov  + ", '" 
					+ cal.getName() + "'" + ", " 
					+ date  + ", "  + cal_type +
					")";
			stmt.executeUpdate(query);
			
			// get the identifier of the new instrument
			query = "select last_insert_id()";
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				cal_id = rs.getInt(1);
			}
			rs.close();	
			stmt.close();
			
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
		return cal_id;
		
	}
	
	
	/**
	 * Insert an instrument into the database.
	 * 
	 * @param instrument_name	the name of the new instrument
	 * 
	 * @throws SPECCHIOFactoryException could not insert the new instrument
	 */
	public void insertInstrument(String instrument_name) throws SPECCHIOFactoryException {
		
		try {	
			Statement stmt = getStatementBuilder().createStatement();
			String query = "insert into instrument (name, sensor_id) " +
					"values ('" + instrument_name + "', (select sensor_id from sensor limit 1))";
			stmt.executeUpdate(query);
			stmt.close();
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	
	/**
	 * Insert an instrument into the database.
	 * 
	 * @param instr	instrument data structure
	 * 
	 * @throws SPECCHIOFactoryException could not insert the new instrument
	 */	
	public void insertInstrument(Instrument instr) throws SPECCHIOFactoryException {
		
		try {	
			Statement stmt = getStatementBuilder().createStatement();
			
			// update the instrument table
			String query = "insert into instrument (name, sensor_id, institute_id, serial_number) " +
					"values ('" + instr.getInstrumentName().get_value() + "', " + instr.getSensorId() + "," 
					+ getStatementBuilder().is_null_key_get_val_and_op(instr.getInstrumentId()).id + ",'" + instr.getInstrumentNumber().get_value() + "')";
			stmt.executeUpdate(query);
			
			// get the identifier of the new instrument
			query = "select last_insert_id()";
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				instr.setInstrumentId(rs.getInt(1));
			}
			rs.close();
			
			// update the cache
			getDataCache().add_instrument(instr);
			
			// clean up
			stmt.close();
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}				
		
	}
	
	
	/**
	 * Insert an instrument calibration into the database.
	 * 
	 * @param cal			the calibration
	 * @return id of the new calibration
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public int insertInstrumentCalibration(Calibration cal) throws SPECCHIOFactoryException {
		
		return insertCalibration(INSTRUMENT, cal);
		
	}
	
	/**
	 * Insert calibration factors into the database.
	 * 
	 * @param cal			the calibration
	 * @return id of the new calibration factor entry
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public int insertCalibrationFactors(Calibration cal) throws SPECCHIOFactoryException {
		
		Integer id = 0;
		
		try {
			// get the sensor id
			SpecchioMessage msg = new SpecchioMessage();
			int sensor_id = this.getInstrument(cal.getInstrumentId()).getSensorId();
			
			Statement stmt = getStatementBuilder().createStatement();
			String query = "INSERT INTO instrumentation_factors "
				+ "(loading_date, sensor_id, measurement_unit_id) "
				+ "VALUES (" + "current_timestamp, " + Integer.toString(sensor_id) + ", "
				+ cal.getMeasurement_unit_id() + ")";
			stmt.executeUpdate(query);
			ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
			while (rs.next())
				id = rs.getInt(1);
			rs.close();
			stmt.close();

			// insert the measurement blob
			query = "UPDATE instrumentation_factors set measurement = ? where instrumentation_factors_id = " + id.toString();
			PreparedStatement pstmt = getStatementBuilder().prepareStatement(query);

			InputStream refl = cal.getFactorsInputStream();
			pstmt.setBinaryStream(1, refl, cal.getFactors().length * 4);
			pstmt.executeUpdate();
			refl.close();
			pstmt.close();
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		catch (IOException ex) {
			// don't know why this would happen
			throw new SPECCHIOFactoryException(ex);
		}

		return id;		
		
	}	
	
	
	/**
	 * Insert an instrument picture into the database.
	 * 
	 * @param instrument_id	the instrument identifier
	 * @param image_data	the image data
	 * @param caption		the image's caption
	 * 
	 * @return the identifier of the new picture
	 *
	 * @throws SPECCHIOFactoryException	database error
	 */
	public int insertInstrumentPicture(int instrument_id, byte[] image_data, String caption) throws SPECCHIOFactoryException {
		
		return insertPicture(INSTRUMENT, instrument_id, image_data, caption);
		
	}
	
	
	/**
	 * Insert a spectral file into the instrumentation factors table.
	 * 
	 * @param spec_file	the file to be inserted
	 * @param spec_no	the index of the spectrum to be inserted
	 * 
	 * @return the identifier of the instrumentation factors that were inserted, or 0 if the sensor was not recognised
	 * 
	 * @throws SPECCHIOFactoryException database error
	 */
	public int insertInstrumentationFactors(SpectralFile spec_file, int spec_no) throws SPECCHIOFactoryException {
		
		Integer id = 0;
		
		try {
			// get the sensor id
			SpecchioMessage msg = new SpecchioMessage();
			int sensor_id = getDataCache().get_sensor_id_for_file(spec_file, spec_no, this.getDatabaseUserName(), msg);
			if (sensor_id == 0) {
				return 0;
			}
			
			Statement stmt = getStatementBuilder().createStatement();
			String query = "INSERT INTO instrumentation_factors "
				+ "(loading_date, sensor_id, measurement_unit_id) "
				+ "VALUES (" + "current_timestamp, " + Integer.toString(sensor_id) + ", "
				+ getDataCache().get_measurement_unit_id(spec_file.getMeasurementUnits(spec_no)) + ")";
			stmt.executeUpdate(query);
			ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
			while (rs.next())
				id = rs.getInt(1);
			rs.close();
			stmt.close();

			// insert the measurement blob
			query = "UPDATE instrumentation_factors set measurement = ? where instrumentation_factors_id = " + id.toString();
			PreparedStatement pstmt = getStatementBuilder().prepareStatement(query);

			InputStream refl = spec_file.getInputStream(spec_no);
			pstmt.setBinaryStream(1, refl, spec_file.getNumberOfChannels(spec_no) * 4);
			pstmt.executeUpdate();
			refl.close();
			pstmt.close();
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		catch (IOException ex) {
			// don't know why this would happen
			throw new SPECCHIOFactoryException(ex);
		}

		return id;
		
	}
	
	
	/**
	 * Insert a reference into the database.
	 * 
	 * @param reference_name	the name of the new reference
	 * 
	 * @return an empty string
	 * 
	 * @throws SPECCHIOFactoryException could not insert the new reference
	 */
	public void insertReference(String reference_name) throws SPECCHIOFactoryException {
		
		try {	
			Statement stmt = getStatementBuilder().createStatement();
			String query = "insert into reference (name)  values ('" + reference_name + "')";
			stmt.executeUpdate(query);
			stmt.close();
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	

	/**
	 * Insert an reference calibration into the database.
	 * 
	 * @param cal			the calibratino
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public void insertReferenceCalibration(Calibration cal) throws SPECCHIOFactoryException {
		
		insertCalibration(REFERENCE, cal);
		
	}
	
	
	/**
	 * Insert an reference picture into the database.
	 * 
	 * @param reference_id	the reference identifier
	 * @param image_data	the image data
	 * @param caption		the image's caption
	 * 
	 * @return the identifier of the new picture
	 *
	 * @throws SPECCHIOFactoryException	database error
	 */
	public int insertReferencePicture(int reference_id, byte[] image_data, String caption) throws SPECCHIOFactoryException {
		
		return insertPicture(REFERENCE, reference_id, image_data, caption);
		
	}
	
	
	/**
	 * Insert a picture into the database.
	 * 
	 * @param object_type	the type of object with which this picture is associated ("instrument" or "reference")
	 * @param object_id		the identifier of the object with which this picture is associated
	 * @param image_data	the image data
	 * @param caption		the image caption
	 * 
	 * @return the identifier of the new picture
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	private int insertPicture(String object_type, int object_id, byte[] image_data, String caption) throws SPECCHIOFactoryException {
		
		int picture_id = 0;
		
		// work out table and column names
		String join_table = object_type + "_x_picture";
		String id_column = object_type + "_id";
		
		try {
			String query;
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			
			// add the picture to the instrumentation_pictures table
			Blob image_blob = SQL.createBlob();
			image_blob.setBytes(1, image_data);
			query = "insert into instrumentation_picture(caption, image_data) values(?, ?)";
			PreparedStatement pstmt = SQL.prepareStatement(query);
			pstmt.setString(1, caption);
			pstmt.setBlob(2, image_blob);
			pstmt.executeUpdate();
			pstmt.close();
			
			// get the identifier assigned to the new picture
			query = "select last_insert_id()";
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				picture_id = rs.getInt(1);
			}
			rs.close();
			
			// add the picture to the join table
			query = "insert into " + join_table + "(instrumentation_picture_id," + id_column + ") " +
					"values(" + SQL.conc_ids(picture_id, object_id) + ")";
			stmt.executeUpdate(query);
			
			// clean up
			stmt.close();
			
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
		return picture_id;
		
	}
	
	/**
	 * Test for the existence of a calibration in the database.
	 * 
	 * @param cal		calibration object to check
	 * 
	 * @return true if the calibration already exists in the database, false otherwise
	 */	
	public boolean instrumentCalibrationExists(Calibration cal) {
		int cnt = 0;
		try {
			
			// read information from database
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();			

			String query = "select count(*) from calibration where instrument_id = " + cal.getInstrumentId() +
					" and calibration_no = " + cal.getCalibrationNumber() + " and fov " + (cal.getField_of_view() == 0?"is null": " = " + cal.getField_of_view()) + " and calibration_type = " + cal.getCalibration_type();
			
						
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				cnt = rs.getInt(1);
			}
			rs.close();		
			
			stmt.close();
			
		} catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
		if(cnt > 0)
			return true;
		else		
			return false;
	}
	
	
	
	/**
	 * Load a sensor definition from an input stream.
	 * 
	 * @param is	the input stream
	 * 
	 * @return the identifier of the new sensor
	 *
	 * @throws SPECCHIOFactoryException	the input stream cannot be parsed
	 * @throws IOException				the input stream cannot be read
	 */
	public int loadSensorDefinition(InputStream is) throws SPECCHIOFactoryException, IOException {
		
		String sensor_name, sensor_desc, company;
		int type_no, no_of_channels;
		int sensor_id = 0;
		
		Sensor s = new Sensor();
		
		String line;
		double[] wavelengths;

			// use buffered stream to read lines
			DataInputStream data_in = new DataInputStream(is);
			BufferedReader d = new BufferedReader(new InputStreamReader(data_in));
			
			// read header
			d.readLine(); // skip first line
			line=d.readLine();
			
			// tokenise
			String[] hdr = line.split("\t");
			int cnt = 0;
			
			sensor_name = hdr[cnt++];
			sensor_desc = hdr[cnt++];
			company = hdr[cnt++];
			type_no = Integer.valueOf(hdr[cnt++]);
			no_of_channels = Integer.valueOf(hdr[cnt++]);
			
			wavelengths = new double[no_of_channels];
			
			d.readLine(); // skip the body header
			
			cnt = 0;
			// read line by line
			while((line=d.readLine()) != null)
			{
				String[]tokens = line.split("\t");
				
				// second token is the wavelength				
				wavelengths[cnt++] = Double.valueOf(tokens[1]).doubleValue();			
			}

			
			// fill the sensor with the known data
			s.setName(sensor_name);
			s.setDescription(sensor_desc);
			s.setManufacturerName(company);
			s.setSensorTypeNumber(type_no);
			s.setNumberOfChannels(no_of_channels);
			s.setAverageWavelengths(wavelengths);
			
//			// check if manufacturer exists
//			int manufacturer_id = 0;
//			String query = "select manufacturer_id from manufacturer where name = '" + company + "' OR short_name = '" + company + "'";
//			ResultSet rs = stmt.executeQuery(query);
//			
//			while (rs.next()) {
//				manufacturer_id = rs.getInt(1);
//			}
//
//			if(manufacturer_id == 0) // insert manufacturer
//			{
//				String cmd = "insert into manufacturer (name, short_name) " +
//						"values (" + SQL.quote_string(company) + "," + SQL.quote_string(company) + ")";
//				stmt.executeUpdate(cmd);
//				 
//				rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
//				 
//				 while (rs.next())
//					 manufacturer_id = rs.getInt(1);	
//			}
//			
//			hdr[2] = Integer.toString(manufacturer_id); // replace had information
//			
//			// insert sensor			
//			 String cmd = "insert into sensor (name, description, manufacturer_id, sensor_type_no, no_of_channels) " +
//			 		"values (" + getStatementBuilder().conc_values(hdr) +
//			 ")";
//			 stmt.executeUpdate(cmd);
//			 rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
//			 while (rs.next())
//				 sensor_id = rs.getInt(1);
//			 rs.close();
			 
			 // insert all elements
//			 short sensor_element_type_id = 0;
//			 short sensor_element_types[] = new short[no_of_channels];
//			 query = "select sensor_element_type_id from sensor_element_type where code = 0";
//			 rs = stmt.executeQuery(query);
//			 while (rs.next()) {
//				 sensor_element_type_id = rs.getShort(1);
//			 }
//			 for(int i = 0; i < no_of_channels;i++)
//			 {
//				 // by default we assume narrow bands. Broadband channels must be defined manually in the database
//				 cmd = "insert into sensor_element (avg_wavelength, sensor_id, sensor_element_type_id) values (" + 
//				 Double.toString(wavelengths[i]) + ", " + Integer.toString(sensor_id) + ", " + Short.toString(sensor_element_type_id) + ")";
//				 stmt.executeUpdate(cmd);
//				 sensor_element_types[i] = sensor_element_type_id;
//			 }
//
//			 // clean up
//			 stmt.close();
			 
			 // add the new sensor to the cache
			sensor_id = insertSensor(s);
			
			
			 //Sensor s = new Sensor(sensor_id);
			s.setSensorId(sensor_id);
			
//			 s.setName(sensor_name);
//			 s.setDescription(sensor_desc);
//			 if (manufacturer_id != 0) {
//				 s.setManufacturerId(manufacturer_id);
//			 }
//			 s.setManufacturerName(company);
//			 s.setSensorTypeNumber(type_no);
			 
//			 s.setAverageWavelengths(wavelengths);
//			 s.setElementTypes(sensor_element_types);
			 getDataCache().add_sensor(s);

		
		return sensor_id;
	
	}
	
	
	/**
	 * Load a sensor definition from an input stream.
	 * 
	 * @param s	the sensor to insert. The passed sensor instance is updated.
	 * 
	 * @return the identifier of the new sensor
	 *
	 * @throws SPECCHIOFactoryException	error inserting the sensor
	 */
	public int insertSensor(Sensor s) throws SPECCHIOFactoryException{
		
		SQL_StatementBuilder SQL = getStatementBuilder();
		Statement stmt;
		int sensor_id = 0;
		
		
		try {
			stmt = SQL.createStatement();



			// check if manufacturer exists
			int manufacturer_id = 0;
			String query = "select manufacturer_id from manufacturer where name = '" + s.getManufacturerName().value + "' OR short_name = '" + s.getManufacturerName().value + "'";
			ResultSet rs = stmt.executeQuery(query);

			while (rs.next()) {
				manufacturer_id = rs.getInt(1);
			}

			if(manufacturer_id == 0) // insert manufacturer
			{
				String cmd = "insert into manufacturer (name, short_name) " +
						"values (" + SQL.quote_string(s.getManufacturerName().value) + "," + SQL.quote_string(s.getManufacturerName().value) + ")";
				stmt.executeUpdate(cmd);

				rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");

				while (rs.next())
					manufacturer_id = rs.getInt(1);	

			}

			s.setManufacturerId(manufacturer_id);

			// insert sensor			
			String cmd = "insert into sensor (name, description, manufacturer_id, sensor_type_no, no_of_channels) " +
					"values ('"  +
					s.getName().value +
					"', '" +
					s.getDescription().value +
					"', " +
					s.getManufacturerId() +
					", " +
					s.getSensorTypeNumber() +
					", " +
					s.getNumberOfChannels().value +
					")";
			stmt.executeUpdate(cmd);
			rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
			while (rs.next())
				sensor_id = rs.getInt(1);
			rs.close();
			
			s.setSensorId(sensor_id);

			// insert all elements
			short sensor_element_type_id = 0;
			short sensor_element_types[] = new short[s.getNumberOfChannels().value];
			query = "select sensor_element_type_id from sensor_element_type where code = 0";
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				sensor_element_type_id = rs.getShort(1);
			}

			for(int i = 0; i < s.getNumberOfChannels().value;i++)
			{
				// by default we assume narrow bands. Broadband channels must be defined manually in the database
				cmd = "insert into sensor_element (avg_wavelength, sensor_id, sensor_element_type_id) values (" + 
						s.getAverageWavelength(i) + ", " + Integer.toString(sensor_id) + ", " + Short.toString(sensor_element_type_id) + ")";
				stmt.executeUpdate(cmd);
				sensor_element_types[i] = sensor_element_type_id;
			}
			
			s.setElementTypes(sensor_element_types);

			// clean up
			stmt.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		return sensor_id;
		
	}
	
	
	/**
	 * Update calibration metadata.
	 * 
	 * @param cm		the new calibration metadata
	 *
	 * @throws SPECCHIOFactoryException	database error
	 */
	public void updateCalibrationMetadata(CalibrationMetadata cm) throws SPECCHIOFactoryException {
		
		try {
			SQL_StatementBuilder SQL = getStatementBuilder();
			
			// build a list of columns and values
			ArrayList<String> attr_and_vals = new ArrayList<String>(8);
			attr_and_vals.add("calibration_no"); attr_and_vals.add(Integer.toString(cm.getCalibrationNumber()));
			if (cm.getComments() != null) {
				attr_and_vals.add("comments");
				attr_and_vals.add(cm.getComments());
			}
			if (cm.getCalibrationDate() != null) {
				attr_and_vals.add("calibration_date");
				DateTimeFormatter fmt = DateTimeFormat.forPattern(MetaDate.DEFAULT_DATE_FORMAT);
				attr_and_vals.add(fmt.print(cm.getCalibrationDate()));
			}
			
			// update database
			Statement stmt = SQL.createStatement();
			String query =SQL.assemble_sql_update_query(
					SQL.conc_attr_and_vals("calibration", attr_and_vals.toArray(new String[attr_and_vals.size()])),
					"calibration",
					"calibration_id=" + cm.getCalibrationId()
				);
			stmt.executeUpdate(query);
			
			// clean up
			stmt.close();
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	
	
	/**
	 * Update a picture associated with an instrument.
	 * 
	 * @param picture	the new picture
	 * 
	 * @return an emptu string
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public void updateInstrumentPicture(Picture picture) throws SPECCHIOFactoryException {
		
		updatePicture(INSTRUMENT, picture);
		
	}
	
	
	/**
	 * Update an existing instrument.
	 * 
	 * @param instrument	the instrument
	 * 
	 * @throws SPECCHIOFactoryException database error
	 */
	public void updateInstrument(Instrument instrument) throws SPECCHIOFactoryException {
		
		try {
			SQL_StatementBuilder SQL = getStatementBuilder();
			
			// build a list of columns and values
			ArrayList<String> attr_and_vals = new ArrayList<String>(8);
			attr_and_vals.add("sensor_id"); attr_and_vals.add(Integer.toString(instrument.getSensorId()));
			attr_and_vals.add("serial_number"); attr_and_vals.add(instrument.getInstrumentNumber().toString());
			attr_and_vals.add("name"); attr_and_vals.add(instrument.getInstrumentName().toString());
			
			// get the identifier of the instrument's owner
			attr_and_vals.add("institute_id");
			Institute institute = getDataCache().get_institute_by_name(instrument.getInstrumentOwner().toString());
			if (institute != null) {
				attr_and_vals.add(Integer.toString(institute.getInstituteId()));
			} else {
				attr_and_vals.add("null");
			}
			
			// update database
			Statement stmt = SQL.createStatement();
			String query = SQL.assemble_sql_update_query(
					SQL.conc_attr_and_vals("instrument", attr_and_vals.toArray(new String[attr_and_vals.size()])),
					"instrument",
					"instrument_id=" + instrument.getInstrumentId()
				);
			stmt.executeUpdate(query);
			stmt.close();
		}
		catch (SQLException ex) {
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	
	
	/**
	 * Update a picture associated with a picture or a reference.
	 * 
	 * @param object_type	"instrument" or "reference"
	 * @param picture		the new picture
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	private void updatePicture(String object_type, Picture picture) throws SPECCHIOFactoryException {
		
		try {
			String query;
			SQL_StatementBuilder SQL = getStatementBuilder();
			
			// add the picture to the instrumentation_pictures table
			Blob image_blob = SQL.createBlob();
			image_blob.setBytes(1, picture.getImageData());
			query = "update instrumentation_picture set caption=?, image_data=? where instrumentation_picture_id=" + picture.getPictureId();
			PreparedStatement pstmt = SQL.prepareStatement(query);
			pstmt.setString(1, picture.getCaption());
			pstmt.setBlob(2, image_blob);
			pstmt.executeUpdate();
			pstmt.close();
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	
	
	/**
	 * Update a reference.
	 * 
	 * @param reference	the reference
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public void updateReference(Reference reference) throws SPECCHIOFactoryException {
		
		try {
			SQL_StatementBuilder SQL = getStatementBuilder();
			
			// build a list of columns and values
			ArrayList<String> attr_and_vals = new ArrayList<String>(10);
			attr_and_vals.add("serial_number"); attr_and_vals.add(reference.getSerialNumber().toString());
			attr_and_vals.add("name"); attr_and_vals.add(reference.getReferenceName().toString());
			attr_and_vals.add("comments");
			if (reference.getComments() != null) {
				attr_and_vals.add(reference.getComments().toString());
			} else {
				attr_and_vals.add("");
			}
			
			// get the identifier of the reference's owner
			Institute institute = getDataCache().get_institute_by_name(reference.getReferenceOwner().toString());
			if (institute != null) {
				attr_and_vals.add("institute_id");
				attr_and_vals.add(Integer.toString(institute.getInstituteId()));
			}
			
			// get the identifier of the reference's brand name
			ReferenceBrand brand = getDataCache().get_reference_brand_by_name(reference.getBrandName().toString());
			if (brand != null) {
				attr_and_vals.add("reference_brand_id");
				attr_and_vals.add(Integer.toString(brand.getReferenceBrandId()));
			}
			
			Statement stmt = SQL.createStatement();
			String query = SQL.assemble_sql_update_query(
					SQL.conc_attr_and_vals("reference", attr_and_vals.toArray(new String[attr_and_vals.size()])),
					"reference",
					"reference_id=" + reference.getReferenceId()
				);
			stmt.executeUpdate(query);
			stmt.close();
		}
		catch (SQLException ex) {
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	
	
	/**
	 * Update a picture associated with a reference.
	 * 
	 * @param picture	the new picture
	 * 
	 * @return an emptu string
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public void updateReferencePicture(Picture picture) throws SPECCHIOFactoryException {
		
		updatePicture(REFERENCE, picture);
		
	}







	
}
