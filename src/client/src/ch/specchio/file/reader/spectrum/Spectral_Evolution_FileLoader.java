package ch.specchio.file.reader.spectrum;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.file.reader.campaign.SpecchioCampaignDataLoader;
import ch.specchio.spaces.MeasurementUnit;
import ch.specchio.types.MetaParameterFormatException;
import ch.specchio.types.SpectralFile;
import ch.specchio.types.spatial_pos;

/**
 * Reader for Spectral Evolution Files
 * 
 * ahueni, 2013
 */

public class Spectral_Evolution_FileLoader extends SpectralFileLoader {

	SpectralFile spec_file;
	private String[] dates;
	private String[] times;	
	private DateTime[] tmp_capture_dates;
	private double longitude;
	private double latitude;
	private double altitude;
	private boolean gps_pos_available = false;
	private int no_of_columns;
	private String measurement_type;
	private int no_of_spectral_vectors;
	private boolean contains_DNs;
	private boolean contains_Ls;
	private boolean contains_Ref;
	private boolean contains_Tgt;
	private boolean contains_R;
	private String[] data_headers;
	private int no_of_bands;
	private boolean contains_TGT_Ls;
	private boolean contains_REF_Ls;
	private boolean contains_REF_DNs;
	private boolean contains_TGT_DNs;
	private boolean contains_channel_nos;
	private boolean contains_R_as_factor_of_1;
	
	public Spectral_Evolution_FileLoader(SPECCHIOClient specchio_client, SpecchioCampaignDataLoader campaignDataLoader) {
		super("Spectral Evolution Data File", specchio_client, campaignDataLoader);
	}


	@Override
	public SpectralFile load(File file) throws IOException,
			MetaParameterFormatException {

		spec_file = new SpectralFile();
		
		spec_file.setCompany("Spectral Evolution");
		
		spec_file.setPath(file.getAbsolutePath());	
		spec_file.setFilename(file.getName());		

		spec_file.setFileFormatName(this.file_format_name);		
		
		file_input = new FileInputStream (file);			
		
		data_in = new DataInputStream(file_input);
		
		read_file(data_in);
				
		
		return spec_file;
	}
	
	
	public void read_file(DataInputStream in) throws IOException
	{
		String line;
		boolean hdr_ended = false;
		
		// use buffered stream to read lines
		BufferedReader d = new BufferedReader(new InputStreamReader(in));
		
		// read line by line
		while(!hdr_ended && (line=d.readLine()) != null)
		{
			// tokenise the line
			String[] tokens = line.split(": ");
			
			// analyse the tokens
			hdr_ended = analyse_file(tokens, d);						
		}		
		
		
		// read data header
		line=d.readLine();
		parse_data_header(line);
		
		// figure out how many spectral vectors there are
		no_of_spectral_vectors = no_of_columns - 1;
				
		
		if (contains_channel_nos)
		{
			no_of_spectral_vectors = no_of_columns - 2;
		}

		
		// create metadata entries for all spectral vectors
		spec_file.setNumberOfSpectra(no_of_spectral_vectors);
		add_spatial_positions_if_needed();	
		
		for(int i=0;i<this.no_of_spectral_vectors;i++)
		{	
			spec_file.addSpectrumFilename(spec_file.getFilename());
			spec_file.addNumberOfChannels(no_of_bands);
			
			 // capture times
			if(spec_file.getMeasurandDesignator(i) == SpectralFile.REFERENCE)
			{
				spec_file.setCaptureDate(i, this.tmp_capture_dates[0]); // first entry is associated with the reference
			}
			else
			{
				spec_file.setCaptureDate(i, this.tmp_capture_dates[1]); // second entry is associated with the target
			}
			
		}
		
		// read the measurements
		this.read_data(in, d);
		
		
//		f.setMeasurements(read_data(data_in, d));
//		
//		f.addNumberOfChannels(f.getMeasurement(0).length);
		
		
	}
	
