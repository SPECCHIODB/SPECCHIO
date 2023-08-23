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

import java.io.IOException;

public interface FileReaderUtil {
	void close() throws IOException;
	long remaining() throws IOException;
	long position() throws IOException;
	byte readByte()throws IOException ;
	byte readByte(int position) throws IOException;
	short readShort() throws IOException ;
	short readShort(int position) throws IOException;
	int readInt(int position) throws IOException;
	int readInt() throws IOException ;
	float readFloat (int position) throws IOException;
	float readFloat () throws IOException;
	double readDouble () throws IOException ;
	double readDouble (int position) throws IOException;
	void position(int position) throws IOException;	
}
