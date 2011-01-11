/**
 * $Id: BrowseFacet.java 5161 2010-07-02 11:34:56Z KevinVandeVelde $
 * $URL: http://scm.dspace.org/svn/repo/modules/dspace-discovery/trunk/block/src/main/java/org/dspace/app/xmlui/aspect/discovery/BrowseFacet.java $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.app.xmlui.aspect.discovery;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Community;
import org.dspace.content.Collection;
import org.dspace.authorize.AuthorizeException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.dspace.discovery.*;
import org.xml.sax.SAXException;

import java.io.Serializable;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


/**
 *
 */
public class NavigationBrowse extends AbstractDSpaceTransformer implements CacheableProcessingComponent {

    private static final Logger log = Logger.getLogger(NavigationBrowse.class);

    /** Language Strings */
    private static final Message T_head_all_of_dspace =
            message("xmlui.Discovery.Navigation.head_all_of_dspace");
    private static final Message T_head_browse =
            message("xmlui.Discovery.Navigation.head_browse");
    private static final Message T_communities_and_collections =
            message("xmlui.Discovery.Navigation.communities_and_collections");
    private static final Message T_head_this_collection =
            message("xmlui.Discovery.Navigation.head_this_collection");
    private static final Message T_head_this_community =
            message("xmlui.Discovery.Navigation.head_this_community");

    /**
     * The cache of recently submitted items
     */
//    protected QueryResponse queryResults;

    /**
     * Cached validity object
     */
    protected SourceValidity validity;

//    final static String STARTS_WITH = "starts_with";
//    final static String PAGE = "page";
//    final static String ORDER = "order";
//    final static String RESULTS_PER_PAGE = "rpp";
//    final static String SORT_BY = "sort_by";
//    final static String VIEW = "view";

    public static final String FACET_FIELD = "field";
    public static final String BROWSE_URL_BASE = "browse";

//    private ConfigurationService config = null;
//    private SearchService searchService = null;

    public NavigationBrowse() {
//        DSpace dspace = new DSpace();
//        config = dspace.getConfigurationService();
//        searchService = dspace.getServiceManager().getServiceByName(SearchService.class.getName(), SearchService.class);
    }

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    @Override
    public Serializable getKey() {
        try {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

            if (dso == null) {
                return "0";
            }

            return HashUtil.hash(dso.getHandle());
        } catch (SQLException sqle) {
            // Ignore all errors and just return that the component is not
            // cachable.
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     * <p/>
     * The validity object will include the collection being viewed and
     * all recently submitted items. This does not include the community / collection
     * hierarch, when this changes they will not be reflected in the cache.
     */
    @Override
    public SourceValidity getValidity() {
        if (this.validity == null) {

            try {
                DSpaceValidity validity = new DSpaceValidity();

                DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

                if (dso != null) {
                    // Add the actual collection;
                    validity.add(dso);
                }

                // add reciently submitted items, serialize solr query contents.
//                QueryResponse response = getQueryResponse(dso);

//                validity.add("numFound:" + response.getResults().getNumFound());

//                for (SolrDocument doc : response.getResults()) {
//                    validity.add(doc.toString());
//                }
//
//                for (FacetField field : response.getFacetFields()) {
//                    validity.add(field.getName());
//
//                    for (FacetField.Count count : field.getValues()) {
//                        validity.add(count.getName() + "#" + count.getCount());
//                    }
//                }
//

                this.validity = validity.complete();
            } catch (Exception e) {
                // Just ignore all errors and return an invalid cache.
            }

            //TODO: dependent on tags as well :)
        }
        return this.validity;
    }


    /**
     * Add the basic navigational options:
     *
     * Search - advanced search
     *
     * browse - browse by Titles - browse by Authors - browse by Dates
     *
     * language FIXME: add languages
     *
     * context no context options are added.
     *
     * action no action options are added.
     */
    @Override
    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        List browse = options.addList("browse");

        browse.setHead(T_head_browse);

        List browseGlobal = browse.addList("global");
//        browseGlobal.setHead(T_head_all_of_dspace);
//        browseGlobal.addItemXref(contextPath + "/community-list", T_communities_and_collections);
        // Add the configured browse lists for 'top level' browsing
        addBrowseOptions(browseGlobal, contextPath + "/browse");

        List browseContext = browse.addList("context");

        if (dso != null)
        {
            if (dso instanceof Collection)
            {
                browseContext.setHead(T_head_this_collection);
                // Add the configured browse lists for scoped browsing
                String handle = dso.getHandle();
                addBrowseOptions(browseContext, contextPath + "/handle/" + handle + "/browse");
                //browseContext.addItem().addXref(contextPath + "/discovery/?q=search.resourcetype%3A2+AND+location%3Al" + dso.getID(), T_head_this_collection );
            }
            if (dso instanceof Community)
            {
                browseContext.setHead(T_head_this_community);
                // Add the configured browse lists for scoped browsing
                String handle = dso.getHandle();
                addBrowseOptions(browseContext, contextPath + "/handle/" + handle + "/browse");
                //browseContext.addItem().addXref(contextPath + "/discovery/?q=search.resourcetype%3A2+AND+location%3Am" + dso.getID(), T_head_this_community );
            }
        }
    }

    
    /**
     * Add navigation for the configured browse tables to the supplied list.
     *
     * @param browseList
     * @param browseURL
     * @throws WingException
     */
    private void addBrowseOptions(List browseList, String browseURL) throws WingException
    {
        // FIXME Exception handling
        try
        {
            // Get a Map of all the browse tables
            String[] facets = SearchUtils.getFacetsForType("browse");
            for (String facet : facets)
            {
                // Create a Map of the query parameters for this link
                Map<String, String> parameters = new HashMap<String, String>();
                parameters.put(FACET_FIELD, facet);

                // Add a link to this browse
                browseList.addItemXref(generateURL(browseURL, parameters),
                        message("xmlui.ArtifactBrowser.Navigation.browse_" + facet));
            }
        }
        catch (Exception bex)
        {
            throw new UIException("Unable to get browse indicies", bex);
        }
    }

    
    /**
     * Recycle
     */
    @Override
    public void recycle() {
        // Clear out our item's cache.
        this.validity = null;
        super.recycle();
    }

}
