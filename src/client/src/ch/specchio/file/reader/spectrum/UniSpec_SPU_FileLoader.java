
package ch.specchio.file.reader.spectrum;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.ListIterator;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.file.reader.campaign.SpecchioCampaignDataLoader;
import ch.specchio.types.MetaDate;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.MetaParameterFormatException;
import ch.specchio.types.Metadata;
import ch.specchio.types.SpecchioMessage;
import ch.specchio.types.SpectralFile;
import ch.specchio.types.spatial_pos;

public class UniSpec_SPU_FileLoader extends SpectralFileLoader {
	
	boolean not_eof = true;
	boolean spectral_interpolation = false;
	
	Metadata smd_a;
	Metadata smd_b;
	
	
	public UniSpec_SPU_FileLoader(SPECCHIOClient specchio_client, SpecchioCampaignDataLoader campaignDataLoader)
	{
		super("UniSpec_SPU", specchio_client, campaignDataLoader);
	}
	
	
	public SpectralFile load(File file) throws IOException, MetaParameterFormatException
	{		
		smd_a = new Metadata();
		smd_b = new Metadata();		
		
		SpectralFile f = new SpectralFile();
		f.setNumberOfSpectra(2); 
		
		f.setPath(file.getAbsolutePath());	
		f.setFilename(file.getName());		
		
		f.setCompany("PP Systems");
		f.setFileFormatName(this.file_format_name);
		f.setInstrumentTypeNumber(1); // hard coded arbitrary value within SPECCHIO as PP systems assigns no instrument codes

	
		file_input = new FileInputStream (file);			
		
		data_in = new DataInputStream(file_input);
		
		
		BufferedReader d = new BufferedReader(new InputStreamReader(data_in));
		
		// analyse header to get no of spectra
		read_header(d, f, true);
		
		f.addSpectrumFilename(f.getFilename());
		f.addSpectrumFilename(f.getFilename());
		
		f.setMeasurements(read_data(d, f));
//		f.capture_dates[0] = file.lastModified(); 
		
		data_in.close ();
		
		
		// metadata handling	

		if (f.getCapturingSoftwareName() != null) {
			MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("Capturing Software Name"));
			mp.setValue(f.getCapturingSoftwareName(), "String");
			smd_a.addEntry(mp);
			smd_b.addEntry(mp);
		}		
		

		if (f.getCapturingSoftwareName() != null) {
			MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("Capturing Software Version"));
			mp.setValue(f.getCapturingSoftwareVersion(), "String");
			smd_a.addEntry(mp);
			smd_b.addEntry(mp);
		}		
		
		if (this.spectral_interpolation) {
			MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("UniSpec Spectral Resampling"));
			mp.setValue("ON", "String");
			smd_b.addEntry(mp); // only applies to channel B
		}		
		else
		{
			MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("UniSpec Spectral Resampling"));
			mp.setValue("OFF", "String");
			smd_b.addEntry(mp); // only applies to channel B		
		}
		
		// channel information
		MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("Instrument Channel"));
		mp.setValue("A", "String");
		smd_a.addEntry(mp);
		
		mp = MetaParameter.newInstance(attributes_name_hash.get("Instrument Channel"));
		mp.setValue("B", "String");		
		smd_b.addEntry(mp);
		
		
		// Processing level information
		mp = MetaParameter.newInstance(attributes_name_hash.get("Processing Level"));
		mp.setValue(0.0);
		smd_a.addEntry(mp);
		
		mp = MetaParameter.newInstance(attributes_name_hash.get("Processing Level"));
		mp.setValue(0.0);	
		smd_b.addEntry(mp);		
		
		
		f.addEavMetadata(smd_b);
		f.addEavMetadata(smd_a);		
		
		f.addMeasurementUnits(0); // DN
		f.addMeasurementUnits(0); // DN
		
		// share eav metadata between channels a and b
