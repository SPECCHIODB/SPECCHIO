package ch.specchio.client;

import javax.ws.rs.WebApplicationException;


public class SPECCHIOClientException extends WebApplicationException {

	/** serialisation version ID */
	private static final long serialVersionUID = 1L;
	
	/** the detailed message */
	protected String details;
	
	protected String message = "bollocks";
	
	/** the ultimate cause of the exception */
	protected Throwable cause;
	
	
	/**
	 * Constructor for re-throwing an exception with a new detail message.
	 */
	public SPECCHIOClientException(String message, Exception ex) {
		
		//super(message, ex);
		super(ex);
		
		this.message = message;
		
		// initialise member variables
		init(ex);
		
	}
	
	
	/**
	 * Constructor for re-throwing an exception.
	 */
	public SPECCHIOClientException(Exception ex) {
		
		super(ex);
		
		// initialise member variables
		init(ex);
		
	}
	
	
	/**
	 * Constructor for throwing a new exception.
	 * 
	 * @param message	the message to be reported by the exception
	 */
	public SPECCHIOClientException(String message) {
		
		//super(message);
		this.message = message;
		// initialise member variables
		init(null);
		
	}
	
	
	/**
	 * Initialise member variables.
	 * 
	 * @param ex	the cause
	 */
	private void init(Exception ex) {
		
		// set the cause
		if (ex != null) {
			cause = ex.getCause();
			setStackTrace(ex.getStackTrace());
		} else {
			cause = null;
		}
		
		// put the stack trace into the detailed message
		StringBuffer sbuf = new StringBuffer();
		if (ex != null) {
			sbuf.append(ex.getMessage());
		} else if (cause != null) {
			sbuf.append(cause.getMessage());
		} else {
			sbuf.append(getMessage());
		}
		for (StackTraceElement elem : getStackTrace()) {
			sbuf.append("\n  " + elem.toString());
		}
		details = sbuf.toString();
		
	}
	
	
	/**
	 * Get a detailed error message.
	 * 
	 * @return the stack trace
	 */
	public String getDetails() {
		
		return details;
		
	}
	
	
	/**
	 * Get an error message suitable for displaying to the user.
	 *
	 * @return a brief user-readable error message describing the error
	 */
	public String getUserMessage() {
		
		String msg = getMessage();
		
		if(msg == null)
			return this.message; // fallback solution when no proper exception was used to create this one 
		
		return getMessage();
		
	}

}
