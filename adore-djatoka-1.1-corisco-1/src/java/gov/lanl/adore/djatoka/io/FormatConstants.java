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

package gov.lanl.adore.djatoka.io;

/**
 * Format Writer/Reader Constants. Defines default image writer
 * implementations and mimetypes / ext. associated with image format.
 * @author Ryan Chute
 *
 */
public interface FormatConstants {
	// Format ids map to the suffix of the mimetype
	/** JPEG Identifier Constant - "jpeg" */
	public static final String FORMAT_ID_JPEG = "jpeg";
	/** JPEG IAlternate Identifier Constant - "jpg" */
	public static final String FORMAT_ID_JPG = "jpg";
	/** JPEG 2000 Identifier Constant - "jp2" */
	public static final String FORMAT_ID_JP2 = "jp2";
	/** PNG Identifier Constant - "png" */
	public static final String FORMAT_ID_PNG= "png";
	/** BMP Identifier Constant - "bmp" */
	public static final String FORMAT_ID_BMP= "bmp";
	/** PNM Identifier Constant - "pnm" */
	public static final String FORMAT_ID_PNM= "pnm";
	/** TIFF Identifier Constant - "tiff" */
	public static final String FORMAT_ID_TIFF= "tiff";
	/** TIFF Alternate Identifier Constant - "tif" */
	public static final String FORMAT_ID_TIF= "tif";
	/** GIF Identifier Constant - "gif" */
	public static final String FORMAT_ID_GIF= "gif";
	/** Additional JPEG 2000 Identifiers */
	public static final String FORMAT_ID_JPF = "jpf";
	public static final String FORMAT_ID_JPX = "jpx";
	public static final String FORMAT_ID_J2K = "j2k";
	public static final String FORMAT_ID_JPM = "jpm";
	public static final String FORMAT_ID_J2C = "j2c";
	public static final String FORMAT_ID_JPC = "jpc";
	
	// Mimetypes for supported image formats
	/** JPEG Mimetype Constant - "image/jpeg" */
	public static final String FORMAT_MIMEYPE_JPEG = "image/jpeg";
	/** JP2 Mimetype Constant - "image/jp2" */
	public static final String FORMAT_MIMEYPE_JP2 = "image/jp2";
	/** JPX Mimetype Constant - "image/jpx" */
	public static final String FORMAT_MIMEYPE_JPX = "image/jpx";
	/** JPM Mimetype Constant - "image/jpm" */
	public static final String FORMAT_MIMEYPE_JPM = "image/jpm";
	/** PNG Mimetype Constant - "image/png" */
	public static final String FORMAT_MIMEYPE_PNG = "image/png";
	/** BMP Mimetype Constant - "image/bmp" */
	public static final String FORMAT_MIMEYPE_BMP = "image/bmp";
	/** PNM Mimetype Constant - "image/pnm" */
	public static final String FORMAT_MIMEYPE_PNM = "image/pnm";
	/** TIFF Mimetype Constant - "image/tiff" */
	public static final String FORMAT_MIMEYPE_TIFF = "image/tiff";
	/** GIF Mimetype Constant - "image/gif" */
	public static final String FORMAT_MIMEYPE_GIF = "image/gif";
	/** Reader Suffix Constant - "_reader" */
	public static final String FORMAT_READER_SUFFIX = "_reader";
	/** Writer Suffix Constant - "_writer" */
	public static final String FORMAT_WRITER_SUFFIX = "_writer";
	// default implementations for define formats
	/** Default JPEG Writer - "gov.lanl.adore.djatoka.io.writer.JPGWriter" */
	public static final String DEFAULT_JPEG_WRITER = "gov.lanl.adore.djatoka.io.writer.JPGWriter";
	/** Default JP2 Writer - "gov.lanl.adore.djatoka.io.writer.JP2Writer" */
	public static final String DEFAULT_JP2_WRITER = "gov.lanl.adore.djatoka.io.writer.JP2Writer";
	/** Default PNG Writer - "gov.lanl.adore.djatoka.io.writer.PNGWriter" */
	public static final String DEFAULT_PNG_WRITER = "gov.lanl.adore.djatoka.io.writer.PNGWriter";
	/** Default BMP Writer - "gov.lanl.adore.djatoka.io.writer.BMPWriter" */
	public static final String DEFAULT_BMP_WRITER = "gov.lanl.adore.djatoka.io.writer.BMPWriter";
	/** Default PNM Writer - "gov.lanl.adore.djatoka.io.writer.PNMWriter" */
	public static final String DEFAULT_PNM_WRITER = "gov.lanl.adore.djatoka.io.writer.PNMWriter";
	/** Default TIFF Writer - "gov.lanl.adore.djatoka.io.writer.TIFWriter" */
	public static final String DEFAULT_TIFF_WRITER = "gov.lanl.adore.djatoka.io.writer.TIFWriter";
	/** Default GIF Writer - "gov.lanl.adore.djatoka.io.writer.GIFWriter" */
	public static final String DEFAULT_GIF_WRITER = "gov.lanl.adore.djatoka.io.writer.GIFWriter";
	
}
