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
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.util.HashUtil;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.excalibur.source.SourceValidity;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.FacetParams;
import org.apache.log4j.Logger;
import org.dspace.discovery.*;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.Serializable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.net.URLEncoder;
import java.util.List;
//import org.dspace.sort.SortException;
//import org.dspace.sort.SortOption;

/**
 * User: mdiggory
 * Date: Sep 25, 2009
 * Time: 11:54:11 PM
 */
public class BrowseFacet extends AbstractDSpaceTransformer implements CacheableProcessingComponent {

    private static final Logger log = Logger.getLogger(BrowseFacet.class);

    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

    private final static Message T_go = message("xmlui.general.go");

    private final static Message T_update = message("xmlui.general.update");

    private final static Message T_starts_with = message("xmlui.ArtifactBrowser.ConfigurableBrowse.general.starts_with");

    private final static Message T_starts_with_help = message("xmlui.ArtifactBrowser.ConfigurableBrowse.general.starts_with_help");

//    private final static Message T_choose_year = message("xmlui.ArtifactBrowser.ConfigurableBrowse.general.choose_year");

    private final static Message T_jump_year = message("xmlui.ArtifactBrowser.ConfigurableBrowse.general.jump_year");

    private final static Message T_jump_year_help = message("xmlui.ArtifactBrowser.ConfigurableBrowse.general.jump_year_help");

//    private final static Message T_jump_select = message("xmlui.ArtifactBrowser.ConfigurableBrowse.general.jump_select");

    private final static Message T_jump_century_head = message("xmlui.Discovery.BrowseFacet.browse_century_head");
    private final static Message T_jump_century = message("xmlui.Discovery.BrowseFacet.browse_century");

//    private static final Message T_sort_by_relevance = message("xmlui.ArtifactBrowser.AbstractSearch.sort_by.relevance");
//    private final static Message T_sort_by = message("xmlui.ArtifactBrowser.AbstractSearch.sort_by");

    private final static Message T_order = message("xmlui.ArtifactBrowser.AbstractSearch.order");
    private final static Message T_order_asc = message("xmlui.ArtifactBrowser.AbstractSearch.order.asc");
    private final static Message T_order_desc = message("xmlui.ArtifactBrowser.AbstractSearch.order.desc");

    private final static Message T_rpp = message("xmlui.ArtifactBrowser.AbstractSearch.rpp");

    private final static Message T_view = message("xmlui.ArtifactBrowser.AbstractSearch.view");

    /**
     * The options for results per page
     */
    private static final int[] RESULTS_PER_PAGE_PROGRESSION = {10, 20, 50, 100};

    /**
     * The types of view
     */
    private static final String[] VIEW_TYPES = {"list", "cloud"};

    /**
     * The cache of recently submitted items
     */
    protected QueryResponse queryResults;

    /**
     * Cached validity object
     */
    protected SourceValidity validity;

    /**
     * Cached query arguments
     */
    protected SolrQuery queryArgs;

    private int DEFAULT_PAGE_SIZE = 50;
    //private String DEFAULT_ORDER = SortOption.ASCENDING;
    private SolrQuery.ORDER DEFAULT_ORDER = SolrQuery.ORDER.asc;
    private String DEFAULT_SORT_BY = "lex";

//    public static final String OFFSET = "offset";
    public static final String FACET_FIELD = "field";
    final static String STARTS_WITH = "starts_with";
    final static String PAGE = "page";
    final static String ORDER = "order";
    final static String RESULTS_PER_PAGE = "rpp";
    final static String SORT_BY = "sort_by";
    final static String VIEW = "view";

    public static final String BROWSE_URL_BASE = "browse";

    private ConfigurationService config = null;

    private SearchService searchService = null;

    
    public BrowseFacet() {

        DSpace dspace = new DSpace();
        config = dspace.getConfigurationService();
        searchService = dspace.getServiceManager().getServiceByName(SearchService.class.getName(), SearchService.class);

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
                QueryResponse response = getQueryResponse(dso);

                validity.add("numFound:" + response.getResults().getNumFound());

//                for (SolrDocument doc : response.getResults()) {
//                    validity.add(doc.toString());
//                }

                for (SolrDocument doc : response.getResults()) {
                    validity.add(doc.toString());
                }

                for (FacetField field : response.getFacetFields()) {
                    validity.add(field.getName());

                    for (FacetField.Count count : field.getValues()) {
                        validity.add(count.getName() + "#" + count.getCount());
                    }
                }


                this.validity = validity.complete();
            } catch (Exception e) {
                // Just ignore all errors and return an invalid cache.
            }

