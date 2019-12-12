package ch.specchio.proc_modules;

import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.ListIterator;
import javax.swing.JOptionPane;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.constants.FileTypes;
import ch.specchio.constants.HeaderBody;
import ch.specchio.constants.TimeFormats;
import ch.specchio.file.writer.SpectrumWriter;
import ch.specchio.file.writer.SpectrumWriterFactory;
import ch.specchio.gui.FileOutputDialog;
import ch.specchio.gui.ProgressReportDialog;
import ch.specchio.gui.SPECCHIOApplication;
import ch.specchio.interfaces.ProgressReportInterface;
import ch.specchio.spaces.Space;
import ch.specchio.types.Spectrum;



public class FileOutputManager extends Thread {
	
	String target_dir;
	String base_name;
	int file_type;
	boolean split_hdr_and_body;
	
	boolean configured = false;
	boolean close_progress_report = false;
	boolean done = false;
	
	public boolean get_unit_from_spectrum = false;
	
	ArrayList<SpaceProcessingChainComponent> spaces = null;
	
	int spectra_progress_cnt = 0;	
	int line_progress_cnt = 1;
	int total_progress_cnt = 0;
	int time_format;
	
	SPECCHIOClient specchio_client;
	
	ProgressReportInterface pr = null;
	
	
	public FileOutputManager(SPECCHIOClient specchio_client, ArrayList<SpaceProcessingChainComponent> spaces)
	{
		this.specchio_client = specchio_client;
		this.spaces = spaces;
	}
	
	
	public FileOutputManager(SPECCHIOClient specchio_client, ArrayList<SpaceProcessingChainComponent> spaces, String target_dir, String base_name, int file_type, boolean split_hdr_and_body, int time_format)
	{
		this(specchio_client, spaces);
		
		this.target_dir = target_dir;
		this.base_name = base_name;
		this.file_type = file_type;
		this.split_hdr_and_body = split_hdr_and_body;
		this.time_format = time_format;
		
		configured = true;
	}
	
	public void set_progress_report(ProgressReportInterface pr)
	{
		this.pr = pr;
	}
	
