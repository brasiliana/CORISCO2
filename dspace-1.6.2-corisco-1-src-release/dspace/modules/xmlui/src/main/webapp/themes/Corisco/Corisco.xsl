<?xml version="1.0" encoding="UTF-8"?>

<!--
  Corisco.xsl

  Version: $Revision: 1 $

  Date: $Date: 2010-08-09 10:40:00 -0300 (Mon, 09 Aug 2010) $

-->

<!--
    Extensible stylesheet for the Brasiliana theme.
    This xsl overrides and extends the dri2xhtml of Manakin, which takes the DRI
    XML and produces the XHTML for a nice interface with a DSpace repository.
    Some of the overridden templates here just provide new ids and classes on
    tags to make the css work.  Other overridden or new templates provide new
    functionality. The purpose of each template is indicated in comments
    preceding and sometimes inside the template.

    Author: Fabio N. Kepler

    Based on a lot of other themes (including Kubrick and dri2xhtml).

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
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:fn="http://www.w3.org/2005/xpath-functions"
    xmlns="http://www.w3.org/1999/xhtml"
    exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

    <!-- Adding general theme to cover unspecified cases. -->
    <!-- <xsl:import href="../dri2xhtml.xsl"/>-->

    <!-- Structuring theme similarly to dri2xhtml. -->
    <!-- @General-Handler.xsl -->
    <xsl:import href="./xsl/bitstream-listing.xsl"/>
    <xsl:import href="./xsl/DIM-Handler.xsl"/>
    <xsl:import href="./xsl/structural.xsl"/>
    <xsl:import href="./xsl/viewer.xsl"/>
    <xsl:import href="./xsl/static.xsl"/>
<!--    <xsl:import href="./xsl/QDC-Handler.xsl"/>
    <xsl:import href="./xsl/MODS-Handler.xsl"/>-->

    <!-- Supporting XSLs. -->
    <xsl:import href="./lib/xsl/date.month-name.template.xsl"/>
    <xsl:import href="./lib/xsl/date.day-in-month.template.xsl"/>
    <xsl:import href="./lib/xsl/date.year.template.xsl"/>

    <xsl:output indent="yes"/>

    <!-- Global variables -->

    <xsl:variable name="handle-prefix" select="1918"/>

    <!--
        Context path provides easy access to the context-path parameter. This is
        used when building urls back to the site, they all must include the
        context-path paramater.
    -->
    <xsl:variable name="context-path" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>

    <!--
        Theme path represents the full path back to theme. This is usefull for
        accessing static resources such as images or javascript files. Simply
        prepend this variable and then add the file name, thus
        {$theme-path}/images/myimage.jpg will result in the full path from the
        HTTP root down to myimage.jpg. The theme path is composed of the
        "[context-path]/themes/[theme-dir]/".
    -->
    <xsl:variable name="theme-path" select="concat($context-path,'/themes/',/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path'])"/>

    <xsl:variable name="images-path" select="concat($theme-path,'/lib/img/')"/>

    <xsl:variable name="djatoka-path" select="concat($theme-path,'/lib/Djatoka/')"/>
    <xsl:variable name="bookreader-path" select="concat($theme-path,'/lib/BookReader/')"/>

    <xsl:variable name="server-url"
        select="concat(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='scheme'],
          '://',
          /dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='serverName'],
          ':',
          /dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='serverPort']
          
          )"/>

    <xsl:variable name="absolute-base-url" select="concat($server-url, $context-path)"/>
    <xsl:variable name="djatoka-resolver-url" select="concat($server-url, '/djatoka/resolver')"/>

<!--
http://143.107.81.8:8080/djatoka/resolver?url_ver=Z39.88-2004&rft_id=22491844881151006007500538804168084980&svc_id=info:lanl-repo/svc/getRegion&svc_val_fmt=info:ofi/fmt:kev:mtx:pdf&svc.format=image/png&svc.clayer=0&svc.level=0
-->
    <xsl:variable name="djatoka-thumbnail-base-url">
        <xsl:value-of select="$djatoka-resolver-url"/>
        <xsl:text>?url_ver=Z39.88-2004&amp;svc_id=info:lanl-repo/svc/getRegion&amp;svc_val_fmt=info:ofi/fmt:kev:mtx:jpg&amp;svc.format=image/png&amp;svc.clayer=0&amp;svc.level=1</xsl:text>
        <xsl:text>&amp;rft_id=</xsl:text>
<!--        &rft_id=22491844881151006007500538804168084980-->
    </xsl:variable>

</xsl:stylesheet>
