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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * JPEG 2000 Metadata Parser
 * @author Ryan Chute
 *
 */
public class JP2ImageInfo implements JP2Markers {
	private InputStream is;
	private int currentDataLength;
	private int currentMarker;
	private ImageRecord ir;
	private List<String> xmlDocs;

	public JP2ImageInfo(File f) throws IOException {
		this(new BufferedInputStream(new FileInputStream(f)));
		ir.setImageFile(f.getAbsolutePath());
	}
	
	public JP2ImageInfo(InputStream is) throws IOException {
		this.is = is;
		ir = new ImageRecord();
		setImageInfo();
	}
    
	/**
	 * Gets a populated ImageRecords for the initialized image
	 * @return a populated ImageRecord
	 */
	public ImageRecord getImageRecord() {
		return ir;
	}
	
	/**
	 * Gets a list of xml docs contained in the JPEG 2000 header
	 * 
	 */
	public String[] getXmlDocs() {
		if (xmlDocs != null)
		    return xmlDocs.toArray(new String[xmlDocs.size()]);
		else
			return null;
	}
	
	private void setImageInfo() throws IOException {
		try {
			currentDataLength = read(4);
			if (currentDataLength == MARKER_JP_LEN) {
				currentMarker = read(4);
				if (MARKER_JP != currentMarker) {
					throw new IOException("Expected JP Marker");
				}
				if (MARKER_JP_SIG != read(4)) {
					throw new IOException("Invalid JP Marker");
				}
				nextHeader();
				if (MARKER_FTYP != currentMarker) {
					throw new IOException("FTYP Marker not found");
				}
				skip(currentDataLength - 8);
				nextHeader();
				boolean done = false;
				do {
					if (MARKER_JP2H == currentMarker) {
						nextHeader();
					} else if (MARKER_IHDR == currentMarker) {
						setIHDR();
						nextHeader();
					} else if (MARKER_COLR == currentMarker) {
						setCOLR();
						nextHeader();
					} else if (MARKER_RES_BOX == currentMarker) {
						setResBox();
						nextHeader();
					} else if (MARKER_JP2C == currentMarker) {
					    setJP2C();
					    done = true;
					} else if (MARKER_XML == currentMarker) {
					    addXmlDoc(getXML());
					    nextHeader();
					} else if (MARKER_SOC == currentMarker) {
					    done = true;
					} else {
						skip(currentDataLength - 8);
						nextHeader();
					}
				} while (!done);
				int compLayers = countCompLayers(readBytes(is.available()));
				ir.setCompositingLayerCount(compLayers);
			} else {
				throw new IOException("Invalid Jpeg2000 file");
			}
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {}
				is = null;
			}
		}
	}
	
	/**
	 * Add a doc to the list of xml docs contained in the JPEG 2000 header
	 * @param docs
	 */
	private void addXmlDoc(String doc) {
		if (xmlDocs == null)
			xmlDocs = new LinkedList<String>();
		this.xmlDocs.add(doc);
	}
	
	private void nextHeader() throws IOException {
		currentDataLength = read(4);
		currentMarker = read(4);
		if (currentDataLength == 1) {
			// Process SuperBox
			if (read(4) != 0) {
				throw new IOException("Box length too large");
			}
			currentDataLength = read(4);
			if (currentDataLength == 0)
				throw new IOException("Invalid box size");
		} else if (currentDataLength <= 0 && currentMarker != MARKER_JP2C) {
			throw new IOException("Invalid box size");
		}
	}
	
	private int read(int n) throws IOException {
		int c = 0;
		for (int i = n - 1; i >= 0; i--) {
			c |= (0xff & is.read()) << (8 * i);
		}
		return c;
	}
	
	private void skip(int n) throws IOException {
		long i;
		while (n > 0) {
			i = is.skip(n);
			if (i <= 0)
				break;
			n -= i;
		}
	}
	
	private byte[] readBytes(int n) throws IOException {
		byte[] b = new byte[n];
		is.read(b);
		return b;
	}
	
	private static int countCompLayers(byte[] data) {
		byte[] pattern = MARKER_JPLH_BIN;
		int cnt = 1;
		int j = 1;
		if (data.length == 0)
			return 0;

		for (int i = 0; i < data.length; i++) {
			if (pattern[j] == data[i]) {
				j++;
			} else {
				j = 1;
			}
			if (j == pattern.length) {
				cnt++;
				j = 1;
			}
		}
		return cnt;
	}
	
	private void setIHDR() throws IOException {
		int scaledHeight = read(4);
		ir.setHeight(scaledHeight);
		int scaledWidth = read(4);
		ir.setWidth(scaledWidth);
		int components = read(2);
		ir.setNumChannels(components);
		int bitDepth = read(1);
		ir.setBitDepth((bitDepth == 7) ? bitDepth + 1 : bitDepth);
		int compression = read(1);
		int unknownColor = read(1);
		int intelProp = read(1);
	}

	private void setCOLR() throws IOException {
			int method = read(1);
			int precedence = read(1);
			int approximation = read(1);
			if (method == 2) {
				int proData = read(currentDataLength - 3);
				// ICC Profile Needs to be set
			} else {
				int c = read(4);
			}
	}
	
	private void setResBox() throws IOException {
		int vn = read(3);
		int vd = read(3);
		int hn = read(3);
		int hd = read(3);
		int ve = read(3);
		int he = read(3);
	}
	
	private String getXML() throws IOException {
		// Subtract XML Marker, Length Value, XML Flag
		byte[] xml = readBytes(currentDataLength - 16);
		if (xml != null)
		    return new String(xml);
		else
			return null;
	}
	
	private void setJP2C() throws IOException {
		int soc = read(2); // SOC
		boolean hend = false;
		while (!hend) {
			int h = read(2);
			if (h == MARKER_SIZ) { // SIZ
				int lsiz = read(2); // Length of SIZ
				int rsiz = read(2);
				int xsiz = read(4);
				int ysiz = read(4);
				int xosiz = read(4);
				int yosiz = read(4);
				int xtsiz = read(4);
				int ytsiz = read(4);
				int xtosize = read(4);
				int ytosize = read(4);
				int csiz = read(2);
				int ssiz = read(1);
				int xrsiz = read(1);
				int yrsiz = read(1);
				int a0 = read(2);
				int a1 = read(2);
				int a2 = read(2);
			} else if (h == MARKER_COD) { // COD
				int lcod = read(2); // Length of COD
				int scod = read(1);
				int sgcod_porder = read(1); // Progression Order
				int sgcod_layers = read(2); // Number of layers
				ir.setQualityLayers(sgcod_layers);
				int sgcod_ctrans = read(1); // Component Transformation Type
				int sgcod_levels = read(1); // Number of levels
				ir.setDWTLevels(sgcod_levels);
				int djatokaLevels = ImageProcessingUtils.getLevelCount(ir.getWidth() , ir.getHeight());
				ir.setLevels((djatokaLevels > sgcod_levels) ? sgcod_levels : djatokaLevels); 
				int sgcod_cb_width = read(1); // code-block width
				int sgcod_cb_height = read(1); // code-block height
				int sgcod_cb_style = read(1); // code-block style
				int sgcod_wavelet = read(1); // wavelet type (9:7 && 5:3)
				hend = true;
			} else {
				throw new IOException("Expecting MARKER_COD or MARKER_SIZ in header");
			}
			
		}
	}
	
	public static void main(String[] args) throws Exception {
		JP2ImageInfo jp2 = new JP2ImageInfo(new File(args[0]));
		ImageRecord ir = jp2.getImageRecord();
		System.out.println(ir.toString());
	}
}
