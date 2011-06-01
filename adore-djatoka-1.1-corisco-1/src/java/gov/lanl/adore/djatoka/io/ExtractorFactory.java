/*
 * Copyright (c) 2010 Brasiliana Digital Library, 2008 Los Alamos National Security, LLC.
 *
 * Brasiliana Digital Library
 * http://www.brasiliana.usp.br
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

package gov.lanl.adore.djatoka.io;

import eu.medsea.mimeutil.MimeException;
import eu.medsea.mimeutil.detector.OpendesktopMimeDetector;
import gov.lanl.adore.djatoka.DjatokaExtractProcessor;
import gov.lanl.adore.djatoka.IExtract;
import gov.lanl.adore.djatoka.kdu.KduExtractExe;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

/**
 * Extractor Factory. Uses format writer/reader implementations.
 * @author Fabio Kepler
 *
 */
public class ExtractorFactory implements FormatConstants {
    static Logger logger = Logger.getLogger(ExtractorFactory.class);


    // Mimetypes for supported extractor formats
    /** JP2 Mimetype Constant - "image/jp2" */
    // public static final String FORMAT_MIMEYPE_JP2 = "image/jp2";
    /** JPEG Mimetype Constant - "image/jpeg" */
    // public static final String FORMAT_MIMEYPE_JPEG = "image/jpeg";
    /** PDF Mimetype Constant - "image/jpeg" */
    public static final String FORMAT_MIMEYPE_PDF = "application/pdf";
    // default implementations for defined formats
    public static final String DEFAULT_EXTRACTOR = "gov.lanl.adore.djatoka.kdu.KduExtractExe";
    /** Default JP2 Extractor */
    public static final String DEFAULT_JP2_EXTRACTOR = "gov.lanl.adore.djatoka.kdu.KduExtractExe";
    /** Default JPEG Extractor */
    public static final String DEFAULT_JPEG_EXTRACTOR = "gov.lanl.adore.djatoka.plugin.ExtractJPG";
    /** Default PDF Extractor */
    public static final String DEFAULT_PDF_EXTRACTOR = "gov.lanl.adore.djatoka.plugin.ExtractPDF";

    private static HashMap<String, Class> extractorsImpl = new HashMap<String, Class>();
    private static HashMap<String, IExtract> extractors = new HashMap<String, IExtract>();
    private static HashMap<String, DjatokaExtractProcessor> djatokaExtractors = new HashMap<String, DjatokaExtractProcessor>();

    /** MIME util */
    private static OpendesktopMimeDetector opendesktopMimeDetector = new OpendesktopMimeDetector();
    private static final int MAX_CONCURRENT_DETECTIONS = 1;
    private static final Semaphore detectorRateLimit = new Semaphore(MAX_CONCURRENT_DETECTIONS, true); // true: fair => first-in, first-out


    /**
     * Default Constructor, uses default format map.
     */
    public ExtractorFactory() {
        this(getDefaultFormatMap());
    }
    
    
    /**
     * Create a new ExtractorFactory using provided format map. Format maps
     * must be key/value pair of syntax $mimetype=$impl
     * (e.g. image/jpeg=gov.lanl.adore.djatoka.kdu.KduExtractExe)
     * @param formatMap
     */
    public ExtractorFactory(Properties formatMap) {
        for (Map.Entry<Object, Object> i : formatMap.entrySet()) {
            String k = (String) i.getKey();
            String v = (String) i.getValue();
            try {
                Class<?> impl = Class.forName(v);
                if (k != null && impl != null)
                    extractorsImpl.put(k, impl);
            } catch (ClassNotFoundException e) {
                System.err.println("Class not found for format " + k + ": " + v);
                logger.error(e);
            }
        }
    }

    
    /**
     * Create a new ExtractorFactory using provided format map. Format maps
     * must be key/value pair of syntax $mimetype=$impl
     * (e.g. image/jpeg=gov.lanl.adore.djatoka.kdu.KduExtractExe)
     * @return Properties object containing extractor implementation class key/value pairs
     */
    public static Properties getDefaultFormatMap() {
        Properties formatMap = new Properties();
        formatMap.put(FORMAT_MIMEYPE_JP2, DEFAULT_JP2_EXTRACTOR);
        formatMap.put(FORMAT_MIMEYPE_JPEG, DEFAULT_JPEG_EXTRACTOR);
        formatMap.put(FORMAT_MIMEYPE_PDF, DEFAULT_PDF_EXTRACTOR);
        formatMap.put(DEFAULT_EXTRACTOR, DEFAULT_JP2_EXTRACTOR);
        return formatMap;
    }


