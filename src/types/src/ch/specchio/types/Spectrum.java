package ch.specchio.types;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.xml.bind.annotation.*;


/**
 * A single spectrum.
 */
@XmlRootElement(name="spectrum")
@XmlSeeAlso({ArrayList.class,SerialisableBufferedImage.class})
public class Spectrum implements MetadataInterface
{
	public static final String INSTRUMENT = "instrument";
	public static final String SENSOR = "sensor";
	public static final String CALIBRATION = "calibration"; // not yet used: calibrations do not have a useful name to be displayed
	public static final String MEASUREMENT_UNIT = "measurement_unit";
	public static final String FILE_FORMAT = "file_format";
	public static final String REFERENCE = "reference";
	
	/** spectrum metadata fields */
	public static final String[] METADATA_FIELDS = {
		SENSOR,
		FILE_FORMAT,
		INSTRUMENT, 
		MEASUREMENT_UNIT,
		REFERENCE,
		CALIBRATION
	};
	
//	MetaDatatype<Integer> number;
//	MetaDatatype<String> file_comment;
	MetaDatatype<String> file_format;
//	MetaDatatype<Date> capture_date;
//	MetaDatatype<Date> loading_date;
//	public MetaDatatype<String> file_name;
//	MetaDatatype<Integer> internal_average_cnt;
//	MetaDatatype<String> required_quality_level;
//	MetaDatatype<String> attained_quality_level;
//	MetaDatatype<Boolean> is_reference;
//	MetaDatatype<Double> latitude;
//	MetaDatatype<Double> longitude;
//	MetaDatatype<Double> altitude;
//	MetaDatatype<String> location_name;
//	MetaDatatype<String> campaign_name;
//	MetaDatatype<String> campaign_desc;
//	MetaDatatype<String> landcover;
//	MetaDatatype<String> cloud_cover;
//	MetaDatatype<String> amb_temperature;
//	MetaDatatype<String> air_pressure;
//	MetaDatatype<String> rel_humidity;
//	MetaDatatype<String> wind_direction;
//	MetaDatatype<String> wind_speed;
//	MetaDatatype<String> sensor_zenith;
//	MetaDatatype<String> sensor_azimuth;
//	MetaDatatype<String> illumination_zenith;
//	MetaDatatype<String> illumination_azimuth;
//	MetaDatatype<String> sensor_distance;
//	MetaDatatype<String> illumination_distance;
	MetaDatatype<String> measurement_unit;
//	MetaDatatype<String> measurement_type;
//	MetaDatatype<String> illumination_source;
//	MetaDatatype<String> sampling_environment;
//	MetaDatatype<String> foreoptic;
//
//	MetaDatatype<ArrayList<String>> spectrum_names;
//	MetaDatatype<ArrayList<String>> target_types;	
	
	
	Metadata smd;
	
	float[] spectrum;	
	
	MetaDatatype<ArrayList<Integer>> pictures;

	Float[] measurement_vector = null;
	
	public int spectrum_id;
	int position_id;
	public int sensor_id;
	public int campaign_id;
	int landcover_id;
	int environmental_condition_id;
	int sampling_geometry_id;
	public int measurement_unit_id;
	public int measurement_type_id; 
	int sampling_env_id;
	public int instrument_id;
	int quality_level_id;
	int required_quality_level_id;
	//int foreoptic_id;
	public int file_format_id;
	public int reference_id;
//	int goniometer_id;
	int hierarchy_level_id;
	public int calibration_id;
	
	private Sensor sensor;
	Instrument instrument;
	
	String capt_datetime = "";
	String load_datetime = "";	
	
	float stddev = -1;
	float mean;
	
	float vis_nir_stddev = -1;
	float vis_nir_mean;
	
	
	int report_attr_insert_pos;
	
	int output_timeformat;
	
	public Spectrum() {
		
	}
	
	
	public Spectrum(int spectrum_id) {
		
		this.spectrum_id = spectrum_id;
		
	}
	
//	@XmlElement(name="air_pressure")
//	public MetaDatatype<String> getAirPressure() { return this.air_pressure; }
//	public void setAirPressure(MetaDatatype<String> air_pressure) { this.air_pressure = air_pressure; }
//	
//	@XmlElement(name="altitude")
//	public MetaDatatype<Double> getAltitude() { return this.altitude; }
//	public void setAltitude(MetaDatatype<Double> altitude) { this.altitude = altitude; }
//	
//	@XmlElement(name="amb_temperature")
//	public MetaDatatype<String> getAmbientTemperature() { return this.amb_temperature; }
//	public void setAmbientTemperature(MetaDatatype<String> amb_temperature) { this.amb_temperature = amb_temperature; }
//	
//	@XmlElement(name="attained_quality_level")
//	public MetaDatatype<String> getAttainedQualityLevel() { return this.attained_quality_level; }
//	public void setAttainedQualityLevel(MetaDatatype<String> attained_quality_level) { this.attained_quality_level = attained_quality_level; }
//	
//	@XmlElement(name="campaign_desc")
//	public MetaDatatype<String> getCampaignDescription() { return this.campaign_desc; }
//	public void setCampaignDescription(MetaDatatype<String> campaign_desc) { this.campaign_desc = campaign_desc; }
	
