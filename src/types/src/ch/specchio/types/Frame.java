package ch.specchio.types;

import org.ujmp.core.Matrix;
import org.ujmp.core.doublematrix.factory.DefaultDenseDoubleMatrix2DFactory;

import java.util.Iterator;

public class Frame {


    String filename;
//    private APEXMetadata meta = null;
    int columns;
    int vnir_columns;
    int swir_columns;
    int lines;
    Matrix frame = null;
    Integer frame_id;
//    static int acrosstrack_pixels = 1000;

//    static int mask = (int) (Math.pow(2, 15) + Math.pow(2, 14));



//
//    public Frame(double[][][] pixel_array)
//    {
//
//        DefaultDenseDoubleMatrixFactory factory = new DefaultDenseDoubleMatrixFactory();
//
//        int frames = pixel_array.length;
//        columns = pixel_array[0].length;
//        lines = pixel_array[0][0].length;
//
//        System.out.println("lines: " + lines);
//        System.out.println("columns: " + columns);
//        System.out.println("frames: " + frames);
//
//        //factory = new DefaultDenseDoubleMatrix2DFactory();
//
//        frame = factory.zeros(frames, columns, lines);
//
//        //double x = pixel_array[1][1][1];
//
//        Iterable<long[]> iter =frame.allCoordinates();
//
//        Iterator<long[]> it = iter.iterator();
//
//
//
//        while(it.hasNext())
//        {
//            long[] coord = it.next();
//
////	    	System.out.println(coord[0]);
////	    	System.out.println(coord[1]);
////	    	System.out.println(coord[2]);
//
//            double val = pixel_array[(int)coord[0]][(int)coord[1]][(int)coord[2]];
//
//            frame.setAsDouble(val,  coord);
//
//        }


        // store data
//	    for (int f = 0; f < frames; f++)
//	    {
//		    for (int col = 0; col < columns;col++)
//		    {
//			    for (int row = 0; row < lines;row++)
//			    {
//			    	System.out.println("Frame " + f);
//			    	System.out.println(col);
//			    	System.out.println(row);
//			    	frame.setAsDouble(pixel_array[f][col][row], f, col, row);
//			    }
//		    }
//	    }

//    }


    public Frame(double[][] pixel_array)
    {

        DefaultDenseDoubleMatrix2DFactory factory = new DefaultDenseDoubleMatrix2DFactory();

        System.out.println("dim 2: " + pixel_array[0].length);
        System.out.println("dim 1: " + pixel_array.length);

        if(pixel_array[0].length < 1000)
        {
            System.out.println("Interpreting as vector collection ...");
            // this is collection of vectors: n * spectral_bands
            columns = pixel_array[0].length;
            int layers = pixel_array.length;

            frame = factory.zeros(layers, columns);

            // store data
            for (int l = 0; l < layers;l++)
            {
                for (int c = 0; c < columns;c++)
                {
                    frame.setAsDouble(pixel_array[l][c], l, c);
                }
            }

        }
        else
        {
            System.out.println("Interpreting as single frame ...");
            columns = pixel_array.length;
            lines = pixel_array[0].length;


            frame = factory.zeros(columns, lines);

            Iterable<long[]> iter =frame.allCoordinates();

            Iterator<long[]> it = iter.iterator();



            while(it.hasNext())
            {
                long[] coord = it.next();

//		    	System.out.println(coord[0]);
//		    	System.out.println(coord[1]);
//		    	System.out.println(coord[2]);

                double val = pixel_array[(int)coord[0]][(int)coord[1]];

                frame.setAsDouble(val,  coord);

            }


        }



        System.out.println("lines: " + lines);
        System.out.println("columns: " + columns);



    }

    public Frame(Matrix m)
    {
        frame = m;

        columns = (int) m.getSize(0);
        lines = (int) m.getSize(1);
    }


    public Frame(int frame_id, Matrix m) {

        this.frame_id = frame_id;
        frame = m;

        columns = (int) m.getSize(0);
        lines = (int) m.getSize(1);
    }

//
//    // lazy loading of metadata
//    public APEXMetadata get_metadata()
//    {
//        if (meta == null)
//        {
//            try {
//                meta = new APEXMetadata(frame_id);
//            } catch (SQLException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//        return meta;
//    }
//
//    public void set_metadata(APEXMetadata m)
//    {
//        this.meta = m;
//    }


    public void set_frame(double[][] pixel_array)
    {
        DefaultDenseDoubleMatrix2DFactory factory = new DefaultDenseDoubleMatrix2DFactory();
        columns = pixel_array.length;
        lines = pixel_array[0].length;

        frame = factory.zeros(columns, lines);

        Iterable<long[]> iter =frame.allCoordinates();

        Iterator<long[]> it = iter.iterator();

        while(it.hasNext())
        {
            long[] coord = it.next();
            double val = pixel_array[(int)coord[0]][(int)coord[1]];
            frame.setAsDouble(val,  coord);
        }

    }



    public int get_no_of_bytes_of_datatype()
    {

        //if (frame.getValueType() == ValueType.SHORT) return 2;

        //if (frame.getValueType() == ValueType.DOUBLE) return 8; // 64 bit


        return 2;

        //return 0; // default, which should never happen

    }


    public double[][] get_array_as_double()
    {
        return frame.toDoubleArray();
    }

