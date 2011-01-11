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

package gov.lanl.adore.djatoka.io.writer;

import gov.lanl.adore.djatoka.io.FormatIOException;
import gov.lanl.adore.djatoka.io.IWriter;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

/**
 * PNG File Writer. Uses JAI Image I/O to write BufferedImage as PNG
 * @author Ryan Chute
 *
 */
public class PNGWriter implements IWriter {
	static Logger logger = Logger.getLogger(PNGWriter.class);
	/**
	 * Write a BufferedImage instance using implementation to the 
	 * provided OutputStream.
	 * @param bi a BufferedImage instance to be serialized
	 * @param os OutputStream to output the image to
	 * @throws FormatIOException
	 */
	public void write(BufferedImage bi, OutputStream os) throws FormatIOException {
		if (bi != null) {
			BufferedOutputStream bos = null;
			try {
				bos = new BufferedOutputStream(os);
				ImageIO.write(bi, "png", bos);
			} catch (IOException e) {
				logger.error(e,e);
			}
		}
	}
	
	/**
	 * NOT SUPPORTED.
	 */
	public void setWriterProperties(Properties props) {
	}
}
