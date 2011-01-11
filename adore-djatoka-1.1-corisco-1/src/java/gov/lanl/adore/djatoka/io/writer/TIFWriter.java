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
import ij.ImagePlus;
import ij.io.TiffEncoder;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.log4j.Logger;

import uk.co.mmscomputing.imageio.tiff.TIFFImageWriterSpi;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.TIFFEncodeParam;

/**
 * TIF File Writer. Uses JAI to write BufferedImage as TIF
 * @author Ryan Chute
 *
 */
public class TIFWriter implements IWriter {
	static Logger logger = Logger.getLogger(TIFWriter.class);
	/**
	 * Write a BufferedImage instance using implementation to the 
	 * provided OutputStream.
	 * @param bi a BufferedImage instance to be serialized
	 * @param os OutputStream to output the image to
	 * @throws FormatIOException
	 */
	public void write(BufferedImage bi, OutputStream os) throws FormatIOException {
		writeUsingImageIO(bi, os);
		//writeUsingJAI(bi, os);
		//writeUsingImageJ(bi, os);
		//writeUsingMMCComputingImageIO(bi, os);
	}
	
	private void writeUsingImageJ(BufferedImage bi, OutputStream os) throws FormatIOException {
		ImagePlus imp = new ImagePlus("tempTif", bi);
		TiffEncoder encoder = new TiffEncoder(imp.getFileInfo());
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(os));
		try {
			encoder.write(out);
		} catch (IOException e) {
			logger.error(e);
			throw new FormatIOException(e);
		}
	}
	
	private void writeUsingImageIO(BufferedImage bi, OutputStream os) throws FormatIOException {
		if (bi != null) {
			BufferedOutputStream bos = null;
			try {
				bos = new BufferedOutputStream(os);
				ImageIO.write(bi, "tif", bos);
			} catch (IOException e) {
				logger.error(e,e);
			} finally {
				if (bos != null) {
					try {
						bos.flush();
						bos.close();
					} catch (IOException e) {
						logger.error(e,e);
						throw new FormatIOException(e);
					}
				}
			}
		}
	}
	
	/* issue with using this serialization for kakadu input */
	private void writeUsingJAI(BufferedImage bi, OutputStream os) throws FormatIOException {
		if (bi != null) {
			BufferedOutputStream bos = null;
			try {
				bos = new BufferedOutputStream(os);
				TIFFEncodeParam param = new TIFFEncodeParam();
				ImageEncoder enc = ImageCodec.createImageEncoder("TIFF", bos, param);
				enc.encode(bi);
			} catch (IOException e) {
				logger.error(e,e);
			} finally {
				if (bos != null) {
					try {
						bos.flush();
						bos.close();
					} catch (IOException e) {
						logger.error(e,e);
						throw new FormatIOException(e);
					}
				}
			}
		} 
	}
	
    private void writeUsingMMCComputingImageIO(BufferedImage bi, OutputStream os) throws FormatIOException {
        TIFFImageWriterSpi tiffspi = new TIFFImageWriterSpi();
        ImageOutputStream ios = null;
        ImageWriter writer;
        try {
            writer = tiffspi.createWriterInstance();
            ios = ImageIO.createImageOutputStream(os);
            writer.setOutput(ios);
            writer.write(bi);
        } catch (IOException e) {
            logger.error(e,e);
        } finally {
            if (ios != null) {
                try {
                    ios.flush();
                    ios.close();
                } catch (IOException e) {
                    logger.error(e,e);
                    throw new FormatIOException(e);
                }
            }
        }

    } 
	
	/**
	 * NOT SUPPORTED. 
	 * TODO: Add support for key TIFF properties
	 */
	public void setWriterProperties(Properties props) {
	}
}
