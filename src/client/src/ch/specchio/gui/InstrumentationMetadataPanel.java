package ch.specchio.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

//import org.freixas.jcalendar.DateEvent;
//import org.freixas.jcalendar.DateListener;
//import org.freixas.jcalendar.JCalendar;
//import org.freixas.jcalendar.JCalendarCombo;
import ch.specchio.metadata.MD_EAV_Field;
import ch.specchio.types.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.file.reader.calibration.CalibrationFileLoader;

public class InstrumentationMetadataPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	private InstrumentationMetadataEditorGroup editor_group = null;
	private boolean border;
	protected String object_title;
	protected GridbagLayouter l;
	protected GridBagConstraints constraints;
	boolean changed;
	boolean editable = true;
	boolean allow_conflicts;
	Color default_background_colour = null;
	protected SPECCHIOClient specchio_client;
	protected Frame parent;
	
	public InstrumentationMetadataPanel(Frame parent, SPECCHIOClient specchio_client, String object_title) {
		
		this.object_title = object_title;
		this.parent = parent;
		this.specchio_client = specchio_client;
		
		allow_conflicts = false;
		
		l = new GridbagLayouter(this);
		constraints = new GridBagConstraints();
		
		// some default values. subclasses can always overwrite these
		constraints.gridwidth = 1;
		constraints.insets = new Insets(4, 4, 4, 4);
		constraints.gridheight = 1;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		
		default_background_colour = this.getBackground();
		
	}
	
	
	public boolean changed()
	{
		return this.changed;
	}

	
	// notifies this object that one of the field contents was changed
	public void changed(boolean is_changed)
	{
		changed = is_changed;
		
		// inform metadata_object_group
		InstrumentationMetadataEditorGroup editor_group = get_editor_group();
		if(editor_group != null)
			editor_group.changed(changed);
		
	}
	
	
	private InstrumentationMetadataEditorGroup get_editor_group()
	{
		
		if(editor_group == null)
		{
			// search for the MetadataObjectGroup that contains the instance of the component
			boolean found = false;
			Container c = this;
			while(found == false && c != null)
			{
				c = c.getParent();
				if(c instanceof InstrumentationMetadataEditorGroup)
				{
					found = true;
					editor_group = (InstrumentationMetadataEditorGroup)c;
				}
			}
						
		}

		return editor_group;
	}
	
	public boolean getEditable()
	{
		return this.editable;
	}
	
	public void setEditable(boolean editable)
	{
		this.editable = false;
	}
	
	public void show_border(boolean border)
	{
		this.border = border;
		
		if(this.border)
		{
			Border blackline = BorderFactory.createLineBorder(Color.black);
			TitledBorder tb;
			
			tb = BorderFactory.createTitledBorder(blackline, object_title);
			this.setBorder(tb);
		}
	
	}
	
	// called after metadata has been updated on the server
	public void updateComplete() {
		
		// do nothing
		
	}

}


