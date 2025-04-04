/************************************************************************
*            VTE - Visual Terrain Explorer
*    Copyright (C) 2005 Ricardo Veguilla-Gonzalez,
*                    Nayda G. Santiago
*          University of Puerto Rico, Mayaguez
*
*    This program is free software; you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation; either version 2, or (at your option)
*    any later version.
*
*    This program is distributed in the hope that it will be useful, but
*    WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
*    General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program; if not, write to the Free Software
*    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
*    02110-1301, USA.
*
 ************************************************************************/

package ch.specchio.file.reader.utils;

import ch.specchio.types.SpectralFile;
import org.ujmp.core.Matrix;
import org.ujmp.core.doublematrix.DenseDoubleMatrix;
//import mikera.vectorz.*;
//import mikera.matrixx.*;
//import org.ujmp.core.doublematrix.DenseDoubleMatrix;

import java.io.IOException;

import static org.ujmp.core.Matrix.factory;


public class EnviFileLoader  {
	protected static String extensionList[] = { "hdr" };



	

	public EnviFileLoader() {
		super();
	}

	

	protected float readData(int dataType,FileReaderUtil fileReader) throws IOException {
		float sample = 0.0f;
		switch (dataType) {

		case -10:
			sample = (float) fileReader.readByte();
			break;
		case 1:
			sample =  (float) fileReader.readByte();
			break;
		case 2:
			sample =  (float) fileReader.readShort();
			break;
		case 4:
			sample =  fileReader.readFloat();
			break;
		case 8:
			sample =  (float) fileReader.readDouble();
			break;
		case 10:
			sample =  (float) fileReader.readByte();
			break;
		case 12:
				sample =  (float) fileReader.readShort();
				break;
		case 20:
			sample =  (float) fileReader.readShort();
			break;
		case 40:
			sample =  (float) fileReader.readInt();
			break;
		}
		return sample;
	}
	
//	public Matrix readBSQ(SpectralFile spec_file, FileReaderUtil fileReader) {
//		/*
//		 *
//		 * BSQ (Band Sequential Format) In its simplest form, the data is in BSQ
//		 * format, with each line of the data followed immediately by the next
//		 * line in the same spectral band. This format is optimal for spatial
//		 * (X, Y) access of any part of a single spectral band.
//		 *
//		 *
//		 */
//
//
//		int dataType = spec_file.getDataType();
//		int bytesPerSample = getBytesPerSample(dataType);
//		int samples = spec_file.getAcross_track_dim();
//		int bands = spec_file.getNumberOfChannels(0);
//		int lines = spec_file.getAlong_track_dim();
//		//short sample = 0;
//		float sample;
//		int pixel = 0;
//
////		DefaultDenseDoubleMatrixFactory factory = new DefaultDenseDoubleMatrixFactory();
////
//		Matrix cube = factory.zeros(samples, bands, lines);
////		Matrix cube = ShortMatrix.factory.zeros(samples, bands, lines);
//
//		//AMatrix cube = Matrixx..create(samples, bands);
//
//		try {
////			for (int l = 0; l < lines; l++) {
////				for (int s = 0; s < samples; s++) {
////					pixel = (l * samples) + s;
////					fileReader.position(pixel * bytesPerSample * bands);
////					for (int b = 0; b < bands; b++) {
////
////						//sample = fileReader.readShort();
////
////						if(s==1000){
////							int x = 1;
////						};
////
////						sample = readData(dataType, fileReader);
//////						data.putSample(s, l, b, (sample > 0.0 ? sample : 0.0f ));
////
////						//cube.setAsShort(sample, s, b, l);
////						cube.setAsDouble(sample, s, b, l);
////
////
////					}
////				}
////			}
//			for (int l = 0; l < lines; l++) {
//
//
//				for (int b = 0; b < bands; b++) {
//
//					for (int s = 0; s < samples; s++) {
////					pixel = (l * samples) + s;
////					fileReader.position(pixel * bytesPerSample * bands);
//
//
//						//sample = fileReader.readShort();
//
//						if(s==653){
//							int x = 1;
//						};
//
//						sample = readData(dataType, fileReader);
////						data.putSample(s, l, b, (sample > 0.0 ? sample : 0.0f ));
//
//						if (sample  > 20000)
//						{
//							int x = 2;
//						}
//
//						//cube.setAsShort(sample, s, b, l);
//						cube.setAsDouble(sample, s, b, l);
//
//
//					}
//				}
//			}
//
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
////		cube.showGUI();
//
//		return cube;
//
////
////		int bands = header.getBands();
////		int lines = header.getLines();
////		int samples = header.getSamples();
////		int dataType = header.getDataType();
////
////		int bytesPerSample = getBytesPerSample(dataType);
////		float sample = 0;
////
////		try {
////
////			for (int b = 0; b < bands; b++) {
////				fileReader.position(bytesPerSample * samples * lines * b);
////
////				for (int l = 0; l < lines; l++) {
////					for (int s = 0; s < samples; s++) {
////
////						sample = readData(dataType, fileReader);
////						//data.putSample(s, l, b, (sample > 0.0 ? sample : 0.0f ));
////					}
////				}
////			}
////
////		} catch (IOException e) {
////			// TODO Auto-generated catch block
////			e.printStackTrace();
////		}
//
//	}

//	protected void readBIP(EnviHeader header, FileReaderUtil fileReader,
//			TerrainData data) {
//		/*
//		 * BIP (Band Interleaved by Pixel Format) Images stored in BIP format
//		 * have the first pixel for all bands in sequential order, followed by
//		 * the second pixel for all bands, followed by the third pixel for all
//		 * bands, etc., interleaved up to the number of pixels. This format
//		 * provides optimum performance for spectral (Z) access of the image
//		 * data.
//		 *
//		 */
//
//		int bands = header.getBands();
//		int lines = header.getLines();
//		int samples = header.getSamples();
//		int dataType = header.getDataType();
//		//float sample = 0;
//		try {
//			for (int l = 0; l < lines; l++) {
//				for (int s = 0; s < samples; s++) {
//					for (int b = 0; b < bands; b++) {
//
//						switch (dataType) {
//
//						case -10:
//						case 1:
//						case 10:
//							data.putSample(s, l, b, fileReader.readByte());
//							break;
//						case 2:
//						case 20:
//							data.putSample(s, l, b, (float) fileReader.readShort());
//							break;
//						case 4:
//							data.putSample(s, l, b, (float) fileReader.readFloat());
//							break;
//						case 8:
//							data.putSample(s, l, b, (float) fileReader.readDouble());
//							break;
//						case 40:
//							data.putSample(s, l, b, (float) fileReader.readInt());
//							break;
//						}
//
//						//sample = readData(dataType, fileReader);
//						//data.putSample(s, l, b, (sample > 0.0 ? sample : 0.0f ));
//
//					}
//
//				}
//			}
//
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//	}

