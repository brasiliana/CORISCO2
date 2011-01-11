/**
 * $Id: CommunityViewer.java 4679 2010-01-11 20:05:13Z mdiggory $
 * $URL: http://scm.dspace.org/svn/repo/modules/dspace-discovery/trunk/block/src/main/java/org/dspace/app/xmlui/aspect/discovery/CommunityViewer.java $
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
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.app.xmlui.wing.element.Reference;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseItem;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.LogManager;
import org.xml.sax.SAXException;

/**
 * Display a single community. This includes a full text search, browse by list,
 * community display and a list of recent submissions.
 *     private static final Logger log = Logger.getLogger(DSpaceFeedGenerator.class);

 * @author Scott Phillips
 */
public class CommunityViewer extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    private static final Logger log = Logger.getLogger(CommunityViewer.class);
	
    /** Language Strings */
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_full_text_search =
        message("xmlui.ArtifactBrowser.CommunityViewer.full_text_search");
    
    private static final Message T_go =
        message("xmlui.general.go");
    
    public static final Message T_untitled = 
    	message("xmlui.general.untitled");

    private static final Message T_head_browse =
        message("xmlui.ArtifactBrowser.CommunityViewer.head_browse");
    
    private static final Message T_browse_titles = 
        message("xmlui.ArtifactBrowser.CommunityViewer.browse_titles");
    
    private static final Message T_browse_authors =
        message("xmlui.ArtifactBrowser.CommunityViewer.browse_authors");
    
    private static final Message T_browse_dates =
        message("xmlui.ArtifactBrowser.CommunityViewer.browse_dates");
    
    private static final Message T_advanced_search_link=
    	message("xmlui.ArtifactBrowser.CommunityViewer.advanced_search_link");
    
    private static final Message T_head_sub_communities = 
        message("xmlui.ArtifactBrowser.CommunityViewer.head_sub_communities");
    
    private static final Message T_head_sub_collections =
        message("xmlui.ArtifactBrowser.CommunityViewer.head_sub_collections");
    
    private static final Message T_head_recent_submissions =
        message("xmlui.ArtifactBrowser.CommunityViewer.head_recent_submissions");
    
    /** How many recient submissions to list */
    private static final int RECENT_SUBMISISONS = 5;

    /** The cache of recently submitted items */
    private java.util.List<BrowseItem> recentSubmittedItems;
    
    /** Cached validity object */
    private SourceValidity validity;
    
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        try {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            
            if (dso == null)
                return "0"; // no item, something is wrong
            
            return HashUtil.hash(dso.getHandle());
        } 
        catch (SQLException sqle)
        {
            // Ignore all errors and just return that the component is not cachable.
            return "0";
        }
    }

    
    /**
     * Generate the cache validity object.
     * 
     * This validity object includes the community being viewed, all 
     * sub-communites (one level deep), all sub-collections, and 
     * recently submitted items.
     */
    public SourceValidity getValidity() 
    {
        if (this.validity == null)
    	{
            Community community = null;
	        try {
	            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
	            
	            if (dso == null)
	                return null;
	            
	            if (!(dso instanceof Community))
	                return null;
	            
	            community = (Community) dso;
	            
	            DSpaceValidity validity = new DSpaceValidity();
	            validity.add(community);
	            
	            Community[] subCommunities = community.getSubcommunities();
	            Collection[] collections = community.getCollections();
	            // Sub communities
	            for (Community subCommunity : subCommunities)
	            {
	                validity.add(subCommunity);
	            }
	            // Sub collections
	            for (Collection collection : collections)
	            {
	                validity.add(collection);
                }
	            
	            this.validity = validity.complete();

	        } 
	        catch (Exception e)
	        {
	            // Ignore all errors and invalidate the cache.
	        }

            log.info(LogManager.getHeader(context, "view_community", "community_id=" + (community == null ? "" : community.getID())));

        }


        return this.validity;
    }
    
    
    /**
     * Add the community's title and trail links to the page's metadata
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Community))
            return;

        // Set up the major variables
        Community community = (Community) dso;
        // Set the page title
        String name = community.getMetadata("name");
        if (name == null || name.length() == 0)
        	pageMeta.addMetadata("title").addContent(T_untitled);
        else
        	pageMeta.addMetadata("title").addContent(name);

        // Add the trail back to the repository root.
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        HandleUtil.buildHandleTrail(community, pageMeta,contextPath);
        
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
					
				String feedURL = contextPath+"/feed/"+format.trim()+"/"+community.getHandle();
				pageMeta.addMetadata("feed", feedFormat).addContent(feedURL);
			}
		}
    }

    /**
     * Display a single community (and refrence any sub communites or
     * collections)
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Community))
            return;

        // Set up the major variables
        Community community = (Community) dso;
        Community[] subCommunities = community.getSubcommunities();
        Collection[] collections = community.getCollections();


        // Build the community viewer division.
        Division home = body.addDivision("community-home", "primary repository community");
        String name = community.getMetadata("name");
        if (name == null || name.length() == 0)
        	home.setHead(T_untitled);
        else
        	home.setHead(name);

        
        // Add main reference:
        {
        	Division viewer = home.addDivision("community-view","secondary");
        	
            ReferenceSet referenceSet = viewer.addReferenceSet("community-view",
                    ReferenceSet.TYPE_DETAIL_VIEW);
            Reference communityInclude = referenceSet.addReference(community);

            // If the community has any children communities also refrence them.
            if (subCommunities != null && subCommunities.length > 0)
            {
                ReferenceSet communityReferenceSet = communityInclude
                        .addReferenceSet(ReferenceSet.TYPE_SUMMARY_LIST,null,"hierarchy");

                communityReferenceSet.setHead(T_head_sub_communities);

                // Sub communities
                for (Community subCommunity : subCommunities)
                {
                    communityReferenceSet.addReference(subCommunity);
                }
            }
            if (collections != null && collections.length > 0)
            {
                ReferenceSet communityReferenceSet = communityInclude
                        .addReferenceSet(ReferenceSet.TYPE_SUMMARY_LIST,null,"hierarchy");

                communityReferenceSet.setHead(T_head_sub_collections);
                       

                // Sub collections
                for (Collection collection : collections)
                {
                    communityReferenceSet.addReference(collection);
                }

            }
        }// main refrence

    }

    /**
     * Recycle
     */
    public void recycle()
    {
        this.validity = null;
        super.recycle();
    }
    
    
    
}
