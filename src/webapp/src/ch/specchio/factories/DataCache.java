package ch.specchio.factories;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.ListIterator;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import ch.specchio.constants.SensorElementType;
import ch.specchio.constants.SensorType;
import ch.specchio.eav_db.SQL_StatementBuilder;
import ch.specchio.spaces.MeasurementUnit;
import ch.specchio.types.Calibration;
import ch.specchio.types.Capabilities;
import ch.specchio.types.ConflictStruct;
import ch.specchio.types.Country;
import ch.specchio.types.Institute;
import ch.specchio.types.Instrument;
import ch.specchio.types.MetaDate;
import ch.specchio.types.ReferenceBrand;
import ch.specchio.types.Sensor;
import ch.specchio.types.SpecchioMessage;
import ch.specchio.types.SpectralFile;


public class DataCache {
	
	String datasource_name;
	SQL_StatementBuilder SQL;
	
	/** server capabilities and configurations */
	public Capabilities capabilities;
	
	
	ArrayList<Calibration> calibrations;
	ArrayList<Institute> institutes;
	ArrayList<Instrument> instruments;
	ArrayList<Sensor> sensors;
	ArrayList<MeasurementUnit> measurement_units;
//	ArrayList<GoniometerStruct> goniometers;
//	ArrayList<SamplingEnvironmentStruct> sampling_environments;	
//	ArrayList<BeamGeometryStruct> beam_geometries;
	ArrayList<ReferenceBrand> reference_brands;
	Hashtable<String, Integer> file_format_name_2_id_hash = new Hashtable<String, Integer>();
	
	
	public DataCache(SQL_StatementBuilder SQL, String datasource_name, Capabilities capabilities) throws SQLException
	{
		this.SQL = SQL;
		this.datasource_name = datasource_name;
		this.capabilities = capabilities;
		load_cache();
	}
	
	private void load_cache()
	{
		load_institutes();
		load_sensors();
		load_measurement_units();
		load_calibrations();
		load_instruments();		
		load_reference_brands();
		load_file_format_name_2_id_hash();
	}
	
	
	
	private void load_file_format_name_2_id_hash() {
		
		int file_format_id = -1;
		String name;
		
		try {
			Statement stmt = SQL.createStatement();
			String query = "SELECT name, file_format_id from file_format";
			ResultSet rs = stmt.executeQuery(query);	
			while (rs.next())
			{
				name = rs.getString(1);
				file_format_id = rs.getInt(2);
				file_format_name_2_id_hash.put(name, file_format_id);
				
			}
			rs.close();
			stmt.close();
			
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}

		
	}
	
	
	public void add_file_format_id(String name, int file_format_id)
	{
		file_format_name_2_id_hash.put(name, file_format_id);
	}
	
	
	public int get_file_format_id(String name)
	{
		
		Integer file_format_id = file_format_name_2_id_hash.get(name);
		
		if (file_format_id == null)
		{			
			file_format_id = -1; // the caller should insert a new file format (typically the spectral file factory)	
		}
				
		
		return file_format_id;
	}
	

