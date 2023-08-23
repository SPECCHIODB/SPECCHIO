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
///map info = {UTM, 1.000, 1.000, 681506.250, 2023827.750, 2.8500000000e+001, 2.8500000000e+001, 19, North, units=Meters}
//lists geographic coordinates information in the order of 
// projection name (UTM), 
// reference pixel x location in file coordinates, 
// reference pixel y, 
// pixel easting, 
// pixel northing, 
// x pixel size,
// y pixel size, 
// Projection Zone, 
// "North" or "South" for UTM only.
public class MapInfo {

	protected String projectionName;
	protected float referenceX;
	protected float referenceY;
	protected double easting;
	protected double northing;
	protected double pixelSizeX;
	protected double pixelSizeY;
	protected int projectionZone;
	protected String hemisphere;
	protected String units;
	public String getProjectionName() {
		return projectionName;
	}
	public float getReferenceX() {
		return referenceX;
	}
	public float getReferenceY() {
		return referenceY;
	}
	public double getEasting() {
		return easting;
	}
	public double getNorthing() {
		return northing;
	}
	public double getPixelSizeX() {
		return pixelSizeX;
	}
	public double getPixelSizeY() {
		return pixelSizeY;
	}
	public int getProjectionZone() {
		return projectionZone;
	}
	public String getHemisphere() {
		return hemisphere;
	}
	public String getUnits() {
		return units;
	}
	@Override
	public String toString() {
		
		return "Map Info: "+projectionName+", "+referenceX+", "+referenceY+", "+easting+", "+northing+", "+pixelSizeX+", "+pixelSizeY+", "+projectionZone+", "+hemisphere+", untis="+units;
	}
	
}
