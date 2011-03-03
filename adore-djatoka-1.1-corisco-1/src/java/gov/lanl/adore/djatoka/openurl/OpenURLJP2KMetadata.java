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

package gov.lanl.adore.djatoka.openurl;

import gov.lanl.adore.djatoka.DjatokaException;
import gov.lanl.adore.djatoka.IExtract;
import gov.lanl.adore.djatoka.io.ExtractorFactory;
import gov.lanl.adore.djatoka.io.FormatConstants;
import gov.lanl.adore.djatoka.util.IOUtils;
import gov.lanl.adore.djatoka.util.ImageRecord;
import gov.lanl.util.HttpDate;
import info.openurl.oom.ContextObject;
import info.openurl.oom.OpenURLRequest;
import info.openurl.oom.OpenURLRequestProcessor;
import info.openurl.oom.OpenURLResponse;
import info.openurl.oom.Service;
import info.openurl.oom.config.ClassConfig;
import info.openurl.oom.config.OpenURLConfig;
import info.openurl.oom.entities.ServiceType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * The OpenURLJP2KMetadata OpenURL Service
 * 
 * @author Ryan Chute
 */
public class OpenURLJP2KMetadata implements Service, FormatConstants {
    static Logger logger = Logger.getLogger(OpenURLJP2KMetadata.class);
    private static final String DEFAULT_IMPL_CLASS = SimpleListResolver.class.getCanonicalName();
    private static final String PROPS_KEY_IMPL_CLASS = "OpenURLJP2KService.referentResolverImpl";
    private static final String SVC_ID = "info:lanl-repo/svc/getMetadata";
    private static final String RESPONSE_TYPE = "application/json";

    private static String implClass = null;
    private static Properties props = new Properties();

    private static ExtractorFactory extractorFactory = new ExtractorFactory();

    /**
     * Construct an info:lanl-repo/svc/getMetadata web service class. Initializes 
     * Referent Resolver instance using OpenURLJP2KService.referentResolverImpl property.
     * 
     * @param openURLConfig OOM Properties forwarded from OpenURLServlet
     * @param classConfig Implementation Properties forwarded from OpenURLServlet
     * @throws ResolverException 
     */
    public OpenURLJP2KMetadata(OpenURLConfig openURLConfig, ClassConfig classConfig) throws ResolverException {
        try {
            if (!ReferentManager.isInit()) {
                props = IOUtils.loadConfigByCP(classConfig.getArg("props"));
                implClass = props.getProperty(PROPS_KEY_IMPL_CLASS,DEFAULT_IMPL_CLASS);
                ReferentManager.init((IReferentResolver) Class.forName(implClass).newInstance(), props);
            }
        } catch (IOException e) {
            throw new ResolverException("Error attempting to open props file from classpath, disabling " + SVC_ID + " : " + e.getMessage());
        } catch (Exception e) {
            throw new ResolverException("Unable to inititalize implementation: " + props.getProperty(implClass) + " - " + e.getMessage());
        }
    }

    /**
     * Returns the OpenURL service identifier for this implementation of
     * info.openurl.oom.Service
     */
    public URI getServiceID() throws URISyntaxException {
        return new URI(SVC_ID);
    }

    /**
     * Returns the OpenURLResponse of a JSON object defining the core image
     * properties. Having obtained a result, this method is then responsible for
     * transforming it into an OpenURLResponse that acts as a proxy for
     * HttpServletResponse.
     */
    public OpenURLResponse resolve(ServiceType serviceType,
            ContextObject contextObject, OpenURLRequest openURLRequest,
            OpenURLRequestProcessor processor) {

        String responseFormat = RESPONSE_TYPE;
        int status = HttpServletResponse.SC_OK;
        ByteArrayOutputStream baos =  new ByteArrayOutputStream();
        try {
            baos = new ByteArrayOutputStream();
            ImageRecord r = ReferentManager.getImageRecord(contextObject.getReferent());
            IExtract jp2 = extractorFactory.getExtractorInstanceForFile(r.getImageFile());
            r = jp2.getMetadata(r);
            StringBuffer sb = new StringBuffer();
            sb.append("{");
            sb.append("\n\"identifier\": \"" + r.getIdentifier() + "\",");
            sb.append("\n\"imagefile\": \"" + r.getImageFile() + "\",");
            sb.append("\n\"width\": \"" + r.getWidth() + "\",");
            sb.append("\n\"height\": \"" + r.getHeight() + "\",");
            sb.append("\n\"dwtLevels\": \"" + r.getDWTLevels() + "\",");
            sb.append("\n\"levels\": \"" + r.getLevels() + "\",");
            sb.append("\n\"compositingLayerCount\": \"" + r.getCompositingLayerCount() + "\",");
            if (r.getInstProps() != null) {
                //HashMap<String, String> props = r.getInstProps();
                sb.append("\n\"instProps\": {");
                String separator = "";
                for (Map.Entry<String, String> entry : r.getInstProps().entrySet()) {
                    sb.append(separator + "\n\t\"" + entry.getKey() + "\": \"" + entry.getValue() + "\"");
                    separator = ",";
                }
                sb.append("\n\t}");
            }
            sb.append("\n}");
            baos.write(sb.toString().getBytes());
        } catch (DjatokaException e) {
            responseFormat = "text/plain";
            status = HttpServletResponse.SC_NOT_FOUND;
        } catch (Exception e) {
            baos = new ByteArrayOutputStream();
            try {
                if (e.getMessage() != null)
                    baos.write(e.getMessage().getBytes("UTF-8"));
                else {
                    logger.error(e,e);
                    baos.write("Internal Server Error: ".getBytes());
                }
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
            responseFormat = "text/plain";
            status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        } 
        HashMap<String, String> header_map = new HashMap<String, String>();
        header_map.put("Content-Length", baos.size() + "");
        header_map.put("Date", HttpDate.getHttpDate());
        return new OpenURLResponse(status, responseFormat, baos.toByteArray(), header_map);
    }
}
