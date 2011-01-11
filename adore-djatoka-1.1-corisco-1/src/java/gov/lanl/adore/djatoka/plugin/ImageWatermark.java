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

import java.awt.AlphaComposite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import javax.imageio.ImageIO;

/**
 * Extends TextWatermark to provide an overlaid image watermark
 * in additional to the text watermark.  Simply, set the statement
 * value to "" to prevent the textual watermark.
 * @author Ryan Chute
 *
 */
public class ImageWatermark extends TextWatermark {
	/** Overlay Image Property: "ImageWatermark.imagePath" */
	public static final String PROP_WATERMARK_OVERLAYIMAGE = "ImageWatermark.imagePath";
	/** Overlaid Image Opacity Property: "ImageWatermark.imageOpacity" */
	public static final String PROP_WATERMARK_IMAGEOPACITY = "ImageWatermark.imageOpacity";
	/** Default Image Opacity: 0.25f */
	public static final float DEFAULT_IMAGEOPACITY = 0.25f;
	private BufferedImage overlayImage = null;
	private float imageOpacity = DEFAULT_IMAGEOPACITY;
	
	/**
	 * Performs the transformation based on the provided global and instance properties.
	 * 
	 * @param bi the extracted region BufferedImage to be transformed
	 * @return the resulting BufferedImage or the same bi if no changes are made
	 * @throws TransformException
	 */
	public BufferedImage run(BufferedImage bi) throws TransformException {
		if (!isTransformable())
			return bi;
		Graphics2D graphics = bi.createGraphics();
	    graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, imageOpacity));
	    graphics.drawImage(overlayImage, bi.getWidth() - overlayImage.getWidth(), bi.getHeight() - overlayImage.getHeight(), null);
	    if (msg != null) {
	    	graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fontOpacity));
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(color);
			graphics.setFont(new Font(fontName, Font.PLAIN, fontSize));
			graphics.drawString(msg, 10, bi.getHeight() - 10);
		}
	    graphics.dispose();
	    return bi;
	}
	
	/**
	 * Initializes the implementation, overriding default values. Property keys
	 * are typically of the form ClassName.PropName. These are global instance fields.
	 * @param props Properties object containing implementation properties
	 */
	public void setup(Properties props) {
		super.setup(props);
		if (props.containsKey(PROP_WATERMARK_ALLOWED))
			allowedReferringEntity = (String) props.get(PROP_WATERMARK_ALLOWED);
		if (props.containsKey(PROP_WATERMARK_IMAGEOPACITY))
			imageOpacity = Float.parseFloat((String) props.get(PROP_WATERMARK_IMAGEOPACITY));
		String imagePath = null;
		if (props.containsKey(PROP_WATERMARK_OVERLAYIMAGE))
			imagePath = (String) props.get(PROP_WATERMARK_OVERLAYIMAGE);
		if (imagePath != null) {
		    try {
				overlayImage = ImageIO.read(new File(imagePath));
			}  catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Returns boolean indicator whether or not an image is transformable based on 
	 * the global and instance properties.  This is very helpful for cache logic.
	 * @return true if transformable
	 */
	public boolean isTransformable() {
		if (!super.isTransformable())
			return false;
		if (overlayImage == null)
			return false;
		if (addProps == null)
			return false;
		if (allowedReferringEntity == null || addProps.containsKey(PROPS_REFERRING_ENTITY) 
				&& addProps.get(PROPS_REFERRING_ENTITY).contains(allowedReferringEntity))
			return false;
		return true;
	}

	/**
	 * Sets the instance properties, from which per dissemination changes can be based on. 
	 * @param addProps HashMap object containing image transform instance properties
	 */
	public void setInstanceProps(HashMap<String, String> addProps) {
		super.addProps = addProps;
	}
}
