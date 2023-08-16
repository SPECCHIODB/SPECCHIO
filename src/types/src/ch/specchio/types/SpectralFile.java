package ch.specchio.types;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.ListIterator;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import ch.specchio.jaxb.XmlDateTimeAdapter;
import org.ujmp.core.Matrix;

import static java.lang.Math.max;
import static org.ujmp.core.util.SerializationUtil.*;


/**
 * Class representing a spectral file.
 */
@XmlRootElement(name="spectral_file")
//@XmlRootElement
//@XmlSeeAlso({DefaultDenseDoubleMatrixMultiD.class})
public class SpectralFile {
	
	// static file error code definitions
	public static int NO_ERRORS = 0;
	public static int RECOVERABLE_ERROR = 1;
	public static int UNRECOVERABLE_ERROR = 2;
	
	
	// static measurand designators
	public static int UNSPECIFIED = 0;
	public static int REFERENCE = 1;
	public static int TARGET = 2;
	
	// these three fields are used by the "insert spectral file" web service
	private String campaign_type = "specchio";
	private int campaign_id;
	private int hierarchy_id;

	private String file_format_name;
	private String company;
	private String comment;
	private ArrayList<DateTime> capture_dates = new ArrayList<>();
	private DateTime capture_date; // in case there is a single one and we do not
								// know the number of spectra yet ...
	private ArrayList<spatial_pos> pos = new ArrayList<>();
	private spatial_pos single_pos;
	private ArrayList<Integer> no_of_channels = new ArrayList<>();
	private int no_of_spectra; // can be more than one for e.g. ENVI spectral
									// libraries
	private int data_type; // for datatypes see
							// http://www.brockmann-consult.de/beam/doc/help/BeamDimapFormat.html
	private String interleave;
	private int byte_order; // little (0) or big endian (1)
	private boolean is_calibration_file = false; // e.g. available for ASD: ILL, REF and RAW files
	private boolean has_standardised_wavelengths = false; // this is true for e.g. the ASDs where all instruments are resampled to same wavelengths
	private int foreoptic_degrees; // e.g. available for ASD
	private int calibration_series = -1; // e.g. available for ASD
	private DateTime calibration_date = null; // e.g. available for the FLoX
	private String instrument_number = null; // e.g. available for ASD
	private int instrument_type_number = -1; // e.g. available for ASD
	private ArrayList<Integer> measurement_units  = new ArrayList<>(); // a number code that defines: refl, rad,
									// raw (acc. ASD specification)
	private ArrayList<Integer> measurement_types  = new ArrayList<>(); // CASE number according to Nicodemus and
									// Schaepman-Strub -> beam geometries
	private ArrayList<Integer> measurand_designator  = new ArrayList<>(); // internal designator for Reference panel, Target and others in future
	private ArrayList<Float> sensor_azimuth = new ArrayList<>();
	private ArrayList<Float> sensor_zenith = new ArrayList<>();
	private ArrayList<Float> illumination_azimuth = new ArrayList<>();
	private ArrayList<Float> illumination_zenith = new ArrayList<>();
	private int[] measurement_id;
	private String[] author;
	private String campaign_name;
	private String instrument_name;
	private String instrument_extended_name;
	private String[] instrument_parameters;
	private String light_source;
	private String light_source_param;
	private String location;
	private String weather;
	private String white_reference_target;
	private String processing;
	private String sampling_environment;
	private String[] processing_list;
	private String raw_data_format;
	private String source_file;
	private String target_description;
	private String[] keywords;
	private Float[] arm_length;
	private String azimuth_sensor_type;
	private String[] polarization;
	private Double[] polarization_dir;
	private String[] spectrum_type;
	private String fgi_file_loader_comment;
	private ArrayList<Metadata> specchio_eav_metadata = new ArrayList<>();

	// fields necessary for specpr files
	private String[] siderial_time;
	private int[] atm_thickness;
	private int[] no_of_ind_spectral_scans;
	private int[] rec_number;
	private int[] text_record_pointer;
	private String[] autom_history;
	private String[] man_history;
	private int[] no_of_runs;
	private int[] phase_angle;
	private Float[] temperature;
	private boolean[] is_spectra;
	private boolean is_specpr_file = false;
	private int[] specpr_ext_number;
	
	// fields for UniSpec and maybe others
	private String capturing_software_name;
	private String capturing_software_version;
	
	private boolean parent_garbage_flag;

	private Float[][] measurements; // first dimension (rows): spectra

	// https://iliachemodanov.ru/en/blog-en/10-java/jaxb/36-jaxb-and-interfaces-en: whatever; solving the interface problem via adapters was unsuccessful.
//	@XmlElements({
//			@XmlElement(type = DefaultDenseDoubleMatrixMultiD.class),
//			@XmlElement(type = Annotation.class)
//	})

//	@XmlJavaTypeAdapter(XmlMatrixAdapter.class)


	//	@XmlTransient
	private ArrayList<Matrix> measurement_matrices = new ArrayList<>();
	private ArrayList<String> measurement_matrices_serialised = new ArrayList<String>();
	private boolean UJMP_storage;
//	@XmlTransient private   measurement_matrix; // UJMP version for new default storage model

	private ArrayList<Float[]> wvls = new ArrayList<>();
	private String path;
	protected String filename;
	private String base_name; // filename without the extension
	private String ext; // extension of the filename
	private ArrayList<String> spectra_filenames= new ArrayList<>(); // for single spectrum files this is
										// ditto to the file_name
	// for multiple spectra files this is an autoconstructed name
	// of the file_name including a sequential number
	private ArrayList<String> spectra_names= new ArrayList<>();
	private String spectrum_name_type;

	private int number_of_spectra_names;

	private String[] FGI_HDRF_BRF_Flag;

	private boolean FGI_hdrf_brf_combined;

	private String FGI_datastructure_version;

	private String[][] FGI_calculation_processes;

	private int spectral_grouping_size = 1; // e.g. I/Q/U vector groups would be
											// 3

	private boolean shared_metadata_for_multiple_spectra = false;

	private String[] fgi_comments;

	private String weather_comment;

	private String fgi_optics;

	private boolean asd_v7;

	private Float[][] dn;
	
	// ASD specific calibration data
	private Float[] base_calibration_data;
	private Float[] lamp_calibration_data;
	private Float[] fibre_optic_data;	

