package ch.specchio.types;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="SpectralFileInsertResult")
public class SpectralFileInsertResult {
	
	private ArrayList<Integer> spectrum_ids;
	private ArrayList<SpecchioMessage> errors;
	private ArrayList<Boolean> added_new_instruments;
	
//	private ArrayList<SpectralFile> spectral_file_list;
//	private String campaign_type = "specchio";
//	private int campaign_id;		
	
	/** default constructor */
	public SpectralFileInsertResult()
	{
		spectrum_ids = new ArrayList<Integer>();
		errors = new ArrayList<SpecchioMessage>();
		added_new_instruments = new ArrayList<Boolean>();
	}

	@XmlElement(name="spectrum_ids")
	public ArrayList<Integer> getSpectrumIds() {
		return spectrum_ids;
	}
	public void setSpectrumIds(ArrayList<Integer> spectrum_ids) {
		this.spectrum_ids = spectrum_ids;
	}
	public void addSpectrumId(int id) {
		spectrum_ids.add(id);
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
	
	
	public void add(SpectralFileInsertResult result) {
		this.added_new_instruments.addAll(result.getAdded_new_instrument());
		spectrum_ids.addAll(result.getSpectrumIds());
		errors.addAll(result.getErrors());
	}	
	
	
	public ArrayList<SpecchioMessage> get_nonredudant_errors()
	{
		return get_nonredudant_errors(false);
	}
	
	
	public ArrayList<SpecchioMessage> get_nonredudant_errors(boolean only_warnings_and_errors)
	{
		ArrayList<SpecchioMessage> nonred_errors = new ArrayList<SpecchioMessage>();
		
		ArrayList<String> msgs = new ArrayList<String>();
		
		for (SpecchioMessage error : errors)
		{
			// add message if verbose level is INFO, or if it is a warning or error
			if(only_warnings_and_errors == false || (only_warnings_and_errors && !error.getType().equals(SpecchioMessage.INFO)))
			{
			
				if (!msgs.contains(error.getMessage())) {
					nonred_errors.add(error);	
					msgs.add(error.getMessage());
				}
			}
		}
		
		return nonred_errors;
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
	
	public void addAdded_new_instruments(ArrayList<Boolean> new_instruments) {
		this.added_new_instruments.addAll(new_instruments);
	}	
	
	
	
	
//	@XmlElement(name="spectral_file_list")
//	public ArrayList<SpectralFile> getSpectral_file_list() {
//		return spectral_file_list;
//	}
//	public void setSpectral_file_list(ArrayList<SpectralFile> spectral_file_list) {
//		this.spectral_file_list = spectral_file_list;
//	}
//	
//	@XmlElement(name="campaign_type")
//	public String getCampaignType() { return this.campaign_type; }
//	public void setCampaignType(String campaign_type) { this.campaign_type = campaign_type; }
//
//	@XmlElement(name="campaign_id")
//	public int getCampaignId() { return this.campaign_id; }
//	public void setCampaignId(int campaign_id) { this.campaign_id = campaign_id; }
	
	

}
