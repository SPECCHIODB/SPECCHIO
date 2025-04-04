package ch.specchio.file.reader.campaign;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.file.reader.spectrum.ASD_FileFormat_V7_FileLoader;
import ch.specchio.file.reader.spectrum.JB_FileLoader;
import ch.specchio.file.reader.spectrum.SpectralFileLoader;
import ch.specchio.spaces.MeasurementUnit;
import ch.specchio.types.*;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class ChunkLoader extends Thread{

    private SPECCHIOClient specchio_client;
    List<File> files;
    SpecchioCampaignDataLoader SCDL;
    boolean is_garbage;
    ArrayList<String> file_errors = new ArrayList<String>();
    ArrayList<SpectralFile> spectral_file_list = new ArrayList<SpectralFile>();
    private int file_counter;
    private int parsed_file_counter;
    private int spectrum_counter;
    private int successful_file_counter;
    private int files_with_null_sfl_cnt;
    private SpecchioMessage file_loading_from_FS;
    boolean[] exists_array;
    int parent_id;
    Hashtable<String, SpectralFileLoader> spectral_file_loader_hash = new Hashtable<String, SpectralFileLoader>();
    private ArrayList<SpectralFileLoader> loaders_of_new_instruments = new ArrayList<SpectralFileLoader>();



    public ChunkLoader(List<File> files, SpecchioCampaignDataLoader SCDL, boolean is_garbage, int parent_id){

        this.files = files;
        this.SCDL = SCDL;
        this.is_garbage = is_garbage;
        this.specchio_client = SCDL.getSpecchio_client();

        this.parent_id = parent_id;
    }


    public void run(){

        try {
        String timeStamp = new SimpleDateFormat("HH.mm.ss").format(new java.util.Date());
        //System.out.println(timeStamp + " " + this.getName() + ": Start loading files");
        loadFiles();
        timeStamp = new SimpleDateFormat("HH.mm.ss").format(new java.util.Date());
        //System.out.println(timeStamp + " " + this.getName() + ": Files loaded");
        getExistsArray();
        timeStamp = new SimpleDateFormat("HH.mm.ss").format(new java.util.Date());
        //System.out.println(timeStamp + " " + this.getName() + ": Exists checked");

            timeStamp = new SimpleDateFormat("HH.mm.ss").format(new java.util.Date());
            //System.out.println(timeStamp + " " + this.getName() + ": Start insert");
            insertFiles();
            timeStamp = new SimpleDateFormat("HH.mm.ss").format(new java.util.Date());
            //System.out.println(timeStamp + " " + this.getName() + ": Insert done");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void getExistsArray(){

        // check existence of all spectral files
        SpectralFiles sfs = new SpectralFiles(); // Container class for spectral files

        ArrayList<SpectralFile> spectral_light_file_list = new ArrayList<SpectralFile>();

        // create lightweight objects
        ListIterator<SpectralFile> sf_li = spectral_file_list.listIterator();

        while(sf_li.hasNext()) {
            SpectralFile spec_file = sf_li.next();
            SpectralFile light_clone = new SpectralFile(spec_file); // special constructor to create a lightweigth clone for checking the file existance
            light_clone.setHierarchyId(parent_id);
            light_clone.setCampaignId(SCDL.campaign.getId());
            light_clone.setCampaignType(SCDL.campaign.getType());

            spectral_light_file_list.add(light_clone);
        }

        sfs.setSpectral_file_list(spectral_light_file_list);
        sfs.setCampaignId(SCDL.campaign.getId());
        sfs.setCampaignType(SCDL.campaign.getType());

        exists_array = specchio_client.spectralFilesExist(sfs);


    }


    protected void loadFiles(){

        Instant start = Instant.now();

        int index = 0;

        // iterate over the files
        ListIterator<File> file_li = files.listIterator();

            while(file_li.hasNext()) { // For each data file create a loader

//                if (exists_array[index] == false)
//                {

                File file = file_li.next();

//                ArrayList<File> this_file = new ArrayList<File>(); // overkill ... change to single object later ...
//
//                this_file.add(file);

                System.out.println(file.getName());


                SpectralFileLoader sfl = this.SCDL.get_spectral_file_loader(file); //

                if (sfl != null) {

                    try {

                        // the loader can return null, e.g. if ENVI files are
                        // read
                        // and a body (*.slb) is passed.
                        // In such a case no spectrum is inserted.
                        SpectralFile spec_file = sfl.load(file);
                        if (spec_file != null && spec_file.getNumberOfSpectra() > 0) {
                            if (spec_file.getFileErrorCode() != SpectralFile.UNRECOVERABLE_ERROR) {
                                spec_file.setGarbageIndicator(is_garbage);

                                // add to file list
                                spectral_file_list.add(spec_file);

                                // keep a copy of the file loader for new ASD files to insert calibration data later on
                                if (spec_file.getAsdV7()) {
                                    Integer id = spec_file.get_asd_instr_and_cal_fov_identifier();
                                    if (!spectral_file_loader_hash.containsKey(id.toString()))
                                        spectral_file_loader_hash.put(id.toString(), sfl);

                                }

                                // keep a copy of the file loader for FLoX and RoX instruments
                                if (sfl instanceof JB_FileLoader) {
                                    String instrument_name = spec_file.getInstrumentName();

                                    if (!spectral_file_loader_hash.containsKey(instrument_name))
                                        spectral_file_loader_hash.put(instrument_name, sfl);
                                }

                            } else {
                                // serious error
                                // add the message to the list of all errors
                                // concatenate all errors into one message
                                StringBuffer buf = new StringBuffer("Issues found in " + spec_file.getFilename() + ":");

                                for (SpecchioMessage error : spec_file.getFileErrors(!this.SCDL.prefs.getBooleanPreference("VERBOSE_LEVEL_INFO"))) {

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


                        SCDL.campaignDataLoadFileCountUpdate(this, parsed_file_counter, file_counter, spectrum_counter);

//                        listener.campaignDataLoadFileCount(file_counter, spectrum_counter);
                    } catch (IOException ex) {
                        if (SCDL.listener != null)
                            SCDL.listener.campaignDataLoadError(file + ": " + ex.getMessage());
                        else
                            System.out.println(ex.getMessage());

                    } catch (MetaParameterFormatException ex) {
                        if (SCDL.listener != null)
                            SCDL.listener.campaignDataLoadError(file + ": " + ex.getMessage());
                        else
                            System.out.println(ex.getMessage());
                    }



                }
                else
                {
                    if(file != this.SCDL.getFlox_rox_cal_file()) // only count files without parser if they are not one of the CAL files ...
                        files_with_null_sfl_cnt ++;
                }

//                    index = index + 1;
//
//                }



            Instant end = Instant.now();

            file_loading_from_FS = new SpecchioMessage("file_loading_from_FS: " + Duration.between(start, end).getSeconds(), SpecchioMessage.INFO);



        }


    }
    
    
    public void insertFiles() throws IOException {

        ListIterator<SpectralFile> sf_li = spectral_file_list.listIterator();

        int index = 0;

        while(sf_li.hasNext()) {
            SpectralFile spec_file = sf_li.next();

            if (exists_array[index] == false)
            {

                SpectralFileInsertResult insert_result = new SpectralFileInsertResult();

                insert_result.addError(file_loading_from_FS); // time logging

                Instant start = Instant.now();

                insert_result = insert_spectral_file(spec_file, parent_id);

                Instant end = Instant.now();

                insert_result.addError(new SpecchioMessage("time for client side during insert_spectral_file: " + Duration.between(start, end).toMillis() + " [ms]", SpecchioMessage.INFO));


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

                insert_result.addErrors(spec_file.getFileErrors(!SCDL.prefs.getBooleanPreference("VERBOSE_LEVEL_INFO"))); // compile into one list of errors
                //				if(insert_result.getErrors().size() == 0) successful_file_counter++;

                // check on file errors
                if(insert_result.getErrors().size() > 0)
                {
                    ArrayList<SpecchioMessage> messages = insert_result.get_nonredudant_errors(!SCDL.prefs.getBooleanPreference("VERBOSE_LEVEL_INFO"));

                    if(messages.size() > 0)
                    {

                        // concatenate all errors into one message
                        StringBuffer buf = new StringBuffer("Issues found in " + spec_file.getFilename() + ":");

                        for (SpecchioMessage error : messages) {

                            buf.append("\n\t");

                            buf.append(error.toString());
                        }

                        buf.append("\n");

                        // add the message to the list of all errors
                        this.file_errors.add(buf.toString());
                    }

                }

                SCDL.campaignDataLoadFileCountUpdate(this, this.parsed_file_counter, file_counter, spectrum_counter);

            }

            index = index + 1;

        }


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
        spec_file.setCampaignType(SCDL.campaign.getType());
        spec_file.setCampaignId(SCDL.campaign.getId());
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

            Metadata md = new Metadata();

            try {
                MetaParameter mp = MetaParameter.newInstance(specchio_client.getAttributesNameHash().get("Integration Time"));
                Long temp = loader.getCbIT()[2];
                mp.setValue(temp.intValue() , "RAW");
                md.add_entry(mp);

                mp = MetaParameter.newInstance(specchio_client.getAttributesNameHash().get("Gain_SWIR1"));
                mp.setValue( loader.getCbSwir1Gain()[2], "RAW");
                md.add_entry(mp);

                mp = MetaParameter.newInstance(specchio_client.getAttributesNameHash().get("Gain_SWIR2"));
                mp.setValue( loader.getCbSwir2Gain()[2], "RAW");
                md.add_entry(mp);

            } catch (MetaParameterFormatException e) {
                e.printStackTrace();
            }

            c.setMetadata(md);
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
            if(!loader.getDw_coef_fluorescence().isEmpty()) insertInstrumentCalibration(loader, loader.getDw_coef_fluorescence(), instr, "dw_coef", "Downwelling channel - radiometric gain", Calibration.RADIOMETRIC_CALIBRATION);
            if(!loader.getUp_coef_fluorescence().isEmpty()) insertInstrumentCalibration(loader, loader.getUp_coef_fluorescence(), instr, "up_coef", "Upwelling channel - radiometric gain", Calibration.RADIOMETRIC_CALIBRATION);
            if(!loader.getNl_coefs_fluorescence().isEmpty()) insertInstrumentCalibration(loader, loader.getNl_coefs_fluorescence(), instr, "nl_coefs", "Non-Linearity Coefficients", Calibration.RADIOMETRIC_CALIBRATION);
            if(!loader.getAutonulling_coefs_fluorescence().isEmpty()) insertInstrumentCalibration(loader, loader.getAutonulling_coefs_fluorescence(), instr, "autonulling_coefs", "Autonulling Coefficients", Calibration.RADIOMETRIC_CALIBRATION);
        }
        else
        {
            if(!loader.getDw_coef_broadrange().isEmpty()) insertInstrumentCalibration(loader, loader.getDw_coef_broadrange(), instr, "dw_coef", "Downwelling channel - radiometric gain", Calibration.RADIOMETRIC_CALIBRATION);
            if(!loader.getUp_coef_broadrange().isEmpty()) insertInstrumentCalibration(loader, loader.getUp_coef_broadrange(), instr, "up_coef", "Upwelling channel - radiometric gain", Calibration.RADIOMETRIC_CALIBRATION);
            if(!loader.getNl_coefs_broadrange().isEmpty()) insertInstrumentCalibration(loader, loader.getNl_coefs_broadrange(), instr, "nl_coefs", "Non-Linearity Coefficients", Calibration.RADIOMETRIC_CALIBRATION);
            if(!loader.getAutonulling_coefs_broadrange().isEmpty()) insertInstrumentCalibration(loader, loader.getAutonulling_coefs_broadrange(), instr, "autonulling_coefs", "Autonulling Coefficients", Calibration.RADIOMETRIC_CALIBRATION);
        }

    }





    private void insertInstrumentCalibration(SpectralFileLoader loader, ArrayList<Float> coeffs, Instrument instr, String name, String comment, int calibration_type) {

        Calibration c = new Calibration();
        c.setInstrumentId(instr.getInstrumentId());
        c.setCalibrationNumber(loader.getSpec_file().getCalibrationSeries());
        c.setCalibrationDate(loader.getCalibration_date());
        c.setName(name);
        c.setComments(comment);
        c.setMeasurement_unit_id(specchio_client.getMeasurementUnitFromCoding(MeasurementUnit.DN_div_Radiance).getUnitId()); // should be L/DN
        c.setCalibration_type(calibration_type);

        double[] doubleArray = new double[coeffs.size()];
        for (int i = 0; i < coeffs.size(); i++) {
            doubleArray[i] = coeffs.get(i);  // no casting needed
        }


        c.setFactors(doubleArray);
        specchio_client.insertInstrumentCalibration(c);
    }



}