	private boolean asd_v7_radiance_flag;
	private boolean asd_v7_reflectance_flag;
	private boolean create_unit_folder_for_asd_old_files;
	private boolean create_DN_folder_for_asd_files;
	
	private  ArrayList<SpecchioMessage> file_errors = new ArrayList<>();
	private int file_error_code;
	
	
	// data insert speedup fields
	private ArrayList<ArrayList<Integer>> redundancy_reduced_metaparameter_index_per_spectrum;
	private boolean use_shared_loading_time = true; // uses same loading time for all spectra in file
	private DateTime loading_time = null;
	private ArrayList<MetaParameter> unique_metaparameters;

	// ENVI Hdr fields
	private String fileType;
	private int across_track_dim;
	private int along_track_dim;

	//@XmlElement
	//@XmlJavaTypeAdapter(XmlMatrixAdapter.class)

	//@XmlAnyElement
	//@XmlTransient private Matrix measurement_matrix;

	public SpectralFile() {
		
	}
	
	public SpectralFile(SpectralFile spec_file) {
		// special constructor to create a lightweigth clone for checking the file existance
		this.setFileFormatName(spec_file.getFileFormatName());
		this.setCompany(spec_file.getCompany());
		this.setInstrumentNumber(spec_file.getInstrumentNumber());
		this.setInstrumentTypeNumber(spec_file.getInstrumentTypeNumber());
		this.setNumberOfSpectra(spec_file.getNumberOfSpectra());
		this.setCapturingSoftwareName(spec_file.getCapturingSoftwareName());
		this.FGI_hdrf_brf_combined = spec_file.isFgiHdrfBrfCombined();
		this.setAsdV7(spec_file.getAsdV7());
		this.setAsdV7RadianceFlag(spec_file.getAsdV7RadianceFlag());
		this.setAsdV7ReflectanceFlag(spec_file.getAsdV7ReflectanceFlag());		
		this.setFilename(spec_file.getFilename());
		for (String filename : spec_file.getSpectraFilenames()) {
			this.spectra_filenames.add(filename);
		}
		this.setFileErrorCode(SpectralFile.NO_ERRORS);
		this.setMeasurementUnits(spec_file.getMeasurementUnits());
		this.setCreateUnitFolderForasdOldFiles(spec_file.getCreateUnitFolderForasdOldFiles());
		this.setCreate_DN_folder_for_asd_files(spec_file.getCreate_DN_folder_for_asd_files());
	}

	@XmlElement(name="arm_length")
	public Float[] getArmLengths() { return this.arm_length; }
	public void setArmLengths(Float[] arm_length) { this.arm_length = arm_length; }
	public Float getArmLength(int i) { return this.arm_length[i]; }
	public void setArmLength(int i, Float arm_length) { this.arm_length[i] = arm_length; }
	
	@XmlElement(name="asd_v7")
	public boolean getAsdV7() { return this.asd_v7; }
	public void setAsdV7(boolean asd_v7) { this.asd_v7 = asd_v7; }
	
	@XmlElement(name="asd_v7_radiance_flag")
	public boolean getAsdV7RadianceFlag() { return this.asd_v7_radiance_flag; }
	public void setAsdV7RadianceFlag(boolean asd_v7_radiance_flag) { this.asd_v7_radiance_flag = asd_v7_radiance_flag; }
	
	@XmlElement(name="asd_v7_reflectance_flag")
	public boolean getAsdV7ReflectanceFlag() { return this.asd_v7_reflectance_flag; }
	public void setAsdV7ReflectanceFlag(boolean asd_v7_reflectance_flag) { this.asd_v7_reflectance_flag = asd_v7_reflectance_flag; }
	
	@XmlElement(name="atm_thickness")
	public int[] getAtmThicknesses() { return this.atm_thickness; }
	public void setAtmThicknesses(int[] atm_thickness) { this.atm_thickness = atm_thickness; }
	public int getAtmThickness(int i) { return this.atm_thickness[i]; }
	public void setAtmThickness(int i, int atm_thickness) { this.atm_thickness[i] = atm_thickness; }
	
	@XmlElement(name="author")
	public String[] getAuthors() { return this.author; }
	public void setAuthors(String[] author) { this.author = author; }
	public String getAuthor(int i) { return this.author[i]; }
	public void setAuthor(int i, String author) { this.author[i] = author; }
	
	@XmlElement(name="autom_history")
	public String[] getAutomaticHistories() { return this.autom_history; }
	public void setAutomaticHistories(String[] autom_history) { this.autom_history = autom_history; }
	public String getAutomaticHistory(int i) { return this.autom_history[i]; }
	public void setAutomaticHistory(int i, String autom_history) { this.autom_history[i] = autom_history; }
	
	@XmlElement(name="azimuth_sensor_type")
	public String getAzimuthSensorType() { return this.azimuth_sensor_type; }
	public void setAzimuthSensorType(String azimuth_sensor_type) { this.azimuth_sensor_type = azimuth_sensor_type; }
	
	@XmlElement(name="basename")
	public String getBasename() { return this.base_name; }
	public void setBasename(String base_name) { this.base_name = base_name; }
	
	@XmlElement(name="byte_order")
	public int getByteOrder() { return this.byte_order; }
	public void setByteOrder(int byte_order) { this.byte_order = byte_order; }
	
	@XmlElement(name="calibration_series")
	public int getCalibrationSeries() { return this.calibration_series; }
	public void setCalibrationSeries(int calibration_series) { this.calibration_series = calibration_series; }
	
	@XmlElement(name="campaign_id")
	public int getCampaignId() { return this.campaign_id; }
	public void setCampaignId(int campaign_id) { this.campaign_id = campaign_id; }
	
	@XmlElement(name="campaign_name")
	public String getCampaignName() { return this.campaign_name; }
	public void setCampaignName(String campaign_name) { this.campaign_name = campaign_name; }
	
	@XmlElement(name="campaign_type")
	public String getCampaignType() { return this.campaign_type; }
	public void setCampaignType(String campaign_type) { this.campaign_type = campaign_type; }
	
	@XmlElement(name="capture_date")
	@XmlJavaTypeAdapter(XmlDateTimeAdapter.class)
	public DateTime getCaptureDate() { return this.capture_date; }
	public void setCaptureDate(DateTime capture_date) { this.capture_date = capture_date; }
	
