/**
 * $Id: CollectionViewer.java 4679 2010-01-11 20:05:13Z mdiggory $
 * $URL: http://scm.dspace.org/svn/repo/modules/dspace-discovery/trunk/block/src/main/java/org/dspace/app/xmlui/aspect/discovery/CollectionViewer.java $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.app.xmlui.aspect.discovery;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.LogManager;
import org.xml.sax.SAXException;

/**
 * Display a single collection. This includes a full text search, browse by
 * list, community display and a list of recent submissions.
 * 
 * @author Scott Phillips
 */
public class CollectionViewer extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    private static final Logger log = Logger.getLogger(CollectionViewer.class);

    /** Language Strings */
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    public static final Message T_untitled = 
    	message("xmlui.general.untitled");
    
    /** Cached validity object */
    private SourceValidity validity;
    
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey()
    {
        try
        {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

            if (dso == null)
                return "0";
                
            return HashUtil.hash(dso.getHandle());
        }
        catch (SQLException sqle)
        {
            // Ignore all errors and just return that the component is not
            // cachable.
            return "0";
        }
    }


    /**
     * Generate the cache validity object.
     * 
     * The validity object will include the collection being viewed and 
     * all recently submitted items. This does not include the community / collection
     * hierarch, when this changes they will not be reflected in the cache.
     */
    public SourceValidity getValidity()
    {
    	if (this.validity == null)
    	{
            Collection collection = null;
	        try
	        {
	            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
	
	            if (dso == null)
	                return null;
	
	            if (!(dso instanceof Collection))
	                return null;
	
	            collection = (Collection) dso;
	
	            DSpaceValidity validity = new DSpaceValidity();
	            
	            // Add the actual collection;
	            validity.add(collection);
	            
	            this.validity = validity.complete();
	        }
	        catch (Exception e)
	        {
	            // Just ignore all errors and return an invalid cache.
	        }

            log.info(LogManager.getHeader(context, "view_collection", "collection_id=" + (collection == null ? "" : collection.getID())));

    	}
    	return this.validity;
    }
    
    
    /**
     * Add a page title and trail links.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Collection))
            return;

        Collection collection = (Collection) dso;

        // Set the page title
        String name = collection.getMetadata("name");
        if (name == null || name.length() == 0)
        	pageMeta.addMetadata("title").addContent(T_untitled);
        else
        	pageMeta.addMetadata("title").addContent(name);

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        HandleUtil.buildHandleTrail(collection,pageMeta,contextPath);
        
        // Add RSS links if available
        String formats = ConfigurationManager.getProperty("webui.feed.formats");
		if ( formats != null )
		{
			for (String format : formats.split(","))
			{
				// Remove the protocol number, i.e. just list 'rss' or' atom'
				String[] parts = format.split("_");
				if (parts.length < 1) 
					continue;
				
				String feedFormat = parts[0].trim()+"+xml";
					
				String feedURL = contextPath+"/feed/"+format.trim()+"/"+collection.getHandle();
				pageMeta.addMetadata("feed", feedFormat).addContent(feedURL);
			}
		}
    }

    /**
     * Display a single collection
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Collection))
            return;

        // Set up the major variables
        Collection collection = (Collection) dso;
        

        // Build the collection viewer division.
        Division home = body.addDivision("collection-home", "primary repository collection");
        String name = collection.getMetadata("name");
        if (name == null || name.length() == 0)
        	home.setHead(T_untitled);
        else
        	home.setHead(name);

        // Add the reference
        {
        	Division viewer = home.addDivision("collection-view","secondary");
            ReferenceSet mainInclude = viewer.addReferenceSet("collection-view",
                    ReferenceSet.TYPE_DETAIL_VIEW);
            mainInclude.addReference(collection);
        }
        
    }
    
    /**
     * Recycle
     */
    public void recycle() 
    {   
        // Clear out our item's cache.
        this.validity = null;
        super.recycle();
    }
}
