package ch.specchio.file.reader.utils;

public class CHB_Setting {

    public int ID;
    public Double across_track_scan_angle = null;
    public Double along_track_scan_angle = null;
    public String cal_mode = null;
    public Double wavelength = null;


    public void add(String parameter, String value) {

        if(parameter.equals("<request_id>")){
            ID = Integer.valueOf(value);
        }
        else if(parameter.equals("<scan_angle>")){
            across_track_scan_angle = Double.valueOf(value);
        }
        else if(parameter.equals("<angle>")){
            along_track_scan_angle = Double.valueOf(value);
        }
        else if(parameter.equals("<cal_mode>")){
            if(value.equals("geo")){
                cal_mode = "Geometric";
            } else if (value.equals("srf")){
                cal_mode = "Spectral";
            } else if (value.equals("radiometric")){
                cal_mode = "Radiometric";
            }



        }
        else if(parameter.equals("<wavelength>")){
            wavelength = Double.valueOf(value);
        }


    }
}
