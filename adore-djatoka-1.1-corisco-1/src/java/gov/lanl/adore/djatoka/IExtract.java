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

import gov.lanl.adore.djatoka.util.ImageRecord;

import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Abstract extraction interface. Allows use of common input method
 * (e.g. String path, InputStream). The underlying implementations are 
 * responsible for handling a file path or InputStream and returning 
 * a BufferedImage.
 * @author Ryan Chute
 *
 */
public interface IExtract {

	/**
	 * Extracts region defined in DjatokaDecodeParam as BufferedImage
	 * @param input InputStream containing a JPEG 2000 image bitstream.
	 * @param params DjatokaDecodeParam instance containing region and transform settings.
	 * @return extracted region as a BufferedImage
	 * @throws DjatokaException
	 */
	public BufferedImage process(InputStream input, DjatokaDecodeParam params) throws DjatokaException;
	
	/**
	 * Extracts region defined in DjatokaDecodeParam as BufferedImage
	 * @param input absolute file path of JPEG 2000 image file.
	 * @param params DjatokaDecodeParam instance containing region and transform settings.
	 * @return extracted region as a BufferedImage
	 * @throws DjatokaException
	 */
	public BufferedImage process(String input, DjatokaDecodeParam params) throws DjatokaException;

	/**
	 * Extracts region defined in DjatokaDecodeParam as BufferedImage
	 * @param input ImageRecord wrapper containing file reference, inputstream, etc.
	 * @param params DjatokaDecodeParam instance containing region and transform settings.
	 * @return extracted region as a BufferedImage
	 * @throws DjatokaException
	 */
	public BufferedImage process(ImageRecord input, DjatokaDecodeParam params) throws DjatokaException;
	
	/**
	 * Returns JPEG 2000 width, height, resolution levels in Integer[]
	 * @param  input ImageRecord containing absolute file path of JPEG 2000 image file.
	 * @return a populated ImageRecord object containing width,height,DWT levels of image
	 * @throws DjatokaException
	 */
	public ImageRecord getMetadata(ImageRecord input) throws DjatokaException;
	
	/**
	 * Returns JPEG 2000 XML Box data in String[]
	 * @param input ImageRecord contains a file path or file reference, inputstream, etc.
	 * @return an array of XML box values
	 * @throws DjatokaException
	 */
	public String[] getXMLBox(ImageRecord input) throws DjatokaException;

}