	private void parse_data_header(String line) {
		
		// split line by tabs
		data_headers = line.split("\t");

		contains_channel_nos = line.contains("Chan.#");
		contains_DNs = line.contains("Counts");
		contains_Ls = line.contains("Rad.");
		contains_Ref = line.contains("Ref.");
		contains_Tgt = line.contains("Target");
		contains_R = line.contains("Reflect.");
		
		contains_REF_DNs = line.contains("Raw Counts (Ref.)");
		contains_TGT_DNs = line.contains("Raw Counts (Target)");
		
		contains_REF_Ls = line.contains("Rad. (Ref.)");
		contains_TGT_Ls = line.contains("Rad. (Target)");
		
		contains_R_as_factor_of_1 = line.contains("Tgt./Ref. [1.0]");
		
		
		
		
		// build measurement type metadata, measurand designators and assign correct capture times
		for(int i=1;i<data_headers.length;i++)
		{
			if(data_headers[i].contains("Counts")) this.spec_file.addMeasurementUnits(MeasurementUnit.DN);
			
			if(data_headers[i].contains("Rad")) this.spec_file.addMeasurementUnits(MeasurementUnit.Radiance);
			
			if(data_headers[i].contains("Reflect.")) this.spec_file.addMeasurementUnits(MeasurementUnit.Reflectance);
			if(data_headers[i].contains("Tgt./Ref. [1.0]")) this.spec_file.addMeasurementUnits(MeasurementUnit.Reflectance);
			
			// measurand designators
			if(data_headers[i].contains("Ref.")) this.spec_file.addMeasurandDesignator(SpectralFile.REFERENCE);
			else if(data_headers[i].contains("Target")) this.spec_file.addMeasurandDesignator(SpectralFile.TARGET);
			else  this.spec_file.addMeasurandDesignator(SpectralFile.UNSPECIFIED);
			

			
		}
		
	}


