package ch.specchio.proc_modules;


import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.ListIterator;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.jgraph.graph.CellView;
import org.jgraph.graph.GraphConstants;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.constants.SpaceTypes;
import ch.specchio.gui.GridbagLayouter;
import ch.specchio.gui.SPECCHIOApplication;
import ch.specchio.interfaces.ProgressReportInterface;
import ch.specchio.spaces.MeasurementUnit;
import ch.specchio.spaces.Space;
import ch.specchio.spaces.SpectralSpace;



class SmartProgressBar extends JProgressBar implements ProgressReportInterface
{
	private static final long serialVersionUID = 1L;
	private int internal_step_size, curr_internal_value;
	private boolean internal_step_conversion = false;
	

	public SmartProgressBar(int min, int max)
	{
		adjust_to_internal_values(min,max);
		set_min_max(min, max);
		this.setFont(new Font("Arial", Font.PLAIN,11));
	}
	
	
	public void set_component(String c)
	{
		// not used
	}
	
	
	public void set_indeterminate(boolean indeterminate)
	{
		setIndeterminate(indeterminate);
	}
	
	public boolean set_progress(double value)
	{
	     setValue((int)value);
	     
	     return true;
	}

	public boolean set_progress(int value) 
	{
		if(internal_step_conversion)
		{
			if(value >= (curr_internal_value + internal_step_size))
			{
				curr_internal_value += internal_step_size;
				setValue(curr_internal_value);
				return true;
			}			
		}
		else
		{
			setValue(value);
			return true;
		}
		return false;
	}

	public void set_operation(String op) {
		setString(op);		
	}

	public void set_min_max(int min, int max) {
		this.setMinimum(min);
		this.setMaximum(max);	
		adjust_to_internal_values(min,max);
	}
	
	void adjust_to_internal_values(int min, int max)
	{
		internal_step_conversion = false;
		if (max > 200)
		{
			//internal_max = (int) (max*0.1);
			internal_step_size = (int) (max*0.1);
			internal_step_conversion = true;
			curr_internal_value = 0;
		}
	}
	
}



public abstract class Module extends ProcessingChainComponent  implements ModuleCallback, ProgressReportInterface{

	protected ArrayList<SpaceProcessingChainComponent> input_spaces = new ArrayList<SpaceProcessingChainComponent>();
	protected ArrayList<SpaceProcessingChainComponent> output_spaces = new ArrayList<SpaceProcessingChainComponent>();
	protected ArrayList<RequiredInputSpaceStruct> required_input_spaces = new ArrayList<RequiredInputSpaceStruct>();
	protected ArrayList<OutputSpaceStruct> output_space_definitions = new ArrayList<OutputSpaceStruct>();
	Hashtable<SpaceProcessingChainComponent, SpaceProcessingChainComponent> old_space_new_space_hash =
			new Hashtable<SpaceProcessingChainComponent, SpaceProcessingChainComponent>();
	JLabel module_name_label;
	JLabel module_type_label;
	JLabel settings;
	JLabel settings_label = new JLabel("Settings");
	JLabel status_label = new JLabel("Status");
	SmartProgressBar progressBar;
	String set_input_spaces_str =  "Set Input Spaces";
	JMenuItem config_menuItem;
	
	static protected int CONFIG = 1;
	static protected int INPUT_SPACE_DEF = 2;
	protected boolean valid_configuration = true;
	
	protected SPECCHIOClient specchio_client;
	
	public Module(Frame owner, SPECCHIOClient specchio_client)
	{
		super(owner);
		
		this.specchio_client = specchio_client;
		
		define_required_input_spaces();
		
		config_menuItem = new JMenuItem("Configure");
		config_menuItem.addActionListener(this);
	    popup.add(config_menuItem);
	    JMenuItem menuItem = new JMenuItem(set_input_spaces_str);
	    menuItem.addActionListener(this);
	    popup.add(menuItem);
	    menuItem = new JMenuItem("Remove");
	    menuItem.setEnabled(false);
	    menuItem.addActionListener(this);
	    popup.add(menuItem);	
	    
	    progressBar = new SmartProgressBar(0,100);
	    progressBar.setValue(0);
	    progressBar.setStringPainted(true);
	    progressBar.setString("Waiting for data.");

	}
	
	
	public void set_component(String c)
	{
		progressBar.set_component(c);
	}
	
	
	public void set_indeterminate(boolean indeterminate)
	{
		progressBar.set_indeterminate(indeterminate);
	}
	
	
	public boolean set_progress(double value)
	{
		set_progress((int)value);
		return true;
	}

