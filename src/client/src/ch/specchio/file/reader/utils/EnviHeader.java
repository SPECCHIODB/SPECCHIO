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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


//public final static int 



// Data type: parameter identifying the type of data representation, where 
//	1=8 bit byte; 
//	2=16-bit signed integer; 
//	3=32-bit signed long integer; 
//	4=32-bit floating point; 
//	5=64- bit double precision floating point; 
//	6=2x32-bit complex, real-imaginary pair of double precision; 
//	9=2x64-bit double precision complex, real-imaginary pair of double precision; 
//	12=16-bit unsigned integer; 
//	13=32-bit unsigned long integer; 
//	14=64-bit unsigned integer; 
//	15=64-bit unsigned long integer.

public class EnviHeader {

	private String description =null;
	private int samples = 0;
	private int lines = 0;
	private int bands =0;
	private int headerOffset = 0;
	private String fileType = null;
	private int dataType = 0;
	private String interleave = null;
	private String sensorType = null;
	private int byteOrder = 0;
	private int xStart = 0;
	private int yStart = 5476;
	private String wavelengthUnits = null;
	private MapInfo mapInfo = null;
	private List<String> bandNames = null;
	private List<String> wavelengths = null;
	private String headerFilename = null;
	private String dataFilename = null;
	
	//private ProjectionInfo projectionInfo;
	
	public static EnviHeader readHeaderFile(File file) {
		EnviHeader header = new EnviHeader();
		String headerFullPath = file.getAbsolutePath();
		header.headerFilename = file.getName();
		header.dataFilename = headerFullPath.substring(0,headerFullPath.length()-4);
		
		boolean valid = false;
		try {
			FileInputStream input = new FileInputStream(file);
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
			String line = null;
			
			
			while((line = reader.readLine())!= null) {
				//lines.add(line);
				//System.out.println(line);
		
				if(line.startsWith("ENVI")) {
					valid = true;
				}
				else if (line.startsWith("description")) {
					header.description = parseBlock(reader);
				}
				else if (line.startsWith("sample")) {
					header.samples = Integer.parseInt(parseValue("sample",line));
				}
				else if (line.startsWith("lines" )) {
					header.lines = Integer.parseInt(parseValue("lines",line));
				}
				else if (line.startsWith("bands")) {
					header.bands = Integer.parseInt(parseValue("bands",line));
				}
				else if (line.startsWith("header offset")) {
					header.headerOffset = Integer.parseInt(parseValue("header offset",line));
				}
				else if (line.startsWith("file type")) {
					header.fileType = parseValue("file type",line);
				}
				else if (line.startsWith("interleave")) {
					header.interleave = parseValue("interleave",line);
				}
				else if (line.startsWith("sensor type")) {
					header.sensorType = parseValue("sensor type",line);
				}
				else if (line.startsWith("data type")) {
					header.dataType = Integer.parseInt(parseValue("data type",line));
				}
				else if (line.startsWith("byte order")) {
					header.byteOrder = Integer.parseInt(parseValue("byte order",line));
				}
				else if (line.startsWith("x start")) {
					header.xStart = Integer.parseInt(parseValue("x start",line));
				}
				else if (line.startsWith("y start")) {
					header.yStart = Integer.parseInt(parseValue("y start",line));
				}
				else if (line.startsWith("map info")) {
					header.mapInfo = parseMapInfoLine(parseValue("map info",line));
				}
				else if (line.startsWith("wavelength units")) {
					header.wavelengthUnits = parseValue("wavelength units",line);
				}
				else if (line.startsWith("band names")) {
					header.bandNames = parseList(parseBlock(reader));
				}
				else if (line.startsWith("wavelength")) {
					header.wavelengths = parseList(parseBlock(reader));
				}
			}
			
			reader.close();
			input.close();
			
		} catch (FileNotFoundException e) {
		
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(!valid) {
			int should_catch_this = 1;
		}
		return header;
	}


	private static List<String> parseList(String line) {
		List<String> list = new ArrayList<String>();
		StringTokenizer tokenizer = new StringTokenizer(line);
		String token;
		while(tokenizer.hasMoreTokens()) {
			token = tokenizer.nextToken(",").trim();
			list.add(token);
			//System.out.println(token);
		}
		return list;
	}
	
	private static String parseBlock (BufferedReader reader) throws IOException {
		String line = reader.readLine().trim();
		
		StringBuffer block = new StringBuffer();
		while (!line.endsWith("}")) {
			block.append(line);
			line = reader.readLine().trim();
		}
		block.append(line.substring(0, line.length()-1));
		
		return block.toString();
	}
	private static MapInfo parseMapInfoLine(String line) {
		//map info = {UTM, 1.000, 1.000, 681506.250, 2023827.750, 2.8500000000e+001, 2.8500000000e+001, 19, North, units=Meters}
		
	
		List<String> list = parseList(line.substring(1,line.length()-1));
		
		MapInfo info = new MapInfo();
		info.projectionName = list.get(0);
		info.referenceX = Float.parseFloat(list.get(1));
		info.referenceY = Float.parseFloat(list.get(2));
		info.easting = Double.parseDouble(list.get(3));
		info.northing = Double.parseDouble(list.get(4));
		info.pixelSizeX = Double.parseDouble(list.get(5));
		info.pixelSizeY = Double.parseDouble(list.get(6));
		info.projectionZone = Integer.parseInt(list.get(7));
		info.hemisphere = list.get(8);
		info.units = list.get(9).substring("untis=".length());
		//System.out.println(info);
		return info;
	}


	private static String parseValue(String name,String line) {
		String value =line.substring(line.indexOf("=")+1).trim();
		
		return value;
		
	}


	public String getDescription() {
		return description;
	}


	public int getSamples() {
		return samples;
	}


	public int getLines() {
		return lines;
	}


	public int getBands() {
		return bands;
	}


	public int getHeaderOffset() {
		return headerOffset;
	}


	public String getFileType() {
		return fileType;
	}


	public int getDataType() {
		return dataType;
	}


	public String getInterleave() {
		return interleave;
	}


	public String getSensorType() {
		return sensorType;
	}


	public int getByteOrder() {
		return byteOrder;
	}


	public int getXStart() {
		return xStart;
	}


	public int getYStart() {
		return yStart;
	}


	public String getWavelengthUnits() {
		return wavelengthUnits;
	}


	public MapInfo getMapInfo() {
		return mapInfo;
	}


	public List<String> getBandNames() {
		return bandNames;
	}


	public List<String> getWavelengths() {
		return wavelengths;
	}


	public String getHeaderFilename() {
		return headerFilename;
	}


	public String getDataFilename() {
		return dataFilename;
	}
	

}
