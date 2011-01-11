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
import gov.lanl.adore.djatoka.util.IOUtils;
import ij.ImagePlus;
import ij.io.Opener;
import ij.process.ImageProcessor;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

import net.sf.ij.jaiio.JAIReader;

import org.apache.log4j.Logger;

/**
 * Use ImageJ API to read image InputStream or image file path.
 * @author Ryan Chute
 *
 */
public class ImageJReader implements IReader {
	static Logger logger = Logger.getLogger(ImageJReader.class);
	/**
	 * Returns a BufferedImage instance for provided InputStream
	 * @param input an InputStream consisting of an image bitstream
	 * @return a BufferedImage instance for source image InputStream
	 * @throws FormatIOException
	 */
	public BufferedImage open(InputStream input) throws FormatIOException {
		Opener o = new Opener();
		BufferedImage bi = null;
		// Most of the time we're dealing with TIF so try direct
		ImagePlus imp = o.openTiff(input, "tif");
		// Otherwise, we'll just stay in ImageJ but just provide a file path
		if (imp == null) {
			logger.info("Creating temp image");
			File path = IOUtils.createTempImage(input);
			bi = open(path.getAbsolutePath());
			// Clean-up the temp file if we made one.
			if (path != null)
				path.delete();
		} else
		    bi = open(imp);
		
		return bi;
	}
	
	/**
	 * Returns a BufferedImage instance for provided image file path
	 * @param input absolute file path for image file
	 * @return a BufferedImage instance for source image file
	 * @throws FormatIOException
	 */
	public BufferedImage open(String input) throws FormatIOException {
		ImagePlus ip;
		try {
			ip = JAIReader.read(new File(input))[0];
		} catch (Exception e) {
			logger.error(e,e);
			throw new FormatIOException(e);
		}
		return open(ip);
	}
	
	/**
	 * Internal ImagePlus processing to populate BufferedIMage using Graphics objects
	 * @param imp an ImageJ ImagePlus object
	 * @return a BufferedImage of type BufferedImage.TYPE_3BYTE_BGR
	 * @throws FormatIOException
	 */
	private BufferedImage open(ImagePlus imp) throws FormatIOException {
		if (imp == null) {
			logger.error("Null ImagePlus Object.");
			throw new FormatIOException("Null ImagePlus Object.");
		}
		ImageProcessor ip = imp.getProcessor();
		int width = ip.getWidth();
		int height = ip.getHeight();
		Image img = ip.createImage();
		imp.flush();
		imp = null;
		ip = null;
		
		BufferedImage bImg = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		Graphics g = bImg.getGraphics();
		g.drawImage(img, 0, 0, null);
		img.flush();
		img = null;
		g.dispose();
		g = null;

		return bImg;
	}
}
