package ch.specchio.file.writer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ch.specchio.types.*;
import org.joda.time.DateTime;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.constants.FileTypes;
import ch.specchio.constants.HeaderBody;
import ch.specchio.constants.TimeFormats;
import ch.specchio.spaces.Space;


/**
 * Class for writing spectral data as a comma-separated values.
 */
public abstract class CsvWriter extends SpectrumWriter {
	
	/** separator character */
	private static String separator = ",";
	
	/** buffered writer for output */
	private BufferedWriter bw;
	
	/**
	 * Constructor.
	 *
	 * @param os		the output stream upon which to write
	 * @param header	HeaderBody.Header or HeaderBody.Body
	 */
	protected CsvWriter(OutputStream os, int header, SPECCHIOClient specchio_client) {
		
		super(os, FileTypes.CSV, header, specchio_client);
		
		// create a buffered writer for output
		bw = new BufferedWriter(new OutputStreamWriter(os));
		
	}
	
	
	/**
	 * Finish writing the current space.
	 * 
	 * @throws IOException	could not write to output
	 */
	public void endSpace() throws IOException {
		
		// flush the buffer
		bw.flush();
		
		super.endSpace();
		
	}
	
	
	/**
	 * Get a new instance of a CSV writer
	 * 
	 * @param os		the output stream upon which to write
	 * @param header	HeaderBody.Header or HeaderBody.Body
	 * 
	 * @throws IllegalArgumentException invalid value for header
	 */
	public static CsvWriter newInstance(OutputStream os, int header, SPECCHIOClient specchio_client) {
		
		if (header == HeaderBody.Header) {
			return new CsvHdrWriter(os, specchio_client);
		} else if (header == HeaderBody.Body) {
			return new CsvBodyWriter(os, specchio_client);
		} else {
			throw new IllegalArgumentException("Unrecognised header-body value passed to CsvWriter.newInstance().");
		}
	}
	
	
	/**
	 * Write a field.
	 * 
	 * @param value	the value of the field
	 * 
	 * @throws IOException	could not write to the output
	 */
	protected void writeField(String value) throws IOException {
		
		// escape special characters
		boolean specialCharacters = false;
		String escapedValue = null;
		if (value != null) {
			specialCharacters = value.contains(separator) || value.contains("\n") || value.contains("\"");
			escapedValue = value.replaceAll("\"", "\"\"");
		}
		
		// output the value, with quotes if necessary
		if (specialCharacters) {
			bw.write("\"");
		}
		if (escapedValue != null) {
			bw.write(escapedValue);
		}
		if (specialCharacters) {
			bw.write("\"");
		}
		
	}
	
	
	/**
	 * Write a field separator.
	 * 
	 * @throws IOException	could not write to the output
	 */
	protected void writeFieldSeparator() throws IOException {
		
		bw.write(separator);
		
	}
	
	
	/**
	 * Write a record separator.
	 * 
	 * @throws IOException	could not write to the output
	 */
	protected void writeRecordSeparator() throws IOException {
		
		bw.newLine();
		
	}
	
}

		
/**
 * CSV header writer.
 */
class CsvHdrWriter extends CsvWriter {
	
	/** date formatter */
	private static final DateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
	
	/** the list of spectra to be written out */
	private List<Spectrum> spectra;
	
	/** the set of metadata fields to be written out */
	private Set<String> metadataFields;
	
	/** the set of attribute names to be written out */
	private Set<String> attributeNames;
	
