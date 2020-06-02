package ch.specchio.file.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import ch.specchio.constants.FileTypes;

public class CampaignExportXMLWriter extends TableWriter {

	public CampaignExportXMLWriter(OutputStream os) {
		super(os, FileTypes.CAMPAIGN_EXPORT_XML);
		
	}

	public void write_table(String name, Hashtable<String, String> table) throws IOException 
	{
		write_start_tag("table", name);
		write_nl();
		
		// write all fields
		Enumeration<String> fields = table.keys();
		while (fields.hasMoreElements()) {
			String field = fields.nextElement();
			String value = table.get(field);
			
			write_start_tag("field", field);
			bw.write(value);
			write_end_tag("field");
			write_nl();
		}
	
		write_end_tag("table");
		write_nl();
		
		bw.flush(); // apparently, the writer does this automatically ...
	}
	
	
	void write_start_tag(String tag, String tag_name) throws IOException
	{
		bw.write("<" + tag + " name=\"" + tag_name + "\">");
	}
	
	void write_end_tag(String tag) throws IOException
	{
		bw.write("</" + tag + ">");
	}


}