//		f.specchio_eav_metadata_redundancy_groups[0] = 1;
//		f.specchio_eav_metadata_redundancy_groups[1] = 1;
//		
//		f.shared_metadata_for_multiple_spectra = true;
		
		
		return f;
	}
	
	// reads the header line in order to get the number of spectra contained in this file
	// or stores the spectra names (depending on the analyse flag)

	public void read_header(BufferedReader d, SpectralFile f, boolean analyse) throws IOException, MetaParameterFormatException
	{
		
		
		
		String line=d.readLine(); // line 1: File path
		
		while(line.length() == 0 || line.substring(0,1).equals("\""))
		{
			d.mark(500); // best guess ...
			
			if(line.length() > 0)
			{
				
				line = line.substring(1, line.length()-1); // remove the double quotes at start and end of string
				
				String[] sub_toks = line.split(":");				
				String tag = sub_toks[0];
				String val = sub_toks[1];
				
				
				if (tag.equals("Remarks")) 
				{
					
					
					String[] attribute_value_pairs = tokenise_attr_value_string(val);

					
					// process attribute value pairs
					for(int i=0;i<attribute_value_pairs.length;i=i+2)
					{
						//String[] av_tokens = value_tokens[i].split("=");
						
						if(attribute_value_pairs[i].equals("SW"))
						{
							f.setCapturingSoftwareName(attribute_value_pairs[i+1]);
						}
						
						if(attribute_value_pairs[i].equals("Version"))
						{
							f.setCapturingSoftwareVersion(attribute_value_pairs[i+1]);
						}						
						
					}
					
					
					
				}
				
				
				if (tag.equals("GPS")) 
				{
					double lat = 0, lon = 0, alt = 0;
					boolean gps_is_filled = false;
					
					String[] attribute_value_pairs = tokenise_attr_value_string(val);
					
					// clean up the tokens: there are still tabs and white spaces ....
					for(int i=0;i<attribute_value_pairs.length;i++)
					{
						attribute_value_pairs[i] = this.remove_trailing_tabs(attribute_value_pairs[i]);
						attribute_value_pairs[i] = this.remove_leading_spaces(attribute_value_pairs[i]);						
					}

					
					// process attribute value pairs
					for(int i=0;i<attribute_value_pairs.length;i=i+2)
					{
						//String[] av_tokens = value_tokens[i].split("=");
						
						if(attribute_value_pairs[i].equals("LAT"))
						{
							if(!attribute_value_pairs[i+1].equals("Ukn"))
							{
								lat = get_decimal_pos(attribute_value_pairs[i+1]);
								gps_is_filled = true;
							}
						}
						
						if(attribute_value_pairs[i].equals("LON"))
						{
							if(!attribute_value_pairs[i+1].equals("Ukn"))
							{
								lon = get_decimal_pos(attribute_value_pairs[i+1]);
								gps_is_filled = true;
							}
						}						

						if(attribute_value_pairs[i].equals("ALT"))
						{
							if(!attribute_value_pairs[i+1].equals("Ukn"))
							{
								alt = get_decimal_pos(attribute_value_pairs[i+1]);
								gps_is_filled = true;
							}
						}						
												

					}
					
					
					if(gps_is_filled == true)
					{
						spatial_pos s = new spatial_pos();
						s.latitude = lat;
						s.longitude = lon;
						s.altitude = alt;
						
						// add for both spectra
						f.addPos(s); 
						f.addPos(s);

					}
					
					
					
					
				}				
				
				
				if (tag.equals("Time")) 
				{
					// skip white spaces					
					String time_str = line.substring(6); // use original line because the time format can also contain colons

					time_str = remove_leading_spaces(time_str);
					
					// "Time:       6/12/2010 12:05:58 PM" (for spu files)
					// "Time:       05/07/2000  14:32:38" (for SPU files)
					// "Time:       01/22/2006  12:51:12  99.251"	Decidedly odd ... this is the one with 3 tokens
					
					
					// clean up tokens to have just one space between date and time
					String[] tokens = time_str.split("\\s+");
					
					if (tokens.length == 3 && !(time_str.contains("PM") || time_str.contains("AM")))
					{
						// looks like we got one of the odd three token items without AM/PM
						time_str = tokens[0] + " " + tokens[1];
					}
					else
					{
						time_str = tokens[0];
						for(int i=1;i<tokens.length;i++)
						{
							time_str = time_str + " " + this.remove_leading_spaces( tokens[i]);
						}
					}
					
					
//					TimeZone tz = TimeZone.getTimeZone("UTC");
					DateTimeFormatter formatter;
					
					if(time_str.contains("PM") || time_str.contains("AM"))
					{
						formatter = DateTimeFormat.forPattern("MM/dd/yyyy hh:mm:ss a").withZoneUTC();
					}
					else
					{
						formatter = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss").withZoneUTC();
					}
					
					DateTime dt = formatter.parseDateTime(time_str);
					
					DateTimeFormatter fmt = DateTimeFormat.forPattern(MetaDate.DEFAULT_DATE_FORMAT);
					String date_str = fmt.print(dt);
					 
					 
//					sdf.setTimeZone(tz);
//				    Date date;
//					try {
						//date = sdf.parse(time_str);
//						sdf.parse(time_str);
//						date = dt.toDate();
//						
//						System.out.println("Date and Time: " + date);
//						
						f.setCaptureDate(0, dt);		
						f.setCaptureDate(1, dt); // time for both measurements is taken as the same				
						
						
//					} catch (ParseException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//						
//						// fallback scenario: use creation time stamp of input file: only possible with Java Release 7!!!!!!
//						//BasicFileAttributes attr = Files.readAttributes(this.input_file, BasicFileAttributes.class);
//
//						
//					}

					
					
				}
				
				
				if (tag.equals("Integration")) 
				{				
					String int_time_str = val.substring(0, val.length()-3); // cut the ms from the number	
					int_time_str = this.remove_leading_spaces(int_time_str);
					
					MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("Integration Time"));
					mp.setValue(Integer.valueOf(int_time_str), "ms");
					smd_a.addEntry(mp);
					smd_b.addEntry(mp);										
				}
				
				if (tag.equals("Number Scans")) 
				{													
					MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("Number of Internal Scans"));
					mp.setValue(Integer.valueOf(Integer.valueOf(remove_leading_spaces(val))), "RAW");
					smd_a.addEntry(mp);
					smd_b.addEntry(mp);					
				}				
				
			}
			
			line=d.readLine();
			
		}
		
		d.reset();

	}
	

	
	private Double get_decimal_pos(String string) {
		
		Double pos = null;
		
		// variations of strings:
		// 51.100191  N
		// 50 54.19333' N 	
		
		// case 1: 50 54.19333'
		if(string.contains("'"))
		{
			String[] tokens = string.split(" ");
			
			double degrees = Double.valueOf(tokens[0].substring(0, tokens[0].length()-1));
			
			double minutes = Double.valueOf(tokens[1].substring(0, tokens[1].length()-1));
			
			pos = degrees + minutes/60;
			
		}
		else
		{
			// 51.100191  N
			
			String tmp = string.substring(0, string.length()-4);				
			pos = Double.valueOf(tmp);
			

		}
		
		if(pos != null)
		{
		
			if(string.substring(string.length()).equals("S"))
			{
				pos = pos * (-1); // southern hemisphere coordinate
			}
			
		}
		

		return pos;
	}


	private String[] tokenise_attr_value_string(String val) {
		
		// split by equal signs
		String[] av_strings = val.split("=");
		
		String[] avs = new String[(av_strings.length - 1) * 2];
		
		int avs_ind = 0;
		
		// backward scan to extract the attribute from the av_strings
		for(int i=0;i<av_strings.length - 1;i++)
		{
		
			int last_space_ind = av_strings[i].lastIndexOf(" ");
			
			avs[avs_ind++] = av_strings[i].substring(last_space_ind+1);
			
			// special handling of last value entry, as there is no space to detect.
			if(i<av_strings.length - 2) last_space_ind = av_strings[i+1].lastIndexOf(" "); else last_space_ind= av_strings[i+1].length();
							
			avs[avs_ind++] = av_strings[i+1].substring(0,last_space_ind);
		
		}
		
		return avs;
	}


	Float[][] read_data(BufferedReader d, SpectralFile f)
	{
		// first column: wvl
		// second column: channel b
		// third column: channel a

		spectral_interpolation = false;
		
		ArrayList<Float> a = new ArrayList<Float>();
		
		ArrayList<Float> b = new ArrayList<Float>();
		ArrayList<Float> wvl = new ArrayList<Float>();
		String line;
		
		try {
			
			// read all lines
			line=d.readLine();
			while(line != null && line.length() > 0)
			{

				// see: http://stackoverflow.com/questions/225337/how-do-i-split-a-string-with-any-whitespace-chars-as-delimiters
				String[] tokens = line.split("\\s+");	

				try {
					try {
						a.add(Float.valueOf(tokens[2]));
					} catch (NumberFormatException e) {

						if(tokens[2].equals("."))
							a.add(0.0f);
						
					}
					
					//tokens[1] = this.remove_trailing_spaces(tokens[1]);
					
					//tokens[1] = remove_trailing_tabs(tokens[1]); // clean up for the integer conversion (apparently, the Float conversion is more tolerant)				
					
					try {
						Integer.valueOf(tokens[1]);
					} catch (NumberFormatException e) {
						// tried to parse a floating point value, therefore, this spectrum was spectrally interpolated
						
						// avoid the detection of special cases where unbinned values show up as "0."
	
							// catching cases of just a decimal point instead of a float
							if(e.getMessage().equals("For input string: \".\""))
							{
								tokens[1] = "0.0";
							}						
							
							if(Float.valueOf(tokens[1]) > 0)
							{
								spectral_interpolation = true;
							}
	
						
					} 
					
						
					
					b.add(Float.valueOf(tokens[1]));
					
				
				
				} catch (ArrayIndexOutOfBoundsException e) {

						// case of incomplete lines (less than 3 values), e.g. 324.53   100 
						// either channel A or B is missing a value, apparently the value should be zero
						// Educated guess based previous spectral band

						float existing_value = Float.valueOf(tokens[1]);
						float diffA;
						float diffB;
						
						if (!a.isEmpty())
						{
							diffA = Math.abs(a.get(a.size()-1) - existing_value);
							diffB = Math.abs(b.get(a.size()-1) - existing_value);
						}
						else
						{
							// first band has already a problem
							// best would be to read the next line and figure out the likely bad channel ...
							
							// we assume that B needs filling
							diffA = 0.0f;
							diffB = 1.0f;
							
						}

						String failed_channel;

						if (diffA < diffB)
						{
							// A channel value exists, B needs filling with zero
							a.add(Float.valueOf(tokens[1]));
							b.add(0.0f);
							failed_channel = "B";
						}
						else
						{
							// B channel value exists, A needs filling with zero
							a.add(0.0f);
							b.add(Float.valueOf(tokens[1]));	
							failed_channel = "A";
						}
						
						ArrayList<SpecchioMessage> file_errors = f.getFileErrors();
						if(file_errors == null)
						{
							file_errors = new ArrayList<SpecchioMessage>();						
						}
						file_errors.add(new SpecchioMessage("Missing value in channel " + failed_channel + " filled with zero", SpecchioMessage.WARNING));
						f.setFileErrors(file_errors);
						

				}
				
				wvl.add(Float.valueOf(tokens[0]));
				
				line=d.readLine();

			}


	
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		f.addNumberOfChannels(wvl.size());
		
		// fill wvls for channels a and b
		f.addWvls(wvl.toArray(new Float[wvl.size()]));
		f.addWvls(wvl.toArray(new Float[wvl.size()]));
		
		
		Float[][] out_spectrum = new Float[f.getNumberOfSpectra()][f.getNumberOfChannels(0)];
		
		// check if size of the two vectors are the same: happens if corrupted files are encountered (e.g. YS00093.SPT of John Gamons sky oaks dataset)
		if (a.size() != b.size())
		{
			f.setFileErrorCode(SpectralFile.UNRECOVERABLE_ERROR);
			ArrayList<SpecchioMessage> file_errors = f.getFileErrors();
			if(file_errors == null)
			{
				file_errors = new ArrayList<SpecchioMessage>();						
			}

			file_errors.add(new SpecchioMessage("Target and reference vectors are of unequal length.", SpecchioMessage.ERROR));
			f.setFileErrors(file_errors);
		}
		else
		{	
	
			ListIterator<Float> a_li = a.listIterator();
			ListIterator<Float> b_li = b.listIterator();
			int i = 0;
			while(a_li.hasNext())
			{
				out_spectrum[0][i] = (Float)b_li.next(); // channel b
				out_spectrum[1][i++] = (Float)a_li.next(); // channel a
			}	
		}
		
		return out_spectrum;
		
	}
	
	
}
