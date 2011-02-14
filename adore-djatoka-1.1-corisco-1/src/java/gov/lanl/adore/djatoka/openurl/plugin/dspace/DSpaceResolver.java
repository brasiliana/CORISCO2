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

package gov.lanl.adore.djatoka.openurl.plugin.dspace;

import gov.lanl.adore.djatoka.openurl.DjatokaImageMigrator;
import gov.lanl.adore.djatoka.openurl.IReferentMigrator;
import gov.lanl.adore.djatoka.openurl.IReferentResolver;
import gov.lanl.adore.djatoka.openurl.ResolverException;
import gov.lanl.adore.djatoka.util.ImageRecord;
import info.openurl.oom.entities.Referent;
import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

/**
 * Alternate IReferentResolver implementation using DSpace bitstream internal ID.
 * 
 * ----- OpenURLJP2KService.properties -----
 * OpenURLJP2KService.referentResolverImpl=gov.lanl.adore.djatoka.openurl.plugin.DSpaceResolver
 * DSpaceResolver.assetpath=dspace.assetpath
 * 
 * @author Fabio N. Kepler
 */
public class DSpaceResolver implements IReferentResolver {
    static Logger log = Logger.getLogger(DSpaceResolver.class.getName());

    private static final String DSPACE_ASSETPATH = "DSpaceResolver.assetpath";
    private static IReferentMigrator dim = new DjatokaImageMigrator();
    private static String dspace_assetpath;
    private static Map<String, ImageRecord> imgs;

    /**
     * Referent Identifier to be resolved from Identifier Resolver. The returned
     * ImageRecord need only contain the imageId and image file path.
     * 
     * @param rft
     *            identifier of the image to be resolved
     * @return ImageRecord instance containing resolvable metadata
     * @throws ResolverException
     */
    @Override
    public ImageRecord getImageRecord(Referent rft) throws ResolverException {
        String id = ((URI) rft.getDescriptors()[0]).toASCIIString();
        return getImageRecord(id);
    }
    
    /**
     * Referent Identifier to be resolved from Identifier Resolver. The returned
     * ImageRecord need only contain the imageId and image file path.
     * 
     * @param rftId
     *            identifier of the image to be resolved
     * @return ImageRecord instance containing resolvable metadata
     * @throws ResolverException
     */
    @Override
    public ImageRecord getImageRecord(String rftId) throws ResolverException {
        try {
            ImageRecord ir = null;
            if (isResolvableURI(rftId)) {
                ir = getRemoteImage(rftId);
            } else {
                ir = getLocalImage(rftId);
            }
            return ir;
        } catch (Exception e) {
            log.error(e, e);
            throw new ResolverException(e);
        }
    }

    @Override
    public IReferentMigrator getReferentMigrator() {
        return dim;
    }
    
    @Override
    public int getStatus(String rftId) {
        if (imgs.get(rftId) != null)
            return HttpServletResponse.SC_OK;
        else if (dim.getProcessingList().contains(rftId))
            return HttpServletResponse.SC_ACCEPTED;
        else
            return HttpServletResponse.SC_NOT_FOUND;
    }

    /**
     * Sets a Properties object that may be used by underlying implementation
     * 
     * @param props
     *            Properties object for use by implementation
     * @throws ResolverException
     */
    @Override
    public void setProperties(Properties props) throws ResolverException {
        String dap = props.getProperty(DSPACE_ASSETPATH);
        if (dap != null) {
            dspace_assetpath = dap;
            imgs = Collections.synchronizedMap(new LinkedHashMap<String, ImageRecord>(16, 0.75f, true));
        } else {
            throw new ResolverException(DSPACE_ASSETPATH + " is not defined.");
        }
    }
    
    private static boolean isResolvableURI(String rftId) {
        return (rftId.startsWith("http") || rftId.startsWith("file") || rftId.startsWith("ftp"));
    }
    
    /**
     * Logic for translating the DSpace bitstream's internal identifier to its filesystem path.
     * Must be the same logic used inside DSpace!
     */
    private ImageRecord getLocalImage(String rftId) throws ResolverException {
        String internalId = rftId;
        StringBuilder bitstreamPathBuilder = new StringBuilder();
        
        // Get the asset store root path.
        bitstreamPathBuilder.append(dspace_assetpath);
        // If the specified path does not end with the path separator character, add one.
        if (!dspace_assetpath.endsWith(File.separator)) {
            bitstreamPathBuilder.append(File.separator);
        }
        
        bitstreamPathBuilder.append(internalId.substring(0, 2))
                            .append(File.separator)
                            .append(internalId.substring(2, 4))
                            .append(File.separator)
                            .append(internalId.substring(4, 6))
                            .append(File.separator)
                            .append(internalId).toString();
        String bitstreamPath = bitstreamPathBuilder.toString();
        
        File bit = new File(bitstreamPath);
        if (!bit.exists())
            throw new ResolverException("An error occurred processing file: " + bitstreamPath);
        
        ImageRecord ir = new ImageRecord(rftId, bitstreamPath);
        imgs.put(rftId, ir);
        return ir;
    }
    
    private ImageRecord getRemoteImage(String rftId) throws ResolverException {
        throw new ResolverException("No support for remote images processing: " + rftId);
    }
}