	public boolean set_progress(int value) {
		boolean updated = progressBar.set_progress(value);	
		if(processing_plane != null && updated)
		{
			processing_plane.redraw_object(get_ppo());
		}
		return updated;
	}

	public void set_operation(String op) {
		progressBar.setString(op);
		if (processing_plane != null) {
			processing_plane.redraw_object(get_ppo());
		}
	}

	public void set_min_max(int min, int max) {
		progressBar.set_min_max(min, max);	
	}

	public void actionPerformed(ActionEvent e) 
	{
		
		if("Configure".equals(e.getActionCommand()))
		{
			configure();
		}
		
		if(set_input_spaces_str.equals(e.getActionCommand()))
		{
			input_space_definition();
		}
		
	}

	abstract public void configure();
	
	void define_required_input_spaces()
	{		
		required_input_spaces.add(new RequiredInputSpaceStruct("Input Space", null));
	}
	
	void fill_required_input_space_structures_with_available_spaces()
	{
		ListIterator<RequiredInputSpaceStruct> li = required_input_spaces.listIterator();	
		while(li.hasNext())
		{	
			li.next().spaces = this.processing_plane.get_spaces();
		}		
	}
	

	public void input_space_definition()
	{
		fill_required_input_space_structures_with_available_spaces();
		
		DialogThread dt = new DialogThread(new InputSpaceSelectionDialog(required_input_spaces, get_coords_for_popups()), this, INPUT_SPACE_DEF);
		dt.start();
		
	}
	
	
	Point get_coords_for_popups()
	{
		// get the screen coords to place the dialog near the module
		CellView v = this.processing_plane.graph.getGraphLayoutCache().getMapping(ppo, false);
		Rectangle2D d = v.getBounds();		
		Point loc = processing_plane.main_panel.getLocationOnScreen();
		loc.x += d.getX();
		loc.y += d.getY();
		
		return loc;
	}
	
	public void define_output_spaces()
	{
		output_space_definitions.add(new OutputSpaceStruct(""));
	}
		
	public synchronized SpaceProcessingChainComponent add_input_space(SpaceProcessingChainComponent spcc, int index)
	{
		if(index == -1)
			input_spaces.add(spcc);
		else
			input_spaces.add(index, spcc);
		
		spcc.add_following_module(this);
		this.notifyAll();
		
		return spcc;
	}
	
	public void remove_input_space(SpaceProcessingChainComponent space)
	{
		input_spaces.remove(space);
	}
	
	public void remove_output_space(SpaceProcessingChainComponent space)
	{
		output_spaces.remove(space);
	}
	
	
	// this rebuilds the output spaces (recursive!)
	public void update_output_spaces() throws SPECCHIOClientException
	{
		
		// only update if there are any output spaces at all
		// this also handles updates due to configuration changes when the output spaces are not yet created
		// e.g. configuring waveband filters without the input space yet connected
		if(output_spaces.size() > 0)
		{
				
			// keep copy of old spaces
			ArrayList<SpaceProcessingChainComponent> old_spaces = this.output_spaces;
			
			create_output_spaces();
			
			for(int i=0;i < old_spaces.size();i++)
			{
				ArrayList<Module> following_modules = old_spaces.get(i).get_following_modules();
				
				processing_plane.deregister_space(old_spaces.get(i));
				processing_plane.remove_processing_plane_object(old_spaces.get(i));
	
				// add new space and connect on the processing plane
				// get position of old space
				Rectangle2D rect = GraphConstants.getBounds(old_spaces.get(i).get_ppo().getAttributes());
				add_space_to_processing_plane_and_connect(output_spaces.get(i), (int)rect.getMinX(), (int)rect.getMinY());
				
				for(int j=0; j<following_modules.size();j++)
				{
					
					following_modules.get(j).exchange_space(old_spaces.get(i), output_spaces.get(i));
					
					following_modules.get(j).update_output_spaces();		
				}
				
				remove_output_space(old_spaces.get(i));
	
			}
		
		}
	}
	
	public ArrayList<SpaceProcessingChainComponent> create_output_spaces_and_add_to_processing_plane(int x, int y) throws SPECCHIOClientException
	{
		// create output space and add to processing plane
		create_output_spaces();
		
		for(int i=0; i<output_spaces.size();i++)
		{
			add_space_to_processing_plane_and_connect(output_spaces.get(i), x + i*200, y);					
		}
		
		return output_spaces;

	}
	
