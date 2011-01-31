/*
 * Copyright (c) 2010 Brasiliana Digital Library (http://brasiliana.usp.br).
 * Based on similar source code from Djatoka.
 */

package gov.lanl.adore.djatoka.plugin;

import gov.lanl.adore.djatoka.DjatokaDecodeParam;
import gov.lanl.adore.djatoka.DjatokaException;
import gov.lanl.adore.djatoka.IExtract;
import gov.lanl.adore.djatoka.openurl.OpenURLJP2KService;
import gov.lanl.adore.djatoka.util.IOUtils;
import gov.lanl.adore.djatoka.util.ImageProcessingUtils;
import gov.lanl.adore.djatoka.util.ImageRecord;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.logging.Level;

import org.apache.log4j.Logger;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.Info;
import org.im4java.core.InfoException;
import org.im4java.core.PDFInfo;
import org.im4java.core.Stream2BufferedImage;

/**
 * Uses im4java library to extract PDF pages.  
 * @author Fabio Kepler
 *
 */
public class ExtractPDF implements IExtract {

	private static Logger logger = Logger.getLogger(ExtractPDF.class);

    private static int DEFAULT_DENSITY = 150;
    private static String DEFAULT_COLORSPACE = "RGB";
    private static int DEFAULT_LEVELS = 3;