    /**
     * Returns format extractor implementation for provided format identifier
     * @param format identifier of requested identifier
     * @return format extractor for provided format identifier
     */
    public IExtract getExtractorInstanceForFile(String file) {
        try {
            String format = getMimetypeForFile(file);
            return getExtractorInstanceForFormat(format);
        } catch (IOException ex) {
            logger.log(Priority.FATAL, null, ex);
        }
        return null;
    }

    
    /**
     * Returns format writer implementation for provided format identifier
     * @param format identifier of requested identifier
     * @return format writer for provided format identifier
     */
    public DjatokaExtractProcessor getDjatokaExtractorProcessorForFile(String file) {
        try {
            String format = getMimetypeForFile(file);
            return getDjatokaExtractorProcessorForFormat(format);
        } catch (IOException ex) {
            logger.log(Priority.FATAL, null, ex);
        }
        return null;
    }


    /**
     * Get mimetype for 'file' based on its content.
     * @param file Doesn't need to have an extension.
     * @return Most probable mimetype.
     * @throws FileNotFoundException
     * @throws MimeException
     */
    public static String getMimetypeForFile(String file) throws FileNotFoundException, MimeException {
        if (MAX_CONCURRENT_DETECTIONS > 0) {
            try {
                if (!detectorRateLimit.tryAcquire(0, TimeUnit.SECONDS)) {
                    logger.debug("Waiting for semaphore");
                    detectorRateLimit.acquire();
                    logger.debug("Acquired semaphore");
                }
            } catch (InterruptedException e) {
                // Shouldn't happen?
                logger.error("MimeType detection interrupted waiting for semaphore", e);
            }
        }

        BufferedInputStream bis = null;
        Collection<String> coll = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            coll = opendesktopMimeDetector.getMimeTypesInputStream(bis);
            logger.debug("coll size: " + (coll == null ? "null" : coll.size()) + "; coll: " + coll.toString());
            return (String) (coll.size() > 0 ? coll.toArray()[0] : "");
        } catch (IllegalArgumentException ex) { // Trying to circumvent a bug in mime-util (see http://sourceforge.net/tracker/?func=detail&aid=3007610&group_id=205064&atid=992132#).
            int max_tries = 2;
            int next_try = 1;
            while (coll == null && next_try <= max_tries) {
                logger.error("Exception in MimeDetector; retrying " + next_try + " of " + max_tries + " try(ies)", ex);
                coll = opendesktopMimeDetector.getMimeTypesInputStream(bis);
            }
            if (coll == null) return "";
            else return (String) (coll.size() > 0 ? coll.toArray()[0] : "");
        } catch (Exception ex) {
            logger.error("Exception in MimeDetector", ex);
            return "";
        } finally {
            if (MAX_CONCURRENT_DETECTIONS > 0) detectorRateLimit.release();
            try {
                if (bis != null) bis.close();
            } catch (IOException ex) {
                logger.error("Closing file stream", ex);
            }
        }
    }

    
    public IExtract getExtractorInstanceForFormat(String format) {
        try {
            if (extractors.containsKey(format)) {
                return extractors.get(format);
            } else if (extractorsImpl.containsKey(format)) {
                extractors.put(format, (IExtract) extractorsImpl.get(format).newInstance());
                return extractors.get(format);
            } else {
                if (extractors.containsKey(DEFAULT_EXTRACTOR)) {
                    return extractors.get(DEFAULT_EXTRACTOR);
                } else if (extractorsImpl.containsKey(DEFAULT_EXTRACTOR)) {
                    extractors.put(DEFAULT_EXTRACTOR, (IExtract) extractorsImpl.get(DEFAULT_EXTRACTOR).newInstance());
                    return extractors.get(DEFAULT_EXTRACTOR);
                }
            }
        } catch (InstantiationException ex) {
            logger.log(Priority.FATAL, null, ex);
        } catch (IllegalAccessException ex) {
            logger.log(Priority.FATAL, null, ex);
        }
        extractors.put(DEFAULT_EXTRACTOR, (IExtract) new KduExtractExe());
        return extractors.get(DEFAULT_EXTRACTOR);
    }

    
    public DjatokaExtractProcessor getDjatokaExtractorProcessorForFormat(String format) {
        if (djatokaExtractors.containsKey(format)) {
            return djatokaExtractors.get(format);
        } else {
            djatokaExtractors.put(format, new DjatokaExtractProcessor(getExtractorInstanceForFormat(format)));
            return djatokaExtractors.get(format);
        }
    }
}

