package ch.specchio.file.reader.spectrum;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.file.reader.campaign.SpecchioCampaignDataLoader;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.MetaParameterFormatException;
import ch.specchio.types.MetaSpatialPoint;
import ch.specchio.types.Metadata;
import ch.specchio.types.Point2D;
import ch.specchio.types.SpecchioMessage;
import ch.specchio.types.SpectralFile;
import ch.specchio.types.spatial_pos;

public class RoX_FileLoader extends JB_FileLoader {


	
	String date, time, measurement_designator, IT_WR, IT_VEG, mainboard_temp, mainboard_humidity, outside_temp, box_rel_hum, rel_hum, instrument_number, instrument_name;
	ArrayList<ArrayList<Float>> DNs = new ArrayList<ArrayList<Float>>();
	Metadata smd;

	
	public RoX_FileLoader(SPECCHIOClient specchio_client, SpecchioCampaignDataLoader campaignDataLoader) {
		super("RoX", specchio_client, campaignDataLoader);
		// TODO Auto-generated constructor stub
	}

	@Override
	public SpectralFile load(File file) throws IOException, MetaParameterFormatException {
		spec_file = new SpectralFile();
		
		
		spec_file.setCompany("JB Hyperspectral");
		
		// Parse file	
		Reader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
		//Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(bufferedReader);
		Reader reader = Files.newBufferedReader(Paths.get(file.getAbsolutePath()));	
		CSVParser csvParser = new CSVParser(bufferedReader, CSVFormat.newFormat(';'));
		
		//Iterable<CSVRecord> csvRecords = csvParser.getRecords();
		
		try
		{		
			int measurement_row_index = 0;
		
		for (CSVRecord r : csvParser) {
			
			// skip empty lines
			if(r.get(0).length() != 0)
			{
				// check if this a metadata line
				if(r.get(3).equals("auto_mode"))
				{


					spectrum_number = r.get(0);
					date = r.get(1);
					time = r.get(2);	
					IT_WR = r.get(5);	
					IT_VEG = r.get(7);
					mainboard_temp = r.get(11);
					mainboard_humidity = r.get(13);
//					box_rel_hum = r.get(15);
//					rel_hum = r.get(17);
					instrument_name = r.get(14);
					String[] name_parts = instrument_name.split(" ");
					String[] tmp = name_parts[2].split("-");
					instrument_number = tmp[1];					
					gps_time  = r.get(16);
					
					// remove decimal point from time string if existing
					gps_time = gps_time.replace(".", "");		
					
					gps_date = r.get(18);
					lat = r.get(20);
					lon = r.get(22);
					//voltage = r.get(i);


					
				}
				else
				{
					// this is a measurement
					measurement_designator = r.get(0);
					
					// fill vector
					ArrayList<Float> DN = new ArrayList<Float>();
					
					for(int i=1;i<r.size();i++)
					{
						// skip empty cells (they do appear at the end of the FLOX files ... WTF?)
						if(!r.get(i).isEmpty())
							DN.add(Float.valueOf(r.get(i)));
					}
					
					DNs.add(DN);
					
					// add spectrum metadata to the file object
					spec_file.addSpectrumFilename(spectrum_number + "-" + measurement_designator + " (" + file.getName() + ")");
					/*System.out.println("---------------------------------");
					System.out.println(" Measurement Designator (FULL) = " + measurement_designator);
					System.out.println("                   Name (FULL) = " + r.get(0));
					System.out.println("                      Filename = " + file.getName());
					System.out.println("      Spectrum Filename (FULL) = " + spec_file.getSpectrumFilename(measurement_row_index));
					System.out.println("---------------------------------");*/

					
					MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("Acquisition Time"));
					DateTime dt;
					int time_str_len = time.length();
					Integer sec = Integer.valueOf(time.substring(time_str_len-2,time_str_len));
					Integer min = Integer.valueOf(time.substring(time_str_len-4,time_str_len-2));
					Integer hour = Integer.valueOf(time.substring(0,time_str_len-4));
					Integer mday = Integer.valueOf(date.substring(4,6));
					Integer month = Integer.valueOf(date.substring(2,4)); 
					Integer year = Integer.valueOf(date.substring(0,2)) + 2000;
					
					
					dt =  new DateTime(year, month, mday, hour, min, sec, DateTimeZone.UTC); // joda months start at 1
					
					mp.setValue(dt);
					
					smd = new Metadata();
					smd.addEntry(mp);	
					
					mp = MetaParameter.newInstance(attributes_name_hash.get("PCB Temperature"));
					mp.setValue(mainboard_temp);
					smd.addEntry(mp);	
					
					mp = MetaParameter.newInstance(attributes_name_hash.get("PCB Humidity"));
					mp.setValue(mainboard_humidity);
					smd.addEntry(mp);				
				
					mp = MetaParameter.newInstance(attributes_name_hash.get("Integration Time"));
					if(measurement_designator.equals("WR") || measurement_designator.equals("DC_WR") || measurement_designator.equals("WR2"))
					{
						mp.setValue(IT_WR);
					}
					else
					{
						mp.setValue(IT_VEG);
					}
					smd.addEntry(mp);	
					
					// add coordinates
					if(isGPSValid())
					{	
						// add to metadata of this spectrum rather than the obsolete position array of the spec_file
						// this allows dealing with missing positions much easier
						mp = MetaParameter.newInstance(this.attributes_name_hash.get("Spatial Position"));
						Point2D coord = new Point2D(pos.latitude, pos.longitude);
						((MetaSpatialPoint) mp).setValue(coord);
						smd.addEntry(mp);							
					}
					
					// GPS Time
					if(isGPSValid())
					{
						utc = getUTC();
						mp = MetaParameter.newInstance(this.attributes_name_hash.get("Acquisition Time (UTC)"));
						mp.setValue(utc);
						smd.addEntry(mp);
						
						if(first_valid_gps_time_index == -1)
						{
							first_valid_gps_time_index = measurement_row_index;
						}						
					}
					
					
					// Spectrum number to include either GPS time stamp, or if that is not available use Acquisition Time
					// This is due to the spectrum number restarting from zero if instrument gets restarted during the day
					if(isGPSValid())
						spectrum_number_ext = spectrum_number + utc.toString(DateTimeFormat.forPattern("HHmmss"));
					else
						spectrum_number_ext = spectrum_number + dt.toString(DateTimeFormat.forPattern("HHmmss"));

					
					mp = MetaParameter.newInstance(this.attributes_name_hash.get("Spectrum Number"));
					mp.setValue(Integer.valueOf(spectrum_number_ext), "RAW");
					smd.addEntry(mp);						
					
//					// add coordinates
//					spatial_pos pos = new spatial_pos();
//					String[] lat_tokens = lat.split(" ");
//					int lat_sign = 1;
//					if(lat_tokens.length>1)
//					{
//						if(lat_tokens[1].equals("N"))
//						{
//							// remains positive
//						}
//						else
//							lat_sign = -1;
//					}
//					
//					String[] lon_tokens = lon.split(" ");
//					int lon_sign = 1;
//					if(lon_tokens.length>1)
//					{
//						if(lon_tokens[1].equals("E"))
//						{
//							// remains positive
//						}
//						else
//							lon_sign = -1;
//					}					
//					
//					pos.latitude = Double.valueOf(lat_tokens[0]) * lat_sign;
//					pos.longitude = Double.valueOf(lon_tokens[0]) * lon_sign;
//					
//					spec_file.addPos(pos);
					
					spec_file.addEavMetadata(smd);
					
					measurement_row_index++;
					
					int x  =0;
					
				}
			
			}

		}
		
		} catch (MetaParameterFormatException ex) {
			
			int gotcha = 1;
			
		}		
		
