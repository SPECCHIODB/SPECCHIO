package ch.specchio.types;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="uncertainty_source_pair")
public class UncertaintySourcePair {

	@XmlElement public int source_id;
	@XmlElement public String source_link_description;
	
	public UncertaintySourcePair() {
		
	}

	public UncertaintySourcePair(int source_id, String source_link_description) {
		
		this.source_id = source_id;
		this.source_link_description = source_link_description;
		
	}
	
	@XmlElement(name="source_id")
	public int getSourceId() { return this.source_id; }
	public void setSourceId(int source_id) { this.source_id = source_id; }
	
	@XmlElement(name="source_link_description")
	public String getSourceLinkDescription() { return this.source_link_description; }
	public void setSourceLinkDescription(String source_link_description) { this.source_link_description = source_link_description; }
	
	
	
}

