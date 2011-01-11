/**
 * $Id: CollectionRecentSubmissions.java 4969 2010-05-19 07:08:41Z mdiggory $
 * $URL: http://scm.dspace.org/svn/repo/modules/dspace-discovery/trunk/block/src/main/java/org/dspace/app/xmlui/aspect/discovery/CollectionRecentSubmissions.java $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.app.xmlui.aspect.discovery;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchUtils;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Display a single collection. This includes a full text search, browse by
 * list, community display and a list of recent submissions.
 *
 * @author Ben Bosman
 * @author Mark Diggory
 */
public class CollectionRecentSubmissions extends AbstractFiltersTransformer {

    private static final Logger log = Logger.getLogger(CollectionRecentSubmissions.class);
    
    private static final Message T_head_recent_submissions =
            message("xmlui.ArtifactBrowser.CollectionViewer.head_recent_submissions");


    /**
     * Display a single collection
     */
    public void addBody(Body body) throws SAXException, WingException,
            SQLException, IOException, AuthorizeException {

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        // Set up the major variables
        Collection collection = (Collection) dso;

        performSearch(collection);

        if(queryResults == null)
            return;// queryResults;

        // Build the collection viewer division.
        Division home = body.addDivision("collection-home", "primary repository collection");

        Division lastSubmittedDiv = home
                .addDivision("collection-recent-submission", "secondary recent-submission");

        lastSubmittedDiv.setHead(T_head_recent_submissions);

        ReferenceSet lastSubmitted = lastSubmittedDiv.addReferenceSet(
                "collection-last-submitted", ReferenceSet.TYPE_SUMMARY_LIST,
                null, "recent-submissions");

        for (SolrDocument doc : queryResults.getResults()) {
            lastSubmitted.addReference(
                    SearchUtils.findDSpaceObject(context, doc));
        }

    }


    /**
     * Get the recently submitted items for the given community or collection.
     *
     * @param scope The comm/collection.
     * @return the response of the query
     */
    public void performSearch(DSpaceObject scope) {


        if(queryResults != null)
            return;// queryResults;

        queryArgs = prepareDefaultFilters("collection");

        queryArgs.setQuery("search.resourcetype:" + Constants.ITEM);

        queryArgs.setRows(SearchUtils.getConfig().getInt("solr.recent-submissions.size", 5));

        queryArgs.setSortField(
                ConfigurationManager.getProperty("recent.submissions.sort-option"),
                SolrQuery.ORDER.desc
        );

        // TODO: make facet.fields options configurable (e.g., {!ex=...}).
        queryArgs.setFilterQueries("{!tag=loc}location:l" + scope.getID());
        queryArgs.add("f.location.facet.mincount", "0");

        try {
            queryResults = getSearchService().search(queryArgs);
        } catch (Throwable e) {
            log.error(e.getMessage(),e);
        }


    }

}
