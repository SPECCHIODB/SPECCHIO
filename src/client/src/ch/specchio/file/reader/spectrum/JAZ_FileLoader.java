package ch.specchio.file.reader.spectrum;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.text.DateFormatSymbols;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.file.reader.campaign.SpecchioCampaignDataLoader;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.MetaParameterFormatException;
import ch.specchio.types.Metadata;
import ch.specchio.types.SpecchioMessage;
import ch.specchio.types.SpectralFile;

public class JAZ_FileLoader extends SpectralFileLoader {
	
	
	protected Metadata smd;
	String start_of_spectral_data;

	public JAZ_FileLoader(SPECCHIOClient specchio_client, SpecchioCampaignDataLoader campaignDataLoader) {
		super("JAZ", specchio_client, campaignDataLoader);
		start_of_spectral_data = ">>>>>Begin Processed Spectral Data<<<<<";
	}
	
	public JAZ_FileLoader(String file_format_name, SPECCHIOClient specchio_client, SpecchioCampaignDataLoader campaignDataLoader) {
		super(file_format_name, specchio_client, campaignDataLoader);
	}	

	public SpectralFile load(File file) throws IOException
	{
		spec_file = new SpectralFile();
		spec_file.setNumberOfSpectra(1); // is that always correct????
		
		spec_file.setPath(file.getAbsolutePath());		
		spec_file.setFilename(file.getName());
		spec_file.setFileFormatName(this.file_format_name);
		
		smd = new Metadata();
		
						
		spec_file.addSpectrumFilename(spec_file.getFilename()); 
		
		file_input = new FileInputStream (file);			
				
		data_in = new DataInputStream(file_input);
		
		read_JAZ_file(data_in, spec_file);
		
		spec_file.addEavMetadata(smd);
		
		data_in.close ();
		
		return spec_file;
	}
	
