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

import java.util.Properties;

import gov.lanl.adore.djatoka.util.ImageRecord;
import info.openurl.oom.entities.Referent;

/**
 * Interface for OpenURL Referent Resolution.  
 * @author Ryan Chute
 *
 */
public interface IReferentResolver {
	
	/**
	 * Referent Identifier to be resolved from Identifier Resolver. The returned
	 * ImageRecord need only contain the imageId and image file path.
	 * @param rftId identifier of the image to be resolved
	 * @return ImageRecord instance containing resolvable metadata
	 * @throws ResolverException
	 */
	public ImageRecord getImageRecord(String rftId) throws ResolverException;
	
	/**
	 * Referent to be resolved from Identifier Resolver. The returned
	 * ImageRecord need only contain the imageId and image file path.
	 * @param rft an OpenURL OOM Referent instance to be used to resolved image.
	 * @return ImageRecord instance containing resolvable metadata
	 * @throws ResolverException
	 */
	public ImageRecord getImageRecord(Referent rft) throws ResolverException;
	
	/**
	 * Sets a Properties object that may be used by underlying implementation
	 * @param props Properties object for use by implementation
	 * @throws ResolverException
	 */
	public void setProperties(Properties props) throws ResolverException;
	
	/**
	 * Gets the djatoka HttpServletResponse status for the requested resource
	 */
	public int getStatus(String rftId);
	
	/**
	 * Gets the resolver's active resource migrator.
	 */
	public IReferentMigrator getReferentMigrator();
}