	/**
	 * Constructor.
	 * 
	 * @param os	the output stream being written to
	 */
	public CsvHdrWriter(OutputStream os, SPECCHIOClient specchio_client) {
		
		super(os, HeaderBody.Header, specchio_client);
		
		// initialise member variables
		spectra = new LinkedList<Spectrum>();
		metadataFields = new TreeSet<String>();
		attributeNames = new TreeSet<String>();
		
	}
	
	
	/**
	 * Finish writing the current space.
	 * 
	 * @throws IOException	could not write to output
	 */
	public void endSpace(Boolean combine) throws IOException {
// ---------------------- START WITH THE HEADERS OF METADATA, ATTRIBUTE COLUMNS AND MEASUREMENT:

		// write one row for every metadata field
		for (String metadataField : metadataFields) {
			
			try {
			
				// work out the name of this field
				String metadataName = null;
				Iterator<Spectrum> iter = spectra.iterator();
				while (metadataName == null && iter.hasNext()) {
					Spectrum s = iter.next();
					Object value = s.getMetadataValue(metadataField);
					if (value != null && value instanceof MetaDatatype) {
						// use the name supplied in this metadata value object
						metadataName = ((MetaDatatype<?>)value).name;
					}
				}
				if (metadataName == null) {
					// not a MetaDatatype; make up something from the field name
					metadataName = inferMetadataName(metadataField);
				}
				
				// write the name of the field
				writeField(metadataName);

			}
			catch (NoSuchMethodException ex) {
				// should never happen if Spectrum.METADATA_FIELDS is correct
				ex.printStackTrace();
			}
			
			// write end of the record
			writeFieldSeparator();
			
		}

		// write one row for every attribute
		Hashtable<String, Integer> max_mp_per_attribute_LUT = new Hashtable<String, Integer>();
		
		for (String attributeName : attributeNames) {
			
			
			int max_number_of_entries = 0;
			// get number of entries 
			for (Spectrum s : spectra) {
				int number_of_entries = s.getMetadata().get_all_entries(attributeName).size();
				
				if(number_of_entries > max_number_of_entries)
					max_number_of_entries = number_of_entries;
			}
			
			// store the number of max entries for the attribute in a lookup table
			max_mp_per_attribute_LUT.put(attributeName, max_number_of_entries);
			
			// write the name of the attribute
			for(int entry_index =0;entry_index < max_number_of_entries; entry_index++)
			{
				writeField(attributeName);
				writeFieldSeparator();
			}
			
		}

		// write one row for the campaign id
		writeField("CampaignId");
		writeFieldSeparator();

		// write one row for the spectrum id
		writeField("SpectrumId");		// write the attribute's value for each spectrum

		if(combine){
            writeFieldSeparator();
			for (int channel = 0; channel < getCurrentSpace().getDimensionality(); channel++){
				if (channel != 0) {
					writeFieldSeparator();
				}
				writeField(Double.toString(getCurrentSpace().get_dimension_number(channel)));
			}
		}
		writeRecordSeparator();



// ---------------------- ADD THE VALUES
		for (Spectrum s : spectra) {

			// METADATA
			for (String metadataField : metadataFields){
			// write the fields's value
				Object value = null;
				try {
					value = s.getMetadataValue(metadataField);
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
				if (value != null) {
				writeField(value.toString());
			}
			// write a field separator
			writeFieldSeparator();
			}

			// ATTRIBUTES
			for (String attributeName : attributeNames){
//				if(s.getMetadata().get_all_entries(attributeName).size() > 0){
					
					// handle multiple entries per attribute
					int no_of_mps = s.getMetadata().get_all_entries(attributeName).size();
					int max_mp_per_attribute = max_mp_per_attribute_LUT.get(attributeName);
					
					for(int i=0; i< max_mp_per_attribute; i++)
					{	
						if(i < no_of_mps)
						{
							MetaParameter mp = s.getMetadata().get_all_entries(attributeName).get(i);
							if (mp != null && mp.getValue() != null) {

								if (mp instanceof MetaDate) {

									// output date according to the time format setting
									DateTime date = (DateTime) mp.getValue();
									if (getTimeFormat() == TimeFormats.Seconds) {
										writeField(Long.toString(date.getMillis()));
									} else {
										//writeField(df.format(date));
										writeField(mp.valueAsString());
									}
								}
								else if (mp instanceof MetaTaxonomy)
								{
									Hashtable<Integer, String> hash = this.specchio_client.getTaxonomyIdToNameHash(mp.getAttributeId());


									long id = (long) mp.getValue();

									String taxonomy_field_name = hash.get((int) id);						
									writeField(taxonomy_field_name);

								} else {

									// convert the value to its string form
									writeField(mp.valueAsString());
								}
							}
						}
						else
						{
							// this spectrum has no further entries for this attribute, but some other spectrum has them within the exported set.
							// field is just left empty
						}
						
						writeFieldSeparator(); // separate each entry
					}

//				}
//				else {
//					writeField("NA");
//					writeFieldSeparator();
//				}
//				try {
//				}catch (java.lang.IndexOutOfBoundsException e) {
//					writeField("NA");
//				}
				
			}
			writeField(Integer.toString(s.campaign_id));
			writeFieldSeparator();
			writeField(Integer.toString(s.spectrum_id));

			if(combine){
                writeFieldSeparator();
				// MEASUREMENT VALUES
				for (int channel = 0; channel < getCurrentSpace().getDimensionality(); channel++) {
					if (channel != 0) {
						writeFieldSeparator();
					}
					writeField(Double.toString(getCurrentSpace().get_vector_element(s.getSpectrumId(), channel)));
				}
			}
			writeRecordSeparator();

		}



// -------------------------------------------------------------
		//		HERE THE SPECTRA NEED TO BE WRITTEN IN THE ROWS!
//		// write the field's value for each spectrum
//		for (Spectrum s : spectra) {
//
//			// write a field separator
//			writeFieldSeparator();
//
//			// write the fields's value
//			Object value = s.getMetadataValue(metadataField);
//			if (value != null) {
//				writeField(value.toString());
//			}
//
//		}
//
//		// write the attribute's value for each spectrum
//		int i = 0;
//		for (Spectrum s : spectra) {
//
//			// write a field separator
//			writeFieldSeparator();
//
//			// write the attribute's value if it exists
//			if(number_of_entries_list.get(i) > entry_index)
//			{
//				MetaParameter mp = s.getMetadata().get_all_entries(attributeName).get(entry_index);
//				if (mp != null && mp.getValue() != null) {
//
//					if (mp instanceof MetaDate) {
//
//						// output date according to the time format setting
//						DateTime date = (DateTime) mp.getValue();
//						if (getTimeFormat() == TimeFormats.Seconds) {
//							writeField(Long.toString(date.getMillis()));
//						} else {
//							//writeField(df.format(date));
//							writeField(mp.valueAsString());
//						}
//					} else {
//
//						// convert the value to its string form
//						writeField(mp.valueAsString());
//					}
//				}
//			}
//
//			i++;
//
//		}
//
//		// write the end of the record
//		writeRecordSeparator();


//		// write the attribute's value for each spectrum
//		for (Spectrum s : spectra) {
//			// write a field separator
//			writeFieldSeparator();
//			writeField(Integer.toString(s.campaign_id));
//		}
//		for (Spectrum s : spectra) {
//			// write a field separator
//			writeFieldSeparator();
//			writeField(Integer.toString(s.spectrum_id));
//		}
//
//		writeRecordSeparator();


		super.endSpace();
		
	}


	/**
	 * Infer the name of a metadata field given its SQL field name.
	 * 
	 * @param field	a field name from Spectrum.METADATA_FIELDS
	 * 
	 * @return a name constructed from the input string
	 */
	private String inferMetadataName(String field) {
		
		StringBuffer sbuf = new StringBuffer();
		String parts[] = field.split("_");
		for (String part : parts) {
			if (!part.equals("id") && part.length() > 0) {
				
				if (sbuf.length() > 0) {
					// insert a space
					sbuf.append(' ');
				}
				
				// capitalise the first letter of the field name
				sbuf.append(part.substring(0, 1).toUpperCase());
				
				if (part.length() > 1) {
					// append the rest of the field name in its ordinary case
					sbuf.append(part.substring(1));
				}
			}
			
		}
		
		return sbuf.toString();
		
	}
	
	
	/**
	 * Start writing a new space.
	 * 
	 * @param spaceIn	the space
	 * 
	 * @throws IOException	could not write to output
	 */
	public void startSpace(Space spaceIn) throws IOException {
		
		super.startSpace(spaceIn);
		
		// reset lists
		spectra.clear();
		metadataFields.clear();
		attributeNames.clear();
		
	}
	
	
	/**
	 * Write a spectrum to the output stream.
	 * 
	 * @param s	the spectrum to be written
	 * 
	 * @throws IOException	could not write to the output stream
	 */
	public void writeSpectrum(Spectrum s) throws IOException {
		
		// add the spectrum to the list to be written
		spectra.add(s);
		
		// make sure all of this spectrum's non-null metadata is included in the set to be output
		for (String field : Spectrum.METADATA_FIELDS) {
			try {
				if (s.getMetadataId(field) != 0) {
					metadataFields.add(field);
				}
			}
			catch (NoSuchMethodException ex) {
				// should never happen if Spectrum.METADATA_FIELDS is correct
				ex.printStackTrace();
			}
		}
		
		// make sure all of this spectrum's non-binary attributes are contained in the set to be output
		for (MetaParameter mp : s.getMetadata().getEntries()) {
			if (!"binary_val".equals(mp.getDefaultStorageField())) {
				attributeNames.add(mp.getAttributeName());
			}
		}
		
	}

}


/**
 * CSV body writer.
 */
class CsvBodyWriter extends CsvWriter {
	
