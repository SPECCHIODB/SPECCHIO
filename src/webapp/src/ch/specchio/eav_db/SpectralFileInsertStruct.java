package ch.specchio.eav_db;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;

import ch.specchio.types.Metadata;
import ch.specchio.types.SpecchioMessage;


public class SpectralFileInsertStruct {
	
	
	private ArrayList<SpecchioMessage> errors;
	private ArrayList<Boolean> added_new_instruments;
	
	public id_and_op_struct hierarchy_id_and_op;
	public id_and_op_struct campaign_id_and_op;
	public String sensor_id;
	public Object file_format_id;
	public String instrument_id;
	public String calibration_id;
	public String measurement_unit_id;
	public String measurement_as_hex;
	public String storage_format; // default is zero, UJMP storage is 1
	public Metadata metadata;
	
	
	/** default constructor */
	public SpectralFileInsertStruct()
	{
		errors = new ArrayList<SpecchioMessage>();
		added_new_instruments = new ArrayList<Boolean>();
	}	
	
	@XmlElement(name="errors")
	public ArrayList<SpecchioMessage> getErrors() {
		return errors;
	}
	public void setErrors(ArrayList<SpecchioMessage> errors) {
		this.errors = errors;
	}
	public void addError(SpecchioMessage error) {
		errors.add(error);
	}
	public void addErrors(ArrayList<SpecchioMessage> errors) {
		this.errors.addAll(errors);
	}		
	
	@XmlElement(name="added_new_instrument")
	public ArrayList<Boolean> getAdded_new_instrument() {
		return added_new_instruments;
	}

	public void setAdded_new_instrument(ArrayList<Boolean> added_new_instrument) {
		this.added_new_instruments = added_new_instrument;
	}
	
	public void addAdded_new_instrument(Boolean added_new_instrument) {
		this.added_new_instruments.add(added_new_instrument);
	}	
	
	

}