class CalibrationListMetadataPanel extends InstrumentationMetadataPanel implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	private JPanel cal_panel;
	private GridbagLayouter cal_panel_l;
	private JScrollPane calScroller;
	private JFileChooser fc;
	private JPopupMenu popup;
	private JMenuItem fileMenuItem;
	private JMenuItem addMenuItem;
	private JMenuItem removeMenuItem;
	private MouseListener popupListener;
	private CalibrationMetadataPanel popup_source = null;
	private ArrayList<CalibrationMetadataPanel> cal_md_panels;
	private String object_type;
	private int object_id = 0;

	public CalibrationListMetadataPanel(Frame parent, SPECCHIOClient specchio_client, String object_type) {
		
		super(parent, specchio_client, "Calibration Data");
		
		this.object_type = object_type;
		
		// create GUI using grid layout			
		constraints.gridx = 0;
		constraints.gridy = 0;		
		constraints.gridwidth = 2;
		
		cal_panel = new JPanel();
		popupListener = new PopupListener();
		cal_panel.addMouseListener(popupListener);
		
		cal_panel_l = new GridbagLayouter(cal_panel);
		this.setLayout(new BorderLayout());
				
		calScroller = new JScrollPane(cal_panel);
		calScroller.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
		calScroller.getVerticalScrollBar().setUnitIncrement(10);
		
		this.add(calScroller, BorderLayout.CENTER);
		
		// add border around scroll list
		Border blackline = BorderFactory.createLineBorder(Color.black);
		calScroller.setBorder(BorderFactory.createTitledBorder(blackline, "Calibrations"));
		
		fc = new JFileChooser();
		
		popup = new JPopupMenu();
		fileMenuItem = new JMenuItem("Add calibration from file");
	    fileMenuItem.addActionListener(this);
	    popup.add(fileMenuItem);
		addMenuItem = new JMenuItem("Add calibration");
	    addMenuItem.addActionListener(this);
	    popup.add(addMenuItem);	    
	    removeMenuItem = new JMenuItem("Remove calibration");
	    removeMenuItem.addActionListener(this);
	    popup.add(removeMenuItem);
	    
	    cal_md_panels = new ArrayList<CalibrationMetadataPanel>();
		
		setPreferredSize(new Dimension(500, 350));
		
	}
	
	
	public void actionPerformed(ActionEvent e)
	{				
		if ("Add calibration from file".equals(e.getActionCommand())) {
			
			int returnVal = fc.showOpenDialog(this);
			
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				
				try {
					CalibrationFileLoader cfl = new CalibrationFileLoader(specchio_client);
					Calibration c = cfl.loadFile(fc.getSelectedFile());
					c.setReferenceId(this.object_id);
					if ("instrument".equals(object_type)) {
						specchio_client.insertInstrumentCalibration(c);
					} else if ("reference".equals(object_type)) {
						specchio_client.insertReferenceCalibration(c);
					}
					addCalibrationMetadata(c);
				}
				catch (IOException ex) {
					JOptionPane.showMessageDialog(
			    		SPECCHIOApplication.getInstance().get_frame(),
			    		ex.getMessage(),
			    		"Error",
			    		JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
			    	);
			    }
				catch (SPECCHIOClientException ex) {
					JOptionPane.showMessageDialog(
			    			SPECCHIOApplication.getInstance().get_frame(),
			    			ex.getMessage(),
			    			"Error",
			    			JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
			    		);
			    }
				catch (MetaParameterFormatException ex) {
					JOptionPane.showMessageDialog(
			    			SPECCHIOApplication.getInstance().get_frame(),
			    			ex.getMessage(),
			    			"Error",
			    			JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
			    		);
			    }
				
			}	    	
		}
		
		if ("Add calibration".equals(e.getActionCommand())) {
			
			try {
				Calibration c = new Calibration();
				c.setReferenceId(this.object_id);
				if ("instrument".equals(object_type)) {
					specchio_client.insertInstrumentCalibration(c);
				} else if ("reference".equals(object_type)) {
					specchio_client.insertReferenceCalibration(c);
				}
				addCalibrationMetadata(c);
			}
			catch (SPECCHIOClientException ex) {
				JOptionPane.showMessageDialog(
		    			SPECCHIOApplication.getInstance().get_frame(),
		    			ex.getMessage(),
		    			"Error",
		    			JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
		    		);
		    }
	
		}		
		
		if ("Remove calibration".equals(e.getActionCommand())) {

			if (popup_source != null) {
				try {
					specchio_client.deleteCalibration(popup_source.getCalibrationId());
					deleteCalibrationMetadataPanel(popup_source);
				}
				catch (SPECCHIOClientException ex) {
					JOptionPane.showMessageDialog(
			    			SPECCHIOApplication.getInstance().get_frame(),
			    			ex.getMessage(),
			    			"Error",
			    			JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
			    		);
			    }
			}
			
		}
	}

	
	
	public void clear()
	{
		cal_md_panels.clear();
		cal_panel.removeAll();
		cal_panel.revalidate();
	}
	
	
	public void addCalibrationMetadata(Calibration cal) throws SPECCHIOClientException
	{
		// download the calibration metadata from the server
		CalibrationMetadata[] cm = null;
		if ("instrument".equals(object_type)) {
			cm = specchio_client.getInstrumentCalibrationMetadata(cal.getReferenceId());
		} else if ("reference".equals(object_type)) {
			cm = specchio_client.getReferenceCalibrationMetadata(cal.getReferenceId());
		}
		
		// laod the metadata into the panel
		if (cm != null && cm.length > 0) {
			setCalibrationMetadata(cm);
		}
	}
	
	
	public void addCalibrationMetadata(CalibrationMetadata cm)
	{
		CalibrationMetadataPanel cmp = new CalibrationMetadataPanel(parent, specchio_client, "", cm);
		cal_md_panels.add(cmp);
		constraints.gridx = 0;
		constraints.gridy = cal_md_panels.size();
		constraints.ipady = 10;
		cal_panel_l.insertComponent(cmp, constraints);
		MouseListener popupListener = new PopupListener();
		cmp.addMouseListener(popupListener);
	}
	
	
	private void deleteCalibrationMetadataPanel(CalibrationMetadataPanel cmp)
	{
		cal_md_panels.remove(cmp);
		cal_panel.remove(cmp);
		cal_panel.revalidate();
		cal_panel.repaint();
	}
	
	
	public List<CalibrationMetadata> getChangedCalibrationMetadata() throws SPECCHIOUserInterfaceException
	{
		List<CalibrationMetadata> cm_list = new ArrayList<CalibrationMetadata>();
		
		for (CalibrationMetadataPanel cal_md_panel : cal_md_panels) {
			if (cal_md_panel.changed()) {
				cm_list.add(cal_md_panel.getCalibrationMetadata());
			}
		}
		
		return cm_list;
	}
	
	
	public void setCalibrationMetadata(CalibrationMetadata cm[])
	{
		// remove the existing panels
		clear();
		
		if (cm != null) {
			// add the new panels
			for (int i = 0; i < cm.length; i++) {
				addCalibrationMetadata(cm[i]);
			}
		}
		
		// re-draw
		cal_panel.revalidate();
		cal_panel.repaint();
	}
	
	
	public void setEditable(boolean editable)
	{
		super.setEditable(editable);
		cal_panel.removeMouseListener(popupListener);
		if (editable)  {
			cal_panel.addMouseListener(popupListener);
		}
	}
	
	
	
	public void setObjectId(int object_id)
	{
		this.object_id = object_id;
	}
			
	
	
	private class PopupListener extends MouseAdapter {
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}
		
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}
		
		private void maybeShowPopup(MouseEvent e) {
			if (e.isPopupTrigger() && object_id != 0) {
				Component c = e.getComponent();
				if(c instanceof CalibrationMetadataPanel) {				
					popup_source = (CalibrationMetadataPanel)e.getComponent();
					removeMenuItem.setEnabled(true);
				} else {
					popup_source = null;
					removeMenuItem.setEnabled(false);
				}
				popup.show(e.getComponent(), e.getX(), e.getY());
				
			}
		}
	}	
	
}


