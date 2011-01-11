/**
 * $Id: $
 * $URL: $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.app.xmlui.aspect.discovery;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.FacetParams;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.SolrServiceImpl;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map;
import java.util.TreeMap;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.Constants;

/**
 * User: @author kevinvandevelde (kevin at atmire.com)
 * Date: 17-feb-2010
 * Time: 14:45:25
 */
public abstract class AbstractFiltersTransformer extends AbstractDSpaceTransformer {


    private static final Logger log = Logger.getLogger(AbstractFiltersTransformer.class);


    /**
     * Cached query results
     */
    protected QueryResponse queryResults;

    protected QueryResponse queryResultsAllScope;

    /**
     * Cached query arguments
     */
    protected SolrQuery queryArgs;

    protected SolrQuery queryArgsAllScope;


    /**
     * Cached validity object
     */
    protected SourceValidity validity;

    protected SearchService getSearchService()
    {
        DSpace dspace = new DSpace();
        
        org.dspace.kernel.ServiceManager manager = dspace.getServiceManager() ;

        return manager.getServiceByName(SearchService.class.getName(),SearchService.class);
    }

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        try {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

            if (dso == null)
                return "0";

            return HashUtil.hash(dso.getHandle());
        }
        catch (SQLException sqle) {
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
    public SourceValidity getValidity() {
        if (this.validity == null) {

            try {
                DSpaceObject dso = HandleUtil.obtainHandle(objectModel);


                DSpaceValidity val = new DSpaceValidity();

                // add reciently submitted items, serialize solr query contents.
                performSearch(dso);

                // Add the actual collection;
                if (dso != null)
                    val.add(dso);

                val.add("numFound:" + queryResults.getResults().getNumFound());

                for (SolrDocument doc : queryResults.getResults()) {
                    val.add(doc.toString());
                }

//                for (SolrDocument doc : queryResults.getResults()) {
//                    val.add(doc.toString());
//                }

                for (FacetField field : queryResults.getFacetFields()) {
                    val.add(field.getName());

                    for (FacetField.Count count : field.getValues()) {
                        val.add(count.getName() + count.getCount());
                    }
                }


                this.validity = val.complete();
            }
            catch (Exception e) {
                log.error(e.getMessage(),e);
            }

            //TODO: dependent on tags as well :)
        }
        return this.validity;
    }


    public abstract void performSearch(DSpaceObject object) throws SearchServiceException, UIException;


    /** Allowed Query Parameters for Search Enabled View */
    protected String[] getFacetsList() {

        String[] list = null;

        try {
            list = ObjectModelHelper.getRequest(objectModel).getParameterValues("fl");
        }
        catch (Exception e) {
            return new String[0];
        }

        return list == null ? new String[0] : list;
    }

    protected SolrQuery prepareDefaultFilters(String scope) {

        queryArgs = new SolrQuery();


        /* Get Dynamic list of Facets off Request *
        for(String facet : getFacetsList())
        {
            queryArgs.addFacetField(facet);
            int max = ConfigurationManager.getIntProperty("search.facet.max", 10) + 1;
            //Set the default limit to 11
            queryArgs.setFacetLimit(max);
            queryArgs.setFacetMinCount(1);
            queryArgs.setFacet(true);
        }                                     */


        String[] facets = SearchUtils.getFacetsForType(scope);
        String[] dateFacets = SearchUtils.getDateFacetsForType(scope);

        log.debug("facets for scope '" + scope + "': " + (facets != null ? facets.length : null));
        log.debug("date facets for scope, " + scope + ": " + (dateFacets != null ? dateFacets.length : null));


        if (facets != null || dateFacets != null) {
            int max = ConfigurationManager.getIntProperty("search.facet.max", 10) + 1;
            //Set the default limit to 11
            queryArgs.setFacetLimit(max);
            queryArgs.setFacetMinCount(1);
            queryArgs.setFacet(true);
        }

        /** enable faceting of search results */
        if (facets != null)
            queryArgs.addFacetField(facets);

        /** add any dateFacets configured */
        if (dateFacets != null && 0 < dateFacets.length) {
            queryArgs.setParam(FacetParams.FACET_DATE, dateFacets);
            queryArgs.setParam(FacetParams.FACET_DATE_GAP, "+1YEAR");
            queryArgs.setParam(FacetParams.FACET_DATE_START, "NOW/YEAR-" + SearchUtils.getConfig().getString("solr.date.gap", "10") + "YEARS");
            queryArgs.setParam(FacetParams.FACET_DATE_END, "NOW");
            queryArgs.setParam(FacetParams.FACET_DATE_OTHER, FacetParams.FacetDateOther.ALL.toString());
        }

        //Add the default filters
        queryArgs.addFilterQuery(SearchUtils.getDefaultFilters(scope));

        return queryArgs;
    }



    @Override
    public void addOptions(Options options) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        Request request = ObjectModelHelper.getRequest(objectModel);

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        java.util.List fqs = Arrays.asList(
                request.getParameterValues("fq") != null ? request.getParameterValues("fq") : new String[0]);

        if (this.queryResults != null) {
            java.util.List<FacetField> facetFields = this.queryResults.getFacetFields();
            if (facetFields == null)
                facetFields = new ArrayList<FacetField>();

            if(queryResults.getFacetDates() != null)
                facetFields.addAll(this.queryResults.getFacetDates());

            if (facetFields.size() > 0) {
                addDiscoveryLocation(options, request, fqs, dso);

                ////////////
                List browse = options.addList("discovery");
                boolean firstSubList = true;

                for (FacetField field : facetFields) {

                    // Skip location filter, since this is handled above.
                    if (field.getName().equals("location")) {
                        continue;
                    }

                    java.util.List<FacetField.Count> values = field.getValues();

                    //This is needed for a dirty hack to make sure that the date filters do not remain empty
                    boolean valueAdded = false;
                    if (values != null) {

                        if (firstSubList) {
                            browse.setHead(message("xmlui.ArtifactBrowser.AdvancedSearch.filter_by.head"));
                            firstSubList = false;
                        }
                        List facet = browse.addList(field.getName());

                        facet.setHead(message("xmlui.ArtifactBrowser.AdvancedSearch.type_" + field.getName().replace("_lc", "").replace("_dt", "")));

                        Iterator<FacetField.Count> iter = values.iterator();
                        for (int i = 0; i < this.queryArgs.getFacetLimit(); i++) {
                            if (!iter.hasNext())
                                break;
                            FacetField.Count value = iter.next();

                            if (i < this.queryArgs.getFacetLimit() - 1) {
                                String displayedValue = value.getName();
                                String filterQuery = value.getAsFilterQuery();
                                String itemName = null;
                                String itemRend = null;
                                if (field.getName().equals("location.comm") || field.getName().equals("location.coll")) {
                                    //We have a community/collection, resolve it to a dspaceObject
//                                    displayedValue = SolrServiceImpl.locationToName(context, field.getName(), displayedValue);
                                    int type = field.getName().equals("location.comm") ? Constants.COMMUNITY : Constants.COLLECTION;
                                    DSpaceObject commColl = DSpaceObject.find(context, type, Integer.parseInt(displayedValue));
                                    if (commColl != null) {
                                        displayedValue = commColl.getName();
                                        itemName = commColl.getHandle();
                                    }
                                }
                                if (field.getGap() != null) {
                                    //We have a date field
                                    //Since we currently only support years, get the year
                                    //PS: date looks something like this: 2005-01-01T00:00:00Z
                                    displayedValue = displayedValue.split("-")[0];
                                    filterQuery = ClientUtils.escapeQueryChars(value.getFacetField().getName()) + ":" + displayedValue + "*";
                                    // What we do now is, if we have a date and we have selected one as a filter query, do not show the other values
                                    // There is no need to this since we can only have one date at a time
                                    // What we do show however is the current date.
                                    boolean skipValue = false;
                                    for (Object fq1 : fqs) {
                                        String fq = (String) fq1;
                                        if (fq.startsWith(value.getFacetField().getName() + ":") && !fq.equals(filterQuery))
                                            skipValue = true;
                                    }
                                    //Skipt these vals, there are not relevant, they are required to indicate if we need a view more url
                                    if (displayedValue.equals(FacetParams.FacetDateOther.AFTER.toString())
                                            || displayedValue.equals(FacetParams.FacetDateOther.BEFORE.toString())
                                            || displayedValue.equals(FacetParams.FacetDateOther.BETWEEN.toString()))
                                        skipValue = true;

                                    if(value.getCount() == 0)
                                        skipValue = ConfigurationManager.getBooleanProperty("solr.date.skip.empty", true);

                                    if (skipValue)
                                        continue;
                                }

                                if (fqs.contains(filterQuery)) {
                                    valueAdded = true;
                                    facet.addItem(Math.random() + "", "selected").addContent(displayedValue + " (" + value.getCount() + ")");
                                } else {
                                    valueAdded = true;
                                    facet.addItem(itemName, itemRend).addXref(
                                            contextPath +
                                                    (dso == null ? "" : "/handle/" + dso.getHandle()) +
                                                    "/search?" +
                                                    request.getQueryString() +
                                                    "&fq=" +
                                                    URLEncoder.encode(filterQuery, "UTF-8"),
                                            displayedValue + " (" + value.getCount() + ")" // + "TESTE"
                                    );
                                }
                            }
                            if (i == this.queryArgs.getFacetLimit() - 1 && field.getGap() == null) {

                                addViewMoreUrl(facet, dso, request, field.getName());
                            }
                        }
                        if (field.getGap() != null) {
                            if (!valueAdded) {
                                /** THIS IS A DIRTY HACK TO MAKE SURE WE DO NOT HAVE AN EMPTY DATE FILTER
                                 * THIS WILL NOT WORK IF THE DATE FIELD SHOULD BE REPEATABLE ! **/
                                /** Add a field using this content **/
                                /** Locate the value in the filter **/
                                for (Object fq1 : fqs) {
                                    String fq = (String) fq1;
                                    if (fq.startsWith(field.getName() + ":")) {
                                        String valShown = fq.substring(fq.indexOf(":") + 1);
                                        //Remove the * at the end
                                        valShown = valShown.substring(0, valShown.length() - 1);
                                        facet.addItem().addContent(valShown + " (" + queryResults.getResults().getNumFound() + ")");
                                    }
                                }
                            }


                            // We have a date check if we need a view more
                            // We need a view more if we have values that come after OR before our shown dates
                            boolean showMoreUrl = false;
                            for (FacetField.Count facetValue : values) {
                                if (facetValue.getName().equals(FacetParams.FacetDateOther.AFTER.toString()) || facetValue.getName().equals(FacetParams.FacetDateOther.BEFORE.toString().toString()))
                                    if (0 < facetValue.getCount())
                                        showMoreUrl = true;
                            }
                            // Add the _dt postfix to make sure that our browse is shown as a date
                            if (showMoreUrl)
                                addViewMoreUrl(facet, dso, request, field.getName() + "_dt");
                        }
                        // If we have no value added
                    }
                }
            }
        }
    }

