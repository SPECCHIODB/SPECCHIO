package ch.specchio.types;

//import javax.xml.bind.annotation.XmlElement;
//import javax.xml.bind.annotation.XmlRootElement;

//@XmlRootElement(name="spatial_pos")
public class spatial_pos
{
	public Double latitude = new Double(0);
	public Double longitude = new Double(0);
	public Double altitude = new Double(0);
	public String location_name = "";
	
	public spatial_pos()
	{
		
	}

//	@XmlElement(name="latitude")
//	public Double getLatitude() {
//		return latitude;
//	}

//	public void setLatitude(Double latitude) {
//		this.latitude = latitude;
//	}
//
////	@XmlElement(name="longitude")
//	public Double getLongitude() {
//		return longitude;
//	}
//
//	public void setLongitude(Double longitude) {
//		this.longitude = longitude;
//	}
//
////	@XmlElement(name="altitude")
//	public Double getAltitude() {
//		return altitude;
//	}
//
//	public void setAltitude(Double altitude) {
//		this.altitude = altitude;
//	}
//
////	@XmlElement(name="location_name")
//	public String getLocation_name() {
//		return location_name;
//	}
//
//	public void setLocation_name(String location_name) {
//		this.location_name = location_name;
//	}
	
	
}