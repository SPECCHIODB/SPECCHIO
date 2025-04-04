package ch.specchio.file.reader.utils;

import java.util.ArrayList;
import java.util.Hashtable;

public class CHB_Settings {

    String xml_filepath;
    String CAL_type;
    String extended_CAL_type; // includes further info for batches

    public String LUT_key;

    public Hashtable<Integer, CHB_Setting> settings = new Hashtable<Integer, CHB_Setting>();


}