	@XmlElement(name="capture_dates")
	@XmlJavaTypeAdapter(XmlDateTimeAdapter.class)
	public ArrayList<DateTime> getCaptureDates() { return this.capture_dates; }
	public void setCaptureDates(ArrayList<DateTime> capture_dates) { this.capture_dates = capture_dates; }
	public DateTime getCaptureDate(int i) { 
		if (this.capture_dates.size() < i+1)
			return null;
		else
			return this.capture_dates.get(i);
		
		}
	public void setCaptureDate(int i, DateTime date) { capture_dates.add(i, date);  } //   if(ithis.capture_dates.set(i, date);
//	public DateTime getJodaCaptureDate(int i) { 
//		DateTimeFormatter formatter = DateTimeFormat.forPattern(MetaDate.DEFAULT_DATE_FORMAT);
//		formatter.withZoneUTC();
//		//return formatter.parseDateTime(this.capture_dates[i]); 
//		return this.capture_dates[i];
//	}
	
	@XmlElement(name="capturing_software_name")
	public String getCapturingSoftwareName() { return this.capturing_software_name; }
	public void setCapturingSoftwareName(String capturing_software_name) { this.capturing_software_name = capturing_software_name; }
	
	@XmlElement(name="capturing_software_version")
	public String getCapturingSoftwareVersion() { return this.capturing_software_version; }
	public void setCapturingSoftwareVersion(String capturing_software_version) { this.capturing_software_version = capturing_software_version; }	
	
	@XmlElement(name="create_unit_folder_for_asd_old_files")
	public boolean getCreateUnitFolderForasdOldFiles() { return this.create_unit_folder_for_asd_old_files; }
	public void setCreateUnitFolderForasdOldFiles(boolean create_unit_folder_for_asd_old_files) { this.create_unit_folder_for_asd_old_files = create_unit_folder_for_asd_old_files; }

	@XmlElement(name="create_DN_folder_for_asd_files")
	public boolean getCreate_DN_folder_for_asd_files() {	return create_DN_folder_for_asd_files;}
	public void setCreate_DN_folder_for_asd_files(boolean create_DN_folder_for_asd_files) {	this.create_DN_folder_for_asd_files = create_DN_folder_for_asd_files;}	
	
	@XmlElement(name="comment")
	public String getComment() { return this.comment; }
	public void setComment(String comment) { this.comment = comment; }
	
	@XmlElement(name="company")
	public String getCompany() { return this.company; }
	public void setCompany(String company) { this.company = company; }
	
	@XmlElement(name="data_type")
	public int getDataType() { return this.data_type; }
	public void setDataType(int data_type) { this.data_type = data_type; }
	
	@XmlElement(name="dn")
	public Float[][] getDn() { return this.dn; }
	public void setDn(Float[][] dn) { this.dn = dn; }
	
	@XmlElement(name="ext")
	public String getExt() { return this.ext; }
	public void setExt(String ext) { this.ext = ext; }
	
	@XmlElement(name="FGI_calculation_processes")
	public String[][] getFgiCalculationProcesses() { return this.FGI_calculation_processes; }
	public void setFgiCalculationProcesses(String FGI_calculation_processes[][]) { this.FGI_calculation_processes = FGI_calculation_processes; }
	public String[] getFgiCalculationProcess(int i) { return this.FGI_calculation_processes[i]; }
	public String getFgiCalculationProcess(int i, int j) { return this.FGI_calculation_processes[i][j]; }
	public void setFgiCalculationProcess(int i, String[] FGI_calculation_processes) { this.FGI_calculation_processes[i] = FGI_calculation_processes; }
	
	@XmlElement(name="fgi_comments")
	public String[] getFgiComments() { return this.fgi_comments; }
	public void setFgiComments(String fgi_comments[]) { this.fgi_comments = fgi_comments; }
	public String getFgiComment(int i) { return this.fgi_comments[i]; }
	public void setFgiComment(int i, String fgi_comments) { this.fgi_comments[i] = fgi_comments; }
	
	@XmlElement(name="FGI_datastructure_version")
	public String getFgiDataStructureVersion() { return this.FGI_datastructure_version; }
	public void setFgiDataStructureVersion(String FGI_datastructure_version) { this.FGI_datastructure_version = FGI_datastructure_version; }
	
	@XmlElement(name="fgi_file_loader_comment")
	public String getFgiFileLoaderComment() { return this.fgi_file_loader_comment; }
	public void setFgiFileLoaderComment(String fgi_file_loader_comment) { this.fgi_file_loader_comment = fgi_file_loader_comment; }
	
	@XmlElement(name="FGI_hdrf_brf_combined")
	public boolean isFgiHdrfBrfCombined() { return this.FGI_hdrf_brf_combined; }
	public void setFgiHdrfBrfCombined(boolean FGI_hdrf_brf_combined) { this.FGI_hdrf_brf_combined = FGI_hdrf_brf_combined; }
	
	@XmlElement(name="FGI_HDRF_BRF_Flag")
	public String[] getFgiHdrfBrfFlags() { return this.FGI_HDRF_BRF_Flag; }
	public void setFgiHdrfBrfFlags(String FGI_HDRF_BRF_Flag[]) { this.FGI_HDRF_BRF_Flag = FGI_HDRF_BRF_Flag; }
	public String getFgiHdrfBrfFlag(int i) { return this.FGI_HDRF_BRF_Flag[i]; }
	public void setFgiHdrfBrfFlag(int i, String FGI_HDRF_BRF_Flag) { this.FGI_HDRF_BRF_Flag[i] = FGI_HDRF_BRF_Flag; }
	
	@XmlElement(name="fgi_optics")
	public String getFgiOptics() { return this.fgi_optics; }
	public void setFgiOptics(String fgi_optics) { this.fgi_optics = fgi_optics; }
	
	@XmlElement(name="file_errors")
	public ArrayList<SpecchioMessage> getFileErrors() { return this.file_errors; }
	
	public ArrayList<SpecchioMessage> getFileErrors(boolean only_warnings_and_errors) { 
		
		ArrayList<SpecchioMessage> warnings_and_errors = new ArrayList<SpecchioMessage>();
		for (SpecchioMessage msg : file_errors) {
		  if (!msg.getType().equals(SpecchioMessage.INFO))
			  warnings_and_errors.add(msg);
		}
		
		return warnings_and_errors; 
		
	}
	
	public void setFileErrors(ArrayList<SpecchioMessage> file_errors) { this.file_errors = file_errors; }
	

		
	
	
	@XmlElement(name="file_format_name")
	public String getFileFormatName() { return this.file_format_name; }
	public void setFileFormatName(String file_format_name) { this.file_format_name = file_format_name; }
	
