/**
 * $Id: SiteRecentSubmissions.java 5065 2010-06-04 18:55:12Z mdiggory $
 * $URL: http://scm.dspace.org/svn/repo/modules/dspace-discovery/trunk/block/src/main/java/org/dspace/app/xmlui/aspect/discovery/SiteRecentSubmissions.java $
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
import org.apache.solr.common.SolrDocument;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Display a list of recent submissions.
 *
 * @author Mark Diggory
 */
public class SiteRecentSubmissions extends AbstractFiltersTransformer {

    private static final Logger log = Logger.getLogger(SiteRecentSubmissions.class);

    private static final Message T_head_recent_submissions =
            message("xmlui.ArtifactBrowser.SiteViewer.head_recent_submissions");


    /**
     * Display a single community (and refrence any sub communites or
     * collections)
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException {

        try {
            performSearch(null);
        } catch (SearchServiceException e) {
            log.error(e.getMessage(), e);
        }

        Division home = body.addDivision("site-home", "primary repository");

        Division lastSubmittedDiv = home
                .addDivision("site-recent-submission", "secondary recent-submission");

        lastSubmittedDiv.setHead(T_head_recent_submissions);

        ReferenceSet lastSubmitted = lastSubmittedDiv.addReferenceSet(
                "site-last-submitted", ReferenceSet.TYPE_SUMMARY_LIST,
                null, "recent-submissions");

        if (queryResults != null)  {
            for (SolrDocument doc : queryResults.getResults()) {
                DSpaceObject obj = SearchUtils.findDSpaceObject(context, doc);
                if(obj != null)
                    lastSubmitted.addReference(obj);
            }
        }
    }


    /**
     * facet.limit=11&wt=javabin&rows=5&sort=dateaccessioned+asc&facet=true&facet.mincount=1&q=search.resourcetype:2&version=1
     *
     * @param object
     */
    @Override
    public void performSearch(DSpaceObject object) throws SearchServiceException, UIException {

        if (queryResults != null)
            return;// queryResults;

        queryArgs = prepareDefaultFilters("site");

        queryArgs.setQuery("search.resourcetype:" + Constants.ITEM);

        queryArgs.setRows(SearchUtils.getConfig().getInt("solr.recent-submissions.size", 5));

        queryArgs.setSortField(
                ConfigurationManager.getProperty("recent.submissions.sort-option"),
                SolrQuery.ORDER.desc
        );

        queryArgs.add("f.location.facet.mincount", "0");

        SearchService service = getSearchService();

        queryResults = service.search(queryArgs);

    }

}