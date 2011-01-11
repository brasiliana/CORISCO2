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

package gov.lanl.adore.djatoka.openurl.plugin.rftdb;

import gov.lanl.adore.djatoka.openurl.DjatokaImageMigrator;
import gov.lanl.adore.djatoka.openurl.IReferentMigrator;
import gov.lanl.adore.djatoka.openurl.IReferentResolver;
import gov.lanl.adore.djatoka.openurl.ResolverException;
import gov.lanl.adore.djatoka.util.ImageRecord;
import gov.lanl.util.DBCPUtils;

import info.openurl.oom.entities.Referent;

import java.io.File;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

/**
 * Alternate IReferentResolver implementation using JDBC & DBCP.  Define ImageRecordManager.query
 * to map the unique identifier and file path to 'identifier' and 'imageFile'
 * 
 * ----- OpenURLJP2KService.properties -----
 * OpenURLJP2KService.referentResolverImpl=gov.lanl.adore.djatoka.openurl.plugin.rftdb.DatabaseResolver
 * DatabaseResolver.url=jdbc:mysql://localhost/djatoka
 * DatabaseResolver.driver=com.mysql.jdbc.Driver
 * DatabaseResolver.login=root
 * DatabaseResolver.pwd=
 * DatabaseResolver.query=SELECT identifier, imageFile FROM resources WHERE identifier='\\i';
 * 
 *  ----- DEFAULT SCHEMA -----
 * CREATE TABLE `resources` (
 *  `identifier` varchar(100) NOT NULL,
 *  `imageFile` varchar(255) NOT NULL,
 *  PRIMARY KEY  (`identifier`)
 * ) ENGINE=MyISAM DEFAULT CHARSET=latin1;
 * 
 * 
 * @author Ryan Chute
 *
 */
public class DatabaseResolver implements IReferentResolver {
	static Logger log = Logger.getLogger(DatabaseResolver.class.getName());
	public static final String DEFAULT_DBID = "DatabaseResolver";
	public static final String FIELD_IDENTIFIER = "identifier";
	public static final String FIELD_IMAGEFILE = "imageFile";
	public static final String REPLACE_ID_KEY = "\\i";
	private static IReferentMigrator dim = new DjatokaImageMigrator();
	private static DataSource dataSource;
	private static Map<String, ImageRecord> remoteImgs;
	private static Map<String, ImageRecord> localImgs;
	private static final String PROP_REMOTE_CACHE = "DatabaseResolver.maxRemoteCacheSize";
	private static final int DEFAULT_REMOTE_CACHE_SIZE = 100;
	private static int maxRemoteCacheSize = DEFAULT_REMOTE_CACHE_SIZE;
	private static String query = "SELECT identifier, imageFile FROM resources WHERE identifier='\\i';";
	
	/**
	 * Referent Identifier to be resolved from Identifier Resolver. The returned
	 * ImageRecord need only contain the imageId and image file path.
	 * @param rft identifier of the image to be resolved
	 * @return ImageRecord instance containing resolvable metadata
	 * @throws ResolverException
	 */
	public ImageRecord getImageRecord(Referent rft) throws ResolverException {
        String id = ((URI)  rft.getDescriptors()[0]).toASCIIString();
        return getImageRecord(id);
	}

	/**
	 * Referent Identifier to be resolved from Identifier Resolver. The returned
	 * ImageRecord need only contain the imageId and image file path.
	 * @param rftId identifier of the image to be resolved
	 * @return ImageRecord instance containing resolvable metadata
	 * @throws ResolverException
	 */
	public ImageRecord getImageRecord(String rftId) throws ResolverException {
		ImageRecord ir = null;
		if (isResolvableURI(rftId)) {
			ir = getRemoteImage(rftId);
		} else {
			ir = getLocalImage(rftId);
		}
        return ir;
	}

	public IReferentMigrator getReferentMigrator() {
		return dim;
	}

