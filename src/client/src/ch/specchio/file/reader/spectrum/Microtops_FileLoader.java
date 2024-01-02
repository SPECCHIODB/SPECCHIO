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
import ch.specchio.types.MetaParameter;
import ch.specchio.types.MetaParameterFormatException;
import ch.specchio.types.Metadata;
import ch.specchio.types.SpecchioMessage;
import ch.specchio.types.SpectralFile;
import ch.specchio.types.spatial_pos;

public class Microtops_FileLoader  extends SpectralFileLoader {
	
	public Microtops_FileLoader(SPECCHIOClient specchio_client, SpecchioCampaignDataLoader campaignDataLoader) {
		super("MicrotopsTXT", specchio_client, campaignDataLoader);
	}

	@Override
	public SpectralFile load(File file) throws IOException,
			MetaParameterFormatException {

		
		spec_file = new SpectralFile();
		
		spec_file.setPath(file.getAbsolutePath());	
		spec_file.setFilename(file.getName());		
		
		spec_file.setCompany("Solar Systems");
		spec_file.setFileFormatName(this.file_format_name);
		
		
		Float[] wvls = new Float[5];
		wvls[0] = 440F;
		wvls[1] = 675F;
		wvls[2] = 870F;
		wvls[3] = 936F;
		wvls[4] = 1020F;
		
		
		ArrayList<Float[]> spectra = new ArrayList<Float[]>();
		ArrayList<Float[]> spectra_std = new ArrayList<Float[]>();
		ArrayList<Float[]> aot = new ArrayList<Float[]>();
		ArrayList<DateTime> times = new ArrayList<DateTime>();

		
		
		DateTimeFormatter formatter = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss").withZoneUTC();
		
		// open file
		file_input = new FileInputStream (file);					
		data_in = new DataInputStream(file_input);	
		BufferedReader d = new BufferedReader(new InputStreamReader(data_in));		
		
		
		// read file
		String line=d.readLine(); // line 1: e.g. REC#0174
		
		line=d.readLine(); // line 1: FIELDS:
		
		line = d.readLine(); // header
		
		String[] header_fields = line.split(",");
		
		
		// read till the end
		line = d.readLine();
		
		String[] fields = line.split(",");
		
		spec_file.setInstrumentNumber(fields[0]);
		
		while(!line.equals("END."))
		{			
			if (!line.contains("NaN"))
			{

				fields = line.split(",");

				Metadata smd = new Metadata();


				// Date and Time: 03/25/2015, 8:53:12
				String time_str;
				if ( fields[2].length() == 7)
				{
					time_str = fields[1] + fields[2];
				}
				else
				{
					time_str = fields[1] + " " + this.remove_leading_spaces(fields[2]);
				}


				DateTime dt = formatter.parseDateTime(time_str);
				times.add(dt);

				// lat / lon
				spatial_pos p = new spatial_pos();
				p.latitude = Double.valueOf(fields[3]);
				p.longitude = Double.valueOf(fields[4]);
				p.altitude = Double.valueOf(fields[5]);
				spec_file.addPos(p);

				// pressure
				MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("Air Pressure"));
				mp.setValue(Double.valueOf(fields[6]));
				smd.addEntry(mp); 	

				// SZA
				mp = MetaParameter.newInstance(attributes_name_hash.get("Illumination Zenith"));
				mp.setValue(Double.valueOf(fields[7]));
				smd.addEntry(mp); 	

				// AM
				//			mp = MetaParameter.newInstance(attributes_name_hash.get("Illumination Azimuth"));
				//			mp.setValue(Double.valueOf(fields[8]));
				//			smd.addEntry(mp); 	

				// SDCORR

				// TEMP
				mp = MetaParameter.newInstance(attributes_name_hash.get("Ambient Temperature"));
				mp.setValue(Double.valueOf(fields[10]));
				smd.addEntry(mp); 	


				// ID

				// spectrum
				Float[] spectrum = new Float[5];

				spectrum[0] = Float.valueOf(fields[12]);
				spectrum[1] = Float.valueOf(fields[13]);
				spectrum[2] = Float.valueOf(fields[14]);
				spectrum[3] = Float.valueOf(fields[15]);
				spectrum[4] = Float.valueOf(fields[16]);

				spectra.add(spectrum);
				spec_file.addMeasurementUnits(0); // DN
				spec_file.addNumberOfChannels(5);
				spec_file.addWvls(wvls);

				// stddev of spectrum
				spectrum = new Float[5];

				spectrum[0] = Float.valueOf(fields[17]);
				spectrum[1] = Float.valueOf(fields[18]);
				spectrum[2] = Float.valueOf(fields[19]);
				spectrum[3] = Float.valueOf(fields[20]);
				spectrum[4] = Float.valueOf(fields[21]);

				spectra_std.add(spectrum);
				spec_file.addMeasurementUnits(0); // DN
				spec_file.addNumberOfChannels(5);
				spec_file.addWvls(wvls);

				// AOT			

				spectrum = new Float[5];

				spectrum[0] = Float.valueOf(fields[26]);
				spectrum[1] = Float.valueOf(fields[27]);
				spectrum[2] = Float.valueOf(fields[28]);
				spectrum[3] = Float.valueOf(fields[29]);
				spectrum[4] = Float.valueOf(fields[30]);

				aot.add(spectrum);
				spec_file.addMeasurementUnits(0); // DN
				spec_file.addNumberOfChannels(5);
				spec_file.addWvls(wvls);

				// WATER
				mp = MetaParameter.newInstance(attributes_name_hash.get("Atmospheric Water Content"));
				mp.setValue(Double.valueOf(fields[31]));
				smd.addEntry(mp); 	

				spec_file.addEavMetadata(smd);
			}
			else
			{
				spec_file.setFileErrorCode(SpectralFile.RECOVERABLE_ERROR);
				ArrayList<SpecchioMessage> file_errors = spec_file.getFileErrors();
				if(file_errors == null)
				{
					file_errors = new ArrayList<SpecchioMessage>();						
				}

				file_errors.add(new SpecchioMessage("Skipped line containing NaN", SpecchioMessage.ERROR));
				spec_file.setFileErrors(file_errors);
				
				
			}

			
			line = d.readLine();
		}
		
