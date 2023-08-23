package ch.specchio.file.reader.spectrum;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.file.reader.campaign.SpecchioCampaignDataLoader;
import ch.specchio.file.reader.utils.EnviFileLoader;
import ch.specchio.file.reader.utils.FileChannelReaderUtil;
import ch.specchio.types.SpectralFile;
import org.ujmp.core.Matrix;
import org.ujmp.core.doublematrix.DenseDoubleMatrix;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ENVI_Cube_FileLoader  extends ENVI_FileLoader {


    public ENVI_Cube_FileLoader(SPECCHIOClient specchio_client, SpecchioCampaignDataLoader campaignDataLoader)
    {
        super("ENVI Cube", specchio_client, campaignDataLoader);
    }

    @Override
    public SpectralFile load(File file) throws IOException {

        super.load(file);

        spec_file.setSpectrumNameType("ENVI Cube");

        spec_file.setFilename(file.getName());
        spec_file.setFileFormatName(this.file_format_name);
        spec_file.addSpectrumFilename(spec_file.getFilename());

        spec_file.setPath(file.getAbsolutePath());

        read_ENVI_header(file);

        spec_file.setNumberOfSpectra(spec_file.getAcross_track_dim()*spec_file.getAlong_track_dim());

        // expected body path name
        String body_pathname = spec_file.getPath().substring(0,
                spec_file.getPath().length() - spec_file.getExt().length() - 1);

        // new file object for the header
        File body = new File(body_pathname.concat(".raw"));
        body = new File(body_pathname.concat(""));

        file_input = new FileInputStream(body);

        FileChannelReaderUtil ENVI_Reader = new FileChannelReaderUtil(file_input, file_input.getChannel());

        EnviFileLoader EVL = new EnviFileLoader();
        Matrix cube = EVL.readBIL(spec_file, ENVI_Reader);

        cube.showGUI();

        spec_file.setIsUjmpStorage(true);

//        data_in = new DataInputStream(file_input);

        // load all spectra
        //spec_file.setMeasurements(read_data(data_in, spec_file.getNumberOfChannels(0), spec_file.getNumberOfSpectra()));

//        Float[][] data = read_data(ENVI_Reader);

//        ShortMatrix sc = cube.toShortMatrix();

        spec_file.addMeasurementMatrix(cube);

//        spec_file.setMeasurementMatrix(cube);

        return spec_file;
    }

//
//    Float[][] read_data(DataInputStream in) throws IOException {
////        Float[][] f = new Float[no_of_spectra][channels];
//
//        Float[][] f = null;
//
//        // BIL
//        readBIL(in);
//
//
//
////
////        if (this.spec_file.getDataType() == 4) // 32 bit float
////        {
////
////            for (int spec_no = 0; spec_no < no_of_spectra; spec_no++) {
////                for (int band = 0; band < channels; band++) {
////                    if (spec_file.getByteOrder() == 1)
////                        f[spec_no][band] = in.readFloat();
////                    else
////                        f[spec_no][band] = read_float(in);
////                }
////            }
////        }
////
////        if(this.spec_file.getDataType() == 5) // 64 bit float
////        {
////
////            for(int spec_no = 0; spec_no < no_of_spectra;spec_no++)
////            {
////                for(int band=0;band < channels;band++)
////                {
////                    if(spec_file.getByteOrder() == 1)
////                        f[spec_no][band] = (float) in.readDouble();
////                    else
////                        f[spec_no][band] = read_double(in).floatValue();
////                }
////            }
////
////
////        }
//
//        return f;
//    }


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
}
