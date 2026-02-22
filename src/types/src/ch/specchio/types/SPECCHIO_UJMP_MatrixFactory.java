package ch.specchio.types;

import org.ujmp.core.Matrix;

import java.util.Iterator;

import static org.ujmp.core.Matrix.factory;


public class SPECCHIO_UJMP_MatrixFactory {

    public Matrix getMatrix(int dim1, int dim2) {

        Matrix cube = factory.zeros(dim1, dim2);
        return cube;

    }

    public Matrix getCube(int dim1, int dim2, int dim3) {

        Matrix cube = factory.zeros(dim1, dim2, dim3);
        return cube;

    }


    public Matrix getCube(double[][][] pixel_array)
    {

        int frames = pixel_array.length;
        int columns = pixel_array[0].length;
        int lines = pixel_array[0][0].length;

//        System.out.println("lines: " + lines);
//        System.out.println("columns: " + columns);
//        System.out.println("frames: " + frames);

        Matrix cube = factory.zeros(frames, columns, lines);

        Iterable<long[]> iter =cube.allCoordinates();

        Iterator<long[]> it = iter.iterator();



        while(it.hasNext())
        {
            long[] coord = it.next();

            double val = pixel_array[(int)coord[0]][(int)coord[1]][(int)coord[2]];

            cube.setAsDouble(val,  coord);

        }

        return cube;

    }

}
