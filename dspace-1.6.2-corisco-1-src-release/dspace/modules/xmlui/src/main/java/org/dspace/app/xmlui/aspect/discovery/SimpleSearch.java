/**
 * $Id: SimpleSearch.java 5165 2010-07-02 14:11:04Z KevinVandeVelde $
 * $URL: http://scm.dspace.org/svn/repo/modules/dspace-discovery/trunk/block/src/main/java/org/dspace/app/xmlui/aspect/discovery/SimpleSearch.java $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.app.xmlui.aspect.discovery;

/*
 * SimpleSearch.java
 *
 * Version: $Revision: 5165 $
 *
 * Date: $Date: 2010-07-02 11:11:04 -0300 (Fri, 02 Jul 2010) $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.net.URLEncoder;
import java.util.Collection;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.discovery.SearchUtils;
import org.xml.sax.SAXException;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SolrServiceImpl;
import org.dspace.core.Constants;

/**
 * Preform a simple search of the repository. The user provides a simple one
 * field query (the url parameter is named query) and the results are processed.
 *
 * @author mdiggory at atmire.com
 * @author kevinvandevelde at atmire.com
 */
public class SimpleSearch extends AbstractSearch implements CacheableProcessingComponent {

    private static final Logger log = Logger.getLogger(SimpleSearch.class);
    /**
     * Language Strings
     */
    private static final Message T_title =
            message("xmlui.ArtifactBrowser.SimpleSearch.title");
    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");
    private static final Message T_trail =
            message("xmlui.ArtifactBrowser.SimpleSearch.trail");
    private static final Message T_head =
            message("xmlui.ArtifactBrowser.SimpleSearch.head");
    private static final Message T_search_scope =
            message("xmlui.ArtifactBrowser.SimpleSearch.search_scope");
    private static final Message T_full_text_search =
            message("xmlui.ArtifactBrowser.SimpleSearch.full_text_search");
    private static final Message T_go =
            message("xmlui.general.go");
    private static final Message T_FILTER_HELP = message("xmlui.Discovery.SimpleSearch.filter_help");
    private static final Message T_FILTER_HEAD = message("xmlui.Discovery.SimpleSearch.filter_head");

    /**
     * Add Page metadata.
     */
    public void addPageMeta(PageMeta pageMeta) throws WingException, SQLException {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if ((dso instanceof Collection) || (dso instanceof Community)) {
            HandleUtil.buildHandleTrail(dso, pageMeta, contextPath);
        }

        pageMeta.addTrail().addContent(T_trail);


        // Add hit highlighting information
        //Map<String, Map<String, java.util.List<String>>> hl = queryResults.getHighlighting();
        //log.debug("hl: " + hl.toString());

        if (queryArgs != null) {
            log.debug("queryArgs: " + queryArgs.toString());
            //The search url must end with a /
            String searchUrl = SearchUtils.getConfig().getString("solr.search.server");
            if (searchUrl != null && !searchUrl.endsWith("/")) {
                searchUrl += "/";
            }
            String q = queryArgs.toString();
            q = "select?wt=xslt&tr=DRI.xsl&" + q;
            pageMeta.addMetadata("search", "hitHighlighting").addContent(searchUrl + q);
            log.debug("HL: " + searchUrl + q);
        } else {
            log.debug("queryArgs == NULL");
        }

    }

    /**
     * build the DRI page representing the body of the search query. This
     * provides a widget to generate a new query and list of search results if
     * present.
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException {

        String queryString = getQuery();

        // Build the DRI Body
        Division search = body.addDivision("search", "primary");
        search.setHead(T_head);
        //The search url must end with a /
        String searchUrl = SearchUtils.getConfig().getString("solr.search.server");
        if (searchUrl != null && !searchUrl.endsWith("/")) {
            searchUrl += "/";
        }

        search.addHidden("solr-search-url").setValue(searchUrl);

        Request request = ObjectModelHelper.getRequest(objectModel);
        java.util.List<String> fqs = new ArrayList<String>();
        if (request.getParameterValues("fq") != null) {
            fqs.addAll(Arrays.asList(request.getParameterValues("fq")));
        }

        //Have we added a filter using the UI
        // BUG: was "search-filter-controls_add" instead of "submit_search-filter-controls_add".
        if (request.getParameter("filter") != null && !"".equals(request.getParameter("filter")) && request.getParameter("submit_search-filter-controls_add") != null) {
            fqs.add((request.getParameter("filtertype").equals("*") ? "" : request.getParameter("filtertype") + ":") + request.getParameter("filter"));
        }

        Division query = search.addInteractiveDivision("general-query",
                "search", Division.METHOD_GET, "secondary search");

        List queryList = query.addList("search-query", List.TYPE_FORM);

        /*
        if (variableScope()) {
        Select scope = queryList.addItem().addSelect("scope");
        scope.setLabel(T_search_scope);
        buildScopeList(scope);
        }
         */