	@XmlElement(name="filename")
	public String getFilename() { return this.filename; }
	public void setFilename(String filename) { this.filename = filename; 
	
		// get base name and extension
		if (filename.contains(".")) {
			String[] tokens = filename.split("\\.");
			ext = tokens[tokens.length - 1]; // last element is the extension
	
			// filename without the extentsion
			base_name = filename.substring(0, filename.length()
					- (ext.length() + 1));
		} else { // if file has no extensions (i.e. specpr files)
			base_name = filename;
			ext = "";
		}	
	
	}
	
	@XmlElement(name="foreoptic_degrees")
	public int getForeopticDegrees() { return this.foreoptic_degrees; }
	public void setForeopticDegrees(int foreoptic_degrees) { this.foreoptic_degrees = foreoptic_degrees; }
	
	@XmlElement(name="garbage_indicator")
	public boolean getGarbageIndicator() { return this.parent_garbage_flag; }
	public void setGarbageIndicator(boolean is_garbage) { this.parent_garbage_flag = is_garbage; }
	
	@XmlElement(name="hierarchy_id")
	public int getHierarchyId() { return this.hierarchy_id; }
	public void setHierarchyId(int hierarchy_id) { this.hierarchy_id = hierarchy_id; }
	
	@XmlElement(name="illumination_azimuth")
	public ArrayList<Float> getIlluminationAzimuths() { return this.illumination_azimuth; }
	public void setIlluminationAzimuths(ArrayList<Float> illumination_azimuth) { this.illumination_azimuth = illumination_azimuth; }
	public Float getIlluminationAzimuth(int i) { return this.illumination_azimuth.get(i); }
	public void addIlluminationAzimuth(Float illumination_azimuth) { this.illumination_azimuth.add(illumination_azimuth); }
	public void addIlluminationAzimuth(int i, Float illumination_azimuth) { this.illumination_azimuth.add(i, illumination_azimuth); }
	
	@XmlElement(name="illumination_zenith")
	public ArrayList<Float> getIlluminationZeniths() { return this.illumination_zenith; }
	public void setIlluminationZeniths(ArrayList<Float> illumination_zenith) { this.illumination_zenith = illumination_zenith; }
	public Float getIlluminationZenith(int i) { return this.illumination_zenith.get(i); }
	public void addIlluminationZenith(Float illumination_zenith) { this.illumination_zenith.add(illumination_zenith); }
	public void addIlluminationZenith(int i, Float illumination_zenith) { this.illumination_zenith.add(i, illumination_zenith); }
	
	@XmlElement(name="instrument_extended_name")
	public String getInstrumentExtendedName() { return this.instrument_extended_name; }
	public void setInstrumentExtendedName(String instrument_extended_name) { this.instrument_extended_name = instrument_extended_name; }
	
	@XmlElement(name="instrument_name")
	public String getInstrumentName() { return this.instrument_name; }
	public void setInstrumentName(String instrument_name) { this.instrument_name = instrument_name; }
	
	@XmlElement(name="instrument_number")
	public String getInstrumentNumber() { return this.instrument_number; }
	public void setInstrumentNumber(String instrument_number) { this.instrument_number = instrument_number; }
	
	@XmlElement(name="instrument_parameters")
	public String[] getInstrumentParameters() { return this.instrument_parameters; }
	public void setInstrumentParameters(String[] instrument_parameters) { this.instrument_parameters = instrument_parameters; }
	public String getInstrumentParameters(int i) { return this.instrument_parameters[i]; }
	public void setInstrumentParameters(int i, String instrument_parameters) { this.instrument_parameters[i] = instrument_parameters; }
	
	@XmlElement(name="instrument_type_number")
	public int getInstrumentTypeNumber() { return this.instrument_type_number; }
	public void setInstrumentTypeNumber(int instrument_type_number) { this.instrument_type_number = instrument_type_number; }
	
	@XmlElement(name="interleave")
	public String getInterleave() { return this.interleave; }
	public void setInterleave(String interleave) { this.interleave = interleave; }
	
	@XmlElement(name="is_calibration_file")
	public boolean isCalibrationFile() { return this.is_calibration_file; }
	public void setCalibrationFile(boolean is_calibration_file) { this.is_calibration_file = is_calibration_file; }		
	
	@XmlElement(name="is_specpr_file")
	public boolean isSpecprFile() { return this.is_specpr_file; }
	public void setSpecprFile(boolean is_specpr_file) { this.is_specpr_file = is_specpr_file; }
	
	@XmlElement(name="is_spectra")
	public boolean[] getSpectraFlags() { return this.is_spectra; }
	public void setSpectraFlags(boolean[] is_spectra) { this.is_spectra = is_spectra; }
	public boolean isSpectrum(int i) { return this.is_spectra[i]; }
	public void setSpectrumFlag(int i, boolean is_spectra) { this.is_spectra[i] = is_spectra; }
	
	@XmlElement(name="keywords")
	public String[] getKeywords() { return this.keywords; }
	public void setKeywords(String[] keywords) { this.keywords = keywords; }
	public String getKeyword(int i) { return this.keywords[i]; }
	public void setKeyword(int i, String keywords) { this.keywords[i] = keywords; }
	
	@XmlElement(name="light_source")
	public String getLightSource() { return this.light_source; }
	public void setLightSource(String light_source) { this.light_source = light_source; }
	
	@XmlElement(name="light_source_param")
	public String getLightSourceParam() { return this.light_source_param; }
	public void setLightSourceParam(String light_source_param) { this.light_source_param = light_source_param; }
	
	@XmlElement(name="location")
	public String getLocation() { return this.location; }
	public void setLocation(String location) { this.location = location; }
	
	@XmlElement(name="man_history")
	public String[] getManualHistories() { return this.man_history; }
	public void setManualHistories(String[] man_history) { this.man_history = man_history; }
	public String getManualHistory(int i) { return this.man_history[i]; }
	public void setManualHistory(int i, String man_history) { this.man_history[i] = man_history; }
	
