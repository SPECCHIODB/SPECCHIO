package ch.specchio.file.writer;

import java.io.OutputStream;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.constants.FileTypes;


/**
 * Factory class for spectrum writers.
 */
public class SpectrumWriterFactory {
	
	SPECCHIOClient specchio_client;
	
	/**
	 * Constructor.
	 */
	public SpectrumWriterFactory(SPECCHIOClient specchio_client) {
		this.specchio_client = specchio_client;
	}
	
	
	/**
	 * Get a writer for a given output stream and file type.
	 * 
	 * @param os		the output stream
	 * @param type		the file type
	 * @param header	HeaderBody.Header or HeaderBody.Body
	 * 
	 * @return a new SpectrumWriter
	 * 
	 * @throws IllegalArgumentException	type is not a recognised file type
	 */
	public SpectrumWriter getWriter(OutputStream os, int type, int header) {
		
		if (type == FileTypes.CSV) {
			return CsvWriter.newInstance(os, header, specchio_client);
		} else if (type == FileTypes.ENVI_SLB) {
			return ENVIWriter.newInstance(os, header);
		} else {
			throw new IllegalArgumentException("Unrecognised file type " + type + " passed to SpectrumWriter.");
		}
		
	}

}
