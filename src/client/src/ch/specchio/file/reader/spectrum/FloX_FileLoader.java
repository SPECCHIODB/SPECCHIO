package ch.specchio.file.reader.spectrum;

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

public class FloX_FileLoader extends JB_FileLoader {
	
	String date, time, measurement_designator, IT_WR, IT_VEG, chamber_temp, outside_temp, box_rel_hum, rel_hum, instrument_number, instrument_name;
	ArrayList<ArrayList<Float>> DNs = new ArrayList<ArrayList<Float>>();
	Metadata smd;
	
	private String QEpro_Frame;
	private String QEpro_CCD;
	private String mainboard_temp;
	private String mainboard_humidity;
	private String chamber_humidity;
	
	public FloX_FileLoader(SPECCHIOClient specchio_client, SpecchioCampaignDataLoader campaignDataLoader) {
		super("FloX", specchio_client, campaignDataLoader);
		// TODO Auto-generated constructor stub
	}

	@Override
	public SpectralFile load(File file) throws IOException, MetaParameterFormatException {
		spec_file = new SpectralFile();
		
		
		spec_file.setCompany("JB Hyperspectral");
		
		if(file.getName().charAt(0) != 'F')
		{
			is_fluoresence_sensor = true;
		}
		
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
					QEpro_Frame = r.get(11);	
					QEpro_CCD =  r.get(13);	
					mainboard_temp = r.get(15);	
					
					chamber_temp = r.get(17);
					mainboard_humidity= r.get(19);
					chamber_humidity= r.get(21);
					instrument_name = r.get(22);
					String[] name_parts = instrument_name.split(" ");
					String[] tmp = name_parts[2].split("-");
					instrument_number = tmp[1];				
					
//					outside_temp = r.get(13);
//					box_rel_hum = r.get(15);
//					rel_hum = r.get(17);
					
					gps_time  = r.get(24); // typically of the formnat: 020142.
					
					// remove decimal point from time string if existing
					gps_time = gps_time.replace(".", "");
					
					gps_date = r.get(26);
					lat = r.get(28);
					lon = r.get(30);					
						

					
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
					
					mp = MetaParameter.newInstance(attributes_name_hash.get("Spectrometer Frame Temperature"));
					mp.setValue(QEpro_Frame);
					smd.addEntry(mp);
					
					mp = MetaParameter.newInstance(attributes_name_hash.get("Detector Temperature"));
					mp.setValue(QEpro_CCD);
					smd.addEntry(mp);	
					
					mp = MetaParameter.newInstance(attributes_name_hash.get("PCB Temperature"));
					mp.setValue(mainboard_temp);
					smd.addEntry(mp);	
					
					mp = MetaParameter.newInstance(attributes_name_hash.get("Optical Compartment Temperature"));
					mp.setValue(chamber_temp);
					smd.addEntry(mp);	
					
					mp = MetaParameter.newInstance(attributes_name_hash.get("PCB Humidity"));
					mp.setValue(mainboard_humidity);
					smd.addEntry(mp);	
					
					mp = MetaParameter.newInstance(attributes_name_hash.get("Optical Compartment Humidity"));
					mp.setValue(chamber_humidity);
					smd.addEntry(mp);						

				
					mp = MetaParameter.newInstance(attributes_name_hash.get("Integration Time"));
					if(measurement_designator.equals("WR") || measurement_designator.equals("DC_WR"))
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
					
					
					spec_file.addEavMetadata(smd);
					
					int x  =0;
					
					measurement_row_index++;
					
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
		
		spec_file.setInstrumentName(instrument_name + " - Fluorescence Range");
		spec_file.setInstrumentNumber(instrument_number);
		
		// get wavelength reference from CAL file
		getCalibrationData();

	    
		return spec_file;
	}

  
}