	/**
	 * Returns PDF props in ImageRecord
	 * @param r ImageRecord containing absolute file path of PDF file.
	 * @return a populated ImageRecord object
	 * @throws DjatokaException
	 */
    @Override
	public final ImageRecord getMetadata(ImageRecord r) throws DjatokaException {
		if ((r.getImageFile() == null || !new File(r.getImageFile()).exists()) && r.getObject() == null)
			throw new DjatokaException("Image Does Not Exist: " + r.toString());
		logger.debug("get metadata: " + r.toString());
        try {
            DjatokaDecodeParam params = new DjatokaDecodeParam();
            BufferedImage bi = process(r, params);

			r.setWidth(bi.getWidth());
			r.setHeight(bi.getHeight());

            r.setDWTLevels(DEFAULT_LEVELS);
            r.setLevels(DEFAULT_LEVELS);

            r.setBitDepth(bi.getColorModel().getPixelSize());
            r.setNumChannels(bi.getColorModel().getNumColorComponents());

            logger.debug("setting compositing layer count");
            r.setCompositingLayerCount(getNumberOfPages(r)); // Semantics: number of pages in the PDF file.

            logger.debug("r: "+r.toString());
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

            r.setDWTLevels(DEFAULT_LEVELS);
            r.setLevels(DEFAULT_LEVELS);

            r.setBitDepth(bi.getColorModel().getPixelSize());
            r.setNumChannels(bi.getColorModel().getNumColorComponents());
            //r.setCompositingLayerCount(getNumberOfPages(r)); // 'bi' refers to just one page extracted from the PDF file.
            logger.debug("r2: "+r.toString());
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
	 * @param input absolute file path of PDF file.
	 * @param params DjatokaDecodeParam instance containing region and transform settings.
	 * @return extracted region as a BufferedImage
	 * @throws DjatokaException
	 */
    @Override
	public BufferedImage process(String input, DjatokaDecodeParam params)
			throws DjatokaException {
        input += "[" + params.getCompositingLayer() + "]";
        IMOperation op = new IMOperation();
        op.density(DEFAULT_DENSITY, DEFAULT_DENSITY);
        op.colorspace(DEFAULT_COLORSPACE);
        op.background("white");
        op.addImage(input);

        setScaleParam1(op, params);

        String crop = getCropParam(params);
        if (crop != null) {
            op.addRawArgs("-crop " + crop);
        }

        if (params.getRotationDegree() > 0) {
            op.rotate(params.getRotationDegree() * 1.0);
        }

        setScaleParam2(op, params);
        params.setScalingFactor(1.0);
        params.setScalingDimensions(null);

        op.addImage("png:-");

        logger.debug("op: " + op.toString());
        Stream2BufferedImage s2b = new Stream2BufferedImage();

        ConvertCmd cmd = new ConvertCmd();
        cmd.setOutputConsumer(s2b);
        try {
            cmd.run(op);
            BufferedImage bi = s2b.getImage();
            return bi;
        } catch (IOException ex) {
            logger.error(ex, ex);
        } catch (InterruptedException ex) {
            logger.error(ex, ex);
        } catch (IM4JavaException ex) {
            logger.error(ex, ex);
        }

        return null;
	}

    public BufferedImage processUsingTemp(InputStream input, DjatokaDecodeParam params)
            throws DjatokaException {
        File in;
        // Copy to tmp file
        try {
            String cacheDir = OpenURLJP2KService.getCacheDir();
            if (cacheDir != null) {
                in = File.createTempFile("tmp", ".pdf", new File(cacheDir));
            } else {
                in = File.createTempFile("tmp", ".pdf");
            }
            FileOutputStream fos = new FileOutputStream(in);
            in.deleteOnExit();
            IOUtils.copyStream(input, fos);
        } catch (IOException e) {
            logger.error(e, e);
            throw new DjatokaException(e);
        }

        BufferedImage bi = process(in.getAbsolutePath(), params);

        if (in != null) {
            in.delete();
        }

        return bi;
    }

	/**
	 * Extracts region defined in DjatokaDecodeParam as BufferedImage
	 * @param input InputStream containing a PDF bitstream.
	 * @param params DjatokaDecodeParam instance containing region and transform settings.
	 * @return extracted region as a BufferedImage
	 * @throws DjatokaException
	 */
    @Override
	public BufferedImage process(InputStream input, DjatokaDecodeParam params)
            throws DjatokaException {
        return processUsingTemp(input, params);
    }

    private BufferedImage applyParams(BufferedImage bi, DjatokaDecodeParam params) throws DjatokaException {
        ImageRecord r = getMetadata(bi);
        setLevelReduction(r, params);
        if (params.getLevelReductionFactor() > 0) {
            int reduce = 1 << params.getLevelReductionFactor(); // => image.size() / 2^r: reduce 0 means image/1, reduce 1 means image/2, etc.
            bi = ImageProcessingUtils.scale(bi, 1.0 / reduce);
        }
        if (params.getRegion() != null) {
            ArrayList<Double> dims = null;
            dims = getRegionMetadata(r, params); // Region info: dims[0..3] = Y,X,H,W
            logger.debug("dims: " + dims.toString());
            logger.debug("region: " + params.getRegion());
            logger.debug("reduce: " + params.getLevelReductionFactor());
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
        logger.debug("in imagerecord;");
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


    private static int getNumberOfPages(ImageRecord input) throws DjatokaException {
        logger.debug("getting number of pages");
        FileInputStream fis = null;
        if (input.getImageFile() != null) {
            try {
                logger.debug("PDFInfo image file: " + input.getImageFile());
                PDFInfo imageInfo = new PDFInfo(input.getImageFile());
                Enumeration<String> props = imageInfo.getPropertyNames();
                if (props == null) {
                    return 0;
                }
                while (props.hasMoreElements()) {
                    String prop = props.nextElement();
                    logger.debug("PDFInfo: " + prop + "=" + imageInfo.getProperty(prop));
                }
                String num = imageInfo.getProperty("Pages");
                logger.debug("num: " + num);
                int inum = Integer.parseInt(num);
                logger.debug("int num: " + inum);
                return inum;
            } catch (InfoException ex) {
                logger.error(ex, ex);
            }
        } else if (input.getObject() != null && (input.getObject() instanceof InputStream)) {
            fis = (FileInputStream) input.getObject();
            File in;
            // Copy to tmp file
            try {
                String cacheDir = OpenURLJP2KService.getCacheDir();
                if (cacheDir != null) {
                    in = File.createTempFile("tmp", ".pdf", new File(cacheDir));
                } else {
                    in = File.createTempFile("tmp", ".pdf");
                }

                FileOutputStream fos = new FileOutputStream(in);
                in.deleteOnExit();
                IOUtils.copyStream(fis, fos);
            } catch (IOException e) {
                logger.error(e, e);
                throw new DjatokaException(e);
            }

            try {
                PDFInfo imageInfo = new PDFInfo(in.getAbsolutePath());
                Enumeration<String> props = imageInfo.getPropertyNames();
                if (props == null) {
                    return 0;
                }
                while (props.hasMoreElements()) {
                    String prop = props.nextElement();
                    logger.debug("PDFInfo: " + prop + "=" + imageInfo.getProperty(prop));
                }

                String num = imageInfo.getProperty("Pages");
                return Integer.parseInt(num);
            } catch (InfoException ex) {
                logger.error(ex, ex);
            }

            if (in != null) {
                in.delete();
            }

            return 0;
        } else {
            throw new DjatokaException(
                    "File not defined and Input Object Type "
                    + input //.getObject().getClass().getName()
                    + " is not supported");
        }
        return 0;
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


    private boolean setScaleParam1(IMOperation op, DjatokaDecodeParam params) {
        if (params.getLevel() >= 0) {
            int levels = DEFAULT_LEVELS;
            int reduce = levels - params.getLevel();
            params.setLevelReductionFactor((reduce >= 0) ? reduce : 0);
        } else if (params.getLevel() == -1 && params.getRegion() == null && params.getScalingDimensions() != null) {
            int width = params.getScalingDimensions()[0];
            int height = params.getScalingDimensions()[1];
            op.scale(width, height);
            return true;
        }

        if (params.getLevelReductionFactor() > 0) {
            int reduce = 1 << params.getLevelReductionFactor(); // => image.size() / 2^r: reduce 0 means image/1, reduce 1 means image/2, etc.
            double s = 1.0 / reduce;
            op.scale((s * 100.0), Boolean.TRUE);
            return true;
        }
        return false;
    }

    private boolean setScaleParam2(IMOperation op, DjatokaDecodeParam params) {
        if (params.getScalingFactor() != 1.0
                && params.getScalingFactor() > 0
                && params.getScalingFactor() < 3) {
            op.scale(params.getScalingFactor() * 100.0, true);
            return true;
        } else if (params.getScalingDimensions() != null
                && params.getScalingDimensions().length == 2) {
            int width = params.getScalingDimensions()[0];
            int height = params.getScalingDimensions()[1];
            op.scale(width, height);
        }
        return false;
    }

    private String getCropParam(DjatokaDecodeParam params) {
        String crop = null;

        if (params.getRegion() != null) {
            StringTokenizer st = new StringTokenizer(params.getRegion(), "{},");
            String token;
            logger.info("region params: " + params.getRegion());
            int x, y;
            String w, h;
            // top
            if ((token = st.nextToken()).contains(".")) {
                y = Integer.parseInt(token);
            } else {
                y = Integer.parseInt(token);
            }
            // left
            if ((token = st.nextToken()).contains(".")) {
                x = Integer.parseInt(token);
            } else {
                x = Integer.parseInt(token);
            }
            // height
            if ((token = st.nextToken()).contains(".")) {
                h = (Double.parseDouble(token) * 100.0) + "%";
            } else {
                h = token;
            }
            // width
            if ((token = st.nextToken()).contains(".")) {
                w = (Double.parseDouble(token) * 100.0) + "%";
            } else {
                w = token;
            }
            
            crop = w + "x" + h + "+" + x + "+" + y;
        }

        return crop;
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

    private static final String escape(String path) {
        if (path.contains(" ")) {
            path = "\"" + path + "\"";
        }
        return path;
    }

}