	/** number formatter */
	private static final DecimalFormat df = new java.text.DecimalFormat("###.#########E0");
	
	/** the list of spectra to be written out */
	private List<Spectrum> spectra;

	/**
	 * Constructor.
	 * 
	 * @param os	the output stream being written to
	 */
	public CsvBodyWriter(OutputStream os, SPECCHIOClient specchio_client) {
		
		super(os, HeaderBody.Body, specchio_client);
		
		// initialise member variables
		spectra = new LinkedList<Spectrum>();
		
	}
	
	
	/**
	 * Finish writing the current space.
	 * 
	 * @throws IOException	could not write to output
	 */
	public void endSpace() throws IOException {

		for (int channel = 0; channel < getCurrentSpace().getDimensionality(); channel++){
			writeField(df.format(getCurrentSpace().get_dimension_number(channel)));
            if(channel < (getCurrentSpace().getDimensionality() - 1)) writeFieldSeparator();
		}
		writeRecordSeparator();
		for(Spectrum s : spectra){
			for (int channel = 0; channel < getCurrentSpace().getDimensionality(); channel++) {
				writeField(Double.toString(getCurrentSpace().get_vector_element(s.getSpectrumId(), channel)));
                if(channel < getCurrentSpace().getDimensionality() - 1) writeFieldSeparator();
			}
			writeRecordSeparator();
		}
		super.endSpace();
	}


	
	
	/**
	 * Start writing a new space.
	 * 
	 * @param spaceIn	the space
	 * 
	 * @throws IOException	could not write to output
	 */
	public void startSpace(Space spaceIn) throws IOException {
		
		super.startSpace(spaceIn);
		
		// reset lists
		spectra.clear();
		
	}
	
	
	/**
	 * Write a spectrum to the output stream.
	 * 
	 * @param s	the spectrum to be written
	 * 
	 * @throws IOException	could not write to the output stream
	 */
	public void writeSpectrum(Spectrum s) throws IOException {
		
		// add the spectrum to the list to be output
		spectra.add(s);
		
	}


	public void endSpaceCombined() throws IOException{

	}

}
