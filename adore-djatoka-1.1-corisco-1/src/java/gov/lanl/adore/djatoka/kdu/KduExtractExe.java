/*
 * Copyright (c) 2008  Los Alamos National Security, LLC.
 * With modifications by Brasiliana Digital Library (http://brasiliana.usp.br), 2010.
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

package gov.lanl.adore.djatoka.kdu;

import gov.lanl.adore.djatoka.DjatokaDecodeParam;
import gov.lanl.adore.djatoka.DjatokaException;
import gov.lanl.adore.djatoka.IExtract;
import gov.lanl.adore.djatoka.io.reader.PNMReader;
import gov.lanl.adore.djatoka.util.IOUtils;
import gov.lanl.adore.djatoka.util.ImageProcessingUtils;
import gov.lanl.adore.djatoka.util.ImageRecord;
import gov.lanl.adore.djatoka.util.JP2ImageInfo;
import gov.lanl.util.ExecuteStreamHandler;
import gov.lanl.util.PumpStreamHandler;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.StringTokenizer;

import kdu_jni.Jp2_family_src;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_codestream;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_params;

import org.apache.log4j.Logger;

import gov.lanl.util.ConfigurationManager;

/**
 * Java bridge for kdu_expand application
 * @author Ryan Chute
 *
 */
public class KduExtractExe implements IExtract {

    private static final Logger logger = Logger.getLogger(KduExtractExe.class);
    private static boolean isWindows = false;
    private static String env;
    private static String exe;
    private static String[] envParams;
    private final static BufferedImage OOB = getOutOfBoundsImage();
    /** Extract App Name "kdu_expand" */
    public static final String KDU_EXPAND_EXE = "kdu_expand";
    /** UNIX/Linux Standard Out Path: "/dev/stdout" */
    public static String STDOUT = "/dev/stdout";
    public static String STDIN = "/dev/stdin";
    private static final String PROPS_KAKADU_HOME = "kakadu.home";
    private static String kakaduHome;

    static {
        env = System.getProperty(KDU_EXPAND_EXE)
                + System.getProperty("file.separator");
        exe = env
                + ((System.getProperty("os.name").contains("Win")) ? KDU_EXPAND_EXE
                + ".exe"
                : KDU_EXPAND_EXE);
        if (System.getProperty("os.name").startsWith("Mac")) {
            envParams = new String[]{"DYLD_LIBRARY_PATH="
                        + System.getProperty("DYLD_LIBRARY_PATH")};
        } else if (System.getProperty("os.name").startsWith("Win")) {
            isWindows = true;
        } else if (System.getProperty("os.name").startsWith("Linux")) {
            envParams = new String[]{"LD_LIBRARY_PATH="
                        + System.getProperty("LD_LIBRARY_PATH")};
        } else if (System.getProperty("os.name").startsWith("Solaris")) {
            envParams = new String[]{"LD_LIBRARY_PATH="
                        + System.getProperty("LD_LIBRARY_PATH")};
        }
        logger.debug("envParams: " + ((envParams != null) ? envParams[0] + " | " : "") + exe);
        kakaduHome = System.getProperty(KDU_EXPAND_EXE);
        logger.info("Setting " + KDU_EXPAND_EXE + ": " + System.getProperty(KDU_EXPAND_EXE));
    }

    public KduExtractExe() {
        if (kakaduHome == null) {
            logger.info("Trying to set kakadu.home.");
            try {
                kakaduHome = ConfigurationManager.getProperty(PROPS_KAKADU_HOME);
            } catch (Exception e) {
                logger.info(e, e);
                kakaduHome = System.getProperty(PROPS_KAKADU_HOME);
            }
        }

        logger.debug("conf.kakadu.home: " + kakaduHome);
        env = kakaduHome + System.getProperty("file.separator");
        exe = env
                + ((System.getProperty("os.name").contains("Win"))
                ? KDU_EXPAND_EXE + ".exe"
                : KDU_EXPAND_EXE);
    }

