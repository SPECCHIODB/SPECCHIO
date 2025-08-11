package ch.specchio.file.reader.campaign;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.*;
import java.util.prefs.BackingStoreException;

import ch.specchio.file.reader.utils.CHB_Setting;
import ch.specchio.file.reader.utils.CHB_Settings;
import ch.specchio.file.reader.utils.CHB_XML_Loader;
import ch.specchio.types.*;
import org.joda.time.DateTime;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.client.SPECCHIOPreferencesStore;
import ch.specchio.file.reader.spectrum.*;
import org.joda.time.format.DateTimeFormatter;

public class SpecchioCampaignDataLoader extends CampaignDataLoader {



	private SPECCHIOClient specchio_client;
	private SpectralFileLoader sfl;

	private int root_hierarchy_id = 0;
	private boolean load_existing_hierarchy = false;
	private boolean simple_delta_loading = true;
	private boolean is_nl_cal_corr = false;

	ArrayList<String> file_errors = new ArrayList<String>();
	private int successful_file_counter;
	private int parsed_file_counter;
	protected SPECCHIOPreferencesStore prefs;

	class counters{
		int parsed_file_counter;
		int file_counter;
		int spectrum_counter;

		public counters(int parsed_file_counter, int file_counter, int spectrum_counter) {
			this.parsed_file_counter = parsed_file_counter;
			this.file_counter = file_counter;
			this.spectrum_counter = spectrum_counter;
		}

	}

	Hashtable<String, counters> counters_hash = new Hashtable<String, counters>();


	private File flox_rox_cal_file;
	private FileTime lastModifiedTime;
	private int files_with_null_sfl_cnt = 0;

	private Hashtable<String, CHB_Settings> CHB_Settings_hash = new Hashtable<String, CHB_Settings>();

	private DateTimeFormatter Spectra_Vista_HR_1024_FileLoader_User_Selected_DateTimeFormatter = null;

