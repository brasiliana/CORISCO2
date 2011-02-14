<?xml version="1.0" encoding="UTF-8"?>

<!--
  bitstream-listing.xsl
-->

<!--
    Author: Fabio N. Kepler
    Description: Specialization of 'General-Handler.xsl'.
-->

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
        xmlns:dri="http://di.tamu.edu/DRI/1.0/"
        xmlns:mets="http://www.loc.gov/METS/"
        xmlns:xlink="http://www.w3.org/TR/xlink/"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
        xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
        xmlns:xhtml="http://www.w3.org/1999/xhtml"
        xmlns:mods="http://www.loc.gov/mods/v3"
        xmlns:dc="http://purl.org/dc/elements/1.1/"
        xmlns="http://www.w3.org/1999/xhtml"
        exclude-result-prefixes="mets xlink xsl dim xhtml mods dc">

    <xsl:output indent="yes"/>


    <!-- Generate the thunbnail, if present, from the file section -->
    <xsl:template match="mets:fileSec" mode="artifact-preview">
        <xsl:param name="primaryBitstream" select="-1"/>
        <xsl:comment><xsl:value-of select="$primaryBitstream"/></xsl:comment>

        <a href="{ancestor::mets:METS/@OBJID}">
            <xsl:choose>
                <xsl:when test="$primaryBitstream != -1">
                    <xsl:choose>
                        <xsl:when test="mets:fileGrp[@USE='THUMBNAIL']/mets:file[@ID=$primaryBitstream]">
                            <img class="preview-thumbnail" alt="Thumbnail">
                                <xsl:attribute name="src">
                                    <xsl:value-of select="mets:fileGrp[@USE='THUMBNAIL']/mets:file[@ID=$primaryBitstream]/mets:FLocat[@LOCTYPE='URL']/@xlink:href" />
                                </xsl:attribute>
                            </img>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:variable name="thumbnail-path">
                                <xsl:value-of select="$djatoka-thumbnail-base-url"/>
                                <xsl:value-of select="//mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']//mets:fptr[@FILEID=$primaryBitstream]/@FILEINTERNALID"/>
                            </xsl:variable>
        
                            <img class="preview-thumbnail" alt="Thumbnail" onerror="this.onerror=null;this.src='{$bookreader-path}/images/transparent.png';">
                                <xsl:attribute name="src">
                                    <xsl:value-of select="$thumbnail-path"/>
                                </xsl:attribute>
                            </img>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>

                <xsl:when test="mets:fileGrp[@USE='THUMBNAIL']">
                    <img class="preview-thumbnail" alt="Thumbnail">
                        <xsl:attribute name="src">
                            <xsl:value-of select="mets:fileGrp[@USE='THUMBNAIL']/mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href" />
                        </xsl:attribute>
                    </img>
                </xsl:when>

                <xsl:when test="mets:fileGrp[@USE='LOGO']">
                    <xsl:comment>@USE='LOGO' preview-thumbnail</xsl:comment>
                    <xsl:apply-templates select="mets:fileGrp[@USE='LOGO']"/>
                </xsl:when>

                <xsl:otherwise>
                    <xsl:variable name="fileid">
                        <xsl:value-of select="mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']/mets:file[1]/@ID"/>
                    </xsl:variable>
                    <xsl:variable name="thumbnail-path">
                        <xsl:value-of select="$djatoka-thumbnail-base-url"/>
                        <xsl:value-of select="//mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']//mets:fptr[@FILEID=$fileid]/@FILEINTERNALID"/>
                    </xsl:variable>

                    <img class="preview-thumbnail" alt="Thumbnail" onerror="this.onerror=null;this.src='{$bookreader-path}/images/transparent.png';">
                        <xsl:attribute name="src">
                            <xsl:value-of select="$thumbnail-path"/>
                        </xsl:attribute>
                    </img>
