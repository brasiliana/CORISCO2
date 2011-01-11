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

package gov.lanl.adore.djatoka.util;

/**
 * Constants for JP2 Marker Segment Names
 * @author rchute
 *
 */

public interface JP2Markers {
	public static final int MARKER_BPCC = 1651532643; // 0x62706363
	public static final int MARKER_COLR = 1668246642; // 0x636f6c72
	public static final int MARKER_DBTL = 1685348972; // 0x6474626c
	public static final int MARKER_DIS_BOX = 1919251300; // 0x72657364
	public static final int MARKER_FTYP = 1718909296; // 0x66747970
	public static final int MARKER_IHDR = 1768449138; // 0x69686472
	public static final int MARKER_IP = 1685074537; // 0x64703269
	public static final int MARKER_JP = 1783636000; // 0x6a502020
	public static final int MARKER_JP_LEN = 12; // 0x0000000c
	public static final int MARKER_JP_SIG = 218793738; // 0x0d0a870a
	public static final int MARKER_JP2 = 1785737760; // 0x6a703220
	public static final int MARKER_JP2C = 1785737827; // 0x6a703263
	public static final int MARKER_JP2H = 1785737832; // 0x6a703268
	public static final int MARKER_JPCH = 1785750376; // 0x6a706368
	public static final int MARKER_JPIP = 1785751920; // 0x6a706970
	public static final int MARKER_JPLH = 1785752680; // 0x6a706c68
	public static final byte[] MARKER_JPLH_BIN = "jplh".getBytes();
	public static final int MARKER_RES_BOX = 1919251232; // 0x72657320 - 'res'
	public static final int MARKER_RREQ = 1920099697; // 0x72726571
	public static final int MARKER_URL = 1970433056; // 0x75726c20 - 'url\040' 
	public static final int MARKER_UUID = 1970628964; // 0x75756964 - 'uuid'
	public static final int MARKER_XML = 2020437002; // 0x786D6C0A - 'xml\040'
	
	// Delimiting marker segments
	public static final int MARKER_SOC = 65359; // 0xFF4F - Start of codestream 
	public static final int MARKER_SOT = 65424; // 0xFF90 - Start of tile-part 
	public static final int MARKER_SOD = 65427; // 0xFF93 - Start of data
	public static final int MARKER_EOD = 65497; // 0xFFD9 - End of data
	
    // Fixed information marker segments
	public static final int MARKER_SIZ = 65361; // 0xFF51 - Image and tile size
	
	// Functional marker segments 
	public static final int MARKER_COD = 65362; // 0xFF52 - Coding style default
	public static final int MARKER_COC = 65363; // 0xFF53 - Coding style component 
}
