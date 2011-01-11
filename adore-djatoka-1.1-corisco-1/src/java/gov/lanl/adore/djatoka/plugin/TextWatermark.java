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
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Properties;

/**
 * Applies a Textual Visual Watermark on the image
 * @author Ryan Chute
 *
 */
public class TextWatermark implements ITransformPlugIn {
	/** Requester Instance Property: "requester" */
    public static final String PROPS_REQUESTER = "requester";
    /** Referring Entity Instance Property: "referringEntity" */
    public static final String PROPS_REFERRING_ENTITY = "referringEntity";
    /** Watermark Copyright Statement Property: "Watermark.statement" */
	public static final String PROP_WATERMARK_COPYRIGHT = "Watermark.statement";
	/**
	 * Watermark Allowed Domain Property: "Watermark.allowedDomain". If defined,
	 * only images requested outside the defined domain will have the watermark
	 * applied.
	 */
	public static final String PROP_WATERMARK_ALLOWED = "Watermark.allowedDomain";
	/** Watermark Copyright Font Name Property: "Watermark.fontName" */
	public static final String PROP_WATERMARK_FONTNAME = "Watermark.fontName";
	/** Watermark Copyright Font Size Property: "Watermark.fontSize" */
	public static final String PROP_WATERMARK_FONTSIZE = "Watermark.fontSize";
	/** Watermark Copyright Font Color Property: "Watermark.fontColor" */
	public static final String PROP_WATERMARK_FONTCOLOR = "Watermark.fontColor";
	/** Watermark Copyright Font Opacity Property: "Watermark.fontOpacity" */
	public static final String PROP_WATERMARK_FONTOPACITY = "Watermark.fontOpacity";
	/** Default Font Color: 255,255,255,255 */
	public static final Color DEFAULT_COLOR = new Color(255, 255, 255, 255);
	/** Default Font Opacity: 0.5f */
	public static final float DEFAULT_FONTOPACITY = 0.5f;
	protected String msg = null;
	protected String allowedReferringEntity = null;
	protected Color color = DEFAULT_COLOR;
	protected String fontName = "Arial";
	protected int fontSize = 12;
	protected float fontOpacity = DEFAULT_FONTOPACITY;
	protected HashMap<String, String> addProps;
	
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
	    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	    graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fontOpacity));
	    graphics.setColor(color);
	    graphics.setFont(new Font(fontName,Font.PLAIN,fontSize));
	    graphics.drawString(msg, 10, bi.getHeight() - 10);
        return bi;
	}
	
	/**
	 * Initializes the implementation, overriding default values. Property keys
	 * are typically of the form ClassName.PropName.
	 * @param props Properties object containing implementation properties
	 */
	public void setup(Properties props) {
		if (props.containsKey(PROP_WATERMARK_COPYRIGHT))
			msg = (String) props.get(PROP_WATERMARK_COPYRIGHT);
		if (props.containsKey(PROP_WATERMARK_ALLOWED))
			allowedReferringEntity = (String) props.get(PROP_WATERMARK_ALLOWED);
		if (props.containsKey(PROP_WATERMARK_FONTNAME))
			fontName = (String) props.get(PROP_WATERMARK_FONTNAME);
		if (props.containsKey(PROP_WATERMARK_FONTSIZE))
			fontSize = Integer.parseInt((String) props.get(PROP_WATERMARK_FONTSIZE));
		if (props.containsKey(PROP_WATERMARK_FONTCOLOR)) {
			String[] c = ((String) props.get(PROP_WATERMARK_FONTCOLOR)).split(",");
			if (c.length > 3)
				color = new Color(
						Integer.parseInt(c[0]),
						Integer.parseInt(c[1]),
						Integer.parseInt(c[2]),
						Integer.parseInt(c[3]));
			else if (c.length == 3)
				color = new Color(
						Integer.parseInt(c[0]),
						Integer.parseInt(c[1]),
						Integer.parseInt(c[2]),
						150);
		}
		
	}

	/**
	 * Sets the instance properties, from which per dissemination changes can be based on. 
	 * @param addProps HashMap object containing image transform instance properties
	 */
	public void setInstanceProps(HashMap<String, String> addProps) {
		this.addProps = addProps;
	}
	
	/**
	 * Returns boolean indicator whether or not an image is transformable based on 
	 * the global and instance properties.  This is very helpful for cache logic.
	 * @return true if transformable
	 */
	public boolean isTransformable() {
		if (addProps == null)
			return false;
		if (msg == null)
			return false;
		if (allowedReferringEntity == null || addProps.containsKey(PROPS_REFERRING_ENTITY) 
				&& addProps.get(PROPS_REFERRING_ENTITY).contains(allowedReferringEntity))
			return false;
		return true;
	}
}
