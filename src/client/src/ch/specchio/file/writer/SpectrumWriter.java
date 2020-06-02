package ch.specchio.file.writer;

import java.io.IOException;
import java.io.OutputStream;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.spaces.Space;
import ch.specchio.types.Spectrum;


/**
 * Abstract class for writing spectral data.
 */
public abstract class SpectrumWriter {
	
	/** the output stream */
	private OutputStream os;
	
	/** the current space */
	private Space space;
	
	/** time format from TimeFormats */
	private int timeFormat;
	
	/** use units from the spectrum objects? */
	private boolean useSpectrumUnits;
	
	SPECCHIOClient specchio_client;
	
	
	/**
	 * Constructor.
	 * 
	 * @param osIn				the output stream being written to
	 * @param typeIn			the file type
	 * @param headerIn			HeaderBody.Header or HeaderBody.Body
	 * @param specchio_client	reference to the specchio client (used for e.g. taxonomy translations)
	 */
	public SpectrumWriter(OutputStream osIn, int typeIn, int headerIn, SPECCHIOClient specchio_client) {
		
		this.specchio_client = specchio_client;
		
		// save references to the input parameters
		os = osIn;
		
		// initialise member variables
		space = null;
		useSpectrumUnits = false;
		
	}
	
	/**
	 * Constructor.
	 * 
	 * @param osIn		the output stream being written to
	 * @param typeIn	the file type
	 * @param headerIn	HeaderBody.Header or HeaderBody.Body
	 */
	public SpectrumWriter(OutputStream osIn, int typeIn, int headerIn) {
		
		// save references to the input parameters
		os = osIn;
		
		// initialise member variables
		space = null;
		useSpectrumUnits = false;
		
	}	
	
	
	/**
	 * Finish writing the current space.
	 * 
	 * @throws IOException	could not write to output
	 */
	public void endSpace() throws IOException {
		
		// flush the write buffer
		os.flush();
		
		// reset for a new space
		space = null;
		
	}


	/**
	 * Finish writing the current space.
	 * @param combine indicates if the measurement should be appended at the end of the metadata and attribute columns
	 * @throws IOException	could not write to output
	 */
	public void endSpace(Boolean combine) throws IOException{
		// flush the write buffer
		os.flush();

		// reset for a new space
		space = null;
	}
	
	
	/**
	 * Get a reference to the Space object being written.
	 * 
	 * @return a reference to the current Space object
	 */
	public Space getCurrentSpace() {
		
		return space;
		
	}
	
	
	/**
	 * Get a reference to the output stream.
	 * 
	 * @return a reference to the output stream
	 */
	public OutputStream getOutputStream() {
		
		return os;
		
	}
	
	
	/**
	 * Get the time format.
	 * 
	 * @return TimeFormat.Seconds or TimeFormats.Formatted
	 */
	public int getTimeFormat() {
		
		return timeFormat;
		
	}
	
	
	/**
	 * Start writing a new space.
	 * 
	 * @param spaceIn	the space
	 * 
	 * @throws IOException	could not write to output
	 */
	public void startSpace(Space spaceIn) throws IOException {
		
		space = spaceIn;
		
	}
	
	
	/**
	 * Set the time format.
	 * 
	 * @param timeFormatIn	TimeFormats.Seconds or TimeFormats.Formatted
	 */
	public void setTimeFormat(int timeFormatIn) {
		
		timeFormat = timeFormatIn;
		
	}
	
	
	/**
	 * Set whether or not to use the units specified by the Spectrum objects.
	 * 
	 * @param useSpectrumUnitsIn	true or false
	 */
	public void setUseSpectrumUnits(boolean useSpectrumUnitsIn) {
		
		useSpectrumUnits = useSpectrumUnitsIn;
		
	}
	
	
	/**
	 * Return whether or not to use the units specified by the Spectrum objects.
	 * 
	 * @return true or false
	 */
	public boolean useSpectrumUnits() {
		
		return useSpectrumUnits;
		
	}
	
	
	/**
	 * Write a spectrum to the output stream.
	 * 
	 * @param s	the spectrum to be written
	 * 
	 * @throws IOException	could not write to the output stream
	 */
	public abstract void writeSpectrum(Spectrum s) throws IOException;
	
}
