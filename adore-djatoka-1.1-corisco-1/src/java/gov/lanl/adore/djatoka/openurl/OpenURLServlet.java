/**
 * Copyright 2006 OCLC Online Computer Library Center Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or
 * agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.lanl.adore.djatoka.openurl;

import info.openurl.oom.OpenURLRequest;
import info.openurl.oom.OpenURLRequestProcessor;
import info.openurl.oom.OpenURLResponse;
import info.openurl.oom.Transport;
import info.openurl.oom.config.OpenURLConfig;
import gov.lanl.util.AccessManager;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

/**
 * OpenURL Servlet - Added referrer and requester to Context Object
 * 
 * @author Jeffrey A. Young
 * @author Ryan Chute
 */
public class OpenURLServlet extends HttpServlet {
	static Logger logger = Logger.getLogger(OpenURLServlet.class);
    /**
     * Initial version
     */
    private static final long serialVersionUID = 1L;
    private OpenURLConfig openURLConfig;
    private OpenURLRequestProcessor processor;
    private Transport[] transports;
    private AccessManager am;
    
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        try {
            // load the configuration file from the classpath
        	openURLConfig = new org.oclc.oomRef.config.OpenURLConfig(config);
            
            // Construct the configured transports
            transports = openURLConfig.getTransports();
            
            // Construct a processor
            processor = openURLConfig.getProcessor();
            
            ClassLoader cl = OpenURLServlet.class.getClassLoader();
            java.net.URL url = cl.getResource("access.txt");
            if (url != null)
            	am = new AccessManager(url.getFile());
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e.getMessage(), e);
        }
    }

    /**
	 * Extends HttpServlet Request to build OpenURL Request and Context Objects.
	 * The req.getHeader("referer") is used to add an OpenURL ReferringEntities
	 * and req.getRemoteAddr() is used to add a Requester.
	 */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException {
        try {
            // Try each Transport until someone takes responsibility
            OpenURLRequest openURLRequest = null;
            for (int i=0; openURLRequest == null && i<transports.length; ++i) {
                openURLRequest = transports[i].toOpenURLRequest(processor, req);
            }
            
            if (openURLRequest == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid Request");
                return;
            }
            // 2009-05-06: rchute Add AccessManager Support
            if (am != null) {
            	try {
            		String url = ((java.net.URI)  openURLRequest.getContextObjects()[0].getReferent().getDescriptors()[0]).toASCIIString();
            		if (url.startsWith("http") || url.startsWith("ftp")) {
            			if (!am.checkAccess(new URL(url).getHost())){
            				int status = HttpServletResponse.SC_FORBIDDEN;
            				resp.sendError(status);
            				return;
            			}
            		}
            	} catch (Exception e) {
            		logger.error(e);
            	}
             
            }
            
            // rchute: Add referrer for possible extension processing
            if (req.getHeader("referer") != null)
                openURLRequest.getContextObjects()[0].getReferringEntities()[0].addDescriptor(req.getHeader("referer"));
            // rchute: Add requester for possible extension processing
            openURLRequest.getContextObjects()[0].getRequesters()[0].addDescriptor(req.getRemoteAddr());
            // Process the ContextObjects
            OpenURLResponse result = processor.resolve(openURLRequest);
            
            // See if anyone handled the request
            int status;
            if (result == null) {
                status = HttpServletResponse.SC_NOT_FOUND;
            } else {
                status = result.getStatus();
                Cookie[] cookies = result.getCookies();
                if (cookies != null) {
                    for (int i=0; i< cookies.length; ++i) {
                        resp.addCookie(cookies[i]);
                    }
                }
                
                Map sessionMap = result.getSessionMap();
                if (sessionMap != null) {
                    HttpSession session = req.getSession(true);
                    Iterator iter = sessionMap.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry entry = (Entry) iter.next();
                        session.setAttribute((String) entry.getKey(), entry.getValue());
                    }
                }
                
                Map headerMap = result.getHeaderMap();
                if (headerMap != null) {
                    Iterator iter = headerMap.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry entry = (Entry) iter.next();
                        resp.setHeader((String) entry.getKey(),
                                (String) entry.getValue());
                    }
                }
            }
            
            // Allow the processor to generate a variety of response types
            switch (status) {
            case HttpServletResponse.SC_MOVED_TEMPORARILY:
                resp.sendRedirect(
                		resp.encodeRedirectURL(
                				result.getRedirectURL()));
                break;
            case HttpServletResponse.SC_SEE_OTHER:
            case HttpServletResponse.SC_MOVED_PERMANENTLY:
                resp.setStatus(status);
                resp.setHeader("Location", result.getRedirectURL());
                break;
            case HttpServletResponse.SC_NOT_FOUND:
                resp.sendError(status);
                break;
            default:
                OutputStream out = resp.getOutputStream();
                resp.setStatus(status);
                resp.setContentType(result.getContentType());
                InputStream is = result.getInputStream();
                byte[] bytes = new byte[1024];
                int len;
                while ((len = is.read(bytes)) != -1) {
                    out.write(bytes, 0, len);
                }
                out.close();
                break;
            }
        } catch (SocketException e) {
        	logger.error(e);
        } catch (Throwable e) {
        	logger.debug(e);
        	//throw new ServletException(e.getMessage(), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException {
        doGet(req, resp);
    }
}
