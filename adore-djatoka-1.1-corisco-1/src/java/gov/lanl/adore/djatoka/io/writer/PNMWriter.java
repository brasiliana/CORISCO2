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

import org.apache.log4j.Logger;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.PNMEncodeParam;

/**
 * PNM File Writer. Uses JAI to write BufferedImage as PNM
 * @author Ryan Chute
 *
 */
public class PNMWriter implements IWriter {
	static Logger logger = Logger.getLogger(PNMWriter.class);
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
				PNMEncodeParam param = new PNMEncodeParam();
				param.setRaw(true);
				ImageEncoder enc = ImageCodec.createImageEncoder("PNM", bos, param);
				enc.encode(bi);
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
