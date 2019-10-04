package ch.specchio.file.reader.campaign;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.ListIterator;

import org.joda.time.DateTime;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.file.reader.spectrum.*;
import ch.specchio.spaces.MeasurementUnit;
import ch.specchio.spaces.Space;
import ch.specchio.types.Calibration;
import ch.specchio.types.Instrument;
import ch.specchio.types.InstrumentDescriptor;
import ch.specchio.types.MetaDate;
import ch.specchio.types.MetaParameterFormatException;
import ch.specchio.types.MetaparameterStatistics;
import ch.specchio.types.SpecchioMessage;
import ch.specchio.types.SpectralFile;
import ch.specchio.types.SpectralFileInsertResult;
import ch.specchio.types.SpectralFiles;
import ch.specchio.types.Spectrum;
import ch.specchio.types.attribute;
import ch.specchio.types.hierarchy_node;

public class SpecchioCampaignDataLoader extends CampaignDataLoader {
	
	private SPECCHIOClient specchio_client;
	private SpectralFileLoader sfl;

	private int root_hierarchy_id = 0;
	private boolean load_existing_hierarchy = false;
	private boolean simple_delta_loading = true;
	
	ArrayList<String> file_errors = new ArrayList<String>();
	private int successful_file_counter;
	private int parsed_file_counter;
	


	private ArrayList<SpectralFileLoader> loaders_of_new_instruments = new ArrayList<SpectralFileLoader>();
	private File flox_rox_cal_file;
	private FileTime lastModifiedTime;
	private int files_with_null_sfl_cnt = 0;

	public SpecchioCampaignDataLoader(CampaignDataLoaderListener listener, SPECCHIOClient specchio_client) {
		super(listener);		
		this.specchio_client = specchio_client;
	}
	
	public SpecchioCampaignDataLoader(SPECCHIOClient specchio_client) {
		super(null);		
		this.specchio_client = specchio_client;
	}	

	// the actual code for loading a campaign
	public void run() {
		try {
			
			// clear EAV known metadata entries because some delete operation might have happened in the meantime
			specchio_client.clearMetaparameterRedundancyList();

			// tell the listener that we're about to begin
			if(listener != null)
				listener.campaignDataLoading();
			
			// update the campaign data on the server (only if a new root path is added; loading an existing hierarchy does not add a new path
			if(!load_existing_hierarchy) specchio_client.updateCampaign(campaign);

			// get the data path for this campaign
			File f = new File(campaign.getPath());

			// now we create the root hierarchy for this campaign (only needed if we are loading root nodes here)
			if(!load_existing_hierarchy) root_hierarchy_id = insert_hierarchy(f.getName(), 0);
			
			load_directory(root_hierarchy_id, f, false);
			
			// force a refresh of the client cache for potentially new sensors, instruments and calibrations
			// TODO: only refresh if new sensors, instruments and/or calibrations were actually inserted
			specchio_client.refreshMetadataCategory("sensor");
			specchio_client.refreshMetadataCategory("instrument");
			specchio_client.refreshMetadataCategory("calibration");
			
			// tell the listener that we're finished
			if(listener != null)
				listener.campaignDataLoaded(parsed_file_counter, successful_file_counter, spectrum_counter, files_with_null_sfl_cnt, this.file_errors, simple_delta_loading);

		}
		catch (SPECCHIOClientException ex) {
			if(listener != null)
				listener.campaignDataLoadException(ex.getMessage() + "\n" + ex.getDetails(), ex);
			else
				System.out.println(ex.getMessage() + "\n" + ex.getDetails());
		}
		catch (IOException ex) {
			if(listener != null)
				listener.campaignDataLoadException(ex.getMessage(), ex);
			else
				System.out.println(ex.getMessage());
		}
		
		// tell the listener that we're finished
		//listener.campaignDataLoaded(successful_file_counter, this.file_errors);

	}

