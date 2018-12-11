package ch.specchio.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import java.awt.datatransfer.*;

import javax.activation.MimetypesFileTypeMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.JTextComponent;

import net.iharder.dnd.FileDrop;

import org.apache.commons.io.FilenameUtils;
import org.freixas.jcalendar.DateEvent;
import org.freixas.jcalendar.DateListener;
import org.freixas.jcalendar.JCalendar;
import org.freixas.jcalendar.JCalendarCombo;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.metadata.MDE_Controller;
import ch.specchio.metadata.MDE_Form;
import ch.specchio.metadata.MD_CategoryContainer;
import ch.specchio.metadata.MD_ChangeListener;
import ch.specchio.metadata.MD_EAV_Field;
import ch.specchio.metadata.MD_EAV_Link_Field;
import ch.specchio.metadata.MD_Field;
import ch.specchio.metadata.MD_Hierarchy_Field;
import ch.specchio.metadata.MD_Spectrum_Field;
import ch.specchio.types.ArrayListWrapper;
import ch.specchio.types.Campaign;
import ch.specchio.types.Capabilities;
import ch.specchio.types.CategoryTable;
import ch.specchio.types.ConflictInfo;
import ch.specchio.types.MetaBoolean;
import ch.specchio.types.MetaDate;
import ch.specchio.types.MetaLink;
import ch.specchio.types.MetaDocument;
import ch.specchio.types.MetaFile;
import ch.specchio.types.MetaImage;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.MetaParameterFormatException;
import ch.specchio.types.MetaSpatialGeometry;
import ch.specchio.types.MetaSpatialPoint;
import ch.specchio.types.MetaSpatialPolygon;
import ch.specchio.types.MetaSpatialPolyline;
import ch.specchio.types.MetaTaxonomy;
import ch.specchio.types.Point2D;
import ch.specchio.types.SerialisableBufferedImage;
import ch.specchio.types.TaxonomyNodeObject;
import ch.specchio.types.attribute;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LineString;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Polygon;

/**
 * Spectrum metadata panel. This panel displays all of the metadata for a spectrum.
 */
public class SpectrumMetadataPanel extends JPanel implements ListSelectionListener {

	/** serialisation version identifier */
	private static final long serialVersionUID = 1L;
	
	/** the frame that owns this panel */
	private Frame owner;
	
	/** the client object */
	private SPECCHIOClient specchioClient;
	
	/** the metadata form */
	private MDE_Form form;
	
	/** the list of metadata change listeners */
	private List<MD_ChangeListener> listeners;
	
	/** is the panel editable? */
	private boolean editable;
	
	private String previous_path = null;
	
	private SpectrumMetadataCategoryList category_list;
	private MDE_Controller mdec;
	JPanel metadata_panel;

	private JScrollPane spectrum_scroll_pane;

	private JScrollPane category_scroll_pane;
	
