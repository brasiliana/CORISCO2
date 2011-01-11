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

import java.util.Properties;

/**
 * Encode Parameters for djatoka compression. Defines compression 
 * parameters (i.e. levels, slope, layers, etc.) to be performed 
 * during extraction of JP2.
 * @author Ryan Chute
 *
 */
public class DjatokaEncodeParam implements DjatokaConstants {

	private String rate;
	private String slope;
	private int layers = DEFAULT_CLAYERS;
	private int levels = DEFAULT_CLEVELS;
	private boolean useReversible = DEFAULT_USE_REVERSIBLE;
	private String precincts = DEFAULT_CPRECINCTS;
	private String progressionOrder = DEFAULT_CORDER;
	private boolean insertPLT = DEFAULT_ORGGEN_PLT;
	private String packetDivision = DEFAULT_ORGTPARTS;
	private String codeBlockSize = DEFAULT_CBLK;

	/**
	 * Default Constructor, uses default slope and compression settings
	 */
	public DjatokaEncodeParam() {
		setSlope(DEFAULT_SLOPE);
	}
	
	/**
	 * Constructor using provided properties to initialize encode parameters
	 * @param props Properties to initialize encode parameters
	 */
	public DjatokaEncodeParam(Properties props) {
		if (props != null) {
			if (props.containsKey("rate")) {
			    rate = props.getProperty("rate");
			} else {
			    slope = props.getProperty("slope", DEFAULT_SLOPE);
			}
			layers = props.getProperty("Clayers") != null ? Integer.parseInt(props.getProperty("Clayers")) : layers;
			levels = props.getProperty("Clevels") != null ? Integer.parseInt(props.getProperty("Clevels")) : levels;
			useReversible = props.getProperty("Creversible") != null ? Boolean.parseBoolean(props.getProperty("Creversible")) : useReversible;
			precincts = props.getProperty("Cprecincts", precincts);
			progressionOrder = props.getProperty("Corder", progressionOrder);
			insertPLT = props.getProperty("ORGgen_plt") != null ? Boolean.parseBoolean(props.getProperty("ORGgen_plt")) : insertPLT;
			packetDivision = props.getProperty("ORGtparts", packetDivision);
			codeBlockSize = props.getProperty("Cblk", codeBlockSize);
		}
	}

	/**
	 * Returns the absolute compression rate, if enabled.
	 * @return the absolute compression rate
	 */
	public String getRate() {
		return rate;
	}

	/**
	 * Sets the absolute compression rate, setting slope to null.
	 * @param rate the absolute compression rate
	 */
	public void setRate(String rate) {
		if (rate != null && slope != null)
			slope = null;
		this.rate = rate;
	}

	/**
	 * Returns the slope distortion slope, that applies a compression 
	 * rate based on the content of the image.
	 * @return the slope distortion slope
	 */
	public String getSlope() {
		return slope;
	}

	/**
	 * Sets the slope distortion slope, that applies a compression 
	 * rate based on the content of the image.
	 * @param slope the slope distortion slope
	 */
	public void setSlope(String slope) {
		if (rate != null && slope != null)
			rate = null;
		this.slope = slope;
	}

	/**
	 * Returns the number of quality layers, must correspond with 
	 * number of slope values. 
	 * @return the number of quality layers
	 */
	public int getLayers() {
		return layers;
	}

	/**
	 * Sets the number of quality layers, must correspond with 
	 * number of slope values. 
	 * @param layers the number of quality layers
	 */
	public void setLayers(int layers) {
		this.layers = layers;
	}

	/**
	 * Returns the number of DWT Levels (i.e. resolution levels)
	 * @return the number of DWT Levels
	 */
	public int getLevels() {
		return levels;
	}

	/**
	 * Sets the number of DWT Levels (i.e. resolution levels)
	 * @param levels the number of DWT Levels
	 */
	public void setLevels(int levels) {
		this.levels = levels;
	}

	/**
	 * Returns indicator of whether or not a reversible compression 
	 * wavelet should be used.
	 * @return use reversible wavelet
	 */
	public boolean getUseReversible() {
		return useReversible;
	}

	/**
	 * Sets indicator of whether or not a reversible compression 
	 * wavelet should be used.
	 * @param useReversible use reversible wavelet
	 */
	public void setUseReversible(boolean useReversible) {
		this.useReversible = useReversible;
	}

	/**
	 * Returns the size of the layer packets. 
	 * @return the size of the layer packets
	 */
	public String getPrecincts() {
		return precincts;
	}

	/**
	 * Sets the size of the layer packets.  Default values improved random
	 * access performance. Format: "{256,256},{256,256},{128,128}"
	 * @param precincts the size of the layer packets. 
	 */
	public void setPrecincts(String precincts) {
		this.precincts = precincts;
	}

	/**
	 * Returns the preferred progression order for layers.
	 * @return progression order
	 */
	public String getProgressionOrder() {
		return progressionOrder;
	}

	/**
	 * Sets the preferred progression order for layers.
	 * @param pOrder progression order
	 */
	public void setProgressionOrder(String pOrder) {
		this.progressionOrder = pOrder;
	}

	/**
	 * Returns indicator of whether or not packet organization header 
	 * info should be included.
	 * @return include packet info
	 */
	public boolean getInsertPLT() {
		return insertPLT;
	}

	/**
	 * Sets indicator of whether or not packet organization header 
	 * info should be included.
	 * @param insertPLT include packet info
	 */
	public void setInsertPLT(boolean insertPLT) {
		this.insertPLT = insertPLT;
	}

	/**
	 * Returns order of header information. Default values improve 
	 * resolution access performance.
	 * @return packet division order
	 */
	public String getPacketDivision() {
		return packetDivision;
	}

	/**
	 * Sets order of header information. Default values improve 
	 * resolution access performance.
	 * @param packetDivision packet division order
	 */
	public void setPacketDivision(String packetDivision) {
		this.packetDivision = packetDivision;
	}
	
    /**
     * Returns the codeblock size.
     * @return the codeblock size
     */
	public String getCodeBlockSize() {
		return codeBlockSize;
	}

    /**
     * Sets the codeblock size.
     * @param codeBlockSize the codeblock size
     */
	public void setCodeBlockSize(String codeBlockSize) {
		this.codeBlockSize = codeBlockSize;
	}

}
