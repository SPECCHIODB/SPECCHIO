package ch.specchio.spaces;

import java.util.ArrayList;

import javax.xml.bind.annotation.*;


/**
 * This class represent a query to the get getSpaces() service.
 */
@XmlRootElement(name="space_query_descriptor")
public class SpaceQueryDescriptor {
	
	@XmlElement public ArrayList<Integer> spectrum_ids;
	@XmlElement public ArrayList<Integer> uncertainty_set_ids;
	@XmlElement public Boolean split_spaces_by_sensor = false;
	@XmlElement public Boolean split_spaces_by_sensor_and_unit = false;
	@XmlElement public String order_by = "date";
	
	
	public SpaceQueryDescriptor() {
		
	}
	
	
	public SpaceQueryDescriptor(ArrayList<Integer> spectrum_ids) {
		
		this.spectrum_ids = spectrum_ids;
		
	}
	
	public SpaceQueryDescriptor(ArrayList<Integer> spectrum_ids, ArrayList<Integer> uncertainty_set_ids) {
		
		this.spectrum_ids = spectrum_ids;
		this.uncertainty_set_ids = uncertainty_set_ids;
		
	}
	
	
	
	public SpaceQueryDescriptor(ArrayList<Integer> spectrum_ids, Boolean split_spaces_by_sensor, Boolean split_spaces_by_sensor_and_unit) {
		
		this(spectrum_ids);
		this.split_spaces_by_sensor = split_spaces_by_sensor;
		this.split_spaces_by_sensor_and_unit = split_spaces_by_sensor_and_unit;
		
	}
	
	
	public SpaceQueryDescriptor(ArrayList<Integer> spectrum_ids, Boolean split_spaces_by_sensor, Boolean split_spaces_by_sensor_and_unit, String order_by) {
		
		this(spectrum_ids, split_spaces_by_sensor, split_spaces_by_sensor_and_unit);
		this.order_by = order_by;
	
	}

}
