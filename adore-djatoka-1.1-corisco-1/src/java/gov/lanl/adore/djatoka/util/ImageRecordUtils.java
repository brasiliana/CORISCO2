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

package gov.lanl.adore.djatoka.util;

import ij.ImagePlus;
import ij.io.Opener;
import ij.process.ImageProcessor;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

/**
 * Image Information Utility used to obtain width and height information.
 * This util is useful when processing images using external compression
 * applications, such as Kakadu JPEG 2000 kdu_compress.  The image to be
 * compressed is opened by either ImageJ or JAI and determines dimensions.
 * JAI is better with TIFF files and and file extensions are used to
 * determine which API will be used to resolve dimensions.
 * @author Ryan Chute
 *
 */
public class ImageRecordUtils {
	
	/**
	 * Return an ImageRecord containing the images pixel dimensions.
	 * @param file absolute file path to image
	 * @return ImageRecord containing the images pixel dimensions
	 */
	public static ImageRecord getImageDimensions(String file) {
		ImageRecord dim = null;
		// try JAI
		dim = setUsingJAI(file);
		// if that fails, try ImageJ
		if (dim == null)
			dim = setUsingImageJ(file);
		return dim;
	}
	
	private static ImageRecord setUsingImageJ(String file) {
		ImageRecord dim = new ImageRecord(file);
		Opener o = new Opener();
		ImagePlus imp = o.openImage(file);
		if (imp == null)
			return null;
		ImageProcessor ip = imp.getProcessor();
		dim.setWidth(ip.getWidth());
		dim.setHeight(ip.getHeight());
		ip = null;
		return dim;
	}
	
	private static ImageRecord setUsingJAI(String file) {
		ImageRecord dim = new ImageRecord(file);
		PlanarImage pi = JAI.create("fileload", file);
		dim.setWidth(pi.getWidth());
		dim.setHeight(pi.getHeight());
		pi.dispose();
		pi = null;
		return dim;
	}
}