	/**
	 * Constructor.
	 * 
	 * @param owner				the frame that owns this panel
	 * @param specchioClient	the client object to use for contacting the server
	 */
	public SpectrumMetadataPanel(Frame owner, SPECCHIOClient specchioClient, MDE_Controller mdec) {
		
		super();
				
		// initialise member variables
		form = null;
		listeners = new LinkedList<MD_ChangeListener>();
		editable = true;
		
		// save a reference to the parameters for later
		this.owner = owner;
		this.specchioClient = specchioClient;
		this.mdec = mdec;
		addMetadataChangeListener(mdec);
		
		// set up vertical box layout
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		
		metadata_panel = new JPanel();
		metadata_panel.setLayout(new BoxLayout(metadata_panel, BoxLayout.Y_AXIS));
		
		// Create basic layout
		spectrum_scroll_pane = new JScrollPane(metadata_panel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		spectrum_scroll_pane.getVerticalScrollBar().setUnitIncrement(10);
					
		category_scroll_pane = new JScrollPane();
		
		category_scroll_pane.getVerticalScrollBar().setUnitIncrement(10);
		
		category_list = new SpectrumMetadataCategoryList(mdec.getFormFactory());
		category_list.addListSelectionListener(this);
		mdec.setCategoryList(category_list);
		
        Dimension d = new Dimension(category_list.getComponent(0).getPreferredSize());
        category_scroll_pane.getVerticalScrollBar().setUnitIncrement(d.height);
        category_scroll_pane.getViewport().setPreferredSize(d);
        category_scroll_pane.getViewport().add(category_list);
		
		add(spectrum_scroll_pane);
		add(category_scroll_pane);


	}
	
	
	/**
	 * Register a metadata change listener.
	 * 
	 * @param listener	the listener to be added
	 */
	public void addMetadataChangeListener(MD_ChangeListener listener) {
		
		listeners.add(listener);
		
	}
	
	
	private void buildGUI(boolean manual_interaction) throws SPECCHIOClientException
	{
		
		if(!manual_interaction && form != null)
		{

			MetaTaxonomy ap_domain = (MetaTaxonomy) form.getEavParameterFromContainer("Application Domain", "General");

			if(ap_domain != null)
			{
				// control of application domain has moved to MDE_Controller
			}
			else
			{
				// enable all categories
				if(category_list.isApplicationDomainEnabled())
				{
					category_list.setAllSelected(true);
					category_list.setApplicationDomain(null);
					mdec.set_form_descriptor(category_list.getFormDescriptor(), false);
				}
			}
		}
		
		
		form = mdec.getForm();	
		
		// set up metadata panel
		setForm(form);
		
	}	
	
	
	/**
	 * Fire the "metadata added" event.
	 * 
	 * @param field	the field that was added
	 */
	private void fireMetadataFieldAdded(MD_Field field) {
		
		// notify all listeners
		for (MD_ChangeListener listener : listeners) {
			listener.metadataFieldAdded(field);
		}
		
	}
	
	
	/**
	 * Fire the "metadata changed" event.
	 * 
	 * @param field	the field that changed
	 * @param value	the new value of the field
	 */
	private void fireMetadataFieldChanged(MD_Field field, Object value) {
		
		// notify all listeners
		for (MD_ChangeListener listener : listeners) {
			listener.metadataFieldChanged(field, value);
		}
		
	}
	
	
	/**
	 * Fire the "metadata removed" event.
	 * 
	 * @param field	the field that was removed
	 */
	private void fireMetadataFieldRemoved(MD_Field field) {
		
		// notify all listeners
		for (MD_ChangeListener listener : listeners) {
			listener.metadataFieldRemoved(field);
		}
		
	}
	
	/**
	 * Fire the "metadata annnotation changed" event.
	 * 
	 * @param field	the field that was changed
	 * @param annotation	the new annotation of the field
	 */	
	private void fireMetadataAnnotationChanged(MD_Field field,
			String annotation) {
		// notify all listeners
		for (MD_ChangeListener listener : listeners) {
			listener.metadataFieldAnnotationChanged(field, annotation);
		}
		
	}
	
	
	
	/**
	 * Get the metadata form associated with this panel.
	 * 
	 * @return a reference to the form
	 */
	public MDE_Form getForm() {
		
		return form;
		
	}
	
	
	/**
	 * Set whether or not the metadata in the panel can be edited.
	 * 
	 * @param editable	whether or not the fields in the panel should be editable
	 */
	public void setEditable(boolean editable) {
		
		// save the value of the flag for later
		this.editable = editable;
		
		// propagate the setting to every category container
		for (Component c : getComponents()) {
			if (c instanceof SpectrumMetadataCategoryContainer) {
				((SpectrumMetadataCategoryContainer)c).setEditable(editable);
			}
		}
		
	}
	
	
	/**
	 * Set the metadata form to be displayed by the panel.
	 * 
	 * @param form	the form
	 * 
	 * @throws SPECCHIOClientException	could not contact the server
	 */
	public void setForm(MDE_Form form) throws SPECCHIOClientException {
		
		// remove all of the existing components
		metadata_panel.removeAll();
		
		if (form != null) {
			
			// create and add panels for each category container
			for (MD_CategoryContainer mdcc : form.getContainers()) {
				SpectrumMetadataCategoryContainer panel = new SpectrumMetadataCategoryContainer(mdcc, this.specchioClient);
				panel.setEditable(editable);
				metadata_panel.add(panel);
			}
			
		}
		
		// save a reference to the new form
		this.form = form;
		
		// force re-draw
		revalidate();
		repaint();
		
	}
	
	
	public void updateForm()
	{
		form = mdec.getForm();
		setForm(form);
	}
	
	
	/**
	 * Category container panel.
	 */
	private class SpectrumMetadataCategoryContainer extends JPanel implements ActionListener, MouseListener {

		/** serialisation version identifier */
		private static final long serialVersionUID = 1L;
		
		/** metadata category container */
		private MD_CategoryContainer mdcc;
		
		/** inner panel */
		private JPanel fieldPanel;
		
		/** the client object */
		private SPECCHIOClient specchioClient;
				
		/** component factory */
		private SpectrumMetadataComponentFactory factory;
		
		/** popup menu */
		private ScrollablePopupMenu popupMenu;
		
		/** is the container editable? */
		private boolean editable;
		
		/** string for "add" menu item */
		private static final String ADD = "Add";
		
		/** string "attribute" client property */
		private static final String ATTRIBUTE = "Attribute";

		
		/**
		 * Constructor.
		 * 
		 * @param mdcc	the category container to be displayed in this panel
		 * @param specchioClient 
		 * 
		 * @throws SPECCHIOClientException	could not contact the server
		 */
		public SpectrumMetadataCategoryContainer(MD_CategoryContainer mdcc, SPECCHIOClient specchioClient) throws SPECCHIOClientException {
			
			super();
			
			// save a reference to the parameters
			this.mdcc = mdcc;
			
			this.specchioClient = specchioClient;
			
			// initialise member variables
			editable = true;
			
			// add a border with the category name
			Border blackline = BorderFactory.createLineBorder(Color.BLACK);
			TitledBorder tb = BorderFactory.createTitledBorder(blackline, mdcc.getCategoryName());
			setBorder(tb);
			
			// create a panel to hold the fields
			fieldPanel = new JPanel();
			fieldPanel.setLayout(new AlignedBoxLayout(fieldPanel, AlignedBoxLayout.Y_AXIS));
			add(fieldPanel);
			
			// add help string
			String toolTipText = "Click menu to add metadata field.";
			if(mdcc.getCategoryName().equals("Location"))
				toolTipText = toolTipText + " Drag and drop KML files to auto-generate geometries.";
				
			this.setToolTipText(toolTipText);
			
			// set up popup menu
			popupMenu = new ScrollablePopupMenu();
			popupMenu.setMaximumVisibleRows(10);
			for (attribute a : mdcc.getPossibleEAVFields()) {
				addMenuItem(a);
			}
			addMouseListener(this);
			
			// add fields
			factory = new SpectrumMetadataComponentFactory(this);
			for (MD_Field field : mdcc.getFields()) {
				addField(field);
			}
			
			
			
	        new FileDrop(this, new FileDrop.Listener()
	        {   public void filesDropped( java.io.File[] files )
	            {   for( int i = 0; i < files.length; i++ )
	                {   try
	                    {   
	                	
	                	File file = files[i];
	                	String ext = FilenameUtils.getExtension(file.getCanonicalFile().toString());
	                	
	                	if(ext.equals("kml") && SpectrumMetadataCategoryContainer.this.mdcc.getCategoryName().equals("Location"))
	                	{
	                		List<Coordinate> coords = null;
	                		ArrayList<Point2D> coords_2d = null;
	                		
	                		final Kml kml = Kml.unmarshal(file);
	                		Document document = (Document) kml.getFeature();
	                		List<Feature> f = document.getFeature();

	                		// get first entry
	                		Placemark p = (Placemark) f.get(0);
	                		Geometry g = p.getGeometry();
	                		MD_EAV_Field field = null;
	                		MetaSpatialGeometry mp = null;

	                		// geometry type dependent
	                		if (g instanceof Polygon)
	                		{
	                			coords = ((Polygon) g).getOuterBoundaryIs().getLinearRing().getCoordinates();
	                			
	                			// add new polygon parameter: MetaSpatialPolygon
	        					attribute a = SpectrumMetadataCategoryContainer.this.specchioClient.getAttributesNameHash().get("Spatial Extent");
	        					mp = (MetaSpatialGeometry) MetaParameter.newInstance(a);
	        					mp.setUnits(SpectrumMetadataCategoryContainer.this.specchioClient.getAttributeUnits(a));	        				
	                			
	                		}
	                		else if (g instanceof de.micromata.opengis.kml.v_2_2_0.Point)
	                		{
	                			
	                			coords = ((de.micromata.opengis.kml.v_2_2_0.Point) g).getCoordinates();
	                			
	                			// add new polygon parameter: MetaSpatialPoint
	        					attribute a = SpectrumMetadataCategoryContainer.this.specchioClient.getAttributesNameHash().get("Spatial Position");
	        					mp = (MetaSpatialGeometry) MetaParameter.newInstance(a);
	        					mp.setUnits(SpectrumMetadataCategoryContainer.this.specchioClient.getAttributeUnits(a));	        				
	                			
	                			
	                		}
	                		else if (g instanceof LineString)
	                		{
	                			
	                			coords = ((LineString) g).getCoordinates();
	                			
	                			// add new polygon parameter: MetaSpatialPolyline
	        					attribute a = SpectrumMetadataCategoryContainer.this.specchioClient.getAttributesNameHash().get("Spatial Transect");
	        					mp = (MetaSpatialGeometry) MetaParameter.newInstance(a);
	        					mp.setUnits(SpectrumMetadataCategoryContainer.this.specchioClient.getAttributeUnits(a));	        				
	                			
	                		}
	                		else
	                		{
	                			// unknown geometry type or geometry type does not fit the selected metaparameter
	                			String message = "KML Geometry type " + g.getClass().getSimpleName() + " does not fit any SPECCHIO spatial attribute ";
	                			JOptionPane.showMessageDialog(SpectrumMetadataCategoryContainer.this, message, "Info", JOptionPane.INFORMATION_MESSAGE, SPECCHIOApplication.specchio_icon);
	                		}
	                		
	                		if(mp != null)
	                		{
	                			coords_2d = new ArrayList<Point2D>();
	                			
	                			Iterator<Coordinate> it = coords.iterator();
	                			while(it.hasNext())
	                			{
	                				Coordinate coord = it.next();
	                				Point2D coord2d = new Point2D(coord.getLatitude(), coord.getLongitude());
	                				coords_2d.add(coord2d);
	                			}
	                			
	                			try {
									mp.setValue(coords_2d);
								} catch (MetaParameterFormatException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
	                			
	        					field = getForm().createEAVField(mp);	                				        					   
	                			SpectrumMetadataCategoryContainer.this.addFieldToPanel(field);
	
	                		}


	                	}
	                	

	                	
	                    }   // end try
	                    catch( java.io.IOException e ) {}
	                }   // end for: through each dropped file
	            }   // end filesDropped
	        }); // end FileDrop.Listener					
			
		}
		
		
		/**
		 * Menu selection handler.
		 *
		 * @param event	the event to be handled
		 */
		public void actionPerformed(ActionEvent event) {
			
			JMenuItem menuItem = (JMenuItem)event.getSource();
			
			if (ADD.equals(event.getActionCommand())) {
				
				try {
				
					// create a new field for this attribute
					attribute a = (attribute)menuItem.getClientProperty(ATTRIBUTE);
					MetaParameter mp = MetaParameter.newInstance(a);
					mp.setUnits(specchioClient.getAttributeUnits(a));
					
					if(mdec.getOnlyHierarchiesAreSelected())
					{
						mp.setLevel(MetaParameter.HIERARCHY_LEVEL);
					}
					
					MD_EAV_Field field = getForm().createEAVField(mp);
					
//					if(mdec.getOnlyHierarchiesAreSelected())
//					{
//						field.setLevel();
//						//field.getConflict().
//					}
					
//					Object value = null;
					if (mp instanceof MetaFile) {
						// need to load the value from a file
						mp = loadMetaFileValue((MetaFile)mp);
						if (mp != null && mp.getValue() != null) {
							// copy the value into the field
							field.getMetaParameter().setValue(mp.getValue());
							field.getMetaParameter().setAnnotation(mp.getAnnotation());
							fireMetadataAnnotationChanged(field, mp.getAnnotation());
//							value = mp.getValue();
						} else {
							// loading was aborted; prevent addition of the field
							field = null;
						}
						
					}
					
//					if (mp instanceof MetaBoolean) {
//						value = mp.getValue();
//					}
				
					addFieldToPanel(field);
		
				
				}
				catch (SPECCHIOClientException ex) {
					// error contacting the server
					ErrorDialog error = new ErrorDialog(owner, "Could not create field", ex.getUserMessage(), ex);
					error.setVisible(true);
				}
				catch (MetaParameterFormatException ex) {
					// should never happen
					ex.printStackTrace();
				}
				
			}
			
		}

		/**
		 * Add a field to the panel
		 * 
		 * @param field	the field
		 * 
		 * @return the component that represents the new field
		 * 
		 * @throws SPECCHIOClientException error contacting the server
		 */

		public void addFieldToPanel(MD_EAV_Field field)
		{
			if (field != null) {
				
				// add the new field to the category container
				mdcc.addField(field);
					
				// add the new field to the panel
				SpectrumMetadataComponent smc = null;
				smc = addField(field);
				smc.getInputComponent().requestFocusInWindow();
				
				// notify the metadata change listeners
				fireMetadataFieldAdded(field);
//				if (mp instanceof MetaFile || mp instanceof MetaBoolean) {
//					// adding a file changes the value as well
//					fireMetadataFieldChanged(field, value);
//				}
				
				// always fire a change event
				fireMetadataFieldChanged(field, field.getMetaParameter().getValue());
				
			}
		
			// force re-draw
			revalidate();
			repaint();						
		}
		
		
		/**
		 * Add a field to the panel and modify popup menu according to cardinality.
		 * 
		 * @param field	the field
		 * 
		 * @return the component that represents the new field
		 * 
		 * @throws SPECCHIOClientException error contacting the server
		 */
		private SpectrumMetadataComponent addField(MD_Field field) throws SPECCHIOClientException {
			
			// create a component for this field
			SpectrumMetadataComponent c = factory.newComponent(field);
			fieldPanel.add(c);
			
			if (field instanceof MD_EAV_Field) {
				// remove this field from the pop-up menu if its cardinality is 1
				// menu item count starts at 1 (first entry is the scrollbar)
				MetaParameter mp = ((MD_EAV_Field)field).getMetaParameter();
				for (int i = 0; i < popupMenu.getComponentCount()-1; i++) {
					JMenuItem menuItem = popupMenu.getJMenuItem(i);
					attribute a = (attribute)menuItem.getClientProperty(ATTRIBUTE);
					if (a.id == mp.getAttributeId() && a.cardinality == 1) {
						popupMenu.remove(i);
						break;
					}
				}
			}
			
			if(field.get_conflict_status() == 2)
			{
				c.setEnabled(false); // ambiguous field
			}
			
			return c;
			
		}
		
		
		/**
		 * Add an option to add an attribute to the pop-up menu.
		 * 
		 * @param a	the attribute
		 */
		private void addMenuItem(attribute a) {
			
			JMenuItem menuItem = new JMenuItem(ADD + " " + a.getName());
			menuItem.setActionCommand(ADD);
			menuItem.addActionListener(this);
			menuItem.putClientProperty(ATTRIBUTE,  a);
			popupMenu.add(menuItem);
			
		}
			
			
		
		
		/**
		 * Load the value of a file-type meta-parameter
		 * 
		 * @param mp	the meta-parameter to be loaded
		 * 
		 * @return the object read from the file, or null if the user cancelled or there was an error
		 */
		private MetaFile loadMetaFileValue(MetaFile mp)  {
			
			// create a file chooser
			JFileChooser fc;
			
			fc = new JFileChooser(previous_path);
			int returnVal = fc.showOpenDialog(this);
			
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				
				try {
					File file = fc.getSelectedFile();
					
					previous_path = file.getAbsolutePath();
					
					// check that the file will be accepted by the server
					Long maxObjectSize = Long.parseLong(specchioClient.getCapability(Capabilities.MAX_OBJECT_SIZE));
					if (file.length() > maxObjectSize) {
						String message =
								"Your file (" + file.length() + " bytes) exceeds " +
								"the maximum size permitted by the server (" + maxObjectSize + " bytes).";
						JOptionPane.showMessageDialog(owner, message, "File too big", JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon);
						return null;
					}
					
					// read the value from the file
					MimetypesFileTypeMap mimetypes = new MimetypesFileTypeMap();
					FileInputStream fis = new FileInputStream(file);
					mp.readValue(fis, mimetypes.getContentType(file));
					if(MetaImage.class == mp.getClass())
						mp.setAnnotation(previous_path);
					
					return mp;
				}
				catch (IOException ex) {
					// read error
					JOptionPane.showMessageDialog(owner, ex.getMessage(), "Could not read file", JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon);
					return null;
				}
				catch (SPECCHIOClientException ex) {
					// could not establish the maximum file size
					ErrorDialog error = new ErrorDialog(owner, "Could not get the maximum upload size", ex.getUserMessage(), ex);
					error.setVisible(true);
					return null;
				}
				
			} else {
				
				// the user cancelled the file chooser
				return null;
				
			}
			
		}
		
		
		/**
		 * Respond to a mouse click. Does nothing.
		 * 
		 * @param event	the event to be handled
		 */
		public void mouseClicked(MouseEvent event) {
			
			// do nothing
			
		}
		
		
		/**
		 * Respond to the mouse entering the panel. Does nothing.
		 * 
		 * @param event	the event to be handled
		 */
		public void mouseEntered(MouseEvent event) {
			
			// do nothing
			
		}
		
		
		/**
		 * Respond to the mouse exiting the panel. Does nothing.
		 * 
		 * @param event	the event to be handled
		 */
		public void mouseExited(MouseEvent event) {
			
			// do nothing
			
		}
		
		
		/**
		 * Respond to a mouse button press.
		 * 
		 * @param event	the event to be handled
		 */
		public void mousePressed(MouseEvent event) {
			
			if (editable && event.isPopupTrigger()) {
				popupMenu.show(this, event.getX(), event.getY());
			}
			
		}
		
		
		/**
		 * Respond to a mouse button release.
		 * 
		 * @param event	the event to be handled
		 */
		public void mouseReleased(MouseEvent event) {
			
			if (editable && event.isPopupTrigger()) {
				popupMenu.show(this, event.getX(), event.getY());
			}
			
		}
		
		
		/**
		 * Remove a field from the category.
		 * 
		 * @param field	the field to be removed
		 */
		public void removeField(MD_Field field) {
			
			// remove the field's GUI component
			for (int i = 0; i < fieldPanel.getComponentCount(); i++) {
				SpectrumMetadataComponent c = (SpectrumMetadataComponent)fieldPanel.getComponent(i);
				if (c.getField() == field) {
					fieldPanel.remove(i);
					break;
				}
			}
			
			if (field instanceof MD_EAV_Field) {
				// add an option to re-add the field to the pop-up menu
				MetaParameter mp = ((MD_EAV_Field)field).getMetaParameter();
				for (attribute a : mdcc.getPossibleEAVFields()) {
					if (a.id == mp.getAttributeId()  && a.cardinality == 1) {
						addMenuItem(a);
						break;
					}
				}
				
			}
			
			// notify the metadata change listeners
			fireMetadataFieldRemoved(field);
			
			// force re-draw
			revalidate();
			repaint();
			
		}
		
		
		/**
		 * Make the panel editable or not editable.
		 * 
		 * @param editable	true to make the panel editable, false otherwise
		 */
		public void setEditable(boolean editable) {
			
			// save the flag for later
			this.editable = editable;
			
			// propagate the setting to every field
			for (Component c : fieldPanel.getComponents()) {
				if (c instanceof SpectrumMetadataComponent) {
					((SpectrumMetadataComponent)c).setEditable(editable);
				}
			}
			
		}
		
	}
	
	
	/**
	 * Factory class for creating spectrum metadata components
	 */
	private class SpectrumMetadataComponentFactory {
		
		/** the category container panel */
		private SpectrumMetadataCategoryContainer container;
		
		
		/**
		 * Constructor.
		 * 
		 * @param container	the category container panel to which components will belong
		 */
		public SpectrumMetadataComponentFactory(SpectrumMetadataCategoryContainer container) {
			
			// save a reference to the parameiters for later
			this.container = container;
			
		}
		
		
		/**
		 * Create a new component for a spectrum field.
		 * 
		 * @param field	the metadata field to be represented by the new component
		 *
		 * @return a SpectrumMetadataComponent corresponding to the input field
		 * 
		 * @throws SPECCHIOClientException	could not contact the server
		 */
		public SpectrumMetadataComponent newComponent(MD_Field field) throws SPECCHIOClientException {
			
			if (field instanceof MD_Spectrum_Field) {
				return newFieldComponent((MD_Spectrum_Field)field);
			} else if (field instanceof MD_EAV_Field) {
				return newEavComponent((MD_EAV_Field)field);
			} else if (field instanceof MD_EAV_Link_Field) {
				return newEavComponent((MD_EAV_Link_Field)field);
			} else if (field instanceof MD_Hierarchy_Field) {
				return newFieldComponent((MD_Hierarchy_Field)field);
				
			} else {
				// this should never happen
				return null;
			}
			
		}
		
		
		/**
		 * Create a new component for an EAV field.
		 * 
		 * @param field	the metadata field to be represented by the new component
		 *
		 * @return a new SpectrumEavMetadataComponent corresponding to the new field
		 * 
		 * @throws SPECCHIOClientException	could not contact the server
		 */
		private SpectrumEavMetadataComponent newEavComponent(MD_EAV_Field field) throws SPECCHIOClientException {
			
			// the component type depends on the meta-parameter's type
			MetaParameter mp = field.getMetaParameter();
			if (mp instanceof MetaTaxonomy) {
				return new SpectrumTaxonomyEavMetadataComponent(container, field);
			} else if (mp instanceof MetaLink) {
				return new SpectrumLinkEavMetadataComponent(container, field);				
			} else if (mp instanceof MetaDate) {
				return new SpectrumDateEavMetadataComponent(container, field);
			} else if (mp instanceof MetaImage) {
				return new SpectrumImageEavMetadataComponent(container, field);
			} else if (mp instanceof MetaDocument) {
				return new SpectrumFileEavMetadataComponent(container, field);
			} else if (mp instanceof MetaDocument) {
				return new SpectrumFileEavMetadataComponent(container, field);
			} else if (mp instanceof MetaBoolean) {
				return new SpectrumBooleanEavMetadataComponent(container, field);
			} else if (mp instanceof MetaSpatialGeometry) {
				return new SpectrumSpatialGeometryEavMetadataComponent(container, field);
			}
			else {
				return new SpectrumSimpleEavMetadataComponent(container, field);
			}
			
			
		}
		
		
		/**
		 * Create a new component for a non-EAV field.
		 * 
		 * @param field	the metadata field to be represented by the new component
		 *
		 * @return a new SpectrumFieldMetadataComponent corresponding to the new field
		 */
		private SpectrumFieldMetadataComponent newFieldComponent(MD_Spectrum_Field field) {
			
			return new SpectrumFieldMetadataComponent(container, field);
			
		}
		
		/**
		 * Create a new component for a non-EAV field.
		 * 
		 * @param field	the metadata field to be represented by the new component
		 *
		 * @return a new SpectrumFieldMetadataComponent corresponding to the new field
		 */
		private HierarchyFieldMetadataComponent newFieldComponent(MD_Hierarchy_Field field) {
			
			return new HierarchyFieldMetadataComponent(container, field);
			
		}		
		
	}
	
	
	
	
	/**
	 * Base class for all spectrum metadata fields.
	 */
	private abstract class SpectrumMetadataComponent extends JPanel implements AlignedBoxLayout.AlignedBox {
		
		/** serialisation version identifier */
		private static final long serialVersionUID = 1L;

		/** the category container panel to which this component belongs */
		private SpectrumMetadataCategoryContainer container;
		
		/** the metadata field represented by this component */
		private MD_Field field;
		
		/** the label of this component */
		private JLabel label;
		
		/** the client object */
		protected SPECCHIOClient specchioClient;
		
		
		/**
		 * Constructor.
		 * 
		 * @param container	the category container panel to which this component belongs
		 * @param field		the metadata field represented by this component
		 */
		public SpectrumMetadataComponent(SpectrumMetadataCategoryContainer container, MD_Field field) {
			
			// save a reference to the parameters
			this.container = container;
			this.field = field;
			this.specchioClient = container.specchioClient;
						
			// create a label for this component
			label = new JLabel(field.getLabelWithUnit());
			label.setHorizontalAlignment(JLabel.RIGHT);
			label.setToolTipText(field.getDescription());
			
			try {
			
				if(field.getLevel() == MetaParameter.HIERARCHY_LEVEL && field.getClass() == MD_EAV_Field.class && field.getConflict().getConflictData(((MD_EAV_Field)field).getMetaParameter().getEavId()).isInherited())
				{				
					label.setForeground(Color.BLUE);
					label.setToolTipText("<html> " + ((field.getDescription() != null && field.getDescription().length() > 0) ? field.getDescription() + "." + "<br>": "")  + "This metaparameter is inherited from a hierarchy." + "</html>");
					
				}
				
				if(field.getLevel() == MetaParameter.HIERARCHY_LEVEL && field.getClass() == MD_EAV_Field.class && field.getConflict().getConflictData(((MD_EAV_Field)field).getMetaParameter().getEavId()).getNumberOfSharingRecords()>1  && !field.getConflict().getConflictData(((MD_EAV_Field)field).getMetaParameter().getEavId()).isInherited())
				{
					Color c = Color.getHSBColor(0.45f, 1f, 0.5f);
					label.setForeground(c);
					label.setToolTipText("<html> " + ((field.getDescription() != null && field.getDescription().length() > 0) ? field.getDescription() + "." + "<br>": "")  + "This metaparameter is shared by a total of " 
					+ field.getConflict().getConflictData(((MD_EAV_Field)field).getMetaParameter().getEavId()).getNumberOfSharingRecords() +" hierarchies ." + "</html>");
					
				}
				
				
				if(field.getLevel() == MetaParameter.SPECTRUM_LEVEL && field.getClass() == MD_EAV_Field.class && field.getConflict().getConflictData(((MD_EAV_Field)field).getMetaParameter().getEavId()).getNumberOfSharingRecords() > 1)
				{	
					Color c = Color.getHSBColor(0.45f, 1f, 0.5f);
					
					Color c2 = Color.BLUE;
					
					label.setForeground(c);
					label.setToolTipText("<html> " + (field.getDescription() != null ? field.getDescription() + "." + "<br>": "")  + "This metaparameter is shared with " + field.getConflict().getConflictData(((MD_EAV_Field)field).getMetaParameter().getEavId()).getNumberOfSharingRecords() + " other spectra." + "</html>");

				}
			
			} catch (java.lang.NullPointerException e)
			{
				int x = 0;
			}
			add(label);
			
		}
		
		
		/**
		 * Get the category container panel to which this component belongs
		 * 
		 * @return a reference to the category container panel
		 */
		public SpectrumMetadataCategoryContainer getCategoryContainerPanel() {
			
			return container;
			
		}
		
		
		/**
		 * Get the label of this panel.
		 * 
		 * @return the label
		 */
		public JLabel getLabel() {
			
			return label;
			
		}
		
		
		/**
		 * Get the field represented by this component.
		 * 
		 * @return a reference to the MD_Field object with which this component was contructed
		 */
		public MD_Field getField() {
			
			return field;
			
		}
		
		public abstract JComponent getInputComponent();
		
		
		/**
		 * Get the horizontal alignment position. 
		 * 
		 * @return the preferred width of the label
		 */
		public int getXAlignmentPosition() {
			
			Dimension dim = label.getPreferredSize();
			
			return dim.width;
			
		}
		
		/**
		 * Get the vertical alignment position. Not used.
		 * 
		 * @return 0
		 */
		public int getYAlignmentPosition() {
			
			return 0;
			
		}
		
		
		/**
		 * Make the field editable or not.
		 * 
		 * @param editable true to make the field editable, false otherwise
		 */
		public abstract void setEditable(boolean editable);
		
		
		/**
		 * Enable or disable the field.
		 *
		 * @param enabled	true or false
		 */
		public abstract void setEnabled(boolean enabled);
		
	}
	
	
	/**
	 * Component for non-EAV metadata fields.
	 */
	private class SpectrumFieldMetadataComponent extends SpectrumMetadataComponent implements ActionListener {
		
		/** serialisation version identifier */
		private static final long serialVersionUID = 1L;
		
		/** the combo box, used for an editable field */
		private JComboBox<combo_table_data> box;
		
		/** the text field, used for a non-editable field */
		private JTextField text;
		
		/**
		 * Constructor.
		 * 
		 * @param container	the category container panel to which this component belongs
		 * @param field	the field to be represented by this component
		 */
		public SpectrumFieldMetadataComponent(SpectrumMetadataCategoryContainer container, MD_Spectrum_Field field) {
			
			super(container, field);
			
			// build combobox
			box = new JComboBox<combo_table_data>();
			combo_table_data nil_item = new combo_table_data("NIL", 0);
			box.addItem(nil_item);
			
			// fill the box with items
			CategoryTable items = field.getCategoryValues();
			Enumeration<Integer> e = items.keys();
			while(e.hasMoreElements())
			{
				Integer key = e.nextElement();					
				String value = items.get(key);
				combo_table_data cdt = new combo_table_data(value, key);
				box.addItem(cdt);
				//System.out.println(key);
				if(field.getId() != null && key.intValue() == field.getId().intValue())
				{
					box.setSelectedItem(cdt);
				}
			}
			
			if(field.get_conflict_status() != 1)
			{
				box.setSelectedItem(nil_item); // NIL field
			}
			
			// add the box to the panel
			add(box);
			box.addActionListener(this);
			
			// create the label but don't display it yet
			text = new JTextField(box.getSelectedItem().toString(), 20);
			text.setEditable(false);
			
		}
		
		
		/**
		 * Combo box selection handler.
		 * 
		 * @param	the event
		 */
		public void actionPerformed(ActionEvent event) {
			
			combo_table_data cdt = (combo_table_data) box.getSelectedItem();
			fireMetadataFieldChanged(getField(), cdt);	
			getField().setNewValue(cdt.id);
			
		}
		
		
		/**
		 * Make the field editable or not.
		 * 
		 * @param editable true to make the field editable, false otherwise
		 */
		public void setEditable(boolean editable) {
			
			if (editable) {
				// display the combo box
				remove(text);
				add(box);
			} else {
				// display the label
				remove(box);
				add(text);
			}
			
			// force re-draw
			revalidate();
			repaint();
			
		}
		
		
		/**
		 * Enable or disable the field.
		 *
		 * @param enabled	true or false
		 */
		public void setEnabled(boolean enabled) {
			
			box.setEnabled(enabled);
			text.setEnabled(enabled);
			
		}


		@Override
		public JComponent getInputComponent() {			
			return this.text;
		}
		
	}
	
	
	/**
	 * Component for non-EAV metadata fields.
	 */
	private class HierarchyFieldMetadataComponent extends SpectrumMetadataComponent implements ActionListener {
		
		/** serialisation version identifier */
		private static final long serialVersionUID = 1L;

		
		/** the text field, used for a non-editable field */
		private JTextField text;
		
		/**
		 * Constructor.
		 * 
		 * @param container	the category container panel to which this component belongs
		 * @param field	the field to be represented by this component
		 */
		public HierarchyFieldMetadataComponent(SpectrumMetadataCategoryContainer container, MD_Hierarchy_Field field) {
			
			super(container, field);
			
			
			text = new JTextField(field.getText(), 20);
			text.setEditable(false);
			add(text);
			
			
		}
		
		
		/**
		 * Combo box selection handler.
		 * 
		 * @param	the event
		 */
		public void actionPerformed(ActionEvent event) {
			
//			combo_table_data cdt = (combo_table_data) box.getSelectedItem();
//			fireMetadataFieldChanged(getField(), cdt);	
//			getField().setNewValue(cdt.id);
			
		}
		
		
		/**
		 * Make the field editable or not.
		 * 
		 * @param editable true to make the field editable, false otherwise
		 */
		public void setEditable(boolean editable) {
			
//			if (editable) {
//				// display the combo box
//				remove(text);
//				add(box);
//			} else {
//				// display the label
//				remove(box);
//				add(text);
//			}
			
			// force re-draw
			revalidate();
			repaint();
			
		}
		
		
		/**
		 * Enable or disable the field.
		 *
		 * @param enabled	true or false
		 */
		public void setEnabled(boolean enabled) {
			
			text.setEnabled(enabled);
			
		}


		@Override
		public JComponent getInputComponent() {			
			return this.text;
		}
		
	}	
	
	
	/**
	 * Base class for EAV metadata components.
	 */
	private abstract class SpectrumEavMetadataComponent extends SpectrumMetadataComponent implements ActionListener, MouseListener, ClipboardOwner {
		
		/** serialisation version identifier */
		private static final long serialVersionUID = 1L;

		/** pop-up menu for deleting the field */
		protected ScrollablePopupMenu popupMenu;
		
		/** is the component editable? */
		private boolean editable;
		
		/** text for the "delete" and copy menu item */
		private static final String DELETE = "Delete";
		private static final String COPY_ATTRIBUTE_NAME = "Copy Attribute Name";
		
		
		/**
		 * Constructor.
		 * 
		 * @param container	the category container panel to which this component belongs
		 * @param field		the metadata field represented by this component
		 */
		public SpectrumEavMetadataComponent(SpectrumMetadataCategoryContainer container, MD_EAV_Field field) {
			
			super(container, field);
			
			// initialise member variables
			editable = true;
			
			// create the popup menu and listen for mouse clicks
			popupMenu = new ScrollablePopupMenu();
			popupMenu.setMaximumVisibleRows(10);
			getLabel().addMouseListener(this);
			
			// add the "delete" menu option
			JMenuItem menuItem = new JMenuItem(DELETE);
			menuItem.addActionListener(this);
			popupMenu.add(menuItem);		
			
			menuItem = new JMenuItem(COPY_ATTRIBUTE_NAME);
			menuItem.addActionListener(this);
			popupMenu.add(menuItem);				
			
		}
		
		
		/**
		 * Menu selection handler.
		 *
		 * @param event	the event to be handled
		 */
		public void actionPerformed(ActionEvent event) {
			
			if (DELETE.equals(event.getActionCommand())) {
				
				// remove the component from its container
				getCategoryContainerPanel().removeField(getField());
				
			}
			
			if(COPY_ATTRIBUTE_NAME.equals(event.getActionCommand())) {
								
				StringSelection stringSelection = new StringSelection(((MD_EAV_Field)getField()).getMetaParameter().getAttributeName());
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			    clipboard.setContents(stringSelection , this);			
			}
			
		}
		
		
		/**
		 * Get the metaparameter corresponding to this component.
		 * 
		 * @return a reference to the field's MetaParameter object
		 */
		public MetaParameter getMetaParameter() {
			
			return ((MD_EAV_Field)getField()).getMetaParameter();
			
		}
		
		
		/**
		 * Test whether or not the associated field has multiple values.
		 * 
		 * @return true if the field's conflict status is 1 or 3
		 */
		public boolean fieldHasMultipleValues() {
			
			return getField().get_conflict_status() != 1 && getField().get_conflict_status() != 3;
			
		}
		
		public void lostOwnership(Clipboard arg0, Transferable arg1) {
			// ignore
			
		}		
		
		/**
		 * Respond to a mouse click. Does nothing.
		 * 
		 * @param event	the event to be handled
		 */
		public void mouseClicked(MouseEvent event) {
			
			// do nothing
			
		}
		
		
		/**
		 * Respond to the mouse entering the panel. Does nothing.
		 * 
		 * @param event	the event to be handled
		 */
		public void mouseEntered(MouseEvent event) {
			
			// do nothing
			
		}
		
		
		/**
		 * Respond to the mouse exiting the panel. Does nothing.
		 * 
		 * @param event	the event to be handled
		 */
		public void mouseExited(MouseEvent event) {
			
			// do nothing
			
		}
		
		
		/**
		 * Respond to a mouse button press.
		 * 
		 * @param event	the event to be handled
		 */
		public void mousePressed(MouseEvent event) {
			
			if (editable && event.isPopupTrigger()) {
				popupMenu.show(this, event.getX(), event.getY());
			}
			
		}
		
		
		/**
		 * Respod to a mouse button release.
		 * @param event	the event to be handled
		 */
		public void mouseReleased(MouseEvent event) {
			
			if (editable && event.isPopupTrigger()) {
				popupMenu.show(this, event.getX(), event.getY());
			}
			
		}
		
		
		/**
		 * Make the component editable or not.
		 * 
		 * @param editable	true if the component can be edited
		 */
		public void setEditable(boolean editable) {
			
			this.editable = editable;
			
		}
		
		
		/**
		 * Enable or disable the component.
		 * 
		 * @param enabled	true if the component is enabled
		 */
		public void setEnabled(boolean enabled) {
			
			// disable pop-up menus if the component is disabled
			if (!enabled) {
				this.editable = false;
			}
			
		}
	
	}


	/**
	 * Component for manipulating date-type metadata field.
	 */
	private class SpectrumDateEavMetadataComponent extends SpectrumEavMetadataComponent implements DateListener {
		
		/** serialisation version identifier */
		private static final long serialVersionUID = 1L;
		
		/** calendar control, used for editable components */
		JCalendarCombo cal_combo;
		
		/** text field, user for non-editable components */
		JTextField text;
			
		/**
		 * Constructor.
		 *
		 * @param container	the category container panel to which this component belongs
		 * @param field	the metadata field represented by this component
		 */
		public SpectrumDateEavMetadataComponent(SpectrumMetadataCategoryContainer container, MD_EAV_Field field) {
				
			super(container, field);
			
			// set up date and format
			Date date = ((MetaDate)field.getMetaParameter()).valueAsDate();
			
			// build the calendar control
			// Interestingly, when debugging this, the cal_combo shows times corrected from UTC to local time, but this disappears after compiling it and installing as regular java app ....
			cal_combo = new JCalendarCombo(JCalendar.DISPLAY_DATE | JCalendar.DISPLAY_TIME,  false);			
			cal_combo.setDateFormat(MetaDate.getDateFormat());
			cal_combo.setDate(date);
			cal_combo.addDateListener(this);
			
			// build the text field
			text = new JTextField(((MetaDate) field.getMetaParameter()).valueAsString(), 20);
//			String date_text = ((MetaDate) field.getMetaParameter()).valueAsString();
			text.setEditable(false);
			
			// start with the combo box on display
			add(cal_combo);
			
		}
				
		
		/**
		 * Date changed handler.
		 * 
		 * @param the event to be handled
		 */
		public void dateChanged(DateEvent event) {	
			
			java.util.Date date = cal_combo.getDate();
			fireMetadataFieldChanged(getField(), date);	
			
		}
		
		
		/**
		 * Make the field editable or not.
		 * 
		 * @param editable true to make the field editable, false otherwise
		 */
		public void setEditable(boolean editable) {
			
			super.setEditable(editable);
			if (editable) {
				// display the calendar combo
				remove(text);
				add(cal_combo);
			} else {
				// display the text field
				remove(cal_combo);
				add(text);
			}
			
			// force re-draw
			revalidate();
			repaint();
			
		}
		
		
		/**
		 * Enable or disable the field.
		 *
		 * @param enabled	true or false
		 */
		public void setEnabled(boolean enabled) {

			super.setEnabled(enabled);
			cal_combo.setEnabled(enabled && !fieldHasMultipleValues());
			text.setEnabled(enabled && !fieldHasMultipleValues());
			
		}


		@Override
		public JComponent getInputComponent() {
			return text;
		}
		
	}
	

	
	/**
	 * Component for file-type metadata.
	 */
	private class SpectrumFileEavMetadataComponent extends SpectrumEavMetadataComponent implements ActionListener {
		
		/** serialisation version identifier */
		private static final long serialVersionUID = 1L;
		
		/** the "view" button */
		private JButton viewButton;
		
		/** text for the "view" button */
		private static final String VIEW = "View";
		
		
		/**
		 * Constructor.
		 * 
		 * @param container	the category container panel to which this component belongs
		 * @param field	the metadata field represented by this component
		 */
		public SpectrumFileEavMetadataComponent(SpectrumMetadataCategoryContainer container, MD_EAV_Field field) {
			
			super(container, field);
			
			if(!fieldHasMultipleValues())
			{
				viewButton = new JButton(VIEW);
				viewButton.setActionCommand(VIEW);
				viewButton.addActionListener(this);
			}
			else
			{
				viewButton = new JButton("-- multiple documents --");
				viewButton.setEnabled(false);
			}
			
			add(viewButton);
			
		}
		
		
		/**
		 * Button handler.
		 * 
		 * @param event	the event to be handled
		 */
		public void actionPerformed(ActionEvent event) {
			
			if (VIEW.equals(event.getActionCommand())) {
				MetaFile mp_file = (MetaFile)getMetaParameter();

				
				
				DocDisplayThread t = new DocDisplayThread(mp_file);
				t.start();


			} else {

				// pass pop-up menu actions to the super-class
				super.actionPerformed(event);
				
			}
		}
		
		// according to: https://stackoverflow.com/questions/15583900/unable-to-update-jdialog-gui-inside-a-thread
		public class DocDisplayThread implements Runnable {
		    
			private MetaFile mp;
			private ProgressReportDialog pr;
		    

		    public DocDisplayThread(MetaFile mp)
			{
				// save parameters for later
				this.mp = mp;
			

				JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(SpectrumFileEavMetadataComponent.this);
				pr = new ProgressReportDialog(topFrame, "Loading binary document", false, 20);
		    	
		    	
		        //new Thread(this).start();
		    }

		    public void start() {
		    	
		    	new Thread(this).start();
		    	
		    }
		    
		    @Override
		    public void run() {
		    	if(mp.getBlob_lazy_loading())
		    	{
		    		// inform user about data loading and size of data ...


		    		pr.set_operation("Loading " + mp.getBlob_size_in_MB() + " MB ...");
		    		pr.set_progress(0);
		    		pr.set_indeterminate(true);

		    		pr.setVisible(true);
		    		pr.validate();
		    		pr.repaint();

		    		mp=(MetaFile) specchioClient.loadMetaparameter(mp.getEavId());

		    		pr.setVisible(false);

		    	}

		    	try {

		    		// write the meta-parameter value to a temporary file
		    		File temp = File.createTempFile("specchio", mp.getDefaultFilenameExtension());
		    		temp.deleteOnExit();
		    		FileOutputStream fos = new FileOutputStream(temp);
		    		mp.writeValue(fos);
		    		fos.close();

		    		// launch the external viewer
		    		SPECCHIOApplication.openInDesktop(temp);

		    	}
		    	catch (IllegalArgumentException ex) {
		    		// something wrong with the temporary file
		    		ErrorDialog error = new ErrorDialog(owner, "Could not start viewer", ex.getMessage(), ex);
		    		error.setVisible(true);
		    	}
		    	catch (IOException ex) {
		    		// no viewer found for this file type
		    		ErrorDialog error = new ErrorDialog(owner, "Could not start viewer", ex.getMessage(), ex);
		    		error.setVisible(true);
		    	} 


		    }
		}
			

		

		
		
		/**
		 * Make the field editable or not.
		 * 
		 * @param editable true to make the field editable, false otherwise
		 */
		public void setEditable(boolean editable) {
			
			super.setEditable(editable);
			
		}
		
		
		/**
		 * Enable or disable the field.
		 *
		 * @param enabled	true or false
		 */
		public void setEnabled(boolean enabled) {

			super.setEnabled(enabled);
			viewButton.setEnabled(enabled && !fieldHasMultipleValues());
			
		}


		@Override
		public JComponent getInputComponent() {
			return viewButton;
		}
		
	}
	
	
	
	/**
	 * Component for image-type metadata.
	 */
	private class SpectrumImageEavMetadataComponent extends SpectrumEavMetadataComponent  implements DocumentListener {
		
		/** serialisation version identifier */
		private static final long serialVersionUID = 1L;
		
		/** the image panel */
		private ImagePanel ip;
		
		/** the label for multiple images */
		private JLabel label;

		private JTextField text;
		
		
		/**
		 * Constructor.
		 * 
		 * @param container	the category container panel to which this component belongs
		 * @param field	the metadata field represented by this component
		 */
		public SpectrumImageEavMetadataComponent(SpectrumMetadataCategoryContainer container, MD_EAV_Field field) {
			
			super(container, field);
			
			// initialise member variables
			ip = null;
			label = null;
			
			if(!fieldHasMultipleValues()) {
				
				try {
					ip = new ImagePanel();
					ip.image = (SerialisableBufferedImage)field.getMetaParameter().getValue();
					ip.mp = (MetaImage) field.getMetaParameter();
					BufferedImage image = ip.image.getImage();
					if (image != null) {
						int im_width = ip.image.getImage().getWidth();
						int im_height = ip.image.getImage().getHeight();
						int width;
						int heigth;
						float factor = im_height / (im_width*1.0f);
						
						if(im_width > im_height)
						{
							width = 300;
							heigth = (int) (width*factor);
						}
						else
						{
							width = 200;
							heigth = (int) (width*factor);
							
						}
												
						
						ip.setSize(width, heigth);
						ip.setPreferredSize(new Dimension(width , heigth));
						
						add(ip);
						
						if(field.getMetaParameter().getAnnotation() != null)
						{
							text = new JTextField(20);
							text.setText(field.getMetaParameter().getAnnotation());
							add(text);
							
							// set up event handler
							text.getDocument().addDocumentListener(this);
							
						}
						
						
						

						
					} else {
						label = new JLabel("-- image data not available --");
						add(label);
					}
				}
				catch (ClassNotFoundException ex) {
					// no image decoder found; replace the image with an error label
					label = new JLabel("-- no image decoder found --");
					add(label);
				}
				catch (IOException ex) {
					// error decoding the image; replace it with an error label
					label = new JLabel("-- error decoding image --");
					add(label);
					
				}
				
			} else {
				
				label = new JLabel("-- multiple pictures --");
				add(label);
				
			}
			
		}
		
		/**
		 * Convert the contents of the input field into the appropriate Java type.
		 * 
		 * @return an Object containing the parsed value, or null if it is not legal input
		 */
		private Object getValidatedInput() {
			
			Object oldValue = getMetaParameter().getValue();
			Object newValue = null;
			String input = text.getText();
			if (oldValue instanceof Integer) {
				try {
					newValue = Integer.parseInt(input);
				}
				catch (NumberFormatException ex) {
					// nothing to do because newValue is already null
				}
			} else if (oldValue instanceof Double) {
				try {
					newValue = Double.parseDouble(input);
				}
				catch (NumberFormatException ex) {
					// nothing to do because newValue is already null
				}
			} else {
				// should be a string
				newValue = input;
			}
			
			return newValue;
			
		}		
		
		
		/**
		 * Make the field editable or not.
		 * 
		 * @param editable true to make the field editable, false otherwise
		 */
		public void setEditable(boolean editable) {
			
			super.setEditable(editable);
			
		}
		
		
		/**
		 * Enable or disable the field.
		 *
		 * @param enabled	true or false
		 */
		public void setEnabled(boolean enabled) {

			super.setEnabled(enabled);
			if (ip != null) {
				ip.setEnabled(enabled);
			}
			
		}
		
		/**
		 * Handle updates to the text component.
		 * 
		 * @param event	the event to handle
		 */
		public void changedUpdate(DocumentEvent event) {
			
			String newValue = (String) getValidatedInput();
			if (newValue != null) {				
				fireMetadataAnnotationChanged(getField(), newValue);
			}	
		}
		

		/**
		 * Handle insertions in the text component.
		 * 
		 * @param event	the event to handle
		 */
		public void insertUpdate(DocumentEvent event) {
			
			String newValue = (String) getValidatedInput();
			if (newValue != null) {				
				fireMetadataAnnotationChanged(getField(), newValue);
			}				
		}


		/**
		 * Handle deletions from the text component.
		 * 
		 * @param event	the event to handle
		 */
		public void removeUpdate(DocumentEvent event) {
			
			String newValue = (String) getValidatedInput();
			if (newValue != null) {				
				fireMetadataAnnotationChanged(getField(), newValue);
			}	
			
		}

		@Override
		public JComponent getInputComponent() {
			return text;
		}		
		

		
			
		
	}
	
	
    static class ImageTransferable implements Transferable
    {
        private Image image;

        public ImageTransferable (Image image)
        {
            this.image = image;
        }

        public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException
        {
            if (isDataFlavorSupported(flavor))
            {
                return image;
            }
            else
            {
                throw new UnsupportedFlavorException(flavor);
            }
        }

        public boolean isDataFlavorSupported (DataFlavor flavor)
        {
            return flavor == DataFlavor.imageFlavor;
        }

        public DataFlavor[] getTransferDataFlavors ()
        {
            return new DataFlavor[] { DataFlavor.imageFlavor };
        }
    }
	
	
	
	/**
	 * Component for simple metadata fields.
	 */
	private class SpectrumSimpleEavMetadataComponent extends SpectrumEavMetadataComponent implements DocumentListener {
		
		/** serialisation version identifier */
		private static final long serialVersionUID = 1L;
		
		/** the text field */
		private JTextComponent text;
		
		
		/**
		 * Constructor.
		 * 
		 * @param container	the category container panel to which this component belongs
		 * @param field	the metadata field represented by this component
		 */
		public SpectrumSimpleEavMetadataComponent(SpectrumMetadataCategoryContainer container, MD_EAV_Field field) {
			
			super(container, field);
			
			// create a text component of type appropriate to the value
			Object value = getMetaParameter().getValue();
			if (value instanceof Integer || value instanceof Double) {
				
				// value is a number; use a text field
				text = new KeylistenerTextFieldNumeric(20);
				((KeylistenerTextFieldNumeric)text).setAllowFloats(value instanceof Double);
				
			} else if (value instanceof String) {
				
				// value is a string; use a text area
				text = new JTextArea(1, 30);
				((JTextArea) text).setLineWrap(true);
				
			} else {
				
				// don't know what to do with this; let's just use a text field
				text = new JTextField(20);
				
			}
			
			// initialise the text component's contents
			if(!fieldHasMultipleValues())
			{
				text.setText((value != null)? value.toString() : null);
			}
			else
			{
				if(field.getMetaParameter().getDefaultStorageField().equals("int_val"))
				{
					text.setText("Range: " + field.getConflict().int_val_min + " to " + field.getConflict().int_val_max);
				}
				else if(field.getMetaParameter().getDefaultStorageField().equals("double_val"))
				{
					DecimalFormat df = new DecimalFormat("#.##########");
					df.setRoundingMode(RoundingMode.CEILING);
					
					// re-create text field to ensure text field is wide enough (dynamaic adjustment seems to fail ...)
					text = new JTextField("Range: " + df.format(field.getConflict().double_val_min) + " to " + df.format(field.getConflict().double_val_max));
					
				}
				
				else
					text.setText("-- multiple values --");
			}
			
			// set up event handler
			text.getDocument().addDocumentListener(this);
			
			// add the text component to the panel
			add(text);
			
		}


		/**
		 * Handle updates to the text component.
		 * 
		 * @param event	the event to handle
		 */
		public void changedUpdate(DocumentEvent event) {
			
			Object newValue = getValidatedInput();
			if (newValue != null) {
				fireMetadataFieldChanged(getField(), newValue);
			}
			
		}
		
		
		/**
		 * Convert the contents of the input field into the appropriate Java type.
		 * 
		 * @return an Object containing the parsed value, or null if it is not legal input
		 */
		private Object getValidatedInput() {
			
			Object oldValue = getMetaParameter().getValue();
			Object newValue = null;
			String input = text.getText();
			if (oldValue instanceof Integer) {
				try {
					newValue = Integer.parseInt(input);
				}
				catch (NumberFormatException ex) {
					// nothing to do because newValue is already null
				}
			} else if (oldValue instanceof Long) {
				try {
					newValue = Long.parseLong(input);
				}
				catch (NumberFormatException ex) {
					// nothing to do because newValue is already null
				}
			} else if (oldValue instanceof Double) {
				try {
					newValue = Double.parseDouble(input);
				}
				catch (NumberFormatException ex) {
					// nothing to do because newValue is already null
				}				
			} else {
				// should be a string
				newValue = input;
			}
			
			return newValue;
			
		}


		/**
		 * Handle insertions in the text component.
		 * 
		 * @param event	the event to handle
		 */
		public void insertUpdate(DocumentEvent event) {
			
			Object newValue = getValidatedInput();
			if (newValue != null) {
				fireMetadataFieldChanged(getField(), newValue);
			}
			
		}


		/**
		 * Handle deletions from the text component.
		 * 
		 * @param event	the event to handle
		 */
		public void removeUpdate(DocumentEvent event) {
			
			Object newValue = getValidatedInput();
			if (newValue != null) {
				fireMetadataFieldChanged(getField(), newValue);
			}
			
		}
		
		
		/**
		 * Make the field editable or not.
		 * 
		 * @param editable true to make the field editable, false otherwise
		 */
		public void setEditable(boolean editable) {
			
			super.setEditable(editable);
			text.setEditable(editable);
			
		}
		
		
		/**
		 * Enable or disable the field.
		 *
		 * @param enabled	true or false
		 */
		public void setEnabled(boolean enabled) {

			super.setEnabled(enabled);
			text.setEnabled(enabled && !fieldHasMultipleValues());
			
		}


		@Override
		public JComponent getInputComponent() {
			return text;
		}
		
	}
	
	
	/**
	 * Component for manipulating taxonomies.
	 */
	private class SpectrumTaxonomyEavMetadataComponent extends SpectrumEavMetadataComponent implements ActionListener {
		
		/** serialisation version identifier */
		private static final long serialVersionUID = 1L;
		
		/** the "select" button */
		private JButton selectButton;
		
		/** the text field for non-editable display */
		private JTextField text;
		
		/** action command for the "select" button */
		private static final String SELECT = "Select";
		
		
		/**
		 * Constructor.
		 * 
		 * @param container	the category container panel to which this component belongs
		 * @param field	the metadata field represented by this component
		 * 
		 * @throws SPECCHIOClientException	could not contact the server
		 */
		public SpectrumTaxonomyEavMetadataComponent(SpectrumMetadataCategoryContainer container, MD_EAV_Field field) throws SPECCHIOClientException {
			
			super(container, field);

			String displayString = "NIL";
			if(!fieldHasMultipleValues()) {
				
				// get the taxonomy name
				MetaTaxonomy mp = (MetaTaxonomy)field.getMetaParameter();
				Integer id = ((Long) mp.getValue()).intValue();
				if(id != 0)
				{
					TaxonomyNodeObject taxonomy = specchioClient.getTaxonomyNode(id);
					displayString = taxonomy.getName();
				}
				selectButton = new JButton(displayString);
			
			}
			else
			{
				displayString = "-- multiple taxa --";
				selectButton = new JButton(displayString);
				selectButton.setEnabled(false);
			}
												
			// create a button for selecting the taxonomy
			selectButton.setActionCommand(SELECT);
			selectButton.addActionListener(this);
			add(selectButton);
			
			// create a text field but don't display it yet
			text = new JTextField(displayString, 30);
			text.setEditable(false);
			
			
		}
		
		
		/**
		 * Button handler.
		 * 
		 * @param	the event
		 */
		public void actionPerformed(ActionEvent event) {
			
			if (SELECT.equals(event.getActionCommand())) {
			
				try {
					
					// get the meta-paramater to be edited
					MetaTaxonomy mp = (MetaTaxonomy)getMetaParameter();
								
					// show the taxonomy selection dialog
					TaxonomySelectionDialog d = new TaxonomySelectionDialog(owner, specchioClient, mp);
					d.setLocation(selectButton.getLocationOnScreen());
					d.setVisible(true);

					int tax_id = d.getSelectedTaxonomyId();
					if (tax_id > 0) {
						Long taxonomy_id = (long) tax_id;
					
						// update value of taxonomy button
						TaxonomyNodeObject taxonomy;
						taxonomy = specchioClient.getTaxonomyNode(tax_id);
						selectButton.setText(taxonomy.getName());
						selectButton.setToolTipText(taxonomy.getDescription());
						
						// notify listeners of the change
						fireMetadataFieldChanged(getField(), taxonomy_id);
					}
					
				} catch (SPECCHIOClientException ex) {
					ErrorDialog error = new ErrorDialog(owner, "Could not retrieve taxonomy", ex.getUserMessage(), ex);
					error.setVisible(true);
				}
				
			} else {
				
				// pass pop-up menu actions to the super class
				super.actionPerformed(event);
				
			}
			
		}
		
		
		/**
		 * Make the field editable or not.
		 * 
		 * @param editable true to make the field editable, false otherwise
		 */
		public void setEditable(boolean editable) {
			
			if (editable) {
				// display the combo box
				remove(text);
				add(selectButton);
			} else {
				// display the label
				remove(selectButton);
				add(text);
			}
			
			// force re-draw
			revalidate();
			repaint();
			
		}
		
		
		/**
		 * Enable or disable the field.
		 *
		 * @param enabled	true or false
		 */
		public void setEnabled(boolean enabled) {
			
			super.setEnabled(enabled);
			selectButton.setEnabled(enabled && !fieldHasMultipleValues());
			
		}


		@Override
		public JComponent getInputComponent() {
			return text;
		}
		
	}
	
	
	/**
	 * Component for manipulating booleans.
	 */
	private class SpectrumBooleanEavMetadataComponent extends SpectrumEavMetadataComponent implements ActionListener {
		
		/** serialisation version identifier */
		private static final long serialVersionUID = 1L;
		
		/** the "checkbox"  */
		private JCheckBox check;;
		
		/** the text field for non-editable display */
		private JTextField text;
		
		/** action command for the "select" button */
		private static final String SELECT = "Select";
		
		
		/**
		 * Constructor.
		 * 
		 * @param container	the category container panel to which this component belongs
		 * @param field	the metadata field represented by this component
		 * 
		 * @throws SPECCHIOClientException	could not contact the server
		 */
		public SpectrumBooleanEavMetadataComponent(SpectrumMetadataCategoryContainer container, MD_EAV_Field field) throws SPECCHIOClientException {
			
			super(container, field);

			String displayString = "NIL";
			if(!fieldHasMultipleValues()) {				
				
				check = new JCheckBox();
			
				check.setSelected((Boolean) field.getMetaParameter().getValue());
				displayString = field.getMetaParameter().getValue().toString();
			}
			else
			{
				displayString = "-- multiple values --";
				check = new JCheckBox(displayString, false);
				check.setEnabled(false);
			}
												
			// create a button for selecting the taxonomy
			check.setActionCommand(SELECT);
			check.addActionListener(this);
			add(check);
			
			// create a text field but don't display it yet
			text = new JTextField(displayString, 30);
			text.setEditable(false);
			
			
		}
		
		
		/**
		 * Button handler.
		 * 
		 * @param	the event
		 */
		public void actionPerformed(ActionEvent event) {
			
			if (SELECT.equals(event.getActionCommand())) {
			
				// notify listeners of the change
				fireMetadataFieldChanged(getField(), this.check.isSelected());

				
			} else {
				
				// pass pop-up menu actions to the super class
				super.actionPerformed(event);
				
			}
			
		}
		
		
		/**
		 * Make the field editable or not.
		 * 
		 * @param editable true to make the field editable, false otherwise
		 */
		public void setEditable(boolean editable) {
			
			if (editable) {
				// display the combo box
				//remove(text);
				add(check);
			} else {
				// display the label
				remove(check);
				add(text);
			}
			
			// force re-draw
			revalidate();
			repaint();
			
		}
		
		
		/**
		 * Enable or disable the field.
		 *
		 * @param enabled	true or false
		 */
		public void setEnabled(boolean enabled) {
			
			super.setEnabled(enabled);
			check.setEnabled(enabled && !fieldHasMultipleValues());
			
		}


		@Override
		public JComponent getInputComponent() {
			return check;
		}
		
	}
	
	
	/**
	 * Component for manipulating spatial geometries.
	 */
	private class SpectrumSpatialGeometryEavMetadataComponent extends SpectrumEavMetadataComponent implements ActionListener, TableModelListener {
		
		/** serialisation version identifier */
		private static final long serialVersionUID = 1L;
		
		/** table for lat and lon  */
		private JTable table;
		private EditableTableModel table_model;
		
		/** the text field for non-editable display */
		private JTextField text;

		private JScrollPane scrollPane;
		
		/** action command for the "select" button */
		private static final String SELECT = "Select";
		
		
		
		
		
		/**
		 * Constructor.
		 * 
		 * @param container	the category container panel to which this component belongs
		 * @param field	the metadata field represented by this component
		 * 
		 * @throws SPECCHIOClientException	could not contact the server
		 */
		public SpectrumSpatialGeometryEavMetadataComponent(SpectrumMetadataCategoryContainer container, MD_EAV_Field field) throws SPECCHIOClientException {
			
			super(container, field);
			
			table_model = new EditableTableModel();
			
			// Create column
			table_model.addColumn("Latitude");	
			table_model.addColumn("Longitude");	
			
			

			String displayString = "NIL";
			if(!fieldHasMultipleValues()) {	

				
				@SuppressWarnings("unchecked")
				List<Point2D> coords = ((ArrayListWrapper<Point2D>) field.getMetaParameter().getValue()).getList();

				if(coords.size() > 0)
				{
					for(int r=0;r<coords.size();r++)
					{
						table_model.addRow(new Object[]{});

						table_model.setValueAt(((Double)coords.get(r).getY()).toString(), r, 0);
						table_model.setValueAt(((Double)coords.get(r).getX()).toString(), r, 1);
					}	
				}
				else
					table_model.addRow(new Object[]{});

				table = new JTable(table_model);
				table_model.addTableModelListener(this);		
				
				// add popup menu to add or delete rows
				// see: https://stackoverflow.com/questions/16743427/jtable-right-click-popup-menu
				final JPopupMenu popupMenu = new JPopupMenu();
				JMenuItem deleteRow = new JMenuItem("Delete Row");

				deleteRow.addActionListener(new ActionListener() {

		            @Override
		            public void actionPerformed(ActionEvent e) {		            			            	
		            	table_model.removeRow(table.getSelectedRow());
		            	Dimension size = table.getSize();
		    			size.height = size.height - 10;
		    			table.setPreferredScrollableViewportSize(size);		
		    			scrollPane.getParent().validate();
		    			scrollPane.getParent().repaint();
		            }
		        });
		        
				JMenuItem addRowAfter = new JMenuItem("Add Row After");
				addRowAfter.addActionListener(new ActionListener() {

		            @Override
		            public void actionPerformed(ActionEvent e) {
		            	
		            	table_model.insertRow(table.getSelectedRow()+1, new Object[]{});
		            	
		            	// highlight newly inserted row
		            	table.setRowSelectionInterval(table.getSelectedRow()+1, table.getSelectedRow()+1);
		            	
		    			Dimension size = table.getSize();
		    			size.height = size.height + 10;
		    			table.setPreferredScrollableViewportSize(size);		
		    			scrollPane.getParent().validate();
		    			scrollPane.getParent().repaint();
		            }
		        });
				
				JMenuItem addRowBefore = new JMenuItem("Add Row Before");
				addRowBefore.addActionListener(new ActionListener() {

		            @Override
		            public void actionPerformed(ActionEvent e) {
		            	
		            	table_model.insertRow(table.getSelectedRow(), new Object[]{});
		            	
		            	// highlight newly inserted row
		            	table.setRowSelectionInterval(table.getSelectedRow(), table.getSelectedRow());
		            	
		            	Dimension size = table.getSize();
		    			size.height = size.height + 10;
		    			table.setPreferredScrollableViewportSize(size);		
		    			scrollPane.getParent().validate();
		    			scrollPane.getParent().repaint();
		            }
		        });
				
				
		        popupMenu.add(deleteRow);
		        popupMenu.add(addRowBefore);
		        popupMenu.add(addRowAfter);		
		        
		        // add popup menu if the geometry is not a point
		        if(!(SpectrumSpatialGeometryEavMetadataComponent.this.getMetaParameter() instanceof MetaSpatialPoint))
		        	table.setComponentPopupMenu(popupMenu);
		        
		        
		        popupMenu.addPopupMenuListener(new PopupMenuListener() {

		            @Override
		            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		                SwingUtilities.invokeLater(new Runnable() {
		                    @Override
		                    public void run() {
		                        int rowAtPoint = table.rowAtPoint(SwingUtilities.convertPoint(popupMenu, new Point(0, 0), table));
		                        if (rowAtPoint > -1) {
		                            table.setRowSelectionInterval(rowAtPoint, rowAtPoint);
		                        }
		                    }
		                });
		            }

		            @Override
		            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		                // TODO Auto-generated method stub
		            }

		            @Override
		            public void popupMenuCanceled(PopupMenuEvent e) {
		                // TODO Auto-generated method stub
		            }
		        });


			}
			else
			{
				displayString = "-- multiple values --";
				table_model.addRow(new Object[]{});
				
				if(field.getMetaParameter() instanceof MetaSpatialPoint)
				{					
					ConflictInfo conflict = field.getConflict();
					
					table_model.setValueAt("Range: " + conflict.spat_val_y_min + " to " + conflict.spat_val_y_max, 0, 0);
					table_model.setValueAt("Range: " + conflict.spat_val_x_min + " to " + conflict.spat_val_x_max, 0, 1);					
				}
				else
				{
					table_model.setValueAt(displayString, 0, 0);
					table_model.setValueAt(displayString, 0, 1);
				}
								
				table = new JTable(table_model);
				table.setEnabled(false);

			}
			
			
					
			//int maxWidths = getMaximalRequiredColumnWidth(table, 0);
			text = new JTextField(displayString, 30);
			Dimension size = text.getPreferredSize();
			
			size.height = size.height + 10 * table_model.getRowCount();
			
			//size = table.getPreferredScrollableViewportSize();
			table.setPreferredScrollableViewportSize(size);		
			table.setFillsViewportHeight(true);			
			
			// renderer to shade uneditable tables (see: https://coderanch.com/t/346106/java/Graying-entire-table)
			table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
				private static final long serialVersionUID = 1L;

				public Component getTableCellRendererComponent(JTable tab, Object val, boolean isSelected, boolean hasFocus, int row, int col)
				{
					setEnabled( tab.isEnabled() );
					return super.getTableCellRendererComponent(tab, val, isSelected, hasFocus, row, col);
				}
			});		
			
			// add help string
			table.setToolTipText("Drag and drop KML files onto table to auto-populate.");


			scrollPane = new JScrollPane(table);
			
			add(scrollPane);
			
			// create a text field but don't display it yet
			text = new JTextField(displayString, 30);
			text.setEditable(false);
			
			
			
			
	        new FileDrop(table, new FileDrop.Listener()
	        {   public void filesDropped( java.io.File[] files )
	            {   for( int i = 0; i < files.length; i++ )
	                {   try
	                    {   
//	                		files[i].getCanonicalPath()
	                	
	                	File file = files[i];
	                	String ext = FilenameUtils.getExtension(file.getCanonicalFile().toString());
	                	
	                	if(ext.equals("kml"))
	                	{
	                		List<Coordinate> coords = null;
	                		
	                		final Kml kml = Kml.unmarshal(file);
	                		Document document = (Document) kml.getFeature();
	                		List<Feature> f = document.getFeature();

	                		// get first entry
	                		Placemark p = (Placemark) f.get(0);
	                		Geometry g = p.getGeometry();

	                		// geometry type dependent
	                		if (g instanceof Polygon && SpectrumSpatialGeometryEavMetadataComponent.this.getMetaParameter() instanceof MetaSpatialPolygon)
	                		{
	                			coords = ((Polygon) g).getOuterBoundaryIs().getLinearRing().getCoordinates();
	                		}
	                		else if (g instanceof de.micromata.opengis.kml.v_2_2_0.Point && SpectrumSpatialGeometryEavMetadataComponent.this.getMetaParameter() instanceof MetaSpatialPoint)
	                		{
	                			coords = ((de.micromata.opengis.kml.v_2_2_0.Point) g).getCoordinates();
	                		}
	                		else if (g instanceof LineString && SpectrumSpatialGeometryEavMetadataComponent.this.getMetaParameter() instanceof MetaSpatialPolyline)
	                		{
	                			coords = ((LineString) g).getCoordinates();
	                		}
	                		else
	                		{
	                			// unknown geometry type or geometry type does not fit the selected metaparameter
	                			String message = "KML Geometry type " + g.getClass().getSimpleName() + " does not fit selected parameter " + SpectrumSpatialGeometryEavMetadataComponent.this.getMetaParameter().getClass().getSimpleName();
	                			JOptionPane.showMessageDialog(SpectrumSpatialGeometryEavMetadataComponent.this, message, "Info", JOptionPane.INFORMATION_MESSAGE, SPECCHIOApplication.specchio_icon);
	                		}

	                		// iterate
	                		if (coords != null)
	                		{
	                			Iterator<Coordinate> it = coords.iterator();
	                			
	                			int r = 0; 
	                			while(it.hasNext())
	                			{
	                				Coordinate coord = it.next();
	                			
	        						table_model.addRow(new Object[]{});

	        						table_model.setValueAt(((Double)coord.getLatitude()).toString(), r, 0);
	        						table_model.setValueAt(((Double)coord.getLongitude()).toString(), r, 1);
	                				r++;
	                			}
	                				
	                		}
			            	
	                		// resize table
			            	Dimension size = table.getSize();
			    			size.height = size.height + 10;
			    			table.setPreferredScrollableViewportSize(size);		
			    			scrollPane.getParent().validate();
			    			scrollPane.getParent().repaint();
	                	}
	                	

	                	
	                    }   // end try
	                    catch( java.io.IOException e ) {}
	                }   // end for: through each dropped file
	            }   // end filesDropped
	        }); // end FileDrop.Listener			
			
			
		}
		
		
		/**
		 * Button handler.
		 * 
		 * @param	the event
		 */
		public void actionPerformed(ActionEvent event) {
			
			if (SELECT.equals(event.getActionCommand())) {
			
				// notify listeners of the change
//				fireMetadataFieldChanged(getField(), this.check.isSelected());

				
			} else {
				
				// pass pop-up menu actions to the super class
				super.actionPerformed(event);
				
			}
			
		}
		
		
		/**
		 * Make the field editable or not.
		 * 
		 * @param editable true to make the field editable, false otherwise
		 */
		public void setEditable(boolean editable) {
			
			super.setEditable(editable);
			
			// force re-draw
//			revalidate();
//			repaint();
			
		}
		
		
		/**
		 * Enable or disable the field.
		 *
		 * @param enabled	true or false
		 */
		public void setEnabled(boolean enabled) {
			
			super.setEnabled(enabled);
//			check.setEnabled(enabled && !fieldHasMultipleValues());
			
		}


		@Override
		public JComponent getInputComponent() {
			return table;
		}


		@Override
		public void tableChanged(TableModelEvent arg0) {
			
			// create 2D double array
			ArrayListWrapper<Point2D> coords = new ArrayListWrapper<Point2D>();			
			
			@SuppressWarnings("unchecked")
			Vector<Vector<String>> vector = this.table_model.getDataVector();
			
			for(int row=0;row<vector.size();row++)
			{
				Vector<String> entry = vector.get(row);
				
				String lat_str = (String) entry.get(0); 
				String lon_str = (String) entry.get(1); 
				
				Point2D coord = new Point2D(0.0, 0.0);
				
				// convert to double
				try
				{
					coord.setY(Double.valueOf(lat_str));
				} catch(NullPointerException e)
				{
					
				}
				catch(NumberFormatException e)
				{
					
					
					// should show a warning to the user, e.g. colour the field red ...
				}
				
				try
				{
					coord.setX(Double.valueOf(lon_str));
				} catch(NullPointerException e)
				{
					
				}
				catch(NumberFormatException e)
				{
					
					
					// should show a warning to the user, e.g. colour the field red ...
				}
				
				coords.getList().add(coord);
				
			}
			
			
			
			fireMetadataFieldChanged(getField(), coords);
		}
				
		
	}
	
		
		
	
	/**
	 * Component for manipulating spectrum links.
	 */
	private class SpectrumLinkEavMetadataComponent extends SpectrumSimpleEavMetadataComponent implements ActionListener {
		
		/** serialisation version identifier */
		private static final long serialVersionUID = 1L;
		
		/** the "select" button */
		private JButton selectButton;
		private JButton showLinkedSpectrumButton;
		
		/** the text field for non-editable display */
		private JTextField text;
		
		/** action command for the "select" button */
		private static final String SELECT = "Select";
		
		/** action command for the "select" button */
		private static final String SHOW = "Show";
		
		
		/**
		 * Constructor.
		 * 
		 * @param container	the category container panel to which this component belongs
		 * @param field	the metadata field represented by this component
		 * 
		 * @throws SPECCHIOClientException	could not contact the server
		 */
		public SpectrumLinkEavMetadataComponent(SpectrumMetadataCategoryContainer container, MD_EAV_Field field) throws SPECCHIOClientException {
			
			super(container, field);


//			if(!fieldHasMultipleValues()) {
//
//				
//			
//			}
//			else
//			{
//				displayString = "-- multiple taxa --";
//				selectButton = new JButton(displayString);
//				selectButton.setEnabled(false);
//			}
			
			selectButton = new JButton("Open Spectral Databrowser");
			showLinkedSpectrumButton = new JButton("Show linked spectrum");
												
			// create a button for selecting the spectrum to link
//			selectButton.setActionCommand(SELECT);
//			selectButton.addActionListener(this);
//			add(selectButton);
			
			showLinkedSpectrumButton.setActionCommand(SHOW);
			showLinkedSpectrumButton.addActionListener(this);
			add(showLinkedSpectrumButton);			
			
			// create a text field but don't display it yet
			text = new JTextField("", 30);
			text.setEditable(false);
			
			
		}
		
		
		/**
		 * Button handler.
		 * 
		 * @param	the event
		 */
		public void actionPerformed(ActionEvent event) {
			
			if (SELECT.equals(event.getActionCommand())) {
			
//				try {
//					
//					// get the meta-paramater to be edited
//					MetaTaxonomy mp = (MetaTaxonomy)getMetaParameter();
//								
//					// show the taxonomy selection dialog
//					TaxonomySelectionDialog d = new TaxonomySelectionDialog(owner, specchioClient, mp);
//					d.setLocation(selectButton.getLocationOnScreen());
//					d.setVisible(true);
//
//					int tax_id = d.getSelectedTaxonomyId();
//					if (tax_id > 0) {
//						Long taxonomy_id = (long) tax_id;
//					
//						// update value of taxonomy button
//						TaxonomyNodeObject taxonomy;
//						taxonomy = specchioClient.getTaxonomyNode(tax_id);
//						selectButton.setText(taxonomy.getName());
//						
//						// notify listeners of the change
//						fireMetadataFieldChanged(getField(), taxonomy_id);
//					}
//					
//				} catch (SPECCHIOClientException ex) {
//					ErrorDialog error = new ErrorDialog(owner, "Could not retrieve taxonomy", ex.getUserMessage(), ex);
//					error.setVisible(true);
//				}
				
			} 
			else if  (SHOW.equals(event.getActionCommand())) 
			{
				
				// get the meta info to be shown
				MetaLink mp = (MetaLink)getMetaParameter();
							
				ArrayList<Integer> ids = new ArrayList<Integer>();
				long id =  (Long) mp.getValue();
				ids.add((int) id);
				
				QueryBuilder d = new QueryBuilder();
				d.set_ids_matching_query(ids);
				
				ActionEvent e = new ActionEvent("Call from SpectrumLinkEavMetadataComponent", 0, "show_report_of_set_ids");
				d.actionPerformed(e);
				
			}
			else {
				
				// pass pop-up menu actions to the super class
				super.actionPerformed(event);
				
			}
		}
			
		
		
		/**
		 * Make the field editable or not.
		 * 
		 * @param editable true to make the field editable, false otherwise
		 */
		public void setEditable(boolean editable) {
			
			if (editable) {
				// display the combo box
				//remove(text);
				//add(selectButton);
			} else {
				// display the label
				//remove(selectButton);
				//add(text);
			}
			
			// force re-draw
			revalidate();
			repaint();
			
		}
		
		
		/**
		 * Enable or disable the field.
		 *
		 * @param enabled	true or false
		 */
		public void setEnabled(boolean enabled) {
			
			super.setEnabled(enabled);
			selectButton.setEnabled(enabled && !fieldHasMultipleValues());
			
		}
		
	}	
	
	
	public void valueChanged(ListSelectionEvent arg0) {
		
//		startOperation();
		try {
			mdec.set_form_descriptor(category_list.getFormDescriptor(), true);
			form = mdec.getForm();
			
			buildGUI(true);
		}
  		catch (SPECCHIOClientException ex) {
			ErrorDialog error = new ErrorDialog(this.owner, "Error", ex.getUserMessage(), ex);
			error.setVisible(true);

	    }
//		endOperation();
		
	}	

}
