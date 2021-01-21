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
import org.joda.time.DateTimeZone;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.file.reader.campaign.SpecchioCampaignDataLoader;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.MetaParameterFormatException;
import ch.specchio.types.Metadata;
import ch.specchio.types.SpectralFile;

public class GER_FileLoader extends SpectralFileLoader {
	
	Metadata smd;
	
	public GER_FileLoader(SPECCHIOClient specchio_client, SpecchioCampaignDataLoader campaignDataLoader)
	{
		super("GER Signature File", specchio_client, campaignDataLoader);
	}


	public SpectralFile load(File file) throws IOException, MetaParameterFormatException
	{
		smd = new Metadata();
		
		SpectralFile ger_file = new SpectralFile();
		ger_file.setNumberOfSpectra(2); // always a target and a reference radiance
		
		ger_file.setPath(file.getAbsolutePath());		
		ger_file.setFilename(file.getName());
		ger_file.setFileFormatName(this.file_format_name);
		
		// spectrum number is contained in the extension		
		MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("Spectrum Number"));
		mp.setValue(Integer.valueOf(ger_file.getExt()), "RAW");
		smd.addEntry(mp);				
		
						
		ger_file.addSpectrumFilename(ger_file.getFilename()); // target name
		ger_file.addSpectrumFilename(ger_file.getFilename()); // reference name
		
		// set measurement unit (always Radiance with the GER 3700)
		
		ger_file.addMeasurementUnits(2);
		ger_file.addMeasurementUnits(2);
		
		//ger_file.setCaptureDate(0, new DateTime[ger_file.getNumberOfSpectra()]); 
		
		file_input = new FileInputStream (file);			
				
		data_in = new DataInputStream(file_input);
		
		read_GER_file(data_in, ger_file);
		
		// entries for both spectra
		ger_file.addEavMetadata(smd);
		ger_file.addEavMetadata(smd);		
		
		data_in.close ();
		
		return ger_file;
	}
	
	public void read_GER_file(DataInputStream in, SpectralFile f) throws IOException, MetaParameterFormatException
	{
		String line;
		boolean hdr_ended = false;
		
		// use buffered stream to read lines
		BufferedReader d = new BufferedReader(new InputStreamReader(in));
		
		// read line by line
		while(!hdr_ended && (line=d.readLine()) != null)
		{
			// tokenise the line
			String[] tokens = line.split("=");
			
			// analyse the tokens
			hdr_ended = analyse_GER_file(tokens, d, f);						
		}		
		
		// read the measurements
		f.setMeasurements(read_data(data_in, d));
		
		f.addNumberOfChannels(f.getMeasurement(0).length);
		
	}
	
	public boolean analyse_GER_file(String[] tokens, BufferedReader in, SpectralFile hdr) throws MetaParameterFormatException
	{
		
		String t1 = tokens[0];
		boolean hdr_ended = false;
		
		if(t1.equals("///GER SIGNATUR FILE///"))
		{
			hdr.setCompany("GER");
		}
		
		if(t1.equals("instrument"))
		{
			tokens[1] = tokens[1].replace(" ", ""); // remove spaces from token 2
			String[] instr_data = tokens[1].split(":");			
			hdr.setInstrumentTypeNumber(Integer.valueOf(instr_data[0]));
			hdr.setInstrumentNumber(instr_data[1]);	
		}
		
		if(t1.equals("time"))
		{
			// remove first space
			tokens[1] = tokens[1].replaceFirst(" ", "");
			
			String[] time_data = tokens[1].split(" ");
			time_data[0] = time_data[0].replace(".", ":");
			String[] date = time_data[0].split(":");
			String[] time = time_data[1].split(":");
			
//			TimeZone tz = TimeZone.getTimeZone("UTC");
//			Calendar cal = Calendar.getInstance(tz);
//			cal.set(Integer.valueOf(date[2]), Integer.valueOf(date[1])-1, // month is zero based!
//					Integer.valueOf(date[0]), Integer.valueOf(time[0]), 
//					Integer.valueOf(time[1]), Integer.valueOf(time[2]));
			
			
			DateTime dt = new DateTime(Integer.valueOf(date[2]), Integer.valueOf(date[1]), Integer.valueOf(date[0]), Integer.valueOf(time[0]), Integer.valueOf(time[1]), Integer.valueOf(time[2]), DateTimeZone.UTC);
			
			hdr.setCaptureDate(0, dt);		
			hdr.setCaptureDate(1, hdr.getCaptureDate(0)); // time for both measurements is taken as the same
		}
		
		if(t1.equals("averaging"))
		{
			tokens[1] = tokens[1].replace(" ", ""); // remove spaces from token 2
			String[] avg = tokens[1].split(",");
			
			// there are two number, one for the target, one for the reference
			// for simplicity we just take the target one.
			// Should it be needed that one needs the correct value for the white reference
			// as well, then a list of this value must be kept for each spectrum
			MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("Number of Internal Scans"));
			mp.setValue(Integer.valueOf(avg[0]), "RAW");			
			
			smd.addEntry(mp);
			

		}
		
		if(t1.equals("comm"))
		{
			hdr.setComment(tokens[1]);
		}
			
		
		if(t1.equals("data"))
		{
			hdr_ended = true;
		}
		
		
		return hdr_ended;
		
	}
	
	Float[][] read_data(DataInputStream in, BufferedReader d) throws IOException
	{
		String line;
		ArrayList<Float> target, reference;
		
		target = new ArrayList<Float>();
		reference = new ArrayList<Float>();
				
		// read line by line
		while((line=d.readLine()) != null)
		{
			// There are some strange cases where radiance values can be negative (of the last channel)
			// the minus signs are inserted into the second space that is separating the values
			// In order to have the values still being separated by two spaces, a replace must be done:
			line = line.replaceAll("-", " -");
			
			// tokenise the line
			String[] tokens = line.split("  "); // values are separated by two spaces
			
			// first token is wavelength
			
			// second token is radiance of target
			target.add(Float.valueOf(tokens[1]));
			
			// third token is radiance of reference
			reference.add(Float.valueOf(tokens[2]));
			
		}
		
		
		
		Float[][] f = new Float[2][target.size()];
		
		ListIterator<Float> li = target.listIterator();
		int i = 0;
		while(li.hasNext())
		{
			f[0][i++] = (Float)li.next();
		}
		

		li = reference.listIterator();
		i = 0;
		while(li.hasNext())
		{
			f[1][i++] = (Float)li.next();
		}
		
		
		return f;
		
		
		
	}

}
