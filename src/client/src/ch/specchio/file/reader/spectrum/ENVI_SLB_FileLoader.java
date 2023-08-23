package ch.specchio.file.reader.spectrum;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.file.reader.campaign.SpecchioCampaignDataLoader;
import ch.specchio.types.SpectralFile;


public class ENVI_SLB_FileLoader extends ENVI_FileLoader {
	
	public ENVI_SLB_FileLoader(SPECCHIOClient specchio_client, SpecchioCampaignDataLoader campaignDataLoader)
	{
		super("ENVI SLB", specchio_client, campaignDataLoader);
	}
	
	public SpectralFile load(File file) throws IOException
	{
//		spec_file = new SpectralFile();
//
//		spec_file.setSpectrumNameType("ENVI Hdr");
//
//		spec_file.setFilename(file.getName());
//		spec_file.setFileFormatName(this.file_format_name);

		super.load(file);
		
		// we take only slb's and sli's as input, header files
		// will be read within this routine by constructing the header file name
		if(spec_file.getExt().equals("slb") || spec_file.getExt().equals("sli"))
		{
			spec_file.setPath(file.getAbsolutePath());
			
			// expected header path name
			String hdr_pathname = spec_file.getPath().substring(0,
					spec_file.getPath().length() - spec_file.getExt().length()) + "hdr";

			// new file object for the header
			File hdr = new File(hdr_pathname);
			read_ENVI_header(hdr);
			

			if(spec_file.getNumberOfSpectraNames() == spec_file.getNumberOfSpectra())
			{
				for(int i=0;i < spec_file.getNumberOfSpectra(); i++)
				{
					spec_file.addSpectrumFilename(spec_file.getSpectrumName(i));
				}	
			}
			else  // clean up at some point ....
			{
			
				
				// construct spectra names from file name if
				// there is more than one spectrum in the file
				if(spec_file.getNumberOfSpectra() > 1)
				{
					for(int i=0;i < spec_file.getNumberOfSpectra(); i++)
					{
						spec_file.addSpectrumFilename(spec_file.getBasename() + "_" + Integer.toString(i));
//						envi_file.spectra_numbers[i] = i+1; // simple auto numbering						
					}
					
				}
				else // use the body name as spectrum name
				{
					spec_file.addSpectrumFilename(spec_file.getBasename() + spec_file.getExt());
//					envi_file.spectra_numbers[0] = 1;
					
					// concat with spectrum name if available
					if(spec_file.getNumberOfSpectraNames() == spec_file.getNumberOfSpectra())
					{
						spec_file.setSpectrumFilename(0, spec_file.getSpectrumFilename(0) + " " + spec_file.getSpectrumName(0));
					}
				}
			}
		
			// read body		
			file_input = new FileInputStream (file);					
			data_in = new DataInputStream(file_input);
			
			// load all spectra
			spec_file.setMeasurements(read_data(data_in, spec_file.getNumberOfChannels(0), spec_file.getNumberOfSpectra()));
			
			return spec_file;
		}
		else
			return null;
	}


	@Override
	void setLinesAttribute(int lines) {
		// this is probably the number of spectra in the body file
		spec_file.setNumberOfSpectra(lines);
	}

	@Override
	void setSamplesAttribute(int samples) {
		// this is the number of spectral bands (really odd, that one ...)
		spec_file.addNumberOfChannels(samples);
	}

	@Override
	void setBandsAttribute(int bands) {
		// do nothing; this field seems useless
	}
}