	@XmlElement(name="measurand_designator")
	public ArrayList<Integer> getMeasurandDesignators() { return this.measurand_designator; }
	public void setMeasurandDesignators(ArrayList<Integer> measurand_designator) { this.measurand_designator = measurand_designator; }
	public Integer getMeasurandDesignator(int i) { return (i < this.measurand_designator.size())? this.measurand_designator.get(i) : 0; }
	public void setMeasurandDesignator(int i, int measurand_designator) { this.measurand_designator.set(i, measurand_designator); }
	public void addMeasurandDesignator(int measurand_designator) { this.measurand_designator.add(measurand_designator); }
	public void addMeasurandDesignator(int i, int measurand_designator) { this.measurand_designator.add(i, measurand_designator); }
	
	
	@XmlElement(name="measurements")
	public Float[][] getMeasurements() { return this.measurements; }
	public void setMeasurements(Float[][] measurements) { this.measurements = measurements; }
	public Float[] getMeasurement(int i) { return this.measurements[i]; }
	public Float getMeasurement(int i, int j) { return this.measurements[i][j]; }
	public void setMeasurement(int i, Float[] measurements) { this.measurements[i] = measurements; }
	public void setMeasurement(int i, int j, Float measurements) { this.measurements[i][j] = measurements; }

	//@XmlElement(name="measurement_matrices")
	//@XmlTransient
	//@XmlElementRef
	//@XmlJavaTypeAdapter(XmlMatrixArrayListAdapter.class)
	public ArrayList<Matrix> getMeasurementMatrices() { return this.measurement_matrices; }
	//@XmlJavaTypeAdapter(XmlMatrixArrayListAdapter.class)
	//@XmlJavaTypeAdapter(XmlMatrixArrayListAdapter.class)

	@XmlTransient
	public void setMeasurementMatrices(ArrayList<Matrix> measurements) { this.measurement_matrices = measurements; }

	//@XmlTransient
	public Matrix getMeasurementMatrix(int i) { return this.measurement_matrices.get(i); }
	public void setMeasurementMatrix(int i, Matrix measurements) { this.measurement_matrices.set(i, measurements); }
	public void addMeasurementMatrix(Matrix measurements) { this.measurement_matrices.add(measurements); }

	@XmlElement(name="measurement_matrices_serialised")
	public ArrayList<String> getMeasurementMatricesSerialised() { return this.measurement_matrices_serialised; }
	public void setMeasurementMatricesSerialised(ArrayList<String> measurement_matrices_serialised) { this.measurement_matrices_serialised = measurement_matrices_serialised; }

