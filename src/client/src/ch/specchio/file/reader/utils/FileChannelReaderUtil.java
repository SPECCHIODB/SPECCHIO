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

import ch.specchio.file.reader.spectrum.SpectralFileLoader;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileChannelReaderUtil implements FileReaderUtil {
	ByteBuffer byteBuffer = null;
	ByteBuffer shortBuffer = null;
	
	ByteBuffer intBuffer = null;
	
	ByteBuffer floatBuffer = null;
	
	ByteBuffer doubleBuffer = null;
	FileChannel channel = null;
	FileInputStream stream = null;
	
	public FileChannelReaderUtil(FileInputStream stream,FileChannel channel) {
		this.stream = stream;
		this.channel = channel;
		byteBuffer = ByteBuffer.allocate(1);
		byteBuffer.order(LITTLE_ENDIAN);
		shortBuffer = ByteBuffer.allocate(2);
		shortBuffer.order(LITTLE_ENDIAN);
		intBuffer = ByteBuffer.allocate(4);
		intBuffer.order(LITTLE_ENDIAN);
		floatBuffer = ByteBuffer.allocate(4);
		floatBuffer.order(LITTLE_ENDIAN);
		doubleBuffer = ByteBuffer.allocate(8);
		doubleBuffer.order(LITTLE_ENDIAN);
		
	}
	
	
	@Override
	public byte readByte() throws IOException {
		byteBuffer.rewind();
		channel.read(byteBuffer);
		byteBuffer.rewind();
		return byteBuffer.get();
	}

	@Override
	public byte readByte(int position) throws IOException {
		channel.position(position);
		return readByte();
	}

	@Override
	public double readDouble() throws IOException {
		doubleBuffer.rewind();
		channel.read(doubleBuffer);
		doubleBuffer.rewind();
		return doubleBuffer.getDouble();
	}

	@Override
	public double readDouble(int position) throws IOException {
		channel.position(position);
		return readDouble();
	}

	@Override
	public float readFloat(int position) throws IOException {
		channel.position(position);
		return readFloat();
	}

	@Override
	public float readFloat() throws IOException {
		floatBuffer.rewind();
		channel.read(floatBuffer);
		floatBuffer.rewind();
		return floatBuffer.getFloat();
	}

	@Override
	public int readInt(int position) throws IOException {
		channel.position(position);
		return readInt();
	}

	@Override
	public int readInt() throws IOException {
		intBuffer.rewind();
		channel.read(intBuffer);
		intBuffer.rewind();
		return intBuffer.getInt();
	}

	@Override
	public short readShort() throws IOException {
		shortBuffer.rewind();
		channel.read(shortBuffer);
		shortBuffer.rewind();

		shortBuffer.array();

		//byte[] b = new byte[2];

		short s = SpectralFileLoader.arr2short(shortBuffer.array(), 0);

		return s;

	//	return shortBuffer.getShort();
	}

	@Override
	public short readShort(int position) throws IOException {
		channel.position(position);
		return readShort();
	}

	@Override
	public void position(int position) throws IOException {
		channel.position(position);
		
	}


	@Override
	public void close() throws IOException {
		channel.close();
		stream.close();
	}


	@Override
	public long position() throws IOException {
		
		return channel.position();
	}


	@Override
	public long remaining() throws IOException {
		
		return channel.size() - channel.position();
	}
}