class CalibrationMetadataPanel extends InstrumentationMetadataPanel implements DateListener, KeyListener
{
	private static final long serialVersionUID = 1L;
	private int calibration_id;	
	private JCalendarCombo calibration_date;
	private JTextField calibration_no;
	private JTextField comment;
	private CalibrationPlotsMetadataPanel cpm_factors;
	private CalibrationPlotsMetadataPanel cpm_uncertainty;
	
	public CalibrationMetadataPanel(Frame parent, SPECCHIOClient specchio_client, String title, CalibrationMetadata cm)
	{
		super(parent, specchio_client, title);
		
		calibration_id = cm.getCalibrationId();
		
		// create GUI using grid layout		
		constraints.gridx = 0;
		constraints.gridy = 0;
		
		l.insertComponent(new JLabel("Calibration Name:"), constraints);									
		calibration_no = new JTextField(cm.getName(), 10);
		calibration_no.addKeyListener(this);
		constraints.gridx = 1;
		l.insertComponent(calibration_no, constraints);				
		
		constraints.gridx = 0;
		constraints.gridy++;		
		l.insertComponent(new JLabel("Cal Date:"), constraints);
		calibration_date = new JCalendarCombo(
			    JCalendar.DISPLAY_DATE | JCalendar.DISPLAY_TIME,
			    false
			);			
		calibration_date.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
		try {
			calibration_date.setDate(cm.getCalibrationDate().toDateTime());
		} catch(java.lang.NullPointerException ex)
		{
			int wtf = 1;
		}
		calibration_date.addDateListener(this);	
		constraints.gridx = 1;
		l.insertComponent(calibration_date, constraints);
		
		constraints.gridx = 0;
		constraints.gridy++;
		l.insertComponent(new JLabel("Calibration ID (auto. ID):"), constraints);									
		calibration_no = new JTextField(Integer.toString(cm.getCalibration_id()), 10);
		calibration_no.setEditable(false);
		constraints.gridx = 1;
		l.insertComponent(calibration_no, constraints);		

		constraints.gridx = 0;
		constraints.gridy++;
		l.insertComponent(new JLabel("Calibration number:"), constraints);									
		calibration_no = new JTextField(Integer.toString(cm.getCalibrationNumber()), 10);
		calibration_no.addKeyListener(this);
		constraints.gridx = 1;
		l.insertComponent(calibration_no, constraints);		
		
		if(cm.getField_of_view() != 0)
		{
			constraints.gridx = 0;
			constraints.gridy++;
			l.insertComponent(new JLabel("FOV:"), constraints);									
			comment = new JTextField(Integer.toString(cm.getField_of_view()), 20);
			comment.addKeyListener(this);
			constraints.gridx = 1;
			l.insertComponent(comment, constraints);
			
		}
		
		constraints.gridx = 0;
		constraints.gridy++;
		l.insertComponent(new JLabel("Comments:"), constraints);									
		comment = new JTextField(cm.getComments(), 20);
		comment.addKeyListener(this);
		constraints.gridx = 1;
		l.insertComponent(comment, constraints);

		// if there are EAV metadata then add them as well
		try {
			if (cm.getMetadata() != null && cm.getMetadata().getEntries().size() > 0) {
				ListIterator<MetaParameter> it = cm.getMetadata().getEntries().listIterator();

				while (it.hasNext()) {

					MetaParameter mp = it.next();
					constraints.gridx = 0;
					constraints.gridy++;
					l.insertComponent(new JLabel(mp.getAttributeName()), constraints);
					comment = new JTextField(mp.valueAsString(), 20);
					//comment.addKeyListener(this);
					constraints.gridx = 1;
					l.insertComponent(comment, constraints);


				}

			}
		} catch(java.lang.NullPointerException ex) {
			int fail = 1;
		}



		
		constraints.gridx = 0;
		constraints.gridy++;
		cpm_factors = new CalibrationPlotsMetadataPanel(parent, specchio_client,"", cm.getCalFactorsId(), cm.getCalibrationFactorsPlot());
		constraints.gridx = 1;
		l.insertComponent(cpm_factors, constraints);
		
		// only add uncertainty plot of there is an uncertainty
		if(cm.getUncertainty_id() != 0)
		{
			constraints.gridx = 0;
			constraints.gridy++;
			cpm_uncertainty = new CalibrationPlotsMetadataPanel(parent, specchio_client,"", cm.getUncertainty_id(), cm.getCalibrationUncertaintyPlot());
			constraints.gridx = 1;
			l.insertComponent(cpm_uncertainty, constraints);
		}
		
		
	}