	@XmlElement(name="campaign_id")
	public int getCampaignId() { return this.campaign_id; }
	public void setCampaignId(int campaign_id) { this.campaign_id = campaign_id; }
	
//	@XmlElement(name="campaign_name")
//	public MetaDatatype<String> getCampaignName() { return this.campaign_name; }
//	public void setCampaignName(MetaDatatype<String> campaign_name) { this.campaign_name = campaign_name; }
	
	@XmlElement(name="calibration_id")
	public int getCalibrationId() { return this.calibration_id; }
	public void setCalibrationId(int calibration_id) { this.calibration_id = calibration_id; }	
	
	public MetaDatatype<Integer> getCalibration() { return new MetaDatatype<Integer>("Calibration ID", calibration_id); }

	
//	@XmlElement(name="capture_date")
//	public MetaDatatype<Date> getCaptureDate() { return this.capture_date; }
//	public void setCaptureDate(MetaDatatype<Date> capture_date) { this.capture_date = capture_date; }
	
//	@XmlElement(name="capt_datetime")
//	public String getCaptureDateTime() { return this.capt_datetime; }
//	public void setCaptureDateTime(String capt_datetime) { this.capt_datetime = capt_datetime; }
	
//	@XmlElement(name="cloud_cover")
//	public MetaDatatype<String> getCloudCover() { return this.cloud_cover; }
//	public void setCloudCover(MetaDatatype<String> cloud_cover) { this.cloud_cover = cloud_cover; }
	
	@XmlElement(name="eav_metadata")
	public Metadata getEavMetadata() { return this.smd; }
	public void setEavMetadata(Metadata smd) { this.smd = smd; }
	
//	@XmlElement(name="environmental_condition_id")
//	public int getEnvironmentalConditionId() { return this.environmental_condition_id; }
//	public void setEnvironmentalConditionId(int environmental_condition_id) { this.environmental_condition_id = environmental_condition_id; }
	
//	@XmlElement(name="file_comment")
//	public MetaDatatype<String> getFileComment() { return this.file_comment; }
//	public void setFileComment(MetaDatatype<String> file_comment) { this.file_comment = file_comment; }

	@XmlElement(name="file_format")
	public MetaDatatype<String> getFileFormat() { return this.file_format; }
	public void setFileFormat(MetaDatatype<String> file_format) { this.file_format = file_format; }
	
	@XmlElement(name="file_format_id")
	public int getFileFormatId() { return this.file_format_id; }
	public void setFileFormatId(int file_format_id) { this.file_format_id = file_format_id; }

//	@XmlElement(name="file_name")
//	public MetaDatatype<String> getFileName() { return this.file_name; }
//	public void setFileName(MetaDatatype<String> file_name) { this.file_name = file_name; }
//	
//	@XmlElement(name="foreoptic")
//	public MetaDatatype<String> getForeoptic() { return this.foreoptic; }
//	public void setForeoptic(MetaDatatype<String> foreoptic) { this.foreoptic = foreoptic; }
	
//	@XmlElement(name="foreoptic_id")
//	public int getForeopticId() { return this.foreoptic_id; }
//	public void setForeopticId(int foreoptic_id) { this.foreoptic_id = foreoptic_id; }
	
//	@XmlElement(name="goniometer_id")
//	public int getGoniometerId() { return this.goniometer_id; }
//	public void setGoniometerId(int goniometer_id) { this.goniometer_id = goniometer_id; }
	