    /**
     * Extracts region defined in DjatokaDecodeParam as BufferedImage
     * @param input InputStream containing a JPEG 2000 image bitstream.
     * @param params DjatokaDecodeParam instance containing region and transform settings.
     * @return extracted region as a BufferedImage
     * @throws DjatokaException
     */
    public BufferedImage processUsingTemp(InputStream input, DjatokaDecodeParam params)
            throws DjatokaException {
        File in;
        // Copy to tmp file
        try {
            in = File.createTempFile("tmp", ".jp2");
            FileOutputStream fos = new FileOutputStream(in);
            in.deleteOnExit();
            IOUtils.copyStream(input, fos);
        } catch (IOException e) {
            logger.error(e, e);
            throw new DjatokaException(e);
        }

        BufferedImage bi = process(in.getAbsolutePath(), params);

		if (in != null)
            in.delete();

        return bi;
    }

    /**
     * Extracts region defined in DjatokaDecodeParam as BufferedImage
     * @param is InputStream containing a JPEG 2000 image bitstream.
     * @param params DjatokaDecodeParam instance containing region and transform settings.
     * @return extracted region as a BufferedImage
     * @throws DjatokaException
     */
    public BufferedImage process(final InputStream is, DjatokaDecodeParam params) throws DjatokaException {
		if (isWindows)
            return processUsingTemp(is, params);

        ArrayList<Double> dims = null;
        if (params.getRegion() != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copyStream(is, baos);
            dims = getRegionMetadata(new ByteArrayInputStream(baos.toByteArray()), params);
            return process(new ByteArrayInputStream(baos.toByteArray()), dims, params);
		} else 
            return process(is, dims, params);
    }
    /**
     * Extracts region defined in DjatokaDecodeParam as BufferedImage
     * @param is InputStream containing a JPEG 2000 image bitstream.
     * @param dims region extraction dimensions
     * @param params DjatokaDecodeParam instance containing region and transform settings.
     * @return extracted region as a BufferedImage
     * @throws DjatokaException
     */
    public BufferedImage process(final InputStream is, ArrayList<Double> dims, DjatokaDecodeParam params) throws DjatokaException {
        String input = STDIN;
        String output = STDOUT;
        BufferedImage bi = null;
        try {
            final String command = getKduExtractCommand(input, output, dims, params);
            final Process process = Runtime.getRuntime().exec(command, envParams, new File(env));
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            ExecuteStreamHandler streamHandler = new PumpStreamHandler(stdout, stderr, is);
            try {
                streamHandler.setProcessInputStream(process.getOutputStream());
                streamHandler.setProcessOutputStream(process.getInputStream());
                streamHandler.setProcessErrorStream(process.getErrorStream());
            } catch (IOException e) {
                logger.error(e, e);
                if (process != null) {
                    closeStreams(process);
                }
                throw e;
            }
            streamHandler.start();

            try {
                waitFor(process);
                final ByteArrayInputStream bais = new ByteArrayInputStream(stdout.toByteArray());
                bi = new PNMReader().open(bais);
                streamHandler.stop();
            } catch (ThreadDeath t) {
                logger.error(t, t);
                process.destroy();
                throw t;
            } finally {
                if (process != null) {
                    closeStreams(process);
                }
            }
        } catch (Exception e) {
            logger.error(e, e);
            throw new DjatokaException(e);
        }
        return bi;
    }

