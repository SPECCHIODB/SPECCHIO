package ch.specchio.file.reader.spectrum;

import java.io.BufferedReader;
import java.text.DateFormatSymbols;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.file.reader.campaign.SpecchioCampaignDataLoader;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.MetaParameterFormatException;
import ch.specchio.types.SpectralFile;
import ch.specchio.types.spatial_pos;

public class OceanView_FileLoader extends JAZ_FileLoader{
	
	public OceanView_FileLoader(SPECCHIOClient specchio_client, SpecchioCampaignDataLoader campaignDataLoader) {
		super("OceanViewTXT", specchio_client, campaignDataLoader);
		start_of_spectral_data = ">>>>>Begin Spectral Data<<<<<";
	}	
	
	
	public boolean analyse_JAZ_file(String[] tokens, BufferedReader in, SpectralFile hdr)
	{
		String t1 = tokens[0];
		boolean hdr_ended = false;
		
		if((t1.contains("Data from")) 
				&& (t1.contains("Node")))
		{
			hdr.setCompany("OceanOptics");
			
			// extract spectrometer name from line 1: this is actually not needed and error prone anyway!
//			String[] line1_tokens = t1.split("_");
//			
//			String tmp = line1_tokens[line1_tokens.length-2];
//			String[] tmp_tokens = tmp.split(" ");
//			
//			String instr_name = tmp_tokens[tmp_tokens.length-1];
//			
//			instr_name = instr_name.substring(0, instr_name.length()-1); // cut the last character as this is the number given within a cluster of spectrometers
			
			// this is actually not consistent either! E.g. L_20150711_D80_USB4C032391_031.txt Node
			//hdr.setInstrumentName(instr_name); // second last token: Data from MAYP1114952_13-22-06-700.txt Node
			
			
		}
		
		if(t1.equals("Spectrometer"))
		{		
			// this info appears to be wrong when multiple instruments are attached!
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
		
		if(t1.equals("Integration Time (sec)"))
		{		
			MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("Integration Time"));
			try {
				String[] sub_tokens = tokens[1].split(" ");
				
				mp.setValue(Float.valueOf(sub_tokens[0]) * 1000, "ms");
				smd.addEntry(mp);				
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MetaParameterFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}			
		
		
		if(t1.equals("Scans to average"))
	{		
		String[] sub_tokens = tokens[1].split(" ");
		
		MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("Number of internal Scans"));
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
	
	


	
//	Date: Tue Apr 06 14:03:39 CEST 2010		
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
		

//		TimeZone tz = TimeZone.getTimeZone("UTC");
//		Calendar cal = Calendar.getInstance(tz);
//		cal.set(year, month, 
//				Integer.valueOf(time_data[2]), Integer.valueOf(time[0]), 
//				Integer.valueOf(time[1]), Integer.valueOf(time[2]));

		
//		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
//		formatter.setTimeZone(tz);
		
		DateTime dt = new DateTime(year, month+1, Integer.valueOf(time_data[2]), Integer.valueOf(time[0]), Integer.valueOf(time[1]), Integer.valueOf(time[2]), DateTimeZone.UTC);
		
		hdr.setCaptureDate(0, dt);
		//hdr.capture_dates[0] = get_date_and_time_from_HR_string(time_data[0]);


	}
	
	
//	if(t1.equals("comm"))
//	{
//		hdr.comment = tokens[1];
//	}
//	
//	if(t1.equals("optic"))
//	{
//		// assumption: lenses do not change between reference and target
//		
//		String[] sub_tokens = tokens[1].split(", ");
//		
//		String str = sub_tokens[0].replaceFirst(" LENS", "");
//		
//		hdr.foreoptic_degrees = Integer.valueOf(str);	
//					
//	}		
//	
	if(t1.equals("Longitude"))
	{		
		
		if(hdr.getPos().size() == 0)
		{
			hdr.addPos(new spatial_pos());
		}
		
		String[] sub_tokens = tokens[1].split(" ");
		
		hdr.getPos(0).longitude = Double.valueOf(sub_tokens[0]);
		
	}
	
	if(t1.equals("Latitude"))
	{		
		
		if(hdr.getPos().size() == 0)
		{
			hdr.addPos(new spatial_pos());
		}
		
		String[] sub_tokens = tokens[1].split(" ");
		
		hdr.getPos(0).latitude = Double.valueOf(sub_tokens[0]);
		
	}
	
	if(t1.equals("Altitude"))
	{		
		
		if(hdr.getPos().size() == 0)
		{
			hdr.addPos(new spatial_pos());
		}
		
		String[] sub_tokens = tokens[1].split(" ");
		
		hdr.getPos(0).altitude = Double.valueOf(sub_tokens[0]);
		
	}	
	

	
	if(t1.equals(this.start_of_spectral_data))
	{
		hdr_ended = true;
	}
	
	
	return hdr_ended;
	
}

}
