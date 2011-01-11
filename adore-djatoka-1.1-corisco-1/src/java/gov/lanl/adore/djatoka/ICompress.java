/*
 * Copyright (c) 2008  Los Alamos National Security, LLC.
 *
 * Los Alamos National Laboratory
 * Research Library
 * Digital Library Research & Prototyping Team
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 * 
 */

package gov.lanl.adore.djatoka;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Abstract compression interface. Allows use of common input method
 * (e.g. String path, InputStream, BufferedImage) and output methods. The 
 * underlying implementations are responsible for handling these variants.
 * @author Ryan Chute
 *
 */
public interface ICompress {

	/**
	 * Compress input using provided DjatokaEncodeParam parameters.
	 * @param input absolute file path for input file.
	 * @param output absolute file path for output file.
	 * @param params DjatokaEncodeParam containing compression parameters.
	 * @throws DjatokaException
	 */
	public void compressImage(String input, String output,
			DjatokaEncodeParam params) throws DjatokaException;

	/**
	 * Compress input using provided DjatokaEncodeParam parameters.
	 * @param input InputStream containing image bitstream
	 * @param output absolute file path for output file.
	 * @param params DjatokaEncodeParam containing compression parameters.
	 * @throws DjatokaException
	 */
	public void compressImage(InputStream input, String output,
			DjatokaEncodeParam params) throws DjatokaException;

	/**
	 * Compress input using provided DjatokaEncodeParam parameters.
	 * @param input InputStream containing image bitstream
	 * @param output OutputStream to serialize compressed image.
	 * @param params DjatokaEncodeParam containing compression parameters.
	 * @throws DjatokaException
	 */
	public void compressImage(InputStream input, OutputStream output,
			DjatokaEncodeParam params) throws DjatokaException;

	/**
	 * Compress input BufferedImage using provided DjatokaEncodeParam parameters.
	 * @param bi in-memory image to be compressed
	 * @param output OutputStream to serialize compressed image.
	 * @param params DjatokaEncodeParam containing compression parameters.
	 * @throws DjatokaException
	 */
	public void compressImage(BufferedImage bi, OutputStream output,
			DjatokaEncodeParam params) throws DjatokaException;

	/**
	 * Compress input BufferedImage using provided DjatokaEncodeParam parameters.
	 * @param bi in-memory image to be compressed
	 * @param output absolute file path for output file.
	 * @param params DjatokaEncodeParam containing compression parameters.
	 * @throws DjatokaException
	 */
	public void compressImage(BufferedImage bi, String output,
			DjatokaEncodeParam params) throws DjatokaException;

}