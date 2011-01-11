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

import gov.lanl.adore.djatoka.DjatokaEncodeParam;
import gov.lanl.adore.djatoka.DjatokaException;
import gov.lanl.adore.djatoka.io.FormatIOException;
import gov.lanl.adore.djatoka.io.IWriter;
import gov.lanl.adore.djatoka.kdu.KduCompressExe;
import gov.lanl.adore.djatoka.util.ImageProcessingUtils;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * JP2 File Writer. Uses KduCompressExe to write BufferedImage as JP2
 * @author Ryan Chute
 *
 */
public class JP2Writer implements IWriter {
	static Logger logger = Logger.getLogger(JP2Writer.class);
	private DjatokaEncodeParam params = new DjatokaEncodeParam();
	
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
				params.setLevels(ImageProcessingUtils.getLevelCount(bi.getWidth(), bi.getHeight()));
				KduCompressExe encoder = new KduCompressExe();
				bos = new BufferedOutputStream(os);
				encoder.compressImage(bi, bos, params);
				bos.close();
			} catch (IOException e) {
				logger.error(e,e);
				throw new FormatIOException(e);
			} catch (DjatokaException e) {
				logger.error(e,e);
				throw new FormatIOException(e);
			} catch (Exception e) {
				logger.error(e,e);
				throw new FormatIOException(e);
			}
		}
	}
	
	/**
	 * Set the Writer Implementations Serialization properties. See djatoka.properties for key/value examples.
	 * @param props writer serialization properties
	 */
	public void setWriterProperties(Properties props) {
		params = new DjatokaEncodeParam(props);
	}
}