	@XmlElement(name="hierarchy_level_id")
	public int getHierarchyLevelId() { return this.hierarchy_level_id; }
	public void setHierarchyLevelId(int hierarchy_level_id) { this.hierarchy_level_id = hierarchy_level_id; }
	
//	@XmlElement(name="illumination_azimuth")
//	public MetaDatatype<String> getIlluminationAzimuth() { return this.illumination_azimuth; }
//	public void setIlluminationAzimuth(MetaDatatype<String> illumination_azimuth) { this.illumination_azimuth = illumination_azimuth; }
//	
//	@XmlElement(name="illumination_distance")
//	public MetaDatatype<String> getIlluminationDistance() { return this.illumination_distance; }
//	public void setIlluminationDistance(MetaDatatype<String> illumination_distance) { this.illumination_distance = illumination_distance; }
//	
//	@XmlElement(name="illumination_source")
//	public MetaDatatype<String> getIlluminationSource() { return this.illumination_source; }
//	public void setIlluminationSource(MetaDatatype<String> illumination_source) { this.illumination_source = illumination_source; }
//		
//	@XmlElement(name="illumination_zenith")
//	public MetaDatatype<String> getIlluminationZenith() { return this.illumination_zenith; }
//	public void setIlluminationZenith(MetaDatatype<String> illumination_zenith) { this.illumination_zenith = illumination_zenith; }
	
	@XmlElement(name="instrument")
	public Instrument getInstrument() { return this.instrument; }
	public void setInstrument(Instrument instrument) { this.instrument = instrument; }
	
	@XmlElement(name="instrument_id")
	public int getInstrumentId() { return this.instrument_id; }
	public void setInstrumentId(int instrument_id) { this.instrument_id = instrument_id; }
	
//	@XmlElement(name="internal_average_cnt")
//	public MetaDatatype<Integer> getInternalAverageCount() { return this.internal_average_cnt; }
//	public void setInternalAverageCount(MetaDatatype<Integer> internal_average_cnt) { this.internal_average_cnt = internal_average_cnt; }
//	
//	@XmlElement(name="is_reference")
//	public MetaDatatype<Boolean> getIsReference() { return this.is_reference; }
//	public void setIsReference(MetaDatatype<Boolean> is_reference) { this.is_reference = is_reference; }
//	
//	@XmlElement(name="landcover")
//	public MetaDatatype<String> getLandcover() { return this.landcover; }
//	public void setLandcover(MetaDatatype<String> landcover) { this.landcover = landcover; }
//	
//	@XmlElement(name="landcover_id")
//	public int getLandcoverId() { return this.landcover_id; }
//	public void setLandcoverId(int landcover_id) { this.landcover_id = landcover_id; }
//	
//	@XmlElement(name="latitude")
//	public MetaDatatype<Double> getLatitude() { return this.latitude; }
//	public void setLatitude(MetaDatatype<Double> latitude) { this.latitude = latitude; }
//	
//	@XmlElement(name="load_datetime")
//	public String getLoadDateTime() { return this.load_datetime; }
//	public void setLoadDateTime(String load_datetime) { this.load_datetime = load_datetime; }
//	
//	@XmlElement(name="loading_date")
//	public MetaDatatype<Date> getLoadingDate() { return this.loading_date; }
//	public void setLoadingDate(MetaDatatype<Date> loading_date) { this.loading_date = loading_date; }
//	
//	@XmlElement(name="location_name")
//	public MetaDatatype<String> getLocationName() { return this.location_name; }
//	public void setLocationName(MetaDatatype<String> location_name) { this.location_name = location_name; }
//	
//	@XmlElement(name="longitude")
//	public MetaDatatype<Double> getLongitude() { return this.longitude; }
//	public void setLongitude(MetaDatatype<Double> longitude) { this.longitude = longitude; }
	
	@XmlElement(name="measurement_vector")
	public Float[] getMeasurementVector() {
		return measurement_vector;
	}
	public void setMeasurementVector(Float[] measurement_vector) {
		this.measurement_vector = measurement_vector;
	}	
	
//	@XmlElement(name="measurement_type")
//	public MetaDatatype<String> getMeasurementType() { return this.measurement_type; }
//	public void setMeasurementType(MetaDatatype<String> measurement_type) { this.measurement_type = measurement_type; }
	
	@XmlElement(name="measurement_type_id")
	public int getMeasurementTypeId() { return this.measurement_type_id; }
	public void setMeasurementTypeId(int measurement_type_id) { this.measurement_type_id = measurement_type_id; }
	
	@XmlElement(name="measurement_unit")
	public MetaDatatype<String> getMeasurementUnit() { return this.measurement_unit; }
	public void setMeasurementUnit(MetaDatatype<String> measurement_unit) { this.measurement_unit = measurement_unit; }
	
	@XmlElement(name="measurement_unit_id")
	public int getMeasurementUnitId() { return this.measurement_unit_id; }
	public void setMeasurementUnitId(int measurement_unit_id) { this.measurement_unit_id = measurement_unit_id; }
	
//	@XmlElement(name="number")
//	public MetaDatatype<Integer> getNumber() { return this.number; }
//	public void setNumber(MetaDatatype<Integer> number) { this.number = number; }
	