	public void serialiseMatrices() {

		// prepare for JAXB transfer by serialising the UJMP matrices and later ignoring the Matrix objects in JAXB via @XmlTransient

		ListIterator<Matrix> li = this.measurement_matrices.listIterator();

		while(li.hasNext()) {
			try {
				// a trial, but this goes too far ...
				//				this.measurement_matrices_serialised.getList().add(new JAXBElement<byte[]>(new javax.xml.namespace.Qname("byte_arr"), byte[].class, serialize(li.next())));

				byte[] byte_arr = serialize(li.next());

				this.measurement_matrices_serialised.add(javax.xml.bind.DatatypeConverter.printHexBinary(byte_arr));
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	//@XmlJavaTypeAdapter(XmlMatrixAdapter.class)
	//@XmlTransient
//	public Matrix getMeasurementMatrix() { return this.measurement_matrix; }
//	@XmlJavaTypeAdapter(XmlMatrixAdapter.class)
	//@XmlTransient
//	public void setMeasurementMatrix(Matrix measurements) {  this.measurement_matrix = measurements; }



	@XmlElement(name="UJMP_storage")
	public boolean getIsUjmpStorage() { return this.UJMP_storage; }
	public void setIsUjmpStorage(boolean UJMP_storage) { this.UJMP_storage = UJMP_storage; }

	@XmlElement(name="measurement_id")
	public int[] getMeasurementIds() { return this.measurement_id; }
	public void setMeasurementIds(int measurement_id[]) { this.measurement_id = measurement_id; }
	public int getMeasurementId(int i) { return this.measurement_id[i]; }
	public void setMeasurementId(int i, int measurement_id) { this.measurement_id[i] = measurement_id; }
	
	@XmlElement(name="measurement_type")
	public ArrayList<Integer> getMeasurementTypes() { return this.measurement_types; }
	public void setMeasurementTypes(ArrayList<Integer> measurement_types) { this.measurement_types = measurement_types; }
	public int getMeasurementType(int i) { return this.measurement_types.get(i); }
	public void setMeasurementType(int i, int measurement_type) { this.measurement_types.set(i, measurement_type); }
	
	@XmlElement(name="measurement_units")
	public ArrayList<Integer> getMeasurementUnits() { return this.measurement_units; }
	public void setMeasurementUnits(ArrayList<Integer> measurement_units) { this.measurement_units = measurement_units; }
	public Integer getMeasurementUnits(int i) { return (i < this.measurement_units.size())? this.measurement_units.get(i) : 0; }
	public void setMeasurementUnits(int i, int measurement_unit) { this.measurement_units.set(i, measurement_unit); }
	public void addMeasurementUnits(int measurement_unit) { this.measurement_units.add(measurement_unit); }
	public void addMeasurementUnits(int i, int measurement_unit) { this.measurement_units.add(i, measurement_unit); }
	
	@XmlElement(name="no_of_channels")
	public ArrayList<Integer> getNumberOfChannels() { return this.no_of_channels; }
	public void setNumberOfChannels(ArrayList<Integer> no_of_channels) { this.no_of_channels = no_of_channels; }
	public Integer getNumberOfChannels(int i) {  if(this.measurements != null && measurements[i] != null){ return measurements[i].length;} else {return this.no_of_channels.get(i);} }
	public void addNumberOfChannels(int n) { this.no_of_channels.add(n); }
	
	@XmlElement(name="no_of_ind_spectral_scans")
	public int[] getNumberOfIndSpectralScans() { return this.no_of_ind_spectral_scans; }
	public void setNumberOfIndSpectralScans(int no_of_ind_spectral_scans[]) { this.no_of_ind_spectral_scans = no_of_ind_spectral_scans; }
	public int getNumberOfIndSpectralScans(int i) { return this.no_of_ind_spectral_scans[i]; }
	public void setNumberOfIndSpectralScans(int i, int no_of_ind_spectral_scans) { this.no_of_ind_spectral_scans[i] = no_of_ind_spectral_scans; }
	
	@XmlElement(name="no_of_runs")
	public int[] getNumberOfRuns() { return this.no_of_runs; }
	public void setNumberOfRuns(int no_of_runs[]) { this.no_of_runs = no_of_runs; }
	public int getNumberOfRuns(int i) { return this.no_of_runs[i]; }
	public void setNumberOfRuns(int i, int no_of_runs) { this.no_of_runs[i] = no_of_runs; }
	
	@XmlElement(name="no_of_spectra")
	public int getNumberOfSpectra() { return this.no_of_spectra; }
	public void setNumberOfSpectra(int no_of_spectra) {
		this.no_of_spectra = no_of_spectra;
	}

	public int getNumberOfMeasurements() {
		if(measurement_matrices_serialised.size() > 0 || measurement_matrices.size() > 0)
		{
			return max(measurement_matrices_serialised.size(), measurement_matrices.size());
		}
		else {
			return getNumberOfSpectra();
		}
	}
	
	@XmlElement(name="number_of_spectra_names")
	public int getNumberOfSpectraNames() { return this.number_of_spectra_names; }
	public void setNumberOfSpectraNames(int number_of_spectra_names) { this.number_of_spectra_names = number_of_spectra_names; }
	
	@XmlElement(name="path")
	public String getPath() { return this.path; }
	public void setPath(String path) { this.path = path; }
	
	@XmlElement(name="phase_angle")
	public int[] getPhaseAngles() { return this.phase_angle; }
	public void setPhaseAngles(int phase_angle[]) { this.phase_angle = phase_angle; }
	public int getPhaseAngle(int i) { return this.phase_angle[i]; }
	public void setPhaseAngle(int i, int phase_angle) { this.phase_angle[i] = phase_angle; }
	
	@XmlElement(name="polarization")
	public String[] getPolarizations() { return this.polarization; }
	public void setPolarizations(String[] polarization) { this.polarization = polarization; }
	public String getPolarization(int i) { return this.polarization[i]; }
	public void setPolarization(int i, String polarization) { this.polarization[i] = polarization; }
	
	@XmlElement(name="polarization_dir")
	public Double[] getPolarizationDirections() { return this.polarization_dir; }
	public void setPolarizationDirections(Double[] polarization_dir) { this.polarization_dir = polarization_dir; }
	public Double getPolarizationDirection(int i) { return this.polarization_dir[i]; }
	public void setPolarizationDirection(int i, Double polarization_dir) { this.polarization_dir[i] = polarization_dir; }
	
	@XmlElement(name="pos")
	public ArrayList<spatial_pos> getPos() { return (ArrayList<spatial_pos>) this.pos; }
	public void setPos(ArrayList<spatial_pos> pos) { this.pos=pos; }
	public spatial_pos getPos(int i) { return this.pos.get(i); }
	public void addPos(spatial_pos pos) { this.pos.add(pos); }
	public void addPos(int i, spatial_pos pos) { 
		
		// check if this index exists, otherwise add null values till index is reached
		while(this.pos.size() < i)
		{
			this.pos.add(null);
		}			
		
		this.pos.add(i, pos); 
		
	}
	
	@XmlElement(name="processing")
	public String getProcessing() { return this.processing; }
	public void setProcessing(String processing) { this.processing = processing; }
	
	@XmlElement(name="processing_list")
	public String[] getProcessingList() { return this.processing_list; }
	public void setProcessingList(String[] processing_list) { this.processing_list = processing_list; }
	public String getgetProcessingList(int i) { return this.processing_list[i]; }
	public void setgetProcessingList(int i, String processing_list) { this.processing_list[i] = processing_list; }
	
	@XmlElement(name="raw_data_format")
	public String getRawDataFormat() { return this.raw_data_format; } 
	public void setRawDataFormat(String raw_data_format) { this.raw_data_format = raw_data_format; }
	
	@XmlElement(name="rec_number")
	public int[] getRecordNumbers() { return this.rec_number; }
	public void setRecordNumbers(int rec_number[]) { this.rec_number = rec_number; }
	public int getRecordNumber(int i) { return this.rec_number[i]; }
	public void setRecordNumber(int i, int rec_number) { this.rec_number[i] = rec_number; }
	
	@XmlElement(name="sampling_environment")
	public String getSamplingEnvironment() { return this.sampling_environment; }
	public void setSamplingEnvironment(String sampling_environment) { this.sampling_environment = sampling_environment; }
	
	@XmlElement(name="sensor_azimuth")
	public ArrayList<Float> getSensorAzimuths() { return this.sensor_azimuth; }
	public void setSensorAzimuths(ArrayList<Float> sensor_azimuth) { this.sensor_azimuth = sensor_azimuth; }
	public Float getSensorAzimuth(int i) { return this.sensor_azimuth.get(i); }
	public void addSensorAzimuth(Float sensor_azimuth) { this.sensor_azimuth.add(sensor_azimuth); }
	public void addSensorAzimuth(int i, Float sensor_azimuth) { this.sensor_azimuth.add(i, sensor_azimuth); }
	
	@XmlElement(name="sensor_zenith")
	public ArrayList<Float> getSensorZeniths() { return this.sensor_zenith; }
	public void setSensorZeniths(ArrayList<Float> sensor_zenith) { this.sensor_zenith = sensor_zenith; }
	public Float getSensorZenith(int i) { return this.sensor_zenith.get(i); }
	public void addSensorZenith(Float sensor_zenith) { this.sensor_zenith.add(sensor_zenith); }
	public void addSensorZenith(int i, Float sensor_zenith) { this.sensor_zenith.add(i, sensor_zenith); }
	
	@XmlElement(name="shared_metadata_for_multiple_spectra")
	public boolean getSharedMetadataForMultipleSpectra() { return this.shared_metadata_for_multiple_spectra; }
	public void setSharedMetadataForMultipleSpectra(boolean shared_metadata_for_multiple_spectra) { this.shared_metadata_for_multiple_spectra = shared_metadata_for_multiple_spectra; }
	
	@XmlElement(name="siderial_time")
	public String[] getSiderialTimes() { return this.siderial_time; }
	public void setSiderialTimes(String[] siderial_time) { this.siderial_time = siderial_time; }
	public String getSiderialTime(int i) { return this.siderial_time[i]; }
	public void setSiderialTime(int i, String siderial_time) { this.siderial_time[i] = siderial_time; }
	
	@XmlElement(name="single_pos")
	public spatial_pos getSinglePos() { return this.single_pos; }
	public void setSinglePos(spatial_pos single_pos) { this.single_pos = single_pos; }
	
	@XmlElement(name="source_file")
	public String getSourceFile() { return this.source_file; }
	public void setSourceFile(String source_file) { this.source_file = source_file; }
	
	@XmlElement(name="specchio_eav_metadata")
	public ArrayList<Metadata> getEavMetadata() { return this.specchio_eav_metadata; }
	public void setEavMetadata(ArrayList<Metadata> specchio_eav_metadata) { this.specchio_eav_metadata = specchio_eav_metadata; }
	public void addEavMetadata(Metadata specchio_eav_metadata) { this.specchio_eav_metadata.add(specchio_eav_metadata); }
	public void addEavMetadata(int i, Metadata specchio_eav_metadata) { this.specchio_eav_metadata.add(i, specchio_eav_metadata); }
	public Metadata getEavMetadata(int i)
	{
		// create empty metadata objects for spectra that do not have any metadata
		while (i >= this.specchio_eav_metadata.size()) {
			this.specchio_eav_metadata.add(new Metadata());
		}	
		return this.specchio_eav_metadata.get(i);
	}
	
//	@XmlElement(name="specchio_eav_metadata_redundancy_groups")
//	public int[] getEavMetadataRedundancyGroups() { return this.specchio_eav_metadata_redundancy_groups; }
//	public void setEavMetadataRedundancyGroups(int[] specchio_eav_metadata_redundancy_groups) { this.specchio_eav_metadata_redundancy_groups = specchio_eav_metadata_redundancy_groups; }
//	public int getEavMetadataRedundancyGroup(int i) { return this.specchio_eav_metadata_redundancy_groups[i]; }
//	public void setEavMetadataRedundancyGroup(int i, int specchio_eav_metadata_redundancy_groups) { this.specchio_eav_metadata_redundancy_groups[i] = specchio_eav_metadata_redundancy_groups; }
	
	@XmlElement(name="specpr_ext_number")
	public int[] getSpecprExtNumbers() { return this.specpr_ext_number; }
	public void setSpecprExtNumbers(int[] specpr_ext_number) { this.specpr_ext_number = specpr_ext_number; }
	public int getSpecprExtNumber(int i) { return this.specpr_ext_number[i]; }
	public void setSpecprExtNumber(int i, int specpr_ext_number) { this.specpr_ext_number[i] = specpr_ext_number; }
	
	@XmlElement(name="spectra_filenames")
	public ArrayList<String> getSpectraFilenames() { return this.spectra_filenames; }
	public void setSpectraFilenames(ArrayList<String> spectra_filenames) { this.spectra_filenames = spectra_filenames; }
	public String getSpectrumFilename(int i) { return this.spectra_filenames.get(i); }
	public void addSpectrumFilename(String spectrum_filename) { this.spectra_filenames.add(spectrum_filename); }
	public void setSpectrumFilename(int i, String spectrum_filename) { this.spectra_filenames.set(i, spectrum_filename); }
	
	@XmlElement(name="spectral_grouping_size")
	public int getSpectralGroupingSize() { return this.spectral_grouping_size; }
	public void setSpectralGroupingSize(int spectral_grouping_size) { this.spectral_grouping_size = spectral_grouping_size; }
	
	@XmlElement(name="spectra_names")
	public ArrayList<String> getSpectraNames() { return this.spectra_names; }
	public void setSpectraNames(ArrayList<String> spectra_names) { this.spectra_names = spectra_names; }
	public String getSpectrumName(int i) { return this.spectra_names.get(i); }
	public void addSpectrumName(String spectrum_name) { this.spectra_names.add(spectrum_name); }
	
	@XmlElement(name="spectrum_name_type")
	public String getSpectrumNameType() { return this.spectrum_name_type; }
	public void setSpectrumNameType(String spectrum_name_type) { this.spectrum_name_type = spectrum_name_type; }
	
	@XmlElement(name="spectrum_type")
	public String[] getSpectrumTypes() { return this.spectrum_type; }
	public void setSpectrumTypes(String[] spectrum_type) { this.spectrum_type = spectrum_type; }
	public String getSpectrumType(int i) { return this.spectrum_type[i]; }
	public void setSpectrumType(int i, String spectrum_type) { this.spectrum_type[i] = spectrum_type; }
	
	@XmlElement(name="target_description")
	public String getTargetDescription() { return this.target_description; }
	public void setTargetDescription(String target_description) { this.target_description = target_description; }
	
	@XmlElement(name="temperature")
	public Float[] getTemperatures() { return this.temperature; }
	public void setTemperatures(Float[] temperature) { this.temperature = temperature; }
	public Float getTemperature(int i) { return this.temperature[i]; }
	public void setTemperature(int i, Float temperature) { this.temperature[i] = temperature; }
	
	@XmlElement(name="text_record_pointer")
	public int[] getTextRecordPointers() { return this.text_record_pointer; }
	public void setTextRecordPointers(int[] text_record_pointer) { this.text_record_pointer = text_record_pointer; }
	public int getTextRecordPointer(int i) { return this.text_record_pointer[i]; }
	public void setTextRecordPointer(int i, int text_record_pointer) { this.text_record_pointer[i] = text_record_pointer; }
	
	@XmlElement(name="weather")
	public String getWeather() { return this.weather; }
	public void setWeather(String weather) { this.weather = weather; }
	
	@XmlElement(name="weather_comment")
	public String getWeatherComment() { return this.weather_comment; }
	public void setWeatherComment(String weather_comment) { this.weather_comment = weather_comment; }
	
	@XmlElement(name="white_reference_target")
	public String getWhiteReferenceTarget() { return this.white_reference_target; }
	public void setWhiteReferenceTarget(String white_reference_target) { this.white_reference_target = white_reference_target; }
	
	@XmlElement(name="wvls")
	public ArrayList<Float[]> getWvls() { return this.wvls; }
	public void setWvls(ArrayList<Float[]> wvls) { this.wvls = wvls; }
	public Float[] getWvls(int i) { return (i < wvls.size())? this.wvls.get(i) : this.wvls.get(0); }
	public void setWvls(int i, Float[] wvls) { this.wvls.set(i, wvls); }
	public void addWvls(Float[] wvls) { this.wvls.add(wvls); }

	@XmlElement(name="across_track_dim")
	public int getAcross_track_dim() {
		return across_track_dim;
	}
	public void setAcross_track_dim(int across_track_dim) {
		this.across_track_dim = across_track_dim;
	}

	@XmlElement(name="along_track_dim")
	public int getAlong_track_dim() {
		return along_track_dim;
	}
	public void setAlong_track_dim(int along_track_dim) {
		this.along_track_dim = along_track_dim;
	}


	public double DDDmm2DDDdecimals(double in) {
		// reformat to dd.mmmmmmmmm
		int deg = (int) in / 100;
		double min = (in - deg * 100) / 60;

		return (deg + min);
	}
	
	public String escape_string(String comment) 
	{
		if (comment != null)
			return comment.replace("'", "''");
		else
			return null;
		
	}
	

	/**
	 * Write the floats into a ByteArrayOutputStream, store as byte array,
	 * then open as ByteArrayInputStream to use in an SQL statement
	 */
	public InputStream getInputStream(int spec_no) {
		byte[] temp_buf;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutput dos = new DataOutputStream(baos);

		if (is_specpr_file == true) {
			for (int i = 0; i < no_of_channels.get(spec_no); i++) {
				try {
					dos.writeFloat(measurements[spec_no][i]);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			
			for (int i = 0; i < measurements[spec_no].length; i++) {
				try {
					dos.writeFloat(measurements[spec_no][i]);
				} catch (IOException e) {
					e.printStackTrace();
				}
				catch (NullPointerException e){
					temp_buf = new byte[no_of_channels.get(0)];
					
					// log the error
					file_errors.add(new SpecchioMessage("Spectrum contains null values.", SpecchioMessage.ERROR));
					
					break;
				}
			}
		}

		temp_buf = baos.toByteArray();

		InputStream refl = new ByteArrayInputStream(temp_buf);

		return refl;
	}

	public int getFileErrorCode() {
		return file_error_code;
	}

	public void setFileErrorCode(int file_error_code) {
		this.file_error_code = file_error_code;
	}

	public Float[] getBase_calibration_data() {
		return base_calibration_data;
	}

	public void setBase_calibration_data(Float[] base_calibration_data) {
		this.base_calibration_data = base_calibration_data;
	}

	public Float[] getLamp_calibration_data() {
		return lamp_calibration_data;
	}

	public void setLamp_calibration_data(Float[] lamp_calibration_data) {
		this.lamp_calibration_data = lamp_calibration_data;
	}

	public Float[] getFibre_optic_data() {
		return fibre_optic_data;
	}

	public void setFibre_optic_data(Float[] fibre_optic_data) {
		this.fibre_optic_data = fibre_optic_data;
	}

	public int get_asd_instr_and_cal_fov_identifier() {
		// combined identifier including instrument number and calibration number and fov (currently only for ASD files)
		String tmp = this.getInstrumentNumber() + this.getCalibrationSeries() + this.getForeopticDegrees();
		
		return Integer.valueOf(tmp);
	}

	public boolean has_standardised_wavelengths() {
		return has_standardised_wavelengths;
	}

	public void setHas_standardised_wavelengths(boolean has_standardised_wavelengths) {
		this.has_standardised_wavelengths = has_standardised_wavelengths;
	}

	@XmlElement(name="calibration_date")
	@XmlJavaTypeAdapter(XmlDateTimeAdapter.class)
	public DateTime getCalibration_date() {
		return calibration_date;
	}
	public void setCalibration_date(DateTime calibration_date) {
		this.calibration_date = calibration_date;
	}
	
	
    public String measurementsToHex(int spec_no) {

        StringBuilder sb = new StringBuilder();

		if(measurement_matrices_serialised.size() > 0)
		{
			// UJMP storage
			// object stream method
//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
//			try {
//				serialize(measurement_matrices.get(spec_no), baos);
//				return javax.xml.bind.DatatypeConverter.printHexBinary(baos.toByteArray());
//
//			} catch (IOException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}

			return measurement_matrices_serialised.get(spec_no); // serialised data are already in HEX format

		}
        else {
			for (int i = 0; i < measurements[spec_no].length; i++) {
				try {

					sb.append(SpectralFile.hex(measurements[spec_no][i]));

				} catch (NullPointerException e) {

					// log the error
					file_errors.add(new SpecchioMessage("Spectrum contains null values.", SpecchioMessage.ERROR));

					break;
				}
			}

			return sb.toString();
		}


    }
    
    public static String hex(int n) {
        // call toUpperCase() if that's required
        return String.format("%8s", Integer.toHexString(n)).replace(' ', '0');
    }

    public static String hex(float f) {
        // change the float to raw integer bits(according to the OP's requirement)
        return hex(Float.floatToRawIntBits(f));
    }

	// using JAXB instead (https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java)
//	private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
//
//	public static String bytesToHex(byte[] bytes) {
//		char[] hexChars = new char[bytes.length * 2];
//		for (int j = 0; j < bytes.length; j++) {
//			int v = bytes[j] & 0xFF;
//			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
//			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
//		}
//		return new String(hexChars);
//	}

	public ArrayList<ArrayList<Integer>> getRedundancy_reduced_metaparameter_index_per_spectrum() {
		return redundancy_reduced_metaparameter_index_per_spectrum;
	}

	public void setRedundancy_reduced_metaparameter_index_per_spectrum(
			ArrayList<ArrayList<Integer>> redundancy_reduced_metaparameter_index_per_spectrum) {
		this.redundancy_reduced_metaparameter_index_per_spectrum = redundancy_reduced_metaparameter_index_per_spectrum;
	}

	public boolean isUse_shared_loading_time() {
		return use_shared_loading_time;
	}

	public void setUse_shared_loading_time(boolean use_shared_loading_time) {
		this.use_shared_loading_time = use_shared_loading_time;
	}

	public DateTime getLoading_time() {
		if(use_shared_loading_time && loading_time == null)
		{
			loading_time = new DateTime(DateTimeZone.UTC); // init loading time to be shared on inital call
		}
		else if(use_shared_loading_time == false)
		{
			loading_time = new DateTime(DateTimeZone.UTC);
		}
		
		return loading_time;
	}

	public void setLoading_time(DateTime loading_time) {
		this.loading_time = loading_time;
	}

	public void setUniqueMetaParameters(ArrayList<MetaParameter> unique_mps) {
		unique_metaparameters = unique_mps;		
	}    

	public ArrayList<MetaParameter> getUniqueMetaParameters() {
		return unique_metaparameters;		
	}


	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public String getFileType() {
		return fileType;
	}
}