	public void add_space_to_processing_plane_and_connect(SpaceProcessingChainComponent spcc, int x, int y)
	{
		processing_plane.register_space(spcc);	
		processing_plane.add_processing_plane_object(spcc, x, y);				
		processing_plane.add_edge(this.get_ppo(), spcc.get_ppo());							
	}
	
	
	public ArrayList<SpaceProcessingChainComponent> create_output_spaces() throws SPECCHIOClientException
	{
		output_spaces = new ArrayList<SpaceProcessingChainComponent>();
		
		define_output_spaces();
		
		Space space = null;
		if(get_output_space_type() == SpaceTypes.SpectralSpace)
		{			
			space = new SpectralSpace(get_measurement_unit());
			
			((SpectralSpace) space).setAverageWavelengths(get_output_space_vector());
		}
		
//		if(get_output_space_type() == SpaceTypes.RefPanelCalSpace)
//		{			
//			space = new RefPanelCalSpace(((PanelCorrFactorsSelection) this).get_sensor_id(), get_measurement_unit());
//			
//			((SpectralSpace) space).setAverageWavelengths(get_output_space_vector());
//		}
		
		SpaceProcessingChainComponent spcc = new SpaceProcessingChainComponent(owner, space);
		
		// check if the new space is valid
		// if a module cannot create a space, then it sets the dimension to zero
		if (space != null && space.getDimensionality() > 0)
		{
			spcc.set_space_name(output_space_definitions.get(0).space_name);
			set_spectrum_ids_in_output_space(space);
			output_spaces.add(spcc);
		}
		else
		{
			JOptionPane.showMessageDialog(owner,"Cannot create output space because the input spectra have no wavelength reference. \nPlease set a sensor with the correct wvls in the metadata editor.", "Error",
	    			JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon);
		}
		
		return output_spaces;
	}
	
	
	// may be overridden by sub-classes if the id's cannot be transferred that simply
	void set_spectrum_ids_in_output_space(Space space) throws SPECCHIOClientException
	{
		space.setSpectrumIds(get_main_input_space().getSpace().getSpectrumIds());
	}
	
	
	public synchronized void run()
	{
		while(true)
		{
			Integer space_state = SpaceProcessingChainComponent.DATA_NOT_READY;
			// wait for data to be ready
			while(space_state == SpaceProcessingChainComponent.DATA_NOT_READY)
			{
				// if not all required spaces are set, wait till woken up
				if(input_spaces.size() != required_input_spaces.size())
				{
					try {
						wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else
				{
					// check that all input spaces are ready
					for(int i = 0;i < input_spaces.size(); i++)
					{
						space_state = input_spaces.get(i).data_ready(this);
						
						if(space_state == SpaceProcessingChainComponent.SPACE_INVALID)
						{
							System.out.println("some space is invalid");
							revalidate_input_spaces();
							
							// do we need to reset i to 0 ????
						}
		
						
					}
				}
			}
			
			
			if(space_state == SpaceProcessingChainComponent.DATA_READY)
			{
				try {
					// process the data
					process();
				
					// notify spaces
					for(int i=0;i < output_spaces.size();i++)
					{
						output_spaces.get(i).finalized();
					}
				
					for(int i = 0;i < input_spaces.size(); i++)
					{
						input_spaces.get(i).all_data_read(this);
					}
				}
		  		catch (SPECCHIOClientException ex) {
					JOptionPane.showMessageDialog(
			    			owner,
			    			ex.getMessage(),
			    			"Server error",
			    			JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
			    		);
			    }
				catch (ModuleException ex) {
					JOptionPane.showMessageDialog(
			    			owner,
			    			ex.getMessage(),
			    			"Module error",
			    			JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
			    		);
			    }
			}
			

		}
	}
	
	
	public void exchange_space(SpaceProcessingChainComponent old_space, SpaceProcessingChainComponent new_space)
	{
		
		old_space_new_space_hash.put(old_space, new_space);
		
		// invalidate all input spaces
		ListIterator<SpaceProcessingChainComponent> li = this.input_spaces.listIterator();	
		while(li.hasNext())
		{	
			li.next().set_invalid();
		}
	}
	
	
	public void revalidate_input_spaces()
	{
		// exchange the ones that need exchanging
		Enumeration<SpaceProcessingChainComponent> e = old_space_new_space_hash.keys();
		
		while(e.hasMoreElements())
		{
			SpaceProcessingChainComponent old_space = e.nextElement();
			SpaceProcessingChainComponent new_space = old_space_new_space_hash.get(old_space);
			
			// get position in input spaces
			int pos = input_spaces.indexOf(old_space);
		    this.remove_input_space(old_space);
		    add_and_connect_input_space(new_space, pos);
		}
		
		old_space_new_space_hash.clear();
		
		// set all input spaces to previous setting
		// invalidate all input spaces
		ListIterator<SpaceProcessingChainComponent> li = this.input_spaces.listIterator();	
		while(li.hasNext())
		{	
			li.next().set_valid();
		}		

	}

	
	// this is the space whose spectrum_ids will be used in the output space
	// ... might need some reengineering ...
	public SpaceProcessingChainComponent get_main_input_space()
	{
		if(input_spaces.size() > 0)
			return input_spaces.get(0);
		else
		{
			System.out.println("Empty space found!");
			return null;
		}
	}
	
	public SpaceProcessingChainComponent get_main_output_space()
	{
		if(output_spaces.size() > 0)
			return output_spaces.get(0);
		else
			return null;
	}
	
	
	public void process() throws SPECCHIOClientException, ModuleException
	{
		// clear output space
		for(int i=0;i < output_spaces.size();i++)
		{
			output_spaces.get(i).getSpace().clearDataVectors();
		}
		
		set_operation("");
		
		// do the transformation, then call the module following the output space to do it's processing
		transform();
		
		try {
			this.set_min_max(0, 100); // make sure that 100% is 100% no matter what other processes have set (e.g. file output manager)
			set_progress(100);
			this.wait(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		set_progress(0);
		set_operation("Waiting for data.");
	}
	
	// the actual transformation
	abstract public void transform() throws SPECCHIOClientException, ModuleException;
	
	abstract public int get_output_space_type();
	abstract public int get_required_input_space_type();
	//abstract public int no_of_datapoints();
	public abstract double[] get_output_space_vector() throws SPECCHIOClientException;
	
	abstract public MeasurementUnit get_measurement_unit();
	
	
	public JPanel get_info_panel()
	{
		super.get_info_panel();
		GridbagLayouter space_panel_l = new GridbagLayouter(info_panel);
		GridBagConstraints constraints = new GridBagConstraints();
		
		info_panel.setBorder(BorderFactory.createLineBorder(Color.black));
		
		Font label_font = new Font("Arial", Font.PLAIN,12);
		module_type_label.setFont(label_font);
		module_name_label.setFont(label_font);
		settings.setFont(label_font);
		settings_label.setFont(label_font);
		status_label.setFont(label_font);
		
		constraints.ipadx = 5;
		constraints.ipady = 5;
		constraints.gridx = 0;
		constraints.gridy = 0;
		space_panel_l.insertComponent(module_type_label, constraints);
		
		constraints.gridx = 1;
		space_panel_l.insertComponent(module_name_label, constraints);
		
		constraints.gridx = 0;
		constraints.gridy = 1;
		space_panel_l.insertComponent(settings_label, constraints);

		constraints.gridx = 1;
		space_panel_l.insertComponent(settings, constraints);
		
		constraints.gridx = 0;
		constraints.gridy = 2;
		space_panel_l.insertComponent(status_label, constraints);

		constraints.gridx = 1;		
		space_panel_l.insertComponent(progressBar, constraints);

		return info_panel;
		
	}
	
	public void add_and_connect_input_space(SpaceProcessingChainComponent s)
	{
		add_and_connect_input_space(s, -1);
	}
	
	public void add_and_connect_input_space(SpaceProcessingChainComponent spcc, int index)
	{
		// add this space to the input spaces of this module
		this.add_input_space(spcc, index);
		
		// add the edge to the processing plane
		this.processing_plane.add_edge(spcc.get_ppo(), this.get_ppo());		
	}
	
	public void user_data_provided(DialogThread dt) throws SPECCHIOClientException {
		boolean ret_val = dt.ret_val;
		
		
		if(ret_val == true)
		{
			if(dt.callback_value == Module.INPUT_SPACE_DEF)
			{			
				ArrayList<RequiredInputSpaceStruct> selected_input_spaces = ((InputSpaceSelectionDialog)dt.md).get_selected_spaces();
				
				for(int s=0;s < selected_input_spaces.size();s++)
				{
					required_input_spaces.get(s).chosen_space = selected_input_spaces.get(s).chosen_space;
					add_and_connect_input_space(required_input_spaces.get(s).chosen_space, -1);
					
				}
			
			}
		}
		
	}

}