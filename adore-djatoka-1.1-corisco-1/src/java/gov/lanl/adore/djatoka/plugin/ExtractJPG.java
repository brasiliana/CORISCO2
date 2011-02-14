/*
 * Copyright (c) 2010 Brasiliana Digital Library (http://brasiliana.usp.br).
 * Based on similar source code from Adore Djatoka.
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

import javax.imageio.ImageIO;

import gov.lanl.adore.djatoka.DjatokaDecodeParam;
import gov.lanl.adore.djatoka.DjatokaException;
import gov.lanl.adore.djatoka.IExtract;
import gov.lanl.adore.djatoka.util.ImageProcessingUtils;
import gov.lanl.adore.djatoka.util.ImageRecord;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Level;
import org.apache.log4j.Logger;

/**
 * Uses JAI library to extract JPEG regions.  
 * @author Fabio N. Kepler
 *
 */
public class ExtractJPG implements IExtract {

	private static Logger logger = Logger.getLogger(ExtractJPG.class);
	/**
	 * Returns JPEG props in ImageRecord
	 * @param r ImageRecord containing absolute file path of JPEG image file.
	 * @return a populated ImageRecord object
	 * @throws DjatokaException
	 */
    @Override
	public final ImageRecord getMetadata(ImageRecord r) throws DjatokaException {
		if ((r.getImageFile() == null || !new File(r.getImageFile()).exists()) && r.getObject() == null)
			throw new DjatokaException("Image Does Not Exist: " + r.toString());
		
        try {
            /*
            JPEGImageDecoder decoder = null;
            if (r.getImageFile() != null && new File(r.getImageFile()).exists()) {
                decoder = JPEGCodec.createJPEGDecoder(new FileInputStream(r.getImageFile()));
            } else {
                decoder = JPEGCodec.createJPEGDecoder((InputStream) r.getObject());
            }
            BufferedImage bi = decoder.decodeAsBufferedImage();
            */
            BufferedImage bi = null;
            if (r.getImageFile() != null && new File(r.getImageFile()).exists()) {
                bi = ImageIO.read(new File(r.getImageFile()));
            } else {
                bi = ImageIO.read((InputStream) r.getObject());
            }
            if (bi == null)
                return null;

            r.setWidth(bi.getWidth());
            r.setHeight(bi.getHeight());

            int minLevels = 3; //FIXME
            int djatokaLevels = ImageProcessingUtils.getLevelCount(r.getWidth(), r.getHeight(), 128);
            r.setDWTLevels((djatokaLevels < minLevels) ? minLevels : djatokaLevels);
            r.setLevels((djatokaLevels < minLevels) ? minLevels : djatokaLevels);

            r.setBitDepth(bi.getColorModel().getPixelSize());
            r.setNumChannels(bi.getColorModel().getNumColorComponents());
//            r.setCompositingLayerCount(frames[0]);
		} catch (Exception e) {
			throw new DjatokaException(e);
		}

		return r;
	}

    public final ImageRecord getMetadata(BufferedImage bi) throws DjatokaException {
		if (bi == null)
			throw new DjatokaException("Image Does Not Exist");

        try {
            ImageRecord r = new ImageRecord();

			r.setWidth(bi.getWidth());
			r.setHeight(bi.getHeight());

            int minLevels = 5;
			r.setDWTLevels(minLevels); //FIXME
            int djatokaLevels = ImageProcessingUtils.getLevelCount(r.getWidth(), r.getHeight());
            r.setLevels((djatokaLevels > minLevels) ? minLevels : djatokaLevels);

            r.setBitDepth(bi.getColorModel().getPixelSize());
            r.setNumChannels(bi.getColorModel().getNumColorComponents());
//            r.setCompositingLayerCount(frames[0]);
            return r;
		} catch (Exception e) {
			throw new DjatokaException(e);
		}
	}

    @Override
    // TODO
    // FIXME
	public final String[] getXMLBox(ImageRecord r) throws DjatokaException {
		String[] xml = null;
		try {
			if (r.getImageFile() == null && r.getObject() != null
					&& r.getObject() instanceof InputStream) {
//				xml = new JP2ImageInfo((InputStream) r.getObject()).getXmlDocs();
			} else {
//				xml = new JP2ImageInfo(new File(r.getImageFile())).getXmlDocs();
			}
		} catch (Exception e) {
			logger.error(e, e);
		}
		return xml;
	}
	