            //TODO: dependent on tags as well :)
        }
        return this.validity;
    }

    /**
     * Get the recently submitted items for the given community or collection.
     *
     * @param scope The collection.
     */
    protected QueryResponse getQueryResponse(DSpaceObject scope) {


        Request request = ObjectModelHelper.getRequest(objectModel);

        if (queryResults != null) {
            return queryResults;
        }

        queryArgs = new SolrQuery();

        //Make sure we add our default filters
        queryArgs.addFilterQuery(SearchUtils.getDefaultFilters(BROWSE_URL_BASE));


        queryArgs.setQuery("search.resourcetype: " + Constants.ITEM + ((request.getParameter("query") != null && !"".equals(request.getParameter("query"))) ? " AND (" + request.getParameter("query") + ")" : ""));
//        queryArgs.setQuery("search.resourcetype:" + Constants.ITEM);

        queryArgs.setRows(0);

        queryArgs.setSortField(
                ConfigurationManager.getProperty("recent.submissions.sort-option"),
                getParameterOrder());

        queryArgs.addFilterQuery(getParameterFacetQueries());

        queryArgs.setFacet(true);
        
        //Set the default limit to 11
        //query.setFacetLimit(11);
        queryArgs.setFacetMinCount(1);

        //sort
        //TODO: why this kind of sorting ? Should the sort not be on how many times the value appears like we do in the filter by sidebar ?
        queryArgs.setFacetSort(getParameterSortBy());

        String facetField = getParameterField();

        int page = getParameterPage();
        int rpp = getParameterRpp();

        // FIXME: Have to retrieve everything, since there is no way of getting total facets in order to do masked pagination.
//        queryArgs.setParam(FacetParams.FACET_OFFSET, String.valueOf((page - 1) * rpp));
//        queryArgs.setFacetLimit(rpp);//setParam(FacetParams.FACET_LIMIT, String.valueOf(rpp));
        queryArgs.setFacetLimit(-1);

//        int offset = RequestUtils.getIntParameter(request, OFFSET);
//        if (offset == -1) {
//            offset = 0;
//        }
//        if (facetField != null && facetField.endsWith(".year")) {
//            queryArgs.setParam(FacetParams.FACET_OFFSET, "0");
//            queryArgs.setParam(FacetParams.FACET_LIMIT, "1000000");
//            facetField = facetField.replace(".year", "");
//        } else {
//            queryArgs.setParam(FacetParams.FACET_OFFSET, String.valueOf(offset));
            //We add +1 so we can use the extra one to make sure that we need to show the next page
//            queryArgs.setParam(FacetParams.FACET_LIMIT, String.valueOf(rpp));
//        }


        if (scope != null) /* top level search / community */ {
            if (scope instanceof Community) {
                //queryArgs.setFilterQueries("location:m" + scope.getID());
                queryArgs.addFilterQuery("location:m" + scope.getID());
            } else if (scope instanceof Collection) {
                //queryArgs.setFilterQueries("location:l" + scope.getID());
                queryArgs.addFilterQuery("location:l" + scope.getID());
            }
        }


        boolean isDate = false;
        if (facetField != null && facetField.endsWith("_dt")) {
            facetField = facetField.split("_")[0];
            isDate = true;
        }

        if (isDate) {

            queryArgs.setParam(FacetParams.FACET_DATE, new String[]{facetField});
            queryArgs.setParam(FacetParams.FACET_DATE_GAP, "+1YEAR");

            Date lowestDate = getLowestDateValue(queryArgs.getQuery(), facetField, queryArgs.getFilterQueries());
            int thisYear = Calendar.getInstance().get(Calendar.YEAR);

            DateFormat formatter = new SimpleDateFormat("yyyy");
            int maxEndYear = Integer.parseInt(formatter.format(lowestDate));

            //Since we have a date, we need to find the last year
            String startDate = "NOW/YEAR-" + SearchUtils.getConfig().getString("solr.date.gap", "10") + "YEARS";
            String endDate = "NOW";
//            int startYear = thisYear - (offset + DEFAULT_PAGE_SIZE);
            int startYear = thisYear - ((page - 1) * rpp);
            // We shouldn't go lower then our max bottom year
            // Make sure to substract one so the bottom year is also counted !
            if (startYear < maxEndYear) {
                startYear = maxEndYear - 1;
            }

//            if (0 < offset) {
            if (page > 1) {
                //Say that we have an offset of 10 years
                //we need to go back 10 years (2010 - (2010 - 10))
                //(add one to compensate for the NOW in the start)
//                int endYear = thisYear - offset + 1;
                int endYear = thisYear - ((page - 1) * rpp) + 1;

                endDate = "NOW/YEAR-" + (thisYear - endYear) + "YEARS";
                //Add one to the startyear to get one more result
                //When we select NOW, the current year is also used (so auto+1)
            }
            startDate = "NOW/YEAR-" + (thisYear - startYear) + "YEARS";

            queryArgs.setParam(FacetParams.FACET_DATE_START, startDate);
            queryArgs.setParam(FacetParams.FACET_DATE_END, endDate);

            queryArgs.setFacetMinCount(1);

            System.out.println(startDate);
            System.out.println(endDate);

        } else {
            queryArgs.addFacetField(new String[]{facetField});
        }

//        log.debug("queryArgs: " + queryArgs.toString());

        String startsWith = getParameterStartsWith();
        if (startsWith != null) {
            queryArgs.setFacetPrefix(facetField, startsWith);
        }

        try {
            queryResults = searchService.search(queryArgs);
        } catch (SearchServiceException e) {
            log.error(e.getMessage(), e);
        }

        return queryResults;
    }

    /**
     * Retrieves the lowest date value in the given field
     * @param query a solr query
     * @param dateField the field for which we want to retrieve our date
     * @param filterquery the filterqueries
     * @return the lowest date found, in a date object
     */
    private Date getLowestDateValue(String query, String dateField, String... filterquery) {

        try {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(query);
            solrQuery.setFields(dateField);
            solrQuery.setRows(1);
            solrQuery.setSortField(dateField, SolrQuery.ORDER.asc);
            solrQuery.setFilterQueries(filterquery);

            QueryResponse rsp = searchService.search(solrQuery);
            if (0 < rsp.getResults().getNumFound()) {
                return (Date) rsp.getResults().get(0).getFieldValue(dateField);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
//        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        //List test = options.addList("browse");

//        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

//        List test = options.addList("browse");

//        org.dspace.app.xmlui.wing.element.List browse = options.addList("browse");

//        browse.setHead(T_head_browse);
/*
        List browseGlobal = browse.addList("global");
        List browseContext = browse.addList("context");

        browseGlobal.setHead(T_head_all_of_dspace);


        if (dso != null)
        {
            if (dso instanceof Collection)
            {
                browseContext.addItem().addXref(contextPath + "/discovery/?q=search.resourcetype%3A2+AND+location%3Al" + dso.getID(), T_head_this_collection );
            }
            if (dso instanceof Community)
            {
                browseContext.addItem().addXref(contextPath + "/discovery/?q=search.resourcetype%3A2+AND+location%3Am" + dso.getID(), T_head_this_community );
            }
        }
        browseGlobal.addItem().addXref(contextPath + "/community-list", T_head_all_of_dspace );
    */

    }

    /**
     * Add a page title and trail links.
     */
    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
//        Request request = ObjectModelHelper.getRequest(objectModel);
        String facetField = getParameterField();

        pageMeta.addMetadata("title").addContent(message("xmlui.ArtifactBrowser.AbstractSearch.type_" + facetField + "_browse"));


        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if ((dso instanceof Collection) || (dso instanceof Community)) {
            HandleUtil.buildHandleTrail(dso, pageMeta, contextPath);
        }

        pageMeta.addTrail().addContent(message("xmlui.ArtifactBrowser.AbstractSearch.type_" + facetField + "_browse"));
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {

        Request request = ObjectModelHelper.getRequest(objectModel);
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        java.util.List fqs = Arrays.asList(getParameterFacetQueries());

        //Make sure we get our results
//        log.debug("query results 1: " + queryResults.toString());
        queryResults = getQueryResponse(dso);

        if (this.queryResults != null) {
            log.debug("query results 2: " + queryResults.toString());

            java.util.List<FacetField> facetFields = this.queryResults.getFacetFields();
            if (facetFields == null) {
                facetFields = new ArrayList<FacetField>();
            }

            facetFields.addAll(this.queryResults.getFacetDates());

            log.debug("facetFields: " + facetFields.toString());

            if (facetFields.size() > 0) {
                FacetField field = facetFields.get(0);
                java.util.List<FacetField.Count> values = field.getValues();
                if (field.getGap() != null || field.getName().endsWith(".year")) {
                    //We are dealing with dates so flip em, top date comes first
                    DEFAULT_ORDER = SolrQuery.ORDER.desc;
                } else {
                    DEFAULT_ORDER = SolrQuery.ORDER.asc;
                }

                // Build the DRI Body
                Division div = body.addDivision("browse-by-" + field.getName(), "primary");
                div.setHead(message("xmlui.ArtifactBrowser.AbstractSearch.type_" + getParameterField() + "_browse"));

                // Build the internal navigation (jump lists)
                addBrowseJumpNavigation(div, field, request);

                // Build the sort and display controls
                addBrowseControls(div, field, request);

                if (values != null && 0 < values.size()) {
                    if (getParameterOrder().equals(SolrQuery.ORDER.desc)) {
                        Collections.reverse(values);
                    }

//                    Division results = body.addDivision("browse-by-" + field.getName() + "-results", "primary");
                    Division results = div.addDivision("browse-by-" + field.getName() + "-results", this.getParameterView());

                    //results.setHead(message("xmlui.ArtifactBrowser.AbstractSearch.type_" + getParameterField() + "_browse"));

                    // Find our faceting offset
                    /*
                    int offSet = 0;
                    try {
                        offSet = Integer.parseInt(queryArgs.get(FacetParams.FACET_OFFSET));
                    } catch (NumberFormatException e) {
                        //Ignore
                    }

                    //Only show the nextpageurl if we have at least one result following our current results
                    String nextPageUrl = null;
                    if (field.getName().endsWith(".year")) {
                        offSet = Util.getIntParameter(request, "offset");
                        offSet = offSet == -1 ? 0 : offSet;

                        if ((offSet + DEFAULT_PAGE_SIZE) < values.size()) {
                            nextPageUrl = getNextPageURL(request);
                        }
                    } else {
                        if (values.size() == (DEFAULT_PAGE_SIZE + 1)) {
                            nextPageUrl = getNextPageURL(request);
                        }
                    }

                    int shownItemsMax;

                    if (field.getName().endsWith(".year")) {
                        if ((values.size() - offSet) < DEFAULT_PAGE_SIZE) {
                            shownItemsMax = values.size();
                        } else {
                            shownItemsMax = DEFAULT_PAGE_SIZE;
                        }
                    } else {
                        shownItemsMax = offSet + (DEFAULT_PAGE_SIZE < values.size() ? values.size() - 1 : values.size());

                    }
                    */

//                    results.setSimplePagination((int) queryResults.getResults().getNumFound(), offSet + 1,
//                            shownItemsMax, getPreviousPageURL(request), nextPageUrl);

//// Switching to masked pagination.
                    // Pagination variables.
//                    int itemsTotal = (int) solrResults.getNumFound();
//                    int firstItemIndex = (int) solrResults.getStart() + 1;
//                    int lastItemIndex = (int) solrResults.getStart() + solrResults.size();
//                    //if (itemsTotal < lastItemIndex)
//                    //    lastItemIndex = itemsTotal;
//                    int currentPage = (int) (solrResults.getStart() / this.queryArgs.getFacetLimit()) + 1;
//                    int pagesTotal = (int) ((solrResults.getNumFound() - 1) / this.queryArgs.getFacetLimit()) + 1;

                    int itemsTotal = values.size();
                    int firstItemIndex = (getParameterPage() - 1) * getParameterRpp() +1;
//                    int firstItemIndex = Integer.parseInt(queryArgs.get(FacetParams.FACET_OFFSET,
//                            String.valueOf((getParameterPage() - 1) * queryArgs.getFacetLimit())));
                    int lastItemIndex = firstItemIndex + getParameterRpp() - 1;
                    if (lastItemIndex > itemsTotal)
                        lastItemIndex = itemsTotal;
                    //if (itemsTotal < lastItemIndex)
                    //    lastItemIndex = itemsTotal;
                    int currentPage = getParameterPage(); //(int) (firstItemIndex / getParameterRpp()) + 1;
                    int pagesTotal = (int) ((itemsTotal - 1) / getParameterRpp()) + 1;

                    //log.error("items total: " + itemsTotal + "; first: " + firstItemIndex + "; last: " + lastItemIndex + "; rpp: " + getParameterRpp() + "; cur page: " + currentPage);

                    Map<String, String> parameters = new HashMap<String, String>();
                    parameters.put("page", "{pageNum}");
                    String pageURLMask = generateURL(parameters);

                    results.setMaskedPagination(itemsTotal, firstItemIndex, lastItemIndex, currentPage, pagesTotal, pageURLMask);
                    ////

                    Table singleTable = results.addTable("browse-by-" + field.getName() + "-results", lastItemIndex - firstItemIndex + 1, 1);
                    List<String> filterQueries = new ArrayList<String>();

//                    if (request.getParameterValues("fq") != null) {
//                        filterQueries = Arrays.asList(request.getParameterValues("fq"));
//                    }
                    filterQueries = Arrays.asList(getParameterFacetQueries());

                    if (field.getName().endsWith(".year")) {
                        for (int i = firstItemIndex - 1; i < lastItemIndex; i++) {
                            FacetField.Count value = values.get(i);
                            renderFacetField(request, dso, field, singleTable, filterQueries, value);
                        }
                        //TODO
//                        int start = (values.size() - 1) - this.queryArgs.getFacetLimit();
//                        int end = start - getParameterRpp();
//                        if (end < 0) {
//                            end = 0;
//                        } else {
//                            end++;
//                        }
//                        for (int i = start; end <= i; i--) {
//                            FacetField.Count value = values.get(i);
//                            renderFacetField(request, dso, field, singleTable, filterQueries, value);
//                        }
                    } else {
//                        int end = values.size();
//                        if (getParameterRpp() < end) {
//                            end = getParameterRpp();
//                        }
//
//                        for (int i = 0; i < end; i++) {
//                            FacetField.Count value = values.get(i);
//                            renderFacetField(request, dso, field, singleTable, filterQueries, value);
//                        }
                        for (int i = firstItemIndex - 1; i < lastItemIndex; i++) {
                            FacetField.Count value = values.get(i);
                            renderFacetField(request, dso, field, singleTable, filterQueries, value);
                        }
                    }


                }
            }
        }

        //DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        /*
        if (dso != null)
        {
        if (dso instanceof Collection)
        {
        browseContext.addItem().addXref(contextPath + "/discovery/?q=search.resourcetype%3A2+AND+location%3Al" + dso.getID(), T_head_this_collection );
        }
        if (dso instanceof Community)
        {
        browseContext.addItem().addXref(contextPath + "/discovery/?q=search.resourcetype%3A2+AND+location%3Am" + dso.getID(), T_head_this_community );
        }
        }

        browseGlobal.addItem().addXref(contextPath + "/discovery/?q=search.resourcetype%3A2", T_head_all_of_dspace );
         */
    }

    private void renderFacetField(Request request, DSpaceObject dso, FacetField field, Table singleTable, List<String> filterQueries, FacetField.Count value) throws SQLException, WingException, UnsupportedEncodingException {
        String displayedValue = value.getName();
        String filterQuery = value.getAsFilterQuery();
        if (field.getName().equals("location.comm") || field.getName().equals("location.coll")) {
            //We have a community/collection, resolve it to a dspaceObject
//            displayedValue = SolrServiceImpl.locationToName(context, field.getName(), displayedValue);
            int type = field.getName().equals("location.comm") ? Constants.COMMUNITY : Constants.COLLECTION;
            DSpaceObject commColl = DSpaceObject.find(context, type, Integer.parseInt(displayedValue));
            if (commColl != null) {
                displayedValue = commColl.getName() + "TEST";
            }
        }
        if (field.getGap() != null) {
            //We have a date get the year so we can display it
            DateFormat simpleDateformat = new SimpleDateFormat("yyyy");
            displayedValue = simpleDateformat.format(SolrServiceImpl.toDate(displayedValue));
            //displayedValue = displayedValue.substring(0, 4);
            filterQuery = ClientUtils.escapeQueryChars(value.getFacetField().getName()) + ":" + displayedValue + "*";
        }

        Cell cell = singleTable.addRow().addCell();

        //No use in selecting the same filter twice
        if (filterQueries.contains(filterQuery)) {
//            cell.addContent(displayedValue + " (" + value.getCount() + ")");
            cell.addContent(displayedValue + " (" + value.getCount() + ")");
        } else {
            cell.addXref(
                    contextPath + (dso == null ? "" : "/handle/" + dso.getHandle())
                    + "/search?"
                    + "&fq="
                    + URLEncoder.encode(filterQuery, "UTF-8")
                    //+ (request.getQueryString() != null ? "&" + request.getQueryString() : "")
                    ,
//                    displayedValue + " (" + value.getCount() + ")");
                    displayedValue,
                    String.valueOf(value.getCount()));
        }
    }
/*
    private String getNextPageURL(Request request) {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(FACET_FIELD, getParameterField());
        int offSet = Util.getIntParameter(request, "offset");
        if (offSet == -1) {
            offSet = 0;
        }

        parameters.put(OFFSET, String.valueOf(offSet + DEFAULT_PAGE_SIZE));

        // Add the filter queries
        String url = generateURL(BROWSE_URL_BASE, parameters);
        String[] fqs = getParameterFacetQueries();
        if (fqs != null) {
            for (String fq : fqs) {
                url += "&fq=" + fq;
            }
        }

        return url;
    }
*/
/*
    private String getPreviousPageURL(Request request) {
        //If our offset should be 0 then we shouldn't be able to view a previous page url

        if ("0".equals(queryArgs.get(FacetParams.FACET_OFFSET)) && Util.getIntParameter(request, "offset") == -1) {
            return null;
        }

        int offset = Util.getIntParameter(request, "offset");
        if (offset == -1 || offset == 0) {
            return null;
        }

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(FACET_FIELD, getParameterField());
        parameters.put(OFFSET, String.valueOf(offset - DEFAULT_PAGE_SIZE));

        // Add the filter queries
        String url = generateURL(BROWSE_URL_BASE, parameters);
        String[] fqs = getParameterFacetQueries();
        if (fqs != null) {
            for (String fq : fqs) {
                url += "&fq=" + fq;
            }
        }

        return url;
    }
*/
    /**
     * Generate a url to the simple search url.
     */
    protected String generateURL(Map<String, String> parameters)
            throws UIException {
//        String query = getQuery();
//        if (!"".equals(query)) {
//            parameters.put("query", URLEncode(query));
//        }
        parameters.put(FACET_FIELD, getParameterField());

        if (parameters.get(PAGE) == null) {
            parameters.put(PAGE, String.valueOf(getParameterPage()));
        }

        if (parameters.get(VIEW) == null) {
            parameters.put(VIEW, String.valueOf(getParameterView()));
        }

        if (parameters.get(RESULTS_PER_PAGE) == null) {
            parameters.put(RESULTS_PER_PAGE, String.valueOf(getParameterRpp()));
        }

//        if (parameters.get("group_by") == null) {
//            parameters.put("group_by", String.valueOf(this.getParameterGroup()));
//        }

        if (parameters.get(SORT_BY) == null) {
            parameters.put(SORT_BY, String.valueOf(getParameterSortBy()));
        }

        if (parameters.get(ORDER) == null) {
            parameters.put(ORDER, String.valueOf(getParameterOrder()));
        }

        if (parameters.get(STARTS_WITH) == null) {
            parameters.put(STARTS_WITH, String.valueOf(getParameterStartsWith()));
        }

        // Add the filter queries
        String url = super.generateURL(BROWSE_URL_BASE, parameters);
        String[] fqs = getParameterFacetQueries();
        if (fqs != null) {
            for (String fq : fqs) {
                url += "&fq=" + fq;
            }
        }

        return url;
    }

    /**
     * Makes the jump-list navigation for the results
     *
     * @param div
     * @param info
     * @param params
     * @throws WingException
     */
    private void addBrowseJumpNavigation(Division div, FacetField field, Request request)
            throws WingException
    {
        // Get the name of the index
        String type = field.getName();

        // Prepare a Map of query parameters required for all links
//        Map<String, String> queryParamsGET = new HashMap<String, String>();
//        queryParamsGET.putAll(params.getCommonParametersEncoded());
//        queryParamsGET.putAll(params.getControlParameters());
//
//        Map<String, String> queryParamsPOST = new HashMap<String, String>();
//        queryParamsPOST.putAll(params.getCommonParameters());
//        queryParamsPOST.putAll(params.getControlParameters());

        // Navigation aid (really this is a poor version of pagination)
        Division jump = div.addInteractiveDivision("browse-navigation", BROWSE_URL_BASE,
                Division.METHOD_POST, "secondary navigation");

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(FACET_FIELD, getParameterField());
        //parameters.put(OFFSET, "0");
        parameters.put(PAGE, Integer.toString(getParameterPage()));

        parameters.put(SORT_BY, getParameterSortBy());
        parameters.put(ORDER, getParameterOrder().toString());
        parameters.put(RESULTS_PER_PAGE, Integer.toString(getParameterRpp()));
        parameters.put(VIEW, getParameterView());

        // Add all the query parameters as hidden fields on the form
        for (String key : parameters.keySet())
            jump.addHidden(key).setValue(parameters.get(key));

        
        // Remove 'page' parameter from jump list
        parameters.remove(PAGE);
        
        // If this is a date based browse, render the date navigation
        if (type != null && type.startsWith("date"))
        {
            org.dspace.app.xmlui.wing.element.List jumpList = jump.addList("jump-list", org.dspace.app.xmlui.wing.element.List.TYPE_SIMPLE, "centuries");
            jumpList.setHead(T_jump_century_head);

            for (int i = 2100; i >= 1500; i -= 100)
            {
//                parameters.put(STARTS_WITH, Character.toString(c));
                parameters.put("fq", type+":["+(i-99)+"+TO+"+(i)+"]");
                // Add the filter queries
                String url = generateURL(BROWSE_URL_BASE, parameters);
                String[] fqs = getParameterFacetQueries();
                if (fqs != null) {
                    for (String fq : fqs) {
                        if (!fq.startsWith(type))
                            url += "&fq=" + fq;
                    }
                }
                int cent = (i / 100);
                jumpList.addItemXref(url, T_jump_century.parameterize(cent));
            }

            // Create a free text entry box for the year
            Para jumpForm = jump.addPara();
            jumpForm.addContent(T_jump_year);
            jumpForm.addText(STARTS_WITH).setHelp(T_jump_year_help);

            jumpForm.addButton("submit").setValue(T_go);
        }
        else
        {
            // Create a clickable list of the alphabet
            org.dspace.app.xmlui.wing.element.List jumpList = jump.addList("jump-list", org.dspace.app.xmlui.wing.element.List.TYPE_SIMPLE, "alphabet");

            // browse params for each letter are all the query params
            // WITHOUT the second-stage browse value, and add STARTS_WITH.
//            Map<String, String> letterQuery = new HashMap<String, String>(queryParamsGET);
//            for (String valueKey : BrowseParams.FILTER_VALUE)
//            {
//                letterQuery.remove(valueKey);
//            }
            parameters.put(STARTS_WITH, "0");
            String url = generateURL(BROWSE_URL_BASE, parameters);
            String[] fqs = getParameterFacetQueries();
            if (fqs != null) {
                for (String fq : fqs) {
                    url += "&fq=" + fq;
                }
            }
            jumpList.addItemXref(url, "0-9");
            //jumpList.addItemXref(super.generateURL(BROWSE_URL_BASE, letterQuery), "0-9");

            for (char c = 'A'; c <= 'Z'; c++)
            {
                parameters.put(STARTS_WITH, Character.toString(c));
                // Add the filter queries
                url = generateURL(BROWSE_URL_BASE, parameters);
                fqs = getParameterFacetQueries();
                if (fqs != null) {
                    for (String fq : fqs) {
                        url += "&fq=" + fq;
                    }
                }
                jumpList.addItemXref(url, Character.toString(c));
            }

            // Create a free text field for the initial characters
            Para jumpForm = jump.addPara();
            jumpForm.addContent(T_starts_with);
            jumpForm.addText(STARTS_WITH).setHelp(T_starts_with_help);

            jumpForm.addButton("submit").setValue(T_go);
        }

    }


    /**
     * Add the controls to changing sorting and display options.
     *
     * @param div
     * @param info
     * @param params
     * @throws WingException
     */
    private void addBrowseControls(Division div, FacetField field, Request request)
            throws WingException
    {
        // Prepare a Map of query parameters required for all links
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(FACET_FIELD, getParameterField());
        //parameters.put(OFFSET, getParameterOffset());

        Division controlsDiv = div.addInteractiveDivision("browse-controls",
                BROWSE_URL_BASE, Division.METHOD_GET, "secondary search");

        for (String par : parameters.keySet())
            controlsDiv.addHidden(par).setValue(parameters.get(par));

        Table controlsTable = controlsDiv.addTable("browse-controls", 1, 3);
        //Table controlsTable = controlsDiv.addTable("browse-controls", 1, 4);
        Row controlsRow = controlsTable.addRow(Row.ROLE_DATA);

        // Create a control for the type of view to display
        Cell viewCell = controlsRow.addCell();
        try {
            viewCell.addContent(T_view);
            Select viewSelect = viewCell.addSelect("view");
            for (String v : VIEW_TYPES) {
                viewSelect.addOption((v.equals(getParameterView())), v, message("xmlui.ArtifactBrowser.AbstractSearch.view." + v));
            }
        }
        catch (Exception e) {
            throw new WingException("Unable to get view options", e);
        }

        // Create a control for the number of records to display
        Cell rppCell = controlsRow.addCell();
        rppCell.addContent(T_rpp);
        Select rppSelect = rppCell.addSelect("rpp");
        for (int i : RESULTS_PER_PAGE_PROGRESSION) {
            rppSelect.addOption((i == getParameterRpp()), i, Integer.toString(i));
        }

        /*
        Cell groupCell = controlsRow.addCell();
        try {
            // Create a drop down of the different sort columns available
            groupCell.addContent(T_group_by);
            Select groupSelect = groupCell.addSelect("group_by");
            groupSelect.addOption(false, "none", T_group_by_none);


            String[] groups = {"publication_grp"};
            for (String group : groups) {
                groupSelect.addOption(group.equals(getParameterGroup()), group,
                        message("xmlui.ArtifactBrowser.AbstractSearch.group_by." + group));
            }

        }
        catch (Exception se) {
            throw new WingException("Unable to get group options", se);
        }
        */

        // Sorting does not make sense in browsing.
//        Cell sortCell = controlsRow.addCell();
//        try {
//            // Create a drop down of the different sort columns available
//            sortCell.addContent(T_sort_by);
//            Select sortSelect = sortCell.addSelect("sort_by");
//            sortSelect.addOption(false, "score", T_sort_by_relevance);
//            for (SortOption so : SortOption.getSortOptions()) {
//                if (so.isVisible()) {
//                    sortSelect.addOption((so.getMetadata().equals(getParameterSortBy())), so.getMetadata(),
//                            message("xmlui.ArtifactBrowser.AbstractSearch.sort_by." + so.getName()));
//                }
//            }
//        }
//        catch (SortException se) {
//            throw new WingException("Unable to get sort options", se);
//        }

        // Create a control to changing ascending / descending order
        Cell orderCell = controlsRow.addCell();
        orderCell.addContent(T_order);
        Select orderSelect = orderCell.addSelect("order");
        orderSelect.addOption(SolrQuery.ORDER.asc.equals(getParameterOrder()), SolrQuery.ORDER.asc.toString(), T_order_asc);
        orderSelect.addOption(SolrQuery.ORDER.desc.equals(getParameterOrder()), SolrQuery.ORDER.desc.toString(), T_order_desc);


        // Create a control for the number of authors per item to display
        // FIXME This is currently disabled, as the supporting functionality
        // is not currently present in xmlui
        //if (isItemBrowse(info))
        //{
        //    controlsForm.addContent(T_etal);
        //    Select etalSelect = controlsForm.addSelect(BrowseParams.ETAL);
        //
        //    etalSelect.addOption((info.getEtAl() < 0), 0, T_etal_all);
        //    etalSelect.addOption(1 == info.getEtAl(), 1, Integer.toString(1));
        //
        //    for (int i = 5; i <= 50; i += 5)
        //    {
        //        etalSelect.addOption(i == info.getEtAl(), i, Integer.toString(i));
        //    }
        //}

//        controlsForm.addButton("update").setValue(T_update);
//        query.addPara(null, "button-list").addButton("submit").setValue(T_go);
        controlsDiv.addPara(null, "button-list").addButton("update").setValue(T_update);

    }


    /**
     * Recycle
     */
    @Override
    public void recycle() {
        // Clear out our item's cache.
        this.queryResults = null;
        this.validity = null;
        super.recycle();
    }

    public String getParameterField() {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String facetField = request.getParameter(FACET_FIELD);
        // There is already a configuration option for this: solr.browse.default.filter.
        // Can't make above option work as desired. Rolling back to the option below.
        if (facetField == null) {
            facetField = SearchUtils.getConfig().getString("solr.facets.default", "dc.title");
        }
        return facetField;
    }

    public String[] getParameterFacetQueries() {
        Request request = ObjectModelHelper.getRequest(objectModel);
        return request.getParameterValues("fq") != null ? request.getParameterValues("fq") : new String[0];
    }

    public String getParameterStartsWith() {
        String s = ObjectModelHelper.getRequest(objectModel).getParameter(STARTS_WITH);
        return s != null ? s : "";
    }

    protected String getParameterView() {
        String s = ObjectModelHelper.getRequest(objectModel).getParameter(VIEW);
        return s != null ? s : VIEW_TYPES[0];
    }

    protected int getParameterPage() {
        try {
            int page = Integer.parseInt(ObjectModelHelper.getRequest(objectModel).getParameter(PAGE));
            return page > 0 ? page : 1;
        }
        catch (Exception e) {
            return 1;
        }
    }

    protected int getParameterRpp() {
        try {
            int rpp = Integer.parseInt(ObjectModelHelper.getRequest(objectModel).getParameter(RESULTS_PER_PAGE));
            return rpp > 0 ? rpp : DEFAULT_PAGE_SIZE;
        }
        catch (Exception e) {
            return DEFAULT_PAGE_SIZE;
        }
    }

    protected String getParameterSortBy() {
        String s = ObjectModelHelper.getRequest(objectModel).getParameter(SORT_BY);
//        return s != null ? s : DEFAULT_SORT_BY;
        return config.getPropertyAsType("solr.browse.sort", DEFAULT_SORT_BY);
    }

//    protected String getParameterGroup() {
//        String s = ObjectModelHelper.getRequest(objectModel).getParameter("group_by");
//        return s != null ? s : "none";
//    }

    protected SolrQuery.ORDER getParameterOrder() {
        String s = ObjectModelHelper.getRequest(objectModel).getParameter(ORDER);
        return s != null ? SolrQuery.ORDER.valueOf(s.toLowerCase()) : DEFAULT_ORDER;
    }

}
