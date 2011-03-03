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
import java.awt.Dimension;

import javax.imageio.ImageIO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import java.nio.ByteBuffer;
//import java.nio.channels.FileChannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.logging.Level;

import org.apache.log4j.Logger;

import gov.lanl.util.ConfigurationManager;

/*
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.Info;
import org.im4java.core.InfoException;
import org.im4java.core.PDFInfo;
import org.im4java.core.Stream2BufferedImage;
*/


/**
 * Uses Poppler PDF commands to extract PDF pages to PNG files.
 * @author Fabio N. Kepler
 */
public class ExtractPDF implements IExtract {

    private static Logger logger = Logger.getLogger(ExtractPDF.class);

    // maximum size of either preview image dimension
    private static final int MAX_PX = 800;

    // maxium DPI - use common screen res, 100dpi.
    private static final int MAX_DPI = 150;

    private static int DEFAULT_DENSITY = 150;
    private static String DEFAULT_COLORSPACE = "RGB";
    private static int DEFAULT_LEVELS = 4;

    // command to get image from PDF; @FILE@, @OUTPUT@ are placeholders
    private static final String PDFTOPPM_COMMAND[] =
    {
        "@COMMAND@", "-q", "-png", "-f", "@FIRSTPAGE@", "-l", "@LASTPAGE@",
        "-r", "@DPI@", "@FILE@", "@OUTPUTFILE@"
    };
    private static final int PDFTOPPM_COMMAND_POSITION_BIN = 0;
    private static final int PDFTOPPM_COMMAND_POSITION_FIRSTPAGE = 4;
    private static final int PDFTOPPM_COMMAND_POSITION_LASTPAGE = 6;
    private static final int PDFTOPPM_COMMAND_POSITION_DPI = 8;
    //private static final int PDFTOPPM_COMMAND_POSITION_OPTIONAL_EXTRAS = 9;
    private static final int PDFTOPPM_COMMAND_POSITION_FILE = 9;
    private static final int PDFTOPPM_COMMAND_POSITION_OUTPUTFILE = 10;
    

    // command to get image from PDF; @FILE@, @OUTPUT@ are placeholders
    private static final String PDFINFO_COMMAND[] =
    {
        "@COMMAND@", "-f", "@FIRSTPAGE@", "-l", "@LASTPAGE@", "-box", "@FILE@"
        //"@COMMAND@", "-f", "@FP@", "-l", "@LP@", "-box", "@FILE@"
    };
    private static final int PDFINFO_COMMAND_POSITION_BIN = 0;
    private static final int PDFINFO_COMMAND_POSITION_FIRSTPAGE = 2;
    private static final int PDFINFO_COMMAND_POSITION_LASTPAGE = 4;
    private static final int PDFINFO_COMMAND_POSITION_FILE = 6;


    // executable path for "pdftoppm", comes from DSpace config at runtime.
    private static String pdftoppmPath = null;

    // executable path for "pdfinfo", comes from DSpace config at runtime.
    private static String pdfinfoPath = null;

    // match line in pdfinfo output that describes file's MediaBox
    private static final Pattern MEDIABOX_PATT = Pattern.compile(
        //"^Page\\s+(\\d+)\\s+size:\\s+([\\.\\d]+)\\s+x\\s+([\\.\\d]+)\\s+pts\\.*$"); // Does not seem to match "Page   41 size: 595 x 842 pts (A4)".
        "^Page\\s+(\\d+)\\s+MediaBox:\\s+([\\.\\d-]+)\\s+([\\.\\d-]+)\\s+([\\.\\d-]+)\\s+([\\.\\d-]+)"); // For use with -box switch
    /* Without the -box switch:
        Page    1 size: 444.72 x 771.12 pts
        Page    2 size: 416.16 x 743.52 pts
    */
    /* With -box switch:
        Page    1 size: 444.72 x 771.12 pts
        Page    2 size: 416.16 x 743.52 pts
        Page    1 MediaBox:     0.00     0.00   444.72   771.12
        Page    1 CropBox:      0.00     0.00   444.72   771.12
        Page    1 BleedBox:     0.00     0.00   444.72   771.12
        Page    1 TrimBox:      0.00     0.00   444.72   771.12
        Page    1 ArtBox:       0.00     0.00   444.72   771.12
        Page    2 MediaBox:     0.00     0.00   416.16   743.52
        Page    2 CropBox:      0.00     0.00   416.16   743.52
        Page    2 BleedBox:     0.00     0.00   416.16   743.52
        Page    2 TrimBox:      0.00     0.00   416.16   743.52
        Page    2 ArtBox:       0.00     0.00   416.16   743.52
    */