	// Recursive method: if the parent_id is zero then it is the root directory
	// otherwise parent_id is the hierarchy_level_id of the parent directory
	// The dir is a File object that points to the directory on the
	// file system to be read
	void load_directory(int parent_id, File dir, boolean parent_garbage_flag) throws SPECCHIOClientException, IOException {
		int hierarchy_id = 0;
		ArrayList<File> files, directories;
		SpectralFile spec_file;
		boolean is_garbage = parent_garbage_flag;

		// Garbage detection: all data that are under a folder called 'Garbage' will get an EAV garbage flag
		// this allows users to load also suboptimal (i.e. garbage) data into the database, but easily exclude them from any selection
		if(dir.getName().equals("Garbage"))
		{
			is_garbage = true; // marks this directory as the one recognised as garbage
		}

		// get the names of all files in dirs in the current dir. NOTE: the order of the files is not guaranteed
		String[] whole_content = dir.list();
		if (whole_content == null) {
			if(load_existing_hierarchy)
				throw new FileNotFoundException("The directory " + dir.toString() + " does not exist.\n"+
						"Note that automatically generated folders, e.g. Reflectance, can not be selected as loading point.");
			else
				throw new FileNotFoundException("The campaign directory " + dir.toString() + " does not exist.");
		}

		// File filter
		// count files that we do not want
		// build content without unwanted files
		ArrayList<String> content = new ArrayList<String>();
		for (int i = 0; i < whole_content.length; i++) {
			// filter the dot files
			if (whole_content[i].startsWith(".", 0)) {
				System.out.println("Filtered .<file>");
			}
			else
			{
				content.add(whole_content[i]);
			}
		}


		// create array to store the File objects in
		directories = new ArrayList<File>();
		files = new ArrayList<File>();

		// get the number of files and subdirectories in the current
		// directory
		ListIterator<String> li = content.listIterator();
		while(li.hasNext()) {
			// here we construct the absolute pathname for each object in
			// the directory

			File f = new File(dir.toString() + File.separator + li.next());						

			if (f.isDirectory())
			{
				directories.add(f);
			}
			else
			{
				files.add(f);
				
				// FloX/RoX specific identification of cal files (for the RoX example this used to be cal.csv)
				if(f.getName().matches("CAL_.*_JB.*\\.csv"))
				{
					setFlox_rox_cal_file(f);
				}
				
			}
		}

		// if there are subdirs, call all of them (recursive call)
		// only call dirs, files are ignored
		ListIterator<File> dir_li = directories.listIterator();
		while(dir_li.hasNext()) 
		{
			File curr_dir = dir_li.next();

			// use the names of the first hierarchy (the one below
			// the root) to show in progress report
			if (parent_id == root_hierarchy_id && listener != null) {
				listener.campaignDataLoadOperation(curr_dir.getName());
			}

			// create a new entry in the database for this directory
			hierarchy_id = insert_hierarchy(curr_dir.getName(), parent_id);

			load_directory(hierarchy_id, curr_dir, parent_garbage_flag);
		}
		
		

		// load each file using the spectral
		// file loader
		if (files.size() > 0) {	


			// get modification date of this directory
			try {
				lastModifiedTime = Files.getLastModifiedTime(dir.toPath(), LinkOption.NOFOLLOW_LINKS);


			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new IOException("Could not read last-modified-time of campaign directory " + dir.toString());
			}
			
			// get spectrum_ids of the current hierarchy	
			//ArrayList<Integer> spectrum_ids = specchio_client.getDirectSpectrumIdsOfHierarchy(parent_id);
			
			hierarchy_node node = new hierarchy_node();
			node.setId(parent_id);
					
			List<Integer> spectrum_ids_ = specchio_client.getSpectrumIdsForNode(node);        
			
			ArrayList<Integer> spectrum_ids = new ArrayList<Integer>();
			spectrum_ids.addAll(spectrum_ids_);
			
			// get the latest insert time stamp of the spectral files contained in this directory
			MetaparameterStatistics stats = null;
			boolean new_data_available = true;
			try {
				if(spectrum_ids.size() > 0) // handling of yet empty hierarchies
				{
					stats = specchio_client.getMetaparameterStatistics(spectrum_ids, "Loading Time");

					// check if this folder was modified after the last load
					DateTime max_file_load_time = (DateTime) ((MetaDate) stats.getMp_max()).getValue();
					new_data_available = lastModifiedTime.toMillis() > max_file_load_time.getMillis();	
				}
			}
			catch (SPECCHIOClientException ex) {
				// ignore if this is caused by an old server version
				if(ex.getUserMessage().contains("get_metaparameter_statistics returned a response status of 404 Not Found"))
				{
					// ignore
				}
				else
					throw ex; // rethrow as this is another error
			}

			// get the spectral file loader needed for this directory
			// sfl = get_spectral_file_loader(files);


			if(simple_delta_loading == false || new_data_available)
			{

				ArrayList<SpectralFile> spectral_file_list = new ArrayList<SpectralFile>();		
				//ArrayList<SpectralFileLoader> spectral_file_loader_list = new ArrayList<SpectralFileLoader>();	
				Hashtable<String, SpectralFileLoader> spectral_file_loader_hash = new Hashtable<String, SpectralFileLoader>();

				// iterate over the files
				ListIterator<File> file_li = files.listIterator();

				while(file_li.hasNext()) {
					File file = file_li.next();

					ArrayList<File> this_file = new ArrayList<File>(); // overkill ... change to single object later ...

					this_file.add(file);



					sfl = get_spectral_file_loader(this_file);

					if (sfl != null) {

						try {

							// the loader can return null, e.g. if ENVI files are
							// read
							// and a body (*.slb) is passed.
							// In such a case no spectrum is inserted.
							spec_file = sfl.load(file);
							if (spec_file != null && spec_file.getNumberOfSpectra() > 0)
							{
								if(spec_file.getFileErrorCode() != SpectralFile.UNRECOVERABLE_ERROR)
								{
									spec_file.setGarbageIndicator(is_garbage);

									// add to file list
									spectral_file_list.add(spec_file);

									// keep a copy of the file loader for new ASD files to insert calibration data later on
									if(spec_file.getAsdV7())
									{
										Integer id = spec_file.get_asd_instr_and_cal_fov_identifier();
										if(!spectral_file_loader_hash.containsKey(id.toString()))
											spectral_file_loader_hash.put(id.toString(), sfl);

									}

									// keep a copy of the file loader for FLoX and RoX instruments
									if(sfl instanceof JB_FileLoader)
									{
										String instrument_name = spec_file.getInstrumentName();

										if(!spectral_file_loader_hash.containsKey(instrument_name))
											spectral_file_loader_hash.put(instrument_name, sfl);
									}

								}
								else
								{
									// serious error
									// add the message to the list of all errors
									// concatenate all errors into one message
									StringBuffer buf = new StringBuffer("Issues found in " + spec_file.getFilename() + ":");

									for (SpecchioMessage error : spec_file.getFileErrors()) {

										buf.append("\n\t");

										buf.append(error.toString());
									}

									buf.append("\n");

									// add the message to the list of all errors
									this.file_errors.add(buf.toString());								

								}
								file_counter++;

							}


							parsed_file_counter++;

							if(listener != null)
								listener.campaignDataLoadFileCount(file_counter, spectrum_counter);
						}
						catch (IOException ex) {
							if(listener != null)
								listener.campaignDataLoadError(file + ": " + ex.getMessage());
							else
								System.out.println(ex.getMessage());

						}					
						catch (MetaParameterFormatException ex) {
							if(listener != null)
								listener.campaignDataLoadError(file + ": " + ex.getMessage());
							else
								System.out.println(ex.getMessage());						
						}				

					}
					else
					{
						if(file != this.getFlox_rox_cal_file()) // only count files without parser if they are not one of the CAL files ...
							files_with_null_sfl_cnt ++;
					}
						
				}

				if (spectral_file_list.size() > 0)
				{

					// check existence of all spectral files
					SpectralFiles sfs = new SpectralFiles();

					ArrayList<SpectralFile> spectral_light_file_list = new ArrayList<SpectralFile>();

					// create lightweight objects
					ListIterator<SpectralFile> sf_li = spectral_file_list.listIterator();

					while(sf_li.hasNext()) {
						spec_file = sf_li.next();		
						SpectralFile light_clone = new SpectralFile(spec_file);
						light_clone.setHierarchyId(parent_id);
						light_clone.setCampaignId(campaign.getId());
						light_clone.setCampaignType(campaign.getType());	

						spectral_light_file_list.add(light_clone);
					}

					sfs.setSpectral_file_list(spectral_light_file_list);
					sfs.setCampaignId(campaign.getId());
					sfs.setCampaignType(campaign.getType());	

					boolean[] exists_array = specchio_client.spectralFilesExist(sfs);


					// insert spectral files

					sf_li = spectral_file_list.listIterator();

					int index = 0;

					while(sf_li.hasNext()) {
						spec_file = sf_li.next();		

						if (exists_array[index] == false)
						{

							SpectralFileInsertResult insert_result = new SpectralFileInsertResult();

							insert_result = insert_spectral_file(spec_file, parent_id);

							spectrum_counter += insert_result.getSpectrumIds().size();

							// check if there was a new instrument inserted and keep in new list for updates of calibration
							// can only do so for radiances
							if(spec_file.getAsdV7() && spec_file.getAsdV7RadianceFlag() && !insert_result.getAdded_new_instrument().isEmpty() && insert_result.getAdded_new_instrument().get(0)) // currently only for new ASD files, hence, always first entry
							{
								Integer id = spec_file.get_asd_instr_and_cal_fov_identifier();

								SpectralFileLoader loader = spectral_file_loader_hash.get(id.toString());
								loader.insert_result = insert_result;

								loaders_of_new_instruments.add(loader);
							}

							if(insert_result.getSpectrumIds().size() > 0) successful_file_counter++;

							insert_result.addErrors(spec_file.getFileErrors()); // compile into one list of errors							
							//				if(insert_result.getErrors().size() == 0) successful_file_counter++;

							// check on file errors
							if(insert_result.getErrors().size() > 0)
							{
								// concatenate all errors into one message
								StringBuffer buf = new StringBuffer("Issues found in " + spec_file.getFilename() + ":");

								for (SpecchioMessage error : insert_result.get_nonredudant_errors()) {

									buf.append("\n\t");

									buf.append(error.toString());
								}

								buf.append("\n");

								// add the message to the list of all errors
								this.file_errors.add(buf.toString());

							}	


							if(listener != null)
								listener.campaignDataLoadFileCount(file_counter, spectrum_counter);



						}

						index = index + 1;

					}

					// insert new instrument calibration factors if available: only ASD new binary file version till now ...
					//				if(loaders_of_new_instruments.size()>0)
					//				{
					//					for(SpectralFileLoader loader : loaders_of_new_instruments)
					//					{
					//						// get involved instrument id by getting the metadata of the inserted spectrum
					//						Spectrum s = specchio_client.getSpectrum(loader.insert_result.getSpectrumIds().get(0), false);
					//						
					//						ASD_FileFormat_V7_FileLoader loader_ = (ASD_FileFormat_V7_FileLoader) loader;
					//						
					//						// lamp
					//						Calibration c = new Calibration();																		
					//						c.setInstrumentId(s.getInstrumentId());
					//						c.setCalibrationNumber(loader_.getSpec_file().getCalibrationSeries());
					//						c.setName("LMP");
					//						c.setComments("Calibration Lamp Radiance");
					//						c.setMeasurement_unit_id(specchio_client.getMeasurementUnitFromCoding(MeasurementUnit.Radiance).getUnitId());
					//						
					//						double[] doubleArray = new double[loader_.getSpec_file().getNumberOfChannels(0)];
					//						for (int i = 0; i < loader_.getSpec_file().getNumberOfChannels(0); i++) {
					//						    doubleArray[i] = loader_.lamp_calibration_data[0][i];  // no casting needed
					//						}
					//						
					//						c.setFactors(doubleArray);						
					//						
					//						specchio_client.insertInstrumentCalibration(c);
					//						
					//						
					//						// reference panel
					//						c.setName("BSE");
					//						c.setComments("Calibration Reference Panel Reflectance");
					//						c.setMeasurement_unit_id(specchio_client.getMeasurementUnitFromCoding(MeasurementUnit.Reflectance).getUnitId());
					//						
					//						for (int i = 0; i < loader_.getSpec_file().getNumberOfChannels(0); i++) {
					//						    doubleArray[i] = loader_.base_calibration_data[0][i];  // no casting needed
					//						}
					//						
					//						c.setFactors(doubleArray);												
					//						specchio_client.insertInstrumentCalibration(c);
					//						
					//						// Digital Numbers obtained during calibration
					//						c.setName("DN");
					//						c.setComments("Instrument Digital Numbers during Calibration for FOV =" + loader_.getSpec_file().getForeopticDegrees() + " degrees");
					//						c.setMeasurement_unit_id(specchio_client.getMeasurementUnitFromCoding(MeasurementUnit.DN).getUnitId());
					//						
					//						for (int i = 0; i < loader_.getSpec_file().getNumberOfChannels(0); i++) {
					//						    doubleArray[i] = loader_.fibre_optic_data[0][i];  // no casting needed
					//						}
					//						
					//						c.setFactors(doubleArray);		
					//						c.setField_of_view(loader_.getSpec_file().getForeopticDegrees());
					//						specchio_client.insertInstrumentCalibration(c);
					//							
					//					}					
					//					
					//				}

					// check if new calibrations must be added for ASDs and other instruments that have gains included (like the FLoX)
					Enumeration<SpectralFileLoader> file_loaders = spectral_file_loader_hash.elements();
					while(file_loaders.hasMoreElements())
					{					
						SpectralFileLoader loader = file_loaders.nextElement();
						if(loader.getSpec_file().getAsdV7RadianceFlag()) // only for radiance data
							insert_instrument_calibration_if_required(loader);

						if(loader instanceof JB_FileLoader)
						{
							insert_instrument_calibration_if_required(loader);
						}
					}



				}

			}
		}
//		else {
//			listener.campaignDataLoadError(
//					"Unknown file types in directory " + dir.toString() + ". \n" +
//							"Data will not be loaded.\n" +
//							"Please check the file types and refer to the user guide for a list of supported files."
//					);
//		}


	}
	
	
	public int getRoot_hierarchy_id() {
		return root_hierarchy_id;
	}