	//@XmlElement(name="pictures")
//	public MetaDatatype<ArrayList<Integer>> getPictures() { return this.pictures; }
//	public void setPictures(MetaDatatype<ArrayList<Integer>> pictures) { this.pictures = pictures; }
//	
//	@XmlElement(name="position_id")
//	public int getPositionId() { return this.position_id; }
//	public void setPositionId(int position_id) { this.position_id = position_id; }
	
	@XmlElement(name="reference_id")
	public int getReferenceId() { return reference_id;}
	public void setReferenceId(int reference_id) { this.reference_id = reference_id;}


//	@XmlElement(name="rel_humidity")
//	public MetaDatatype<String> getRelativeHumidity() { return this.rel_humidity; }
//	public void setRelativeHumidity(MetaDatatype<String> rel_humidity) { this.rel_humidity = rel_humidity; }
//	
//	@XmlElement(name="required_quality_level")
//	public MetaDatatype<String> getRequiredQualityLevel() { return this.required_quality_level; }
//	public void setRequiredQualityLevel(MetaDatatype<String> required_quality_level) { this.required_quality_level = required_quality_level; }
	
//	@XmlElement(name="required_quality_level_id")
//	public int getRequiredQualityLevelId() { return this.required_quality_level_id; }
//	public void setRequiredQualityLevelId(int required_quality_level_id) { this.required_quality_level_id = required_quality_level_id; }
//	
//	@XmlElement(name="quality_level_id")
//	public int getQualityLevelId() { return this.quality_level_id; }
//	public void setQualityLevelId(int quality_level_id) { this.quality_level_id = quality_level_id; }
	
//	@XmlElement(name="sampling_environment")
//	public MetaDatatype<String> getSamplingEnvironment() { return this.sampling_environment; }
//	public void setSamplingEnvironment(MetaDatatype<String> sampling_environment) { this.sampling_environment = sampling_environment; }
	
	@XmlElement(name="sampling_env_id")
	public int getSamplingEnvironmentId() { return this.sampling_env_id; }
	public void setSamplingEnvironmentId(int sampling_env_id) { this.sampling_env_id = sampling_env_id; }
	
	@XmlElement(name="sampling_geometry_id")
	public int getSamplingGeometryId() { return this.sampling_geometry_id; }
	public void setSamplingGeometryId(int sampling_geometry_id) { this.sampling_geometry_id = sampling_geometry_id; }
	
	@XmlElement(name="sensor")
	public Sensor getSensor() { return this.sensor; }
	public void setSensor(Sensor sensor) { this.sensor = sensor; }
	
//	@XmlElement(name="sensor_azimuth")
//	public MetaDatatype<String> getSensorAzimuth() { return this.sensor_azimuth; }
//	public void setSensorAzimuth(MetaDatatype<String> sensor_azimuth) { this.sensor_azimuth = sensor_azimuth; }
//	
//	@XmlElement(name="sensor_distance")
//	public MetaDatatype<String> getSensorDistance() { return this.sensor_distance; }
//	public void setSensorDistance(MetaDatatype<String> sensor_distance) { this.sensor_distance = sensor_distance; }
//	
//	@XmlElement(name="sensor_zenith")
//	public MetaDatatype<String> getSensorZenith() { return this.sensor_zenith; }
//	public void setSensorZenith(MetaDatatype<String> sensor_zenith) { this.sensor_zenith = sensor_zenith; }
	
	@XmlElement(name="sensor_id")
	public int getSensorId() { return this.sensor_id; }
	public void setSensorId(int sensor_id) { this.sensor_id = sensor_id; }
	
	@XmlElement(name="smd")
	public Metadata getMetadata() { return this.smd; }
	public void setMetadata(Metadata smd) { this.smd = smd; }
	
	@XmlElement(name="spectrum_id")
	public int getSpectrumId() { return this.spectrum_id; }
	public void setSpectrumId(int spectrum_id) { this.spectrum_id = spectrum_id; }

//	//@XmlElement(name="spectrum_names")
//	public MetaDatatype<ArrayList<String>> getSpectrumNames() { return this.spectrum_names; }
//	public void setSpectrumNames(MetaDatatype<ArrayList<String>> spectrum_names) { this.spectrum_names = spectrum_names; }
//	
//	//@XmlElement(name="target_types")
//	public MetaDatatype<ArrayList<String>> getTargetTypes() { return this.target_types; }
//	public void setTargetTypes(MetaDatatype<ArrayList<String>> target_types) { this.target_types = target_types; }
//	
//	@XmlElement(name="wind_direction")
//	public MetaDatatype<String> getWindDirection() { return this.wind_direction; }
//	public void setWindDirection(MetaDatatype<String> wind_direction) { this.wind_direction = wind_direction; }
//	
//	@XmlElement(name="wind_speed")
//	public MetaDatatype<String> getWindSpeed() { return this.wind_speed; }
//	public void setWindSpeed(MetaDatatype<String> wind_speed) { this.wind_speed = wind_speed; }
	