	@Override
	public void dateChanged(DateEvent e) {
		changed(true);
	}
	
	
	public int getCalibrationId()
	{
		return calibration_id;
	}
	
	
	public CalibrationMetadata getCalibrationMetadata() throws SPECCHIOUserInterfaceException
	{
		CalibrationMetadata cm = new CalibrationMetadata(calibration_id);
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(calibration_date.getDate().toDate()); // TODO: check if dates and times are properly displayed!
		SimpleDateFormat formatter_1 = new SimpleDateFormat(MetaDate.DEFAULT_DATE_FORMAT);		
		String out=formatter_1.format(cal.getTime());	
		DateTimeFormatter formatter = DateTimeFormat.forPattern(MetaDate.DEFAULT_DATE_FORMAT).withZoneUTC();
		DateTime dt = formatter.parseDateTime(out);	
		
		cm.setCalibrationDate(dt);
		cm.setComments(comment.getText());
		
		String calibration_no_text = calibration_no.getText();
		if (calibration_no_text.length() == 0) {
			throw new SPECCHIOUserInterfaceException("There is a calibration number missing. Please ensure that all calibrations have numbers.");
		}
		try {
			cm.setCalibrationNumber(Integer.parseInt(calibration_no_text));
		}
		catch (NumberFormatException ex) {
			// the calibration number is not an integer
			throw new SPECCHIOUserInterfaceException(
					"The calibration number " + calibration_no.getText() + " is not valid. Calibration numbers must be integers.",
					ex
				);
		}
			
		
		return cm;
	}


	@Override
	public void keyPressed(KeyEvent e) {
		// do nothing
	}


	@Override
	public void keyReleased(KeyEvent e) {
		// do nothing
		
	}


	@Override
	public void keyTyped(KeyEvent e) {
		// something was typed into a text field
		changed(true);
	}

}


class CalibrationPlotsMetadataPanel extends InstrumentationMetadataPanel
{
	private static final long serialVersionUID = 1L;
	
