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

package gov.lanl.adore.djatoka.openurl;

import gov.lanl.adore.djatoka.DjatokaEncodeParam;
import gov.lanl.adore.djatoka.DjatokaException;
import gov.lanl.adore.djatoka.ICompress;
import gov.lanl.adore.djatoka.IExtract;
import gov.lanl.adore.djatoka.io.FormatConstants;
import gov.lanl.adore.djatoka.kdu.KduCompressExe;
import gov.lanl.adore.djatoka.kdu.KduExtractExe;
import gov.lanl.adore.djatoka.util.IOUtils;
import gov.lanl.adore.djatoka.util.ImageProcessingUtils;
import gov.lanl.adore.djatoka.util.ImageRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Utility class used to harvest URIs and compress files into JP2
 * @author Ryan Chute
 *
 */
public class DjatokaImageMigrator implements FormatConstants, IReferentMigrator {
	static Logger logger = Logger.getLogger(DjatokaImageMigrator.class);
	private List<String> processing = java.util.Collections.synchronizedList(new LinkedList<String>());
	private HashMap<String, String> formatMap;
	
	/**
	 * Constructor. Initialized formatMap with common ext suffixes
	 */
	public DjatokaImageMigrator() {
		formatMap = new HashMap<String, String>();
		formatMap.put(FORMAT_ID_JPEG, FORMAT_MIMEYPE_JPEG);
		formatMap.put(FORMAT_ID_JP2, FORMAT_MIMEYPE_JP2);
		formatMap.put(FORMAT_ID_PNG, FORMAT_MIMEYPE_PNG);
		formatMap.put(FORMAT_ID_BMP, FORMAT_MIMEYPE_BMP);
		formatMap.put(FORMAT_ID_PNM, FORMAT_MIMEYPE_PNM);
		formatMap.put(FORMAT_ID_TIFF, FORMAT_MIMEYPE_TIFF);
		formatMap.put(FORMAT_ID_GIF, FORMAT_MIMEYPE_GIF);
		// Additional Extensions
		formatMap.put(FORMAT_ID_JPG, FORMAT_MIMEYPE_JPEG);
		formatMap.put(FORMAT_ID_TIF, FORMAT_MIMEYPE_TIFF);
		// Additional JPEG 2000 Extensions
		formatMap.put(FORMAT_ID_J2C, FORMAT_MIMEYPE_JP2);
		formatMap.put(FORMAT_ID_JPC, FORMAT_MIMEYPE_JP2);
		formatMap.put(FORMAT_ID_J2K, FORMAT_MIMEYPE_JP2);
		formatMap.put(FORMAT_ID_JPF, FORMAT_MIMEYPE_JPX);
		formatMap.put(FORMAT_ID_JPX, FORMAT_MIMEYPE_JPX);
		formatMap.put(FORMAT_ID_JPM, FORMAT_MIMEYPE_JPM);
	}
	
	/**
	 * Returns a delete on exit File object for a provide URI
	 * @param uri the URI of an image to be downloaded and compressed as JP2
	 * @return File object of JP2 compressed image
	 * @throws DjatokaException
	 */
	public File convert(URI uri) throws DjatokaException {
		try {
			logger.info("processingRemoteURI: " + uri.toURL());
			processing.add(uri.toString());
			File urlLocal = null;
			boolean isJp2 = false;
			// Obtain Resource
			InputStream src = IOUtils.getInputStream(uri.toURL());
			String ext = uri.toURL().toString().substring(uri.toURL().toString().lastIndexOf(".") + 1).toLowerCase();
			if (ext.equals(FORMAT_ID_TIF) || ext.equals(FORMAT_ID_TIFF)) {
				urlLocal = File.createTempFile("convert" + uri.hashCode(), "." + FORMAT_ID_TIF);
			} else if (formatMap.containsKey(ext) && (formatMap.get(ext).equals(FORMAT_MIMEYPE_JP2) || formatMap.get(ext).equals(FORMAT_MIMEYPE_JPX))) {
				urlLocal = File.createTempFile("cache" + uri.hashCode(), "."  + ext);
				isJp2 = true;
			} else {
                if (src.markSupported())
                    src.mark(15);
                 
                if (ImageProcessingUtils.checkIfJp2(src))
                     urlLocal = File.createTempFile("cache" + uri.hashCode(), "."  + FORMAT_ID_JP2);
                 
                if (src.markSupported())
					src.reset();
				else {
					// close and reopen the stream
					src.close();
					src = IOUtils.getInputStream(uri.toURL());
				}
		    } 

			if (urlLocal == null) {
				urlLocal = File.createTempFile("convert" + uri.hashCode(), ".img");
			}
			urlLocal.deleteOnExit();
			
			FileOutputStream dest = new FileOutputStream(urlLocal);
			IOUtils.copyStream(src, dest);
			
			// Process Image
			if (!isJp2)
			    urlLocal = processImage(urlLocal, uri);

			// Clean-up
			src.close();
			dest.close();
			
			return urlLocal;
		} catch (Exception e) {
			throw new DjatokaException(e);
		} finally {
			if (processing.contains(uri.toString()))
				processing.remove(uri.toString());
		}
	}
	
	/**
	 * Returns a delete on exit File object for a provide URI
	 * @param img File object on local image to be compressed
	 * @param uri the URI of an image to be compressed as JP2
	 * @return File object of JP2 compressed image
	 * @throws DjatokaException
	 */
	public File processImage(File img, URI uri) throws DjatokaException {
		String imgPath = img.getAbsolutePath();
		String fmt = formatMap.get(imgPath.substring(imgPath.lastIndexOf('.') + 1).toLowerCase());
		try {
			if (fmt == null || !ImageProcessingUtils.isJp2Type(fmt)) {
				ICompress jp2 = new KduCompressExe();
				File jp2Local = File.createTempFile("cache" + uri.hashCode() + "-", ".jp2");
				jp2Local.delete();
				jp2.compressImage(img.getAbsolutePath(), jp2Local.getAbsolutePath(), new DjatokaEncodeParam());
				img.delete();
				img = jp2Local;
			} else {
				try {
					IExtract ex = new KduExtractExe();
					ex.getMetadata(new ImageRecord(uri.toString(), img.getAbsolutePath()));
				} catch (DjatokaException e) {
					throw new DjatokaException("Unknown JP2/JPX file format");
				}
			}
		} catch (Exception e) {
			throw new DjatokaException(e);
		}
		return img;
	}

	/**
	 * Return a unmodifiable list of images currently being processed. Images are removed once complete.
	 * @return list of images being processed
	 */
	public List<String> getProcessingList() {
		return processing;
	}

	/**
	 * Returns map of format extension (e.g. jpg) to mimetype mappings (e.g. image/jpeg)
	 * @return format extension to mimetype mappings
	 */
	public HashMap<String, String> getFormatMap() {
		return formatMap;
	}

	/**
	 * Sets map of format extension (e.g. jpg) to mimetype mappings (e.g. image/jpeg)
	 * @param formatMap extension to mimetype mappings
	 */
	public void setFormatMap(HashMap<String, String> formatMap) {
		this.formatMap = formatMap;
	}
	
	public static void main(String[] args) {
		URI uri;
		try {
			long a = System.currentTimeMillis();
			uri = new URI(args[0]);
			IReferentMigrator dim = new DjatokaImageMigrator();
			File f = dim.convert(uri);
			System.out.println((System.currentTimeMillis() - a) +  ": " + f.getAbsolutePath());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (DjatokaException e) {
			e.printStackTrace();
		}
	}
}