		data_in.close ();

		spec_file.setNumberOfSpectra(spectra.size()*3); // spectrum, stdev and aot for each measurement
		
		// compile measurements into array
		
		Float[][] out_spectrum = new Float[spec_file.getNumberOfSpectra()][spec_file.getNumberOfChannels(0)];
		
		ListIterator<Float[]> a_li = spectra.listIterator();
		ListIterator<Float[]> b_li = spectra_std.listIterator();
		ListIterator<Float[]> c_li = aot.listIterator();
		int i = 0;
		while(a_li.hasNext())
		{
			out_spectrum[i++] = a_li.next(); // spectrum
			out_spectrum[i++] = b_li.next(); // std
			out_spectrum[i++] = c_li.next(); // aot
		}	
		
		ListIterator<DateTime> t_li = times.listIterator();
		i = 0;
		while(t_li.hasNext())
		{
			DateTime t = t_li.next();
			spec_file.setCaptureDate(i++, t); // spectrum
			spec_file.setCaptureDate(i++, t); // std
			spec_file.setCaptureDate(i++, t); // aot
		}			
		
		// add for each spectrum, std and aot
		for(i=1;i<=spectra.size();i++)
		{
			spec_file.addSpectrumFilename(spec_file.getFilename() + "_" + i);
			spec_file.addSpectrumFilename(spec_file.getFilename() + "_" + i);
			spec_file.addSpectrumFilename(spec_file.getFilename() + "_" + i);		
		}
		
		
		
		
		spec_file.setMeasurements(out_spectrum);
		
		return spec_file;
		
	}	
	
	
	
	

}
