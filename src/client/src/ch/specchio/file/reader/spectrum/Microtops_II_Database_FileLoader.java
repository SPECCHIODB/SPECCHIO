package ch.specchio.file.reader.spectrum;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.file.reader.campaign.SpecchioCampaignDataLoader;
import ch.specchio.spaces.MeasurementUnit;
import ch.specchio.types.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.ListIterator;

public class Microtops_II_Database_FileLoader  extends SpectralFileLoader {

    String [] data_fields;
    Hashtable<String, Integer> data_fields_index_hash = new Hashtable<String, Integer>();
    Float[] wvls = new Float[5];

    ArrayList<Float[]> spectra = new ArrayList<Float[]>();
    ArrayList<Float[]> spectra_std = new ArrayList<Float[]>();
    ArrayList<Float[]> aot = new ArrayList<Float[]>();
    ArrayList<DateTime> times = new ArrayList<DateTime>();


    public Microtops_II_Database_FileLoader(SPECCHIOClient specchio_client, SpecchioCampaignDataLoader campaignDataLoader) {
        super("Microtops_II_Database", specchio_client, campaignDataLoader);
    }

    @Override
    public SpectralFile load(File file) throws IOException, MetaParameterFormatException {

        spec_file = new SpectralFile();

        spec_file.setPath(file.getAbsolutePath());
        spec_file.setFilename(file.getName());

        spec_file.setCompany("Solar Systems");
        spec_file.setFileFormatName(this.file_format_name);






        DateTimeFormatter formatter = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss").withZoneUTC();

        // open file
        file_input = new FileInputStream(file);
        data_in = new DataInputStream(file_input);
        BufferedReader d = new BufferedReader(new InputStreamReader(data_in));


        // read file
        String line=d.readLine(); // line 1: [MicrotopsII Database file format]
        line=d.readLine(); // line 1: header

        data_fields = line.split(",");

        for (int i=0;i<data_fields.length;i++)
        {
            data_fields_index_hash.put(data_fields[i], i);
        }

        // get wavelengths from SIG fields

        wvls[0] = get_wvls_from_SIG_token(data_fields[14]);
        wvls[1] = get_wvls_from_SIG_token(data_fields[15]);
        wvls[2] = get_wvls_from_SIG_token(data_fields[16]);
        wvls[3] = get_wvls_from_SIG_token(data_fields[17]);
        wvls[4] = get_wvls_from_SIG_token(data_fields[18]);

        line=d.readLine();

        while (!line.equals("[MicrotopsII Database file format]")) {
            parse_line(line);
            line=d.readLine();
        }

        data_in.close ();

        spec_file.setNumberOfSpectra(spectra.size()*3); // spectrum, stdev and aot for each measurement

        // compile measurements into array

        Float[][] out_spectrum = new Float[spec_file.getNumberOfSpectra()][this.wvls.length];

        ListIterator<Float[]> a_li = spectra.listIterator();
        ListIterator<Float[]> b_li = spectra_std.listIterator();
        ListIterator<Float[]> c_li = aot.listIterator();
        int i = 0;
        while(a_li.hasNext())
        {
            out_spectrum[i++] = a_li.next(); // spectrum
            out_spectrum[i++] = b_li.next(); // std
            out_spectrum[i++] = c_li.next(); // aot
        }

        ListIterator<DateTime> t_li = times.listIterator();
        i = 0;
        while(t_li.hasNext())
        {
            DateTime t = t_li.next();
            spec_file.setCaptureDate(i++, t); // spectrum
            spec_file.setCaptureDate(i++, t); // std
            spec_file.setCaptureDate(i++, t); // aot
        }

        // add for each spectrum, std and aot
        for(i=1;i<=spectra.size();i++)
        {
            spec_file.addSpectrumFilename(spec_file.getFilename() + "_" + i);
            spec_file.addSpectrumFilename(spec_file.getFilename() + "_" + i);
            spec_file.addSpectrumFilename(spec_file.getFilename() + "_" + i);
        }




        spec_file.setMeasurements(out_spectrum);

        return spec_file;
    }

