package ch.specchio.file.reader.spectrum;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOPreferencesStore;
import ch.specchio.file.reader.campaign.SpecchioCampaignDataLoader;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.MetaParameterFormatException;
import ch.specchio.types.SpecchioMessage;
import ch.specchio.types.SpectralFile;
import ch.specchio.types.spatial_pos;
import ch.specchio.types.MetaDate;

public abstract class JB_FileLoader extends SpectralFileLoader {
	
	boolean is_fluoresence_sensor = false;
	boolean valid_gps_data_found = false;
	String voltage;
	Integer first_valid_gps_time_index = -1;
	
	String spectrum_number, spectrum_number_ext, gps_time, gps_date, lat, lon;
	spatial_pos pos;
	DateTime utc;

	private ArrayList<Float> wvls_fluorescence = new ArrayList<Float>();
	private ArrayList<Float> up_coef_fluorescence = new ArrayList<Float>();
	private ArrayList<Float> dw_coef_fluorescence = new ArrayList<Float>();
	private ArrayList<Float> wvls_broadrange = new ArrayList<Float>();
	private ArrayList<Float> up_coef_broadrange = new ArrayList<Float>();
	private ArrayList<Float> dw_coef_broadrange = new ArrayList<Float>();
	

	public JB_FileLoader(String file_format_name, SPECCHIOClient specchio_client,
			SpecchioCampaignDataLoader campaignDataLoader) {
		super(file_format_name, specchio_client, campaignDataLoader);
		// TODO Auto-generated constructor stub
	}

	@Override
	public SpectralFile load(File file) throws IOException, MetaParameterFormatException {
		// TODO Auto-generated method stub
		return null;
	}

	protected boolean isGPSValid()
	{
		if(getGPSPos() == null)
			return false;
		else
		{
			valid_gps_data_found = true;
			return true;			
		}
	}
	
