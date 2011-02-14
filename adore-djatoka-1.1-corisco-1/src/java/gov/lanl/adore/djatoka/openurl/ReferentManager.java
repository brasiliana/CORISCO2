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

import gov.lanl.adore.djatoka.util.ImageRecord;
import info.openurl.oom.entities.Referent;

import java.util.Properties;

/**
 * Allows access to the single ReferentResolver instance across 
 * multiple service implementations.
 * 
 * @author Ryan Chute
 *
 */
public class ReferentManager {
	private static boolean init = false;
	private static IReferentResolver rftResolver;
	private ReferentManager(){};
	
	/**
	 * Gets the underlying IReferentResolver impl.
	 * @return underlying IReferentResolver impl.
	 */
	public static IReferentResolver getResolver() {
		return rftResolver;
	}

	/**
	 * Indicates whether the manager has been initialized and a IReferentResolver
	 * is properly configured
	 * @return true if initialized and ready for use
	 */
	public static boolean isInit() {
		return init;
	}

    /**
     * Gets ImageRecord for the initialized IReferentResolver impl..  This method may
     * be used when metadata is provided about the resource and the underlying resource
     * resolver needs to resolve the metadata to an identifier and resource location.
     * @param rft OpenURL OOM Referent object for the requested ImageRecord 
     * @return an ImageRecord containing a file path to resource or resource 
     * defined in object, typically byte[] or InputStream
     * @throws ResolverException
     */
    public static ImageRecord getImageRecord(Referent rft) throws ResolverException {
    	return rftResolver.getImageRecord(rft);
    }
	
    /**
     * Gets ImageRecord for the initialized IReferentResolver impl.
     * @param rft identifier/url for the requested ImageRecord 
     * @return an ImageRecord containing a file path to resource or resource 
     * defined in object, typically byte[] or InputStream
     * @throws ResolverException
     */
    public static ImageRecord getImageRecord(String rft) throws ResolverException {
    	return rftResolver.getImageRecord(rft);
    }
    
    /**
     * Initialize referent manager w/ resource resolver instance and properties.
     * Example: ReferentManager.init((IReferentResolver) Class.forName(implClass).newInstance(), props);
     * @param referentResolver
     * @param props
     * @throws ResolverException
     */
    public static void init(IReferentResolver referentResolver, Properties props) throws ResolverException {
		rftResolver = referentResolver;
		rftResolver.setProperties(props);
		init = true;
    }
}