	/**
	 * Extracts region defined in DjatokaDecodeParam as BufferedImage
	 * @param input absolute file path of JPEG image file.
	 * @param params DjatokaDecodeParam instance containing region and transform settings.
	 * @return extracted region as a BufferedImage
	 * @throws DjatokaException
	 */
    @Override
	public BufferedImage process(String input, DjatokaDecodeParam params)
			throws DjatokaException {
        InputStream in = null;
        try {
            in = new FileInputStream(input);
            return process(in, params);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ExtractJPG.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(ExtractJPG.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
	}

	/**
	 * Extracts region defined in DjatokaDecodeParam as BufferedImage
	 * @param input InputStream containing a JPEG image bitstream.
	 * @param params DjatokaDecodeParam instance containing region and transform settings.
	 * @return extracted region as a BufferedImage
	 * @throws DjatokaException
	 */
    @Override
	public BufferedImage process(InputStream input, DjatokaDecodeParam params)
            throws DjatokaException {
        try {
            BufferedImage bi = ImageIO.read(input);

            ImageRecord r = getMetadata(bi);
            setLevelReduction(r, params);
            if (params.getLevelReductionFactor() > 0) {
                int reduce = 1 << params.getLevelReductionFactor(); // => image.size() / 2^r: reduce 0 means image/1, reduce 1 means image/2, etc.
                bi = ImageProcessingUtils.scale(bi, 1.0 / reduce);
            }


            if (params.getRegion() != null) {
                ArrayList<Double> dims = null;
                dims = getRegionMetadata(r, params); // Region info: dims[0..3] = Y,X,H,W
                logger.debug("dims: "+dims.toString());
                logger.debug("region: "+params.getRegion());
                logger.debug("reduce: "+params.getLevelReductionFactor());
                if (dims != null && dims.size() == 4) {
                    double x = dims.get(1);
                    double y = dims.get(0);
                    double w = dims.get(3);
                    double h = dims.get(2);
                    bi = ImageProcessingUtils.clipRegion(bi, x, y, w, h); // dims[0..3] = Y,X,H,W
                }
            }
            if (params.getRotationDegree() > 0) {
                bi = ImageProcessingUtils.rotate(bi, params.getRotationDegree());
            }
            return bi;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ExtractJPG.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            java.util.logging.Logger.getLogger(ExtractJPG.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                input.close();
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(ExtractJPG.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
	}
	
	/**
	 * Extracts region defined in DjatokaDecodeParam as BufferedImage
	 * @param input ImageRecord wrapper containing file reference, inputstream, etc.
	 * @param params DjatokaDecodeParam instance containing region and transform settings.
	 * @return extracted region as a BufferedImage
	 * @throws DjatokaException
	 */
    @Override
	public BufferedImage process(ImageRecord input, DjatokaDecodeParam params)
			throws DjatokaException {
		if (input.getImageFile() != null)
			return process(input.getImageFile(), params);
		else if (input.getObject() != null
				&& (input.getObject() instanceof InputStream))
			return process((InputStream) input.getObject(), params);
		else
			throw new DjatokaException(
					"File not defined and Input Object Type "
							+ input.getObject().getClass().getName()
							+ " is not supported");
	}

    private void setLevelReduction(ImageRecord r, DjatokaDecodeParam params) {
        if (params.getLevel() >= 0) {
            int levels = ImageProcessingUtils.getLevelCount(r.getWidth(), r.getHeight());
            levels = (r.getDWTLevels() < levels) ? r.getDWTLevels() : levels;
            int reduce = levels - params.getLevel();
            params.setLevelReductionFactor((reduce >= 0) ? reduce : 0);
        } else if (params.getLevel() == -1 && params.getRegion() == null && params.getScalingDimensions() != null) {
            int width = params.getScalingDimensions()[0];
            int height = params.getScalingDimensions()[1];
            int levels = ImageProcessingUtils.getLevelCount(r.getWidth(), r.getHeight());
            int scale_level = ImageProcessingUtils.getScalingLevel(r.getWidth(), r.getHeight(), width, height);
            levels = (r.getDWTLevels() < levels) ? r.getDWTLevels() : levels;
            int reduce = levels - scale_level;
            System.out.println(reduce);
            params.setLevelReductionFactor((reduce >= 0) ? reduce : 0);
        }
    }
    
    private ArrayList<Double> getRegionMetadata(ImageRecord r, DjatokaDecodeParam params)
            throws DjatokaException {

        int reduce = 1 << params.getLevelReductionFactor(); // => image.size() / 2^r: reduce 0 means image/1, reduce 1 means image/2, etc.
        ArrayList<Double> dims = new ArrayList<Double>();

        if (params.getRegion() != null) {
            StringTokenizer st = new StringTokenizer(params.getRegion(), "{},");
            String token;
            logger.info("region params: " + params.getRegion());
            // top
            if ((token = st.nextToken()).contains(".")) {
                dims.add(Double.parseDouble(token));
            } else {
                int t = Integer.parseInt(token);
                if (r.getHeight() < t) {
                    throw new DjatokaException("Region inset out of bounds: " + t + ">" + r.getHeight());
                }
                dims.add(Double.parseDouble(token) / r.getHeight());
            }
            // left
            if ((token = st.nextToken()).contains(".")) {
                dims.add(Double.parseDouble(token));
            } else {
                int t = Integer.parseInt(token);
                if (r.getWidth() < t) {
                    throw new DjatokaException("Region inset out of bounds: " + t + ">" + r.getWidth());
                }
                dims.add(Double.parseDouble(token) / r.getWidth());
            }
            // height
            if ((token = st.nextToken()).contains(".")) {
                dims.add(Double.parseDouble(token));
            } else {
                dims.add(Double.parseDouble(token) / (Double.valueOf(r.getHeight()) / Double.valueOf(reduce)));
//                dims.add(Double.parseDouble(token) / Double.valueOf(r.getHeight()));
            }
            // width
            if ((token = st.nextToken()).contains(".")) {
                dims.add(Double.parseDouble(token));
            } else {
                dims.add(Double.parseDouble(token) / (Double.valueOf(r.getWidth()) / Double.valueOf(reduce)));
//                dims.add(Double.parseDouble(token) / Double.valueOf(r.getWidth()));
            }
        }

        return dims;
    }

}
