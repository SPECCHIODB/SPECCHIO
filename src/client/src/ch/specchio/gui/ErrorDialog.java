package ch.specchio.gui;

import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


import ch.specchio.client.SPECCHIOClientException;

public class ErrorDialog extends JDialog implements ActionListener {

	/** serialisation ID */
	private static final long serialVersionUID = 1L;
	
	/** the exception being displayed */
	private Exception exception;
	
	/** constraints for laying out the dialogue */
	private GridBagConstraints constraints;
	
	/** the root panel of the dialogue */
	private JPanel panel;
	
	/** the label containing the icon */
	private JLabel iconComponent;
	
	/** the panel containing the error message */
	private JPanel messageComponent;
	
	/** the button for dismissing the dialogue */
	private JButton dismissButton;
	
	public JButton getDismissButton() {
		return dismissButton;
	}


	public void setDismissButton(JButton dismissButton) {
		this.dismissButton = dismissButton;
	}


	/** the button for requesting more details */
	private JButton detailsButton;
	
	/** the text area for displaying a detailed error message */
	private JTextArea detailsArea;
	
	/** the scroll pane for the detailed error message */
	private JScrollPane detailsPane;
	
	/** the string for the dismiss button */
	private static final String DISMISS = "Dismiss";
	
	/** the string for the details button when details are not displayed */
	private static final String GET_DETAILS = "Details...";
	
	/** the string for the details button when details are displayed */
	private static final String REMOVE_DETAILS = "Less...";
	
	
	/**
	 * Constructor.
	 * 
	 * @param owner		the owner of this dialogue
	 * @param title		the title of the dialogue
	 */
	public ErrorDialog(Frame owner, String title) {
		
		super(owner, title, true);
		
	}
	
	
	/**
	 * Constructor.
	 * 
	 * @param owner		the owner of this dialogue
	 * @param title		the title of the dialogue
	 * @param message	the message for this dialogue
	 * @param ex		the exception that caused the error (may be null)
	 */
	public ErrorDialog(Frame owner, String title, String message, Exception ex) {
		
		super(owner, title, true);
		init(message, ex);
		
	}
	
	
	/**
	 * Constructor.
	 * 
	 * @param owner		the owner of this dialogue
	 * @param title		the title of the dialogue
	 * @param message	the message for this dialogue
	 * @param ex		the exception that caused the error (may be null)
	 */
	public ErrorDialog(Dialog owner, String title, String message, Exception ex) {
		
		super(owner, title, true);
		init(message, ex);
		
	}
	
	
	/**
	 * Initialise the dialogue for a given message and exception.
	 * 
	 * @param message	the message for the dialogue
	 * @param ex		the exception that caused the error (may be null)
	 */
	public void init(String message, Exception ex) {
		
		// save the input parameters for later
		exception = ex;
		
		// create a panel with a grid bag layout to contain all of the components
		constraints = new GridBagConstraints();
		constraints.insets = new Insets(5, 5, 5, 5);
		panel = new JPanel(new GridBagLayout());
		getContentPane().add(panel);
		
		// place the icon at the top left
		Icon icon = UIManager.getIcon("OptionPane.errorIcon");
		iconComponent = new JLabel(icon);
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridheight = 2;
		panel.add(iconComponent, constraints);
		
		// place the message at the top right
		messageComponent = new JPanel();
		messageComponent.setLayout(new BoxLayout(messageComponent, BoxLayout.Y_AXIS));
		
		
		
		for (String line : message.split("\n")) {
			String formatted_line = formatMessage(line, true);
			messageComponent.add(new JLabel(formatted_line));
		}
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.gridheight = 1;
		constraints.gridwidth = 3;
		panel.add(messageComponent, constraints);
		
		// place the "dismiss" button underneath and to the right of the message
		dismissButton = new JButton(DISMISS);
		dismissButton.setActionCommand(DISMISS);
		dismissButton.addActionListener(this);
		constraints.gridx = 3;
		constraints.gridy = 1;
		constraints.gridheight = 1;
		constraints.gridwidth = 1;
		constraints.anchor = GridBagConstraints.EAST;
		panel.add(dismissButton, constraints);
		
		if (exception != null) {
			
			// create a button for requesting further details beneath the message
			detailsButton = new JButton(GET_DETAILS);
			detailsButton.setActionCommand(GET_DETAILS);
			detailsButton.addActionListener(this);
			constraints.gridx = 2;
			constraints.gridy = 1;
			constraints.gridheight = 1;
			constraints.gridwidth = 1;
			constraints.anchor = GridBagConstraints.EAST;
			panel.add(detailsButton, constraints);
			
			// create the details text area but don't display it yet
			detailsArea = new JTextArea(25, 80);
			detailsPane = new JScrollPane(detailsArea);
			detailsPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			constraints.gridx = 1;
			constraints.gridy = 2;
			constraints.gridheight = 1;
			constraints.gridwidth = 3;
			constraints.anchor = GridBagConstraints.CENTER;
			
		}
		
		pack();
		
	}
	
	
	/**
	 * Format the details text area.
	 * 
	 * @param ex	the exception whose details are to be displayed
	 */
	private void initDetailsArea(Exception ex) {

		if (ex instanceof SPECCHIOClientException) {
			// use the details message stored inside the exception
			String details = ((SPECCHIOClientException)ex).getDetails();
			
			String formatted_msg = formatMessage(details, false);
			detailsArea.setText(formatted_msg);
					
			
		} else {
			// show the stack trace
			detailsArea.append(ex.getMessage());
			for (StackTraceElement elem : ex.getStackTrace()) {
				detailsArea.append("\n  " + elem.toString());
			}
		}
		
	}
	
