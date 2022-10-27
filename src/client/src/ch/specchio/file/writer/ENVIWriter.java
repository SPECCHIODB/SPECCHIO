package ch.specchio.file.writer;

import java.io.BufferedWriter;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import ch.specchio.constants.FileTypes;
import ch.specchio.constants.HeaderBody;
import ch.specchio.spaces.Space;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.Spectrum;

/**
 * Class for writing spectral data in ENVI SLB format.
 */
public abstract class ENVIWriter extends SpectrumWriter {
	
	/**
	 * Constructor.
	 *
	 * @param os		the output stream upon which to write
	 * @param header	HeaderBody.Header or HeaderBody.Body
	 */
	protected ENVIWriter(OutputStream os, int header) {
		
		super(os, FileTypes.ENVI_SLB, header);
		
	}
	
	/**
	 * Get a new instance of an ENVI SLB writer.
	 * 
	 * @param os		the output stream upon which to write
	 * @param header	HeaderBody.Header or HeaderBody.Body
	 * 
	 * @throws IllegalArgumentException invalid value for header
	 */
	public static ENVIWriter newInstance(OutputStream os, int header) {
		
		if (header == HeaderBody.Header) {
			return new ENVIHdrWriter(os);
		} else if (header == HeaderBody.Body) {
			return new ENVIBodyWriter(os);
		} else {
			throw new IllegalArgumentException("Unrecognised header-body value passed to CsvWriter.newInstance().");
		}
	}
	
}

		
/**
 * ENVI SLB header writer.
 */
class ENVIHdrWriter extends ENVIWriter {
	
	/** buffered writer for output */
	private BufferedWriter bw;
	
	/** the number of spectra written in the current space */
	private int count;
	
	/**
	 * Constructor.
	 * 
	 * @param os	the output stream being written to
	 */
	public ENVIHdrWriter(OutputStream os) {
		
		super(os, HeaderBody.Header);
		
		// initialise member variables
		count = 0;
		
		// output via a buffered writer
		bw = new BufferedWriter(new OutputStreamWriter(os));
		
	}
	
	
	/**
	 * Finish writing a space.
	 * 
	 * @throws IOException	could not write to output
	 */
	public void endSpace() throws IOException {
		
		if (count > 0) {
			bw.write("}");
		}
		bw.flush();
		bw.close();
		
		super.endSpace();
		
	}

	/**
	 * Finish writing the current space.
	 * @param combine indicates if the measurement should be appended at the end of the metadata and attribute columns
	 * @throws IOException	could not write to output
	 */
	public void endSpace(Boolean combine) throws IOException{

		this.endSpace();
	}
	
	
	/**
	 * Start writing a new space.
	 * 
	 * @param space	the space
	 * 
	 * @throws IOException	could not write to output
	 */
	public void startSpace(Space space) throws IOException {
		
		super.startSpace(space);
		
		// reset spectrum counter
		count = 0;
		
	}
	
	
	/**
	 * Write a spectrum to the output stream.
	 * 
	 * @param s	the Spectrum to be written
	 * 
	 * @throws IOException	could not write to the output stream
	 */
	@SuppressWarnings("unused")
	public void writeSpectrum(Spectrum s) throws IOException {
		
		if (count == 0) {
			
			// this is the first spectrum of a new space; write the preamble
			bw.write("ENVI");
			bw.newLine();
			bw.write("samples = ");
			bw.write(Integer.toString(getCurrentSpace().getDimensionality()));
			bw.newLine();
			bw.write("lines = ");
			bw.write(Integer.toString(getCurrentSpace().getSpectrumIds().size()));
			bw.newLine();
			bw.write("bands = 1");
			bw.newLine();
			bw.write("header offset = 0");
			bw.newLine();
			bw.write("file type = ENVI Spectral Library");
			bw.newLine();
			
			// Architecture specific data type setting
			if(Double.SIZE == 64)
				bw.write("data type = 5");
			if(Double.SIZE == 32)
				bw.write("data type = 4");
			
			bw.newLine();
			bw.write("interleave = BSQ");
			bw.newLine();
			bw.write("sensor type = ");
			bw.write(s.getSensor().getName().value);
			bw.newLine();
			bw.write("byte order = 1");
			bw.newLine();
			bw.write("wavelength units = Nanometers");
			bw.newLine();
			bw.write("x start = 1.00000");
			bw.newLine();
			bw.write("y start = 1.00000");
			bw.newLine();
			bw.write("band names = {Spectral Library}");
			bw.newLine();
			bw.write("wavelength = {");
			
			// list all wavelengths
			int cnt = 0;
			for(int i = 0; i < getCurrentSpace().getDimensionality(); i++)
			{
				bw.write(Double.toString(getCurrentSpace().get_dimension_number(i)));
				
				if(i < getCurrentSpace().getDimensionality() - 1)
					bw.write(", ");
				
				// do line breaks every 6 wavelengths
				if(cnt++ == 5)
				{
					bw.newLine();
					cnt = 0;
				}
			}
			
			bw.write("}");
			bw.newLine();
			
			// spectrum names will be added by subsequent calls to this method
			bw.write("spectra names = {");
			
		} else {
			
			// write a separator to indicate the end of the previous spectrum
			bw.write(",");
			
		}
		
		// write spectrum name
		bw.newLine();
		MetaParameter mp = s.getMetadata().get_first_entry("File Name");
		if (mp != null && mp.getValue() != null) {
			bw.write(mp.getValue().toString());
		}
	
		// increment counter
		count++;
		
	}

}


/**
 * ENVI SLB body writer.
 */
class ENVIBodyWriter extends ENVIWriter {

	/**
	 * Constructor.
	 * 
	 * @param os	the output stream being written to
	 */
	public ENVIBodyWriter(OutputStream os) {
		
		super(os, HeaderBody.Body);
		
	}
	
	
	/**
	 * Write a spectrum to the output stream.
	 * 
	 * @param s	the spectrum to be written
	 * 
	 * @throws IOException	could not write to the output stream
	 */
	public void writeSpectrum(Spectrum s) throws IOException {
		
		DataOutput dos = new DataOutputStream(getOutputStream());
		double[] vector = getCurrentSpace().getVector(s.getSpectrumId());
		
		for(int i = 0; i < getCurrentSpace().getDimensionality(); i++)
		{
			dos.writeDouble(vector[i]);		
		}
		
	}

}