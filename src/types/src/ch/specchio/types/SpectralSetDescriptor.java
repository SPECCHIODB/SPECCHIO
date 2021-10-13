package ch.specchio.types;

import java.util.ArrayList;

import javax.xml.bind.annotation.*;

/**
 * This class represents arraylists for spectralset and the spectralset itself, used for
 * requests to the insertUncertaintyNode service.
 */
@XmlRootElement(name="spectral_set_descriptor")

public class SpectralSetDescriptor {

	private ArrayList<UncertaintySourcePair> uc_source_pairs;
	private ArrayList<Integer> uc_source_ids;
	private ArrayList<Integer> uc_spectrum_ids;
	private SpectralSet spectral_set;
	

	/** default constructor */
	public SpectralSetDescriptor() {
		
		this.uc_source_pairs = new ArrayList<UncertaintySourcePair>(); 
		this.uc_source_ids = new ArrayList<Integer>();
		this.uc_spectrum_ids = new ArrayList<Integer>();
		this.spectral_set = new SpectralSet();
		
	}
	
	public SpectralSetDescriptor(ArrayList<UncertaintySourcePair> uc_source_pairs, ArrayList<Integer> uc_source_ids, ArrayList<Integer> uc_spectrum_ids, SpectralSet spectral_set) {
		
		this.uc_source_pairs = uc_source_pairs;
		this.uc_source_ids = uc_source_ids;
		this.uc_spectrum_ids = uc_spectrum_ids;
		this.spectral_set = spectral_set;
		
	}
	
	public SpectralSetDescriptor(ArrayList<Integer> uc_spectrum_ids, SpectralSet spectral_set) {
		
		this.uc_spectrum_ids = uc_spectrum_ids;
		this.spectral_set = spectral_set;
	
	}
	

	@XmlElement(name="uc_source_pairs")
	public ArrayList<UncertaintySourcePair> getUcSourcePairs() { return this.uc_source_pairs; }
	public void setUcSourcePairs(ArrayList<UncertaintySourcePair> uc_source_pairs) { this.uc_source_pairs = uc_source_pairs; }
	
	@XmlElement(name="uc_source_ids")
	public ArrayList<Integer> getUcSourceIds() { return this.uc_source_ids; }
	public void setUcSourceIds(ArrayList<Integer> uc_source_ids) { this.uc_source_ids = uc_source_ids; }
	
	@XmlElement(name="uc_spectrum_ids")
	public ArrayList<Integer> getUcSpectrumIds() { return this.uc_spectrum_ids; }
	public void setUcSpectrumIds(ArrayList<Integer> uc_spectrum_ids) { this.uc_spectrum_ids = uc_spectrum_ids; }
	
	@XmlElement(name="spectral_set")
	public SpectralSet getSpectralSet() { return this.spectral_set; }
	public void setSpectralSet(SpectralSet spectral_set) { this.spectral_set = spectral_set; }
	
}