	/**
	 * Load calibrations
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	private void load_calibrations() {
		
		calibrations = new ArrayList<Calibration> ();
		
		Calibration cal = null;
		
		try {
			Statement stmt = SQL.createStatement();

			String query = "select calibration_id, calibration_no,calibration_date,comments, cal_factors, uncertainty from calibration";
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				cal = new Calibration();
				cal.setCalibration_id(rs.getInt(1));
				cal.setCalibrationNumber(rs.getInt(2));
				DateTimeFormatter formatter = DateTimeFormat.forPattern(MetaDate.DEFAULT_DATE_FORMAT + ".S").withZoneUTC();
				String date_str = rs.getString(3);
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
					cal.setCalibrationDate(d);
				}
				cal.setComments(rs.getString(4));
				cal.setCalFactorsId(rs.getInt(5));
				cal.setUncertainty_id(rs.getInt(6));
				
				// get factors
				if (cal.getCalFactorsId() != 0)
				{
					InstrumentationStruct instr_cal_data = getInstrumentationVector(cal.getCalFactorsId());
					cal.setFactors(instr_cal_data.vector);
					cal.setMeasurement_unit_id(instr_cal_data.measurement_unit_id);
				}

				if (cal.getUncertainty_id() != 0)
				{
					InstrumentationStruct instr_cal_data = getInstrumentationVector(cal.getUncertainty_id());
					cal.setUncertainty(instr_cal_data.vector);
				}		
				
				
				calibrations.add(cal);
								
				
			}
			rs.close();
			stmt.close();
			

//			SpaceFactory sf = new SpaceFactory(this);
//			ArrayList<Integer> calibration_id_list = new ArrayList<Integer>();
//			calibration_id_list.add(cal.getCalFactorsId());
//			ArrayList<Space> spaces = sf.getSpacesForInstrumentationFactors(calibration_id_list);
//			
//			if (spaces.size() > 0) {
//				SpectralSpace ss = (SpectralSpace)spaces.get(0);
//				sf.loadSpace(ss);
//				cal.setFactors(ss.getVector(calibration_id));
//				
//				
//			}
//			
//			calibration_id_list.clear();
//			calibration_id_list.add(cal.getUncertainty_id());
//			spaces = sf.getSpacesForInstrumentationFactors(calibration_id_list);
//			
//			if (spaces.size() > 0) {
//				SpectralSpace ss = (SpectralSpace)spaces.get(0);
//				sf.loadSpace(ss);
//				cal.setUncertainty(ss.getVector(calibration_id));
//			}			
//			
//			// clean up
//			sf.dispose();			
			
			
		}
		catch (SQLException ex) {
			// database error
			ex.printStackTrace();
		} catch (SPECCHIOFactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}	
	

	
	
	private InstrumentationStruct getInstrumentationVector(int instrumentation_factor_id) throws SPECCHIOFactoryException
	{
		
		InstrumentationStruct istruct = new InstrumentationStruct();
		
		try {
			Statement stmt = SQL.createStatement();
			
			String query = "SELECT measurement_unit_id, measurement from instrumentation_factors " +
					"where instrumentation_factors_id = " + instrumentation_factor_id;
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				
				istruct.measurement_unit_id = rs.getInt(1);
				
				Blob measurement = rs.getBlob(2);
				InputStream binstream = measurement.getBinaryStream();
				DataInput dis = new DataInputStream(binstream);

				try {
					int dim = binstream.available() / 4;

					double[] vector = new double[dim];		

					for(int i = 0; i < dim; i++)
					{
						try {
							Float f = dis.readFloat();
							vector[i] = f.doubleValue();
						} catch (IOException e) {
							// don't know what would cause this
							e.printStackTrace();
						}				
					}		
					
					istruct.vector = vector;
					


				} catch (IOException e) {
					// dont't know what would cause this
					e.printStackTrace();
				}


				try {
					binstream.close();
				} catch (IOException e) {
					// don't know what woudl cause this
					e.printStackTrace();
				}
				
				
				

			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
		return istruct;
		
	}
			
	
	
	
	public void add_institute(Institute institute)
	{
		if (get_institute_by_id(institute.getInstituteId()) == null)
			institutes.add(institute);
	}
	
	
	public ArrayList<Institute> get_institutes()
	{
		return institutes;
	}
		
	
	
	public Institute get_institute_by_id(int institutes_id)
	{
		Institute inst = null;
		
		ListIterator<Institute> li = institutes.listIterator();
		
		boolean found = false;
		
		while(li.hasNext() && !found)
		{
			inst = li.next();
			
			if (inst.getInstituteId() == institutes_id) found = true;			
		}
		
		return (found)? inst : null;		
		
	}
	
	
	public Institute get_institute_by_name(String institute_name)
	{
		Institute inst = null;
		
		ListIterator<Institute> li = institutes.listIterator();
		
		boolean found = false;
		
		while(li.hasNext() && !found)
		{
			inst = li.next();
			
			if (inst.toString().equals(institute_name)) found = true;			
		}
		
		return found ? inst : null;		
		
	}




	private void load_institutes() {
		
		institutes = new ArrayList<Institute>();
		
		try {
			
			Statement stmt = SQL.createStatement();

			// read information from database
			String query = "select i.institute_id, i.name, i.department, i.street, i.street_no, i.po_code, i.city, i.www, c.country_id, c.name " +
					"from institute i left join country c on i.country_id = c.country_id";
					
					
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				Institute inst = new Institute(); 
				inst.setInstituteId(rs.getInt(1));
				inst.setInstituteName(rs.getString(2));
				inst.setDepartment(rs.getString(3));
				inst.setStreet(rs.getString(4));
				inst.setStreetNumber(rs.getString(5));
				inst.setPostOfficeCode(rs.getString(6));
				inst.setCity(rs.getString(7));
				inst.setWWWAddress(rs.getString(8));
				int country_id = rs.getInt(9);
				if (country_id != 0) {
					inst.setCountry(new Country(country_id, rs.getString(10)));
				}
				
				this.institutes.add(inst);
			}
			rs.close();
			
			// add statistics information
			for(Institute inst : institutes)
			{
				
				query = "select c.campaign_id, count(s.spectrum_id) from spectrum s, campaign c where s.campaign_id = c.campaign_id and "
						+ " c.research_group_id in "
						+ "(select distinct r.research_group_id from specchio_user u, research_group r, research_group_members m "
						+ "where institute_id = " + inst.getInstituteId() + " and u.user_id = m.member_id and r.research_group_id = m.research_group_id) group by c.campaign_id";
				
				
				int total_spectrum_cnt = 0;
				int campaign_cnt = 0;
				
				rs = stmt.executeQuery(query);
				while (rs.next()) {
					
					int campaign_id = rs.getInt(1);
					int spectrum_cnt = rs.getInt(2);
					total_spectrum_cnt += spectrum_cnt;
					campaign_cnt++;

				}
				
				inst.setNumber_of_campaigns(campaign_cnt);
				inst.setNumber_of_spectra_loaded(total_spectrum_cnt);
				
			}
			
			
			
			stmt.close();
						
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			

	}	
	
	
	
	public Instrument get_instrument(int instrument_id) throws SQLException
	{
		return get_instrument(instrument_id, 0);		
	}
	
	
	public Instrument get_instrument(int instrument_id, int calibration_id) throws SQLException
	{	
		Instrument instrument = null;
		Instrument s;
		// search 
		ListIterator<Instrument> li = instruments.listIterator();

		if(instrument_id != 0)
		{
			while(li.hasNext() && instrument == null)
			{
				s = li.next();
				if(calibration_id > 0)
				{
					if(s.getInstrumentId() == instrument_id && s.getCalibrationId() == calibration_id)
						instrument = s;
				}
				else // this is a case that should not happen as it means that some spectra got inserted with no proper calibration id ... but it appears to happen occasionally anyhow.
				{
					if(s.getInstrumentId() == instrument_id)
						instrument = s;
				}
			}
		}
				
		return instrument;
	}
	
	public ArrayList<Instrument> get_instrument_by_serial_id(String serial_id, int sensor_id) throws SQLException
	{
		ArrayList<Instrument> matching_instruments = new ArrayList<Instrument>();
		Instrument s;
		// search 
		ListIterator<Instrument> li = instruments.listIterator();

		while(li.hasNext())
		{
			s = li.next();
			if(s.getInstrumentNumber().get_value() != null && s.getInstrumentNumber().get_value().equals(serial_id) && s.getSensorId() == sensor_id)
				matching_instruments.add(s);			
		}

		return matching_instruments;				

	}	

	
	
	public Instrument get_instrument(String instrument_number, String company, String instrument_name) 
	{		
		Instrument instrument = null;
		Instrument s;
		// search through instrument list
		ListIterator<Instrument> li = instruments.listIterator();

		
			while(li.hasNext() && instrument == null)
			{
				s = li.next();
				if(s.getInstrumentNumber().get_value() != null && s.getInstrumentNumber().get_value().equals(instrument_number) && s.getSensor().getManufacturerShortName().get_value().equals(company))
				{
					if(instrument_name == null || instrument_name.equals(""))
						instrument = s;	
					else
						if(instrument_name != null && instrument_name.equals(s.getInstrumentName().value)) instrument = s;	
				}
							
			}

				
		return instrument;		

	}
	
	

	public Instrument get_instrument_id_for_file(SpectralFile spec_file, int spec_no, SpecchioMessage msg) throws SQLException, SPECCHIOFactoryException {
		
		Integer sensor_id = get_sensor_id_for_file(spec_file, spec_no, "", msg);
		
		Instrument instr = null;
		
		ArrayList<Instrument> instruments = get_instrument_by_serial_id(spec_file.getInstrumentNumber(), sensor_id);
		
		// the returned instruments may differ by their calibration. Next task is to find the one with the right calibration.
		if(instruments.size() > 0 && spec_file.getWvls().size() > 0) // check that the input file has got wavelengths defined
		{			
			double[] d_wvls = getAsDoubleVector(spec_file.getWvls(spec_no));

			ListIterator<Instrument> li = instruments.listIterator();

			while(li.hasNext() && instr == null)
			{
				Instrument i = li.next();
				wvl_matching_struct wvlm = full_wvls_matching(i.getCentreWavelengths(), d_wvls);
				
				if(wvlm.possible_match && wvlm.wvls_match)
				{
					instr = i;
				}					
			}
			
			// instrument exists but calibration does not yet exist
			if (instr == null)
			{

				int cal_id = insert_calibration(spec_file, spec_no, instruments.get(0));
				
				// create new instrument instance with values copied from existing instrument but with new calibration
				instr = new Instrument();

				Sensor s = this.get_sensor(sensor_id);

				instr.setInstrumentId(instruments.get(0).getInstrumentId());
				instr.setInstrumentName(instruments.get(0).getInstrumentName());
				instr.setSensorId(sensor_id);
				instr.setInstrumentNumber(instruments.get(0).getInstrumentNumber());
				instr.setSensor(s);
				instr.setAverageWavelengths(d_wvls); // same as newly inserted calibration
				instr.setCalibrationId(cal_id);


				// update cache with new instrument
				add_instrument(instr);

				// add info to the spectral file
				msg.setMessage("Added new calibration for instrument: " + instr.getInstrumentName().get_value());
				msg.setType(SpecchioMessage.INFO);				
				
			}

		}
		 
		if(instruments.size() > 0 && instr == null) // this is an instrument that uses the sensor blueprint (therefore has no calibrations), in this case, just pick the first in the list
		{			
			instr = instruments.get(0);
		}
		

		if(instr == null && spec_file.getInstrumentNumber() != null) // this means that we must insert it as new instrument
		{
			instr = insertNewInstrument(spec_file, spec_no, msg);
		}
		
		if(instr == null)
		{
			// try via centre wvls
			instr = get_instrument_by_centre_wvls(spec_file, spec_no, msg);
		}

		return instr;

	}
	
	
	private Instrument get_instrument_by_centre_wvls(SpectralFile spec_file, int spec_no, SpecchioMessage msg) throws SQLException, SPECCHIOFactoryException {
		
		Instrument instrument = null;
		Instrument i;

		ListIterator<Instrument> li = instruments.listIterator();
		//boolean wvls_match = true;
		boolean serial_no_match = false;
		
		// get wvls information
		Float[] wvls = null;
		double[] d_wvls = null;
		if(spec_file.getWvls().size() > 0)
		{
			wvls = spec_file.getWvls(spec_no);
			//d_wvls = new double[spec_file.getWvls(spec_no).length];
			//for (int j=0;j<d_wvls.length;j++) d_wvls[j] = wvls[j];
			d_wvls = getAsDoubleVector(wvls);
		}
		else
		{
			Integer sensor_id = get_sensor_id_for_file(spec_file, spec_no, "", msg);
			if (sensor_id != 0)
				d_wvls = this.get_sensor(sensor_id).getAverageWavelengths();
		}
		if (d_wvls == null || d_wvls.length == 0) {
			// no wavelengths to check
			return null;
		}
		
		while(li.hasNext() && instrument == null)
		{
			i = li.next();
			
			//System.out.println(i.getInstrumentName());
			
//			float wvl_err = 0.01f; // wvl error in nm allowed when looking for instruments: used to deal with rounding errors when data is read from DB again ...
//
//
//			// quick check: first and last band		
//			boolean no_of_bands_match = d_wvls.length == i.getNoOfBands();
//			boolean band_1_match = Math.abs(d_wvls[0] - i.getCentreWavelengths()[0]) < wvl_err;
//			boolean band_2_match = Math.abs(d_wvls[d_wvls.length-1] - i.getCentreWavelengths()[i.getNoOfBands()-1]) < wvl_err;
//			
//			boolean possible_match = no_of_bands_match & band_1_match & band_2_match;
//
//			if(possible_match) // do a full check
//			{
//				// check of the centre wavelengths match
//				int band = 0;
//				for(Double wvl : d_wvls)
//				{
//					wvls_match = wvls_match & (Math.abs(wvl - i.getCentreWavelengths()[band++])  < wvl_err);
//
//					if (wvls_match == false) break;
//				}				
//			}
			
			wvl_matching_struct wvlm = full_wvls_matching(i.getCentreWavelengths(), d_wvls);
			
			if(wvlm.possible_match && spec_file.getInstrumentNumber() != null &&
					!spec_file.getInstrumentNumber().equals("")) // check the serial number
			{
				if(spec_file.getInstrumentNumber().equals(i.getInstrumentNumber())) 
				{
					serial_no_match = true;
				}
				else
				{
					serial_no_match = false;
				}
			}

			if(wvlm.possible_match && wvlm.wvls_match && serial_no_match) instrument = i;
			
			if(wvlm.possible_match && wvlm.wvls_match && !serial_no_match)
			{
				instrument = i;
				// maybe issue a warning for the user that a matching instrument was found only based on wvls matching
			}


		}

		// insert as new instrument only if we have an instrument number
		// new approach: insert anyway and use the centre wvls as a fingerprint
//		if(instrument == null && spec_file.getInstrumentNumber() != null)
		if(instrument == null)
		{
			instrument = insertNewInstrument(spec_file, spec_no, msg);
		}
			
		
		return instrument;
	}
	
	
	class wvl_matching_struct
	{
		boolean wvls_match;
		boolean possible_match;
	}
	
	private wvl_matching_struct full_wvls_matching(double[] reference, double[] test)
	{
		float wvl_err = 0.01f; // wvl error in nm allowed when looking for instruments: used to deal with rounding errors when data is read from DB again ...

		boolean wvls_match = true;
		
		// quick check: first and last band		
		boolean no_of_bands_match = test.length == reference.length;
		boolean band_1_match = Math.abs(test[0] - reference[0]) < wvl_err;
		boolean band_2_match = Math.abs(test[test.length-1] - reference[reference.length-1]) < wvl_err;
		
		boolean possible_match = no_of_bands_match & band_1_match & band_2_match;

		if(possible_match) // do a full check
		{
			// check of the centre wavelengths match
			int band = 0;
			for(Double wvl : test)
			{
				wvls_match = wvls_match & (Math.abs(wvl - reference[band++])  < wvl_err);

				if (wvls_match == false) break;
			}				
		}
		
		wvl_matching_struct wvlm = new wvl_matching_struct();
		
		wvlm.wvls_match = wvls_match;
		wvlm.possible_match = possible_match;
		
		return wvlm;
	}
	
	
	
	private double[] getAsDoubleVector(Float[] wvls)
	{
		double[] d_wvls = null;
		d_wvls = new double[wvls.length];
		for (int j=0;j<d_wvls.length;j++) d_wvls[j] = wvls[j];

		return d_wvls;
	}
	
	
	private Instrument insertNewInstrument(SpectralFile spec_file, int spec_no, SpecchioMessage msg)
	{
		DateTimeFormatter formatter = DateTimeFormat.forPattern(MetaDate.DEFAULT_DATE_FORMAT);
		formatter.withZoneUTC();
		
		Instrument instr = null;		
		double[] d_wvls;
		Integer sensor_id;
		try {
			sensor_id = this.get_sensor_id_for_file(spec_file, spec_no, "", msg);		
			
				
			if(spec_file.getWvls().size() >= spec_no + 1)
			{
				d_wvls = getAsDoubleVector(spec_file.getWvls(spec_no));
			}
			else
			{
				// special case for e.g. ASD where the sensor is preloaded and all instruments have common wavelengths
				d_wvls = this.get_sensor(sensor_id).getAverageWavelengths();
			}

			if(sensor_id != 0)
			{					

				// connect to DB as admin to insert a new instrument

				InstrumentationFactory factory = new InstrumentationFactory(datasource_name);
				//factory.capabilities = this.capabilities;

				instr = new Instrument();

				Sensor s = this.get_sensor(sensor_id);

				String instrument_designator = "";

				if (spec_file.getInstrumentNumber() != null)
				{
					instrument_designator= " #" + spec_file.getInstrumentNumber();
				}

				if (spec_file.getInstrumentName() != null)
				{
					instrument_designator= spec_file.getInstrumentName();
				}		
				
				int l = instrument_designator.length();

				if (instrument_designator.length() == 0 && spec_file.getCaptureDate(spec_no) != null && spec_file.getCaptureDate(spec_no).toString() != null)
				{
	
					
					DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMdd");
					String str = fmt.print(spec_file.getCaptureDate(spec_no));

					instrument_designator = str + " (sample time)"; // try to augment with date of spectrum capture
				}

				if (instrument_designator.length() == 0)
				{
					//DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMdd");
					
					DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
					Calendar cal = Calendar.getInstance();
					//System.out.println(dateFormat.format(cal.getTime()));					
					String str = dateFormat.format(cal.getTime());
	
					instrument_designator = str  + " (insert time)"; // augment with current time/date
				}

				if (spec_file.getInstrumentName() != null)
				{
					instr.setInstrumentName(instrument_designator);
				}
				else
				{
					instr.setInstrumentName(s.getName().get_value() + " " + instrument_designator);
				}
				instr.setSensorId(sensor_id);
				instr.setInstrumentNumber(spec_file.getInstrumentNumber());
				instr.setSensor(s);
				instr.setAverageWavelengths(d_wvls);
				instr.setNewly_inserted(true);

				factory.insertInstrument(instr);					

				// create new calibration with wavelength calibration factors: only for instruments where wvls are not resampled to blueprint (e.g. ASD is always blueprint)
				if(!spec_file.has_standardised_wavelengths())
				{
					int cal_id = insert_calibration(spec_file, spec_no, instr);

					instr.setCalibrationId(cal_id);
				}

				factory.dispose();

				// update cache with new instrument
				add_instrument(instr);


				// add info to the spectral file
				msg.setMessage("Added new instrument: " + instr.getInstrumentName().get_value());
				msg.setType(SpecchioMessage.INFO);
			}


		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SPECCHIOFactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new SPECCHIOFactoryException(e);
		}

		return instr;
		
	}
	
	
	public int insert_calibration(SpectralFile spec_file, int spec_no, Instrument instr)
	{
		
		// connect to DB as admin to insert a new instrument

		InstrumentationFactory factory = new InstrumentationFactory(datasource_name);
		//factory.capabilities = this.capabilities;
		
		System.out.println(factory.getDatabaseUserName());
		
		// catch the case when data insert happens from a non-specchio file loader, like a Matlab process
		if (spec_file.getFilename() == null) spec_file.setFilename("external process");
		
		SpectralFile wvl_cal = new SpectralFile(spec_file);
		wvl_cal.setNumberOfSpectra(1);
		wvl_cal.setMeasurements(new Float[spec_file.getWvls(spec_no).length][1]);
		wvl_cal.setMeasurement(0, spec_file.getWvls(spec_no));
		wvl_cal.setWvls(spec_file.getWvls());
		wvl_cal.setNumberOfChannels(spec_file.getNumberOfChannels());
		
		// create a new units array as otherwise the original copied spectral file is modified!!!! 
		ArrayList<Integer> units = new ArrayList<Integer>();
		units.add(100); // wavelength code = 100
		wvl_cal.setMeasurementUnits(units); 
		
		wvl_cal.setInstrumentTypeNumber(spec_file.getInstrumentTypeNumber()); // needed for some files to identify the sensor

		Calibration cal = new Calibration();
		cal.setIncludesUncertainty(false);
		cal.setSpectralFile(wvl_cal);
		cal.setInstrumentId(instr.getInstrumentId());
		cal.setComments("Wavelength Calibration auto-generated by SPECCHIO file loader process");
		cal.setCalibration_type(Calibration.SPECTRAL_CALIBRATION);
		if(spec_file.getCalibrationSeries() != 0) cal.setCalibrationNumber(spec_file.getCalibrationSeries());
		DateTime cal_date = spec_file.getCalibration_date();
		if(spec_file.getCalibration_date() != null && cal_date.getMillis() > 0)
			{
				cal.setCalibrationDate(spec_file.getCalibration_date());
			}
		int cal_id = factory.insertInstrumentCalibration(cal);
		
		
		factory.dispose();
		
		return cal_id;
		
	}
	
	
	public void add_instrument(Instrument instr) throws SQLException
	{
		// only insert if instrument does not exist in cache ..
		Instrument tmp = get_instrument(instr.getInstrumentId(), instr.getCalibrationId());
		if (tmp == null)
			instruments.add(instr);
	}
	
	public void delete_instrument(int instrument_id) throws SQLException {
		
		ListIterator<Instrument> iter = instruments.listIterator();
		while (iter.hasNext()) {
			if (iter.next().getInstrumentId() == instrument_id) {
				iter.remove();
			}
		}
		
	}

	private void load_instruments() {
		
		instruments = new ArrayList<Instrument>();
		
		try {
			
			Statement stmt = SQL.createStatement();

			// read information from database
//			String[] tables = new String[]{"instrument"};
//			String[] attr = new String[]{"instrument_id", "name", "institute_id", "serial_number", "sensor_id"};
//
//			String query = SQL.assemble_sql_select_query(
//						SQL.conc_attributes(attr),
//						SQL.conc_tables(tables),
//						"");
			String query = "SELECT i.instrument_id, i.name, institute_id, serial_number, i.sensor_id, calibration_id FROM instrument as i left join calibration as c on i.instrument_id = c.instrument_id";
			
						
			ResultSet rs = stmt.executeQuery(query);
			
			while (rs.next()) {
				int i = 1;
				int instrument_id = rs.getInt(i++);
				String name = rs.getString(i++);
				int institute_id = rs.getInt(i++);
				String instr_no = rs.getString(i++);
				int sensor_id = rs.getInt(i++);
				int calibration_id = rs.getInt(i++);
				
				Instrument instr = load_instrument(instrument_id, name, institute_id, instr_no, sensor_id, calibration_id);
				this.instruments.add(instr);
				
			}
			rs.close();		

			stmt.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SPECCHIOFactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			

	}
	
	private Instrument load_instrument(int instrument_id2, String name, int institute_id,
			String instr_no2, int sensor_id2, int calibration_id) throws SPECCHIOFactoryException {

		Instrument instrument = new Instrument();
		
		instrument.setInstrumentId(instrument_id2);
		instrument.setInstrumentName(name);
		
		Institute inst = get_institute_by_id(institute_id);
		if (inst != null) {
			instrument.setInstrumentOwner(inst.toString());
		}
		
		instrument.setInstrumentNumber(instr_no2);
		
		instrument.setSensorId(sensor_id2);
		
		instrument.setSensor(get_sensor(sensor_id2));
		
		instrument.setCalibrationId(calibration_id);
		
		// get centre wavelengths from calibration if existing
		if (calibration_id != 0)
		{
			
			Calibration cal = getCalibration(calibration_id);
			
			MeasurementUnit unit = get_measurement_unit_via_id(cal.getMeasurement_unit_id());
			
			if (unit.getUnitName().equals("Wavelength"))
			{
				// use these factors as centre wavelengths for this instrument
				instrument.setAverageWavelengths(cal.getFactors());
				
			}
			
			
		}
		
		
		
		return instrument;
		
	}
	
	/**
	 * Get calibration.
	 * 
	 * @param calibration_id	the calibration identifier
	 * 
	 * @return calibration object
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public Calibration getCalibration(int calibration_id) throws SPECCHIOFactoryException {
		
		Calibration cal = null;
		Calibration c;
		
		if(calibration_id != 0)
		{
		
			// search through sensor list
			ListIterator<Calibration> li = calibrations.listIterator();

			while(li.hasNext() && cal == null)
			{
				c = li.next();
				if(c.getCalibration_id() == calibration_id)
					cal = c;			
			}
		}
		
		return cal;
		
	}	
		
	
	public void deleteCalibration(int calibration_id) throws SQLException {
		
		ListIterator<Calibration> iter = calibrations.listIterator();
		while (iter.hasNext()) {
			if (iter.next().getCalibration_id() == calibration_id) {
				iter.remove();
			}
		}
		
	}	
	
	
	
	public ArrayList<Sensor> get_sensors()
	{	
		return this.sensors;
	}
	
	public Sensor get_sensor(int sensor_id)
	{	
		Sensor sensor = null;
		Sensor s;
		
		if(sensor_id != 0)
		{
		
			// search through sensor list
			ListIterator<Sensor> li = sensors.listIterator();

			while(li.hasNext() && sensor == null)
			{
				s = li.next();
				if(s.getSensorId() == sensor_id)
					sensor = s;			
			}
		}
				
		return sensor;
	}
	
	
	public Sensor get_sensor_by_name(String sensor_name)
	{	
		Sensor sensor = null;
		Sensor s;
		
		if(sensor_name != null)
		{
		
			// search through sensor list
			ListIterator<Sensor> li = sensors.listIterator();

			while(li.hasNext() && sensor == null)
			{
				s = li.next();
				if(s.getName().value.equals(sensor_name))
					sensor = s;			
			}
		}
				
		return sensor;
	}	
	
	
	public Sensor get_sensor(Float[] wvls, String company)
	{
		//float nm_diff_threshold = 10.0f; // could make this configurable for better control
		Sensor sensor = null;
		Sensor s;
		//boolean wvls_match = true;
			// search through sensor list
			ListIterator<Sensor> li = sensors.listIterator();
			
			while(li.hasNext() && sensor == null)
			{
				s = li.next();
				
				if(s.getNumberOfChannels().value == wvls.length && s.getManufacturerShortName().get_value().equals(company))
				{
					// check of the centre wavelengths match
////					int band = 0;
////					for(Float wvl : wvls)
////					{
////						wvls_match = wvls_match & ((wvl - s.getAverageWavelengths()[band++]) < nm_diff_threshold);
////						
////						if (wvls_match == false) break;
////					}
				
					if(simple_wvls_match(s, wvls)) sensor = s;		

				}
					
					
					
			}
				
		return sensor;		
	}
	
	
	private boolean simple_wvls_match(Sensor s,Float[]  wvls)
	{

		float nm_diff_threshold = 10.0f; // could make this configurable for better control
		
		// quick check: first and last band	
		boolean wvls_match = (wvls[0] - s.getAverageWavelengths()[0]) < nm_diff_threshold & (wvls[wvls.length-1] - s.getAverageWavelengths()[s.getNumberOfChannels().get_value()-1]) < nm_diff_threshold;
		
		return 	wvls_match;		
	}
	
	
	// should also check if the wvls are matching!
	public Sensor get_sensor(Float[] wvls)
	{
		Sensor sensor = null;
		Sensor s;
		// search through sensor list
		ListIterator<Sensor> li = sensors.listIterator();

		while(li.hasNext() && sensor == null)
		{
			s = li.next();
			
			System.out.println(s.getName());
			
			wvl_matching_struct wvlm = full_wvls_matching(s.getAverageWavelengths(), getAsDoubleVector(wvls));
			
			if(wvlm.possible_match && wvlm.wvls_match)
			{
				sensor = s;		
				break;
			}
			
//			int no_of_sensor_bands = s.getNumberOfChannels().value;
//			if(no_of_sensor_bands == wvls.length)
//			{					
//				if(simple_wvls_match(s, wvls)) sensor = s;		
//			}
		}

		return sensor;		
	}
	
	
	public Sensor get_sensor(String company, int instrument_type_number)
	{
		Sensor sensor = null;
		Sensor s;
		
		if(instrument_type_number > -1) // -1 is the default value when undefined
		{
			
			// search through sensor list
			ListIterator<Sensor> li = sensors.listIterator();
	
			while(li.hasNext() && sensor == null)
			{
				s = li.next();
				if(s.getManufacturerShortName().get_value().equals(company) && s.getSensorTypeNumber() == instrument_type_number)
					sensor = s;			
			}
		}

		return sensor;		

	}
	
	public Sensor get_sensorByCompanyAndNoOfBands(String company, int number_of_bands)
	{
		Sensor sensor = null;
		Sensor s;
		
			// search through sensor list
			ListIterator<Sensor> li = sensors.listIterator();
	
			while(li.hasNext() && sensor == null)
			{
				s = li.next();
				if(s.getManufacturerShortName().get_value().equals(company) && s.getNumberOfChannels().get_value() == number_of_bands)
					sensor = s;			
			}

		return sensor;		

	}	
	
	// returns the sensor_id based on information read from the input file
	// or 'null' if sensor could not be found in the database
	public Integer get_sensor_id_for_file(SpectralFile spec_file, int spec_no, String username, SpecchioMessage msg) throws SQLException, SPECCHIOFactoryException {
		Integer sensor_id = 0;
		Sensor s = null;
		
		try {
			
			// we get a company in most cases, which helps identifyting sensors
			if(spec_file.getCompany() != null && !spec_file.getCompany().equals(""))
			{

				if (spec_file.getCompany().equals("APOGEE")) {

					s = get_sensor(spec_file.getWvls(0), spec_file.getCompany());

					return s.getSensorId();

				}

				if (spec_file.getCompany().equals("PP Systems" ) && (spec_file.getFileFormatName().equals("UniSpec_SPU"))) {

					s = get_sensor(spec_file.getWvls(0), spec_file.getCompany());

					return s.getSensorId();	

				}


				if (spec_file.getCompany().equals("COST_OO_CSV")) {

					s = get_sensor(new Float[spec_file.getNumberOfChannels(0)], "OceanOptics");

					return s.getSensorId();

				}

				// last case: for ASD calibration files where the instrument type number
				// is set to zero for the *.ILL and *.REF files
				if (spec_file.getCompany().equals("ASD") && spec_file.getInstrumentTypeNumber() == 0) {
					s = get_sensorByCompanyAndNoOfBands(spec_file.getCompany(), spec_file.getNumberOfChannels(spec_no));
					if (s == null)
						return sensor_id; // "null"
					else
						return s.getSensorId();
				}

				// get sensor id via the instrument if instrument number is defined
				if (spec_file.getInstrumentNumber() != null) {

					Instrument i = get_instrument(spec_file.getInstrumentNumber(), spec_file.getCompany(), spec_file.getInstrumentName());

					if (i != null)
					{
						return i.getSensor().getSensorId();
					}

				}

				s = get_sensor(spec_file.getCompany(), spec_file.getInstrumentTypeNumber());
				if (s != null)
					return s.getSensorId();
				else
				{
					s = get_sensor(spec_file.getWvls(spec_no)); // fallback option: wrong selection can happen if the blueprints of two different companies 
					
					// check if company names are matching ...
					String spec_file_company = spec_file.getCompany();
					if(s != null && !s.getManufacturerShortName().get_value().equals(spec_file_company))
					{
						s = null;
					}
				}
			}
			else
			{
				// file without a company
				// find via number of channel and wvls matching
				s = get_sensor(spec_file.getWvls(spec_no));

			}
			
	
			return s.getSensorId();
			//return (s != null)? s.getSensorId() : 0;
			
		}
		catch(NullPointerException e)
		{
			
			// we got an unknown sensor at hand
			// at this point we auto-insert a new sensor
			
			
			// TODO Insert Sensor
			InstrumentationFactory factory = new InstrumentationFactory(datasource_name);
			
			s = new Sensor();
			
			String range = "(" + spec_file.getWvls(spec_no)[0] + "-" + spec_file.getWvls(spec_no)[spec_file.getWvls(spec_no).length-1] + " [nm])";
			
			if (!spec_file.getCompany().equals(""))
			{
				s.setName(spec_file.getCompany() + ", " + spec_file.getWvls(spec_no).length + " bands " + range);
			}
			else
			{
				s.setName("Unknown Sensor, " + spec_file.getWvls(spec_no).length + " bands " + range);
			}
			
			if(spec_file.getFileFormatName() != null)
				s.setDescription("Auto-Inserted by SPECCHIO based on "  + spec_file.getFileFormatName() + " file by " + username);
			else
				s.setDescription("Auto-Inserted by SPECCHIO, initiated by " + username);
			s.setManufacturerName(spec_file.getCompany());
			s.setManufacturerShortName(spec_file.getCompany());
			s.setSensorTypeNumber(spec_file.getInstrumentTypeNumber());
			s.setNumberOfChannels(spec_file.getWvls(spec_no).length); // safer than calling NumberOfChannels, as some file readers do not set it properly
			
			// convert to double: the sensor entity should be subjected to major DB revision
			Float[] wvls = spec_file.getWvls(spec_no);
			double[] d_wvls = new double[spec_file.getWvls(spec_no).length];
			for (int j=0;j<d_wvls.length;j++) d_wvls[j] = wvls[j];
			
			s.setAverageWavelengths(d_wvls);		
			
			sensor_id = factory.insertSensor(s);
			
			factory.dispose();
			
			add_sensor(s);
			
			// add info to the spectral file
			msg.setMessage("Added new sensor: " + s.getName().value);
			msg.setType(SpecchioMessage.INFO);			
						
			
			return sensor_id;
		}

	}
	
	
	public void add_sensor(Sensor s) {
		
		if (get_sensor(s.getSensorId()) == null) {
			this.sensors.add(s);
		}
		
	}
	
	
	private void load_sensors() {
		
		sensors = new ArrayList<Sensor>();
		
		try {
			
			Statement stmt = SQL.createStatement();

			// read information from database

			String[] tables = new String[]{"sensor"};
			String[] attr = new String[]{"sensor_id", "name", "no_of_channels", "description", "manufacturer_id", "sensor_type_no"};

			String query = SQL.assemble_sql_select_query(
						SQL.conc_attributes(attr),
						SQL.conc_tables(tables),
						"");
			
						
			ResultSet rs = stmt.executeQuery(query);
			
			while (rs.next()) {
				int i = 1;
				int  sensor_id = rs.getInt(i++);
				String name = rs.getString(i++);
				int no_of_channels = rs.getInt(i++);
				String description = rs.getString(i++);
				int manufacturer_id = rs.getInt(i++);
				int sensor_type_no = rs.getInt(i++);
				
				Sensor sensor = load_sensor(sensor_id, name, no_of_channels, description, manufacturer_id, sensor_type_no);
				this.sensors.add(sensor);
				
			}
			rs.close();
			
			stmt.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			

	}
	
	
	private Sensor load_sensor(int sensor_id2, String name2, int no_of_channels2, String description,
			int manufacturer_id2, int sensor_type_no) throws SQLException {
		
		Sensor sensor = new Sensor();
		
		sensor.setSensorId(sensor_id2);
		sensor.setName(name2);
		sensor.setNumberOfChannels(no_of_channels2);
		sensor.setDescription(description);
		sensor.setManufacturerId(manufacturer_id2);
		sensor.setSensorTypeNumber(sensor_type_no);
		
		sensor.setValid(true);
		
		load_sensor_definition(sensor);
		
		return sensor;
		
	}

	private void load_sensor_definition(Sensor sensor) throws SQLException
	{		
		int element_type_sum = 0;
			
		sensor.setAverageWavelengths(new double[sensor.getNumberOfChannels().value]);
		sensor.setElementTypes(new short[sensor.getNumberOfChannels().value]);
			
		// get sensor elements info
		String query = "select se.avg_wavelength, sety.code from sensor_element se, sensor_element_type sety where " +
				"se.sensor_element_type_id = sety.sensor_element_type_id and " +
				"sensor_id = " + Integer.toString(sensor.getSensorId()) + " order by sensor_element_id";
			
		Statement stmt = SQL.createStatement();
		ResultSet rs = stmt.executeQuery(query);
			
		int i = 0;
		while (rs.next()) {
			sensor.setAverageWavelength(i, rs.getFloat(1));
			sensor.setElementType(i, rs.getShort(2));
			element_type_sum += sensor.getElementType(i);
			i++;
		}
		rs.close();

		java.text.DecimalFormat df = new java.text.DecimalFormat("###.####");
		sensor.setWavelengthRange(
				df.format(sensor.getAverageWavelength(0)) +
				" - " +
				df.format(sensor.getAverageWavelength(sensor.getNumberOfChannels().value - 1))
			);
			
		// get manufacturer info
		query = "select name, www, short_name from manufacturer where manufacturer_id = " + Integer.toString(sensor.getManufacturerId());
		rs = stmt.executeQuery(query);
		while (rs.next()) {
			sensor.setManufacturerName(rs.getString(1));
			sensor.setManufacturerWWW(rs.getString(2));
			sensor.setManufacturerShortName(rs.getString(3));
		}
		rs.close();	
			
		// check if it is a pure instrument (i.e. narrow or broadband only)
		// the check on the sum works because only codes of 0 and 1 are possible
		// thus any value between indicates a mixed instrument
		if(element_type_sum > 0 && element_type_sum <= sensor.getNumberOfChannels().value)
		{
			sensor.setSensorType(SensorType.NB_BB);
		}
		else
		{
			if(element_type_sum == SensorElementType.NB)
				sensor.setSensorType(SensorElementType.NB);
			else
				sensor.setSensorType(SensorElementType.BB);	
		}
		
		stmt.close();
		
	}
	
	
	
	public int get_measurement_unit_id(int ASD_coding)
	{			
		return get_measurement_unit(ASD_coding).getUnitId();						
	}
	
	
	public MeasurementUnit get_measurement_unit(int ASD_coding)
	{	
		MeasurementUnit s = null;
			// search through measurement unit list
			ListIterator<MeasurementUnit> li = measurement_units.listIterator();

			while(li.hasNext())
			{
				s = li.next();
				if(s.getUnitNumber() == ASD_coding)
					break;			
			}
		
		return s;
	}

	public MeasurementUnit get_measurement_unit(String unit_name)
	{
		MeasurementUnit s = null;
		// search through measurement unit list
		ListIterator<MeasurementUnit> li = measurement_units.listIterator();

		while(li.hasNext())
		{
			s = li.next();
			if(s.getUnitName().equals(unit_name))
				break;
		}

		return s;
	}
	
	public MeasurementUnit get_measurement_unit_via_id(int measurement_unit_id)
	{	
		MeasurementUnit s = null;
			// search through measurement unit list
			ListIterator<MeasurementUnit> li = measurement_units.listIterator();

			while(li.hasNext())
			{
				s = li.next();
				if(s.getUnitId() == measurement_unit_id)
					break;			
			}
		
		return s;
	}
	

	public Integer get_measurement_unit_id_for_file(SpectralFile spec_file, int index) throws SQLException {
		
		Integer measurement_units = spec_file.getMeasurementUnits(index);
		int id = get_measurement_unit_id(measurement_units);
		//return (measurement_units != 0)? get_measurement_unit_id(measurement_units) : 0;
		return id;
		
	}

//	public String get_measurement_type_id_for_file(SpectralFile spec_file, int spec_no) {
//		
//		int measurement_type_id = get_measurement_type_id(Integer.toString(spec_file.getMeasurementType(spec_no)));
//		
//		return SQL.is_null_key_get_val_and_op(measurement_type_id).id;
//		
//	}
	
	
	
	private void load_measurement_units() {
		
		measurement_units = new ArrayList<MeasurementUnit>();
		
		try {
			
			Statement stmt = SQL.createStatement();

			// read information from database

			String query = "select measurement_unit_id, ASD_coding, name from measurement_unit";

			ResultSet rs;
			rs = stmt.executeQuery(query);

			while (rs.next()) {
				int measurement_unit_id = rs.getInt(1);
				int ASD_coding = rs.getInt(2);
				String name = rs.getString(3);

				MeasurementUnit mu = new MeasurementUnit();
				mu.setUnitId(measurement_unit_id);
				mu.setUnitNumber(ASD_coding);
				mu.setUnitName(name);

				measurement_units.add(mu);
			}

			rs.close();	
			stmt.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			

	}

	
//	public int get_goniometer_id(String name)
//	{
//		GoniometerStruct o = null;
//			// search through sensor list
//			ListIterator<GoniometerStruct> li = goniometers.listIterator();
//
//			while(li.hasNext())
//			{
//				o = li.next();
//				if(o.name.equals(name))
//					break;			
//			}
//			
//				
//		return o.goniometer_id;				
//		
//	}	
	
	
//	private void load_goniometers() {
//		
//		goniometers = new ArrayList<GoniometerStruct>();
//		
//		try {
//			
//			Statement stmt = SQL.createStatement();
//
//			// read information from database
//			String query = "select goniometer_id, name from goniometer";
//
//			
//						
//			ResultSet rs = stmt.executeQuery(query);
//			
//			while (rs.next()) {
//				int i = 1;
//				GoniometerStruct g = new GoniometerStruct();
//				g.goniometer_id = rs.getInt(i++);
//				g.name = rs.getString(i++);
//
//				this.goniometers.add(g);
//				
//			}
//			rs.close();		
//			stmt.close();
//						
//			
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}			
//
//	}		
	
	
//	public int get_sampling_environment_id(String name)
//	{
//		SamplingEnvironmentStruct o = null;
//			// search through sensor list
//			ListIterator<SamplingEnvironmentStruct> li = sampling_environments.listIterator();
//
//			while(li.hasNext())
//			{
//				o = li.next();
//				if(o.name.equals(name))
//					break;			
//			}
//			
//				
//		return o.sampling_environment_id;				
//		
//	}	
	
	
//	private void load_sampling_environments() {
//		
//		sampling_environments = new ArrayList<SamplingEnvironmentStruct>();
//		
//		try {
//			
//			Statement stmt = SQL.createStatement();
//
//			// read information from database
//			String query = "select sampling_environment_id, name from sampling_environment";
//
//			
//						
//			ResultSet rs = stmt.executeQuery(query);
//			
//			while (rs.next()) {
//				int i = 1;
//				SamplingEnvironmentStruct o = new SamplingEnvironmentStruct();
//				o.sampling_environment_id = rs.getInt(i++);
//				o.name = rs.getString(i++);
//
//				this.sampling_environments.add(o);
//				
//			}
//			rs.close();		
//
//			stmt.close();
//			
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}			
//
//	}		
	
//	public int get_measurement_type_id(String beam_geometry)
//	{
//		if (beam_geometry.equals("0")) return 0;
//		
//		BeamGeometryStruct s = null;
//			// search through sensor list
//			ListIterator<BeamGeometryStruct> li = beam_geometries.listIterator();
//
//			while(li.hasNext())
//			{
//				s = li.next();
//				if(s.name.contains(beam_geometry))
//					break;			
//			}
//			
//				
//		return s.measurement_type_id;				
//		
//	}
	
	
	
//	private void load_measurement_types() {
//		
//		beam_geometries = new ArrayList<BeamGeometryStruct>();
//
//		try {
//
//			Statement stmt = SQL.createStatement();
//
//			// read information from database
//			String query = "select measurement_type_id, name from measurement_type";
//
//			ResultSet rs;
//			rs = stmt.executeQuery(query);
//
//			while (rs.next()) {
//				BeamGeometryStruct o = new BeamGeometryStruct();
//
//				o.measurement_type_id = rs.getInt(1);
//				o.name = rs.getString(2);
//
//				beam_geometries.add(o);
//			}
//
//			rs.close();
//			stmt.close();
//
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}			
//
//	}
	
	
	public ReferenceBrand get_reference_brand_by_name(String reference_brand_name)
	{
		ReferenceBrand brand = null;
		
		ListIterator<ReferenceBrand> li = reference_brands.listIterator();
		
		boolean found = false;
		
		while(li.hasNext() && !found)
		{
			brand = li.next();
			
			if (brand.toString().equals(reference_brand_name)) found = true;			
		}
		
		return brand;		
		
	}
	
	
	private void load_reference_brands() {
		
		reference_brands = new ArrayList<ReferenceBrand>();
		
		try {
			
			Statement stmt = SQL.createStatement();

			// read information from database
			String[] tables = new String[]{"reference_brand"};
			String[] attr = new String[]{"reference_brand_id", "reference_type_id", "manufacturer_id", "name"};

			String query = SQL.assemble_sql_select_query(
						SQL.conc_attributes(attr),
						SQL.conc_tables(tables),
						"");
			
						
			ResultSet rs = stmt.executeQuery(query);
			
			while (rs.next()) {
				int i = 1;
				int reference_brand_id = rs.getInt(i++);
				int reference_type_id = rs.getInt(i++);
				int manufacturer_id = rs.getInt(i++);
				String name = rs.getString(i++);
				
				ReferenceBrand brand = new ReferenceBrand(reference_brand_id, reference_type_id, manufacturer_id, name);
				this.reference_brands.add(brand);
				
			}
			rs.close();		

			stmt.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			

	}
	

}


class GoniometerStruct
{
	public String name;
	public int goniometer_id;
}

class SamplingEnvironmentStruct
{
	public String name;
	public int sampling_environment_id;
}

class BeamGeometryStruct
{	
	public String name;
	public int measurement_type_id;	
}


class InstrumentationStruct
{
	public double[] vector;
	public int measurement_unit_id;
}

