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

import gov.lanl.adore.djatoka.plugin.ITransformPlugIn;

/**
 * Decode Parameters for djatoka extraction. Defines extraction 
 * parameters (i.e. region, rotate, level, transform plug-in) to 
 * be performed during extraction of JP2.
 * @author Ryan Chute
 *
 */
public class DjatokaDecodeParam implements DjatokaConstants {

	private static final int DEFAULT_LEVEL_REDUCE = 0;

	private static final String DEFAULT_EXTRACTION_REGION = null;
	
	private int reduce = DEFAULT_LEVEL_REDUCE;

	private int level = -1;
	
	private String region = DEFAULT_EXTRACTION_REGION;
	
	private int rotate = 0;
	
	private double scalingFactor = 1.0;
	
	private int[] scalingDims = null;
	
	private int compLayer = 0;
	
	private ITransformPlugIn transform;

	public DjatokaDecodeParam() {}

	/**
	 * Returns the number of levels to reduce from the highest 
	 * resolution level
	 * @return the number of levels to reduce
	 */
	public int getLevelReductionFactor() {
		return reduce;
	}

	/**
	 * Sets the number of levels to reduce from the highest resolution
	 * level.
	 * @param levelReductionFactor the number of levels to reduce
	 */
	public void setLevelReductionFactor(int levelReductionFactor) {
		this.reduce = levelReductionFactor;
	}

	/**
	 * Returns the resolution level to extract
	 * @return resolution level to extract from JPEG 2000 image
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * Sets the resolution level to extract
	 * @param level resolution level to extract from JPEG 2000 image
	 */
	public void setLevel(int level) {
		this.level = level;
	}
	
	/**
	 * Returns the discrete positive degrees the extracted image is 
	 * to be rotated (e.g. 90, 180, 270)
	 * @return degrees to rotate image
	 */
	public int getRotationDegree() {
		return rotate;
	}
	
	/**
	 * Sets the discrete positive degrees the extracted image is 
	 * to be rotated (e.g. 90, 180, 270)
	 * @param rotate degrees to rotate image
	 */
	public void setRotationDegree(int rotate) {
		if ((rotate % 90) != 0)
			rotate = 0;
		this.rotate = rotate;
	}

	/**
	 * Returns the parameter for the region to be extracted.  The region
	 * parameter format is: Y,X,H,W
	 * @return the region parameter
	 */
	public String getRegion() {
		return region;
	}
	
	/**
	 * Sets the parameter for the region to be extracted.  The region
	 * parameter format is: Y,X,H,W
	 * @param region the region parameter
	 */
	public void setRegion(String region) {
		this.region = region;
	}

	/**
	 * Returns the transformation plug-in to be applied post extraction.
	 * @return transformation plug-in to be applied
	 */
	public ITransformPlugIn getTransform() {
		return transform;
	}

	/**
	 * Sets the transformation plug-in to be applied post extraction.
	 * @param transform transformation plug-in to be applied
	 */
	public void setTransform(ITransformPlugIn transform) {
		this.transform = transform;
	}

	public void setCompositingLayer(int compLayer) {
		this.compLayer = compLayer;
	}
	
	public int getCompositingLayer() {
		return compLayer;
	}

	/**
	 * Gets a positive scaling factor (e.g. 0.85643), where 1.0 is the 
	 * current size. Value must be greater than 0 and less than 2. 
	 * @return a positive scaling factor
	 */
	public double getScalingFactor() {
		return scalingFactor;
	}

	/**
	 * Sets a positive scaling factor (e.g. 0.85643), where 1.0 is the 
	 * current size. Value must be greater than 0 and less than 2. 
	 * @param scalingFactor a positive scaling factor to be applied
	 */
	public void setScalingFactor(double scalingFactor) {
		this.scalingFactor = scalingFactor;
	}
	
	/**
	 * Gets the ScalingDimensions to be applied; w,h.
	 * @return null if no scaling is to be performed or an int[] 
	 * with length of 2 containing w,h values
	 */
	public int[] getScalingDimensions() {
		return scalingDims;
	}

	/**
	 * Sets the ScalingDimensions to be applied; w,h.
	 * @param scalingDims int[] with length of 2 containing w,h values. 
	 * Use value of 0 to maintain aspect ratio and calculate second value. 
	 */
	public void setScalingDimensions(int[] scalingDims) {
		this.scalingDims = scalingDims;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("{");
		sb.append("\"reduce\": \"" + reduce+ "\", ");
		sb.append("\"level\": \"" + level + "\", ");
		sb.append("\"region\": \"" + region + "\", ");
		sb.append("\"rotate\": \"" + rotate + "\", ");
		sb.append("\"scalingFactor\": \"" + scalingFactor + "\", ");
		if (scalingDims == null || scalingDims.length == 0) 
			sb.append("\"scalingDims\": \"\", ");
		else if (scalingDims != null && scalingDims.length == 1) 
			sb.append("\"scalingDims\": \"" + scalingDims[0] + "\", ");
		else if (scalingDims != null && scalingDims.length == 2) 
			sb.append("\"scalingDims\": \"" + scalingDims[0] + "," + scalingDims[1] + "\", ");
		sb.append("\"compLayer\": \"" + compLayer + "\" ");
		sb.append("}");
		return sb.toString();
	}
}