	/**
	 * Convert an SQL-style field name into a Java-style getter or setter name
	 * 
	 * @param prefix	"get" or "set"
	 * @param fieldname	the field name
	 *
	 * @return a string consisting of the prefix, followed by the field name in camel case
	 */
	private String getAccessorName(String prefix, String fieldname) {
		
		// start with the prefix
		StringBuffer sbuf = new StringBuffer();
		sbuf.append(prefix);
		
		// split the fieldname on underscores
		String parts[] = fieldname.split("_");
		
		// convert each part into camel case
		for (String part : parts) {
			if (part.length() > 0) {
				sbuf.append(part.substring(0, 1).toUpperCase());
				if (part.length() > 1) {
					sbuf.append(part.substring(1));
				}
			}
		}
		
		return sbuf.toString();
		
	}
	
	
	/**
	 * Generic metadata identifier getter.
	 * 
	 * @param field	a field name from Spectrum.METADATA_FIELDS
	 * 
	 * @return the id assigned to this field
	 * 
	 * @throws NoSuchMethodException	the field does not exist
	 */
	public int getMetadataId(String field) throws NoSuchMethodException {
		
		Integer id = 0;
		
		try {
			
			// get getter method name
			Method getter = getClass().getMethod(getAccessorName("get", field) + "Id");
			
			// invoke the method
			id = (Integer)getter.invoke(this);
			
		} catch (IllegalArgumentException ex) {
			// should never happen because we asked for the correct method parameters
			ex.printStackTrace();
		} catch (IllegalAccessException ex) {
			// should never happen because getters are always public
			ex.printStackTrace();
		} catch (InvocationTargetException ex) {
			// should never happen because getters never throw exceptions
			ex.printStackTrace();
		} catch (SecurityException ex) {
			// should never happen because getters are always public
			ex.printStackTrace();
		}
		
		return id;
		
	}
	
	
	/**
	 * Generic metadata value getter.
	 * 
	 * @param field	a field name from Spectrum.METADATA_FIELDS
	 * 
	 * @return the value of this field, or null if it is not set
	 * 
	 * @throws NoSuchMethodException	the field does not exist
	 */
	public Object getMetadataValue(String field) throws NoSuchMethodException {
		
		Object value = null;
		
		try {
			
			// get the getter name
			Method getter = getClass().getMethod(getAccessorName("get", field));
			
			// invoke the method
			value = getter.invoke(this);
			
		} catch (IllegalArgumentException ex) {
			// should never happen because we asked for the correct method parameters
			ex.printStackTrace();
		} catch (IllegalAccessException ex) {
			// should never happen because getters are always public
			ex.printStackTrace();
		} catch (InvocationTargetException ex) {
			// should never happen because getters never throw exceptions
			ex.printStackTrace();
		} catch (SecurityException ex) {
			// should never happen because getters are always public
			ex.printStackTrace();
		}
		
		return value;
		
	}


	public int getId() {
		return this.spectrum_id;
	}
	
	
	/**
	 * Generic metadata identifier setter.
	 * 
	 * @param field	a field name from Spectrum.METADATA_FIELDS
	 * @param id	the id to assign to the field
	 * 
	 * @throws NoSuchMethodException	the field does not exist
	 */
	public void setMetadataId(String field, int id) throws NoSuchMethodException {

		try {
			
			// get setter method name
			Method setter = getClass().getMethod(getAccessorName("set", field) + "Id", Integer.TYPE);
			
			// invoke the method
			setter.invoke(this, new Integer(id));
			
		} catch (IllegalArgumentException ex) {
			// should never happen because we asked for the correct method parameters
			ex.printStackTrace();
		} catch (IllegalAccessException ex) {
			// should never happen because setters are always public
			ex.printStackTrace();
		} catch (InvocationTargetException ex) {
			// should never happen because setters never throw exceptions
			ex.printStackTrace();
		} catch (SecurityException ex) {
			// should never happen because setters are always public
			ex.printStackTrace();
		}
		
	}

	
//	public void set_output_timeformat(int format)
//	{
//		this.output_timeformat = format;
//		this.capture_date.set_timeformat(format);
//		this.loading_date.set_timeformat(format);
//	}
	
}



