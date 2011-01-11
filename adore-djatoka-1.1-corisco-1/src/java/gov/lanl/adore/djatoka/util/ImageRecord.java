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

import java.util.Map;

/**
 * Image Record Metadata. Used to transfer image properties.
 * @author Ryan Chute
 *
 */
public class ImageRecord {
	private String identifier;
	private String imageFile;
	private Object object;
	private int width;
	private int height;
	private int levels;
	private int dwtLevels;
	private int qualityLayers;
	private int compositingLayers;
	private int bitDepth;
	private int numChannels;
	private Map<String, String> instProps;
	
	/**
	 * Default Constructor
	 */
	public ImageRecord(){};
	
	/**
	 * Constructor used to prime imageFile value
	 * @param imageFile the absolute file path of the image
	 */
	public ImageRecord(String imageFile) {
		this.imageFile = imageFile;
	}
	
	/**
	 * Constructor used to prime identifier and imageFile value
	 * @param identifier unique identifier of image
	 * @param imageFile the absolute file path of the image
	 */
	public ImageRecord(String identifier, String imageFile) {
		this.identifier = identifier;
		this.imageFile = imageFile;
	}
	
	/**
	 * Returns the unique identifier of image
	 * @return unique identifier of image
	 */
	public String getIdentifier() {
		return identifier;
	}
	
	/**
	 * Sets the unique identifier of image
	 * @param identifier unique identifier of image
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	
	/**
	 * Returns the absolute file path of the image
	 * @return the absolute file path of the image
	 */
	public String getImageFile() {
		return imageFile;
	}
	
	/**
	 * Returns the absolute file path of the image
	 * @param imageFile the absolute file path of the image
	 */
	public void setImageFile(String imageFile) {
		this.imageFile = imageFile;
	}
	
	/**
	 * Returns the pixel width of the image
	 * @return the pixel width of the image
	 */
	public int getWidth() {
		return width;
	}
	
	/**
	 * Sets the pixel width of the image
	 * @param width the pixel width of the image
	 */
	public void setWidth(int width) {
		this.width = width;
	}
	
	/**
	 * Returns the pixel height of the image
	 * @return the pixel height of the image
	 */
	public int getHeight() {
		return height;
	}
	
	/**
	 * Sets the pixel height of the image
	 * @param height the pixel height of the image
	 */
	public void setHeight(int height) {
		this.height = height;
	}
	
	/**
	 * Returns the number of dwt levels
	 * @return the number of dwt levels
	 */
	public int getDWTLevels() {
		return dwtLevels;
	}
	
	/**
	 * Sets the number of dwt levels
	 * @param dwtLevels the number of dwt levels
	 */
	public void setDWTLevels(int dwtLevels) {
		this.dwtLevels = dwtLevels;
	}

	/**
	 * Returns the number of djatoka resolution levels
	 * @return the number of djatoka resolution levels
	 */
	public int getLevels() {
		return levels;
	}
	
	/**
	 * Sets the number of djatoka resolution levels
	 * @param levels the number of djatoka resolution levels
	 */
	public void setLevels(int levels) {
		this.levels = levels;
	}
	
	/**
	 * Returns a map of properties associated with the image. The properties may
	 * be used transformation processes down the line.
	 * @return a map of properties associated with the image. 
	 */
	public Map<String, String> getInstProps() {
		return instProps;
	}

	/**
	 * Sets a map of properties associated with the image. The properties may
	 * be used transformation processes down the line.
	 * @param instProps a map of properties associated with the image. 
	 */
	public void setInstProps(Map<String, String> instProps) {
		this.instProps = instProps;
	}

	/**
	 * Gets the number of JPEG2000 quality layers
	 * @return the number of quality layers
	 */
	public int getQualityLayers() {
		return qualityLayers;
	}

	/**
	 * Sets the number of JPEG2000 quality layers
	 * @param layers the number of quality layers
	 */
	public void setQualityLayers(int layers) {
		this.qualityLayers = layers;
	}

	/**
	 * Gets the number of JPEG2000 jpx compositing layers, also known as frames
	 * @return the number of frames
	 */
	public int getCompositingLayerCount() {
		return compositingLayers;
	}

	/**
	 * Sets the number of JPEG2000 jpx compositing layers, also known as frames
	 * @param frames the number of frames
	 */
	public void setCompositingLayerCount(int frames) {
		this.compositingLayers = frames;
	}

	/**
	 * Gets the bit depth (e.g. 8) for each color channel
	 * @return the bit depth for each channel
	 */
	public int getBitDepth() {
		return bitDepth;
	}

	/**
	 * Sets the bit depth (e.g. 8) for each color channel
	 * @param bitDepth the bit depth for each channel
	 */
	public void setBitDepth(int bitDepth) {
		this.bitDepth = bitDepth;
	}

	/**
	 * Gets the number of color channels (e.g. 3 for RGB)
	 * @return the number of color channels
	 */
	public int getNumChannels() {
		return numChannels;
	}

	/**
	 * Sets the number of color channels (e.g. 3 for RGB)
	 * @param numChannels the number of color channels
	 */
	public void setNumChannels(int numChannels) {
		this.numChannels = numChannels;
	}
	
    /**
	 * Sets InputStream, ByteArray, URL, etc.
	 * 
	 * @param object
	 *            object to be resolved by another process
	 */
	public void setObject(Object object) {
		this.object = object;
	}

	/**
	 * Returns InputStream, ByteArray, URL, whatever was set
	 * 
	 * @return the object to be resolved by another process
	 */
	public Object getObject() {
		return object;
	}
	
	public String toString() {
	    StringBuffer sb = new StringBuffer();
		sb.append("{");
		sb.append("\n\"identifier\": \"" + identifier + "\",");
		sb.append("\n\"imageFile\": \"" + imageFile + "\",");
		sb.append("\n\"object\": \"" + ((object != null) ? "type= " + object.getClass().getCanonicalName() : null) + "\",");
		sb.append("\n\"width\": \"" + width + "\",");
		sb.append("\n\"height\": \"" + height + "\",");
		sb.append("\n\"dwtLevels\": \"" + dwtLevels + "\",");
		sb.append("\n\"levels\": \"" + levels + "\",");
		sb.append("\n\"qualityLayers\": \"" + qualityLayers + "\",");
		sb.append("\n\"compositingLayers\": \"" + compositingLayers + "\",");
		sb.append("\n\"bitDepth\": \"" + bitDepth + "\",");
		sb.append("\n\"numChannels\": \"" + numChannels + "\",");
		sb.append("\n\"instProps\": \"" + ((instProps != null) ? "size= " + instProps.size() : null) + "\"");
		sb.append("\n}");
	    return sb.toString();
	}
}