    // match line in pdfinfo output that describes file's MediaBox
    private static final Pattern PAGES_PATT = Pattern.compile(
        "^Pages:\\s+([\\d-]+)");


    private static Properties props = new Properties();

    private static final String DEFAULT_PDFTOPPM_PATH = "/usr/bin/pdftoppm";
    private static final String DEFAULT_PDFINFO_PATH = "/usr/bin/pdfinfo";

    private static final String PROPS_PDF_PDFTOPPM_PATH = "PDF.pdftoppmPath";
    private static final String PROPS_PDF_PDFINFO_PATH = "PDF.pdfinfoPath";


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
		//logger.debug("Get metadata: " + r.toString());
        try {
            DjatokaDecodeParam params = new DjatokaDecodeParam();
            BufferedImage bi = process(r, params);

			r.setWidth(bi.getWidth());
			r.setHeight(bi.getHeight());
            r.setDWTLevels(DEFAULT_LEVELS);
            r.setLevels(DEFAULT_LEVELS);
            r.setBitDepth(bi.getColorModel().getPixelSize());
            r.setNumChannels(bi.getColorModel().getNumColorComponents());
            
            //logger.debug("setting compositing layer count");
            //r.setCompositingLayerCount(getNumberOfPages(r)); // Semantics: number of pages in the PDF file.
            HashMap<String, String> pdfProps = (HashMap<String, String>)getPDFProperties(r);
            int n = Integer.parseInt(pdfProps.remove("Pages"));
            r.setCompositingLayerCount(n);
            
            // Since it is not possible for the viewer to query about a specific page's width and height
            // (because in Djatoka's point of view a PDF is just one image with various compositing layers, which are the pages),
            // at this point right here we query the PDF file about the size of all pages and store this
            // information in a Map. This map can be returned by getMetadata by setting it as the instProps member of the
            // ImageRecord class, which Djatoka already implements and which is returned as JSON to viewer.
            // The viewer then has to store this information and later query it instead of asking Djatoka (getMetadata) again.
            //Map<String, String> instProps = getPagesSizes(r);
            r.setInstProps(pdfProps);
            logger.debug("instProps: " + r.getInstProps());

            logger.debug("Get metadata: "+r.toString());
		} catch (Exception e) {
			throw new DjatokaException(e);
		}

		return r;
	}


//*
    public final ImageRecord getMetadata(BufferedImage bi) throws DjatokaException {
		if (bi == null)
			throw new DjatokaException("Image Does Not Exist");

        logger.debug("getMetadata(BufferedImage): " + bi.getWidth());
        try {
            ImageRecord r = new ImageRecord();

			r.setWidth(bi.getWidth());
			r.setHeight(bi.getHeight());

            r.setDWTLevels(DEFAULT_LEVELS);
            r.setLevels(DEFAULT_LEVELS);

            r.setBitDepth(bi.getColorModel().getPixelSize());
            r.setNumChannels(bi.getColorModel().getNumColorComponents());
            //r.setCompositingLayerCount(getNumberOfPages(r)); // 'bi' refers to just one page extracted from the PDF file.
            //logger.debug("r2: "+r.toString());
            
            //TODO
            
            return r;
		} catch (Exception e) {
			throw new DjatokaException(e);
		}
	}
