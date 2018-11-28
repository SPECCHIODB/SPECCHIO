package ch.specchio.file.writer;

import java.io.OutputStream;

import ch.specchio.constants.FileTypes;

/**
 * Factory class for table writers.
 */
public class TableWriterFactory {
	
	/**
	 * Constructor.
	 */
	public TableWriterFactory() {
		
	}
	
	
	/**
	 * Instantiate a writer for a given output stream and file type.
	 * 
	 * @param os	the output stream
	 * @param type	FileTypes.CAMPAIGN_EXPORT_XML
	 *
	 * @return a new TableWriter
	 * 
	 * @throws IllegalArgumentException	type is not a valid file type
	 */
	public TableWriter getWriter(OutputStream os, int type) {
		
		if (type == FileTypes.CAMPAIGN_EXPORT_XML) {
			return new CampaignExportXMLWriter(os);
		} else {
			throw new IllegalArgumentException("Unknown file type passed to TableWriter.getWriter().");
		}
		
	}

}