	public void setRoot_hierarchy_id(int root_hierarchy_id) {
		this.root_hierarchy_id = root_hierarchy_id;
	}
	
	
	public void set_hierarchy_to_load(int hierarchy_id)
	{
		this.load_existing_hierarchy = true;
		setRoot_hierarchy_id(hierarchy_id);
	}

	public boolean isSimple_delta_loading() {
		return simple_delta_loading;
	}

	public void setSimple_delta_loading(boolean simple_delta_loading) {
		this.simple_delta_loading = simple_delta_loading;
	}

	SpectralFileInsertResult insert_spectral_file(SpectralFile spec_file, int hierarchy_id) throws SPECCHIOClientException {
		
		SpectralFileInsertResult results = new SpectralFileInsertResult();
		
		// first check whether or not the file has already been loaded
		// to do this, create a clone of the spectral file, remove it's measurement to reduce size and send it 
		// to the web service
//		SpectralFile light_clone = new SpectralFile(spec_file);
//		light_clone.setHierarchyId(hierarchy_id);
//		light_clone.setCampaignId(campaign.getId());
//		light_clone.setCampaignType(campaign.getType());
//		
//		boolean exists = specchio_client.spectralFileExists(light_clone);
//		
//		// if it doesn't exist, upload it
//		if (!exists) {
			spec_file.setCampaignType(campaign.getType());
			spec_file.setCampaignId(campaign.getId());
			spec_file.setHierarchyId(hierarchy_id);
			results = specchio_client.insertSpectralFile(spec_file);
			
//			ids = new int[results.size()];
//			for (int i = 0; i < results.size(); i++) {
//				ids[i] = results.get(i);
//			}
//		} 
		
		return results;
		
	}
	
	
	public void insert_instrument_calibration_if_required(SpectralFileLoader loader)
	{
		
							
		Instrument instr = specchio_client.getInstrumentForSpectralFile(loader.getSpec_file());
		
		Calibration c = new Calibration();	
		c.setInstrumentId(instr.getInstrumentId());
		c.setCalibrationNumber(loader.getSpec_file().getCalibrationSeries());
		c.setCalibration_type(Calibration.RADIOMETRIC_CALIBRATION);
        c.setField_of_view(loader.getSpec_file().getForeopticDegrees());
		
		boolean exists = specchio_client.instrumentCalibrationExists(c);
		
		if(!exists)
		{
			// insert new calibration
			if(loader instanceof ASD_FileFormat_V7_FileLoader)
			{
				insert_asd_instrument_calibration((ASD_FileFormat_V7_FileLoader)loader, instr, "LMP");
				insert_asd_instrument_calibration((ASD_FileFormat_V7_FileLoader)loader, instr, "BSE");
				insert_asd_instrument_calibration((ASD_FileFormat_V7_FileLoader)loader, instr, "DN");		
			}
			
			if(loader instanceof JB_FileLoader)
			{
				insert_JB_instrument_calibration((JB_FileLoader)loader, instr);
			}
		}	
		else
		{
			// calibration exists, now check if that particular foreoptic exists
			if (loader instanceof ASD_FileFormat_V7_FileLoader)
			{
				c.setField_of_view(loader.getSpec_file().getForeopticDegrees());

				exists = specchio_client.instrumentCalibrationExists(c);

				if(!exists)
				{
					// this foreoptic calibration does not yet exist
					insert_asd_instrument_calibration((ASD_FileFormat_V7_FileLoader)loader, instr, "DN");

				}
			}
		}
		
		

		
		
		
	}
	
	
	private void insert_asd_instrument_calibration(ASD_FileFormat_V7_FileLoader loader, Instrument instr, String cal_type)
	{
		
		double[] doubleArray = null;
		
		Calibration c = new Calibration();																		
		c.setInstrumentId(instr.getInstrumentId());
		c.setCalibrationNumber(loader.getSpec_file().getCalibrationSeries());	
		c.setField_of_view(loader.getSpec_file().getForeopticDegrees());
		c.setCalibration_type(Calibration.RADIOMETRIC_CALIBRATION);

		
		// lamp
		if(cal_type == "LMP")
		{
			c.setName("LMP");
			c.setComments("Calibration Lamp Radiance");
			c.setMeasurement_unit_id(specchio_client.getMeasurementUnitFromCoding(MeasurementUnit.Radiance).getUnitId());
			doubleArray = get_double_array(loader, loader.lamp_calibration_data);			
		}
		
		
		// reference panel
		if(cal_type == "BSE")
		{
			c.setName("BSE");
			c.setComments("Calibration Reference Panel Reflectance");
			c.setMeasurement_unit_id(specchio_client.getMeasurementUnitFromCoding(MeasurementUnit.Reflectance).getUnitId());
			doubleArray = get_double_array(loader, loader.base_calibration_data);		
		}
		
		
		// Digital Numbers obtained during calibration
		if(cal_type == "DN")
		{
			c.setName("DN");
			c.setComments("Instrument Digital Numbers during Calibration for FOV =" + loader.getSpec_file().getForeopticDegrees() + " degrees");
			c.setMeasurement_unit_id(specchio_client.getMeasurementUnitFromCoding(MeasurementUnit.DN).getUnitId());
			doubleArray = get_double_array(loader, loader.fibre_optic_data);	
			c.setField_of_view(loader.getSpec_file().getForeopticDegrees());
		}		
		
		c.setFactors(doubleArray);								
		specchio_client.insertInstrumentCalibration(c);	
		
	}
	
	
	private double[] get_double_array(ASD_FileFormat_V7_FileLoader loader, Float[][] input_arr)
	{		
		double[] doubleArray = new double[loader.getSpec_file().getNumberOfChannels(0)];
		for (int i = 0; i < loader.getSpec_file().getNumberOfChannels(0); i++) {
		    doubleArray[i] = input_arr[0][i];  // no casting needed
		}	
		return doubleArray;
	}
	