	private FactorsPlotField spectrum_plot;

	public CalibrationPlotsMetadataPanel(Frame parent, SPECCHIOClient specchio_client, String title, int calibration_id, CalibrationPlotsMetadata cpm) {
		super(parent, specchio_client, title);
		spectrum_plot = new FactorsPlotField(cpm.getSpace(), calibration_id);
		this.add(spectrum_plot);
	}
	
}


class InstrumentMetadataPanel extends InstrumentationMetadataPanel implements ActionListener, KeyListener {
	
	private static final long serialVersionUID = 1L;
	
	private Institute owners[];
	private Sensor sensors[];
	
	private JTextField instrument_name;
	private SteppedComboBox instrument_owner;
	private JTextField serial_no;
	private SteppedComboBox sensor;
	/** button for adding a new institute */
	private JButton addInstituteButton;
	
	/** button text for adding a new institute */
	private static final String ADD_INSTITUTE = "Add new institute...";
	

	
	public InstrumentMetadataPanel(Frame parent, SPECCHIOClient specchio_client, Institute owners[], Sensor sensors[])  {
		
		super(parent, specchio_client, "Instrument Data");
		
		// save possible owners and sensors for later
		this.owners = owners;
		this.sensors = sensors;

		// create GUI using grid layout		
		constraints.gridx = 0;
		constraints.gridy = 0;
		l.insertComponent(new JLabel("Instrument name:"), constraints);					
		instrument_name = new JTextField(20);
		instrument_name.addKeyListener(this);
		constraints.gridx = 1;
		l.insertComponent(instrument_name, constraints);
		
		constraints.gridx = 0;
		constraints.gridy++;
		l.insertComponent(new JLabel("Instrument owner:"), constraints);									
		instrument_owner = new SteppedComboBox();	
		instrument_owner.addItem(null);
		for (Institute owner : owners) {
			instrument_owner.addItem(owner);
		}
		instrument_owner.addActionListener(this);
		instrument_owner.setPreferredComboWidth(50);
		constraints.gridx = 1;
		l.insertComponent(instrument_owner, constraints);
		
		// add button to add a new institute easily
		addInstituteButton = new JButton(ADD_INSTITUTE);
		addInstituteButton.setActionCommand(ADD_INSTITUTE);

		addInstituteButton.addActionListener(this);
		constraints.gridx = 2;
		add(addInstituteButton, constraints);
		//constraints.gridy++;
		
		

		constraints.gridx = 0;
		constraints.gridy++;
		l.insertComponent(new JLabel("Serial number:"), constraints);									
		serial_no = new JTextField(10);
		serial_no.addKeyListener(this);
		constraints.gridx = 1;
		l.insertComponent(serial_no, constraints);
		
		
		constraints.gridx = 0;
		constraints.gridy++;
		l.insertComponent(new JLabel("Sensor:"), constraints);									
		sensor = new SteppedComboBox(sensors);
		sensor.addActionListener(this);
		constraints.gridx = 1;
		sensor.setPreferredComboWidth(50);
		l.insertComponent(sensor, constraints);
		
	}
	
	public void actionPerformed(ActionEvent event)
	{
		
	if (ADD_INSTITUTE.equals(event.getActionCommand())) {
		
		try {
			// open the institute dialogue
			InstituteDialog id = new InstituteDialog(parent, specchio_client);
			id.setVisible(true);
			
			Institute inst = id.getInstitute();
			if (inst != null) {
				// add the new institute to the combo box and select it
				instrument_owner.addItem(inst);
				int ind = instrument_owner.getItemCount();
				instrument_owner.setSelectedIndex(ind-1);
				
				// need to re-layout the dialogue since the combo box size might have changed
				//pack();
			}
		}
		catch (SPECCHIOClientException ex) {
			// could not contact the server
			ErrorDialog error = new ErrorDialog(
					parent , "Could not create an institute",
					ex.getUserMessage(),
					ex
				);
			error.setVisible(true);
		}	
	}
		else {
		
			// the value of a combo box was changed
			changed(true);
		
		}
	}
	
	
	public String getInstrumentName() {
		
		return this.instrument_name.getText();
		
	}
	
	
	public String getInstrumentNumber() {
		
		return this.serial_no.getText();
		
	}
	
	
	public Institute getInstrumentOwner() {
		
		return (Institute)this.instrument_owner.getSelectedItem();
		
	}
	
	
	public Sensor getSensor() {
		
		return (Sensor)this.sensor.getSelectedItem();
		
	}
	
	
	public void setEditable(boolean editable)
	{
		super.setEditable(editable);
		instrument_name.setEditable(editable);
		serial_no.setEditable(editable);

		// these should be setEditable() but this does not work as advertised for JComboBoxes
		instrument_owner.setEnabled(editable);
		sensor.setEnabled(editable);
	}
	