    /*
     * Top level locations (communities and collections).
     */
    private void addDiscoveryLocation(Options options, Request request, java.util.List fqs, DSpaceObject dso) throws NumberFormatException, SQLException, WingException {
        // UPDATE: listing all locations returned by Solr is not what we want.
        // We just want the selected comm/coll, its children/sibling collections, parent community, children community.
        // FIXME: probably encapsulate in a new method and allow for a depth parameter like community-list.
        List browseCommColl = options.addList("discovery-location");
        browseCommColl.setHead(message("xmlui.ArtifactBrowser.AdvancedSearch.filter_by.head"));
        if (this.queryResults != null) {
            FacetField fieldLocation = this.queryResults.getFacetField("location");
            if (fieldLocation != null) {
                java.util.List<FacetField.Count> values = fieldLocation.getValues();
                if (values != null) {
                    String queryString = request.getQueryString();
                    queryString = queryString != null ? queryString.replaceAll("&fq=location[:[%3A]+][ml][0-9]+", "") : queryString;
                    // Find selected community/collection.
                    String curCommCollSelectedName = "m1"; // Default
                    for (FacetField.Count v : values) {
                        if (fqs.contains(v.getAsFilterQuery())) {
                            curCommCollSelectedName = v.getName();
                            break;
                        }
                        int type = v.getName().startsWith("m") ? Constants.COMMUNITY : Constants.COLLECTION;
                        DSpaceObject commColl = DSpaceObject.find(context, type, Integer.parseInt(v.getName().substring(1)));
                        if (dso != null && commColl != null && dso.getHandle().equals(commColl.getHandle())) {
                            curCommCollSelectedName = v.getName();
                            break;
                        }
                    }
                    // UPDATE: see UPDATE above.
                    // Build list of locations (communities/collections) ordered by handle part after prefix.
                    Map<String, FacetField.Count> sortedSolrNameLocationWithValues = new TreeMap<String, FacetField.Count>();
                    for (FacetField.Count v : values) {
                        sortedSolrNameLocationWithValues.put(v.getName(), v);
                    }
                    // Default selection is "m1" (community with ID 1).
                    int curCommCollSelectedType = Constants.COMMUNITY;
                    int curCommCollSelectedID = 1;
                    if (curCommCollSelectedName != null) {
                        curCommCollSelectedType = curCommCollSelectedName.startsWith("m") ? Constants.COMMUNITY : Constants.COLLECTION;
                        curCommCollSelectedID = Integer.parseInt(curCommCollSelectedName.substring(1));
                    }
                    Map<Integer, String> sortedHandleWithSolrName = new TreeMap<Integer, String>();
                    Map<String, DSpaceObject> sortedSolrNameWithDSpaceObject = new TreeMap<String, DSpaceObject>();
                    
                    int topCommID = 1;
                    String topCommName = "m1";
                    boolean curSelectedIsTop = curCommCollSelectedName.equals(topCommName) ? true : false;
                    
                    Community mtop = Community.find(context, topCommID);
                    sortedHandleWithSolrName.put(Integer.parseInt(mtop.getHandle().split("/")[1]), topCommName);
                    sortedSolrNameWithDSpaceObject.put(topCommName, mtop);

                    Collection[] collections = mtop.getCollections();
                    for (Collection l : collections) {
                        String solrName = "l" + l.getID();
                        if (solrName.equals(curCommCollSelectedName))
                            curSelectedIsTop = true;
                        sortedHandleWithSolrName.put(Integer.parseInt(l.getHandle().split("/")[1]), solrName);
                        sortedSolrNameWithDSpaceObject.put(solrName, l);
                    }

                    Community[] communities = mtop.getSubcommunities();
                    for (Community mm : communities) {
                        String solrName = "m" + mm.getID();
                        if (solrName.equals(curCommCollSelectedName))
                            curSelectedIsTop = true;
                        sortedHandleWithSolrName.put(Integer.parseInt(mm.getHandle().split("/")[1]), solrName);
                        sortedSolrNameWithDSpaceObject.put(solrName, mm);
                    }
                    if (curSelectedIsTop == false) {
                        if (curCommCollSelectedType == Constants.COMMUNITY) {
                            Community msel = Community.find(context, curCommCollSelectedID);
                            Community[] parents = msel.getAllParents();
                            if (parents.length < 2) {
                                log.error("Something is wrong: this should not happen.");
                            }
                            Community topParent = parents[parents.length - 2];
                            curCommCollSelectedName = "m" + topParent.getID();
                        } else {
                            // Supposing Constants.COLLECTION
                            Collection lsel = Collection.find(context, curCommCollSelectedID);
                            Community firstParent = lsel.getCommunities()[0];
                            Community[] parents = firstParent.getAllParents();
                            if (parents.length <= 1) {
                                log.error("Something is wrong: this should not happen.");
                            }
                            Community topParent = parents[parents.length - 2];
                            curCommCollSelectedName = "m" + topParent.getID();
                        }
                    }
                    //                            Iterator<FacetField.Count> iter = sortedValues.values().iterator();
                    Iterator<String> iter = sortedHandleWithSolrName.values().iterator();
                    List facet = browseCommColl.addList(fieldLocation.getName());
                    //                            facet.setHead(message("xmlui.ArtifactBrowser.AdvancedSearch.type_" + fieldLocation.getName()));
                    facet.setHead(queryString);
                    for (int i = 0; i < this.queryArgs.getFacetLimit(); i++) {
                        if (!iter.hasNext()) {
                            break;
                        } //                                String displayedValue = value.getName();
                        String solrName = iter.next();
                        DSpaceObject commColl = sortedSolrNameWithDSpaceObject.get(solrName);
                        FacetField.Count value = sortedSolrNameLocationWithValues.get(solrName);
                        String displayedValue = commColl.getName();
                        String itemName = commColl.getHandle();
                        String itemRend = null;
                        //We have a community/collection, resolve it to a dspaceObject
                        //                                    displayedValue = SolrServiceImpl.locationToName(context, field.getName(), displayedValue);
                        //                                int type = fieldLocation.getName().equals("location.comm") ? Constants.COMMUNITY : Constants.COLLECTION;
                        if (curCommCollSelectedName != null && curCommCollSelectedName.equals(value.getName())) {
                            facet.addItem(itemName, "selected").addContent(displayedValue + " (" + value.getCount() + ")");
                        } else if (curCommCollSelectedName == null && value.getName().equals("m1")) {
                            facet.addItem(itemName, "selected").addContent(displayedValue + " (" + value.getCount() + ")");
                        } else {
                            if (value.getCount() > 0) {
                                String URI = null;
                                try {
                                    URI = request.getSitemapURI().split("/")[request.getSitemapURI().split("/").length - 1];
                                    if (URI.matches("[0-9]+")) {
                                        URI = null;
                                    }
                                } catch (Exception e) {
                                    log.error(e.getMessage(), e);
                                }
                                facet.addItem(itemName, itemRend).addXref(contextPath + (commColl == null ? "" : "/handle/" + commColl.getHandle()) + "/" + (URI != null ? URI : "") + (queryString != null ? "?" + queryString : ""), displayedValue + " (" + "" + value.getCount() + ")");
                            } else {
                                facet.addItem(itemName, "disabled").addContent(displayedValue + " (" + value.getCount() + ")");
                            }
                        }
                    }
                }
            }
        }
    }

