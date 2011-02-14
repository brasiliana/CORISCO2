/**
 * $Id: CommunityRecentSubmissions.java 4873 2010-04-27 00:40:13Z mdiggory $
 * $URL: http://scm.dspace.org/svn/repo/modules/dspace-discovery/trunk/block/src/main/java/org/dspace/app/xmlui/aspect/discovery/CommunityRecentSubmissions.java $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.app.xmlui.aspect.discovery;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchUtils;
import org.xml.sax.SAXException;

/**
 * Display a single community. This includes a full text search, browse by list,
 * community display and a list of recent submissions.
 *     private static final Logger log = Logger.getLogger(DSpaceFeedGenerator.class);

 * @author Scott Phillips
 */
public class CommunityRecentSubmissions extends AbstractFiltersTransformer
{

    private static final Logger log = Logger.getLogger(CommunityRecentSubmissions.class);

    private static final Message T_head_recent_submissions =
            message("xmlui.ArtifactBrowser.CollectionViewer.head_recent_submissions");



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

        // Build the community viewer division.
        Division home = body.addDivision("community-home", "primary repository community");


        performSearch(dso);

        Division lastSubmittedDiv = home
                .addDivision("community-recent-submission", "secondary recent-submission");

        lastSubmittedDiv.setHead(T_head_recent_submissions);

        ReferenceSet lastSubmitted = lastSubmittedDiv.addReferenceSet(
                "community-last-submitted", ReferenceSet.TYPE_SUMMARY_LIST,
                null, "recent-submissions");

        for (SolrDocument doc : queryResults.getResults()) {
            lastSubmitted.addReference(SearchUtils.findDSpaceObject(context, doc));
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

        queryArgs = prepareDefaultFilters("community");

        queryArgs.setQuery("search.resourcetype:" + Constants.ITEM);

        queryArgs.setRows(SearchUtils.getConfig().getInt("solr.recent-submissions.size", 5));

        queryArgs.setSortField(
                ConfigurationManager.getProperty("recent.submissions.sort-option"),
                SolrQuery.ORDER.desc
        );

        /* Set the communities facet filters */
        queryArgs.setFilterQueries("{!tag=loc}location:m" + scope.getID());
        queryArgs.add("f.location.facet.mincount", "0");
        queryArgs.add("f.location.facet.limit", "-1");

        try {
            queryResults =  getSearchService().search(queryArgs);
        } catch (Throwable e) {
            log.error(e.getMessage(),e);
        }


    }

}