<!--                    <img alt="Thumbnail">
                        <xsl:attribute name="src">
                            <xsl:value-of select="$images-path"/>
                            <xsl:text>visualizador_pg1.gif</xsl:text>
                        </xsl:attribute>
                    </img>-->
                </xsl:otherwise>
            </xsl:choose>
        </a>
    </xsl:template>



    <!-- Generate the bitstream information from the file section -->
    <xsl:template match="mets:fileGrp[@USE='CONTENT']">
        <xsl:param name="context"/>
        <xsl:param name="primaryBitstream" select="-1"/>

        <h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h2>
        <table class="ds-table file-list">
            <tr class="ds-table-header-row">
                <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-file</i18n:text></th>
                <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-size</i18n:text></th>
                <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-format</i18n:text></th>
                <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-view</i18n:text></th>
                <!-- Display header for 'Description' only if at least one bitstream contains a description -->
                <xsl:if test="mets:file/mets:FLocat/@xlink:label != ''">
                    <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-description</i18n:text></th>
                </xsl:if>
		    </tr>
            <xsl:choose>
                <!-- If one exists and it's of text/html MIME type, only display the primary bitstream -->
                <xsl:when test="mets:file[@ID=$primaryBitstream]/@MIMETYPE='text/html'">
                    <xsl:apply-templates select="mets:file[@ID=$primaryBitstream]">
                        <xsl:with-param name="context" select="$context"/>
                    </xsl:apply-templates>
                </xsl:when>
                <!-- Otherwise, iterate over and display all of them -->
                <xsl:otherwise>
                    <xsl:apply-templates select="mets:file">
                     	<xsl:sort data-type="number" select="boolean(./@ID=$primaryBitstream)" order="descending" />
                        <xsl:sort select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                        <xsl:with-param name="context" select="$context"/>
                    </xsl:apply-templates>
                </xsl:otherwise>
            </xsl:choose>
        </table>
    </xsl:template>


    <!-- Build a single row in the bitsreams table of the item view page -->
    <xsl:template match="mets:file">
        <xsl:param name="context" select="."/>
        <tr>
            <xsl:attribute name="class">
                <xsl:text>ds-table-row </xsl:text>
                <xsl:if test="(position() mod 2 = 0)">even </xsl:if>
                <xsl:if test="(position() mod 2 = 1)">odd </xsl:if>
            </xsl:attribute>
            <td>
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                    </xsl:attribute>
                    <xsl:attribute name="title">
                        <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="string-length(mets:FLocat[@LOCTYPE='URL']/@xlink:title) > 50">
                            <xsl:variable name="title_length" select="string-length(mets:FLocat[@LOCTYPE='URL']/@xlink:title)"/>
                            <xsl:value-of select="substring(mets:FLocat[@LOCTYPE='URL']/@xlink:title,1,15)"/>
                            <xsl:text> ... </xsl:text>
                            <xsl:value-of select="substring(mets:FLocat[@LOCTYPE='URL']/@xlink:title,$title_length - 25,$title_length)"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </a>
            </td>
            <!-- File size always comes in bytes and thus needs conversion -->
            <td>
                <xsl:choose>
                    <xsl:when test="@SIZE &lt; 1000">
                        <xsl:value-of select="@SIZE"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-bytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="@SIZE &lt; 1000000">
                        <xsl:value-of select="substring(string(@SIZE div 1000),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-kilobytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="@SIZE &lt; 1000000000">
                        <xsl:value-of select="substring(string(@SIZE div 1000000),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-megabytes</i18n:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="substring(string(@SIZE div 1000000000),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-gigabytes</i18n:text>
                    </xsl:otherwise>
                </xsl:choose>
            </td>
            <!-- Lookup File Type description in local messages.xml based on MIME Type.
                In the original DSpace, this would get resolved to an application via
                the Bitstream Registry, but we are constrained by the capabilities of METS
                and can't really pass that info through. -->
            <td>
              <xsl:call-template name="getFileTypeDesc">
                <xsl:with-param name="mimetype">
                  <xsl:value-of select="substring-before(@MIMETYPE,'/')"/>
                  <xsl:text>/</xsl:text>
                  <xsl:value-of select="substring-after(@MIMETYPE,'/')"/>
                </xsl:with-param>
              </xsl:call-template>
            </td>
            <td>
                <xsl:choose>
                    <xsl:when test="$context/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/
                        mets:file[@GROUPID=current()/@GROUPID]">
                        <a class="image-link">
                            <xsl:attribute name="href">
<!--                                <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>-->
                                <xsl:call-template name="getFileViewURL">
                                    <xsl:with-param name="file" select="."/>
                                </xsl:call-template>
                            </xsl:attribute>
                            <img alt="Thumbnail">
                                <xsl:attribute name="src">
                                    <xsl:value-of select="$context/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/
                                        mets:file[@GROUPID=current()/@GROUPID]/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                                </xsl:attribute>
                            </img>
                        </a>
                    </xsl:when>
                    <xsl:otherwise>
                        <a>
                            <xsl:attribute name="href">
                                <xsl:call-template name="getFileViewURL">
                                    <xsl:with-param name="file" select="."/>
                                </xsl:call-template>
                            </xsl:attribute>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-viewOpen</i18n:text>
                        </a>
                    </xsl:otherwise>
                </xsl:choose>
            </td>
	    <!-- Display the contents of 'Description' as long as at least one bitstream contains a description -->
	    <xsl:if test="$context/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file/mets:FLocat/@xlink:label != ''">
	        <td>
	            <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:label"/>
	        </td>
	    </xsl:if>

        </tr>
    </xsl:template>

    <!--
    File Type Mapping template

    This maps format MIME Types to human friendly File Type descriptions.
    Essentially, it looks for a corresponding 'key' in your messages.xml of this
    format: xmlui.dri2xhtml.mimetype.{MIME Type}

    (e.g.) <message key="xmlui.dri2xhtml.mimetype.application/pdf">PDF</message>

    If a key is found, the translated value is displayed as the File Type (e.g. PDF)
    If a key is NOT found, the MIME Type is displayed by default (e.g. application/pdf)
    -->
    <xsl:template name="getFileTypeDesc">
      <xsl:param name="mimetype"/>

      <!--Build full key name for MIME type (format: xmlui.dri2xhtml.mimetype.{MIME type})-->
      <xsl:variable name="mimetype-key">xmlui.dri2xhtml.mimetype.<xsl:value-of select='$mimetype'/></xsl:variable>

      <!--Lookup the MIME Type's key in messages.xml language file.  If not found, just display MIME Type-->
      <i18n:text i18n:key="{$mimetype-key}"><xsl:value-of select="$mimetype"/></i18n:text>
    </xsl:template>


    <xsl:template name="getFileViewURL">
        <xsl:param name="file"/>
        <xsl:variable name="original-link">
            <xsl:value-of select="substring-before(mets:FLocat[@LOCTYPE='URL']/@xlink:href, '?')"/>
        </xsl:variable>

        <xsl:choose>
            <xsl:when test="@MIMETYPE = 'image/jpeg'">
                <xsl:variable name="viewer-link">
                    <xsl:value-of select="ancestor::mets:METS/@OBJID"/>
                    <xsl:text>/stream/</xsl:text>
                    <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                </xsl:variable>
                <xsl:value-of select="$viewer-link"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$original-link"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <!-- Generate the license information from the file section -->
    <xsl:template match="mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE']">
        <div class="license-info">
            <p><i18n:text>xmlui.dri2xhtml.METS-1.0.license-text</i18n:text></p>
            <ul>
                <xsl:if test="@USE='CC-LICENSE'">
                    <li><a href="{mets:file/mets:FLocat[@xlink:title='license_text']/@xlink:href}"><i18n:text>xmlui.dri2xhtml.structural.link_cc</i18n:text></a></li>
                </xsl:if>
                <xsl:if test="@USE='LICENSE'">
                    <li><a href="{mets:file/mets:FLocat[@xlink:title='license.txt']/@xlink:href}"><i18n:text>xmlui.dri2xhtml.structural.link_original_license</i18n:text></a></li>
                </xsl:if>
            </ul>
        </div>
    </xsl:template>


    <!-- Generate the logo, if present, from the file section -->
    <xsl:template match="mets:fileGrp[@USE='LOGO']">
        <!--
        <div class="ds-logo-wrapper">
            <img src="{mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href}" class="logo">
                <xsl:attribute name="alt">xmlui.dri2xhtml.METS-1.0.collection-logo-alt</xsl:attribute>
                <xsl:attribute name="attr" namespace="http://apache.org/cocoon/i18n/2.1">alt</xsl:attribute>
            </img>
        </div>
        -->
                    <img class="preview-thumbnail" alt="Thumbnail" onerror="this.onerror=null;this.src='{$bookreader-path}/images/transparent.png';">
                        <xsl:attribute name="src">
                            <xsl:value-of select="mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                        </xsl:attribute>
                        <xsl:attribute name="alt">xmlui.dri2xhtml.METS-1.0.collection-logo-alt</xsl:attribute>
                        <xsl:attribute name="attr" namespace="http://apache.org/cocoon/i18n/2.1">alt</xsl:attribute>
                    </img>
    </xsl:template>


</xsl:stylesheet>
