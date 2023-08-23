package ch.specchio.file.reader.spectrum;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.file.reader.campaign.SpecchioCampaignDataLoader;
import ch.specchio.types.SpectralFile;

import java.io.*;
import java.util.ArrayList;


public abstract class ENVI_FileLoader extends SpectralFileLoader {

    Character c = new Character(' ');

    ArrayList<Float> wvls = new ArrayList<Float>();

    public ENVI_FileLoader(String file_format_name, SPECCHIOClient specchio_client, SpecchioCampaignDataLoader campaignDataLoader)
    {
        super(file_format_name, specchio_client, campaignDataLoader);
    }

    public SpectralFile load(File file) throws IOException {
        spec_file = new SpectralFile();

        spec_file.setSpectrumNameType("ENVI Hdr");

        spec_file.setFilename(file.getName());
        spec_file.setFileFormatName(this.file_format_name);

        return spec_file;
    }


        Float[][] read_data(DataInputStream in, int channels, int no_of_spectra) throws IOException
    {
        Float[][] f = new Float[no_of_spectra][channels];

        if(this.spec_file.getDataType() == 4) // 32 bit float
        {

            for(int spec_no = 0; spec_no < no_of_spectra;spec_no++)
            {
                for(int band=0;band < channels;band++)
                {
                    if(spec_file.getByteOrder() == 1)
                        f[spec_no][band] = in.readFloat();
                    else
                        f[spec_no][band] = read_float(in);
                }
            }
        }

        if(this.spec_file.getDataType() == 5) // 64 bit float
        {

            for(int spec_no = 0; spec_no < no_of_spectra;spec_no++)
            {
                for(int band=0;band < channels;band++)
                {
                    if(spec_file.getByteOrder() == 1)
                        f[spec_no][band] = (float) in.readDouble();
                    else
                        f[spec_no][band] = read_double(in).floatValue();
                }
            }


        }


        return f;
    }

    public void read_ENVI_header(File hdr) throws IOException {

        if(spec_file == null) spec_file = new SpectralFile();

        file_input = new FileInputStream(hdr);
        data_in = new DataInputStream(file_input);

        read_ENVI_header(data_in);

        file_input.close();
        data_in.close();

    }


    void read_ENVI_header(DataInputStream in) throws IOException
    {
        String line;

        // use buffered stream to read lines
        BufferedReader d = new BufferedReader(new InputStreamReader(in));
        d.mark(100); // mark to enable re-read of line

        // read line by line
        while((line=d.readLine()) != null)
        {
            // tokenise the line
            String[] tokens = line.split("=");

            // remove leading and trailing white spaces from first and second token
            for (int i=0;i<tokens.length;i++) tokens[i] = tokens[i].trim();

            // analyse the tokens
            analyse_ENVI_HDR(tokens, d);

            d.mark(100); // mark to enable re-read of line
        }

        if(spec_file.getWvls().size() == 0)
        {
            Float[] wvl_array = new Float[spec_file.getNumberOfChannels(0)] ;
            // create band numbers in case of missing wavelength vectors
            for(int i=0;i<spec_file.getNumberOfChannels(0);i++)
            {
                wvl_array[i] = Float.valueOf(i+1);
            }

            spec_file.addWvls(wvl_array);
        }

        if (spec_file.getMeasurementUnits().size()==0)
        {
            for(int i=0;i<spec_file.getNumberOfSpectra();i++)
            {
                spec_file.addMeasurementUnits(0); // assume DN if no unit is given in header
            }
        }

    }

    void analyse_ENVI_HDR(String[] tokens, BufferedReader in) throws IOException
    {
        String t1 = tokens[0];

        if(t1.equals("ENVI"))
        {
            spec_file.setCompany(t1);
        }

        if(t1.equals("samples"))
        {
            this.setSamplesAttribute(Integer.valueOf(get_value_from_tokens(tokens)).intValue());

        }

        if(t1.equals("lines"))
        {
            this.setLinesAttribute(Integer.valueOf(get_value_from_tokens(tokens)).intValue());
        }

        if(t1.equals("bands"))
        {
            this.setBandsAttribute(Integer.valueOf(get_value_from_tokens(tokens)).intValue());
        }

        if(t1.equals("file type"))
        {
            spec_file.setFileType(get_value_from_tokens(tokens));
        }

        if(t1.equals("data type"))
        {
            spec_file.setDataType(Integer.valueOf(get_value_from_tokens(tokens)).intValue());
        }

        if(t1.equals("interleave"))
        {
            spec_file.setInterleave(get_value_from_tokens(tokens));
        }

        if(t1.equals("byte order"))
        {
            spec_file.setByteOrder(Integer.valueOf(get_value_from_tokens(tokens)).intValue());
        }

        if(t1.equals("spectra names"))
        {
            // return to start of line and re-read
            in.reset();
            read_spectra_names(in);
        }

        if(t1.equals("wavelength"))
        {
            // return to start of line and re-read
            in.reset();
            read_wvls(in);

            spec_file.addWvls(wvls.toArray(new Float[wvls.size()]));
        }

    }

    abstract void setLinesAttribute(int lines);
    abstract void setSamplesAttribute(int samples);
    abstract void setBandsAttribute(int bands);


    String get_value_from_tokens(String[] tokens)
    {
        int i = 1;
        // search the equal sign because the value follows afterwards
//        while(!(tokens[i++].equals("=")));

        return tokens[i];
    }

    void read_spectra_names(BufferedReader in) throws IOException
    {
        spec_file.setNumberOfSpectraNames(0);
        opening_parenthesis(in);
        names(in);
    }

    void read_wvls(BufferedReader in) throws IOException
    {
        opening_parenthesis(in);
        wvls(in);

    }

    // reads till opening parenthesis is found
    void opening_parenthesis(BufferedReader in) throws IOException
    {
        do
        {
            read_char(in);
        } while (!c.equals('{'));

    }

    void names(BufferedReader in) throws IOException
    {
        spaces(in);
        name(in);

        while(c.equals(','))
        {
            spaces(in);
            linebreak(in);
            name(in);
        }

    }



    void name(BufferedReader in) throws IOException
    {
        StringBuffer name = new StringBuffer("");

        while(!(c.equals(',') || c.equals('}')))
        {
            name.append(c);
            read_char(in);
        }

        spec_file.addSpectrumName(name.toString());

    }

    void wvls(BufferedReader in) throws IOException
    {
        spaces(in);
        wvl(in);

        while(c.equals(','))
        {
            spaces(in);
            linebreak(in);
            wvl(in);
        }

    }

    void wvl(BufferedReader in) throws IOException
    {
        String wvl = new String();

        while(!(c.equals(',') || c.equals('}')))
        {
            wvl = wvl + c;
            read_char(in);
        }

        wvls.add(Float.valueOf(wvl));


    }

    void spaces(BufferedReader in) throws IOException
    {
        do
        {
            read_char(in);
        } while(c.equals(' '));

    }

    void linebreak(BufferedReader in) throws IOException
    {
        if(c.equals('\n'))
            read_char(in);
    }



    void read_char(BufferedReader in) throws IOException
    {
        char[] c_arr = new char[1];
        in.read(c_arr,0,1);
        c = c_arr[0];
    }

}