	public void read_JAZ_file(DataInputStream in, SpectralFile f) throws IOException
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
			hdr_ended = analyse_JAZ_file(tokens, d, f);		
			
		}	
		
		if (!hdr_ended)
		{
			// something went wrong, maybe the line ">>>>>Begin Processed Spectral Data<<<<<" was not present ...
			f.setFileErrorCode(SpectralFile.UNRECOVERABLE_ERROR);
			ArrayList<SpecchioMessage> file_errors = f.getFileErrors();
			if(file_errors == null)
			{
				file_errors = new ArrayList<SpecchioMessage>();						
			}

			file_errors.add(new SpecchioMessage("Could not find beginning of spectral data: " + start_of_spectral_data + " is missing in file.", SpecchioMessage.ERROR));
			f.setFileErrors(file_errors);
			
		}
		
		
		// read the measurements
		f.setMeasurements(read_data(data_in, d));
		
		f.addNumberOfChannels(f.getMeasurement(0).length);
		
	}
	
	public boolean analyse_JAZ_file(String[] tokens, BufferedReader in, SpectralFile hdr)
	{
		String t1 = tokens[0];
		boolean hdr_ended = false;
		
		if(t1.equals("SpectraSuite Data File"))
		{
			hdr.setCompany("OceanOptics");
		}
		
		if(t1.equals("Spectrometers"))
		{		
			hdr.setInstrumentName(tokens[1]);
		}
		
		if(t1.equals("Integration Time (usec)"))
		{		
			MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("Integration Time"));
			try {
				String[] sub_tokens = tokens[1].split(" ");
				
				mp.setValue(Integer.valueOf(sub_tokens[0]) / 1000, "ms");
				smd.addEntry(mp);				
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MetaParameterFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
		
		if(t1.equals("Spectra Averaged"))
		{		
			String[] sub_tokens = tokens[1].split(" ");
			
			MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("Number of Internal Scans"));
			try {
				mp.setValue(Integer.valueOf(sub_tokens[0]), "RAW");
				smd.addEntry(mp);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MetaParameterFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		


		
//		Date: Tue Apr 06 14:03:39 CEST 2010		
		if(t1.equals("Date"))
		{

			String[] time_data = tokens[1].split(" ");
			
			DateFormatSymbols dfs = new DateFormatSymbols();
			String[] months = dfs.getMonths();	
			
			int month = 0;
			
			for (int i=0; i < 12; i = i + 1)
			{
				//String short_month = months[i].substring(0, 3);
				if (time_data[1].equals(months[i].substring(0, 3)))
				{
					month = i;
					break;
				}
				
			}
			
			int year = Integer.valueOf(time_data[5]);
			
			String[] time = time_data[3].split(":");
			

//			TimeZone tz = TimeZone.getTimeZone("UTC");
//			Calendar cal = Calendar.getInstance(tz);
//			cal.set(year, month, 
//					Integer.valueOf(time_data[2]), Integer.valueOf(time[0]), 
//					Integer.valueOf(time[1]), Integer.valueOf(time[2]));

			
//			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
//			formatter.setTimeZone(tz);
			
			DateTime dt = new DateTime(year, month+1, Integer.valueOf(time_data[2]), Integer.valueOf(time[0]), Integer.valueOf(time[1]), Integer.valueOf(time[2]), DateTimeZone.UTC);
			
			hdr.setCaptureDate(0, dt);
			//hdr.capture_dates[0] = get_date_and_time_from_HR_string(time_data[0]);


		}
		
		
//		if(t1.equals("comm"))
//		{
//			hdr.comment = tokens[1];
//		}
//		
//		if(t1.equals("optic"))
//		{
//			// assumption: lenses do not change between reference and target
//			
//			String[] sub_tokens = tokens[1].split(", ");
//			
//			String str = sub_tokens[0].replaceFirst(" LENS", "");
//			
//			hdr.foreoptic_degrees = Integer.valueOf(str);	
//						
//		}		
//		
//		if(t1.equals("longitude"))
//		{		
//			if (hdr.pos[0] == null) 
//			{
//				hdr.pos[0] = new spatial_pos();
//				hdr.pos[1] = new spatial_pos();
//				hdr.pos[2] = new spatial_pos();
//			}
//			
//			double longitudes[] = read_gps_data(tokens[1]);
//			
//			hdr.pos[0].longitude = longitudes[0]; // reference
//			hdr.pos[1].longitude = longitudes[1]; // tgt radiance
//			hdr.pos[2].longitude = longitudes[1]; // tgt reflectance
//			
//		}
//		
//		if(t1.equals("latitude"))
//		{		
//			if (hdr.pos[0] == null) 
//			{
//				hdr.pos[0] = new spatial_pos();
//				hdr.pos[1] = new spatial_pos();
//				hdr.pos[2] = new spatial_pos();
//			}
//			
//			double latitudes[] = read_gps_data(tokens[1]);
//			
//			hdr.pos[0].latitude = latitudes[0]; // reference
//			hdr.pos[1].latitude = latitudes[1]; // tgt radiance
//			hdr.pos[2].latitude = latitudes[1]; // tgt reflectance
//			
//		}			
		
		if(t1.equals(start_of_spectral_data))
		{
			hdr_ended = true;
		}
		
		
		return hdr_ended;
		
	}
	
	
	double[] read_gps_data(String str)
	{
		//  11121.2335,W, 11121.2324,W
		//  5330.5955,N, 5330.5964,N
		
		double[] file_coord = new double[2];	
		double[] coord = new double[2];	
		
		str = str.replace(" ", "");
		
		String[] coords = str.split(",");
		
		file_coord[0] = Double.valueOf(coords[0]);
		file_coord[1] = Double.valueOf(coords[2]);
		
		coord[0] = spec_file.DDDmm2DDDdecimals(file_coord[0]);
		coord[1] = spec_file.DDDmm2DDDdecimals(file_coord[1]);
		
		if (coords[0].equals("S") || coords[0].equals("E"))
		{
			coord[0] = coord[0]*(-1);
			coord[1] = coord[1]*(-1);
		}
		
		return(coord);
	}
	

	
	Date get_date_and_time_from_HR_string(String str)
	{
		
		str = str.replaceFirst(" ", "");
		String[] date_and_time = str.split(" ");
		

		String[] date = date_and_time[0].split("/");
		String[] time = date_and_time[1].split(":");
		
		int hrs = Integer.valueOf(time[0]);
		// AM and PM check
		if (date_and_time[2].equals("AM"))
		{
			if (hrs == 12) hrs = hrs - 12;			
		}
		if (date_and_time[2].equals("PM"))
		{
			if (hrs >= 1 && hrs < 12) hrs = hrs - 12;			
		}		
		
		int year = Integer.valueOf(date[2]) + 2000;
		
		TimeZone tz = TimeZone.getTimeZone("UTC");
		Calendar cal = Calendar.getInstance(tz);
		cal.set((year), Integer.valueOf(date[0]), 
				Integer.valueOf(date[1]), hrs, 
				Integer.valueOf(time[1]), Integer.valueOf(time[2]));

		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
		formatter.setTimeZone(tz);
		
//		String out=formatter.format(cal.getTime());


// int hh = cal.get(Calendar.HOUR_OF_DAY);

		
		
		return cal.getTime();
		
	}
	
	Float[][] read_data(DataInputStream in, BufferedReader d) throws IOException
	//Float[] read_data(DataInputStream in, BufferedReader d) throws IOException
	{
		String line;
		ArrayList<Float> target, wvls;
		
		wvls = new ArrayList<Float>();
		target = new ArrayList<Float>();

				
		// read line by line
		while((line=d.readLine()) != null && !line.equals(">>>>>End Processed Spectral Data<<<<<"))
		{
			
			// GER specific treatment: needed for the HR????
			// There are some strange cases where radiance values can be negative (of the last channel)
			// the minus signs are inserted into the second space that is separating the values
			// In order to have the values still being separated by two spaces, a replace must be done:
			//line = line.replaceAll("-", " -");
			
			// tokenise the line
			String[] tokens = line.split("\t"); // values are separated by a tab
			
			// first token is wavelength
			wvls.add(Float.valueOf(tokens[0]));
			
			// second token is radiance of target
			target.add(Float.valueOf(tokens[1]));
				
			
		}
		
		
		
		Float[][] f = new Float[1][target.size()];
		spec_file.addWvls(new Float[target.size()]);
		

		spec_file.setWvls(0, wvls.toArray(spec_file.getWvls(0)));
		
		f[0] = target.toArray(f[0]);

		
		return f;
		
		
		
	}

}
