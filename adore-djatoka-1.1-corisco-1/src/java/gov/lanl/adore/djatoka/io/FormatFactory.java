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

import java.util.*;

import org.apache.log4j.Logger;

/**
 * Format Factory. Uses format writer/reader implementations.
 * @author Ryan Chute
 *
 */
public class FormatFactory implements FormatConstants {
	static Logger logger = Logger.getLogger(FormatFactory.class);
	private HashMap<String, Class> fmtImpl = new HashMap<String, Class>();
	
	/**
	 * Default Constructor, uses default format map.
	 */
	public FormatFactory() {
		this(getDefaultFormatMap());
	}
	
	/**
	 * Create a new FormatFactory using provided format map. Format maps
	 * must be key/value pair of syntax $formatId_writer=$impl
	 * (e.g. jpeg_writer=gov.lanl.adore.djatoka.io.writer.JPGWriter)
	 * @param formatMap
	 */
	public FormatFactory(Properties formatMap) {
		for (Map.Entry<Object, Object> i : formatMap.entrySet()) {
			String k = (String) i.getKey();
			String v = (String) i.getValue();
			if (k.endsWith("_writer")) {
				try {
					Class<?> impl = Class.forName(v);
					if (k != null && impl != null)
						fmtImpl.put(k, impl);
				} catch (ClassNotFoundException e) {
					System.err.println("Class Not Found for format " + k + ": " + v);
				}
			}
		}
	}
	
	/**
	 * Returns format writer implementation for provided format identifier
	 * @param format identifier of requested identifier
	 * @return format writer for provided format identifier
	 */
	public IWriter getWriter(String format) {
		return getWriter(format, null);
	}
	
	/**
	 * Returns format writer implementation for provided format identifier
	 * @param format identifier of requested identifier
	 * @param props Properties defining alternate Writer instances
	 * @return format writer for provided format identifier
	 */
	public IWriter getWriter(String format, Properties props) {
		return getFormatWriterInstance(format, props);
	}
	
	/**
	 * Returns format reader implementation for provided format identifier
	 * @param format identifier of requested identifier
	 * @return format reader for provided format identifier
	 * @throws Exception
	 */
	public IReader getReader(String format) throws Exception {
		return getFormatReaderInstance(format);
	}
	
	/**
	 * Returns Format Identifier Suffix from mimetype
	 * @param fmtId full mimetype string
	 * @return String format identifier matching FormatConstants
	 */
	public static final String getFormatSuffix(String fmtId) {
		if (fmtId.startsWith("image/"))
			return fmtId.substring(6);
		else if (fmtId.startsWith("image%2F"))
			return fmtId.substring(8);
		else
			return fmtId;
	}
	
	/**
	 * Create a new FormatFactory using provided format map. Format maps
	 * must be key/value pair of syntax $formatId_writer=$impl
	 * (e.g. jpeg_writer=gov.lanl.adore.djatoka.io.writer.JPGWriter)
	 * @return Properties object containing writer implementation instance key/value pairs
	 */
	public static final Properties getDefaultFormatMap() {
		Properties formatMap = new Properties();
		formatMap.put(FORMAT_ID_JPEG + FORMAT_WRITER_SUFFIX, DEFAULT_JPEG_WRITER);
		formatMap.put(FORMAT_ID_JPG + FORMAT_WRITER_SUFFIX, DEFAULT_JPEG_WRITER);
		formatMap.put(FORMAT_ID_JP2 + FORMAT_WRITER_SUFFIX, DEFAULT_JP2_WRITER);
		formatMap.put(FORMAT_ID_PNG + FORMAT_WRITER_SUFFIX, DEFAULT_PNG_WRITER);
		formatMap.put(FORMAT_ID_BMP + FORMAT_WRITER_SUFFIX, DEFAULT_BMP_WRITER);
		formatMap.put(FORMAT_ID_PNM + FORMAT_WRITER_SUFFIX, DEFAULT_PNM_WRITER);
		formatMap.put(FORMAT_ID_TIFF + FORMAT_WRITER_SUFFIX, DEFAULT_TIFF_WRITER);
		formatMap.put(FORMAT_ID_TIF + FORMAT_WRITER_SUFFIX, DEFAULT_TIFF_WRITER);
		formatMap.put(FORMAT_ID_GIF + FORMAT_WRITER_SUFFIX, DEFAULT_GIF_WRITER);
		return formatMap;
	}
	
	private IWriter getFormatWriterInstance(String format, Properties props) {
		format = getFormatSuffix(format);
		IWriter w = null;
		try {
			w = (IWriter) fmtImpl.get(format + FORMAT_WRITER_SUFFIX).newInstance();
			if (props != null) {
				w.setWriterProperties(props);
			}
		} catch (InstantiationException e) {
			logger.error(e);
		} catch (IllegalAccessException e) {
			logger.error(e);
		}
		return w;
	}
	
	private IReader getFormatReaderInstance(String format) {
		format = getFormatSuffix(format);
		IReader r = null;
		try {
			r = (IReader) fmtImpl.get(format + FORMAT_READER_SUFFIX).newInstance();
		} catch (InstantiationException e) {
			logger.error(e);
		} catch (IllegalAccessException e) {
			logger.error(e);
		}
		return r;
	}
}