	public void setInstrumentData(Instrument instrument) {
		
		if (instrument != null) {
			
			// fill fields with data from the new instrument
			this.instrument_name.setText(instrument.getInstrumentName().value);
			this.serial_no.setText(instrument.getInstrumentNumber().value);
			this.instrument_owner.setSelectedIndex(0);
			for (int i = 0; i < owners.length; i++) {
				if (owners[i].toString().equals(instrument.getInstrumentOwner().value)) {
					this.instrument_owner.setSelectedIndex(i + 1);
				}
			}
			for (int i = 0; i < sensors.length; i++) {
				if (sensors[i].matches(instrument.getSensor())) {
					this.sensor.setSelectedIndex(i);
				}
			}
			
		} else {
			
			// clear fields
			this.instrument_name.setText(null);
			this.serial_no.setText(null);
			this.instrument_owner.setSelectedIndex(-1);
			this.sensor.setSelectedIndex(-1);
			
		}
		
		// reset "changed" flag
		changed(false);
		
	}


	@Override
	public void keyPressed(KeyEvent e) {
		// do nothing
	}


	@Override
	public void keyReleased(KeyEvent e) {
		// do nothing
		
	}


	@Override
	public void keyTyped(KeyEvent e) {
		// something was typed into a text field
		if (getEditable()) {
			changed(true);
		}
	}
	
}