	public int getStatus(String rftId) {
		if (remoteImgs.get(rftId) != null || getLocalImage(rftId) != null)
			return HttpServletResponse.SC_OK;
		else if (dim.getProcessingList().contains(rftId))
			return HttpServletResponse.SC_ACCEPTED;
		else
			return HttpServletResponse.SC_NOT_FOUND;
	}

	/**
	 * Sets a Properties object that may be used by underlying implementation
	 * @param props Properties object for use by implementation
	 * @throws ResolverException
	 */
	public void setProperties(Properties props) throws ResolverException {
		localImgs = Collections.synchronizedMap(new LinkedHashMap<String, ImageRecord>(16, 0.75f, true));
		// Initialize remote image cache management
		String mrcs = props.getProperty(PROP_REMOTE_CACHE);
		if (mrcs != null)
		    maxRemoteCacheSize = Integer.parseInt(mrcs);
		remoteImgs = Collections.synchronizedMap(new LinkedHashMap<String, ImageRecord>(16, 0.75f, true){		
			private static final long serialVersionUID = 1;

			protected boolean removeEldestEntry(Map.Entry<String, ImageRecord> eldest) {
				log.debug("remoteCacheSize: " + size());
				boolean d = size() > maxRemoteCacheSize;
				if (d) {
					File f = new File((String) eldest.getValue().getImageFile());
					log.debug("deleting: " + eldest.getValue().getImageFile());
					if (f.exists())
						f.delete();
					remove(eldest.getKey());
				}
				return false;
			};
		}); 

		query = props.getProperty(DEFAULT_DBID + ".query");
		if (query == null)
			throw new ResolverException(DEFAULT_DBID + ".query is not defined in properties");
		try {
		    dataSource = DBCPUtils.setupDataSource(DEFAULT_DBID, props);
		} catch (Throwable e) {
			log.error(e,e);
			throw new ResolverException("DBCP Libraries are not in the classpath");
		}
	}

	private static boolean isResolvableURI(String rftId) {
		return (rftId.startsWith("http") || rftId.startsWith("file") || rftId.startsWith("ftp"));
	}
	
	private ImageRecord getLocalImage(String rftId) {
		ImageRecord ir = localImgs.get(rftId);
		if (ir == null) {
			Connection conn = null;
			Statement stmt = null;
			ResultSet rset = null;
			try {
				conn = dataSource.getConnection();
				stmt = conn.createStatement();
				rset = stmt.executeQuery(query.replace("\\i", rftId));
				if (rset.next()) {
					ir = new ImageRecord();
					ir.setIdentifier(rset.getString(FIELD_IDENTIFIER));
					ir.setImageFile(rset.getString(FIELD_IMAGEFILE));
				}
			} catch (SQLException e) {
				log.error(e, e);
			} finally {
				try {
					rset.close();
				} catch (Exception e) {
				}
				try {
					stmt.close();
				} catch (Exception e) {
				}
				try {
					conn.close();
				} catch (Exception e) {
				}
			}
			if (ir != null)
				localImgs.put(rftId, ir);
		}
		return ir;
	}
	
	private ImageRecord getRemoteImage(String rftId) throws ResolverException {
		ImageRecord ir = remoteImgs.get(rftId);
		if (ir == null || !new File(ir.getImageFile()).exists()) {
			try {
				URI uri = new URI(rftId);
				if (dim.getProcessingList().contains(uri.toString())) {
					int i = 0;
					Thread.sleep(1000);
					while (dim.getProcessingList().contains(uri)
							&& i < (5 * 60)) {
						Thread.sleep(1000);
						i++;
					}
					if (remoteImgs.containsKey(rftId))
						return remoteImgs.get(rftId);
				}
				File f = dim.convert(uri);
				ir = new ImageRecord(rftId, f.getAbsolutePath());
				if (f.length() > 0)
					remoteImgs.put(rftId, ir);
				else
					throw new ResolverException(
							"An error occurred processing file:"
									+ uri.toURL().toString());
		    } catch (Exception e) {
				log.error("Unable to access " + rftId);
		    	return null;
			}
		}
		return ir;
	}
}
