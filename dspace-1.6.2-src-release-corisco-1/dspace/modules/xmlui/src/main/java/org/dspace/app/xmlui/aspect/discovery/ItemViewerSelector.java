/*
 * ItemViewerSelector.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 13:02:24 -0400 (Sat, 11 Apr 2009) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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

package org.dspace.app.xmlui.aspect.discovery;

import java.util.Map;

import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.selection.Selector;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;

/**
 * This simple selector looks for the MIME-type of the first bitstream in
 * current item, and returns the corresponding MIME.
 *
 * Typical sitemap usage:
 *
 *  <map:match type="handle" pattern="item">
 *    <map:select type="ItemViewerSelector">
 *      <map:when test="image/jp2">
 *        <map:act type="Image"/>
 *        <map:serialize/>
 *      </map:when>
 *      <map:otherwise>
 *        <map:transform type="ItemViewer"/>
 *        <map:serialize type="xml"/>
 *      </map:otherwise>
 *    </map:select>
 *  </map:match>
 *
 * @author Larry Stone
 */
public class ItemViewerSelector implements Selector
{

    private static Logger log = Logger.getLogger(ItemViewerSelector.class);

    /**
     *
     * @param expression is the string tested against in sitemap
     * @param objectModel
     *            environment passed through via cocoon
     * @return null or map containing value of sitemap parameter 'pattern'
     */
    @Override
    public boolean select(String expression, Map objectModel,
            Parameters parameters)
    {
        try
        {
//            Request request = ObjectModelHelper.getRequest(objectModel);
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            if (dso.getType() == Constants.ITEM) {
                Item item = (Item) dso;
                Bundle[] bundles = item.getBundles("ORIGINAL");
                for (Bundle bundle : bundles) {
//                    log.debug("bundle.name: " + bundle.getName());
//                    log.debug("bundle.priID: " + bundle.getPrimaryBitstreamID());
                    for (Bitstream bitstream : bundle.getBitstreams()) {
//                        log.debug("bitstream.name: " + bitstream.getName());
//                        log.debug("bitstream.id:" + bitstream.getID());
                        if (bundle.getPrimaryBitstreamID() == -1
                                || bundle.getPrimaryBitstreamID() == bitstream.getID()) {
//                            log.debug("priID: " + bitstream.getID());
                            String mimetype = bitstream.getFormat().getMIMEType();
//                            log.debug("expression: " + expression);
//                            log.debug("mime: " + mimetype);
                            if (mimetype.equals(expression)) {
                                return true;
                            } else {
                                return false;
                            }
                        }
                    }
                }
            }
            return false;
        }
        catch (Exception e)
        {
            log.error("Error selecting based on ItemViewerSelector: "+e.toString());
            return false;
        }
    }
}