class PictureMetadataPanel extends InstrumentationMetadataPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	
	private JPanel picture_panel;
	private GridbagLayouter picture_panel_l;
	private JFileChooser fc;
	private JPopupMenu popup;
	private int pic_cnt;
	private JScrollPane picScroller;
	private MouseListener popupListener;
	private FigureStruct popup_source = null;
	
	ArrayList<FigureStruct> new_pictures;
	ArrayList<FigureStruct> deleted_pictures;

	public PictureMetadataPanel(Frame parent, SPECCHIOClient specchio_client) {
		
		super(parent, specchio_client, "Pictures");
		
		pic_cnt = 0;	
		
		// create GUI using grid layout			
		constraints.gridx = 0;
		constraints.gridy = 0;		
		constraints.gridwidth = 2;
		
		picture_panel = new JPanel();
		picture_panel_l = new GridbagLayouter(picture_panel);
		this.setLayout(new BorderLayout());
		
		
		picScroller = new JScrollPane(picture_panel);
		this.add(picScroller, BorderLayout.CENTER);
		
		// add border around scroll list
		Border blackline = BorderFactory.createLineBorder(Color.black);
		picScroller.setBorder(BorderFactory.createTitledBorder(blackline, "Pictures"));
		picScroller.getVerticalScrollBar().setUnitIncrement(10);
		
		fc = new JFileChooser();

		popupListener = new PopupListener(this);
		this.addMouseListener(popupListener);
		picScroller.addMouseListener(popupListener);
		
		new_pictures = new ArrayList<FigureStruct>();
		deleted_pictures = new ArrayList<FigureStruct>();	
		
		setPreferredSize(new Dimension(250, 170));
		
	}
	
	
	public PictureMetadataPanel(Frame parent, SPECCHIOClient specchio_client, PictureTable pictures)
	{	
		this(parent, specchio_client);
		setPictureTable(pictures);
	}
	
	
	public void actionPerformed(ActionEvent e)
	{
		if ("Add picture".equals(e.getActionCommand())) {
			
			int returnVal = fc.showOpenDialog(this);
			
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				String path = file.getPath();
				
				FigureStruct f = new FigureStruct(path, 300, 230, 0);
				
				add_figure(f);
				new_pictures.add(f);
				picture_panel.revalidate();
				picture_panel.repaint();
				
				// signal change to object group
				changed(true);
				
			}	    	
	
		}
		
		if("Remove picture".equals(e.getActionCommand()))
		{
			deleted_pictures.add(popup_source);
			picture_panel.remove(popup_source);
			picture_panel.revalidate();
			picture_panel.repaint();
			
			// signal change to object group
			changed(true);
			
		}
			
	}
	
	
	void add_figure(FigureStruct f)
	{
		constraints.gridx = 0;
		constraints.gridy = pic_cnt++;
		picture_panel_l.insertComponent(f, constraints);
		
		f.addMouseListener(popupListener);
	}
	
	
	public List<Integer> getDeletedPictureIds()
	{	
		List<Integer> deleted_picture_ids = new ArrayList<Integer>();
		for (FigureStruct deleted_picture : deleted_pictures) {
			deleted_picture_ids.add(deleted_picture.get_picture_id());
		}
		
		return deleted_picture_ids;
	}
	
	
	public List<Picture> getChangedPictures()
	{
		List<Picture> pictures = new ArrayList<Picture>();
		for (Component c : picture_panel.getComponents()) {
			if (c instanceof FigureStruct) {
				FigureStruct f = (FigureStruct)c;
				if (f.captionChanged() && !new_pictures.contains(f)) {
					Picture picture = new Picture(f.get_picture_id(), 0, f.getCaption(), f.getJPEGImageData());
					pictures.add(picture);
				}
			}
		}
		
		return pictures;
	}
			
	
	
	public List<Picture> getNewPictures()
	{
		List<Picture> pictures = new ArrayList<Picture>();
		for (FigureStruct new_picture : new_pictures) {
			Picture picture = new Picture(new_picture.get_picture_id(), 0, new_picture.getCaption(), new_picture.getJPEGImageData());
			pictures.add(picture);
		}
		
		return pictures;
	}
	
	
	public void setEditable(boolean editable)
	{
		super.setEditable(editable);
		
		// make the main panel editable or not
		picScroller.removeMouseListener(popupListener);
		if (editable) {
			picScroller.addMouseListener(popupListener);
		}
		
		// make individual figures editable or not
		for (Component c : picture_panel.getComponents()) {
			if (c instanceof FigureStruct) {
				((FigureStruct)c).setEditable(editable);
				c.removeMouseListener(popupListener);
				if (editable)  {
					c.addMouseListener(popupListener);
				}
			}
		}
	}
	
	
	public void setPictureTable(PictureTable pictures)
	{
		// remove the existing pictures
		picture_panel.removeAll();
		
		if (pictures != null) {
			// add the new pictures
			Enumeration<Integer> picture_ids = pictures.getIdEnumeration();
			while (picture_ids.hasMoreElements()) {
				Integer picture_id = picture_ids.nextElement();
				Picture picture = pictures.get(picture_id);
				ImageIcon ii = new ImageIcon(picture.getImageData());
				FigureStruct fig = new FigureStruct(ii.getImage(), 300, 230, picture.getCaption(), picture_id);
				add_figure(fig);
			}
		}
		
		// re-validate the panel
		picture_panel.revalidate();
		picture_panel.repaint();
	}
	
	
	@Override
	public void updateComplete()
	{
		new_pictures.clear();
		deleted_pictures.clear();
	}


	private class FigureStruct extends Figure implements KeyListener
	{
		private static final long serialVersionUID = 1L;
		private int picture_id;
		private String originalCaption;
		
		public FigureStruct(String path, int width, int height, int picture_id)
		{
			super(path, width, height);			
			this.picture_id = picture_id;
			this.originalCaption = "";
			addKeyListener(this);
		}
		
		public FigureStruct(Image image, int width, int height, String caption, int picture_id)
		{
			super(image, width, height, caption);			
			this.picture_id = picture_id;
			this.originalCaption = caption;
			addKeyListener(this);
		}
		
		public boolean captionChanged()
		{
			return !originalCaption.equals(getCaption());
		}
		
		public int get_picture_id()
		{
			return this.picture_id;
		}

		@Override
		public void keyPressed(KeyEvent e) {
			// do nothing
		}

		@Override
		public void keyReleased(KeyEvent e) {
			// do nothing
			
		}

		@Override
		public void keyTyped(KeyEvent e) {
			// the caption was changed
			changed(true);
		}	
		
	}

	
	private class PopupListener extends MouseAdapter {
		
		private PictureMetadataPanel parent;
		
		public PopupListener(PictureMetadataPanel parent) {
			this.parent = parent;
		}
		
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}
		
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}
		
		private void maybeShowPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
				
				// create pop-up menu
				popup = new JPopupMenu();
				JMenuItem menuItemAdd = new JMenuItem("Add picture");
			    menuItemAdd.addActionListener(parent);
			    popup.add(menuItemAdd);
			    
			    // add a "remove" option if the mouse is over an existing image
				if (e.getComponent() instanceof FigureStruct) {
				    popup_source = (FigureStruct)e.getComponent();
					JMenuItem menuItemRemove = new JMenuItem("Remove picture");
				    menuItemRemove.addActionListener(parent);
				    popup.add(menuItemRemove);
				}
				
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}
	
}