		reader.close();
		csvParser.close();
		
		// post processing for GPS related data gaps
		post_process();
		
		// add measurements as float matrix
		Float[][] f = new Float[DNs.size()][DNs.get(0).size()];
		
		for(int i = 0;i<DNs.size();i++)
		{				
			f[i] = DNs.get(i).toArray(f[i]);
		}		
		spec_file.setNumberOfSpectra(DNs.size());
		spec_file.setMeasurements(f);
		
		String path = file.getAbsolutePath();
		spec_file.setPath(path);		
		spec_file.setFilename(file.getName());
		spec_file.setFileFormatName(this.file_format_name);		


		// get wavelength reference from CAL file
		getCalibrationData();		
		
		// check if this a single RoX or if it integrated in a FLoX Box
		if(Integer.parseInt(instrument_number) > 100)
		{
			spec_file.setInstrumentName(instrument_name);
		}
		else
		{
			// FLoX
			spec_file.setInstrumentName(instrument_name + " - Broadrange");
		}
		
		spec_file.setInstrumentNumber(instrument_number);		
		
		
//		File cal_file = this.campaignDataLoader.getFlox_rox_cal_file();
//		boolean cal_file_not_found = false;
//		
//		if(cal_file != null && cal_file.exists())
//		{
//
//			bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(cal_file), StandardCharsets.UTF_8));
//			reader = Files.newBufferedReader(Paths.get(cal_file.getAbsolutePath()));	
//			csvParser = new CSVParser(bufferedReader, CSVFormat.newFormat(';'));
//			//csvRecords = csvParser.getRecords();
//			
//			String first_token = csvParser.iterator().next().get(0);
//			
//			if(first_token.equals("wl_F")) // RoX cal file format
//			{
//				int i=0;
//				ArrayList<Float> wvls = new ArrayList<Float>();
//				for (CSVRecord r : csvParser) {
//				
//					if(i>0)
//					{					
//						wvls.add(Float.valueOf(r.get(0)));
//					}
//					i++;
//					
//					if(r.getRecordNumber()==4) setCalibration_date(r.get(6));
//				}
//				
//				spec_file.addWvls(new Float[wvls.size()]);
//				spec_file.setWvls(0, wvls.toArray(spec_file.getWvls(0)));	
//			}
//			
//			else
//				cal_file_not_found = true;
//		
//
//		}
//		else
//		{
//			cal_file_not_found = true;			
//		}
//		
//		
//		if(cal_file_not_found)
//		{
//			
//			// output file error
//			spec_file.setFileErrorCode(SpectralFile.UNRECOVERABLE_ERROR);
//			ArrayList<SpecchioMessage> file_errors = spec_file.getFileErrors();
//			if(file_errors == null)
//			{
//				file_errors = new ArrayList<SpecchioMessage>();						
//			}
//
//			file_errors.add(new SpecchioMessage("No calibration file (cal.csv) could be found alongside the spectral file. You moron!!!", SpecchioMessage.ERROR));
//			spec_file.setFileErrors(file_errors);
//			
//		}
		
		
	    
		return spec_file;
	}

  	

}