	private void insert_JB_instrument_calibration(JB_FileLoader loader, Instrument instr)
	{
		if(loader.is_fluoresence_sensor())
		{
			if(!loader.getDw_coef_fluorescence().isEmpty()) insertInstrumentCalibration(loader, loader.getDw_coef_fluorescence(), instr, "dw_coef", "Downwelling channel - radiometric gain");
			if(!loader.getUp_coef_fluorescence().isEmpty()) insertInstrumentCalibration(loader, loader.getUp_coef_fluorescence(), instr, "up_coef", "Upwelling channel - radiometric gain");			
		}
		else
		{
			if(!loader.getDw_coef_broadrange().isEmpty()) insertInstrumentCalibration(loader, loader.getDw_coef_broadrange(), instr, "dw_coef", "Downwelling channel - radiometric gain");
			if(!loader.getUp_coef_broadrange().isEmpty()) insertInstrumentCalibration(loader, loader.getUp_coef_broadrange(), instr, "up_coef", "Upwelling channel - radiometric gain");
		}		
		
	}
	
	
	
	

	private void insertInstrumentCalibration(SpectralFileLoader loader, ArrayList<Float> coeffs, Instrument instr, String name, String comment) {
		
		Calibration c = new Calibration();																		
		c.setInstrumentId(instr.getInstrumentId());
		c.setCalibrationNumber(loader.getSpec_file().getCalibrationSeries());	
		c.setCalibrationDate(loader.getCalibration_date());
		c.setName(name);
		c.setComments(comment);
		c.setMeasurement_unit_id(specchio_client.getMeasurementUnitFromCoding(MeasurementUnit.DN_div_Radiance).getUnitId()); // should be L/DN
		
		
		double[] doubleArray = new double[coeffs.size()];
		for (int i = 0; i < coeffs.size(); i++) {
		    doubleArray[i] = coeffs.get(i);  // no casting needed
		}	
				
		c.setFactors(doubleArray);								
		specchio_client.insertInstrumentCalibration(c);			
	}