	protected void setCalibration_date(String string)
	{
		Integer mday = Integer.valueOf(string.substring(6,8));
		Integer month = Integer.valueOf(string.substring(4,6)); 
		Integer year = Integer.valueOf(string.substring(0,4));
		
		DateTime dt = new DateTime(year, month, mday, 0, 0);
		spec_file.setCalibration_date(dt);	
		setCalibration_date(dt);
		
	}
	
	
	protected void getCalibrationData()
	{
	
		
		File cal_file = this.campaignDataLoader.getFlox_rox_cal_file();
		boolean cal_file_not_found = false;
		
		// try to get cal file from preferences if they are not available alongside the FloX data files
		if(cal_file == null)
		{
			
			try {
				SPECCHIOPreferencesStore prefs = new SPECCHIOPreferencesStore();
				
				if(is_fluoresence_sensor)
					cal_file = new File(prefs.getStringPreference("FLOX_CAL_FILE"));
				else
					cal_file = new File(prefs.getStringPreference("ROX_CAL_FILE"));
				
				
			} catch (BackingStoreException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(cal_file != null && cal_file.exists())
		{

			BufferedReader bufferedReader;
			try {
				bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(cal_file), StandardCharsets.UTF_8));

			//BufferedReader reader = Files.newBufferedReader(Paths.get(cal_file.getAbsolutePath()));	
			CSVParser csvParser = new CSVParser(bufferedReader, CSVFormat.newFormat(';'));
			//csvRecords = csvParser.getRecords();
			
			CSVRecord Line1 = csvParser.iterator().next();
			String first_token = Line1.get(0);
			//String scnd_token = Line1.get(1);
			
			first_token = first_token.replaceAll("\\p{Punct}","");  // strip high commata from newer cal file strings ... (2019-10-04)
			
			if(first_token.equals("wl_F")) // RoX cal file format: F stands for full range
			{
				int i=0;

				for (CSVRecord r : csvParser) {
				
					if(i>0)
					{					
						wvls_broadrange.add(Float.valueOf(r.get(0)));
						up_coef_broadrange.add(Float.valueOf(r.get(1)));
						dw_coef_broadrange.add(Float.valueOf(r.get(2)));
					}
					i++;
					
					if(r.getRecordNumber()==4)
					{
						setCalibration_date(r.get(6));
						Integer cal_number = Integer.getInteger(r.get(6));
						spec_file.setCalibrationSeries(cal_number);
					}

				}
				

				
				spec_file.addWvls(new Float[wvls_broadrange.size()]);
				spec_file.setWvls(0, wvls_broadrange.toArray(spec_file.getWvls(0)));	
			}
			else if(first_token.equals("wl")) // FloX cal file format (two spectrometers)
			{
				int i=0;

				for (CSVRecord r : csvParser) {

					// FLUO
					wvls_fluorescence.add(Float.valueOf(r.get(0)));
					up_coef_fluorescence.add(Float.valueOf(r.get(1)));
					dw_coef_fluorescence.add(Float.valueOf(r.get(2)));
					// FULL
					wvls_broadrange.add(Float.valueOf(r.get(3)));
					up_coef_broadrange.add(Float.valueOf(r.get(4)));
					dw_coef_broadrange.add(Float.valueOf(r.get(5)));
					i++;
					
					if(r.getRecordNumber()==4)
					{
						setCalibration_date(r.get(6).replaceAll("\\p{Punct}",""));
						Integer cal_number = Integer.parseInt(r.get(6).replaceAll("\\p{Punct}",""));
						spec_file.setCalibrationSeries(cal_number);
					}
					
				}
				
				if (is_fluoresence_sensor)
				{
					spec_file.addWvls(new Float[wvls_fluorescence.size()]);
					spec_file.setWvls(0, wvls_fluorescence.toArray(spec_file.getWvls(0)));						
				}
				else
				{
					spec_file.addWvls(new Float[wvls_broadrange.size()]);
					spec_file.setWvls(0, wvls_broadrange.toArray(spec_file.getWvls(0)));					
				}

				csvParser.close();
				
			}				
			else
				cal_file_not_found = true;
			
			
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		

		}
		else
		{
			cal_file_not_found = true;			
		}
		
		
		if(cal_file_not_found)
		{
			
			// output file error
			spec_file.setFileErrorCode(SpectralFile.UNRECOVERABLE_ERROR);
			ArrayList<SpecchioMessage> file_errors = spec_file.getFileErrors();
			if(file_errors == null)
			{
				file_errors = new ArrayList<SpecchioMessage>();						
			}

			file_errors.add(new SpecchioMessage("No calibration file (cal.csv) could be found alongside the spectral file.", SpecchioMessage.ERROR));
			spec_file.setFileErrors(file_errors);
			
		}
				
	}
	
	
	protected spatial_pos getGPSPos()
	{

		spatial_pos pos_ = new spatial_pos();
		String[] lat_tokens = lat.split(" ");
		int lat_sign = 1;
		if(lat_tokens.length>1)
		{
			if(lat_tokens[1].equals("N"))
			{
				// remains positive
			}
			else
				lat_sign = -1;
		}

		String[] lon_tokens = lon.split(" ");
		int lon_sign = 1;
		if(lon_tokens.length>1)
		{
			if(lon_tokens[1].equals("E"))
			{
				// remains positive
			}
			else
				lon_sign = -1;
		}					

		try {
			pos_.latitude = Double.valueOf(lat_tokens[0]) * lat_sign;
			pos_.longitude = Double.valueOf(lon_tokens[0]) * lon_sign;

			if(pos_.latitude == 0.0f && pos_.longitude == 0.0f)
			{
				return null;
			}
			
			pos = pos_; // this is a valid location

		} catch (NumberFormatException ex) {
			// not a number at all
			return null;
		}

		return pos;

	}
	
	protected DateTime getUTC()
	{
		DateTime dt = null;
		Integer year, month, mday, hour=0, min=0, sec=0;
		
		
		
		// special case handling for the GPS date (according to R code from JB Hyperspectral)
		
		if(gps_date.length() == 5)
		{
			year = Integer.valueOf(gps_date.substring(3, 5));
			month = Integer.valueOf(gps_date.substring(1, 3));
			mday = Integer.valueOf(gps_date.substring(0, 1));
		}
		else
		{
			year = Integer.valueOf(gps_date.substring(4, 6));
			month = Integer.valueOf(gps_date.substring(2, 4));
			mday = Integer.valueOf(gps_date.substring(0, 2));
		}
		
		year = year + 2000;
		
		// special case handling for the GPS time (according to R code from JB Hyperspectral)
		
		if(gps_time.length() == 6)
		{
			hour = Integer.valueOf(gps_time.substring(0, 2));
			min = Integer.valueOf(gps_time.substring(2, 4));
			sec = Integer.valueOf(gps_time.substring(4, 6));
		}
		
		if(gps_time.length() == 5)
		{
			hour = Integer.valueOf(gps_time.substring(0, 1));
			min = Integer.valueOf(gps_time.substring(1, 3));
			sec = Integer.valueOf(gps_time.substring(3, 5));
		}

		if(gps_time.length() == 4)
		{
			hour = 0;
			min = Integer.valueOf(gps_time.substring(0, 2));
			sec = Integer.valueOf(gps_time.substring(2, 4));
		}
		
		if(gps_time.length() == 3)
		{
			hour = 0;
			min = Integer.valueOf(gps_time.substring(0, 1));
			sec = Integer.valueOf(gps_time.substring(1, 3));
		}

		if(gps_time.length() == 2)
		{
			hour = 0;
			min = 0;
			sec = Integer.valueOf(gps_time.substring(0, 2));
		}
		
		if(gps_time.length() == 1)
		{
			hour = 0;
			min = 0;
			sec = Integer.valueOf(gps_time.substring(0, 1));
		}		

		
		dt =  new DateTime(year, month, mday, hour, min, sec, DateTimeZone.UTC); // joda months start at 1
		
		
		return dt;
	}

	public ArrayList<Float> getUp_coef_fluorescence() {
		return up_coef_fluorescence;
	}

	public ArrayList<Float> getDw_coef_fluorescence() {
		return dw_coef_fluorescence;
	}

	public ArrayList<Float> getUp_coef_broadrange() {
		return up_coef_broadrange;
	}

	public ArrayList<Float> getDw_coef_broadrange() {
		return dw_coef_broadrange;
	}
	
	public boolean is_fluoresence_sensor() {
		return is_fluoresence_sensor;
	}


	public void post_process()
	{
		// restoration of missing data is possible if some records have GPS information
		if(valid_gps_data_found)
		{
			// at this point, we used to assume that only ever the first record can be compromised due to instrument switch-on
			// This is however not true, sometimes it takes a few measurements to get a valid GPS position
			
			if(spec_file.getEavMetadata(0).get_first_entry("Spatial Position") == null && first_valid_gps_time_index > 5)
			{
				// copy position from second set of readings
				MetaParameter pos_ = spec_file.getEavMetadata(5).get_first_entry("Spatial Position");
				for(int i=0;i<5;i++) spec_file.getEavMetadata(i).add_entry(pos_); // enter position for first block of spectra

				// get UTC time by back-calculating using the time gap between measurement blocks 
				DateTime dt1 = (DateTime) ((MetaDate) spec_file.getEavMetadata(0).get_first_entry("Acquisition Time")).getValue();
				DateTime dt2 = (DateTime) ((MetaDate) spec_file.getEavMetadata(5).get_first_entry("Acquisition Time")).getValue();
				DateTime gap = dt1.minus(dt2.getMillis());
				long millis = gap.getMillis();

				DateTime utc_2 = (DateTime) ((MetaDate) spec_file.getEavMetadata(this.first_valid_gps_time_index).get_first_entry("Acquisition Time (UTC)")).getValue();
				
				int number_of_blocks_without_utc = first_valid_gps_time_index / 5;
				
				// correct all wrong blocks
				for(int n=0;n<number_of_blocks_without_utc-1;n++)
				{

					DateTime utc_1 = new DateTime(utc_2.plus(millis*(number_of_blocks_without_utc - n)));

					MetaParameter mp = MetaParameter.newInstance(this.attributes_name_hash.get("Acquisition Time (UTC)"));
					try {
						mp.setValue(utc_1);
						for(int i=0 + n*5;i<n*5+5;i++) spec_file.getEavMetadata(i).add_entry(mp); // enter UTC for first block of spectra



						// adjust spectrum number
						MetaParameter spec_no = spec_file.getEavMetadata(0).get_first_entry("Spectrum Number");

						String tmp = spec_no.getValue().toString();
						String tmp_no = tmp.substring(0, tmp.length()-6) + utc_1.toString(DateTimeFormat.forPattern("HHmmss"));

						// enter spectrum_no for first block of spectra
						for(int i=0+n*5;i<n*5+5;i++)
						{						
							spec_file.getEavMetadata(i).get_first_entry("Spectrum Number").setValue(Integer.valueOf(tmp_no));; 
						}

					} catch (MetaParameterFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}	

				}

			}



		}
		
		
		
	}

	
	
}
