/*
 * Copyright (c) 2009  Los Alamos National Security, LLC.
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

package gov.lanl.adore.djatoka.io.reader;

import gov.lanl.adore.djatoka.io.FormatIOException;
import gov.lanl.adore.djatoka.io.IReader;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import org.apache.log4j.Logger;

/**
 * Djatoka Reader Wrapper - Uses known IReader impl. to  
 * read image InputStream or image file path.
 * @author Ryan Chute
 *
 */
public class DjatokaReader implements IReader {
	static Logger logger = Logger.getLogger(DjatokaReader.class);
	private ImageJReader imagejReader = new ImageJReader();
	private ImageIOReader imageioReader = new ImageIOReader();
	
	/**
	 * Returns a BufferedImage instance for provided InputStream
	 * @param input an InputStream consisting of an image bitstream
	 * @return a BufferedImage instance for source image InputStream
	 * @throws FormatIOException
	 */
	public BufferedImage open(String input) throws FormatIOException {
		BufferedImage bi = null;
		// Try ImageIO
		logger.debug("Reading the file using ImageJReader");
		bi = imagejReader.open(input);
		// If null, try ImageJ
		if (bi == null) {
			logger.debug("Unable to open using ImageJReader, trying ImageIOReader");
			bi = imageioReader.open(input);
		}
		return bi;
	}

	/**
	 * Returns a BufferedImage instance for provided image file path
	 * @param input absolute file path for image file
	 * @return a BufferedImage instance for source image file
	 * @throws FormatIOException
	 */
	public BufferedImage open(InputStream input) throws FormatIOException {
		BufferedImage bi = null;
		// Try ImageIO
		logger.debug("Reading the file using ImageJReader");
		bi = imagejReader.open(input);
		// If null, try ImageJ
		if (bi == null) {
			logger.debug("Unable to open using ImageJReader, trying ImageIOReader");
			bi = imageioReader.open(input);
		}
		return bi;
	}
}
