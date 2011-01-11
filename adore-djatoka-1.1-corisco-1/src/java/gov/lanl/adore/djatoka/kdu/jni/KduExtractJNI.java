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

package gov.lanl.adore.djatoka.kdu.jni;

import gov.lanl.adore.djatoka.DjatokaDecodeParam;
import gov.lanl.adore.djatoka.DjatokaException;
import gov.lanl.adore.djatoka.IExtract;
import gov.lanl.adore.djatoka.util.ImageRecord;
import gov.lanl.adore.djatoka.util.JP2ImageInfo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import kdu_jni.Jp2_family_src;
import kdu_jni.Jp2_input_box;
import kdu_jni.Jp2_locator;
import kdu_jni.Jp2_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_channel_mapping;
import kdu_jni.Kdu_codestream;
import kdu_jni.Kdu_compressed_source;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;

import org.apache.log4j.Logger;

/**
 * Uses Kakadu Java Native Interface to extract JP2 regions.  
 * This is modified port of the kdu_expand app.
 * @author Ryan Chute
 *
 */
public class KduExtractJNI implements IExtract {

	private static Logger logger = Logger.getLogger(KduExtractJNI.class);
	/**
	 * Returns JPEG 2000 props in ImageRecord
	 * @param r ImageRecord containing absolute file path of JPEG 2000 image file.
	 * @return a populated ImageRecord object
	 * @throws DjatokaException
	 */
	public final ImageRecord getMetadata(ImageRecord r) throws DjatokaException {
		if (!new File(r.getImageFile()).exists())
			throw new DjatokaException("Image Does Not Exist");
		
		Jp2_source inputSource = new Jp2_source();
		Kdu_compressed_source kduIn = null;
		Jp2_family_src jp2_family_in = new Jp2_family_src();
		Jp2_locator loc = new Jp2_locator();

		try {
			jp2_family_in.Open(r.getImageFile(), true);
			inputSource.Open(jp2_family_in, loc);
			inputSource.Read_header();
			kduIn = inputSource;
			Kdu_codestream codestream = new Kdu_codestream();
			codestream.Create(kduIn);
			Kdu_channel_mapping channels = new Kdu_channel_mapping();
			if (inputSource.Exists())
				channels.Configure(inputSource, false);
			else
				channels.Configure(codestream);
			int ref_component = channels.Get_source_component(0);
			int minLevels = codestream.Get_min_dwt_levels();
			int minLayers= codestream.Get_max_tile_layers();
			Kdu_dims image_dims = new Kdu_dims();
			codestream.Get_dims(ref_component, image_dims);
			Kdu_coords imageSize = image_dims.Access_size();
			
			r.setWidth(imageSize.Get_x());
			r.setHeight(imageSize.Get_y());
			r.setDWTLevels(minLevels);

			channels.Native_destroy();
			if (codestream.Exists())
				codestream.Destroy();
			kduIn.Native_destroy();
			inputSource.Native_destroy();
			jp2_family_in.Native_destroy();
		} catch (KduException e) {
			throw new DjatokaException(e);
		}

		return r;
	}

	public final String[] getXMLBox(ImageRecord r) throws DjatokaException {
		String[] xml = null;
		try {
			if (r.getImageFile() == null && r.getObject() != null
					&& r.getObject() instanceof InputStream) {
				xml = new JP2ImageInfo((InputStream) r.getObject()).getXmlDocs();
			} else {
				xml = new JP2ImageInfo(new File(r.getImageFile())).getXmlDocs();
			}
		} catch (IOException e) {
			logger.error(e, e);
		}
		return xml;
	}
	
	/**
	 * Extracts region defined in DjatokaDecodeParam as BufferedImage
	 * @param input absolute file path of JPEG 2000 image file.
	 * @param params DjatokaDecodeParam instance containing region and transform settings.
	 * @return extracted region as a BufferedImage
	 * @throws DjatokaException
	 */
	public BufferedImage process(String input, DjatokaDecodeParam params)
			throws DjatokaException {
		KduExtractProcessorJNI decoder = new KduExtractProcessorJNI(input, params);
		return decoder.extract();
	}

	/**
	 * Extracts region defined in DjatokaDecodeParam as BufferedImage
	 * @param input InputStream containing a JPEG 2000 image bitstream.
	 * @param params DjatokaDecodeParam instance containing region and transform settings.
	 * @return extracted region as a BufferedImage
	 * @throws DjatokaException
	 */
	public BufferedImage process(InputStream input, DjatokaDecodeParam params)
			throws DjatokaException {
		KduExtractProcessorJNI decoder = new KduExtractProcessorJNI(input, params);
		return decoder.extract();
	}
	
	/**
	 * Extracts region defined in DjatokaDecodeParam as BufferedImage
	 * @param input ImageRecord wrapper containing file reference, inputstream, etc.
	 * @param params DjatokaDecodeParam instance containing region and transform settings.
	 * @return extracted region as a BufferedImage
	 * @throws DjatokaException
	 */
	public BufferedImage process(ImageRecord input, DjatokaDecodeParam params)
			throws DjatokaException {
		if (input.getImageFile() != null)
			return process(input, params);
		else if (input.getObject() != null
				&& (input.getObject() instanceof InputStream))
			return process((InputStream) input.getObject(), params);
		else
			throw new DjatokaException(
					"File not defined and Input Object Type "
							+ input.getObject().getClass().getName()
							+ " is not supported");
	}
}