	public synchronized boolean done()
	{
		if(!done)
		{
			try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return done;
	}
	
	
	public synchronized void run()
	{
	
		try {	
			
			if(configured == false)
			{
				Point loc = new Point(0,0);
				FileOutputDialog d = new FileOutputDialog(loc);
				boolean ret_val = d.get_fileoutput_info();
			
				if(ret_val == true)
				{					
					target_dir = d.target_dir.getText();
					file_type = d.file_type;
					base_name = d.base_name.getText();
					split_hdr_and_body = d.split_header_and_body.isSelected();
					
					if(d.formatted_time_Button.isSelected())
						time_format = TimeFormats.Formatted;
					
					if(d.seconds_time_Button.isSelected())
						time_format = TimeFormats.Seconds;
					
					configured = true;
				}
				
			}
			
			
			if(configured == true)
			{
				
				// create progress report if not yet externally defined
				if(pr == null)
				{
					pr = new ProgressReportDialog(SPECCHIOApplication.getInstance().get_frame(), "Writing to filesystem", false, 20);
					((ProgressReportDialog)pr).setVisible(true);
					close_progress_report = true;
				}

				int numOutput = 0;
				try {
					numOutput = write();
				}
		  		catch (SPECCHIOClientException ex) {
					JOptionPane.showMessageDialog(
			    			SPECCHIOApplication.getInstance().get_frame(),
			    			ex.getMessage(),
			    			"Error",
			    			JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
			    		);
			    }
				
				
				if(close_progress_report)
				{
					try {
						// figure out if there is a data usage policy
						String data_policies = "";
						ArrayList<String> policies = new ArrayList<String>();
						for (SpaceProcessingChainComponent spcc : spaces) {
							String[] policies_for_space = specchio_client.getPoliciesForSpace(spcc.getSpace());
							for (String policy : policies_for_space) {
								policies.add(policy);
							}
						}
						if (policies.size() > 0) {
							int cnt = 0;
							data_policies = "The following data policies apply: \n";
							for (String policy : policies) {
								if(cnt>1) data_policies = data_policies +  " / ";
								data_policies = data_policies + policy;
							}
						}
						
						JOptionPane.showMessageDialog(SPECCHIOApplication.getInstance().get_frame(), "Exported " + Integer.toString(numOutput) + " spectra" + "\n" + data_policies);
					}
			  		catch (SPECCHIOClientException ex) {
						JOptionPane.showMessageDialog(
				    			SPECCHIOApplication.getInstance().get_frame(),
				    			ex.getMessage(),
				    			"Error",
				    			JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
				    		);
				    }
					
					((ProgressReportDialog)pr).setVisible(false);					
				}
				

			}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		
		done = true;
		this.notifyAll(); // wake up threads that are waiting for the writing to finish
		
	}
	
	
	int write() throws IOException, SPECCHIOClientException
	{
		
		// initialise the progress bar
		pr.set_operation("Initialising");
		
		// initialise variables
		SpectrumWriterFactory writerFactory = new SpectrumWriterFactory();
		
		// write spaces one at a time
		int count = 0;
		for (SpaceProcessingChainComponent og_c : spaces) {
			
			// make sure the space is loaded
			Space og;
			if (og_c.get_data_ready_status()) {
				og = og_c.getSpace();
			} else {
				og = specchio_client.loadSpace(og_c.getSpace());
			}
			pr.set_min_max(0, og.getNumberOfDataPoints());
			
			// open output files
			OutputStream headerStream;
			OutputStream bodyStream;

			// download spectra from the server
			pr.set_operation("Downloading spectra");
			Spectrum spectra[] = new Spectrum[og.getSpectrumIds().size()];
			int i = 0;
			for (Integer spectrumId : og.getSpectrumIds()) {
				spectra[i] = specchio_client.getSpectrum(spectrumId, true);
				pr.set_progress(++i);
			}

			if (split_hdr_and_body) {

				// separate writers for header and body
				File headerFilename = new File(
						target_dir,
						base_name + "_" + og.get_filename_addon() + FileTypes.get_filename_extension(this.file_type, HeaderBody.Header)
					);

				File bodyFilename = new File(
						target_dir,
						base_name + "_" + og.get_filename_addon() + FileTypes.get_filename_extension(this.file_type, HeaderBody.Body)
					);
				headerStream = new FileOutputStream(headerFilename);
				bodyStream = new FileOutputStream(bodyFilename);

				// write header
				pr.set_operation("Writing file header");
				SpectrumWriter headerWriter = writerFactory.getWriter(headerStream, file_type, HeaderBody.Header);
				headerWriter.setTimeFormat(time_format);
				headerWriter.setUseSpectrumUnits(get_unit_from_spectrum);
				headerWriter.startSpace(og);
				i = 0;
				for (Spectrum s : spectra) {
					headerWriter.writeSpectrum(s);
					pr.set_progress(++i);
				}
				headerWriter.endSpace(false);

				// write body
				pr.set_operation("Writing file body");
				SpectrumWriter bodyWriter = writerFactory.getWriter(bodyStream, file_type, HeaderBody.Body);
				bodyWriter.setTimeFormat(time_format);
				bodyWriter.setUseSpectrumUnits(get_unit_from_spectrum);
				bodyWriter.startSpace(og);
				i = 0;
				for (Spectrum s : spectra) {
					bodyWriter.writeSpectrum(s);
					pr.set_progress(++i);

					// everything for this spectrum has now been written, so increment the spectrum counter
					count++;
				}
				bodyWriter.endSpace();
				// close files


			} else {
				// same writer for header and body
				File filename = new File(
						target_dir,
						base_name + "_" + og.get_filename_addon() + FileTypes.get_filename_extension(this.file_type, HeaderBody.Both)
					);
				headerStream = new FileOutputStream(filename);
				bodyStream = headerStream;

				// write header
				pr.set_operation("Writing file ");
				SpectrumWriter headerWriter = writerFactory.getWriter(headerStream, file_type, HeaderBody.Header);
				headerWriter.setTimeFormat(time_format);
				headerWriter.setUseSpectrumUnits(get_unit_from_spectrum);
				headerWriter.startSpace(og);
				i = 0;
				for (Spectrum s : spectra) {
					headerWriter.writeSpectrum(s);
					pr.set_progress(++i);
				}
				headerWriter.endSpace(true);

			}

			headerStream.close();
			if (split_hdr_and_body) {
				bodyStream.close();
			}

		
		}
		
		return count;
	}
	
	int total_no_of_spectra()
	{
		int total_no = 0;
		
		ListIterator<SpaceProcessingChainComponent> li = spaces.listIterator();
		while(li.hasNext())
		{
			total_no += li.next().getSpace().getNumberOfDataPoints();
		}
		
		return total_no;
	}
	
	int total_no_of_bands()
	{
		int total_no = 0;
		
		ListIterator<SpaceProcessingChainComponent> li = spaces.listIterator();
		while(li.hasNext())
		{
			total_no += li.next().getSpace().getDimensionality();
		}
		
		return total_no;		
	}

}