    void parse_line(String line) throws MetaParameterFormatException {

        String[] fields = line.split(",");

        spec_file.setInstrumentNumber(fields[0]);

        Metadata smd = new Metadata();

        DateTimeFormatter formatter = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss").withZoneUTC();

        // Date and Time: 11/02/2023,16:47:26
        String time_str;
        if ( fields[2].length() == 7)
        {
            time_str = fields[1] + fields[2];
        }
        else
        {
            time_str = fields[1] + " " + this.remove_leading_spaces(fields[2]);
        }


        DateTime dt = formatter.parseDateTime(time_str);
        times.add(dt);

        // lat / lon
        spatial_pos p = new spatial_pos();
        p.latitude = Double.valueOf(fields[6]);
        p.longitude = Double.valueOf(fields[7]);
        p.altitude = Double.valueOf(fields[8]);
        spec_file.addPos(p);

        // pressure
        MetaParameter mp = MetaParameter.newInstance(attributes_name_hash.get("Air Pressure"));
        mp.setValue(Double.valueOf(fields[data_fields_index_hash.get("PRESSURE")]));
        smd.addEntry(mp);

        // SZA
        mp = MetaParameter.newInstance(attributes_name_hash.get("Illumination Zenith"));
        mp.setValue(Double.valueOf(fields[data_fields_index_hash.get("SZA")]));
        smd.addEntry(mp);

        // AM
        //			mp = MetaParameter.newInstance(attributes_name_hash.get("Illumination Azimuth"));
        //			mp.setValue(Double.valueOf(fields[8]));
        //			smd.addEntry(mp);

        // SDCORR

        // TEMP
        mp = MetaParameter.newInstance(attributes_name_hash.get("Ambient Temperature"));
        mp.setValue(Double.valueOf(fields[data_fields_index_hash.get("TEMP")]));
        smd.addEntry(mp);


        // ID

        // spectrum
        Float[] spectrum = new Float[5];

        spectrum[0] = Float.valueOf(fields[data_fields_index_hash.get("SIG440")]);
        spectrum[1] = Float.valueOf(fields[data_fields_index_hash.get("SIG675")]);
        spectrum[2] = Float.valueOf(fields[data_fields_index_hash.get("SIG870")]);
        spectrum[3] = Float.valueOf(fields[data_fields_index_hash.get("SIG936")]);
        spectrum[4] = Float.valueOf(fields[data_fields_index_hash.get("SIG1020")]);

        spectra.add(spectrum);
        spec_file.addMeasurementUnits(MeasurementUnit.Irradiance); // Irradiance: W/m2
        spec_file.addNumberOfChannels(5);
        spec_file.addWvls(wvls);

        // stddev of spectrum
        spectrum = new Float[5];

        spectrum[0] = Float.valueOf(fields[data_fields_index_hash.get("STD440")]);
        spectrum[1] = Float.valueOf(fields[data_fields_index_hash.get("STD675")]);
        spectrum[2] = Float.valueOf(fields[data_fields_index_hash.get("STD870")]);
        spectrum[3] = Float.valueOf(fields[data_fields_index_hash.get("STD936")]);
        spectrum[4] = Float.valueOf(fields[data_fields_index_hash.get("STD1020")]);

        spectra_std.add(spectrum);
        spec_file.addMeasurementUnits(MeasurementUnit.Irradiance); // Irradiance: W/m2
        spec_file.addNumberOfChannels(5);
        spec_file.addWvls(wvls);

        // AOT

        spectrum = new Float[5];

        spectrum[0] = Float.valueOf(fields[data_fields_index_hash.get("AOT440")]);
        spectrum[1] = Float.valueOf(fields[data_fields_index_hash.get("AOT675")]);
        spectrum[2] = Float.valueOf(fields[data_fields_index_hash.get("AOT870")]);
        spectrum[3] = Float.valueOf(fields[data_fields_index_hash.get("AOT936")]);
        spectrum[4] = Float.valueOf(fields[data_fields_index_hash.get("AOT1020")]);

        aot.add(spectrum);
        spec_file.addMeasurementUnits(MeasurementUnit.AOT);
        spec_file.addNumberOfChannels(5);
        spec_file.addWvls(wvls);

        // WATER
        mp = MetaParameter.newInstance(attributes_name_hash.get("Atmospheric Water Content"));
        mp.setValue(Double.valueOf(fields[data_fields_index_hash.get("WATER")]));
        smd.addEntry(mp);

        //
        spec_file.addEavMetadata(smd);
        spec_file.addEavMetadata(smd);
        spec_file.addEavMetadata(smd);


    }



    float get_wvls_from_SIG_token(String token) {
        String[] parts = token.split("SIG");

        float wvl = Float.valueOf(parts[1]);

        return wvl;
    }


}
