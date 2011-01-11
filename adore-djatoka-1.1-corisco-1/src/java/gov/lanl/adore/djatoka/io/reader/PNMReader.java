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

package gov.lanl.adore.djatoka.io.reader;

import gov.lanl.adore.djatoka.io.FormatIOException;
import gov.lanl.adore.djatoka.io.IReader;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.media.jai.RenderedImageAdapter;

import org.apache.log4j.Logger;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;

/**
 * Returns BufferedImage give a PNM image InputStream or file path. 
 * Uses JAI to open PNM formatted images.
 * @author Ryan Chute
 *
 */
public class PNMReader implements IReader{
	static Logger logger = Logger.getLogger(PNMReader.class);
	/**
	 * Returns a BufferedImage instance for provided image file path.
	 * @param input absolute file path for image file
	 * @return a BufferedImage instance for source image file
	 * @throws FormatIOException
	 */
	public BufferedImage open(String input) throws FormatIOException {
		RenderedImageAdapter aid = null;
		try {
			ImageDecoder enc = ImageCodec.createImageDecoder("PNM", new File(input), null);
			aid = new RenderedImageAdapter(enc.decodeAsRenderedImage());
		} catch (IOException e) {
			return null;
		}
		return aid.getAsBufferedImage();
	}

	/**
	 * Returns a BufferedImage instance for provided InputStream
	 * @param input an InputStream consisting of an image bitstream
	 * @return a BufferedImage instance for source image InputStream
	 * @throws FormatIOException
	 */
	public BufferedImage open(InputStream input) throws FormatIOException {
		RenderedImageAdapter aid = null;
		try {
			ImageDecoder enc = ImageCodec.createImageDecoder("PNM", input, null);
			aid = new RenderedImageAdapter(enc.decodeAsRenderedImage());
		} catch (IOException e) {
			return null;
		}
		return aid.getAsBufferedImage();
	}
}
