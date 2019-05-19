package ch.specchio.types;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="MetaparameterStatistics")
public class MetaparameterStatistics {
	
	MetaParameter mp_min, mp_mean, mp_max;

	@XmlElement(name="mp_min")
	public MetaParameter getMp_min() {
		return mp_min;
	}

	public void setMp_min(MetaParameter mp_min) {
		this.mp_min = mp_min;
	}

	@XmlElement(name="mp_mean")
	public MetaParameter getMp_mean() {
		return mp_mean;
	}

	public void setMp_mean(MetaParameter mp_mean) {
		this.mp_mean = mp_mean;
	}

	@XmlElement(name="mp_max")
	public MetaParameter getMp_max() {
		return mp_max;
	}

	public void setMp_max(MetaParameter mp_max) {
		this.mp_max = mp_max;
	}
	
	

}
