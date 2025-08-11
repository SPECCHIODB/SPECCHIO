package ch.specchio.file.reader.spectrum;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.file.reader.campaign.SpecchioCampaignDataLoader;
import ch.specchio.file.reader.utils.CHB_Setting;
import ch.specchio.file.reader.utils.CHB_Settings;
import ch.specchio.file.reader.utils.EnviFileLoader;
import ch.specchio.file.reader.utils.FileChannelReaderUtil;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.MetaParameterFormatException;
import ch.specchio.types.Metadata;
import ch.specchio.types.SpectralFile;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.ujmp.core.Matrix;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AVIRIS_4_Float32_FileLoader extends ENVI_FileLoader{

    CHB_Settings chb;

    public AVIRIS_4_Float32_FileLoader(SPECCHIOClient specchio_client, SpecchioCampaignDataLoader campaignDataLoader, CHB_Settings chb)
    {
        super("AVIRIS-4 Float32 Cube", specchio_client, campaignDataLoader);
        this.chb = chb;
    }

    @Override
    public SpectralFile load(File file) throws IOException {

        try {

            spec_file = new SpectralFile();

            spec_file.setSpectrumNameType("AVIRIS-4 Float32 Cube");
            spec_file.setCompany("UZH");
            spec_file.setInstrumentTypeNumber(4);
            spec_file.setInstrumentNumber("4");
            spec_file.setInstrumentName("AVIRIS-4");

            spec_file.setFilename(file.getName());
            spec_file.setFileFormatName(this.file_format_name);
            spec_file.addSpectrumFilename(spec_file.getFilename());


            spec_file.setPath(file.getAbsolutePath());

            spec_file.setDataType(4);  // 32 bit float

            // get acquisition time from filename
            String[] tokens = spec_file.getBasename().split("_");

            Pattern pattern = Pattern.compile("20..-..-..", Pattern.CASE_INSENSITIVE);
            Matcher matcher;
            int date_pos = 0;
            matcher = pattern.matcher(tokens[date_pos]);
            while(!matcher.find() && date_pos < tokens.length ){
                date_pos++;
                matcher = pattern.matcher(tokens[date_pos]);
            }

            String date_str = tokens[date_pos];
            String time_str = tokens[date_pos+1];
            String ms_str = tokens[date_pos+2];

            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HHmmss.SSS").withZoneUTC();

                DateTime dt_tmp = formatter.parseDateTime(date_str + " " + time_str + "." + ms_str);

            spec_file.setCaptureDate(0,dt_tmp);

            int chb_id = -1; // default value

            try {
                 chb_id = Integer.valueOf(tokens[2]);
            } catch(NumberFormatException ex){

                chb_id = -1; // no id found in this file name

            }

            // set metadata
            Metadata md = new Metadata();
            try {
                CHB_Setting setting = chb.settings.get(chb_id);

                if(setting != null) {
                    addMetaparameters(md, setting, chb_id);

//                    if(setting.cal_mode.equals("Radiometric")) {
                        addMetaparametersFromFilename(md, setting.cal_mode);
//                    }

                    spec_file.addEavMetadata(md);
                }

            } catch(NullPointerException ex){
                // can happen with measurements that were not controlled by CHB
            }


            // estimate size
            long bytes = Files.size(file.toPath());

            int samples = 1280;
            this.setSamplesAttribute(samples);

            int bands = 327; // metadata band removed from frame
            this.setBandsAttribute(bands);



            int frame_size_bytes = samples * bands * 4;

            long lines = bytes / frame_size_bytes;
            this.setLinesAttribute((int) lines);


            spec_file.setNumberOfSpectra(spec_file.getAcross_track_dim() * spec_file.getAlong_track_dim());

            file_input = new FileInputStream(file);

            FileChannelReaderUtil ENVI_Reader = new FileChannelReaderUtil(file_input, file_input.getChannel());

            EnviFileLoader EVL = new EnviFileLoader();
            Matrix cube = EVL.readBIL(spec_file, ENVI_Reader);

            spec_file.setIsUjmpStorage(true);

            spec_file.addMeasurementMatrix(cube);



        }  catch (NullPointerException ex) {
            boolean wtf = true;
        }

        return spec_file;

    }

    private void addMetaparametersFromFilename(Metadata md, String cal_mode) {

        // written for the radiometric case
        if(cal_mode.equals("Radiometric")) {
            String[] tokens = spec_file.getFilename().split("_");

            // get lamp combo
            String[] combo_tokens = tokens[3].split("combo");

            MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("Integrating Sphere Lamp Combination"));
            try {
                mp.setValue(Integer.valueOf(combo_tokens[1]));
            } catch (MetaParameterFormatException e) {
                e.printStackTrace();
            }
            md.addEntry(mp);
        }

        // check for dark current frame
        Hashtable<String, Integer> tax_hash = specchio_client.getTaxonomyHash(attributes_name_hash.get("CAL Measurement Type"));
        if(this.spec_file.getFilename().contains("_dark_")) {

            try {
                MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("CAL Measurement Type"));

                mp.setValue(tax_hash.get("Dark Current"));

                md.addEntry(mp);

            } catch (MetaParameterFormatException e) {
                e.printStackTrace();
            }

        }
        else {

            try {
                MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("CAL Measurement Type"));

                mp.setValue(tax_hash.get("Light"));

                md.addEntry(mp);

            } catch (MetaParameterFormatException e) {
                e.printStackTrace();
            }

        }


    }

    @Override
    void setLinesAttribute(int lines) {
        spec_file.setAlong_track_dim(lines);
    }

    @Override
    void setSamplesAttribute(int samples) {
        spec_file.setAcross_track_dim(samples);
    }

    @Override
    void setBandsAttribute(int bands) {
        spec_file.addNumberOfChannels(bands);
    }

    void addMetaparameters(Metadata md, CHB_Setting setting, int chb_id){

        try {

            MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("CHB ID"));
            mp.setValue(chb_id);
            md.addEntry(mp);

            if(setting.across_track_scan_angle != null){
                mp = MetaParameter.newInstance(attributes_name_hash.get("Across track scan angle"));
                mp.setValue(setting.across_track_scan_angle);
                md.addEntry(mp);
            }

            if(setting.along_track_scan_angle != null){
                mp = MetaParameter.newInstance(attributes_name_hash.get("Along track scan angle"));
                mp.setValue(setting.along_track_scan_angle);
                md.addEntry(mp);
            }

            if(setting.cal_mode != null){

                // CHB CAL Mode taxonomy

                Hashtable<String, Integer> tax_hash = specchio_client.getTaxonomyHash(attributes_name_hash.get("CHB CAL Mode"));


                mp = MetaParameter.newInstance(attributes_name_hash.get("CHB CAL Mode"));
                mp.setValue(tax_hash.get(setting.cal_mode));
                md.addEntry(mp);
            }



//            monochromator_wavelength
            if(setting.wavelength != null){
                mp = MetaParameter.newInstance(attributes_name_hash.get("Monochromator wavelength"));
                mp.setValue(setting.wavelength);
                md.addEntry(mp);
            }





        } catch (MetaParameterFormatException e) {
            e.printStackTrace();
        }

    }


}
