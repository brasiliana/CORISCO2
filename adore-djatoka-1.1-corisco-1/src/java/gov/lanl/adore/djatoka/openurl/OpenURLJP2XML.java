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

package gov.lanl.adore.djatoka.openurl;

import gov.lanl.adore.djatoka.IExtract;
import gov.lanl.adore.djatoka.io.FormatConstants;
import gov.lanl.adore.djatoka.kdu.KduExtractExe;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * The OpenURLJP2KMetadata OpenURL Service
 * 
 * @author Ryan Chute
 */
public class OpenURLJP2XML implements Service, FormatConstants {
	static Logger logger = Logger.getLogger(OpenURLJP2XML.class);
    private static final String DEFAULT_IMPL_CLASS = SimpleListResolver.class.getCanonicalName();
    private static final String PROPS_KEY_IMPL_CLASS = "OpenURLJP2KService.referentResolverImpl";
	private static final String SVC_ID = "info:lanl-repo/svc/getJP2XML";

	private static String implClass = null;
	private static Properties props = new Properties();

	/**
	 * Construct an info:lanl-repo/svc/getXML web service class. Initializes 
	 * Referent Resolver instance using OpenURLJP2KService.referentResolverImpl property.
	 * 
	 * @param openURLConfig OOM Properties forwarded from OpenURLServlet
	 * @param classConfig Implementation Properties forwarded from OpenURLServlet
	 * @throws ResolverException 
	 */
	public OpenURLJP2XML(OpenURLConfig openURLConfig, ClassConfig classConfig) throws ResolverException {
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
	 * Returns the OpenURLResponse of an XML object
	 */
	public OpenURLResponse resolve(ServiceType serviceType,
			ContextObject contextObject, OpenURLRequest openURLRequest,
			OpenURLRequestProcessor processor) {

		String responseFormat = "application/xml";;
		int status = HttpServletResponse.SC_OK;
		ByteArrayOutputStream baos =  new ByteArrayOutputStream();
	    try {
			baos = new ByteArrayOutputStream();
			IExtract jp2 = new KduExtractExe();
			ImageRecord r = ReferentManager.getImageRecord(contextObject.getReferent());
			String[] xml = jp2.getXMLBox(r);
			StringBuffer sb = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            sb.append("<jp2:JP2XML xmlns:jp2=\"http://library.lanl.gov/2008-11/aDORe/JP2XML/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  xsi:schemaLocation=\"http://library.lanl.gov/2008-11/aDORe/JP2XML/ http://purl.lanl.gov/aDORe/schemas/2008-11/JP2XML.xsd\"");            
            sb.append(" boxCount=\"" + ((xml != null) ? xml.length : 0) + "\">");
			if (xml != null) {
				for (String x : xml) {
					sb.append("<jp2:XMLBox>");
					if (x.contains("<?xml"))
						sb.append(x.substring(x.indexOf(">") + 1));
					else
						sb.append(x);
					sb.append("</jp2:XMLBox>");
				}
			}
			sb.append("</jp2:JP2XML>");
			baos.write(sb.toString().getBytes());
		} catch (Exception e) {
			logger.error(e,e);
			responseFormat = "text/plain";
			status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		} 
		HashMap<String, String> header_map = new HashMap<String, String>();
		header_map.put("Content-Length", baos.size() + "");
		header_map.put("Date", HttpDate.getHttpDate());
		return new OpenURLResponse(status, responseFormat, baos.toByteArray(), header_map);
	}
}