	public int insert_hierarchy(String name, Integer parent_id) throws SPECCHIOClientException {
		
		// see if the node already exists
		Integer id = specchio_client.getHierarchyId(campaign, name, parent_id);
		
		if (id == -1) {
			// the node doesn't exist; insert it
			id = specchio_client.insertHierarchy(campaign, name, parent_id);
		}
		
		return id;
	}

	SpectralFileLoader get_spectral_file_loader(ArrayList<File> files) throws SPECCHIOClientException {
		ArrayList<String> exts = new ArrayList<String>();
		String filename = "";

		// first thing we do is to get a distinct list of all file extensions
		ListIterator<File> li = files.listIterator();
		while(li.hasNext()) {
			filename =li.next().getName();
			String[] tokens = filename.split("\\.");

			String ext = "";

			if (tokens.length < 2) {
				ext = null;
			} else {
				ext = tokens[tokens.length - 1]; // last element is the
													// extension
			}

			if (!exts.contains(ext))
				exts.add(ext);
		}
		
		// instantiate the appropriate kind of loader
		SpectralFileLoader loader = null;
		try {
			// cx if there are header files and slb (sli) files
			// in that case we got ENVI header and spectral libary files
//			if (exts.contains("hdr")
//					&& (exts.contains("slb") || exts.contains("sli")))
			if (exts.contains("hdr") || exts.contains("slb") || exts.contains("sli"))			
				loader = new ENVI_SLB_FileLoader(specchio_client, this);

			// cx for APOGEE files
			else if (exts.contains("TRM"))
				loader = new APOGEE_FileLoader(specchio_client, this);
			
			else if (exts.contains("xls"))
				loader = new XLS_FileLoader(specchio_client, this);		
			
			// cx for Spectral Evolution files
			else if (exts.contains("sed"))
				loader = new Spectral_Evolution_FileLoader(specchio_client, this);		
			
			
			// cx for UNISPEC SPT files
			else if (exts.contains("SPT"))
				loader = new UniSpec_FileLoader(specchio_client, this);		
			
			
			// cx for UNISPEC SPU files
			else if (exts.contains("spu") || exts.contains("SPU"))
				loader = new UniSpec_SPU_FileLoader(specchio_client, this);			
			
			// cx for Bruker FTIR dpt files
			else if (exts.contains("dpt"))
				loader = new BrukerDPT_FileLoader(specchio_client, this);						

			// cx for MFR out files
			else if (exts.contains("OUT"))
				loader = new MFR_FileLoader(specchio_client, this);

			// cx for HDF FGI out files
			else if (exts.contains("h5"))
				loader = new HDF_FGI_FileLoader(specchio_client, this);
			
			// cx for MODTRAN albedo input dat files
			else if (exts.contains("dat"))
				loader = new ModtranAlbedoFileLoader(specchio_client, this);	
			
			// cx for csv files
//			else if (exts.contains("CSV"))
//				loader = new FloX_FileLoader(specchio_client);	
			
			else {
	
				// those were the easy cases, now we need to start looking into the
				// files. For this, use the first file in the list.
				FileInputStream file_input = null;
				DataInputStream data_in = null;
	
				file_input = new FileInputStream(files.get(0));
				data_in = new DataInputStream(file_input);
				String line, line2, line3, line4;
	
				// use buffered stream to read lines
				BufferedReader d = new BufferedReader(
						new InputStreamReader(data_in));
				line = d.readLine();
				line2 = d.readLine();
				line3 = d.readLine();
				line4 = d.readLine();
				
	
				// cx for JAZ (Ocean Optics files)
				if (exts.contains("txt") && "SpectraSuite Data File".equals(line)) {
					loader = new JAZ_FileLoader(specchio_client, this);
				}
	
				// cx for SpectraSuite OO (Ocean Optics files)
				else if (exts.contains("csv")
						&& ("SpectraSuite Data File".equals(line) ||
								"SpectraSuite Data File\t".equals(line))) {
					loader = new OO_FileLoader(specchio_client, this);
	
				}
				
				
				// cx for Microtops TXT file
				else if ((exts.contains("csv") || exts.contains("TXT"))
						&& line.substring(0, 4).equals("REC#")
						&& line2.equals("FIELDS:")
						) {
					loader = new Microtops_FileLoader(specchio_client, this);
	
				}
								
				
				// cx for Ocean View TXT (Ocean Optics files produced by Ocean View Software)
				else if (exts.contains("txt")
						&& (line.contains("Data from")) 
						&& (line.contains("Node"))) {
					loader = new OceanView_FileLoader(specchio_client, this);
	
				}				
	
				// cx for COST OO CSV file format
				else if (exts.contains("csv")
						&& ("Wl; WR; S; Ref; Info;wl; mri_counts;gains;mri_irradiance".equals(line)
								|| "Wl; WR; S; Ref; Info".equals(line) ||
									"Sample;solar_local;DOY.dayfraction;SZA (deg);SAA (deg);COS(SZA);Qs quality_WR_stability;Ql quality_WR_level;Qd quality_WR_S_difference;Qh quality_WR;Qsat quality_WR;totalQ;Qwl_cal;wl of min L(Ha);wl of min L(O2B);wl of min L(O2A);Lin@400nm;Lin@500nm;Lin@600nm;Lin@680nm;Lin@O2-B;Lin@700nm;Lin@747.5 same as CF;Lin@753_broad;Lin@O2-A;Lin@800nm;Lin@890nm;Lin@990nm;PRI;R531;R570;Lin@643;CF@F656;NF@F656;Lin@680;CF@F687;NF@F687;Lin@753;CF@F760;NF@F760;R680;R800;SR(800,680);ND(800,680);ND(750,705);ND(858.5,645);ND(531,555);ND(531,551);ND(531,645);ND(800,550);SIPI(800,680,445);PSRI(680,500,750);NPQI(415,435);TVI(800,550,680);SR(740,720);GRI;SAVI;MSAVI;OSAVI;MTCI;RVI;WDVI;EVI;GEMI;BI;MODIS_PRI4;MODIS_PRI12;MODIS_PRI1;MODIS_NDVI;MODIS_EVI;R_blu_MODIS;R_green_MODIS;R_nir_MODIS;R_rep_MERIS;R_nir_MERIS;WI(900,970);ND(410,710);ND(530,570);ND(550,410);ND(720,420);ND(542,550);WC_R/(R+G+B);WC_G/(R+G+B);WC_B/(R+G+B);WC_GEI=2*G-(R+B);PPFDsum(umol m-2s-1);PPFDinteg(umol m-2s-1);fAPAR+fTPAR sum;fAPAR+fTPAR^2*Rsoil integ;n of Nan in Wr;perc. of Nan in Wr;n of Nan in S;perc. of Nan in S".equals(line))) {
					loader = new COST_OO_FileLoader(specchio_client, this);
	
				}
				
				
	
				// cx for TXT (ENVI format) files
				else if (exts.contains("txt") || exts.contains("TXT")) {
					loader = new TXT_FileLoader(specchio_client, this);
				}
	
				// cx for Spectra Vista HR-1024 files
				else if (exts.contains("sig")
						&& ("/*** Spectra Vista HR-1024 ***/".equals(line) ||
								"/*** Spectra Vista SIG Data ***/".equals(line))) {
					loader = new Spectra_Vista_HR_1024_FileLoader(specchio_client, this);
				}
	
				// cx if we got GER files
				else if ("///GER SIGNATUR FILE///".equals(line)) {
				loader = new GER_FileLoader(specchio_client, this);
				}
				
				// cx if we got FloX files
				else if ((exts.contains("csv") || exts.contains("CSV")) && line.contains("FloX") && filename.charAt(0) != 'F') {
					loader = new FloX_FileLoader(specchio_client, this);	
				}	
				
				// cx if we got RoX files: Full range instruments like the RoX start with an 'F' in the filename.
				// also, if the instrument number would be higher than 100 it would be a RoX
				else if ((exts.contains("csv") || exts.contains("CSV")) && line.contains("FloX") && filename.charAt(0) == 'F') {
					loader = new RoX_FileLoader(specchio_client, this);	
				}					
				
				// ignore FloX/RoX calibration files
				else if ((exts.contains("csv") || exts.contains("CSV")) && line.contains("wl_F;up_coef_F;dw_coef_F;wl_F;up_coef_F;dw_coef_F;Device ID")) {
					loader = null;	
				}	
				
				
	
				// cx if we got ASD files with the new file format (Indico Version
				// 7)
				else if (exts.contains("asd")) {
					loader = new ASD_FileFormat_V7_FileLoader(specchio_client, this);
				}
	
				// cx for SPECPR files (no extensions)
				else if (line != null && line.contains("SPECPR")) {
					loader = new SPECPR_FileLoader(specchio_client, this);
				}
	
				d.close();
				file_input.close();
				data_in.close();

				if (loader == null) {
					// cx if we got ASD files
					// to do this we open randomly the first file and read an ASD header
					file_input = new FileInputStream(files.get(0));
					data_in = new DataInputStream(file_input);
					ASD_FileLoader asd_loader = new ASD_FileLoader(specchio_client, this);
					SpectralFile sf = asd_loader.asd_file;
					sf.setFilename(files.get(0).getName());
		
					asd_loader.read_ASD_header(data_in, sf);
		
					if (sf.getCompany().equals("ASD")) {
						loader = new ASD_FileLoader(specchio_client, this);
					}
		
					file_input.close();
					data_in.close();
				}
				
				if (loader == null) {
					// cx if we got new ASD files without the proper ending: the case for calibration files
					// to do this we open randomly the first file and read an ASD header
					try {
					ASD_FileFormat_V7_FileLoader asd_loader = new ASD_FileFormat_V7_FileLoader(specchio_client, this);
		
					SpectralFile sf = asd_loader.load(files.get(0));
		
					if (sf.getCompany().equals("ASD")) {
						loader = new ASD_FileFormat_V7_FileLoader(specchio_client, this);
					}
		
					file_input.close();
					data_in.close();
					}
					catch (IOException e) {
						// presumably not an ASD V7 file; ignore it
					}
				}
				
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (MetaParameterFormatException e) {
			e.printStackTrace();
		} catch (org.joda.time.IllegalFieldValueException e) {
			e.printStackTrace();
		}
		
		// set the file format id according to the file format name and the database
		if (loader != null) {
			int file_format_id = specchio_client.getFileFormatId(loader.get_file_format_name());
			loader.set_file_format_id(file_format_id);
		}

		return loader;
	}
	
	
	
	public int getSuccessful_file_counter() {
		return successful_file_counter;
	}

	public void setSuccessful_file_counter(int successful_file_counter) {
		this.successful_file_counter = successful_file_counter;
	}

	
	public ArrayList<String> getFile_errors() {
		return file_errors;
	}

	public void setFile_errors(ArrayList<String> file_errors) {
		this.file_errors = file_errors;
	}

	public int getParsed_file_counter() {
		return parsed_file_counter;
	}

	public void setParsed_file_counter(int parsed_file_counter) {
		this.parsed_file_counter = parsed_file_counter;
	}

	public File getFlox_rox_cal_file() {
		return flox_rox_cal_file;
	}

	public void setFlox_rox_cal_file(File flox_rox_cal_file) {
		this.flox_rox_cal_file = flox_rox_cal_file;
	}	


}
