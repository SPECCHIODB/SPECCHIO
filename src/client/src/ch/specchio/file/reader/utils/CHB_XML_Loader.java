package ch.specchio.file.reader.utils;

import java.io.*;

public class CHB_XML_Loader {

    File xml_filename;

    public CHB_XML_Loader(File xml_filename) {
        this.xml_filename = xml_filename;

    }

    public CHB_Settings load() {

        CHB_Settings chb = new CHB_Settings();
        chb.LUT_key = getLUT_key(xml_filename.getName());

        // open file, read line by line
        try {

            BufferedReader br
                    = new BufferedReader(new FileReader(xml_filename));

            String line;
            while ((line = br.readLine()) != null) {

                CHB_Setting setting = new CHB_Setting();
                getSetting(line, "<request_id>", setting);
                getSetting(line, "<scan_angle>", setting);
                getSetting(line, "<cal_mode>", setting);
                getSetting(line, "<angle>", setting);



                if(xml_filename.getName().contains("srf") && setting.cal_mode.equals("Geometric")){
                    setting.cal_mode = "Spectral"; // correction for wrong CHB logging
                }

                getSetting(line, "<wavelength>", setting);


                chb.settings.put(setting.ID, setting);
            }

            br.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            boolean wtf = true;
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return chb;
    }

    private void getSetting(String line, String parameter, CHB_Setting setting){

        // check if setting exists
        if(line.contains(parameter)){

            // get position
            int para_ind_start = line.indexOf(parameter);
            int para_ind_end = line.indexOf("</" + parameter.substring(1, parameter.length()));

            int value_start = para_ind_start + parameter.length() ;
            int value_end = para_ind_end;
            String value = line.substring(value_start, value_end);

            setting.add(parameter, value);

        }

    }

    public static String getLUT_key(String filename){

        String LUT_key = "";
        String[] tokens = filename.split("_");
        int LUT_token_offset = 0;

        if(tokens[0].equals("ProxyRX")){
            LUT_token_offset = 0;
        }
        else {
            LUT_token_offset = 2;
        }

        // build lookup string
        if(filename.contains("srf")){

            // eg.g RawDataCube_Line_2_srf_900_1050_2024-10-14_175356_724.bin
            String wvl_range = tokens[2+LUT_token_offset] + "_" + tokens[3+LUT_token_offset];
            LUT_key = tokens[1+LUT_token_offset] + "_" + wvl_range;

        } else if(filename.contains("along-track")){


        } else {
            boolean unknown = true;
        }

        return LUT_key;

    }


}