	public String formatMessage(String message, boolean extract_message_only)
	{
		String formatted_msg = "";
		
		if (message != null && message.length() > 0 && message.charAt(0) == '<') {
			
			// the details message may contain an XML or HTML response from the web server; try to parse it
			try {
				// create a document factory with all validation disabled (http://stackoverflow.com/questions/6204827/xml-parsing-too-slow)
				DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
				documentFactory.setNamespaceAware(false);
				documentFactory.setValidating(false);
				documentFactory.setFeature("http://xml.org/sax/features/namespaces", false);
				documentFactory.setFeature("http://xml.org/sax/features/validation", false);
				documentFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
				documentFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

				// try to parse the document as XML
				StringReader reader = new StringReader(message);
				DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
				Document document = documentBuilder.parse(new InputSource(reader));
				reader.close();
				
				// now output it to a string with nice indenting
				StringWriter writer = new StringWriter();
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.METHOD, "html");
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				transformer.transform(new DOMSource(document), new StreamResult(writer));
				writer.close();
				
				// display it in the text area
				if(extract_message_only)
				{
					formatted_msg = writer.toString(); // default unformatted value
					// see: https://stackoverflow.com/questions/19932570/getelementsbytagname-searching-down-all-levels-of-xml-nodes
		            
		            XPathFactory xpathFactory = XPathFactory.newInstance();
		            XPath xpath = xpathFactory.newXPath();
		            try {
						Element element = (Element) xpath.evaluate("//error_message/message", document, XPathConstants.NODE);
						
						if(element == null) // try HTML extraction
						{
							org.jsoup.nodes.Document html_document = Jsoup.parse(message);

							
							org.jsoup.nodes.Element body = html_document.select("body").first();
							
							Iterator<org.jsoup.nodes.Element> it = body.select("p").iterator();
							
							org.jsoup.nodes.Element description = null;
							
							while(it.hasNext())
							{
								org.jsoup.nodes.Element elem = it.next();
								
								Elements descr1 = elem.getElementsMatchingText("description");
								
								if(descr1.size() > 0)
								{
									description = descr1.get(0);
								}
								
							}
							
							org.jsoup.nodes.Element descr2 = body.select("pre").first();
							
							String details = "";
							if(descr2 != null) details = descr2.text();
							
							org.jsoup.nodes.Node error_description = description.getAllElements().get(0).childNode(1);
							String error = error_description.toString();
							
							formatted_msg = error + ": " + details;
							
							
						}
						else
						{
							formatted_msg = element.getTextContent();	
						}
							
											
					} catch (XPathExpressionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					

				}
				else
					formatted_msg = writer.toString();
			
			}
			catch (IOException ex2) {
				// not sure why this would happen; display the unparsed message
				formatted_msg = message;
			}
			catch (ParserConfigurationException ex2) {
				// could not find an XML parser; display the error and the unparsed message
				formatted_msg = ex2.getMessage() + "\n" + message;
			}
			catch (SAXException ex2) {
				// not a valid XML document; assume the details are in plain text
				formatted_msg = message;
			}
			catch (java.lang.IndexOutOfBoundsException ex2) {
				// parsing issue
				formatted_msg = message;
			}	
			catch (TransformerConfigurationException ex2) {
				// could not an XML transformer; display the error and the unparsed message
				formatted_msg = ex2.getMessage() + "\n" + message;
			}
			catch (TransformerException ex2) {
				// transformation failed; display the error the unparsed message
				formatted_msg = ex2.getMessage() + "\n" + message;
			}
			
		} else {
			
			// display the details message
			formatted_msg = message;
			
		}		
		
		return formatted_msg;
	}
	
	
	/**
	 * Handle button clicks.
	 */
	public void actionPerformed(ActionEvent event) {
		
		if (GET_DETAILS.equals(event.getActionCommand())) {
			
			// add the details pane
			panel.add(detailsPane, constraints);
			
			// re-label the button
			detailsButton.setText(REMOVE_DETAILS);
			detailsButton.setActionCommand(REMOVE_DETAILS);
			
			// make sure the details area is initialised
			if (detailsArea.getText().length() == 0) {
				startOperation();
				initDetailsArea(exception);
				endOperation();
			}
			
			// re-draw the panel
			pack();
			panel.revalidate();
			panel.repaint();
			
		} else if (REMOVE_DETAILS.equals(event.getActionCommand())) {
			
			// remove the details pane
			panel.remove(detailsPane);
			
			// re-label the button
			detailsButton.setText(GET_DETAILS);
			detailsButton.setActionCommand(GET_DETAILS);
			
			// re-draw the panel
			pack();
			panel.revalidate();
			panel.repaint();
			
		} else if (DISMISS.equals(event.getActionCommand())) {
			
			// dismiss the dialogue
			setVisible(false);
			
		}
		
	}
	
	
	/**
	 * Handler for ending a potentially long-running operation.
	 */
	private void endOperation() {
		
		// change the cursor to its default start
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		
	}
	
	
	/**
	 * Handler for starting a potentially long-running operation.
	 */
	private void startOperation() {
		
		// change the cursor to its "wait" state
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
	}

}
