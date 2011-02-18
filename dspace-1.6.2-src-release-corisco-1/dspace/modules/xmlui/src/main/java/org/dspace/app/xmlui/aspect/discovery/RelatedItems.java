/**
 * $Id: RelatedItems.java 5171 2010-07-02 18:20:52Z KevinVandeVelde $
 * $URL: http://scm.dspace.org/svn/repo/modules/dspace-discovery/trunk/block/src/main/java/org/dspace/app/xmlui/aspect/discovery/RelatedItems.java $
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
import java.util.*;
import java.util.List;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Community;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.LogManager;
import org.dspace.discovery.SearchUtils;
import org.xml.sax.SAXException;
import org.dspace.discovery.SearchServiceException;

/**
 * Display a single item.
 * 
 * @author Mark Diggory
 */
public class RelatedItems extends AbstractFiltersTransformer
{

    private static final Logger log = Logger.getLogger(RelatedItems.class);
    
    /** Language strings */
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");

    private static final Message T_trail =
        message("xmlui.ArtifactBrowser.ItemViewer.trail");

    private static final Message T_show_simple =
        message("xmlui.ArtifactBrowser.ItemViewer.show_simple");

    private static final Message T_show_full =
        message("xmlui.ArtifactBrowser.ItemViewer.show_full");

    private static final Message T_head_parent_collections =
        message("xmlui.ArtifactBrowser.ItemViewer.head_parent_collections");

    /**
     * The cache of recently submitted items
     */
    protected QueryResponse queryResults;

    /**
     * Cached query arguments
     */
    protected SolrQuery queryArgs;

    /**
     * Display a single item
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        DSpaceObject dspaceObject = HandleUtil.obtainHandle(objectModel);
        if (!(dspaceObject instanceof Item))
            return;
        Item item = (Item) dspaceObject;

        try {
            performSearch(item);
        } catch (SearchServiceException e) {
            log.error(e.getMessage(),e);
        }

        // Build the collection viewer division.


        if (this.queryResults != null) {

            NamedList nList = this.queryResults.getResponse();

            SimpleOrderedMap<SolrDocumentList> mlt = (SimpleOrderedMap<SolrDocumentList>)nList.get("moreLikeThis");

            //home.addPara(nList.toString());
            
            if(mlt != null && 0 < mlt.size())
            {
                //TODO: also make sure if an item is unresolved we do not end up with an empty referenceset !
                List<DSpaceObject> dsos = new ArrayList<DSpaceObject>();
                for(Map.Entry<String,SolrDocumentList> entry : mlt)
                {
                    String key = entry.getKey();

                    //org.dspace.app.xmlui.wing.element.List mltList = mltDiv.addList(key);

                    //mltList.setHead(key);

                    for(SolrDocument doc : entry.getValue())
                    {
                        try{
                            dsos.add(SearchUtils.findDSpaceObject(context, doc));
                        }catch(Exception e){
                            log.error(LogManager.getHeader(context, "Error while resolving related item doc to dso", "Main item: " + item.getID()));
                        }
                        //mltList.addItem().addContent(doc.toString());
                    }
         

                }

                if(0 < dsos.size()){
                    Division home = body.addDivision("test", "secondary related");

                    String name = "Related Items";

                    //if (name == null || name.length() == 0)
                    //	home.setHead(T_untitled);
                    //else
                        home.setHead(name);

                    Division mltDiv = home.addDivision("item-related", "secondary related");

                    mltDiv.setHead("Items By Author:");

                    ReferenceSet set = mltDiv.addReferenceSet(
                            "item-related-items", ReferenceSet.TYPE_SUMMARY_LIST,
                            null, "related-items");

                    for (DSpaceObject dso : dsos) {
                        Reference ref = set.addReference(dso);
                    }
                }
            }

            }
        }

    @Override
    public void performSearch(DSpaceObject dso) throws SearchServiceException {

        if(queryResults != null)
            return;
        

        this.queryArgs = new SolrQuery();
        this.queryArgs.setRows(1);
        this.queryArgs.add("fl","author,handle");
        this.queryArgs.add("mlt","true");
        this.queryArgs.add("mlt.fl","author,handle");
        this.queryArgs.add("mlt.mindf","1");
        this.queryArgs.add("mlt.mintf","1");
        this.queryArgs.setQuery("handle:" + dso.getHandle());
        this.queryArgs.setRows(1);

        queryResults = getSearchService().search(queryArgs);

    }

    /**
     * Recycle
     */
    public void recycle() {
        this.queryArgs = null;
        this.queryResults = null;
    	super.recycle();
    }
}