	public Matrix readBIL(SpectralFile spec_file, FileReaderUtil fileReader) {
		/*
		 * 
		 * BIL (Band Interleaved by Line Format) Images stored in BIL format
		 * have the first line of the first band followed by the first line of
		 * the second band, followed by the first line of the third band,
		 * interleaved up to the number of bands. Subsequent lines for each band
		 * are interleaved in similar fashion. This format provides a compromise
		 * in performance between spatial and spectral processing and is the
		 * recommended file format for most ENVI processing tasks.
		 * 
		 * arguments:
		 * 
		 * samples: total number of samples per line (columns) lines: total
		 * number of lines per image (rows) bands: total number of bands
		 * firstPixel: number of the first pixel to read numPixels: number of
		 * consecutive pixels to read after the first pixel numBytes: number of
		 * bytes per sample filename: name of input file
		 * 
		 * return value: a float array of (numPixels x bands) size which
		 * contains all the bands for numPixel number of pixels starting from
		 * firstPixel.
		 * 
		 */

		int dataType = spec_file.getDataType();
		int bytesPerSample = getBytesPerSample(dataType);
		int samples = spec_file.getAcross_track_dim();
		int bands = spec_file.getNumberOfChannels(0);
		int lines = spec_file.getAlong_track_dim();
		//short sample = 0;
		float sample;
		int pixel = 0;

//		DefaultDenseDoubleMatrixFactory factory = new DefaultDenseDoubleMatrixFactory();
//
		Matrix cube = factory.zeros(samples, bands, lines);
//		Matrix cube = ShortMatrix.factory.zeros(samples, bands, lines);

		//AMatrix cube = Matrixx..create(samples, bands);
		
		try {
//			for (int l = 0; l < lines; l++) {
//				for (int s = 0; s < samples; s++) {
//					pixel = (l * samples) + s;
//					fileReader.position(pixel * bytesPerSample * bands);
//					for (int b = 0; b < bands; b++) {
//
//						//sample = fileReader.readShort();
//
//						if(s==1000){
//							int x = 1;
//						};
//
//						sample = readData(dataType, fileReader);
////						data.putSample(s, l, b, (sample > 0.0 ? sample : 0.0f ));
//
//						//cube.setAsShort(sample, s, b, l);
//						cube.setAsDouble(sample, s, b, l);
//
//
//					}
//				}
//			}
			for (int l = 0; l < lines; l++) {
				for (int b = 0; b < bands; b++) {
//					pixel = (l * samples) + s;
//					fileReader.position(pixel * bytesPerSample * bands);
					for (int s = 0; s < samples; s++) {

						//sample = fileReader.readShort();

						if(s==653){
							int x = 1;
						};

						sample = readData(dataType, fileReader);
//						data.putSample(s, l, b, (sample > 0.0 ? sample : 0.0f ));

						if (sample  > 20000)
						{
							int x = 2;
						}

						//cube.setAsShort(sample, s, b, l);
						cube.setAsDouble(sample, s, b, l);


					}
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

//		cube.showGUI();

		return cube;

	}

	protected int getBytesPerSample(int dataType) {
		int bytesPerSample = 0;

		switch (dataType) {
		case -10:
		case 1:
		case 10:
			bytesPerSample = 1;
			break;
		case 2:
		case 12:
		case 20:
			bytesPerSample = 2;
			break;
		case 4:
		case 40:
			bytesPerSample = 4;
			break;
		case 8:
			bytesPerSample = 8;
			break;
		default:
			bytesPerSample = 1;
		}

		return bytesPerSample;
	}

}
