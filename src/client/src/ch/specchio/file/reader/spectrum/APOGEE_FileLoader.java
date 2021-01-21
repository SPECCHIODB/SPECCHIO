package ch.specchio.file.reader.spectrum;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.ListIterator;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.file.reader.campaign.SpecchioCampaignDataLoader;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.MetaParameterFormatException;
import ch.specchio.types.Metadata;
import ch.specchio.types.SpectralFile;

public class APOGEE_FileLoader extends SpectralFileLoader {
	
	boolean not_eof = true;

	public APOGEE_FileLoader(SPECCHIOClient specchio_client, SpecchioCampaignDataLoader campaignDataLoader) {
		super("APOGEE", specchio_client, campaignDataLoader);
	}
	
	
	public SpectralFile load(File file) throws IOException, MetaParameterFormatException
	{
		SpectralFile f = new SpectralFile();
		f.setNumberOfSpectra(1); 
		
		//f.instr_set = new InstrumentSettings();
		
		f.setPath(file.getAbsolutePath());		
		f.setFilename(file.getName());	
		f.setFileFormatName(this.file_format_name);
		
		f.setCompany("APOGEE");
	
		file_input = new FileInputStream (file);			
		
		data_in = new DataInputStream(file_input);
		
		
		BufferedReader d = new BufferedReader(new InputStreamReader(data_in));
		
		// analyse header to get no of spectra
		read_header(d, f, true);
		
		f.addSpectrumFilename(f.getFilename());
		
		f.setMeasurements(read_data(d, f));
		
		data_in.close ();
		
		return f;
	}
	
	// reads the header line in order to get the number of spectra contained in this file
	// or stores the spectra names (depending on the analyse flag)

	public void read_header(BufferedReader d, SpectralFile f, boolean analyse) throws IOException, MetaParameterFormatException
	{
		Metadata smd = new Metadata();
		
		String line=d.readLine(); // line 1
		line=d.readLine(); // line 2
		
		//line = line.replace("  ", " "); // replace double spaces with single ones
		line = line.replace("\"", ""); // remove the double quotes at the start
		line = line.substring(1, line.length()-1);
		
		// clean-up: any space after a colon is removed
		line = line.replaceAll(": *", ":");
		
		String[] tokens = line.split("  ");	
		
		// first token holds the information about the measurement type
		if (tokens[0].equals("TRANS->")) f.getMeasurementUnits().add(6);

		for(int i=2;i<tokens.length; i++)
		{
			tokens[i] = tokens[i].replace(" ", ""); // remove spaces from the token
			
			String[] sub_toks = tokens[i].split(":");
			
			String tag = sub_toks[0];
			String val = sub_toks[1];
			
			//if (tag.equals("Pix")) f.no_of_channels = Integer.valueOf(val);
			
			if (tag.equals("Time")) 
			{
				String int_time_str = val.substring(0, val.length()-2);
				//f.instr_set.add_setting("Integration_time", Integer.valueOf(int_time_str)); // cut the ms from the number
				// should store here the unit of the integration time as well!
				
				MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("Integration Time"));
				mp.setValue(Integer.valueOf(int_time_str), "ms");
				smd.addEntry(mp);				
				
			}
			
			if (tag.equals("Avg"))
			{
				MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("Number of Internal Scans"));
				mp.setValue(Integer.valueOf(val), "RAW");		
				smd.addEntry(mp);					
			}
		}

		f.setNumberOfSpectra(1);
		
		f.addEavMetadata(smd);

	}
	

	
	Float[][] read_data(BufferedReader d, SpectralFile f)
	{
		

		ArrayList<Float> spectrum = new ArrayList<Float>();
		ArrayList<Float> wvl = new ArrayList<Float>();
	
		try {
			
			String line;;
			
			// read all lines
			while((line=d.readLine()) != null)
			{
				line = line.substring(1); // cut start white space
				
				String[] tokens = line.split("  ");				

				spectrum.add(Float.valueOf(tokens[1]));				
				wvl.add(Float.valueOf(tokens[0]));

			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		f.addNumberOfChannels(spectrum.size());
		
		// fill wvls
		//f.wvls = new Float[wvl.size()];
		f.addWvls(new Float[wvl.size()]);
		Float[] tmp = new Float[wvl.size()];
		wvl.toArray(tmp);
		f.setWvls(0, tmp);		// before wvls was an arraylist: wvl.toArray(f.wvls);
		

		

		
		Float[][] out_spectrum = new Float[f.getNumberOfSpectra()][f.getNumberOfChannels(0)];

		ListIterator<Float> li = spectrum.listIterator();
		int i = 0;
		while(li.hasNext())
		{
			out_spectrum[0][i++] = (Float)li.next();
		}	
		
		return out_spectrum;
		
	}
	
	
}
