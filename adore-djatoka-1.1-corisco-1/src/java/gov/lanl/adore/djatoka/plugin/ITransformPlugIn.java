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

package gov.lanl.adore.djatoka.plugin;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Properties;

/**
 * Interface for post-extraction Image Transformation. Implementing classes are
 * provided BufferedImages upon which they can apply watermarks or other 
 * manipulations for reasons of image security, provenance, etc.
 * 
 * @author Ryan Chute
 * 
 */
public interface ITransformPlugIn {
	
	/**
	 * Initializes the implementation, overriding default values. Property keys
	 * are typically of the form ClassName.PropName. These are global instance fields.
	 * @param props Properties object containing implementation properties
	 */
	public void setup(Properties props);
	
	/**
	 * Sets the instance properties, from which per dissemination changes can be based on. 
	 * @param addProps HashMap object containing image transform instance properties
	 */
	public void setInstanceProps(HashMap<String, String> addProps);
	
	/**
	 * Performs the transformation based on the provided global and instance properties.
	 * 
	 * @param bi the extracted region BufferedImage to be transformed
	 * @return the resulting BufferedImage or the same bi if no changes are made
	 * @throws TransformException
	 */
	public BufferedImage run(BufferedImage bi) throws TransformException;
	
	/**
	 * Returns boolean indicator whether or not an image is transformable based on 
	 * the global and instance properties.  This is very helpful for cache logic.
	 * @return true if transformable
	 */
	public boolean isTransformable();
}