//*/

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
			
		logger.debug("ExtractPDF.process:\n\tinput: " + input + "\n\tparams: " + params);
		
        if (input == null)
            throw new DjatokaException("Unknown failure while converting file: no image produced.");
            
		try {
		    setPDFCommandsPath();
	    } catch (IllegalStateException e) {
            logger.error("Failed to set PDF commands path: ",e);
            throw e;
        }
			
        int page_number = 1 + params.getCompositingLayer(); // From 0-based to 1-based.
        int status = 0;
        BufferedImage processedImage = null;
        try
        {
            // First get max physical dim of bounding box of the page
            // to compute the DPI to ask for..  otherwise some AutoCAD
            // drawings can produce enormous files even at 75dpi, for
            // 48" drawings..
            int dpi = 0;
            Dimension pageSize = getPDFPageSize(input, page_number);
            if (pageSize == null)
            {
                logger.error("Sanity check: Did not find \"Page " + page_number + " size\" line in output of pdfinfo, file="+input);
                throw new IllegalArgumentException("Failed to get \"Page " + page_number + " size\" of PDF with pdfinfo.");
            }
            else
            {
                double w = pageSize.getWidth();
                double h = pageSize.getHeight();
                int maxdim = (int)Math.max(Math.abs(w), Math.abs(h));
                dpi = Math.min(MAX_DPI, (MAX_PX * 72 / maxdim));
                logger.debug("DPI: pdfinfo method got dpi="+dpi+" for max dim="+maxdim+" (points, 1/72\")");
            }

            // Requires Sun JAI imageio additions to read ppm directly.
            // this will get "-000001.ppm" appended to it by pdftoppm
            File outPrefixF = File.createTempFile("pdftopng", "out");
            String outPrefix = outPrefixF.toString();
            outPrefixF.delete();

            String pdfCmd[] = PDFTOPPM_COMMAND.clone();
            pdfCmd[PDFTOPPM_COMMAND_POSITION_BIN] = pdftoppmPath;
            pdfCmd[PDFTOPPM_COMMAND_POSITION_FIRSTPAGE] = "" + page_number;
            pdfCmd[PDFTOPPM_COMMAND_POSITION_LASTPAGE] = "" + page_number;
            pdfCmd[PDFTOPPM_COMMAND_POSITION_DPI] = String.valueOf(dpi);
            pdfCmd[PDFTOPPM_COMMAND_POSITION_FILE] = input.toString();
            pdfCmd[PDFTOPPM_COMMAND_POSITION_OUTPUTFILE] = outPrefix;

            logger.debug("Running pdftoppm command: " + Arrays.deepToString(pdfCmd));
            File outf = null;
            try
            {
                Process pdfProc = Runtime.getRuntime().exec(pdfCmd);
                status = pdfProc.waitFor();
                logger.debug("status: " + status);
                
                // pdftoppm uses variable numbers of padding 0s to the output prefix.
                // E.g., may be prefix-000001.png, prefix-001.png or even prefix-01.png.
                // Version 0.12.3 (Poppler, not XPDF) seems to consider the total number of pages.
                // So, for example, in a PDF with 90 pages, the output will be "prefix-02.png";
                // for a PDF with 350 pages, the output will be "prefix-002.png".
                // FIXME: try some approach where the PDF number of pages is considered without
                // running pdfinfo command again, thus making it simpler to determine the number
                // of padding zeros. Right now we going "brute force" because we do not know if
                // it is feasable to once again run the pdfinfo command.
                String tests[] = {
                    outPrefix + "-" + page_number + ".png",
                    outPrefix + "-0" + page_number + ".png",
                    outPrefix + "-00" + page_number + ".png",
                    outPrefix + "-000" + page_number + ".png",
                    outPrefix + "-0000" + page_number + ".png",
                    outPrefix + "-00000" + page_number + ".png"
                    };
                for (String outname : tests)
                {
                    if ((new File(outname)).exists())
                    {
                        outf = new File(outname);
                        break;
                    }
                }
                logger.debug("PDFTOPPM output is: "+outf+", exists=" + outf.exists());
                processedImage = ImageIO.read(outf);
            }
            catch (InterruptedException e)
            {
                logger.error("Failed converting PDF file to image: ", e);
                throw new IllegalArgumentException("Failed converting PDF file to image: ", e);
            }
            finally
            {
                outf.delete();
            }
        }
        catch (IOException e)
        {
                logger.error("Failed converting PDF file to image: ", e);
                throw new IllegalArgumentException("Failed converting PDF file to image: ", e);
        }
        finally
        {
            if (status != 0)
                logger.error("PDF conversion proc failed, exit status="+status+", file="+input);
        }

        return processedImage;

        // Scale image and return in-memory stream
        /*
        BufferedImage toenail = scaleImage(source, maxwidth*3/4, maxwidth);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(toenail, "jpeg", baos);
        return new ByteArrayInputStream(baos.toByteArray());
        */
        
        //op.density(DEFAULT_DENSITY, DEFAULT_DENSITY);
        //op.colorspace(DEFAULT_COLORSPACE);
        //op.background("white");
        //setScaleParam1(op, params);

        //String crop = getCropParam(params);
        //if (crop != null) {
        //    op.addRawArgs("-crop " + crop);
        //}

        //if (params.getRotationDegree() > 0) {
        //    op.rotate(params.getRotationDegree() * 1.0);
        //}

        //setScaleParam2(op, params);
        //params.setScalingFactor(1.0);
        //params.setScalingDimensions(null);

        //op.addImage("png:-");
        //logger.debug("op: " + op.toString());
        //Stream2BufferedImage s2b = new Stream2BufferedImage();

        /*
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
        */
	}