    public double[][][] get_cube_as_double()
    {
        long[] cube_size = frame.getSize();

        lines= (int) cube_size[2];
        columns = (int) cube_size[1];
        int frames = (int) cube_size[0];

        double[][][] out = new double[frames][columns][lines];
        for (int l = 0; l < frames;l++)
        {
            for (int col = 0; col < columns;col++)
            {
                for (int row = 0; row < lines;row++)
                {

                    //		    	System.out.println(col);
                    //		    	System.out.println(row);
                    //
                    //		    	long[] coord = new long[3];
                    //		    	coord[0] = slice_no;
                    //		    	coord[1] = slice_no;
                    //		    	coord[2] = slice_no;
                    //
                    //out[row][col] = frame.getAsDouble(slice_no, row, col);

                    out[l][col][row] = frame.getAsDouble(l, col, row);

                }
            }
        }

//	    Frame tmp_frame = new Frame(out);
//	    tmp_frame.get_frame_as_matrix().showGUI();
//

        return out;
    }


    public double[][] get_layer_as_double(int slice_no)
    {
        long[] cube_size = frame.getSize();

        if (cube_size.length == 3) return get_cube_slice_as_double(slice_no);

        // this is a vector (or should be ...)
        //int layers= (int) cube_size[1];
        columns = (int) cube_size[1];

        double[][] out = new double[columns][1];

        for (int col = 0; col < columns;col++)
        {
            out[col][0] = frame.getAsDouble(slice_no, col);
        }

        return out;

    }

    private double[][] get_cube_slice_as_double(int slice_no)
    {
        //Matrix sub = frame.subMatrix(Calculation.Ret.ORIG, 1, 1, 1, 316);

        long[] cube_size = frame.getSize();

        lines= (int) cube_size[2];
        columns = (int) cube_size[1];

        double[][] out = new double[columns][lines];

        for (int col = 0; col < columns;col++)
        {
            for (int row = 0; row < lines;row++)
            {

//		    	System.out.println(col);
//		    	System.out.println(row);
//
//		    	long[] coord = new long[3];
//		    	coord[0] = slice_no;
//		    	coord[1] = slice_no;
//		    	coord[2] = slice_no;
//
                //out[row][col] = frame.getAsDouble(slice_no, row, col);

                out[col][row] = frame.getAsDouble(slice_no, col, row);

            }
        }

//	    Frame tmp_frame = new Frame(out);
//	    tmp_frame.get_frame_as_matrix().showGUI();
//

        return out;
    }

    public Matrix get_frame_as_matrix()
    {
        return frame;
    }


    public double get_pixel_zero_indexed(int col, int line)
    {
        return frame.getAsDouble(col, line);
    }

    public double get_pixel(int col, int line)
    {
        return frame.getAsDouble(col-1, line-1);
    }



    public int[] get_frame_size()
    {
        long[] cube_size = frame.getSize();

        int dimensions = cube_size.length;


        int[] size = new int[dimensions];



        for (int i = 0;i<dimensions;i++)
        {
            size[i] = (int) cube_size[i];
        }


        return size;
    }

    public boolean has_layers()
    {
        long[] cube_size = frame.getSize();

        if(cube_size.length == 2 && cube_size[1] < 1000) return true;
        if(cube_size.length > 2)  {

            boolean has_layers = true;

            // all dimensions must be bigger than one
            for(int i = 0;i<cube_size.length;i++){
                has_layers = has_layers & cube_size[i] > 1;
            }


            return has_layers;
        }

        return false;
    }

    public boolean is_wvl_and_spectrum()
    {

        long[] cube_size = frame.getSize();

        if(cube_size.length == 2 && cube_size[0] == 2) return true;

        return false;

    }



    public int get_no_of_layers()
    {
        long[] cube_size = frame.getSize();

        if(has_layers())
        {

            return (int)cube_size[0];

//			if(cube_size.length == 2 && cube_size[1] < acrosstrack_pixels)
//
//				{
//					return (int)cube_size[0];
//				}
//			else
//				{
//					return (int) cube_size[0];
//				}
        }
        else
        {
            return 1;
        }


    }

//
//    public int get_no_of_acrosstrack_pixels()
//    {
//        long[] cube_size = frame.getSize();
//
//        if(cube_size.length == 2 && cube_size[1] < acrosstrack_pixels) return 1; // this is a collection of vector
//        //if(cube_size.length > 2)  return acrosstrack_pixels;
//
//        return acrosstrack_pixels;	 // default
//
//    }


    public int get_no_of_bands()
    {
        long[] cube_size = frame.getSize();

        if (!has_layers() && !is_wvl_and_spectrum()) return (int) cube_size[0];



        if (has_layers()) return (int) cube_size[1];

        return (int) cube_size[1]; //is_wvl_and_spectrum

    }

//    public int get_no_of_VNIR_bands()
//    {
//        if(vnir_columns == 0)
//        {
//            vnir_columns = this.get_metadata().get_entry("NVNIR").valueAsInt();
//        }
//
//        return vnir_columns;
//    }
//
//    public int get_no_of_SWIR_bands()
//    {
//        if(swir_columns == 0)
//        {
//            swir_columns = this.get_metadata().get_entry("NSWIR").valueAsInt();
//        }
//        return swir_columns;
//    }


    public int get_id() {
        return frame_id;
    }





}