    /*
     * This version of the method is contextual: retrieves comm/coll relative to currently selected comm/coll.
     */
    /*
    private void addDiscoveryLocation(Options options, Request request, java.util.List fqs, DSpaceObject dso) throws NumberFormatException, SQLException, WingException {
        ////////////
        // UPDATE: listing all locations returned by Solr is not what we want.
        // We just want the selected comm/coll, its children/sibling collections, parent community, children community.
        // FIXME: probably encapsulate in a new method and allow for a depth parameter like community-list.
        List browseCommColl = options.addList("discovery-location");
        browseCommColl.setHead(message("xmlui.ArtifactBrowser.AdvancedSearch.filter_by.head"));
        if (this.queryResults != null) {
            FacetField fieldLocation = this.queryResults.getFacetField("location");
            if (fieldLocation != null) {
                java.util.List<FacetField.Count> values = fieldLocation.getValues();
                if (values != null) {
                    String queryString = request.getQueryString();
                    queryString = queryString != null ? queryString.replaceAll("&fq=location[:[%3A]+][ml][0-9]+", "") : queryString;
                    // Find selected community/collection.
                    String curCommCollSelectedName = "m1"; // Default
                    for (FacetField.Count v : values) {
                        if (fqs.contains(v.getAsFilterQuery())) {
                            curCommCollSelectedName = v.getName();
                            break;
                        }
                        int type = v.getName().startsWith("m") ? Constants.COMMUNITY : Constants.COLLECTION;
                        DSpaceObject commColl = DSpaceObject.find(context, type, Integer.parseInt(v.getName().substring(1)));
                        if (dso != null && commColl != null && dso.getHandle().equals(commColl.getHandle())) {
                            curCommCollSelectedName = v.getName();
                            break;
                        }
                    }
                    // UPDATE: see UPDATE above.
                    // Build list of locations (communities/collections) ordered by handle part after prefix.
//                    Map<Integer, FacetField.Count> sortedValues = new TreeMap<Integer, FacetField.Count>();
//                    for (FacetField.Count v : values) {
//                    int type = v.getName().startsWith("m") ? Constants.COMMUNITY : Constants.COLLECTION;
//                    DSpaceObject commColl = DSpaceObject.find(context, type, Integer.parseInt(v.getName().substring(1)));
//                    if (commColl != null) {
//                    sortedValues.put(Integer.parseInt(commColl.getHandle().split("/")[1]), v);
//                    } else {
//                    sortedValues.put((int)(9999 + Math.ceil(100*Math.random())), v);
//                    }
//                    }
                    Map<String, FacetField.Count> sortedSolrNameLocationWithValues = new TreeMap<String, FacetField.Count>();
                    for (FacetField.Count v : values) {
                        sortedSolrNameLocationWithValues.put(v.getName(), v);
                    }
                    // Default selection is "m1" (community with ID 1).
                    int curCommCollSelectedType = Constants.COMMUNITY;
                    int curCommCollSelectedID = 1;
                    if (curCommCollSelectedName != null) {
                        curCommCollSelectedType = curCommCollSelectedName.startsWith("m") ? Constants.COMMUNITY : Constants.COLLECTION;
                        curCommCollSelectedID = Integer.parseInt(curCommCollSelectedName.substring(1));
                    }
                    Map<Integer, String> sortedHandleWithSolrName = new TreeMap<Integer, String>();
                    Map<String, DSpaceObject> sortedSolrNameWithDSpaceObject = new TreeMap<String, DSpaceObject>();
                    if (curCommCollSelectedType == Constants.COMMUNITY) {
                        // If selected location is a community, we want:
                        // - Itself;
                        // - It's child collections;
                        // - It's first level child communities;
                        // - It's parent community, if any.
                        Community m = Community.find(context, curCommCollSelectedID);
                        if (m != null) {
                            sortedHandleWithSolrName.put(Integer.parseInt(m.getHandle().split("/")[1]), curCommCollSelectedName);
                            sortedSolrNameWithDSpaceObject.put(curCommCollSelectedName, m);
                            Collection[] collections = m.getCollections();
                            for (Collection l : collections) {
                                String solrName = "l" + l.getID();
                                sortedHandleWithSolrName.put(Integer.parseInt(l.getHandle().split("/")[1]), solrName);
                                sortedSolrNameWithDSpaceObject.put(solrName, l);
                            }
                            Community[] communities = m.getSubcommunities();
                            for (Community mm : communities) {
                                String solrName = "m" + mm.getID();
                                sortedHandleWithSolrName.put(Integer.parseInt(mm.getHandle().split("/")[1]), solrName);
                                sortedSolrNameWithDSpaceObject.put(solrName, mm);
                            }
                            Community mp = m.getParentCommunity();
                            if (mp != null) {
                                String solrName = "m" + mp.getID();
                                sortedHandleWithSolrName.put(Integer.parseInt(mp.getHandle().split("/")[1]), solrName);
                                sortedSolrNameWithDSpaceObject.put(solrName, mp);
                            }
                        } else {
                            log.error("Invalid community id: " + curCommCollSelectedID);
                        }
                    } else {
                        // If selected location is a collection, we want:
                        // - Itself;
                        // - It's *first* parent community (other parents are ignored);
                        // - It's sibling collections;
                        // - It's sibling communities.
                        Collection l = Collection.find(context, curCommCollSelectedID);
                        if (l != null) {
                            // Probably unnecessary, since it's parent children includes it.
                            //sortedHandleName.put(Integer.parseInt(l.getHandle().split("/")[1]), curCommCollSelectedName);
                            Community mp = l.getCommunities()[0];
                            if (mp == null) {
                                mp = Community.find(context, 1);
                            }
                            sortedHandleWithSolrName.put(Integer.parseInt(mp.getHandle().split("/")[1]), "m" + mp.getID());
                            sortedSolrNameWithDSpaceObject.put("m" + mp.getID(), mp);
                            Collection[] collections = mp.getCollections();
                            for (Collection ll : collections) {
                                String solrName = "l" + ll.getID();
                                sortedHandleWithSolrName.put(Integer.parseInt(ll.getHandle().split("/")[1]), solrName);
                                sortedSolrNameWithDSpaceObject.put(solrName, ll);
                            }
                            Community[] communities = mp.getSubcommunities();
                            for (Community mm : communities) {
                                String solrName = "m" + mm.getID();
                                sortedHandleWithSolrName.put(Integer.parseInt(mm.getHandle().split("/")[1]), solrName);
                                sortedSolrNameWithDSpaceObject.put(solrName, mm);
                            }
                        } else {
                            log.error("Invalid community id: " + curCommCollSelectedID);
                        }
                    }
                    //                            Iterator<FacetField.Count> iter = sortedValues.values().iterator();
                    Iterator<String> iter = sortedHandleWithSolrName.values().iterator();
                    List facet = browseCommColl.addList(fieldLocation.getName());
                    //                            facet.setHead(message("xmlui.ArtifactBrowser.AdvancedSearch.type_" + fieldLocation.getName()));
                    facet.setHead(queryString);
                    for (int i = 0; i < this.queryArgs.getFacetLimit(); i++) {
                        if (!iter.hasNext()) {
                            break;
                        } //                                String displayedValue = value.getName();
                        String solrName = iter.next();
                        DSpaceObject commColl = sortedSolrNameWithDSpaceObject.get(solrName);
                        FacetField.Count value = sortedSolrNameLocationWithValues.get(solrName);
                        String displayedValue = commColl.getName();
                        String itemName = commColl.getHandle();
                        String itemRend = null;
                        //We have a community/collection, resolve it to a dspaceObject
                        //                                    displayedValue = SolrServiceImpl.locationToName(context, field.getName(), displayedValue);
                        //                                int type = fieldLocation.getName().equals("location.comm") ? Constants.COMMUNITY : Constants.COLLECTION;
                        if (curCommCollSelectedName != null && curCommCollSelectedName.equals(value.getName())) {
                            facet.addItem(itemName, "selected").addContent(displayedValue + " (" + value.getCount() + ")");
                        } else if (curCommCollSelectedName == null && value.getName().equals("m1")) {
                            facet.addItem(itemName, "selected").addContent(displayedValue + " (" + value.getCount() + ")");
                        } else {
                            if (value.getCount() > 0) {
                                String URI = null;
                                try {
                                    URI = request.getSitemapURI().split("/")[request.getSitemapURI().split("/").length - 1];
                                    if (URI.matches("[0-9]+")) {
                                        URI = null;
                                    }
                                } catch (Exception e) {
                                    log.error(e.getMessage(), e);
                                }
                                facet.addItem(itemName, itemRend).addXref(contextPath + (commColl == null ? "" : "/handle/" + commColl.getHandle()) + "/" + (URI != null ? URI : "") + (queryString != null ? "?" + queryString : ""), displayedValue + " (" + "" + value.getCount() + ")");
                            } else {
                                facet.addItem(itemName, "disabled").addContent(displayedValue + " (" + value.getCount() + ")");
                            }
                        }
                    }
                }
            }
        }
    }
*/
    private void addViewMoreUrl(List facet, DSpaceObject dso, Request request, String fieldName) throws WingException {
        facet.addItem(null, "view-more").addXref(
                contextPath +
                        (dso == null ? "" : "/handle/" + dso.getHandle()) +
                        "/browse?" + BrowseFacet.FACET_FIELD + "=" + fieldName +
                        (request.getQueryString() != null ? "&" + request.getQueryString() : ""),
                message("xmlui.ArtifactBrowser.AdvancedSearch.view_more")
        );
    }

    @Override
    public void recycle() {
        queryResults = null;
        queryArgs = null;
    }
}