/* From XPDFThumbnail.java (DSpace).
    // scale the image, preserving aspect ratio, if at least one
    // dimension is not between min and max.
    private static BufferedImage scaleImage(BufferedImage source,
                                            int min, int max)
    {
        int xsize = source.getWidth(null);
        int ysize = source.getHeight(null);
        int msize = Math.max(xsize, ysize);
        BufferedImage result = null;

        // scale the image if it's outside of requested range.
        // ALSO pass through if min and max are both 0
        if ((min == 0 && max == 0) ||
            (msize >= min && Math.min(xsize, ysize) <= max))
            return source;
        else
        {
            int xnew = xsize * max / msize;
            int ynew = ysize * max / msize;
            result = new BufferedImage(xnew, ynew, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = result.createGraphics();
            g2d.drawImage(source, 0, 0, xnew, ynew, null);
            return result;
        }
    }
*/

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

/*
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
*/
	
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

/*
    private static int getNumberOfPages(ImageRecord input) throws DjatokaException {
        logger.debug("Getting number of pages");
        
    }
*/

    /**
     * Get PDF information with pdfinfo:
     * - "Pages: X": number of pages;
     * - "Page X size: www.ww hhh.hh": size of each page, in pts.
     * @returns a map:
     * - [Pages][n]
     * - [Page 1][111.11 222.22]
     * - [Page i][www.ww hhh.hh]
     * - [Page n][999.99 1000.00]
     */
    private static Map<String, String> getPDFProperties(ImageRecord input) throws DjatokaException {
        logger.debug("Getting PDF info");

		try {
		    setPDFCommandsPath();
	    } catch (IllegalStateException e) {
            logger.error("Failed to set PDF commands path: ",e);
            throw e;
        }
        
        HashMap<String, String> pdfProperties = new HashMap<String, String>();
        
        String sourcePath = null;

        if (input.getImageFile() != null) {
            logger.debug("PDFInfo image file: " + input.getImageFile());
            sourcePath = input.getImageFile();
        } else if (input.getObject() != null && (input.getObject() instanceof InputStream)) {
            FileInputStream fis = null;
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
                in.deleteOnExit();

                FileOutputStream fos = new FileOutputStream(in);
                //in.deleteOnExit();
                IOUtils.copyStream(fis, fos);
            } catch (IOException e) {
                logger.error(e, e);
                throw new DjatokaException(e);
            }
            sourcePath = in.getAbsolutePath();
        } else {
            throw new DjatokaException(
                    "File not defined and Input Object Type "
                    + input //.getObject().getClass().getName()
                    + " is not supported");
        }
        
        String pdfinfoCmd[] = PDFINFO_COMMAND.clone();
        pdfinfoCmd[PDFINFO_COMMAND_POSITION_BIN] = pdfinfoPath;
        pdfinfoCmd[PDFINFO_COMMAND_POSITION_FIRSTPAGE] = "1";
        pdfinfoCmd[PDFINFO_COMMAND_POSITION_LASTPAGE] = "-1"; // Last page even we not knowing its number.
        pdfinfoCmd[PDFINFO_COMMAND_POSITION_FILE] = sourcePath;
        try
        {
            ArrayList<MatchResult> pageSizes = new ArrayList<MatchResult>();
            MatchResult pages = null;
            
            Process pdfProc = Runtime.getRuntime().exec(pdfinfoCmd);
            BufferedReader lr = new BufferedReader(new InputStreamReader(pdfProc.getInputStream()));
            String line;
            for (line = lr.readLine(); line != null; line = lr.readLine())
            {
                Matcher mm1 = PAGES_PATT.matcher(line);
                if (mm1.matches())
                    pages = mm1.toMatchResult();
                Matcher mm2 = MEDIABOX_PATT.matcher(line);
                if (mm2.matches())
                    pageSizes.add(mm2.toMatchResult());
            }

            int istatus = pdfProc.waitFor();
            if (istatus != 0)
                logger.error("pdfinfo proc failed, exit status=" + istatus + ", file=" + sourcePath);
                
            if (pages == null)
            {
                logger.error("Did not find 'Pages' line in output of pdfinfo command: " + Arrays.deepToString(pdfinfoCmd));
                pdfProperties.put("Pages", "0");
            }
            else
            {
                //int n = Integer.parseInteger(pages.group(1));
                pdfProperties.put("Pages", pages.group(1));
            }
            
            if (pageSizes.isEmpty())
            {
                logger.error("Did not find \"Page X size\" lines in output of pdfinfo command: " + Arrays.deepToString(pdfinfoCmd));
                throw new IllegalArgumentException("Failed to get pages size of PDF with pdfinfo.");
            }
            else
            {
                for (MatchResult mr : pageSizes)
                {
                    String page = mr.group(1);
                    
                    float x0 = Float.parseFloat(mr.group(2));
                    float y0 = Float.parseFloat(mr.group(3));
                    float x1 = Float.parseFloat(mr.group(4));
                    float y1 = Float.parseFloat(mr.group(5));
                    float w = Math.abs(x1 - x0);
                    float h = Math.abs(y1 - y0);
                    String width = "" + w; //mr.group(2);
                    String height = "" + h; //mr.group(3);
                    pdfProperties.put("Page " + page, width + " " + height);
                }
            }
            
        }
        catch (Exception e)
        {
            logger.error("Failed getting PDF information: ", e);
            throw new DjatokaException("Failed getting PDF information: ", e);
        }
        return pdfProperties;
    }


    /*
    private static Map<String, String> getPagesSizes(ImageRecord input) throws DjatokaException {
        logger.debug("Getting size of pages");
        Map<String, String> instProps;
    }
    */


    private static Dimension getPDFPageSize(String source, int page_number) throws DjatokaException {
        logger.debug("Getting PDF info");
        
        Dimension pageDimension = null;

		try {
		    setPDFCommandsPath();
	    } catch (IllegalStateException e) {
            logger.error("Failed to set PDF commands path: ",e);
            throw e;
        }
        
        String pdfinfoCmd[] = PDFINFO_COMMAND.clone();
        pdfinfoCmd[PDFINFO_COMMAND_POSITION_BIN] = pdfinfoPath;
        pdfinfoCmd[PDFINFO_COMMAND_POSITION_FIRSTPAGE] = "" + page_number;
        pdfinfoCmd[PDFINFO_COMMAND_POSITION_LASTPAGE] = "" + page_number; // Last page even we not knowing its number.
        pdfinfoCmd[PDFINFO_COMMAND_POSITION_FILE] = source;
        try
        {
            MatchResult pageSize = null;;
            
            Process pdfProc = Runtime.getRuntime().exec(pdfinfoCmd);
            BufferedReader lr = new BufferedReader(new InputStreamReader(pdfProc.getInputStream()));
            String line;
            for (line = lr.readLine(); line != null; line = lr.readLine())
            {
                Matcher mm = MEDIABOX_PATT.matcher(line);
                if (mm.matches())
                    pageSize = mm.toMatchResult();
            }

            int istatus = pdfProc.waitFor();
            if (istatus != 0)
                logger.error("pdfinfo proc failed, exit status=" + istatus + ", file=" + source);
                
            if (pageSize == null)
            {
                logger.error("Did not find 'Page " + page_number + " size' line in output of pdfinfo command: " + pdfinfoCmd);
                //throw new IllegalArgumentException("Failed to get pages size of PDF with pdfinfo.");
                pageDimension = new Dimension(0, 0);
            }
            else
            {
                String page = pageSize.group(1);
                double x0 = Double.parseDouble(pageSize.group(2));
                double y0 = Double.parseDouble(pageSize.group(3));
                double x1 = Double.parseDouble(pageSize.group(4));
                double y1 = Double.parseDouble(pageSize.group(5));
                double width = Math.abs(x1 - x0);
                double height = Math.abs(y1 - y0);
                //double width = Double.parseDouble(pageSize.group(2));
                //double height = Double.parseDouble(pageSize.group(3));
                pageDimension = new Dimension();
                pageDimension.setSize(width, height);
            }
            
        }
        catch (Exception e)
        {
            logger.error("Failed getting PDF page size: ", e);
            throw new DjatokaException("Failed getting PDF page size: ", e);
        }
        return pageDimension;
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

/*
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
*/

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
    
    private static void setPDFCommandsPath() throws IllegalStateException {
        // sanity check: poppler paths are required. can cache since it won't change
        if (pdftoppmPath == null || pdfinfoPath == null)
        {
            //props = IOUtils.loadConfigByCP(classConfig.getArg("props"));
            //pdftoppmPath = props.getProperty(PROPS_PDF_PDFTOPPM_PATH, DEFAULT_PDFTOPPM_PATH);
            //pdfinfoPath = props.getProperty(PROPS_PDF_PDFINFO_PATH, DEFAULT_PDFINFO_PATH);
            
            pdftoppmPath = ConfigurationManager.getProperty(PROPS_PDF_PDFTOPPM_PATH, DEFAULT_PDFTOPPM_PATH);
            pdfinfoPath = ConfigurationManager.getProperty(PROPS_PDF_PDFINFO_PATH, DEFAULT_PDFINFO_PATH);

            if (pdftoppmPath == null)
                throw new IllegalStateException("No value for key \"" + PROPS_PDF_PDFTOPPM_PATH + "\" in djatoka.properties! Should be path to pdftoppm executable.");
            if (pdfinfoPath == null)
                throw new IllegalStateException("No value for key \"" + PROPS_PDF_PDFINFO_PATH + "\" in djatoka.properties! Should be path to pdfinfo executable.");
        }
    }

}
