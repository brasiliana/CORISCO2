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

import gov.lanl.adore.djatoka.io.FormatFactory;
import gov.lanl.adore.djatoka.io.FormatWriterParams;
import gov.lanl.adore.djatoka.io.IWriter;
import gov.lanl.adore.djatoka.util.IOUtils;
import gov.lanl.adore.djatoka.util.ImageProcessingUtils;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

/**
 * Extraction Processor - Intermediate processor between DjatokaExtract 
 * and IExtract implementation. Works with the  format factory to convert 
 * the extracted region to desired output directory.  Handles I/O and post 
 * extraction transform.
 * @author Ryan Chute
 *
 */
public class DjatokaExtractProcessor {
	static Logger logger = Logger.getLogger(DjatokaExtractProcessor.class);
	private static String STDIN = "/dev/stdin";
	private static FormatFactory fmtFactory = new FormatFactory();
	private IExtract extractImpl;

	/**
	 * Constructor requiring an IExtract implementation
	 * @param impl an IExtract implementation
	 */
	public DjatokaExtractProcessor(IExtract impl) {
		this.extractImpl = impl;
	}
	
	/**
	 * Sets the format factory used to serialize extracted region
	 * @param ff the format factory used to serialize extracted region
	 * @throws DjatokaException
	 */
	public void setFormatFactory(FormatFactory ff) throws DjatokaException {
		fmtFactory = ff;
	}

	/**
	 * Extract region or resolution level from JPEG 2000 image file.
	 * @param input absolute file path for input file.
	 * @param output absolute file path for output file.
	 * @param params DjatokaDecodeParam instance containing region and transform settings.
	 * @param fmtId mimetype identifier of output file format (e.g. "image/jpeg")
	 * @throws DjatokaException
	 */
	public void extractImage(String input, String output,
			DjatokaDecodeParam params, String fmtId) throws DjatokaException {
		IWriter w = fmtFactory.getWriter(fmtId);
		extractImage(input, output, params, w);
	}

	/**
	 * Extract region or resolution level from JPEG 2000 image file.
	 * @param input absolute file path for input file.
	 * @param output absolute file path for output file.
	 * @param params DjatokaDecodeParam instance containing region and transform settings.
	 * @param outputParams
	 * @throws DjatokaException
	 */
	public void extractImage(String input, String output,
			DjatokaDecodeParam params, FormatWriterParams outputParams)
			throws DjatokaException {
		IWriter w = fmtFactory.getWriter(outputParams.getFormatId(),
				outputParams.getFormatProps());
		extractImage(input, output, params, w);
	}

	/**
	 * Extract region or resolution level from JPEG 2000 image file.
	 * @param input InputStream containing a JPEG 2000 image bitstream.
	 * @param output absolute file path for output file.
	 * @param params DjatokaDecodeParam instance containing region and transform settings.
	 * @param fmtId mimetype identifier of output file format (e.g. "image/jpeg")
	 * @throws DjatokaException
	 */
	public void extractImage(InputStream input, OutputStream output,
			DjatokaDecodeParam params, String fmtId) throws DjatokaException {
		IWriter w = fmtFactory.getWriter(fmtId);
		extractImage(input, output, params, w);
	}

	/**
	 * Extract region or resolution level from JPEG 2000 image file.
	 * @param input absolute file path for input file.
	 * @param os OutputStream to serialize formatted output image to.
	 * @param params DjatokaDecodeParam instance containing region and transform settings.
	 * @param fmtId mimetype identifier of output file format (e.g. "image/jpeg")
	 * @throws DjatokaException
	 */
	public void extractImage(String input, OutputStream os,
			DjatokaDecodeParam params, String fmtId) throws DjatokaException {
		IWriter w = fmtFactory.getWriter(fmtId);
		extractImage(input, os, params, w);
	}

