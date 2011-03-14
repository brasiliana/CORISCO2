/**
 * $Id: SolrServiceImpl.java 5161 2010-07-02 11:34:56Z KevinVandeVelde $
 * $URL: http://scm.dspace.org/svn/repo/modules/dspace-discovery/trunk/provider/src/main/java/org/dspace/discovery/SolrServiceImpl.java $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.discovery;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.handle.HandleManager;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Service;

import java.io.*;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

/**
 * SolrIndexer contains the methods that index Items and their metadata,
 * collections, communities, etc. It is meant to either be invoked from the
 * command line (see dspace/bin/index-all) or via the indexContent() methods
 * within DSpace.
 * <p/>
 * The Administrator can choose to run SolrIndexer in a cron that repeats
 * regularly, a failed attempt to index from the UI will be "caught" up on in
 * that cron.
 *
 * The SolrServiceImple is registered as a Service in the ServiceManager via
 * A spring configuration file located under
 * classpath://spring/spring-dspace-applicationContext.xml
 *
 * Its configuration is Autowired by the ApplicationContext
 *
 * @author Mark Diggory
 * @author Ben Bosman
 */
@Service
public class SolrServiceImpl implements SearchService, IndexingService {

    private static final Logger log = Logger.getLogger(SolrServiceImpl.class);
    private static final String LAST_INDEXED_FIELD = "SolrIndexer.lastIndexed";
    /**
     * CommonsHttpSolrServer for processing indexing events.
     */
    private static CommonsHttpSolrServer solr = null;
    private ConfigurationService configurationService;

