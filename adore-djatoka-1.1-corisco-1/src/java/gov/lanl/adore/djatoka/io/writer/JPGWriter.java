/*
 * Copyright (c) 2008  Los Alamos National Security, LLC.
 * With modifications by Brasiliana Digital Library (http://brasiliana.usp.br), 2010.
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

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.apache.log4j.Logger;

/**
 * JPG File Writer. Uses JAI to write BufferedImage as JPG
 * @author Ryan Chute
 *
 */
public class JPGWriter implements IWriter {
	static Logger logger = Logger.getLogger(JPGWriter.class);
	public static final int DEFAULT_QUALITY_LEVEL = 85;
	private int q = DEFAULT_QUALITY_LEVEL;
	
	/**
	 * Write a BufferedImage instance using implementation to the 
	 * provided OutputStream.
	 * @param bi a BufferedImage instance to be serialized
	 * @param os OutputStream to output the image to
	 * @throws FormatIOException
	 */
	public void write(BufferedImage bi, OutputStream os) throws FormatIOException {
		//writeUsingJAI(bi, os);
        writeUsingImageIO(bi, os);
	}
	
	/* the JAI implementation is a bit faster */
    /*
	private void writeUsingJavaAPI(BufferedImage bi, OutputStream os) throws FormatIOException {
		if (bi != null) {
			BufferedOutputStream bos = null;
			bos = new BufferedOutputStream(os);
		    JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(bos);
            JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(bi);
            param.setQuality((float)(q/100.0), true);
            try {
				encoder.encode(bi, param);
			} catch (ImageFormatException e) {
				logger.error(e);
				throw new FormatIOException(e);
			} catch (IOException e) {
				logger.error(e);
				throw new FormatIOException(e);
			}
		}
	}
	
    /*
	private void writeUsingJAI(BufferedImage bi, OutputStream os) throws FormatIOException {
		if (bi != null) {
			BufferedOutputStream bos = null;
			try {
				bos = new BufferedOutputStream(os);
				com.sun.media.jai.codec.JPEGEncodeParam param = new com.sun.media.jai.codec.JPEGEncodeParam();
				param.setQuality((float)(q/100.0));
				ImageEncoder enc = ImageCodec.createImageEncoder("jpeg", bos, param);
				enc.encode(bi);
			} catch (IOException e) {
				logger.error(e);
			}
		}
	}*/

        private void writeUsingImageIO(BufferedImage bi, OutputStream os) throws FormatIOException {
                if (bi != null) {
                        ImageWriter writer = ImageIO.getImageWritersBySuffix("jpeg").next();
                        JPEGImageWriteParam param = new JPEGImageWriteParam(null);
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionQuality((float)(q/100.0));
                        ImageOutputStream ios = null;
                        try{
                                ios = new MemoryCacheImageOutputStream(os);
                                writer.setOutput(ios);
                                writer.write((IIOMetadata)null, new IIOImage(bi, null, null), param);
                        } catch (IOException e) {
                                logger.error(e,e);
                        }
                        finally {
                                if (ios != null){
                                        try{
                                                ios.flush();
                                                ios.close();
                                        } catch (IOException e) {
                                                logger.error(e,e);
                                                throw new FormatIOException(e);
                                        }
                                }
                        }
                }
        }

	/**
	 * Set the Writer Implementations Serialization properties. Only JPGWriter.quality_level
	 * is supported in this implementation.
	 * @param props writer serialization properties
	 */
	public void setWriterProperties(Properties props) {
		if (props.containsKey("JPGWriter.quality_level")) {
			q = Integer.parseInt((String)props.get("JPGWriter.quality_level"));
		}
	}
}
