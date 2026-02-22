package ch.specchio.file.reader.utils;

public class CHB_Setting {

    public int ID;
    public Double across_track_scan_angle = null;
    public Double along_track_scan_angle = null;
    public Double wheel_0 = null; // filter wheel for non linearity and straylight
    public Double wheel_1 = null; // filter wheel for non linearity and straylight
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

        else if(parameter.equals("<wheel_0>")){
            wheel_0 = Double.valueOf(value);
        }

        else if(parameter.equals("<wheel_1>")){
            wheel_1 = Double.valueOf(value);
        }

        else if(parameter.equals("<cal_mode>")) {
            if (value.equals("geo")) {
                cal_mode = "Geometric";
            } else if (value.equals("srf")) {
                cal_mode = "Spectral";
            } else if (value.equals("is")) {
                cal_mode = "Radiometric";
            }
        }
        else if(parameter.equals("<wavelength>")){
            wavelength = Double.valueOf(value);
        }


    }
}