        Text text = queryList.addItem().addText("query");
        text.setLabel(T_full_text_search);
        text.setValue(queryString);


//        queryList.addItem().addContent("Filters");
        //If we have any filters, show them
        if (fqs.size() > 0) {
            //if(filters != null && filters.size() > 0){
            Composite composite = queryList.addItem().addComposite("facet-controls");

            composite.setLabel(message("xmlui.ArtifactBrowser.SimpleSearch.selected_filters"));

            CheckBox box = composite.addCheckBox("fq");

            for (String name : fqs) {
                //for(Map.Entry<String, Integer> filter : filters.entrySet()){
                //String name = filter.getKey();
                //long count = filter.getValue();


                String field = name;
                String value = name;

                if (name.contains(":")) {
                    field = name.split(":")[0];
                    value = name.split(":")[1];
                } else {
                    //We have got no field, so we are using everything
                    field = "*";
                }

                field = field.replace("_lc", "");
                value = value.replace("\\", "");
                if (field.equals("*")) {
                    field = "all";
                }
                if (name.startsWith("*:")) {
                    name = name.substring(name.indexOf(":") + 1, name.length());
                }

                Option option = box.addOption(true, name);
                option.addContent(message("xmlui.ArtifactBrowser.SimpleSearch.filter." + field));

                if (field.equals("location.comm") || field.equals("location.coll")) {
                    //We have a community/collection, resolve it to a dspaceObject
                    value = SolrServiceImpl.locationToName(context, field, value);
                }


                option.addContent(": " + value);

            }
        }


        int i = 1;
        String field = SearchUtils.getConfig().getString("solr.search.filter.type." + i, null);
        if (field != null) {
            //We have at least one filter so add our filter box
            Item item = queryList.addItem("search-filter-list", "search-filter-list");
            Composite filterComp = item.addComposite("search-filter-controls");
            filterComp.setLabel(T_FILTER_HEAD);
            filterComp.setHelp(T_FILTER_HELP);

//            filterComp.setLabel("");

            Select select = filterComp.addSelect("filtertype");
            //First of all add a default filter
            select.addOption("*", message("xmlui.ArtifactBrowser.SimpleSearch.filter.all"));
            //For each field found (at least one) add options

            while (field != null) {
                select.addOption(field, message("xmlui.ArtifactBrowser.SimpleSearch.filter." + field));

                field = SearchUtils.getConfig().getString("solr.search.filter.type." + ++i, null);
            }

            //Add a box so we can search for our value
            Text fieldText = filterComp.addText("filter");

            //And last add an add button
            filterComp.enableAddOperation();
        }

        buildSearchControls(query);

        query.addPara(null, "button-list").addButton("submit").setValue(T_go);

        // Build the DRI Body
        //Division results = body.addDivision("results", "primary");
        //results.setHead(T_head);

        // Add the result division
        try {
            buildSearchResultsDivision(search);
        } catch (SearchServiceException e) {
            throw new UIException(e.getMessage(), e);
        }

    }

    protected String[] getParameterFacetQueries() {
        try {
            java.util.List<String> allFilterQueries = new ArrayList<String>();
            Request request = ObjectModelHelper.getRequest(objectModel);
            if (request.getParameterValues("fq") != null) {
                for (int i = 0; i < request.getParameterValues("fq").length; i++) {
                    String fq = request.getParameterValues("fq")[i];
                    log.debug("fq: " + fq);
                    // BUG: adding a '*' breaks queries like date range.
                    //allFilterQueries.add(fq + "*");
                    //log.debug("allFilterQueries.add: " + fq + "*");
                    allFilterQueries.add(fq);
                }

            }

            String type = request.getParameter("filtertype");
            String value = request.getParameter("filter");

            // BUG: was "search-filter-controls_add" instead of "submit_search-filter-controls_add".
            if (value != null && !value.equals("") && request.getParameter("submit_search-filter-controls_add") != null) {
                // BUG: adding a '*' breaks queries with ranges and with sint field types.
                allFilterQueries.add((type.equals("*") ? "" : type + ":") + value); // + "*");
                log.debug("allFilterQueries.add 2nd: " + (type.equals("*") ? "" : type + ":") + value + "*");
            }

            return allFilterQueries.toArray(new String[allFilterQueries.size()]);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the search query from the URL parameter, if none is found the empty
     * string is returned.
     */
    protected String getQuery() throws UIException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String query = URLDecode(request.getParameter("query"));
        if (query == null) {
            return "";
        }
        return query.trim();
    }

    /**
     * Generate a url to the simple search url.
     */
    protected String generateURL(Map<String, String> parameters)
            throws UIException {
        String query = getQuery();
        if (!"".equals(query)) {
            parameters.put("query", URLEncode(query));
        }

        if (parameters.get("page") == null) {
            parameters.put("page", String.valueOf(getParameterPage()));
        }

        if (parameters.get("view") == null) {
            parameters.put("view", String.valueOf(getParameterView()));
        }

        if (parameters.get("rpp") == null) {
            parameters.put("rpp", String.valueOf(getParameterRpp()));
        }

        if (parameters.get("group_by") == null) {
            parameters.put("group_by", String.valueOf(this.getParameterGroup()));
        }

        if (parameters.get("sort_by") == null) {
            parameters.put("sort_by", String.valueOf(getParameterSortBy()));
        }

        if (parameters.get("order") == null) {
            parameters.put("order", getParameterOrder());
        }

        if (parameters.get("etal") == null) {
            parameters.put("etal", String.valueOf(getParameterEtAl()));
        }

        return super.generateURL("search", parameters);
    }
}