	public SpecchioCampaignDataLoader(CampaignDataLoaderListener listener, SPECCHIOClient specchio_client) {
		super(listener);		
		this.specchio_client = specchio_client;
		try {
			this.prefs = new SPECCHIOPreferencesStore();
		} catch (BackingStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public SpecchioCampaignDataLoader(SPECCHIOClient specchio_client) {
		super(null);		
		this.specchio_client = specchio_client;
		try {
			this.prefs = new SPECCHIOPreferencesStore();
		} catch (BackingStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}	

	// the actual code for loading a campaign
	public void run() {
		try {
			
			Instant start = Instant.now();
			
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

			Instant end = Instant.now();
			System.out.println("Loading time: " + Duration.between(start, end).getSeconds());
			
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
		SpecchioMessage file_loading_from_FS = null;

		// Garbage detection: all data that are under a folder called 'Garbage' will get an EAV garbage flag
		// this allows users to load also suboptimal (i.e. garbage) data into the database, but easily exclude them from any selection
		final boolean is_garbage = parent_garbage_flag | dir.getName().equals("Garbage"); // marks this directory as the one recognised as garbage

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
					setFlox_rox_cal_file(f); // Here we store the reference to the calibration file, later used in each individual loader
					if(f.getName().matches(".*_NL.*")){
						is_nl_cal_corr = true;
					}
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

			load_directory(hierarchy_id, curr_dir, parent_garbage_flag);  // recursive call
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

//
//			if(simple_delta_loading == false || new_data_available) {
//
//				Instant start = Instant.now();
//
//
//				if (1 == 0) {
//					if (listener != null)
//						listener.campaignDataLoadFileCount(file_counter, spectrum_counter);
//				}
//
//					if (listener != null)
//						listener.campaignDataLoadError(file + ": " + ex.getMessage());
//					else
//						System.out.println(ex.getMessage());
//
//				}
//
//					if (listener != null)
//						listener.campaignDataLoadError(file + ": " + ex.getMessage());
//					else
//						System.out.println(ex.getMessage());
//				}
//			}

			if(simple_delta_loading == false || new_data_available) {
				// split data into chunks, assign to threads and get started
				int n_threads = 5;
				int chunk_size = 30;

				float chunks_float = Float.valueOf(files.size()) / Float.valueOf(chunk_size);
				int chunks = files.size() / chunk_size;
				float rem = chunks_float - chunks;

				ArrayList<Integer> chunk_sizes = new ArrayList<Integer>();

				ArrayList<List<File>> file_chunks = new ArrayList<List<File>>();

				int j;
				for (j = 0; j < chunks; j++) {
					chunk_sizes.add(chunk_size);
					file_chunks.add(files.subList(j * chunk_size, j * chunk_size + chunk_size));
				}

				chunk_sizes.add(files.size() - chunks * chunk_size);
				file_chunks.add(files.subList(j * chunk_size, j * chunk_size + (files.size() - chunks * chunk_size)));

				ThreadPoolExecutor executor =
						(ThreadPoolExecutor) Executors.newFixedThreadPool(n_threads);

				List<CompletableFuture<String>> futures = new ArrayList<>();

				for (int i = 0; i < chunk_sizes.size(); i++) {

					int finalI = i;
//				Future<Object> future = executor.submit(() -> {
//					ChunkLoader CL = new ChunkLoader(file_chunks.get(finalI), this, is_garbage, parent_id);
//					CL.run();
//					return null;
//				});

					CompletableFuture<String> future = null;
					try {
						future = this.calculateAsync(executor, file_chunks.get(finalI), is_garbage, parent_id);

						futures.add(future);

					} catch (InterruptedException e) {
						e.printStackTrace();
					}


//				try {
//					sleep(1000);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}

				}

				CompletableFuture<Void> allFutures = CompletableFuture.allOf(
						futures.toArray(new CompletableFuture[futures.size()]));

				// Wait for all individual CompletableFuture to complete
				// All individual CompletableFutures are executed in parallel
				try {
					allFutures.get();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				} catch (NullPointerException e) {
					e.printStackTrace();
				}

			}

			boolean completed = true;

		}

//		else {
//			listener.campaignDataLoadError(
//					"Unknown file types in directory " + dir.toString() + ". \n" +
//							"Data will not be loaded.\n" +
//							"Please check the file types and refer to the user guide for a list of supported files."
//					);
//		}


	}

	public CompletableFuture<String> calculateAsync(ThreadPoolExecutor executor, List<File> file_chunk, boolean is_garbage, int parent_id) throws InterruptedException {
		CompletableFuture<String> completableFuture = new CompletableFuture<>();

		executor.submit(() -> {
				ChunkLoader CL = new ChunkLoader(file_chunk, this, is_garbage, parent_id);
				CL.run();
				completableFuture.complete("Thread finished loading.");
				return null;
			});

		return completableFuture;
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



	


	public synchronized int insert_hierarchy(String name, Integer parent_id) throws SPECCHIOClientException {
		
		// see if the node already exists
		Integer id = specchio_client.getHierarchyId(campaign, name, parent_id);
		
		if (id == -1) {
			// the node doesn't exist; insert it
			id = specchio_client.insertHierarchy(campaign, name, parent_id);
		}
		
		return id;
	}

	synchronized SpectralFileLoader get_spectral_file_loader(File file) throws SPECCHIOClientException {
		String ext = new String("");
		String filename = "";

		// first thing we do is to get a distinct list of all file extensions

		filename =  file.getName();
		String[] tokens = filename.split("\\.");

		if (tokens.length < 2) {
			ext = null;
		} else {
			ext = tokens[tokens.length - 1]; // last element is the
												// extension
		}

		
		// instantiate the appropriate kind of loader
		SpectralFileLoader loader = null;
		try {
			// cx if there are header files and slb (sli) files
			// in that case we got ENVI header and spectral library files
//			if (exts.contains("hdr")
//					&& (exts.contains("slb") || exts.contains("sli")))
			if (ext.contains("slb") || ext.contains("sli"))
				loader = new ENVI_SLB_FileLoader(specchio_client, this);

			if (ext.contains("hdr")) {
				// Can either be a spectral library, or a cube
				loader = new ENVI_SLB_FileLoader(specchio_client, this);

				((ENVI_SLB_FileLoader) loader).read_ENVI_header(file);

				if(((ENVI_SLB_FileLoader) loader).getSpec_file().getFileType().equals("ENVI Standard"))
					loader = new ENVI_Cube_FileLoader(specchio_client, this);

			}

			if (ext.contains("bin")) {

				try {

					if (filename.startsWith("RawDataCube") || filename.startsWith("DarkCurrent")) {

						// search the CHB metadata file
						CHB_Settings chb = this.getCHB_CAL_settings(filename, file.getParentFile());


						// This is an AVIRIS-4 data cube
						loader = new AVIRIS_4_Float32_FileLoader(specchio_client, this, chb);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			// cx for APOGEE files
			else if (ext.contains("TRM"))
				loader = new APOGEE_FileLoader(specchio_client, this);
			
			else if (ext.contains("xls"))
				loader = new XLS_FileLoader(specchio_client, this);		
			
			// cx for Spectral Evolution files
			else if (ext.contains("sed"))
				loader = new Spectral_Evolution_FileLoader(specchio_client, this);		
			
			
			// cx for UNISPEC SPT files
			else if (ext.contains("SPT"))
				loader = new UniSpec_FileLoader(specchio_client, this);		
			
			
			// cx for UNISPEC SPU files
			else if (ext.contains("spu") || ext.contains("SPU"))
				loader = new UniSpec_SPU_FileLoader(specchio_client, this);			
			
			// cx for Bruker FTIR dpt files
			else if (ext.contains("dpt"))
				loader = new BrukerDPT_FileLoader(specchio_client, this);						

			// cx for MFR out files
			else if (ext.contains("OUT"))
				loader = new MFR_FileLoader(specchio_client, this);

			// cx for HDF FGI out files
			else if (ext.contains("h5"))
				loader = new HDF_FGI_FileLoader(specchio_client, this);
			
			// cx for MODTRAN albedo input dat files
			else if (ext.contains("dat"))
				loader = new ModtranAlbedoFileLoader(specchio_client, this);	
			
			// cx for csv files
//			else if (exts.contains("CSV"))
//				loader = new FloX_FileLoader(specchio_client);	
			
			else {
	
				// those were the easy cases, now we need to start looking into the
				// files. For this, use the first file in the list.
				FileInputStream file_input = null;
				DataInputStream data_in = null;
	
				file_input = new FileInputStream(file);
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
				if (ext.contains("txt") && "SpectraSuite Data File".equals(line)) {
					loader = new JAZ_FileLoader(specchio_client, this);
				}
	
				// cx for SpectraSuite OO (Ocean Optics files)
				else if (ext.contains("csv")
						&& ("SpectraSuite Data File".equals(line) ||
								"SpectraSuite Data File\t".equals(line))) {
					loader = new OO_FileLoader(specchio_client, this);
	
				}
				
				
				// cx for Microtops TXT file
				else if ((ext.contains("csv") || ext.contains("TXT"))
						&& line.substring(0, 4).equals("REC#")
						&& line2.equals("FIELDS:")
						) {
					loader = new Microtops_FileLoader(specchio_client, this);
	
				}

				// cx for Microtops_II_Database
				else if ((ext.contains("csv"))
						&& line.equals("[MicrotopsII Database file format]")
				) {
					loader = new Microtops_II_Database_FileLoader(specchio_client, this);
				}
								
				
				// cx for Ocean View TXT (Ocean Optics files produced by Ocean View Software)
				else if (ext.contains("txt")
						&& (line.contains("Data from")) 
						&& (line.contains("Node"))) {
					loader = new OceanView_FileLoader(specchio_client, this);
	
				}				
	
				// cx for COST OO CSV file format
				else if (ext.contains("csv")
						&& ("Wl; WR; S; Ref; Info;wl; mri_counts;gains;mri_irradiance".equals(line)
								|| "Wl; WR; S; Ref; Info".equals(line) ||
									"Sample;solar_local;DOY.dayfraction;SZA (deg);SAA (deg);COS(SZA);Qs quality_WR_stability;Ql quality_WR_level;Qd quality_WR_S_difference;Qh quality_WR;Qsat quality_WR;totalQ;Qwl_cal;wl of min L(Ha);wl of min L(O2B);wl of min L(O2A);Lin@400nm;Lin@500nm;Lin@600nm;Lin@680nm;Lin@O2-B;Lin@700nm;Lin@747.5 same as CF;Lin@753_broad;Lin@O2-A;Lin@800nm;Lin@890nm;Lin@990nm;PRI;R531;R570;Lin@643;CF@F656;NF@F656;Lin@680;CF@F687;NF@F687;Lin@753;CF@F760;NF@F760;R680;R800;SR(800,680);ND(800,680);ND(750,705);ND(858.5,645);ND(531,555);ND(531,551);ND(531,645);ND(800,550);SIPI(800,680,445);PSRI(680,500,750);NPQI(415,435);TVI(800,550,680);SR(740,720);GRI;SAVI;MSAVI;OSAVI;MTCI;RVI;WDVI;EVI;GEMI;BI;MODIS_PRI4;MODIS_PRI12;MODIS_PRI1;MODIS_NDVI;MODIS_EVI;R_blu_MODIS;R_green_MODIS;R_nir_MODIS;R_rep_MERIS;R_nir_MERIS;WI(900,970);ND(410,710);ND(530,570);ND(550,410);ND(720,420);ND(542,550);WC_R/(R+G+B);WC_G/(R+G+B);WC_B/(R+G+B);WC_GEI=2*G-(R+B);PPFDsum(umol m-2s-1);PPFDinteg(umol m-2s-1);fAPAR+fTPAR sum;fAPAR+fTPAR^2*Rsoil integ;n of Nan in Wr;perc. of Nan in Wr;n of Nan in S;perc. of Nan in S".equals(line))) {
					loader = new COST_OO_FileLoader(specchio_client, this);
	
				}

				// cx for TXT  files generated by CHB: ignore them
				else if (ext.contains("txt") && line.contains("<Proxy><global><request_id>")) {
					return null;
				}

				else if (ext.contains("txt") && filename.startsWith("CWIS-II")) {
					return null;
				}
	
				// cx for TXT (ENVI format) files
				else if (ext.contains("txt") || ext.contains("TXT")) {
					loader = new TXT_FileLoader(specchio_client, this);
				}
	
				// cx for Spectra Vista HR-1024 files
				else if (ext.contains("sig")
						&& ("/*** Spectra Vista HR-1024 ***/".equals(line) ||
								"/*** Spectra Vista SIG Data ***/".equals(line))) {
					loader = new Spectra_Vista_HR_1024_FileLoader(specchio_client, this);
				}
	
				// cx if we got GER files
				else if ("///GER SIGNATUR FILE///".equals(line)) {
				loader = new GER_FileLoader(specchio_client, this);
				}
				
				// cx if we got FloX files
				else if ((ext.contains("csv") || ext.contains("CSV")) && line.contains("FloX") && filename.charAt(0) != 'F') {
					loader = new FloX_FileLoader(specchio_client, this);	
				}	
				
				// cx if we got legacy RoX files: Full range instruments like the RoX start with an 'F' in the filename.
				// also, if the instrument number would be higher than 100 it would be a RoX
				else if ((ext.contains("csv") || ext.contains("CSV")) && line.contains("FloX") && filename.charAt(0) == 'F') {
					loader = new RoX_FileLoader(specchio_client, this);	
				}

				// cx if we got RoX files: Full range instruments like the RoX start with an 'F' in the filename.
				// also, if the instrument number would be higher than 100 it would be a RoX
				else if ((ext.contains("csv") || ext.contains("CSV")) && line.contains("RoX") && filename.charAt(0) == 'F') {
					loader = new RoX_FileLoader_V2_2(specchio_client, this);
				}

				// ignore FloX/RoX calibration files
				else if ((ext.contains("csv") || ext.contains("CSV")) && line.contains("wl_F;up_coef_F;dw_coef_F;wl_F;up_coef_F;dw_coef_F;Device ID")) {
					loader = null;	
				}	
				
				
	
				// cx if we got ASD files with the new file format (Indico Version
				// 7)
				else if (ext.contains("asd")) {
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
					try {
					file_input = new FileInputStream(file);
					data_in = new DataInputStream(file_input);
					ASD_FileLoader asd_loader = new ASD_FileLoader(specchio_client, this);
					SpectralFile sf = asd_loader.asd_file;
					sf.setFilename(file.getName());


						asd_loader.read_ASD_header(data_in, sf);
		
					if (sf.getCompany().equals("ASD")) {
						loader = new ASD_FileLoader(specchio_client, this);
					}
		
					file_input.close();
					data_in.close();
					}
					catch (IOException e) {
						// presumably not an ASD V7 file; ignore it
					}
					catch (NumberFormatException e) {
						// presumably not an ASD V7 file; ignore it
					}
				}
				
				if (loader == null) {
					// cx if we got new ASD files without the proper ending: the case for calibration files
					// to do this we open randomly the first file and read an ASD header
					try {
					ASD_FileFormat_V7_FileLoader asd_loader = new ASD_FileFormat_V7_FileLoader(specchio_client, this);
		
					SpectralFile sf = asd_loader.load(file);
		
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

	public synchronized void campaignDataLoadFileCountUpdate(ChunkLoader chunkLoader, int parsed_file_counter, int file_counter, int spectrum_counter)
	{

		counters overall_cts = new counters(0, 0, 0);

		counters cts = counters_hash.get(chunkLoader.getName());

		if (cts == null){
			// insert new thread specific counter
			cts = new counters(parsed_file_counter, file_counter, spectrum_counter);
			counters_hash.put(chunkLoader.getName(), cts);
		}
		else {
			cts = counters_hash.get(chunkLoader.getName());
			cts.parsed_file_counter = parsed_file_counter;
			cts.file_counter = file_counter;
			cts.spectrum_counter = spectrum_counter;
		}

		// sum up all thread counters
		Enumeration<String> e = counters_hash.keys();

		while (e.hasMoreElements()) {

			// Getting the key of a particular entry
			String key = e.nextElement();
			cts = counters_hash.get(key);
			//cts = new counters(parsed_file_counter, file_counter, spectrum_counter);

			overall_cts.parsed_file_counter = overall_cts.parsed_file_counter + cts.parsed_file_counter;
			overall_cts.file_counter = overall_cts.file_counter + cts.file_counter;
			overall_cts.spectrum_counter = overall_cts.spectrum_counter + cts.spectrum_counter;

		}

		this.parsed_file_counter = overall_cts.parsed_file_counter;
		this.file_counter = overall_cts.file_counter;
		this.spectrum_counter = overall_cts.spectrum_counter;

		this.successful_file_counter = this.file_counter; // is that right?

		listener.campaignDataLoadFileCount(overall_cts.file_counter, overall_cts.spectrum_counter);



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

	public boolean is_nl_cal_corr(){
		return this.is_nl_cal_corr;
	}

	public DateTimeFormatter getSpectra_Vista_HR_1024_FileLoader_User_Selected_DateTimeFormatter() {
		return Spectra_Vista_HR_1024_FileLoader_User_Selected_DateTimeFormatter;
	}

	public void setSpectra_Vista_HR_1024_FileLoader_User_Selected_DateTimeFormatter(DateTimeFormatter spectra_Vista_HR_1024_FileLoader_User_Selected_DateTimeFormatter) {
		Spectra_Vista_HR_1024_FileLoader_User_Selected_DateTimeFormatter = spectra_Vista_HR_1024_FileLoader_User_Selected_DateTimeFormatter;
	}

	public SPECCHIOClient getSpecchio_client() {
		return specchio_client;
	}

	protected synchronized CHB_Settings getCHB_CAL_settings(String filename, File dir){

		CHB_Settings chb = null;
		String LUT_key = CHB_XML_Loader.getLUT_key(filename);


		if(CHB_Settings_hash.size() == 0){

			// read all RX CHB files and put them in hash
			FileFilter RX_Filefilter = new FileFilter()
			{
				public boolean accept(File file) {
					if (file.getName().startsWith("ProxyRX")) {
						return true;
					}
					return false;
				}
			};

			File[] RX_files = dir.listFiles(RX_Filefilter);


			for(int i=0;i<RX_files.length;i++){

				CHB_XML_Loader xml_l = new CHB_XML_Loader(RX_files[i]);

				CHB_Settings settings = xml_l.load();

				// insert into LUT
				CHB_Settings_hash.put(settings.LUT_key, settings);

			}

			// second option: there is no XML CAL file: this happens for integrating sphere calibrations where the CHB does not control the acquisition through an XML file
			if (filename.startsWith("RawDataCube_Line_is")) {

				CHB_Settings settings = new CHB_Settings();

				CHB_Setting setting = new CHB_Setting();
				setting.cal_mode = "is";
				//LUT_key = "is"; // dynamic modification of LUT key
				settings.LUT_key = LUT_key;
				setting.add("<cal_mode>", "is");
				settings.settings.put(-1, setting); // no ID, therefore set it to -1

				// insert into LUT
				CHB_Settings_hash.put(settings.LUT_key, settings);

			}


		}

		// retrieve from hash
		chb = CHB_Settings_hash.get(LUT_key);

		return chb;
	}

}