    @Autowired
    @Required
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    protected CommonsHttpSolrServer getSolr() throws java.net.MalformedURLException, org.apache.solr.client.solrj.SolrServerException {


        if (solr == null) {
            ExtendedProperties props = null;
            //Method that will retrieve all the possible configs we have

            props = ExtendedProperties.convertProperties(ConfigurationManager.getProperties());

            try {
                File config = new File(props.getProperty("dspace.dir")
                        + "/config/dspace-solr-search.cfg");
                if (config.exists()) {
                    props.combine(new ExtendedProperties(config.getAbsolutePath()));
                } else {
                    ExtendedProperties defaults = new ExtendedProperties();
                    defaults.load(SolrServiceImpl.class.getResourceAsStream("dspace-solr-search.cfg"));
                    props.combine(defaults);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            String solrService = configurationService.getProperty("solr.search.server");

            /*
             * @deprecated need to remove this in favor of looking up above.
             */
            if (solrService == null) {
                solrService = props.getString("solr.search.server", "http://localhost:8080/solr/search");
            }

            log.debug("Solr URL: " + solrService);
            solr = new CommonsHttpSolrServer(solrService);

            solr.setBaseURL(solrService);

            SolrQuery solrQuery = new SolrQuery().setQuery("search.resourcetype:2 AND search.resourceid:1");

            solr.query(solrQuery);
        }

        return solr;
    }

    /**
     * If the handle for the "dso" already exists in the index, and the "dso"
     * has a lastModified timestamp that is newer than the document in the index
     * then it is updated, otherwise a new document is added.
     *
     * @param context Users Context
     * @param dso     DSpace Object (Item, Collection or Community
     * @throws SQLException
     * @throws IOException
     */
    public void indexContent(Context context, DSpaceObject dso)
            throws SQLException {
        indexContent(context, dso, false);
    }

    /**
     * If the handle for the "dso" already exists in the index, and the "dso"
     * has a lastModified timestamp that is newer than the document in the index
     * then it is updated, otherwise a new document is added.
     *
     * @param context Users Context
     * @param dso     DSpace Object (Item, Collection or Community
     * @param force   Force update even if not stale.
     * @throws SQLException
     * @throws IOException
     */
    public void indexContent(Context context, DSpaceObject dso,
            boolean force) throws SQLException {

        String handle = dso.getHandle();

        if (handle == null) {
            handle = HandleManager.findHandle(context, dso);
        }

        try {
            switch (dso.getType()) {
                case Constants.ITEM:
                    Item item = (Item) dso;
                    if (item.isArchived() && !item.isWithdrawn()) {
                        /**
                         * If the item is in the repository now, add it to the index
                         */
                        if (requiresIndexing(handle, ((Item) dso).getLastModified())
                                || force) {
                            unIndexContent(context, handle);
                            buildDocument(context, (Item) dso);
                        }
                    } else {
                        /**
                         * Make sure the item is not in the index if it is not in
                         * archive. TODO: Someday DSIndexer should block withdrawn
                         * content on search/retrieval and allow admins the ablitity
                         * to still search for withdrawn Items.
                         */
                        unIndexContent(context, handle);
                        log.info("Removed Item: " + handle + " from Index");
                    }
                    break;

                case Constants.COLLECTION:
                    buildDocument(context, (Collection) dso);
                    log.info("Wrote Collection: " + handle + " to Index");
                    break;

                case Constants.COMMUNITY:
                    buildDocument(context, (Community) dso);
                    log.info("Wrote Community: " + handle + " to Index");
                    break;

                default:
                    log.error("Only Items, Collections and Communities can be Indexed");
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        try {
            getSolr().commit();
        } catch (SolrServerException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    /**
     * unIndex removes an Item, Collection, or Community only works if the
     * DSpaceObject has a handle (uses the handle for its unique ID)
     *
     * @param context
     * @param dso     DSpace Object, can be Community, Item, or Collection
     * @throws SQLException
     * @throws IOException
     */
    public void unIndexContent(Context context, DSpaceObject dso)
            throws SQLException, IOException {
        try {
            unIndexContent(context, dso.getHandle());
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            emailException(exception);
        }
    }

    /**
     * Unindex a Document in the Lucene index.
     * @param context the dspace context
     * @param handle the handle of the object to be deleted
     * @throws IOException
     * @throws SQLException
     */
    public void unIndexContent(Context context, String handle) throws IOException, SQLException {
        unIndexContent(context, handle, false);
    }

    /**
     * Unindex a Docment in the Lucene Index.
     * @param context the dspace context
     * @param handle the handle of the object to be deleted
     * @throws SQLException
     * @throws IOException
     */
    public void unIndexContent(Context context, String handle, boolean commit)
            throws SQLException, IOException {

        try {
            getSolr().deleteById(handle);
            if (commit) {
                getSolr().commit();
            }
        } catch (SolrServerException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * reIndexContent removes something from the index, then re-indexes it
     *
     * @param context context object
     * @param dso     object to re-index
     */
    public void reIndexContent(Context context, DSpaceObject dso)
            throws SQLException, IOException {
        try {
            indexContent(context, dso);
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            emailException(exception);
        }
    }

    /**
     * create full index - wiping old index
     *
     * @param c context to use
     */
    public void createIndex(Context c) throws SQLException, IOException {

        /* Reindex all content preemptively. */
        updateIndex(c, true);

    }

    /**
     * Iterates over all Items, Collections and Communities. And updates them in
     * the index. Uses decaching to control memory footprint. Uses indexContent
     * and isStale ot check state of item in index.
     *
     * @param context
     */
    public void updateIndex(Context context) {
        updateIndex(context, false);
    }

    /**
     * Iterates over all Items, Collections and Communities. And updates them in
     * the index. Uses decaching to control memory footprint. Uses indexContent
     * and isStale ot check state of item in index.
     * <p/>
     * At first it may appear counterintuitive to have an IndexWriter/Reader
     * opened and closed on each DSO. But this allows the UI processes to step
     * in and attain a lock and write to the index even if other processes/jvms
     * are running a reindex.
     *
     * @param context
     * @param force
     */
    public void updateIndex(Context context, boolean force) {
        try {
            ItemIterator items = null;
            try {
                for (items = Item.findAll(context); items.hasNext();) {
                    Item item = (Item) items.next();
                    indexContent(context, item, force);
                    item.decache();
                }
            } finally {
                if (items != null) {
                    items.close();
                }
            }

            Collection[] collections = Collection.findAll(context);
            for (int i = 0; i < collections.length; i++) {
                indexContent(context, collections[i], force);
                context.removeCached(collections[i], collections[i].getID());

            }

            Community[] communities = Community.findAll(context);
            for (int i = 0; i < communities.length; i++) {
                indexContent(context, communities[i], force);
                context.removeCached(communities[i], communities[i].getID());
            }


        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Iterates over all documents in the Lucene index and verifies they are in
     * database, if not, they are removed.
     *
     * @param force
     * @throws IOException
     * @throws SQLException
     * @throws SolrServerException
     */
    public void cleanIndex(boolean force) throws IOException,
            SQLException, SearchServiceException {

        Context context = new Context();
        context.turnOffAuthorisationSystem();

        try {
            if (force) {
                getSolr().deleteByQuery("*:*");
            } else {
                SolrQuery query = new SolrQuery();
                query.setQuery("*:*");
                QueryResponse rsp = getSolr().query(query);
                SolrDocumentList docs = rsp.getResults();

                Iterator iter = docs.iterator();
                while (iter.hasNext()) {

                    SolrDocument doc = (SolrDocument) iter.next();

                    String handle = (String) doc.getFieldValue("handle");

                    DSpaceObject o = HandleManager.resolveToObject(context, handle);

                    if (o == null) {
                        log.info("Deleting: " + handle);
                        /*
                         * Use IndexWriter to delete, its easier to manage
                         * write.lock
                         */
                        unIndexContent(context, handle);
                    } else {
                        context.removeCached(o, o.getID());
                        log.debug("Keeping: " + handle);
                    }
                }
            }
        } catch (Exception e) {

            throw new SearchServiceException(e.getMessage(), e);
        } finally {
            context.abort();
        }




    }

    // //////////////////////////////////
    // Private
    // //////////////////////////////////
    private void emailException(Exception exception) {
        // Also email an alert, system admin may need to check for stale lock
        try {
            String recipient = ConfigurationManager.getProperty("alert.recipient");

            if (recipient != null) {
                Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(
                        Locale.getDefault(), "internal_error"));
                email.addRecipient(recipient);
                email.addArgument(ConfigurationManager.getProperty("dspace.url"));
                email.addArgument(new Date());

                String stackTrace;

                if (exception != null) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    exception.printStackTrace(pw);
                    pw.flush();
                    stackTrace = sw.toString();
                } else {
                    stackTrace = "No exception";
                }

                email.addArgument(stackTrace);
                email.send();
            }
        } catch (Exception e) {
            // Not much we can do here!
            log.warn("Unable to send email alert", e);
        }

    }

    /**
     * Is stale checks the lastModified time stamp in the database and the index
     * to determine if the index is stale.
     *
     * @param handle
     * @param lastModified
     * @return
     * @throws SQLException
     * @throws IOException
     * @throws SolrServerException
     */
    private boolean requiresIndexing(String handle, Date lastModified)
            throws SQLException, IOException, SearchServiceException {

        boolean reindexItem = false;
        boolean inIndex = false;

        SolrQuery query = new SolrQuery();
        query.setQuery("handle:" + handle);
        QueryResponse rsp = null;

        try {
            rsp = getSolr().query(query);
        } catch (SolrServerException e) {
            throw new SearchServiceException(e.getMessage(), e);
        }

        for (SolrDocument doc : rsp.getResults()) {

            inIndex = true;

            Object value = doc.getFieldValue(LAST_INDEXED_FIELD);

            if (value instanceof Date) {
                Date lastIndexed = (Date) value;

                if (lastIndexed == null
                        || lastIndexed.before(lastModified)) {

                    reindexItem = true;
                }
            }
        }

        return reindexItem || !inIndex;
    }

    /**
     * @param c
     * @param myitem
     * @return
     * @throws SQLException
     */
    private List<String> getItemLocations(Context c, Item myitem)
            throws SQLException {
        List<String> locations = new Vector<String>();

        // build list of community ids
        Community[] communities = myitem.getCommunities();

        // build list of collection ids
        Collection[] collections = myitem.getCollections();

        // now put those into strings
        int i = 0;

        for (i = 0; i < communities.length; i++) {
            locations.add(new String("m" + communities[i].getID()));
        }

        for (i = 0; i < collections.length; i++) {
            locations.add(new String("l" + collections[i].getID()));
        }

        return locations;
    }

    private List<String> getCollectionLocations(Context c,
            Collection target) throws SQLException {
        List<String> locations = new Vector<String>();
        // build list of community ids
        Community[] communities = target.getCommunities();

        // now put those into strings
        String location = "";
        int i = 0;

        for (i = 0; i < communities.length; i++) {
            locations.add(new String("m" + communities[i].getID()));
        }

        return locations;
    }

    /**
     * Write the document to the index under the appropriate handle.
     * @param doc
     * @throws IOException
     */
    private void writeDocument(SolrInputDocument doc) throws IOException {

        try {
            log.debug("solr.add: " + getSolr().add(doc).toString());
        } catch (SolrServerException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Build a Lucene document for a DSpace Community.
     *
     * @param context   Users Context
     * @param community Community to be indexed
     * @throws SQLException
     * @throws IOException
     */
    private void buildDocument(Context context, Community community)
            throws SQLException, IOException {
        // Create Document
        SolrInputDocument doc = buildDocument(Constants.COMMUNITY, community.getID(),
                community.getHandle(), null);

        // and populate it
        String name = community.getMetadata("name");

        // A container's name is in the "name" column in DSpace DB,
        // but we will index it in the field "dc.title" in order to
        // have search results displaying containers and items together
        // when sorted by title. Otherwise, as things were orginally,
        // collections were always appearing on the last page, regardless
        // of sorting order.

        if (name != null) {
            doc.addField("name", name);
            //doc.addField("dc.title", name);
        }

        writeDocument(doc);
    }

    /**
     * Build a Lucene document for a DSpace Collection.
     *
     * @param context    Users Context
     * @param collection Collection to be indexed
     * @throws SQLException
     * @throws IOException
     */
    private void buildDocument(Context context, Collection collection)
            throws SQLException, IOException {
        List<String> locations = getCollectionLocations(context,
                collection);

        // Create Lucene Document
        SolrInputDocument doc = buildDocument(Constants.COLLECTION, collection.getID(),
                collection.getHandle(), locations);

        // and populate it
        String name = collection.getMetadata("name");

        if (name != null) {
            doc.addField("name", name);
            //doc.addField("dc.title", name);
        }

        writeDocument(doc);
    }

    /**
     * Build a Lucene document for a DSpace Item and write the index
     *
     * @param context Users Context
     * @param item    The DSpace Item to be indexed
     * @throws SQLException
     * @throws IOException
     */
    private void buildDocument(Context context, Item item)
            throws SQLException, IOException {
        String handle = item.getHandle();

        if (handle == null) {
            handle = HandleManager.findHandle(context, item);
        }

        // get the location string (for searching by collection & community)
        List<String> locations = getItemLocations(context, item);

        SolrInputDocument doc = buildDocument(Constants.ITEM, item.getID(), handle,
                locations);

        log.debug("Building Item: " + handle);

        List<String> splitFields = new ArrayList<String>();
        int index = 1;
        String filter = SearchUtils.getConfig().getString("solr.search.filter.type." + index);
        while (filter != null) {
            splitFields.add(filter);
            index++;
            filter = SearchUtils.getConfig().getString("solr.search.filter.type." + index);
        }

        try {

            DCValue[] mydc = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);

            // Merge given repeated fields.
            // TODO: put merging delimiter in config
            String delimitier = "; ";
            String[] merge = SearchUtils.getConfig().getStringArray("solr.indexing.merge");
            if (merge != null) {
                log.debug("Merging " + merge.length + " fields");
                //Arrays.asList(mydc)
                log.debug("Metadata fields size: " + mydc.length);
                for (int i = 0; i < merge.length; i++) {
                    log.debug("Searching repeated fields for " + merge[i]);
                    for (int m = 0; m < mydc.length; m++) {
                        DCValue meta = mydc[m];
                        String field = meta.schema + "." + meta.element;
                        if (meta.qualifier != null && !meta.qualifier.trim().equals("")) {
                            field += "." + meta.qualifier;
                        }
                        if (merge[i].equals(field)) {
                            log.debug("Found field for merge: " + field);
                            for (int m2 = m + 1; m2 < mydc.length; m2++) {
                                DCValue meta2 = mydc[m2];
                                String field2 = meta2.schema + "." + meta2.element;
                                if (meta2.qualifier != null && !meta2.qualifier.trim().equals("")) {
                                    field2 += "." + meta2.qualifier;
                                }
                                if (merge[i].equals(field2)) {
                                    log.debug("Found repeated field " + field2);
                                    log.debug("Field 1 value: " + meta.value);
                                    log.debug("Field 2 value: " + meta2.value);
                                    mydc[m].value += delimitier + meta2.value;
                                    log.debug("Merged field value: " + mydc[m].value);
                                    mydc = (DCValue[]) ArrayUtils.remove(mydc, m2);
                                    m2--;
                                    /*
                                    DCValue[] mydctmp = Arrays.copyOf(mydc, mydc.length - 1);
                                    System.arraycopy(mydc, m2+1, mydctmp, m2, mydc.length - 1 - m2); // removing item at index m2
                                    mydc = Arrays.copyOf(mydctmp, mydctmp.length);
                                    m2--;
                                    */
                                }
                            }
                            break;
                        }
                    }
                }
                log.debug("New metadata fields size: " + mydc.length);
            }

            // Go on to default indexing.
            for (DCValue meta : mydc) {
                String field = meta.schema + "." + meta.element;

                String value = meta.value;

                if (meta.qualifier != null && !meta.qualifier.trim().equals("")) {
                    field += "." + meta.qualifier;
                }

                if (meta.element.equals("date") && meta.schema.equals("dc")) {
                    try {
                        Date date = toDate(value);
                        // FIXME: Not adding date field if it doesn't contain a valid date value;
                        //        This behaviour is changeable, but depends on the date fields defined in schema.xml:
                        //        If the field is of type "date" and an invalid value is added, the item will not
                        //        appear in DSpace browsing lists and search results.
                        if (date != null) {
                            value = DateFormatUtils.formatUTC(date, "yyyy-MM-dd'T'HH:mm:ss'Z'");
                            String fieldNameYear = meta.element + (meta.qualifier != null && !meta.qualifier.trim().equals("") ? meta.qualifier : "") + ".year";
                            int fieldValueYear = Integer.parseInt(DateFormatUtils.formatUTC(date, "yyyy"));
                            log.debug("field name:value: " + fieldNameYear + ":" + fieldValueYear);
                            if (doc.getFieldValue(fieldNameYear) == null) {
                                doc.addField(fieldNameYear, fieldValueYear);
                                doc.addField(field, value);
                                log.debug("addField: " + field + "(" + fieldNameYear + "):" + value + "(" + fieldValueYear + ")");
                            } else {
                                Date otherDate = toDate(doc.getFieldValue(field).toString());
                                if (date.after(otherDate)) {
                                    doc.setField(fieldNameYear, fieldValueYear);
                                    doc.setField(field, value);
                                    log.debug("setField: " + field + "(" + fieldNameYear + "):" + value + "(" + fieldValueYear + ")");
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Converting date:");
                        log.warn(e.getMessage(), e);
                    }
                } else {
                    doc.addField(field, value);
                }

                if (meta.language != null && !meta.language.trim().equals("")) {
                    String langField = field + "." + meta.language;
                    doc.addField(langField, value);
                }

                if (splitFields.contains(field + ".split")) {
                    //Split it & store it
                    for (int i = 0; i < value.split(" ").length; i++) {
                        String word = value.split(" ")[i];
                        doc.addField(field + ".split", word.toLowerCase());
                    }
                }
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }


        log.debug("  Added Metadata");

        try {

            DCValue[] values = item.getMetadata("dc.relation.ispartof");

            if (values != null && values.length > 0 && values[0] != null && values[0].value != null) {
                // group on parent
                String handlePrefix = ConfigurationManager.getProperty("handle.canonical.prefix");
                if (handlePrefix == null || handlePrefix.length() == 0) {
                    handlePrefix = "http://hdl.handle.net/";
                }

                doc.addField("publication_grp", values[0].value.replaceFirst(handlePrefix, ""));

            } else {
                // group on self
                doc.addField("publication_grp", item.getHandle());
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }


        log.debug("  Added Grouping");


        Vector<InputStreamReader> readers = new Vector<InputStreamReader>();

        try {
            // now get full text of any bitstreams in the TEXT bundle
            // trundle through the bundles
            Bundle[] myBundles = item.getBundles();

            for (int i = 0; i < myBundles.length; i++) {
                if ((myBundles[i].getName() != null)
                        && myBundles[i].getName().equals("TEXT")) {
                    // a-ha! grab the text out of the bitstreams
                    Bitstream[] myBitstreams = myBundles[i].getBitstreams();

                    for (int j = 0; j < myBitstreams.length; j++) {
                        try {
                            InputStreamReader is = new InputStreamReader(
                                    myBitstreams[j].retrieve()); // get input
                            readers.add(is);

                            // Add each InputStream to the Indexed Document
                            // (Acts like an Append)
                            //doc.addField("default", is);
                            //doc.add(new Field("default", is));

                            StringBuilder buffer = new StringBuilder();
                            Reader in = new BufferedReader(is);
                            int ch;
                            while ((ch = in.read()) > -1) {
                                buffer.append((char)ch);
                            }
                            String str = buffer.toString();
                            doc.addField("content", str);

                            log.debug("  Added BitStream: "
                                    + myBitstreams[j].getStoreNumber() + "	"
                                    + myBitstreams[j].getSequenceID() + "   "
                                    + myBitstreams[j].getName());

                        } catch (Exception e) {
                            // this will never happen, but compiler is now
                            // happy.
                            log.error(e.getMessage(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        // write the index and close the inputstreamreaders
        try {
            log.debug("Write doc: " + doc.toString());
            writeDocument(doc);
            log.info("Wrote Item: " + handle + " to Index");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            Iterator<InputStreamReader> itr = readers.iterator();
            while (itr.hasNext()) {
                InputStreamReader reader = itr.next();
                if (reader != null) {
                    reader.close();
                }
            }
            log.debug("closed " + readers.size() + " readers");
        }
    }

    /**
     * Create Lucene document with all the shared fields initialized.
     *
     * @param type      Type of DSpace Object
     * @param id
     * @param handle
     * @param locations @return
     */
    private SolrInputDocument buildDocument(int type, int id, String handle,
            List<String> locations) {
        SolrInputDocument doc = new SolrInputDocument();

        // want to be able to check when last updated
        // (not tokenized, but it is indexed)
        doc.addField(LAST_INDEXED_FIELD, new Date());

        // New fields to weaken the dependence on handles, and allow for faster
        // list display
        doc.addField("search.resourcetype", Integer.toString(type));

        doc.addField("search.resourceid", Integer.toString(id));

        // want to be able to search for handle, so use keyword
        // (not tokenized, but it is indexed)
        if (handle != null) {
            // want to be able to search for handle, so use keyword
            // (not tokenized, but it is indexed)
            doc.addField("handle", handle);
        }

        if (locations != null) {
            for (String location : locations) {
                doc.addField("location", location);
                if (location.startsWith("m")) {
                    doc.addField("location.comm", location.substring(1));
                } else {
                    doc.addField("location.coll", location.substring(1));
                }
            }
        }

        return doc;
    }

    /**
     * Helper function to retrieve a date using a best guess of the potential
     * date encodings on a field
     *
     * @param t
     * @return
     */
    public static Date toDate(String t) {
        SimpleDateFormat[] dfArr;

        // Choose the likely date formats based on string length
        switch (t.length()) {
            case 4:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat("yyyy")};
                break;
            case 6:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat("yyyyMM")};
                break;
            case 7:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat("yyyy-MM")};
                break;
            case 8:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat("yyyyMMdd"),
                            new SimpleDateFormat("yyyy MMM")};
                break;
            case 10:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat("yyyy-MM-dd")};
                break;
            case 11:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat("yyyy MMM dd")};
                break;
            case 20:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss'Z'")};
                break;
            default:
                dfArr = new SimpleDateFormat[]{new SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")};
                break;
        }

        for (SimpleDateFormat df : dfArr) {
            try {
                // Parse the date
                df.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
                df.setLenient(false);
                return df.parse(t);
            } catch (ParseException pe) {
                log.warn("Unable to parse date format", pe);
                return null;
            }
        }

        return null;
    }

    public static String locationToName(Context context, String field, String value) throws SQLException {
        if ("location.comm".equals(field) || "location.coll".equals(field)) {
            int type = field.equals("location.comm") ? Constants.COMMUNITY : Constants.COLLECTION;
            DSpaceObject commColl = DSpaceObject.find(context, type, Integer.parseInt(value));
            if (commColl != null) {
                return commColl.getName();
            }

        }
        return value;
    }

    //******** SearchService implementation
    public QueryResponse search(SolrQuery query) throws SearchServiceException {
        try {
            return getSolr().query(query);
        } catch (Exception e) {
            throw new org.dspace.discovery.SearchServiceException(e.getMessage(), e);
        }
    }

    /** Simple means to return the search result as an InputStream */
    public java.io.InputStream searchAsInputStream(SolrQuery query) throws SearchServiceException, java.io.IOException {
        try {
            org.apache.commons.httpclient.methods.GetMethod method =
                    new org.apache.commons.httpclient.methods.GetMethod(getSolr().getHttpClient().getHostConfiguration().getHostURL() + "");

            method.setQueryString(query.toString());

            getSolr().getHttpClient().executeMethod(method);

            return method.getResponseBodyAsStream();

        } catch (org.apache.solr.client.solrj.SolrServerException e) {
            throw new SearchServiceException(e.getMessage(), e);
        }

    }

    public List<DSpaceObject> search(Context context, String query, int offset, int max, String... filterquery) {
        return search(context, query, null, true, offset, max, filterquery);
    }

    public List<DSpaceObject> search(Context context, String query, String orderfield, boolean ascending, int offset, int max, String... filterquery) {

        try {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(query);
            solrQuery.setFields("search.resourceid", "search.resourcetype");
            solrQuery.setStart(offset);
            solrQuery.setRows(max);
            if (orderfield != null) {
                solrQuery.setSortField(orderfield, ascending ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
            }
            if (filterquery != null) {
                solrQuery.addFilterQuery(filterquery);
            }
            QueryResponse rsp = getSolr().query(solrQuery);
            SolrDocumentList docs = rsp.getResults();

            Iterator iter = docs.iterator();
            List<DSpaceObject> result = new ArrayList<DSpaceObject>();
            while (iter.hasNext()) {
                SolrDocument doc = (SolrDocument) iter.next();

                String handle = (String) doc.getFieldValue("handle");

                DSpaceObject o = DSpaceObject.find(context, (Integer) doc.getFirstValue("search.resourcetype"), (Integer) doc.getFirstValue("search.resourceid"));

                if (o != null) {
                    result.add(o);
                }
            }
            return result;
        } catch (Exception e) {
            // Any acception that we get ignore it.
            // We do NOT want any crashed to shown by the user
            e.printStackTrace();
            return new ArrayList<DSpaceObject>(0);
        }
    }
}
