/**
 * $Id: ItemFacets.java 4845 2010-04-05 01:05:48Z mdiggory $
 * $URL: http://scm.dspace.org/svn/repo/modules/dspace-discovery/trunk/block/src/main/java/org/dspace/app/xmlui/aspect/discovery/ItemFacets.java $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.app.xmlui.aspect.discovery;

/**
 * Display a single item.
 *
 * @author Mark Diggory
 */
public class ItemFacets extends org.dspace.app.xmlui.aspect.discovery.AbstractFiltersTransformer
{

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(org.dspace.app.xmlui.aspect.discovery.ItemFacets.class);

    /**
     * Display a single item
     */
    public void addBody(org.dspace.app.xmlui.wing.element.Body body) throws org.xml.sax.SAXException, org.dspace.app.xmlui.wing.WingException,
            org.dspace.app.xmlui.utils.UIException, java.sql.SQLException, java.io.IOException, org.dspace.authorize.AuthorizeException
    {

        org.dspace.content.DSpaceObject dso = org.dspace.app.xmlui.utils.HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof org.dspace.content.Item))
            return;
        org.dspace.content.Item item = (org.dspace.content.Item) dso;

        try {
            performSearch(item);
        } catch (org.dspace.discovery.SearchServiceException e) {
            log.error(e.getMessage(),e);
        }


    }

    @Override
    public void performSearch(org.dspace.content.DSpaceObject dso) throws org.dspace.discovery.SearchServiceException {

        if(queryResults != null)
            return;

        this.queryArgs = prepareDefaultFilters("item");
        
        this.queryArgs.setRows(1);
        this.queryArgs.setQuery("handle:" + dso.getHandle());

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