	/**
	 * Extract region or resolution level from JPEG 2000 image file.
	 * @param input absolute file path for input file.
	 * @param output absolute file path for output file.
	 * @param params DjatokaDecodeParam instance containing region and transform settings.
	 * @param w format writer to be used to serialize extracted region.
	 * @throws DjatokaException
	 */
	public void extractImage(String input, String output,
			DjatokaDecodeParam params, IWriter w) throws DjatokaException {
		File in = null;
		String dest = output;

		if (input.equals(STDIN)) {
			try {
				in = File.createTempFile("tmp", ".jp2");
				input = in.getAbsolutePath();
				in.deleteOnExit();
				IOUtils.copyFile(new File(STDIN), in);
			} catch (IOException e) {
				logger.error("Unable to process image from " + STDIN + ": " + e.getMessage());
				throw new DjatokaException(e);
			}
		}

		BufferedImage bi = extractImpl.process(input, params);
		if (bi != null) {
			if (params.getScalingFactor() != 1.0 || params.getScalingDimensions() != null)
				bi = applyScaling(bi, params);
			if (params.getTransform() != null)
				bi = params.getTransform().run(bi);
			try {
				BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(new File(dest)));
				w.write(bi, os);
				os.close();
			} catch (FileNotFoundException e) {
				logger.error("Requested file was not found: " + dest);
				throw new DjatokaException(e);
			} catch (IOException e) {
				logger.error("Error attempting to close: " + dest);
				throw new DjatokaException(e);
			}
		}

		if (in != null)
			in.delete();
	}

	/**
	 * Extract region or resolution level from JPEG 2000 image file.
	 * @param input input absolute file path for input file.
	 * @param os OutputStream to serialize formatted output image to.
	 * @param params DjatokaDecodeParam instance containing region and transform settings.
	 * @param w format writer to be used to serialize extracted region.
	 * @throws DjatokaException
	 */
	public void extractImage(String input, OutputStream os,
			DjatokaDecodeParam params, IWriter w) throws DjatokaException {
		File in = null;

		// If coming in from stdin, copy to tmp file
		if (input.equals(STDIN)) {
			try {
				in = File.createTempFile("tmp", ".jp2");
				input = in.getAbsolutePath();
				in.deleteOnExit();
				IOUtils.copyFile(new File(STDIN), in);
			} catch (IOException e) {
				logger.error("Unable to process image from " + STDIN + ": " + e.getMessage());
				throw new DjatokaException(e);
			}
		}

		BufferedImage bi = extractImpl.process(input, params);
		if (bi != null) {
			if (params.getScalingFactor() != 1.0 || params.getScalingDimensions() != null)
				bi = applyScaling(bi, params);
			if (params.getTransform() != null)
				bi = params.getTransform().run(bi);
			w.write(bi, os);
		}

		if (in != null)
			in.delete();
	}

	/**
	 * Extract region or resolution level from JPEG 2000 image file.
	 * @param input input absolute file path for input file.
	 * @param os OutputStream to serialize formatted output image to.
	 * @param params DjatokaDecodeParam instance containing region and transform settings.
	 * @param w format writer to be used to serialize extracted region.
	 * @throws DjatokaException
	 */
	public void extractImage(InputStream input, OutputStream os,
			DjatokaDecodeParam params, IWriter w) throws DjatokaException {
		BufferedImage bi = extractImpl.process(input, params);
		if (bi != null) {
			if (params.getScalingFactor() != 1.0 || params.getScalingDimensions() != null)
				bi = applyScaling(bi, params);
			if (params.getTransform() != null)
				bi = params.getTransform().run(bi);
			w.write(bi, os);
		}
	}
	
	/**
	 * Apply scaling, if Scaling Factor != to 1.0 then check ScalingDimensions 
	 * for w,h vars.  A scaling factor value must be greater than 0 and less than 2.
	 * Note that ScalingFactor overrides ScalingDimensions.
	 * @param bi BufferedImage to be scaled.
	 * @param params DjatokaDecodeParam containing ScalingFactor or ScalingDimensions vars
	 * @return scaled instance of provided BufferedImage
	 */
	private static BufferedImage applyScaling(BufferedImage bi, DjatokaDecodeParam params) {
		if (params.getScalingFactor() != 1.0 
				&& params.getScalingFactor() > 0 
				&& params.getScalingFactor() < 3)
			bi = ImageProcessingUtils.scale(bi,params.getScalingFactor());
		else if (params.getScalingDimensions() != null 
				&& params.getScalingDimensions().length == 2) {
			int width = params.getScalingDimensions()[0];
			if (width >= 3 * bi.getWidth())
				return bi;
			int height = params.getScalingDimensions()[1];
			if (height >= 3 * bi.getHeight())
				return bi;
			bi = ImageProcessingUtils.scale(bi, width, height);
		}
		return bi;
	}
}