    /**
     * Extracts region defined in DjatokaDecodeParam as BufferedImage
     * @param input absolute file path of JPEG 2000 image file.
     * @param params DjatokaDecodeParam instance containing region and transform settings.
     * @return extracted region as a BufferedImage
     * @throws DjatokaException
     */
    public BufferedImage process(String input, DjatokaDecodeParam params)
            throws DjatokaException {
        String output = STDOUT;
        File winOut = null;
        BufferedImage bi = null;
        if (isWindows) {
            try {
                winOut = File.createTempFile("pipe_", ".ppm");
                winOut.deleteOnExit();
            } catch (IOException e) {
                logger.error(e, e);
                throw new DjatokaException(e);
            }
            output = winOut.getAbsolutePath();
        }
        Runtime rt = Runtime.getRuntime();
        try {
            ArrayList<Double> dims = getRegionMetadata(input, params);
            String command = getKduExtractCommand(input, output, dims, params);
            final Process process = rt.exec(command, envParams, new File(env));

            if (output != null) {
                try {
                    if (output.equals(STDOUT)) {
                        bi = new PNMReader().open(new BufferedInputStream(process.getInputStream()));
                    } else if (isWindows) {
                        process.waitFor();
                        try {
                            bi = new PNMReader().open(new BufferedInputStream(new FileInputStream(new File(output))));
                        } catch (Exception e) {
                            logger.error(e, e);
						    if (winOut != null)
                                winOut.delete();
                            throw e;
                        }
						if (winOut != null)
                            winOut.delete();
                    }
                } catch (RuntimeException e) {
                    logger.debug("Request out of bounds");
                    bi = OOB;
                } catch (Exception e) {
                    String error = null;
                    try {
                        error = new String(IOUtils.getByteArray(process.getErrorStream()));
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    logger.error(error, e);
				    if (error != null)
                        throw new DjatokaException(error);
				    else 
                        throw new DjatokaException(e);
                } finally {
                    if (process != null) {
                        closeStreams(process);
                    }
                }
            }
        } catch (IOException e) {
            logger.error(e, e);
        }
        return bi;
    }

    /**
     * Extracts region defined in DjatokaDecodeParam as BufferedImage
     *
     * @param input
     *            ImageRecord wrapper containing file reference, inputstream,
     *            etc.
     * @param params
     *            DjatokaDecodeParam instance containing region and transform
     *            settings.
     * @return extracted region as a BufferedImage
     * @throws DjatokaException
     */
    public BufferedImage process(ImageRecord input, DjatokaDecodeParam params)
            throws DjatokaException {
        if (input.getImageFile() != null) {
            return process(input.getImageFile(), params);
        } else if (input.getObject() != null) {
            return process(getStreamFromObject(input.getObject()), params);
        } else {
            throw new DjatokaException(
                    "File not defined and Input Object Type "
                    + input.getObject().getClass().getName()
                    + " is not supported");
        }
    }

    /**
     * Gets Kdu Extract Command-line based on dims and params
     * @param input absolute file path of JPEG 2000 image file.
     * @param output absolute file path of PGM output image
     * @param dims array of region parameters (i.e. y,x,h,w)
     * @param params contains rotate and level extraction information
     * @return command line string to extract region using kdu_extract
     */
    public final String getKduExtractCommand(String input, String output,
            ArrayList<Double> dims, DjatokaDecodeParam params) {
        StringBuffer command = new StringBuffer(exe);
		if (input.equals(STDIN))
            command.append(" -no_seek");
        command.append(" -quiet -i ").append(escape(new File(input).getAbsolutePath()));
        command.append(" -o ").append(escape(new File(output).getAbsolutePath()));
        command.append(" ").append(toKduExtractArgs(params));
        if (dims != null && dims.size() == 4) {
            StringBuffer region = new StringBuffer();
            region.append("{").append(dims.get(0)).append(",").append(
                    dims.get(1)).append("}").append(",");
            region.append("{").append(dims.get(2)).append(",").append(
                    dims.get(3)).append("}");
            command.append("-region ").append(region.toString()).append(" ");
        }
		logger.debug(command.toString());
        return command.toString();
    }

    /**
     * Returns populated JPEG 2000 ImageRecord instance
     * @param r ImageRecord containing file path the JPEG 2000 image
     * @return a populated JPEG 2000 ImageRecord instance
     * @throws DjatokaException
     */
    public final ImageRecord getMetadata(ImageRecord r) throws DjatokaException {
		if (r == null)
            throw new DjatokaException("ImageRecord is null");
        if (r.getImageFile() == null && r.getObject() != null) {
            ImageRecord ir = getMetadata(getStreamFromObject(r.getObject()));
            ir.setObject(r.getObject());
            return ir;
        }
        File f = new File(r.getImageFile());
		if (!f.exists())
            throw new DjatokaException("Image Does Not Exist");
		if (!ImageProcessingUtils.checkIfJp2(r.getImageFile()))
            throw new DjatokaException("Not a JP2 image.");
        if (f.length() <= 4096) {
            // If < 4K bytes, image may be corrupt, use safer pure Java Metadata gatherer.
            try {
                return getMetadata(new FileInputStream(f));
            } catch (Exception e) {
                throw new DjatokaException("Invalid file.");
            }
        }

        Jpx_source inputSource = new Jpx_source();
        Jp2_family_src jp2_family_in = new Jp2_family_src();

        int ref_component = 0;
        try {
            jp2_family_in.Open(r.getImageFile(), true);
            inputSource.Open(jp2_family_in, true);
            Kdu_codestream codestream = new Kdu_codestream();
            codestream.Create(inputSource.Access_codestream(ref_component).Open_stream());

            int minLevels = codestream.Get_min_dwt_levels();
            int depth = codestream.Get_bit_depth(ref_component);
            int colors = codestream.Get_num_components();
            int[] frames = new int[1];
            inputSource.Count_compositing_layers(frames);
            Kdu_dims image_dims = new Kdu_dims();
            codestream.Get_dims(ref_component, image_dims);
            Kdu_coords imageSize = image_dims.Access_size();

            r.setWidth(imageSize.Get_x());
            r.setHeight(imageSize.Get_y());
            r.setDWTLevels(minLevels);
            int djatokaLevels = ImageProcessingUtils.getLevelCount(r.getWidth(), r.getHeight());
            r.setLevels((djatokaLevels > minLevels) ? minLevels : djatokaLevels);
            r.setBitDepth(depth);
            r.setNumChannels(colors);
            r.setCompositingLayerCount(frames[0]);

            int[] v = new int[1];
            Kdu_params p = codestream.Access_siz().Access_cluster("COD");
            if (p != null) {
                p.Get(Kdu_global.Clayers, 0, 0, v, true, true, true);
			    if (v[0] > 0)
                    r.setQualityLayers(v[0]);
            }

			if (codestream.Exists())
                codestream.Destroy();
            inputSource.Native_destroy();
            jp2_family_in.Native_destroy();
        } catch (KduException e) {
            logger.error(e, e);
            throw new DjatokaException(e);
        }

        return r;
    }

    /**
     * Returns populated JPEG 2000 ImageRecord instance
     * @param is an InputStream containing the JPEG 2000 codestream
     * @return a populated JPEG 2000 ImageRecord instance
     * @throws DjatokaException
     *
     */
    public final ImageRecord getMetadata(final InputStream is) throws DjatokaException {
        JP2ImageInfo info;
        try {
            info = new JP2ImageInfo(is);
        } catch (IOException e) {
            logger.error(e, e);
            throw new DjatokaException(e);
        }
        return info.getImageRecord();
    }

    /**
     * Returns array of XMLBox records contained in JP2 resource.
     * @param r an ImageRecord containing a file path to resource or has object defined
     * @return an array of XML records contained in JP2 XMLboxes
     */
    public final String[] getXMLBox(ImageRecord r) throws DjatokaException {
        String[] xml = null;
        try {
            if (r.getImageFile() == null && r.getObject() != null) {
                xml = new JP2ImageInfo(getStreamFromObject(r.getObject())).getXmlDocs();
            } else {
                xml = new JP2ImageInfo(new File(r.getImageFile())).getXmlDocs();
            }
        } catch (IOException e) {
            logger.error(e, e);
        }
        return xml;
    }

    /**
     * Utility method to determine type of object stored in ImageRecord
     * and to return it as an InputStream
     * @param o
     * @return an InputStream for the resource contained in ImageRecord object
     */
    public static InputStream getStreamFromObject(Object o) {
		if (o instanceof BufferedInputStream)
            return (InputStream) o;
		if (o instanceof InputStream)
            return new BufferedInputStream((InputStream) o);
		if (o instanceof byte[])
            return new ByteArrayInputStream((byte[]) o);
        logger.error(o.getClass().getName() + " is not a supported ImageRecord object type.");
        return null;
    }

    private final ArrayList<Double> getRegionMetadata(InputStream input,
            DjatokaDecodeParam params) throws DjatokaException {
        ImageRecord r = getMetadata(input);
        return getRegionMetadata(r, params);
    }

    private final ArrayList<Double> getRegionMetadata(String input,
            DjatokaDecodeParam params) throws DjatokaException {
        ImageRecord r = getMetadata(new ImageRecord(input));
        return getRegionMetadata(r, params);
    }

    private final ArrayList<Double> getRegionMetadata(ImageRecord r, DjatokaDecodeParam params)
            throws DjatokaException {
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

        int reduce = 1 << params.getLevelReductionFactor();
        ArrayList<Double> dims = new ArrayList<Double>();

        if (params.getRegion() != null) {
            StringTokenizer st = new StringTokenizer(params.getRegion(), "{},");
            String token;
            // top
			if ((token = st.nextToken()).contains("."))
                dims.add(Double.parseDouble(token));
			else {
                int t = Integer.parseInt(token);
				if (r.getHeight() < t)
                    throw new DjatokaException("Region inset out of bounds: " + t + ">" + r.getHeight());
                dims.add(Double.parseDouble(token) / r.getHeight());
            }
            // left
            if ((token = st.nextToken()).contains(".")) {
                dims.add(Double.parseDouble(token));
            } else {
                int t = Integer.parseInt(token);
				if (r.getWidth() < t)
                    throw new DjatokaException("Region inset out of bounds: " + t + ">" + r.getWidth());
                dims.add(Double.parseDouble(token) / r.getWidth());
            }
            // height
            if ((token = st.nextToken()).contains(".")) {
                dims.add(Double.parseDouble(token));
			} else
                dims.add(Double.parseDouble(token)
						/ (Double.valueOf(r.getHeight()) / Double
								.valueOf(reduce)));
            // width
            if ((token = st.nextToken()).contains(".")) {
                dims.add(Double.parseDouble(token));
			} else
                dims.add(Double.parseDouble(token)
						/ (Double.valueOf(r.getWidth()) / Double
								.valueOf(reduce)));
            }

        return dims;
    }

    private static BufferedImage getOutOfBoundsImage() {
        BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        int rgb = bi.getRGB(0, 0);
        int alpha = (rgb >> 24) & 0xff;
        bi.setRGB(0, 0, alpha);
        return bi;
    }

    private static String toKduExtractArgs(DjatokaDecodeParam params) {
        StringBuffer sb = new StringBuffer();
	    if (params.getLevelReductionFactor() > 0)
            sb.append("-reduce ").append(params.getLevelReductionFactor()).append(" ");
	    if (params.getRotationDegree() > 0)
            sb.append("-rotate ").append(params.getRotationDegree()).append(" ");
	    if (params.getCompositingLayer() > 0)
            sb.append("-jpx_layer ").append(params.getCompositingLayer()).append(" ");
        return sb.toString();
    }

    private static final String escape(String path) {
		if (path.contains(" "))
            path = "\"" + path + "\"";
        return path;
    }

    // Process Handler Utils
    private int waitFor(Process process) {
        try {
            process.waitFor();
            return process.exitValue();
        } catch (InterruptedException e) {
            process.destroy();
        }
        return 2;
    }
    private static void closeStreams(Process process) {
        close(process.getInputStream());
        close(process.getOutputStream());
        close(process.getErrorStream());
        process.destroy();
    }
    private static void close(InputStream device) {
        if (device != null) {
            try {
                device.close();
            } catch (IOException ioex) {
            }
        }
    }
    private static void close(OutputStream device) {
        if (device != null) {
            try {
                device.close();
            } catch (IOException ioex) {
            }
        }
    }
}
