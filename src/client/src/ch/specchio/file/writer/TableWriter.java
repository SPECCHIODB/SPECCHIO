package ch.specchio.file.writer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Hashtable;


public abstract class TableWriter {
	BufferedWriter bw;

	public TableWriter(OutputStream os, int file_type) {
		bw = new BufferedWriter(new OutputStreamWriter(os));
	}
	
	
	public void close() throws IOException
	{
		bw.close();
	}
	
	
	public void write(String s) throws IOException
	{
		bw.write(s);
	}
	
	public abstract void write_table(String name, Hashtable<String, String> table)  throws IOException;

	
	public boolean is_done() {
		// TODO Auto-generated method stub
		return false;
	}


	public void write_next_tag() throws IOException {
		// TODO Auto-generated method stub
		
	}
	

	public void write_tag(String tag) throws IOException {
		// TODO Auto-generated method stub
		
	}	


	public void write_next_value() throws IOException {
		// TODO Auto-generated method stub
		
	}
	

	public void write_next_value(String tag) throws IOException {
		// TODO Auto-generated method stub
		
	}

	public void write_nl() throws IOException 
	{
		bw.newLine();		
	}


	public void write_separator() throws IOException {
		// TODO Auto-generated method stub
		
	}

}