class ReferenceMetadataPanel extends InstrumentationMetadataPanel implements ActionListener, KeyListener {
	
	private static final long serialVersionUID = 1L;
	
	private Institute owners[];
	private ReferenceBrand brands[];
	
	private JTextField reference_name;
	private JComboBox reference_owner;
	private JTextField serial_no;
	private JComboBox sensor;

	public ReferenceMetadataPanel(Frame parent, SPECCHIOClient specchio_client, Institute owners[], ReferenceBrand brands[]) {
		
		super(parent, specchio_client, "Reference Data");
		
		// save possible owners and brands for later
		this.owners = owners;
		this.brands = brands;
		
		// create GUI using grid layout		
		constraints.gridx = 0;
		constraints.gridy = 0;
		l.insertComponent(new JLabel("Reference name:"), constraints);					
		reference_name = new JTextField(20);
		reference_name.addKeyListener(this);
		constraints.gridx = 1;
		l.insertComponent(reference_name, constraints);
		
		constraints.gridx = 0;
		constraints.gridy++;
		l.insertComponent(new JLabel("Reference owner:"), constraints);									
		reference_owner = new JComboBox(owners);
		reference_owner.addActionListener(this);
		constraints.gridx = 1;
		l.insertComponent(reference_owner, constraints);

		constraints.gridx = 0;
		constraints.gridy++;
		l.insertComponent(new JLabel("Serial number:"), constraints);									
		serial_no = new JTextField(10);
		serial_no.addKeyListener(this);
		constraints.gridx = 1;
		l.insertComponent(serial_no, constraints);		
		
		constraints.gridx = 0;
		constraints.gridy++;
		l.insertComponent(new JLabel("Reference brand:"), constraints);									
		sensor = new JComboBox(brands);
		sensor.addActionListener(this);
		constraints.gridx = 1;
		l.insertComponent(sensor, constraints);
		
	}
	
	
	public void actionPerformed(ActionEvent event)
	{
		// the value of a combo box was changed
		changed(true);
	}
	
	
	public String getSerialNumber()
	{
		return serial_no.getText();
	}
	
	
	public String getReferenceBrand()
	{
		return sensor.getSelectedItem().toString();
	}
	
	
	public String getReferenceName()
	{
		return reference_name.getText();
	}
	
	
	public String getReferenceOwner()
	{
		return reference_owner.getSelectedItem().toString();
	}
	
	
	public void setEditable(boolean editable)
	{
		super.setEditable(editable);
		reference_name.setEditable(editable);
		serial_no.setEditable(editable);

		// these should be setEditable() but this does not work as advertised for JComboBoxes
		sensor.setEnabled(editable);
		reference_owner.setEnabled(editable);
	}
	
	
	public void setReferenceData(Reference ref)
	{
		if (ref != null) {
			// fill fields with the new reference data
			reference_name.setText(ref.getReferenceName().value);
			serial_no.setText(ref.getSerialNumber().value);
			for (int i = 0; i < owners.length; i++) {
				if (owners[i].toString().equals(ref.getReferenceOwner().value)) {
					reference_owner.setSelectedIndex(i);
				}
			}
			for (int i = 0; i < brands.length; i++) {
				if (brands[i].toString().equals(ref.getBrandName().value)) {
					sensor.setSelectedIndex(i);
				}
			}
		} else {
			// fields
			reference_name.setText("");
			serial_no.setText("");
			reference_owner.setSelectedIndex(0);
			sensor.setSelectedIndex(0);
		}
		
		// reset "changed" flag
		changed(false);
			
	}


	@Override
	public void keyPressed(KeyEvent e) {
		// do nothing
	}


	@Override
	public void keyReleased(KeyEvent e) {
		// do nothing
		
	}


	@Override
	public void keyTyped(KeyEvent e) {
		// something was typed into a text field
		if (getEditable()) {
			changed(true);
		}
	}
	
}
