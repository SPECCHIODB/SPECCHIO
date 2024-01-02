package ch.specchio.spaces;

import javax.xml.bind.annotation.*;


@XmlRootElement(name="measurement_unit")
public class MeasurementUnit {
	
	int unit_id;
	int unit_no; // as in database
	String unit_name;
	
	public final static int Reflectance = 1;
	public final static int Radiance = 2;
	public final static int Irradiance = 4;
	public final static int Absorbance = 8;
	public final static int Transmittance = 6;
	public final static int DN = 0;
	public final static int Wavelength = 100;
	public final static int DN_div_Radiance = 101;
	public final static int AOT = 102;
	
	public MeasurementUnit()
	{
		setUnitId(0);
	}

	
	public MeasurementUnit(int unit_id)
	{
		setUnitId(unit_id);
	}
	
	public MeasurementUnit(String name)
	{
		setUnitName(name);
	}
	
	
	@XmlElement(name="unit_id")
	public int getUnitId() { return this.unit_id; }
	public void setUnitId(int unit_id)
	{
		if (unit_id == 0) {
			this.unit_no = -1;
			this.unit_name = "unknown";
		} else {
			this.unit_id = unit_id;
		}
	}
	
	@XmlElement(name="unit_name")
	public String getUnitName() { return this.unit_name; }
	public void setUnitName(String unit_name) { this.unit_name = unit_name; }
	
	@XmlElement(name="unit_no")
	public int getUnitNumber() { return this.unit_no; }
	public void setUnitNumber(int unit_no) { this.unit_no = unit_no; }
}