	public boolean analyse_file(String[] tokens, BufferedReader in)
	{
		String t1 = tokens[0];
		boolean hdr_ended = false;
			
		if(t1.equals("Measurement"))
		{
			measurement_type = tokens[1];			
		}
		
		if(t1.equals("Channels"))
		{
			no_of_bands = Integer.parseInt(tokens[1]);	
		}		
		
		
		if(t1.equals("Instrument"))
		{
			// PSR-2500_SN1226208 [2]
			// SR-3500_20680T1
			String[] instr_data = tokens[1].split("_SN");	
			
			if(instr_data.length == 1)
			{
				// try splitting by underscore only
				instr_data = tokens[1].split("_");	
			}
			
			String[] family_and_type_no = instr_data[0].split("-");
			
			if(family_and_type_no.length == 1)
			{	// try to split on + sign (for strings like PSR+3500
				family_and_type_no = instr_data[0].split("\\+");
			}
			
			String[] serial_no_et_al = instr_data[1].split(" ");
			
			Integer type_no = Integer.valueOf(family_and_type_no[1]);
			
			spec_file.setInstrumentTypeNumber(type_no);
			spec_file.setInstrumentNumber(serial_no_et_al[0]);			
		}
		
		
		if(t1.equals("Date"))
		{						
			// split into single dates 
			dates = tokens[1].split(",");	
		}
					
		
		if(t1.equals("Time"))
		{
			// split into single times 
			times = tokens[1].split(",");	
			
			tmp_capture_dates = new DateTime[times.length];
			
			for (int i=0;i<times.length;i++)
			{
				tmp_capture_dates[i] = get_date_and_time(i);								
			}
		}
		
		
		if(t1.equals("Comment") && tokens.length == 2)
		{
			spec_file.setComment(tokens[1]);
		}
		
		if(t1.equals("Foreoptic") ) // && tokens[1].contains(" LENS")
		{
			// assumption: lenses do not change between reference and target
//			
//			String[] sub_tokens = tokens[1].split(", ");
//			
//			String str = sub_tokens[0].replaceFirst(" LENS", "");
//			
//			str = str.replace(" ", "");
//			
//			spec_file.setForeopticDegrees(Integer.valueOf(str));	
						
		}		
		
		if(t1.equals("Longitude") && tokens[1].length() > 3) // empty is a 'n/a' string
		{				
			//		Longitude: -115.70179
			longitude = Double.valueOf(tokens[1]);		
			gps_pos_available = true;
		}
		
		if(t1.equals("Latitude") && tokens[1].length() > 3)
		{					
			//		Latitude: 38.51098			
			latitude = Double.valueOf(tokens[1]);		
			gps_pos_available = true;
		}		
		
		if(t1.equals("Altitude") && tokens[1].length() > 3)
		{	
			//		Altitude: 1437.70	
			// 		Altitude: 77.500m
			try
			{
				altitude = Double.valueOf(tokens[1]);	
			} catch (NumberFormatException e) {
				// deal with strange new formats like Altitude: 77.500m
				
				tokens[1] = tokens[1].substring(0, tokens[1].length()-1);
				altitude = Double.valueOf(tokens[1]);	
			}
			
			gps_pos_available = true;
		}			
		
		if(t1.contains("Columns"))
		{
			String[] tmp = t1.split(" ");
			String number = tmp[1].substring(1,2);
			no_of_columns = Integer.parseInt(number);
			
		}
		
		if(t1.equals("Data:"))
		{
			hdr_ended = true;
		}
		
		
		return hdr_ended;
		
	}
	
	
	DateTime get_date_and_time(int index)
	{
		
		String time_str = this.dates[index] + " " + this.times[index];		
		
		String hour_format = "";


//		TimeZone tz = TimeZone.getTimeZone("UTC");
//		DateFormat sdf;
		
		if(time_str.contains("PM") || time_str.contains("AM"))
		{
			hour_format =" hh:mm:ss a";
		}
		else
		{
			if(time_str.contains("."))
				hour_format =" HH:mm:ss.SS";
			else
				hour_format =" HH:mm:ss";
		}
		
		DateTimeFormatter formatter = DateTimeFormat.forPattern("MM/dd/yyyy" +  hour_format).withZoneUTC();
		
		DateTime dt = formatter.parseDateTime(time_str);
		
//		sdf = new SimpleDateFormat("MM/dd/yy" +  hour_format);
//		
//		
//		sdf.setTimeZone(tz);
//	    Date date = null;
//		try {
//			date = sdf.parse(time_str);
//			
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			
//			// fallback scenario: use creation time stamp of input file: only possible with Java Release 7!!!!!!
//			//BasicFileAttributes attr = Files.readAttributes(this.input_file, BasicFileAttributes.class);
//
//			
//		}
//		
//		
//		Calendar cal = Calendar.getInstance(tz);
//		cal.setTime(date);
//		
//		
//		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd HH");
//		formatter.setTimeZone(tz);
//		
//		String out=formatter.format(cal.getTime());		
		
//		return cal.getTime();
		
		return dt;
		
	}	
		
	
	private void add_spatial_positions_if_needed()
	{
		if(spec_file.getPos().size() == 0 && gps_pos_available)
		{
			spatial_pos s = new spatial_pos();
			s.latitude = this.latitude;
			s.longitude = this.longitude;
			s.altitude = this.altitude;		
			
			// positions for each spectral vector
			for(int i=0;i<this.no_of_spectral_vectors;i++)
			{	
				spec_file.addPos(s);
			}
		}
	}
	
	

	
	void read_data(DataInputStream in, BufferedReader d) throws IOException
	{
		Float[][] f = new Float[this.no_of_spectral_vectors][this.no_of_bands];
		String line;

		Float[] wvls = new Float[this.no_of_bands];
		
		int band_no = 0;
				
		// read line by line
		while((line=d.readLine()) != null)
		{
			while(line.substring(0,1).equals(" "))
			{
				line = line.substring(1); // cut first character if it is a whitespace that messes up the splitting
			}
			
			// tokenise the line
			// see: http://stackoverflow.com/questions/225337/how-do-i-split-a-string-with-any-whitespace-chars-as-delimiters
			String[] tokens = line.split("\\s+");	
			
			int data_start = 1;
			
			Float channel_no;
			if (this.contains_channel_nos)
			{
				channel_no = (Float.valueOf(tokens[0]));
				data_start = 2;
			}
			else
			{
				// first token is wavelength
				data_start = 1;
			}
						
			wvls[band_no] = (Float.valueOf(tokens[data_start - 1]));
			
			// measurements follow
			int cnt = 0;
			for(int i=data_start;i<this.no_of_spectral_vectors+data_start;i++)
			{
				f[cnt][band_no] = Float.valueOf(tokens[i]);
				
				if(spec_file.getMeasurementUnits(i) == MeasurementUnit.Reflectance) // this is a reflectance value
				{
					f[cnt][band_no] /= 100; // normalise to range 0:1	
				}	
				cnt = cnt + 1;
			}
			
			band_no++;
			
		}

		spec_file.addWvls(wvls);
		spec_file.setMeasurements(f);
	
	}
		
	

}
