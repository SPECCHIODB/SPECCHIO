package ch.specchio.client;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ConnectException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.xml.bind.UnmarshalException;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;

public class SPECCHIOWebClientException extends SPECCHIOClientException {

	/** serialisation version ID */
	private static final long serialVersionUID = 1L;
	
	/** the user message */
	private final String userMessage;
	
	
	/**
	 * Constructor for re-throwing a client handler exception thrown by Jersey.
	 */
	public SPECCHIOWebClientException(ClientHandlerException ex) {
		
		super(ex.getMessage(), ex);
		
		// get a friendly error message for the exception
		if (ex.getCause() instanceof ConnectException) {
			userMessage = "Could not connect to the server. Please check that the server is running at the server and port specified.";
		} else if (ex.getCause() instanceof UnknownHostException) {
			userMessage = "Unknown server name. Please check that the server name is correct.";
		} else if (ex.getCause() instanceof SSLHandshakeException) {
			userMessage = "\nLikely, the solution to this problem is documented in the FAQ:\nhttps://specchio.ch/faq/#i-cannot-connect-to-the-database-due-to-a-certificate-error-13-feb-2020";
		} else if (ex.getCause() instanceof SSLException) {
			userMessage = "Could not find any trusted security certificates.\nPlease check that the specchio.keystore file exists and is correct.";
		} else {
			userMessage = null;
		}
		
	}
	
	
	/**
	 * Constructor for re-throwing a uniform interface exception thrown by Jersey.
	 */
	public SPECCHIOWebClientException(UniformInterfaceException ex) {
		
		super(ex.getMessage(), ex);
		
		// the user message is the same as the technical message
		userMessage = ex.getMessage();
		
		// copy the response body into the detailed message
		StringBuffer sbuf = new StringBuffer();
		try {
			char b[] = new char[1024];
			Reader reader = new InputStreamReader(ex.getResponse().getEntityInputStream());
			int n = reader.read(b, 0, b.length);
			while (n >= 0) {
				sbuf.append(b, 0, n);
				n = reader.read(b, 0, b.length);
			}
			reader.close();
		}
		catch (IOException ex2) {
			// not sure why this would happen; nothing we can do about it anyway
			sbuf.append("\n " + ex2.getMessage());
		}
		details = sbuf.toString();
		
	}
	
	
	/**
	 * Constructor for re-throwing an illegal argument exception thrown by Jersey.
	 * 
	 * @param ex	the original exception
	 */
	public SPECCHIOWebClientException(IllegalArgumentException ex) {
		
		super(ex.getCause().getMessage(), ex);

		// set up user message
		userMessage = "The host name, port number or application path contains an illegal character. Please check that they are typed correctly.";
		
	}
	
	
	/**
	 * Constructor for throwing a new exception.
	 * 
	 * @param message	the message to be reported by the exception
	 */
	public SPECCHIOWebClientException(String message) {
		
		super(message);
		
		// use the default messages
		details = super.getDetails();
		userMessage = message;
		
	}
	
	
	public SPECCHIOWebClientException(javax.ws.rs.WebApplicationException ex) {
		super(ex.getCause().getMessage(), ex);
		
		userMessage = message;
	}


	/**
	 * Get an error message suitable for displaying to the user.
	 *
	 * @return a brief user-readable error message describing the error
	 */
	public String getUserMessage() {
		
		return (userMessage != null)? userMessage : super.getUserMessage();
		
	}

}
