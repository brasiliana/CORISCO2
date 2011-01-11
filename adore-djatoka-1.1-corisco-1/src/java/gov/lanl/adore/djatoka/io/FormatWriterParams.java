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

import java.util.Properties;

/**
 * Format Writer Profile Object.  Used to initialize writers with 
 * implementation specific properties (e.g. compression level).
 * @author Ryan Chute
 *
 */
public class FormatWriterParams {
	private Properties formatProps;
	private String formatId;
	private String mimetype;
	
	/**
	 * Constructor using format identifier from FormatConstants
	 * @param fmtId format identifier from FormatConstants
	 */
	public FormatWriterParams(String fmtId) {
		this.formatId = fmtId;
	}
	
	/**
	 * Constructor used to override default writer serialization properties
	 * @param fmtId format identifier from FormatConstants
	 * @param formatProps Writer implementation serialization properties
	 */
	public FormatWriterParams(String fmtId, Properties formatProps) {
		this.formatId = fmtId;
		this.formatProps = formatProps;
	}
	/**
	 * Return format identifier for writer
	 * @return format identifier
	 */
	public String getFormatId() {
		return formatId;
	}
	/**
	 * Set format identifier.
	 * @param fmtId format identifier
	 */
	public void setFormatId(String fmtId) {
		this.formatId = fmtId;
	}
	/**
	 * Return mimetype of writer implementation
	 * @return mimetype of writer implementation
	 */
	public String getFormatMimetype() {
		return mimetype;
	}
	/**
	 * Set mimetype of writer implementation
	 * @param mimetype of writer implementation
	 */
	public void setFormatMimetype(String mimetype) {
		this.mimetype = mimetype;
	}
	/**
	 * Return writer serialization properties
	 * @return writer serialization properties
	 */
	public Properties getFormatProps() {
		return formatProps;
	}
	/**
	 * Set writer serialization properties
	 * @param formatProps writer serialization properties
	 */
	public void setFormatProps(Properties formatProps) {
		this.formatProps = formatProps;
	}
}
