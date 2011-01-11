<?xml version="1.0" encoding="UTF-8"?>

<!--
  structural.xsl

-->

<!--
    TODO: Describe this XSL file
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
        xmlns:exsl="http://exslt.org/common"
        xmlns:fn="http://www.w3.org/2005/xpath-functions"
        xmlns="http://www.w3.org/1999/xhtml"
        extension-element-prefixes="exsl"
        exclude-result-prefixes="#default dri mets xlink xsl dim xhtml mods dc exsl fn">
    
    <xsl:output indent="yes"/>

    <xsl:variable name="string-max-length">80</xsl:variable>

    <!-- This stylesheet's purpose is to translate a DRI document to an HTML one, a task which it accomplishes
        through interative application of templates to select elements. While effort has been made to
        annotate all templates to make this stylesheet self-documenting, not all elements are used (and
        therefore described) here, and those that are may not be used to their full capacity. For this reason,
        you should consult the DRI Schema Reference manual if you intend to customize this file for your needs.
    -->

    <!--
        The starting point of any XSL processing is matching the root element. In DRI the root element is document,
        which contains a version attribute and three top level elements: body, options, meta (in that order).
        
        This template creates the html document, giving it a head and body. A title and the CSS style reference
        are placed in the html head, while the body is further split into several divs. The top-level div
        directly under html body is called "ds-main". It is further subdivided into:
            "ds-header"  - the header div containing title, subtitle, trail and other front matter
            "ds-body"    - the div containing all the content of the page; built from the contents of dri:body
            "ds-options" - the div with all the navigation and actions; built from the contents of dri:options
            "ds-footer"  - optional footer div, containing misc information
        
        The order in which the top level divisions appear may have some impact on the design of CSS and the
        final appearance of the DSpace page. While the layout of the DRI schema does favor the above div
        arrangement, nothing is preventing the designer from changing them around or adding new ones by
        overriding the dri:document template.
    -->
    <xsl:template match="dri:document">
        <html>
            <!-- First of all, build the HTML head element -->
            <xsl:call-template name="buildHead"/>
            <!-- Then proceed to the body -->
            <xsl:choose>
                <xsl:when test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='framing'][@qualifier='popup']">
                    <xsl:apply-templates select="dri:body/*"/>
                    <!-- add setup JS code if this is a choices lookup page -->
                    <xsl:if test="dri:body/dri:div[@n='lookup']">
                        <xsl:call-template name="choiceLookupPopUpSetup"/>
                    </xsl:if>
                </xsl:when>
                <xsl:otherwise>
                    <body>

                        <div id="pagina">
                        <!--
                            The header div, complete with title, subtitle, trail and other junk. The trail is
                            built by applying a template over pageMeta's trail children. -->
                            <xsl:call-template name="buildHeader"/>
                    
                            <!--
                                Goes over the document tag's children elements: body, options, meta. The body template
                                generates the ds-body div that contains all the content. The options template generates
                                the ds-options div that contains the navigation and action options available to the
                                user. The meta element is ignored since its contents are not processed directly, but
                                instead referenced from the different points in the document. -->
<!--                            <xsl:apply-templates />-->
                            <xsl:apply-templates select="*[not(name()='options')]" />

                            <!--
                                The footer div, dropping whatever extra information is needed on the page. It will
                                most likely be something similar in structure to the currently given example. -->
                            <xsl:call-template name="buildFooter"/>
                    
                        </div>
                    </body>
                </xsl:otherwise>
            </xsl:choose>
        </html>
    </xsl:template>
    
    
    <!-- The HTML head element contains references to CSS as well as embedded JavaScript code. Most of this
        information is either user-provided bits of post-processing (as in the case of the JavaScript), or
        references to stylesheets pulled directly from the pageMeta element. -->
    <xsl:template name="buildHead">
        <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
            <meta name="Generator">
                <xsl:attribute name="content">
                    <xsl:text>DSpace</xsl:text>
                    <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='dspace'][@qualifier='version']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='dspace'][@qualifier='version']"/>
                    </xsl:if>
                </xsl:attribute>
            </meta>
            <!-- Add stylsheets -->
            <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='stylesheet']">
                <link rel="stylesheet" type="text/css">
                    <xsl:attribute name="media">
                        <xsl:value-of select="@qualifier"/>
                    </xsl:attribute>
                    <xsl:attribute name="href">
                        <xsl:value-of select="$theme-path"/>
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:attribute>
                </link>
            </xsl:for-each>
            
            <!-- Add syndication feeds -->
            <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='feed']">
                <link rel="alternate" type="application">
                    <xsl:attribute name="type">
                        <xsl:text>application/</xsl:text>
                        <xsl:value-of select="@qualifier"/>
                    </xsl:attribute>
                    <xsl:attribute name="href">
                        <xsl:value-of select="."/>
                    </xsl:attribute>
                </link>
            </xsl:for-each>
            
            <!--  Add OpenSearch auto-discovery link -->
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='opensearch'][@qualifier='shortName']">
                <link rel="search" type="application/opensearchdescription+xml">
                    <xsl:attribute name="href">
                        <xsl:value-of select="$absolute-base-url"/>
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='opensearch'][@qualifier='context']"/>
                        <xsl:text>description.xml</xsl:text>
                    </xsl:attribute>
                    <xsl:attribute name="title" >
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='opensearch'][@qualifier='shortName']"/>
                    </xsl:attribute>
                </link>
            </xsl:if>
            
            <!-- The following javascript removes the default text of empty text areas when they are focused on or submitted -->
            <!-- There is also javascript to disable submitting a form when the 'enter' key is pressed. -->
            <script type="text/javascript">
                                //Clear default text of emty text areas on focus
                                function tFocus(element)
                                {
                                        if (element.value == '<i18n:text>xmlui.dri2xhtml.default.textarea.value</i18n:text>'){element.value='';}
                                }
                                //Clear default text of emty text areas on submit
                                function tSubmit(form)
                                {
                                        var defaultedElements = document.getElementsByTagName("textarea");
                                        for (var i=0; i != defaultedElements.length; i++){
                                                if (defaultedElements[i].value == '<i18n:text>xmlui.dri2xhtml.default.textarea.value</i18n:text>'){
                                                        defaultedElements[i].value='';}}
                                }
                                //Disable pressing 'enter' key to submit a form (otherwise pressing 'enter' causes a submission to start over)
                                function disableEnterKey(e)
                                {
                                     var key;
                                
                                     if(window.event)
                                          key = window.event.keyCode;     //Internet Explorer
                                     else
                                          key = e.which;     //Firefox and Netscape
                                
                                     if(key == 13)  //if "Enter" pressed, then disable!
                                          return false;
                                     else
                                          return true;
                                }
            </script>
            
            <!-- add "external" javascript from static, path is absolute-->
            <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='javascript'][@qualifier='external']">
                <script type="text/javascript">
                    <xsl:attribute name="src">
                        <xsl:value-of select="."/>
                    </xsl:attribute>&#160;
                </script>
            </xsl:for-each>

            <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='javascript'][@qualifier='plugin']">
                <script type="text/javascript">
                    <xsl:attribute name="src">
                        <xsl:value-of select="$theme-path"/>
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:attribute>&#160;
                </script>
            </xsl:for-each>

            <!-- add "shared" javascript from static, path is relative to webapp root-->
            <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='javascript'][@qualifier='static']">
                <script type="text/javascript">
                    <xsl:attribute name="src">
                        <xsl:value-of select="$context-path"/>
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:attribute>&#160;
                </script>
            </xsl:for-each>
            
            <!-- Add theme javascipt  -->
            <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='javascript'][not(@qualifier)]">
                <script type="text/javascript">
                    <xsl:attribute name="src">
                        <xsl:value-of select="$theme-path"/>
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:attribute>&#160;
                </script>
            </xsl:for-each>


            <!-- Add a google analytics script if the key is present -->
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='google'][@qualifier='analytics']">
                <script type="text/javascript">
                    <xsl:text>var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");</xsl:text>
                    <xsl:text>document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));</xsl:text>
                </script>

                <script type="text/javascript">
                    <xsl:text>try {</xsl:text>
                    <xsl:text>var pageTracker = _gat._getTracker("</xsl:text>
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='google'][@qualifier='analytics']"/>
                    <xsl:text>");</xsl:text>
                    <xsl:text>pageTracker._trackPageview();</xsl:text>
                    <xsl:text>} catch(err) {}</xsl:text>
                </script>
            </xsl:if>
            
            
            <!-- Add the title in -->
            <xsl:variable name="page_title" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='title']" />
            <title>
                <xsl:choose>
                    <xsl:when test="not($page_title)">
                        <xsl:text>  </xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:copy-of select="$page_title/node()" />
                    </xsl:otherwise>
                </xsl:choose>
            </title>

            <!-- Head metadata in item pages -->
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='xhtml_head_item']">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='xhtml_head_item']"
                              disable-output-escaping="yes"/>
            </xsl:if>
            
        </head>
    </xsl:template>
    
    
    <!-- The header (distinct from the HTML head element) contains the title, subtitle, login box and various
        placeholders for header images -->
    <xsl:template name="buildHeader">
        <div id="cabecalho">
            <xsl:call-template name="cabecalho-menu"/>

            <div id="cabecalho-barra">
                <div id="detalhes-site">
                    <h1 id="site-logo">
                        <a>
                            <xsl:attribute name="href">
                                <xsl:value-of select="$context-path"/>
                                <xsl:text>/</xsl:text>
                            </xsl:attribute>
                            <img id="imagem-logo">
                                <xsl:attribute name="src">
                                    <xsl:value-of select="$images-path"/>
                                    <xsl:text>logo.png</xsl:text>
                                </xsl:attribute>
                            </img>
                        </a>
                    </h1>
<!--                    <h1 id="site-nome">
                        <xsl:choose>
                             protectiotion against an empty page title 
                            <xsl:when test="not(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='title'])">
                                <xsl:text> </xsl:text>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:copy-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='title']/node()"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </h1>-->
<!--
                    <h2 class="static-pagetitle">
                        <i18n:text>xmlui.dri2xhtml.structural.head-subtitle</i18n:text>
                    </h2>
-->
<!--
                    <ul id="ds-trail">
                        <xsl:choose>
                            <xsl:when test="count(/dri:document/dri:meta/dri:pageMeta/dri:trail) = 0">
                                <li class="ds-trail-link first-link"> - </li>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:apply-templates select="/dri:document/dri:meta/dri:pageMeta/dri:trail"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </ul>
-->
                </div>

                <div id="cabecalho-busca">
                    <form id="formulario-busca-cabecalho" method="get">
                        <xsl:attribute name="action">
                            <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='simpleURL']"/>
                        </xsl:attribute>

                        <xsl:apply-templates select="/dri:document/dri:options/dri:list[@n='top-search']" mode="header"/>


<!-- Changing search type: instead of scoped search by collection, we're going to do scoped search by filter.
                        <fieldset>
                            <input id="caixa-busca" type="text">
                                <xsl:attribute name="name">
                                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='queryField']"/>
                                </xsl:attribute>
                                <xsl:choose>
                                    <xsl:when test="//dri:field[@id='aspect.discovery.SimpleSearch.field.query']">
                                        <xsl:attribute name="value">
                                            <xsl:value-of select="//dri:field[@id='aspect.discovery.SimpleSearch.field.query']/dri:value"/>
                                        </xsl:attribute>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:attribute name="i18n:attr">value</xsl:attribute>
                                        <xsl:attribute name="value">xmlui.ArtifactBrowser.AbstractSearch.main_search_box_default</xsl:attribute>
                                    </xsl:otherwise>
                                </xsl:choose>
                                <xsl:attribute name="onFocus"><xsl:text>clearText(this)</xsl:text></xsl:attribute>
                                <xsl:attribute name="onBlur"><xsl:text>clearText(this)</xsl:text></xsl:attribute>
                            </input>

                            <div id="!selecionar-filtro">
                                <xsl:variable name="sub-groups">
                                    <xsl:call-template name="getFirstLevelGroupTitles"/>
                                </xsl:variable>

                                <select id="selecionar-filtro" class="selecionar-display" name="scope">
                                    <option value="">
                                        <i18n:text>xmlui.ArtifactBrowser.CommunityViewer.all_of_dspace</i18n:text>
                                    </option>
                                    <xsl:for-each select="exsl:node-set($sub-groups)/*">
                                        <option>
                                            <xsl:attribute name="value">
                                                <xsl:value-of select="@handle"/>
                                            </xsl:attribute>
                                            <xsl:value-of select="."/>
                                        </option>
                                    </xsl:for-each>
                                </select>
                            </div>

                            <input id="botao-enviar" class="ds-button-field " name="submit" type="submit" i18n:attr="value" value="xmlui.general.go" >
                                <xsl:attribute name="onclick">
                                    <xsl:text>
                                            var radio = document.getElementById(&quot;ds-search-form-scope-container&quot;);
                                            if (radio != undefined &amp;&amp; radio.checked)
                                            {
                                            var form = document.getElementById(&quot;ds-search-form&quot;);
                                            form.action=
                                    </xsl:text>
                                    <xsl:text>&quot;</xsl:text>
                                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath']"/>
                                    <xsl:text>/handle/&quot; + radio.value + &quot;/search&quot; ; </xsl:text>
                                    <xsl:text>
                                            }
                                    </xsl:text>
                                </xsl:attribute>
                            </input>

                        </fieldset>-->
                    </form>
                </div>
            </div>

            <xsl:choose>
                <xsl:when test="/dri:document/dri:meta/dri:userMeta/@authenticated = 'yes'">
                    <div id="ds-user-box">
                        <p>
                            <a>
                                <xsl:attribute name="href">
                                    <xsl:value-of select="/dri:document/dri:meta/dri:userMeta/
                                        dri:metadata[@element='identifier' and @qualifier='url']"/>
                                </xsl:attribute>
                                <i18n:text>xmlui.dri2xhtml.structural.profile</i18n:text>
                                <xsl:value-of select="/dri:document/dri:meta/dri:userMeta/
                                    dri:metadata[@element='identifier' and @qualifier='firstName']"/>
                                <xsl:text> </xsl:text>
                                <xsl:value-of select="/dri:document/dri:meta/dri:userMeta/
                                    dri:metadata[@element='identifier' and @qualifier='lastName']"/>
                            </a>
                            <xsl:text> | </xsl:text>
                            <a>
                                <xsl:attribute name="href">
                                    <xsl:value-of select="/dri:document/dri:meta/dri:userMeta/
                                        dri:metadata[@element='identifier' and @qualifier='logoutURL']"/>
                                </xsl:attribute>
                                <i18n:text>xmlui.dri2xhtml.structural.logout</i18n:text>
                            </a>
                        </p>
                    </div>
                </xsl:when>
                <xsl:otherwise>
<!--                    <div id="ds-user-box">
                        <p>
                            <a>
                                <xsl:attribute name="href">
                                    <xsl:value-of select="/dri:document/dri:meta/dri:userMeta/
                                        dri:metadata[@element='identifier' and @qualifier='loginURL']"/>
                                </xsl:attribute>
                                <i18n:text>xmlui.dri2xhtml.structural.login</i18n:text>
                            </a>
                        </p>
                    </div>-->
                </xsl:otherwise>
            </xsl:choose>

        </div>
    </xsl:template>
    
    
    <!-- Like the header, the footer contains various miscellanious text, links, and image placeholders -->
    <xsl:template name="buildFooter">
        <div id="rodape">
            <i18n:text>xmlui.dri2xhtml.structural.footer-promotional</i18n:text>
            <div id="ds-footer-links">
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                        <xsl:text>/contact</xsl:text>
                    </xsl:attribute>
                    <i18n:text>xmlui.dri2xhtml.structural.contact-link</i18n:text>
                </a>
                <xsl:text> | </xsl:text>
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                        <xsl:text>/feedback</xsl:text>
                    </xsl:attribute>
                    <i18n:text>xmlui.dri2xhtml.structural.feedback-link</i18n:text>
                </a>
            </div>
            <!--Invisible link to HTML sitemap (for search engines) -->
            <a>
                <xsl:attribute name="href">
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                    <xsl:text>/htmlmap</xsl:text>
                </xsl:attribute>
                &#160;
            </a>
        </div>
        <!--
            <a href="http://di.tamu.edu">
                <div id="ds-footer-logo"></div>
            </a>
            <p>
            This website is using Manakin, a new front end for DSpace created by Texas A&amp;M University
            Libraries. The interface can be extensively modified through Manakin Aspects and XSL based Themes.
            For more information visit
            <a href="http://di.tamu.edu">http://di.tamu.edu</a> and
            <a href="http://dspace.org">http://dspace.org</a>
            </p>
        -->
    </xsl:template>
    
    
    
    <!--
        The trail is built one link at a time. Each link is given the ds-trail-link class, with the first and
        the last links given an additional descriptor.
    -->
    <xsl:template match="dri:trail">
        <li>
            <xsl:attribute name="class">
                <xsl:text>ds-trail-link </xsl:text>
                <xsl:if test="position()=1">
                    <xsl:text>first-link </xsl:text>
                </xsl:if>
                <xsl:if test="position()=last()">
                    <xsl:text>last-link</xsl:text>
                </xsl:if>
            </xsl:attribute>
            <!-- Determine whether we are dealing with a link or plain text trail link -->
            <xsl:choose>
                <xsl:when test="./@target">
                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of select="./@target"/>
                        </xsl:attribute>
                        <xsl:apply-templates />
                    </a>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates />
                </xsl:otherwise>
            </xsl:choose>
        </li>
    </xsl:template>


    
    
<!--
        The meta, body, options elements; the three top-level elements in the schema
-->
    
    
    
    
    <!--
        The template to handle the dri:body element. It simply creates the ds-body div and applies
        templates of the body's child elements (which consists entirely of dri:div tags).
    -->
    <xsl:template match="dri:body">
<!--            <form id="formulario-busca" method="post">
                <xsl:attribute name="action">
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='simpleURL']"/>
                </xsl:attribute>-->

        <div id="conteudo">
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']">
                <div id="ds-system-wide-alert">
                    <p>
                        <xsl:copy-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']/node()"/>
                    </p>
                </div>
            </xsl:if>

<!--            STOP-->

            <form>
                <xsl:apply-templates select="//dri:div[@interactive='yes'][@n='general-query']" mode="formAttributes" />
                <xsl:attribute name="id">formulario-busca</xsl:attribute>
                <xsl:attribute name="method">GET</xsl:attribute>


<!--                <xsl:for-each select="//dri:div[@interactive='yes'][@n='general-query']/*[not(name()='head')]">
                    <xsl:value-of select="name()"/>
                    <xsl:apply-templates select="."/>
                </xsl:for-each>-->

<!--                <xsl:apply-templates select="//dri:div[@interactive='yes'][@n='general-query']/dri:list"/>-->
<!--                <xsl:apply-templates select="//dri:div[@interactive='yes'][@n='general-query']/dri:table"/>-->
<!--                <xsl:apply-templates select="//dri:div[@interactive='yes'][@n='general-query']/dri:p"/>-->

<!--                <input type="submit" i18n:attr="value" value="xmlui.general.add_filter" name="{concat('submit_',@n,'-filter-controls_add')}"/>-->

                <xsl:call-template name="conteudo-alto">
                    <xsl:with-param name="body" select="."/>
                </xsl:call-template>
                <xsl:call-template name="conteudo-central">
                    <xsl:with-param name="body" select="."/>
                </xsl:call-template>

            </form>

            <xsl:call-template name="conteudo-baixo">
                <xsl:with-param name="body" select="."/>
            </xsl:call-template>

<!--            <xsl:apply-templates />-->

        </div>
<!--        </form>-->
    </xsl:template>


    <xsl:template match="dri:div[@interactive='yes'][@n='general-query']" mode="formAttributes" priority="2">
<!--        <xsl:apply-templates select="dri:head"/>-->
<!--        <form>-->
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-interactive-div</xsl:with-param>
            </xsl:call-template>
            <xsl:attribute name="action">
                <xsl:value-of select="@action"/>
            </xsl:attribute>
            <xsl:attribute name="method">
                <xsl:value-of select="@method"/>
            </xsl:attribute>
            <xsl:if test="@method='multipart'">
                <xsl:attribute name="method">post</xsl:attribute>
                <xsl:attribute name="enctype">multipart/form-data</xsl:attribute>
            </xsl:if>
<!--            <xsl:attribute name="onsubmit">javascript:tSubmit(this);</xsl:attribute>-->

                        <!--For Item Submission process, disable ability to submit a form by pressing 'Enter'-->
<!--            <xsl:if test="starts-with(@n,'submit')">
                <xsl:attribute name="onkeydown">javascript:return disableEnterKey(event);</xsl:attribute>
            </xsl:if>-->

<!--            <xsl:apply-templates select="*[not(name()='head')]"/>-->

<!--        </form>-->
        <!-- JS to scroll form to DIV parent of "Add" button if jump-to -->
        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='jumpTo']">
            <script type="text/javascript">
                <xsl:text>var button = document.getElementById('</xsl:text>
                <xsl:value-of select="translate(@id,'.','_')"/>
                <xsl:text>').elements['</xsl:text>
                <xsl:value-of select="concat('submit_',/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='jumpTo'],'_add')"/>
                <xsl:text>'];</xsl:text>
                <xsl:text>
                      if (button != null) {
                        var n = button.parentNode;
                        for (; n != null; n = n.parentNode) {
                            if (n.tagName == 'DIV') {
                              n.scrollIntoView(false);
                              break;
                           }
                        }
                      }
                </xsl:text>
            </script>
        </xsl:if>
    </xsl:template>


    <!--
        The template to handle dri:options. Since it contains only dri:list tags (which carry the actual
        information), the only things than need to be done is creating the ds-options div and applying
        the templates inside it.
        
        In fact, the only bit of real work this template does is add the search box, which has to be
        handled specially in that it is not actually included in the options div, and is instead built
        from metadata available under pageMeta.
    -->
    <!-- TODO: figure out why i18n tags break the go button -->
    <xsl:template match="dri:options">
<!--        <div id="ds-options">-->
        <div id="coluna-filtros">
            <xsl:apply-templates />
<!--            <xsl:call-template name="coluna-filtros"/>-->
        </div>
    </xsl:template>


    <!-- Currently the dri:meta element is not parsed directly. Instead, parts of it are referenced from inside
        other elements (like reference). The blank template below ends the execution of the meta branch -->
    <xsl:template match="dri:meta">
    </xsl:template>
    
    <!-- Meta's children: userMeta, pageMeta, objectMeta and repositoryMeta may or may not have templates of
        their own. This depends on the meta template implementation, which currently does not go this deep.
    <xsl:template match="dri:userMeta" />
    <xsl:template match="dri:pageMeta" />
    <xsl:template match="dri:objectMeta" />
    <xsl:template match="dri:repositoryMeta" />
    -->
    

<!-- dri:body structure -->


    <!-- Discovery structure. -->
    <xsl:template match="dri:div[@id='aspect.discovery.SiteRecentSubmissions.div.site-recent-submission']" priority="10">
<!--        SITE-RECENT-SUBMISSION-->
<!--        <xsl:apply-templates select="dri:head"/>-->
        <xsl:apply-templates select="*[not(name()='head')]"/>
    </xsl:template>


    <xsl:template match="dri:div[@id='aspect.discovery.CommunitySearch.div.community-search-browse']">
        <xsl:apply-templates select="dri:div[@id='aspect.discovery.CommunitySearch.div.community-search']"/>
        <xsl:apply-templates select="dri:div[@id='aspect.discovery.CommunitySearch.div.community-browse']"/>
    </xsl:template>

    <xsl:template match="dri:div[@id='aspect.discovery.CommunitySearch.div.community-search']" priority="10">
        COMMUNITY-SEARCH
    </xsl:template>

    <xsl:template match="dri:div[@id='aspect.discovery.CommunitySearch.div.community-browse']" priority="10">
        COMMUNITY-BROWSE
    </xsl:template>

    <xsl:template match="dri:div[@id='aspect.discovery.CommunityViewer.div.community-home']" priority="10">
<!--        COMMUNITY-HOME-->
<!--        <xsl:apply-templates select="dri:head"/>-->
        <xsl:apply-templates select="dri:div[@id='aspect.discovery.CommunityViewer.div.community-view']"/>
        <xsl:apply-templates select="dri:div[@id='aspect.discovery.CommunityRecentSubmissions.div.community-recent-submission']"/>
    </xsl:template>

    <xsl:template match="dri:div[@id='aspect.discovery.CommunityViewer.div.community-view']" priority="10">
<!--        COMMUNITY-VIEW-->
    </xsl:template>

    <xsl:template match="dri:div[@id='aspect.discovery.CommunityRecentSubmissions.div.community-recent-submission']" priority="10">
<!--        COMMUNITY-RECENT-SUBMISSION-->
<!--        <xsl:apply-templates select="dri:head"/>-->
        <xsl:apply-templates select="*[not(name()='head')]"/>
    </xsl:template>


    <xsl:template match="dri:div[@id='aspect.discovery.CollectionSearch.div.collection-search-browse']">
        <xsl:apply-templates select="dri:div[@id='aspect.discovery.CollectionSearch.div.collection-search']"/>
        <xsl:apply-templates select="dri:div[@id='aspect.discovery.CollectionSearch.div.collection-browse']"/>
    </xsl:template>

    <xsl:template match="dri:div[@id='aspect.discovery.CollectionSearch.div.collection-search']" priority="10">
        COLLECTION-SEARCH
    </xsl:template>

    <xsl:template match="dri:div[@id='aspect.discovery.CollectionSearch.div.collection-browse']" priority="10">
        COLLECTION-BROWSE
    </xsl:template>

    <xsl:template match="dri:div[@id='aspect.discovery.CollectionViewer.div.collection-home']" priority="10">
<!--        COLLECTION-HOME-->
<!--        <xsl:apply-templates select="dri:head"/>-->
        <xsl:apply-templates select="dri:div[@id='aspect.discovery.CollectionViewer.div.collection-view']"/>
        <xsl:apply-templates select="dri:div[@id='aspect.discovery.CollectionRecentSubmissions.div.collection-recent-submission']"/>
    </xsl:template>

    <xsl:template match="dri:div[@id='aspect.discovery.CollectionViewer.div.collection-view']" priority="10">
<!--        COLLECTION-VIEW-->
    </xsl:template>

    <xsl:template match="dri:div[@id='aspect.discovery.CollectionRecentSubmissions.div.collection-recent-submission']" priority="10">
<!--        COLLECTION-RECENT-SUBMISSION-->
<!--        <xsl:apply-templates select="dri:head"/>-->
        <xsl:apply-templates select="*[not(name()='head')]"/>
    </xsl:template>


    <!-- SECTION: search -->
    <xsl:template match="dri:div[@id='aspect.discovery.SimpleSearch.div.search']" priority="10">
<!--        <xsl:apply-templates select="dri:head"/>-->
        <xsl:apply-templates select="*[not(name()='head')]"/>
    </xsl:template>

    <xsl:template match="dri:div[@id='aspect.discovery.SimpleSearch.div.search-results']" priority="10">
<!--        <xsl:apply-templates select="dri:head"/>-->
        <xsl:apply-templates select="*[not(name()='head')]"/>
    </xsl:template>

    <xsl:template match="dri:div[@id='aspect.discovery.SimpleSearch.div.search-results']/dri:p" priority="10">
        <div id="sem-resultados" class="borda">
            <xsl:apply-templates />
        </div>
    </xsl:template>



    <xsl:template name="conteudo-alto">
        <xsl:param name="body"/>

        <div id="conteudo-alto">
            <xsl:choose>
                <xsl:when test="$body//dri:div[@rend='secondary recent-submission']">
                    <div id="dados-item">
                        <h3>
                            <span id="nome-item">
                                <xsl:apply-templates select="$body//dri:div[@rend='secondary recent-submission']/dri:head"/>
                            </span>
                        </h3>
                    </div>
                </xsl:when>

                        
                <xsl:when test="$body//dri:div[@id='aspect.discovery.SimpleSearch.div.search-results']">
                    <div id="dados-item">
                        <h3>
<!--                            <span id="nome-item"><xsl:apply-templates select="$body//dri:p[@id='aspect.discovery.SimpleSearch.p.result-query']/i18n:translate"/></span>-->
                            <span id="nome-item">
                                <xsl:choose>
                                    <!-- If query is empty and there is no facet filter, display a possible different header. -->
                                    <xsl:when test="//dri:body//dri:field[@id='aspect.discovery.SimpleSearch.field.query']/dri:value = ''
                                            and not(//dri:div[@interactive='yes'][@n='general-query']/dri:list[@n='search-query']/dri:item/dri:field[@n='facet-controls'])">
                                        <i18n:text>xmlui.ArtifactBrowser.AbstractSearch.result_empty_query_head</i18n:text>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <i18n:text>xmlui.ArtifactBrowser.AbstractSearch.result_query_head</i18n:text>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </span>
<!--                            <span id="autoria-item"><xsl:copy-of select="$body//dri:p[@n='result-query'][@rend='result-query']"/></span>-->
                            <span id="autoria-item">
                                <xsl:value-of select="$body//dri:field[@id='aspect.discovery.SimpleSearch.field.query']/dri:value"/>
                                <xsl:apply-templates select="//dri:div[@interactive='yes'][@n='general-query']/dri:list[@n='search-query']/dri:item/dri:field[@n='query']" mode="dados-item"/>
                                <xsl:apply-templates select="//dri:div[@interactive='yes'][@n='general-query']/dri:list[@n='search-query']/dri:item/dri:field[@n='facet-controls']/dri:field" mode="dados-item"/>
                            </span>
                        </h3>
                    </div>
                </xsl:when>


                <xsl:when test="$body/dri:div[starts-with(@n, 'browse-by')]">
                    <div id="dados-item">
                        <h3>
                            <span id="nome-item">
                                <i18n:text>xmlui.ArtifactBrowser.AbstractSearch.browse_head</i18n:text>
                            </span>
                            <span id="autoria-item">
                                <xsl:apply-templates select="$body/dri:div[starts-with(@n, 'browse-by')]/dri:head"/>
                            </span>
                        </h3>
                    </div>
                </xsl:when>


                <xsl:when test="/dri:document/dri:body[@n='item-view']">

                    <xsl:apply-templates select="/dri:document/dri:body/dri:div/dri:referenceSet/dri:reference" mode="headDetailView"/>

                </xsl:when>


                <xsl:otherwise>
                    <div id="dados-item">
                        <h3>
                            <span id="nome-item"></span>
<!--                            <span id="nome-item"><i18n:text>xmlui.ArtifactBrowser.AbstractSearch.result_query_head</i18n:text></span>
                            <span id="autoria-item">palavras utilizadas na busca</span>-->
                        </h3>
                    </div>
                </xsl:otherwise>
            </xsl:choose>


            <xsl:call-template name="caixa-barra"/>
<!--            <xsl:apply-templates select="$body/dri:div[@n='search']//dri:table[@n='search-controls']"/>-->
        </div>
    </xsl:template>


    <xsl:template name="conteudo-central">
        <xsl:param name="body"/>
        <div id="conteudo-central">
            <xsl:choose>

                <xsl:when test="$body[@n='item-view']">
                    <xsl:call-template name="itemViewer"/>
                </xsl:when>

                <xsl:otherwise>
                    <div id="coluna-resultados">
                        <xsl:if test="//dri:options/dri:list[@n='discovery-location']">
                            <div id="abas">
        <!--                    <xsl:apply-templates select="//dri:referenceSet[@type = 'summaryList'][@n = 'community-browser']"/>-->
        <!--                    <xsl:call-template name="abas"/>-->
                                <xsl:apply-templates select="//dri:options/dri:list[@n='discovery-location']" mode="tabList"/>
                            </div>
                        </xsl:if>

        <!--                <xsl:call-template name="resultados"/>-->
                        <xsl:apply-templates select="$body/dri:div"/>
                    </div>

                    <xsl:apply-templates select="//dri:options"/>
                </xsl:otherwise>
            </xsl:choose>
        </div>
    </xsl:template>


    <xsl:template name="conteudo-baixo">
        <xsl:param name="body"/>
        <div id="conteudo-baixo" class="borda">
            <xsl:call-template name="caixa-barra"/>
<!--            <xsl:apply-templates select="$body/dri:div[@n='search']//dri:table[@n='search-controls']"/>-->
        </div>
    </xsl:template>


    <xsl:template match="mets:METS" mode="getID">
        <xsl:value-of select="substring-after(@ID, ':')"/>
    </xsl:template>


<!--TODO-->
    <xsl:template name="caixa-barra">
        <xsl:choose>
            <xsl:when test="/dri:document/dri:body/dri:div[@n='search']">
                <xsl:apply-templates select="//dri:body/dri:div[@n='search']//dri:table[@n='search-controls']"/>
            </xsl:when>
            <xsl:when test="/dri:document/dri:body/dri:div[starts-with(@n, 'browse-by-')]">
<!--                <xsl:apply-templates select="//dri:body/dri:div[starts-with(@n, 'browse-by-')]//dri:table[@n='browse-controls']"/>-->
                <xsl:apply-templates select="//dri:body/dri:div[starts-with(@n, 'browse-by-')]/dri:div[@n='browse-controls']"/>
            </xsl:when>
            <xsl:when test="/dri:document/dri:body[@n='item-view']">
                <xsl:comment>Empty</xsl:comment>
<!--                <xsl:apply-templates select="/dri:document/dri:body[@n='item-view']/dri:div" mode="viewControls"/>-->

<!--                <xsl:call-template name="viewControls"/>-->
            </xsl:when>
            <xsl:otherwise>
                <xsl:comment>Empty</xsl:comment>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    
    <xsl:template match="dri:table[@n='search-controls']" priority="4">
        <div class="caixa">
            <xsl:apply-templates select="//dri:div[@interactive='yes'][@n='general-query']/dri:table//dri:cell[dri:field[@n='view']]" mode="searchControls"/>

<!--            <xsl:call-template name="numero-resultados"/>-->
                <xsl:apply-templates select="//dri:div[@n='search-results']/@pagination"/>

            <xsl:apply-templates select="//dri:div[@interactive='yes'][@n='general-query']/dri:table//dri:cell[not(dri:field[@n='view'])]" mode="searchControls"/>
        </div>
    </xsl:template>


    <xsl:template match="dri:div[@n='browse-controls']" priority="1">
        <div class="caixa">
            <xsl:apply-templates select="dri:p[@rend='hidden']"/>
            <xsl:apply-templates select="dri:table"/>
        </div>
    </xsl:template>


    <xsl:template name="viewControls">
        <div class="caixa" id="visualizador-barra">
            <div class="modos_visualizador">
                <span class="barra-texto">Modos de visualização:</span>
                <span class="pagina-unica">
                    <img>
                        <xsl:attribute name="src">
                            <xsl:value-of select="$images-path"/>
                            <xsl:text>/modo_pg_unicaON.png</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="onClick">
                            <xsl:text>br.switchMode(1); return false;</xsl:text>
                        </xsl:attribute>
                    </img>
                </span>
                <span class="mosaico">
                    <img>
                        <xsl:attribute name="src">
                            <xsl:value-of select="$images-path"/>
                            <xsl:text>/modo_mosaico.png</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="onClick">
                            <xsl:text>br.switchMode(3); return false;</xsl:text>
                        </xsl:attribute>
                    </img>
                </span>
                <span class="pagina-dupla">
                    <img>
                        <xsl:attribute name="src">
                            <xsl:value-of select="$images-path"/>
                            <xsl:text>/modo_pg_dupla.png</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="onClick">
                            <xsl:text>br.switchMode(2); return false;</xsl:text>
                        </xsl:attribute>
                    </img>
                </span>
                <!-- adicionei classe OCR -->
                <span class="ocr">
                    <img>
                        <xsl:attribute name="src">
                            <xsl:value-of select="$images-path"/>
                            <xsl:text>/modo_ocr.png</xsl:text>
                        </xsl:attribute>
<!--                        <xsl:attribute name="onClick">
                            <xsl:text>br.switchMode(2); return false;</xsl:text>
                        </xsl:attribute>-->
                    </img>
                </span>
            </div>
            <div class="paginacao">
                <span class="barra-texto">Páginas</span>
						<!-- tirei ul e deixei span pra poder alinhar centralizado e porque não sei se isso consiste em uma lista
						tirei span paginacao links
						mudei o modo de navegação... vejam se vocês gostam. achei o outro meio fora do usual -->
                <span class="primeira-pagina">
                    <a>&lt;&lt;</a>
                </span>
                <span class="pagina-anterior">
                    <a>&lt;</a>
                </span>
                <span class="pagina-link">
                    <a>5</a>
                </span>
                <span class="pagina-link">
                    <a>6</a>
                </span>
                <span class="pagina-atual">
                    <a>7</a>
                </span>
                <span class="pagina-link">
                    <a>8</a>
                </span>
                <span class="proxima-pagina">
                    <a>&gt;</a>
                </span>
                <span class="ultima-pagina">
                    <a>&gt;&gt;</a>
                </span>
            </div>
					<!-- utilizei classe resultados-pagina, como no resultado de busca -->
            <div class="resultados-pagina">
                <span class="barra-texto">Ir à página:</span>
                <select class="selecionar">
                    <option>1</option>
                    <option>2</option>
                    <option>3</option>
                    <option>4</option>
                    <option>5</option>
                </select>
            </div>
            <div class="funcional">
                <span class="imprimir">
                    <img src="{$images-path}/func_imprimir.png"/>
                </span>
                <span class="baixar">
                    <img src="{$images-path}/func_baixar.png"/>
                </span>
                <span class="citar">
                    <img src="{$images-path}/func_citar.png"/>
                </span>
                <span class="linkar">
                    <img src="{$images-path}/func_link.png"/>
                </span>
            </div>
        </div>
    </xsl:template>


    <xsl:template name="itemViewer">
        <div id="visualizador-paginas">
            <!--Different types of viewers-->
<!--            <div id="pagina-unica" style="display: block; ">-->
                <xsl:apply-templates select="/dri:document/dri:body/dri:div/dri:referenceSet"/>
<!--                <div id="trilho-pagina" class="borda">
                </div>-->
<!--            </div>-->
        </div>
    </xsl:template>



    

    <xsl:template match="dri:table[@n='browse-controls']" priority="1">
            <xsl:apply-templates select="dri:row/dri:cell[dri:field[@n='view']]" mode="browseControls"/>

<!--            <xsl:call-template name="numero-resultados"/>-->
<!--            <xsl:apply-templates select="//dri:div[starts-with(@n, 'browse-by-')][fn:ends-with(fn:string(@n), '-results')]/@pagination"/>-->
            <xsl:apply-templates select="//dri:div[fn:matches(string(@n), '^browse-by-.+-results$')]/@pagination"/>

            <xsl:apply-templates select="dri:row/dri:cell[not(dri:field[@n='view'])]" mode="browseControls"/>
    </xsl:template>

    <xsl:template match="dri:table[starts-with(@n, 'browse-by-')]">
        <ul>
            <xsl:for-each select="dri:row">
                <li>
                    <xsl:apply-templates />
                </li>
            </xsl:for-each>
        </ul>
    </xsl:template>
    
    <xsl:template match="dri:div[@rend='cloud']/dri:table[starts-with(@n, 'browse-by-')]">
        CLOUD
    </xsl:template>


    <xsl:template match="dri:item" mode="tabList">
        <xsl:choose>
            <xsl:when test="@rend='selected'">
                <div id="aba-ativa" class="borda">
                    <span>
                        <xsl:value-of select="."/>
                    </span>
                </div>
            </xsl:when>
            <xsl:when test="@rend='disabled'">
                <div class="borda">
                    <h4>
                        <span class="cor2" id="zero">
                            <xsl:value-of select="."/>
                        </span>
                    </h4>
                </div>
            </xsl:when>
            <xsl:otherwise>
                <div class="borda">
                    <h4>
                        <span class="cor2">
                            <a>
                                <xsl:attribute name="href">
                                    <xsl:value-of select="dri:xref/@target"/>
                                </xsl:attribute>
                                <xsl:choose>
                                    <xsl:when test="string-length(dri:xref) &gt; 0">
                                        <xsl:value-of select="dri:xref"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </a>
                        </span>
                    </h4>
                </div>
            </xsl:otherwise>
        </xsl:choose>

		<!--Display collection strengths (item counts) if they exist-->
<!--        <span class="total-colecao">
            <xsl:if test="string-length(dri:xref) &gt; 0">
                <xsl:text>(</xsl:text>
                <xsl:value-of select="dri:xref"/>
                <xsl:text>)</xsl:text>
            </xsl:if>
        </span>-->
    </xsl:template>


    <xsl:template match="dri:list" mode="tabList">
<!--        <xsl:variable name="current-tab">
            <xsl:value-of select="substring-after(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='container'], ':')"/>
        </xsl:variable>
        <xsl:comment>cur-tab: <xsl:copy-of select="$current-tab"/></xsl:comment>-->

        <xsl:for-each select="dri:list[@n='location']/dri:item">
            <xsl:apply-templates select="." mode="tabList"/>
<!--            <xsl:choose>
                <xsl:when test="$current-tab = '' and substring-after(@n, '/') = 1">
                    <div id="aba-ativa" class="borda">
                        <xsl:apply-templates select="." mode="tabList"/>
                    </div>
                </xsl:when>
                <xsl:when test="@n = $current-tab">
                    <div id="aba-ativa" class="borda">
                        <xsl:apply-templates select="." mode="tabList"/>
                    </div>
                </xsl:when>
                <xsl:otherwise>
                    <div class="borda">
                        <h4><span class="cor2">
                        <xsl:apply-templates select="." mode="tabList"/>
                        </span></h4>
                    </div>
                </xsl:otherwise>
            </xsl:choose>-->
        </xsl:for-each>
    </xsl:template>



<!--TODO-->
    <xsl:template name="resultados">
        <xsl:choose>
            <xsl:when test="//dri:referenceSet[@rend='recent-submissions']">
                <xsl:apply-templates select="//dri:referenceSet[@rend='recent-submissions']"/>
            </xsl:when>
            <xsl:when test="//dri:referenceSet[@rend='repository-search-results']">
                <xsl:apply-templates select="//dri:referenceSet[@rend='repository-search-results']"/>
            </xsl:when>
            <xsl:otherwise>
<!--                NONONO-->
                <div id="sem-resultados" class="borda">
                    <p>
                        <i18n:text>xmlui.ArtifactBrowser.AbstractSearch.no_results</i18n:text>
                    </p>
                </div>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>


    <xsl:template name="numero-resultados">
<!--        <div class="numero-resultados">-->
<!--            <span class="barra-texto">Resultados </span>
            <span class="mostrando-resultados-inicio">12</span> ~
            <span class="mostrando-resultados-fim">24</span> de
            <span class="total-resultados">126</span>-->
            <xsl:choose>
                <xsl:when test="//dri:div[@n='search-results']">
                    <xsl:apply-templates select="//dri:div[@n='search-results']/@pagination"/>
                </xsl:when>
                <xsl:otherwise>
<!--                    &#160;-->
                </xsl:otherwise>
            </xsl:choose>
<!--        </div>-->
    </xsl:template>




<!-- dri:options structure -->

    <xsl:template match="dri:list[@id='aspect.artifactbrowser.Navigation.list.browse']" priority="10">
        NAVIGATION-BROWSE
    </xsl:template>

    <xsl:template match="dri:list[@id='aspect.administrative.Navigation.list.account']" priority="100">
        <!-- hide -->
    </xsl:template>


    <xsl:template name="options-search-box">
                    <xsl:apply-templates select="//dri:div[@interactive='yes'][@n='general-query']/dri:list[@n='search-query']/dri:item[@n='search-filter-list']"
                                         mode="Corisco"
                                         />
<!--/dri:field[@n='search-filter-controls'] mode="compositeComponent"-->
<!--STOP-->

<!--                    <xsl:apply-templates select="//dri:div[@interactive='yes'][@n='general-query']/dri:p/dri:field[@type='button']"/>-->
                    
<!--            <a>
                <xsl:attribute name="href">
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='advancedURL']"/>
                </xsl:attribute>
                <i18n:text>xmlui.dri2xhtml.structural.search-advanced</i18n:text>
            </a>-->
<!--        </div>-->
    </xsl:template>






<!-- Utility functions. -->

    <xsl:template name="getRepositoryMetsURL">
        <xsl:comment>METS: /dri:document/dri:meta/dri:repositoryMeta/dri:repository[@repositoryID=$handle-prefix]</xsl:comment>
        <xsl:variable name="repository-mets" select="/dri:document/dri:meta/dri:repositoryMeta/dri:repository[@repositoryID=$handle-prefix]"/>
        <xsl:value-of select="$repository-mets/@url"/>
<!--        <xsl:for-each select="$repository-mets">
            <xsl:value-of select="@url"/>
        </xsl:for-each>-->
    </xsl:template>


    <xsl:template name="getMetsDCTitle">
        <xsl:param name="mets-URL"/>
        <xsl:variable name="url">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="$mets-URL"/>
        </xsl:variable>
        <xsl:variable name="title">
            <xsl:value-of select="document($url)/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='title'][1]"/>
        </xsl:variable>
        <xsl:value-of select="$title"/>
    </xsl:template>

    <xsl:template name="getMetsID">
        <xsl:param name="mets-URL"/>
        <xsl:variable name="url">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="$mets-URL"/>
        </xsl:variable>

        <xsl:variable name="id">
            <xsl:value-of select="document($url)/mets:METS/@ID"/>
        </xsl:variable>
        <xsl:value-of select="substring-after($id, ':')"/>
    </xsl:template>


    <xsl:template name="getSubGroupsURLsOfTopLevelCommunity">
        <xsl:param name="group-name" select="'item'"/>
        <xsl:variable name="repository-mets">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:call-template name="getRepositoryMetsURL"/>
            <xsl:text>?sections=structMap</xsl:text>
        </xsl:variable>
        <xsl:variable name="struct-map" select="document($repository-mets)/mets:METS/mets:structMap[@LABEL='DSpace']"/>

        <xsl:for-each select="$struct-map/mets:div[@TYPE='DSpace Repository']/mets:div[@TYPE='DSpace Community']/mets:div">
            <xsl:element name="{$group-name}">
                <xsl:value-of select="mets:mptr[@LOCTYPE='URL']/@xlink:href"/>
            </xsl:element>
        </xsl:for-each>
    </xsl:template>


    <!-- Subcommunities and collections inside the first top level community. -->
    <xsl:template name="getFirstLevelGroupTitles">
        <xsl:variable name="sub-groups">
            <xsl:call-template name="getSubGroupsURLsOfTopLevelCommunity"/>
        </xsl:variable>

        <xsl:for-each select="exsl:node-set($sub-groups)/*">
            <xsl:variable name="cocoon-url">
<!--                <xsl:text>cocoon:/</xsl:text>-->
                <xsl:value-of select="."/>
                <xsl:text>?sections=dmdSec</xsl:text>
            </xsl:variable>
            <item>
                <xsl:attribute name="handle">
                    <xsl:call-template name="getMetsID">
                        <xsl:with-param name="mets-URL" select="$cocoon-url"/>
                    </xsl:call-template>
                </xsl:attribute>
                <xsl:call-template name="getMetsDCTitle">
                    <xsl:with-param name="mets-URL" select="$cocoon-url"/>
                </xsl:call-template>
            </item>
        </xsl:for-each>
    </xsl:template>


    <xsl:template name="getReferenceSetOfTopLevelCommunity">
        <xsl:variable name="repository-mets">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:call-template name="getRepositoryMetsURL"/>
            <xsl:text>?sections=structMap</xsl:text>
        </xsl:variable>
        <xsl:variable name="struct-map" select="document($repository-mets)/mets:METS/mets:structMap[@LABEL='DSpace']"/>

        <dri:referenceSet rend="tabs" type="anyList">
            <dri:reference type="DSpace Community">
                <xsl:attribute name="url">
<!--                    <xsl:text>cocoon:/</xsl:text>-->
                    <xsl:value-of select="$struct-map/mets:div[@TYPE='DSpace Repository']/mets:div[@TYPE='DSpace Community']/mets:mptr[@LOCTYPE='URL']/@xlink:href"/>
                </xsl:attribute>
                &#160;
            </dri:reference>
            <xsl:for-each select="$struct-map/mets:div[@TYPE='DSpace Repository']/mets:div[@TYPE='DSpace Community']/mets:div">
                <xsl:element name="dri:reference">
<!--                <dri:reference>-->
                    <xsl:attribute name="type">
                        <xsl:value-of select="@TYPE"/>
                    </xsl:attribute>
                    <xsl:attribute name="url">
<!--                        <xsl:text>cocoon:/</xsl:text>-->
                        <xsl:value-of select="mets:mptr[@LOCTYPE='URL']/@xlink:href"/>
                    </xsl:attribute>
                    &#160;
<!--                </dri:reference>-->
                </xsl:element>
            </xsl:for-each>
        </dri:referenceSet>
    </xsl:template>


    <xsl:template name="convertToPlainLowCaps">
        <xsl:param name="from_text"/>
        <xsl:variable name="upper">ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÚÀÈÌÒÙÃẼĨÕŨÂÊÎÔÛÄËÏÖÜÇ</xsl:variable>
        <xsl:variable name="lower">abcdefghijklmnopqrstuvwxyzáéíóúàèìòùãẽĩõũâêîôûäëïöüç</xsl:variable>
        <xsl:variable name="from_text_lc">
            <xsl:value-of select="translate($from_text, $upper, $lower)"/>
        </xsl:variable>
        <xsl:variable name="accent">áéíóúàèìòùãẽĩõũâêîôûäëïöüç </xsl:variable>
        <xsl:variable name="plain">aeiouaeiouaeiouaeiouaeiouc_</xsl:variable>
        <xsl:value-of select="translate($from_text_lc, $accent, $plain)"/>
    </xsl:template>


    <xsl:template name="getDocumentTypeThumbnailPath">
        <xsl:param name="document_type_name"/>

        <xsl:variable name="document_type_name_plain">
            <xsl:call-template name="convertToPlainLowCaps">
                <xsl:with-param name="from_text" select="$document_type_name"/>
            </xsl:call-template>
        </xsl:variable>

        <xsl:variable name="thumbnail-path">
            <xsl:value-of select="$images-path"/>
            <xsl:value-of select="$document_type_name_plain"/>
            <xsl:text>.png</xsl:text>
        </xsl:variable>

        <xsl:value-of select="$thumbnail-path"/>
    </xsl:template>


    <xsl:template name="getDocumentTypeThumbnail">
        <xsl:param name="document_type_name"/>

        <xsl:variable name="thumbnail-path">
            <xsl:call-template name="getDocumentTypeThumbnailPath">
                <xsl:with-param name="document_type_name" select="$document_type_name"/>
            </xsl:call-template>
        </xsl:variable>

        <xsl:choose>
            <xsl:when test="$document_type_name">
                <img>
                    <xsl:attribute name="src">
                        <xsl:value-of select="$thumbnail-path"/>
                    </xsl:attribute>
                    <xsl:attribute name="title">
                        <xsl:value-of select="$document_type_name"/>
                    </xsl:attribute>
                    <!--
                    <xsl:attribute name="alt">
                        <xsl:value-of select="$document_type_name"/>
                    </xsl:attribute>
                    -->
                </img>
            </xsl:when>
            <xsl:otherwise>
                &#160;
                <!--
                <xsl:attribute name="src">
                    <xsl:value-of select="$theme-path"/>
                    <xsl:text>/images/thumbs/thumb-nenhum.jpg</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="title">
                    <xsl:text>Não definido.</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="alt">
                    Não def.
                </xsl:attribute>
                -->
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

















    <!-- This template matches all subcommunities and collections inside the first top level community. -->
<!--    <xsl:template match="mets:structMap[@LABEL='DSpace']/mets:div[@TYPE='DSpace Repository']/mets:div[@TYPE='DSpace Community']/mets:div" mode="getSubGroups">
        <xsl:variable name="metsURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="mets:mptr[@LOCTYPE='URL']/@xlink:href"/>
            <xsl:text>?sections=dmdSec</xsl:text>
        </xsl:variable>

        <xsl:value-of select="document($metsURL)/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='title'][1]"/>
    </xsl:template>-->


<!--    <xsl:template match="dri:referenceSet[@type = 'anyList']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <xsl:param name="current-tab"/>

        <xsl:comment>current: <xsl:copy-of select="$current-tab"/></xsl:comment>

        <xsl:comment>url: <xsl:copy-of select="node()/*"/></xsl:comment>
        <xsl:for-each select="./*">
            <xsl:variable name="tab-name">
                <xsl:call-template name="getMetsID">
                    <xsl:with-param name="mets-URL" select="@url"/>
                </xsl:call-template>
            </xsl:variable>
            <xsl:comment>tab-name: <xsl:copy-of select="$tab-name"/></xsl:comment>

            <xsl:choose>
                <xsl:when test="$tab-name = $current-tab">
                    <div class="aba-ativa">T&#160;
                        <xsl:apply-templates select="." mode="anyList"/>
                    </div>
                </xsl:when>
                <xsl:otherwise>
                    <div class="aba-inativa">X&#160;
                        <xsl:apply-templates select="." mode="anyList"/>
                    </div>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
    </xsl:template>-->


    <xsl:template match="dri:reference" mode="anyList">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- Since this is a summary only grab the descriptive metadata, and the thumbnails -->
            <xsl:text>?sections=dmdSec,fileSec,structMap</xsl:text>
            <!--&amp;fileGrpTypes=THUMBNAIL-->
            <!-- An example of requesting a specific metadata standard (MODS and QDC crosswalks only work for items)->
            <xsl:if test="@type='DSpace Item'">
                <xsl:text>&amp;dmdTypes=DC</xsl:text>
            </xsl:if>-->
        </xsl:variable>
        <xsl:comment> External Metadata URL:
            <xsl:value-of select="$externalMetadataURL"/>
            anyList
        </xsl:comment>

        <xsl:apply-templates select="document($externalMetadataURL)" mode="summaryList"/>

    </xsl:template>


<!-- *********************************************************************** -->

<!--
        Structural elements from here on out. These are the tags that contain static content under body, i.e.
        lists, tables and divs. They are also used in making forms and referencing metadata from the object
        store through the use of reference elements. The lists used by options also have templates here.
-->
    
    
    
    
    <!-- First and foremost come the div elements, which are the only elements directly under body. Every
        document has a body and every body has at least one div, which may in turn contain other divs and
        so on. Divs can be of two types: interactive and non-interactive, as signified by the attribute of
        the same name. The two types are handled separately.
    -->
    
    <!-- Non-interactive divs get turned into HTML div tags. The general process, which is found in many
        templates in this stylesheet, is to call the template for the head element (creating the HTML h tag),
        handle the attributes, and then apply the templates for the all children except the head. The id
        attribute is -->
<!--    <xsl:template match="dri:div" priority="1">
        <xsl:apply-templates select="dri:head"/>
        <xsl:apply-templates select="@pagination">
            <xsl:with-param name="position">top</xsl:with-param>
        </xsl:apply-templates>
        <div>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-static-div</xsl:with-param>
            </xsl:call-template>
            <xsl:choose>
                      does this element have any children 
                <xsl:when test="child::node()">
                    <xsl:apply-templates select="*[not(name()='head')]"/>
                </xsl:when>
                         if no children are found we add a space to eliminate self closing tags 
                <xsl:otherwise>
                                &#160;
                </xsl:otherwise>
            </xsl:choose>
        </div>
        <xsl:apply-templates select="@pagination">
            <xsl:with-param name="position">bottom</xsl:with-param>
        </xsl:apply-templates>
    </xsl:template>-->
    
    <!-- Interactive divs get turned into forms. The priority attribute on the template itself
        signifies that this template should be executed if both it and the one above match the
        same element (namely, the div element).
        
        Strictly speaking, XSL should be smart enough to realize that since one template is general
        and other more specific (matching for a tag and an attribute), it should apply the more
        specific once is it encounters a div with the matching attribute. However, the way this
        decision is made depends on the implementation of the XSL parser is not always consistent.
        For that reason explicit priorities are a safer, if perhaps redundant, alternative. -->
    <xsl:template match="dri:div[@interactive='yes' and false]" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <xsl:apply-templates select="@pagination">
            <xsl:with-param name="position">top</xsl:with-param>
        </xsl:apply-templates>
        <form>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-interactive-div</xsl:with-param>
            </xsl:call-template>
            <xsl:attribute name="action">
                <xsl:value-of select="@action"/>
            </xsl:attribute>
            <xsl:attribute name="method">
                <xsl:value-of select="@method"/>
            </xsl:attribute>
            <xsl:if test="@method='multipart'">
                <xsl:attribute name="method">post</xsl:attribute>
                <xsl:attribute name="enctype">multipart/form-data</xsl:attribute>
            </xsl:if>
            <xsl:attribute name="onsubmit">javascript:tSubmit(this);</xsl:attribute>
                        <!--For Item Submission process, disable ability to submit a form by pressing 'Enter'-->
            <xsl:if test="starts-with(@n,'submit')">
                <xsl:attribute name="onkeydown">javascript:return disableEnterKey(event);</xsl:attribute>
            </xsl:if>
            <xsl:apply-templates select="*[not(name()='head')]"/>
          
        </form>
        <!-- JS to scroll form to DIV parent of "Add" button if jump-to -->
        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='jumpTo']">
            <script type="text/javascript">
                <xsl:text>var button = document.getElementById('</xsl:text>
                <xsl:value-of select="translate(@id,'.','_')"/>
                <xsl:text>').elements['</xsl:text>
                <xsl:value-of select="concat('submit_',/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='jumpTo'],'_add')"/>
                <xsl:text>'];</xsl:text>
                <xsl:text>
                      if (button != null) {
                        var n = button.parentNode;
                        for (; n != null; n = n.parentNode) {
                            if (n.tagName == 'DIV') {
                              n.scrollIntoView(false);
                              break;
                           }
                        }
                      }
                </xsl:text>
            </script>
        </xsl:if>
        <xsl:apply-templates select="@pagination">
            <xsl:with-param name="position">bottom</xsl:with-param>
        </xsl:apply-templates>
    </xsl:template>
    
    
    <!-- Special case for divs tagged as "notice" -->
    <xsl:template match="dri:div[@n='general-message']" priority="3">
        <div>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-notice-div</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </div>
    </xsl:template>
    
    
    <!-- Next come the three structural elements that divs that contain: table, p, and list. These are
        responsible for display of static content, forms, and option lists. The fourth element under
        body, referenceSet, is used to reference blocks of metadata and will be discussed further down.
    -->
    
    
    <!-- First, the table element, used for rendering data in tabular format. In DRI tables consist of
        an optional head element followed by a set of row tags. Each row in turn contains a set of cells.
        Rows and cells can have different roles, the most common ones being header and data (with the
        attribute omitted in the latter case). -->
<!--    <xsl:template match="dri:table">
        <xsl:apply-templates select="dri:head"/>
        <table>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table</xsl:with-param>
            </xsl:call-template>
             rows and cols atributes are not allowed in strict
            <xsl:attribute name="rows"><xsl:value-of select="@rows"/></xsl:attribute>
            <xsl:attribute name="cols"><xsl:value-of select="@cols"/></xsl:attribute>

            <xsl:if test="count(dri:row[@role='header']) &gt; 0">
                    <thead>
                        <xsl:apply-templates select="dri:row[@role='header']"/>
                    </thead>
            </xsl:if>
            <tbody>
                <xsl:apply-templates select="dri:row[not(@role='header')]"/>
            </tbody>
            
            <xsl:apply-templates select="dri:row"/>
        </table>
    </xsl:template>
    
     Header row, most likely filled with header cells 
    <xsl:template match="dri:row[@role='header']" priority="2">
        <tr>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table-header-row</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </tr>
    </xsl:template>
    
     Header cell, assumed to be one since it is contained in a header row 
    <xsl:template match="dri:row[@role='header']/dri:cell | dri:cell[@role='header']" priority="2">
        <th>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table-header-cell
                    <xsl:if test="(position() mod 2 = 0)">even</xsl:if>
                    <xsl:if test="(position() mod 2 = 1)">odd</xsl:if>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:if test="@rows">
                <xsl:attribute name="rowspan">
                    <xsl:value-of select="@rows"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="@cols">
                <xsl:attribute name="colspan">
                    <xsl:value-of select="@cols"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </th>
    </xsl:template>
    
    
     Normal row, most likely filled with data cells 
    <xsl:template match="dri:row" priority="1">
        <tr>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table-row
                    <xsl:if test="(position() mod 2 = 0)">even</xsl:if>
                    <xsl:if test="(position() mod 2 = 1)">odd</xsl:if>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </tr>
    </xsl:template>
    
     Just a plain old table cell 
    <xsl:template match="dri:cell" priority="1">
        <td>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table-cell
                    <xsl:if test="(position() mod 2 = 0)">even</xsl:if>
                    <xsl:if test="(position() mod 2 = 1)">odd</xsl:if>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:if test="@rows">
                <xsl:attribute name="rowspan">
                    <xsl:value-of select="@rows"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="@cols">
                <xsl:attribute name="colspan">
                    <xsl:value-of select="@cols"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </td>
    </xsl:template>
    -->




    <xsl:template match="dri:table" mode="searchControls">
<!--        <xsl:apply-templates select="dri:head"/>-->
        <table>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table</xsl:with-param>
            </xsl:call-template>
            <!-- rows and cols atributes are not allowed in strict
            <xsl:attribute name="rows"><xsl:value-of select="@rows"/></xsl:attribute>
            <xsl:attribute name="cols"><xsl:value-of select="@cols"/></xsl:attribute>

            <xsl:if test="count(dri:row[@role='header']) &gt; 0">
                    <thead>
                        <xsl:apply-templates select="dri:row[@role='header']"/>
                    </thead>
            </xsl:if>
            <tbody>
                <xsl:apply-templates select="dri:row[not(@role='header')]"/>
            </tbody>
            -->
            <xsl:apply-templates select="dri:row" mode="searchControls"/>
        </table>
    </xsl:template>

    <!-- Normal row, most likely filled with data cells -->
    <xsl:template match="dri:row" priority="1" mode="searchControls">
        <tr>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table-row
                    <xsl:if test="(position() mod 2 = 0)">even</xsl:if>
                    <xsl:if test="(position() mod 2 = 1)">odd</xsl:if>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates  mode="searchControls"/>
        </tr>
    </xsl:template>

    <!-- Just a plain old table cell -->
    <xsl:template match="dri:cell" priority="1" mode="searchControls">
        <xsl:choose>
            <xsl:when test="dri:field[@n='rpp']">
                <div class="resultados-pagina">
                    <span class="barra-texto"><xsl:apply-templates select="child::*[1]"/></span>
                    <xsl:apply-templates select="child::*[position()>1]" mode="searchControls"/>
                </div>
            </xsl:when>
            <xsl:when test="dri:field[@n='sort_by']">
                <div class="resultados-ordenar">
                    <span class="barra-texto"><xsl:apply-templates select="child::*[1]"/></span>
                    <span class="ordenar-crescente">
                        <xsl:apply-templates select="..//dri:field[@n='order']/dri:option[@returnValue='ASC']" mode="searchControlsImage"/>
                    </span>
                    <span class="barra-texto">
                        <xsl:apply-templates select="child::*[position()>1]" mode="searchControls"/>
                    </span>
                    <span class="ordenar-decrescente">
                        <xsl:apply-templates select="..//dri:field[@n='order']/dri:option[@returnValue='DESC']" mode="searchControlsImage"/>
                    </span>
                </div>
            </xsl:when>
            <xsl:when test="dri:field[@n='view']">
                <div class="modos">
                    <span class="barra-texto"><xsl:apply-templates select="child::*[1]"/></span>
                    <span class="listagem">
                        <xsl:apply-templates select="dri:field[@n='view']/dri:option[@returnValue='listing']" mode="searchControlsImage"/>
<!--                            <xsl:with-param name="selected" select="'modo_listagemON.png'"/>
                            <xsl:with-param name="deselected" select="'modo_listagem.png'"/>
                        </xsl:apply-templates>-->
                    </span>
                    <span class="mosaico">
                        <xsl:apply-templates select="dri:field[@n='view']/dri:option[@returnValue='grid']" mode="searchControlsImage"/>
<!--                            <xsl:with-param name="selected" select="'modo_mosaicoON.png'"/>
                            <xsl:with-param name="deselected" select="'modo_mosaico.png'"/>
                        </xsl:apply-templates>-->
                    </span>
                </div>
            </xsl:when>
            <xsl:when test="dri:field[@n='order']">
            </xsl:when>
            <xsl:otherwise>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="dri:table" mode="browseControls">
        <xsl:apply-templates select="dri:row" mode="browseControls"/>
    </xsl:template>

    <!-- Normal row, most likely filled with data cells -->
    <xsl:template match="dri:row" priority="1" mode="browseControls">
<!--        <xsl:call-template name="standardAttributes">
            <xsl:with-param name="class">ds-table-row
                <xsl:if test="(position() mod 2 = 0)">even</xsl:if>
                <xsl:if test="(position() mod 2 = 1)">odd</xsl:if>
            </xsl:with-param>
        </xsl:call-template>-->
        <xsl:apply-templates  mode="browseControls"/>
    </xsl:template>

    <xsl:template match="dri:cell" priority="1" mode="browseControls">
        <xsl:choose>
            <xsl:when test="dri:field[@n='rpp']">
                <div class="resultados-pagina">
                    <span class="barra-texto"><xsl:apply-templates select="child::*[1]"/></span>
                    <xsl:apply-templates select="child::*[position()>1]" mode="searchControls"/>
                </div>
            </xsl:when>
            <xsl:when test="dri:field[@n='sort_by']">
                <div class="resultados-ordenar">
                    <span class="barra-texto"><xsl:apply-templates select="child::*[1]"/></span>
                    <span class="ordenar-crescente">
                        <xsl:apply-templates select="..//dri:field[@n='order']/dri:option[@returnValue='asc']" mode="searchControlsImage"/>
                    </span>
                    <span class="barra-texto">
                        <xsl:apply-templates select="child::*[position()>1]" mode="searchControls"/>
                    </span>
                    <span class="ordenar-decrescente">
                        <xsl:apply-templates select="..//dri:field[@n='order']/dri:option[@returnValue='desc']" mode="searchControlsImage"/>
                    </span>
                </div>
            </xsl:when>
            <xsl:when test="dri:field[@n='view']">
                <div class="modos">
                    <span class="barra-texto"><xsl:apply-templates select="child::*[1]"/></span>
                    <span class="listagem">
                        <xsl:apply-templates select="dri:field[@n='view']/dri:option[@returnValue='list']" mode="searchControlsImage"/>
<!--                            <xsl:with-param name="selected" select="'modo_listaON.png'"/>
                            <xsl:with-param name="deselected" select="'modo_lista.png'"/>
                        </xsl:apply-templates>-->
                    </span>
                    <!-- FIXME TODO: sem nuvem por enquanto. -->
<!--                    <span class="nuvem">
                        <xsl:apply-templates select="dri:field[@n='view']/dri:option[@returnValue='cloud']" mode="searchControlsImage"/>
                    </span>-->
                </div>
            </xsl:when>
            <xsl:when test="dri:field[@n='order']">
                <div class="resultados-ordenar">
                    <span class="ordenar-crescente">
                        <xsl:apply-templates select="dri:field[@n='order']/dri:option[@returnValue='asc']" mode="searchControlsImage"/>
                    </span>
                    <span class="barra-texto"><xsl:apply-templates select="child::*[1]"/></span>
                    <span class="ordenar-decrescente">
                        <xsl:apply-templates select="dri:field[@n='order']/dri:option[@returnValue='desc']" mode="searchControlsImage"/>
                    </span>
                </div>
            </xsl:when>
            <xsl:otherwise>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>


    <!-- Second, the p element, used for display of text. The p element is a rich text container, meaning it
        can contain text mixed with inline elements like hi, xref, figure and field. The cell element above
        and the item element under list are also rich text containers.
    -->
    <xsl:template match="dri:p">
        <p>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-paragraph</xsl:with-param>
            </xsl:call-template>
            <xsl:choose>
                <!--  does this element have any children -->
                <xsl:when test="child::node()">
                    <xsl:apply-templates />
                </xsl:when>
                        <!-- if no children are found we add a space to eliminate self closing tags -->
                <xsl:otherwise>
                                &#160;
                </xsl:otherwise>
            </xsl:choose>
            
        </p>
    </xsl:template>
    
    
    
    <!-- Finally, we have the list element, which is used to display set of data. There are several different
        types of lists, as signified by the type attribute, and several different templates to handle them. -->
    
    <!-- First list type is the bulleted list, a list with no real labels and no ordering between elements. -->
    <xsl:template match="dri:list[@type='bulleted']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <ul>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-bulleted-list</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="*[not(name()='head')]" mode="nested"/>
        </ul>
    </xsl:template>
    
    <!-- The item template creates an HTML list item element and places the contents of the DRI item inside it.
        Additionally, it checks to see if the currently viewed item has a label element directly preceeding it,
        and if it does, applies the label's template before performing its own actions. This mechanism applies
        to the list item templates as well. -->
    <xsl:template match="dri:list[@type='bulleted']/dri:item" priority="2" mode="nested">
        <li>
            <xsl:if test="name(preceding-sibling::*[position()=1]) = 'dri:label'">
                <xsl:apply-templates select="preceding-sibling::*[position()=1]"/>
            </xsl:if>
            <xsl:apply-templates />
        </li>
    </xsl:template>
    
    <!-- The case of nested lists is handled in a similar way across all lists. You match the sub-list based on
        its parent, create a list item approtiate to the list type, fill its content from the sub-list's head
        element and apply the other templates normally. -->
    <xsl:template match="dri:list[@type='bulleted']/dri:list" priority="3" mode="nested">
        <li>
            <xsl:apply-templates select="."/>
        </li>
    </xsl:template>
    
       
    <!-- Second type is the ordered list, which is a list with either labels or names to designate an ordering
        of some kind. -->
    <xsl:template match="dri:list[@type='ordered']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <ol>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-ordered-list</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="*[not(name()='head')]" mode="nested">
                <xsl:sort select="dri:item/@n"/>
            </xsl:apply-templates>
        </ol>
    </xsl:template>
    
    <xsl:template match="dri:list[@type='ordered']/dri:item" priority="2" mode="nested">
        <li>
            <xsl:if test="name(preceding-sibling::*[position()=1]) = 'label'">
                <xsl:apply-templates select="preceding-sibling::*[position()=1]"/>
            </xsl:if>
            <xsl:apply-templates />
        </li>
    </xsl:template>
    
    <xsl:template match="dri:list[@type='ordered']/dri:list" priority="3" mode="nested">
        <li>
            <xsl:apply-templates select="."/>
        </li>
    </xsl:template>
    
    
    <!-- Progress list used primarily in forms that span several pages. There isn't a template for the nested
        version of this list, mostly because there isn't a use case for it. -->
    <xsl:template match="dri:list[@type='progress']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <ul>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-progress-list</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="dri:item"/>
        </ul>
    </xsl:template>
    
    <xsl:template match="dri:list[@type='progress']/dri:item" priority="2">
        <li>
            <xsl:attribute name="class">
                <xsl:value-of select="@rend"/>
                <xsl:if test="position()=1">
                    <xsl:text> first</xsl:text>
                </xsl:if>
                <xsl:if test="descendant::dri:field[@type='button']">
                    <xsl:text> button</xsl:text>
                </xsl:if>
                <xsl:if test="position()=last()">
                    <xsl:text> last</xsl:text>
                </xsl:if>
            </xsl:attribute>
            <xsl:apply-templates />
        </li>
        <xsl:if test="not(position()=last())">
            <li class="arrow">
                <xsl:text>&#8594;</xsl:text>
            </li>
        </xsl:if>
    </xsl:template>
        
    
    <!-- The third type of list is the glossary (gloss) list. It is essentially a list of pairs, consisting of
        a set of labels, each followed by an item. Unlike the ordered and bulleted lists, gloss is implemented
        via HTML definition list (dd) element. It can also be changed to work as a two-column table. -->
    <xsl:template match="dri:list[@type='gloss']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <dl>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-gloss-list</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="*[not(name()='head')]" mode="nested"/>
        </dl>
    </xsl:template>
    
    <xsl:template match="dri:list[@type='gloss']/dri:item" priority="2" mode="nested">
        <dd>
            <xsl:apply-templates />
        </dd>
    </xsl:template>
    
    <xsl:template match="dri:list[@type='gloss']/dri:label" priority="2" mode="nested">
        <dt>
            <span>
                <xsl:attribute name="class">
                    <xsl:text>ds-gloss-list-label </xsl:text>
                    <xsl:value-of select="@rend"/>
                </xsl:attribute>
                <xsl:apply-templates />
                <xsl:text>:</xsl:text>
            </span>
        </dt>
    </xsl:template>
    
    <xsl:template match="dri:list[@type='gloss']/dri:list" priority="3" mode="nested">
        <dd>
            <xsl:apply-templates select="."/>
        </dd>
    </xsl:template>
    
    
    <!-- The next list type is one without a type attribute. In this case XSL makes a decision: if the items
        of the list have labels the the list will be made into a table-like structure, otherwise it is considered
        to be a plain unordered list and handled generically. -->
    <!-- TODO: This should really be done with divs and spans instead of tables. Form lists have already been
        converted so the solution here would most likely mirror that one -->
    <xsl:template match="dri:list[not(@type)]" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <xsl:if test="count(dri:label)>0">
            <table>
                <xsl:call-template name="standardAttributes">
                    <xsl:with-param name="class">ds-gloss-list</xsl:with-param>
                </xsl:call-template>
                <xsl:apply-templates select="dri:item" mode="labeled"/>
            </table>
        </xsl:if>
        <xsl:if test="count(dri:label)=0">
            <ul>
                <xsl:call-template name="standardAttributes">
                    <xsl:with-param name="class">ds-simple-list</xsl:with-param>
                </xsl:call-template>
                <xsl:apply-templates select="dri:item" mode="nested"/>
            </ul>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="dri:list[not(@type)]/dri:item" priority="2" mode="labeled">
        <tr>
            <xsl:attribute name="class">
                <xsl:text>ds-table-row </xsl:text>
                <xsl:if test="(position() mod 2 = 0)">even </xsl:if>
                <xsl:if test="(position() mod 2 = 1)">odd </xsl:if>
                <xsl:value-of select="@rend"/>
            </xsl:attribute>
            <xsl:if test="name(preceding-sibling::*[position()=1]) = 'label'">
                <xsl:apply-templates select="preceding-sibling::*[position()=1]" mode="labeled"/>
            </xsl:if>
            <td>
                <xsl:apply-templates />
            </td>
        </tr>
    </xsl:template>
    
    <xsl:template match="dri:list[not(@type)]/dri:label" priority="2" mode="labeled">
        <td>
            <xsl:if test="count(./node())>0">
                <span>
                    <xsl:attribute name="class">
                        <xsl:text>ds-gloss-list-label </xsl:text>
                        <xsl:value-of select="@rend"/>
                    </xsl:attribute>
                    <xsl:apply-templates />
                    <xsl:text>:</xsl:text>
                </span>
            </xsl:if>
        </td>
    </xsl:template>
    
    <xsl:template match="dri:list[not(@type)]/dri:item" priority="2" mode="nested">
        <li>
            <xsl:apply-templates />
            <!-- Wrap orphaned sub-lists into the preceding item -->
            <xsl:variable name="node-set1" select="./following-sibling::dri:list"/>
            <xsl:variable name="node-set2" select="./following-sibling::dri:item[1]/following-sibling::dri:list"/>
            <xsl:apply-templates select="$node-set1[count(.|$node-set2) != count($node-set2)]"/>
        </li>
    </xsl:template>
            
   
    
    
    
    <!-- Special treatment of a list type "form", which is used to encode simple forms and give them structure.
        This is done partly to ensure that the resulting HTML form follows accessibility guidelines. -->
    
    <xsl:template match="dri:list[@type='form']" priority="3">
        <xsl:choose>
            <xsl:when test="ancestor::dri:list[@type='form']">
                <li>
                    <fieldset>
                        <xsl:call-template name="standardAttributes">
                            <xsl:with-param name="class">
                                <!-- Provision for the sub list -->
                                <xsl:text>ds-form-</xsl:text>
                                <xsl:if test="ancestor::dri:list[@type='form']">
                                    <xsl:text>sub</xsl:text>
                                </xsl:if>
                                <xsl:text>list </xsl:text>
                                <xsl:if test="count(dri:item) > 3">
                                    <xsl:text>thick </xsl:text>
                                </xsl:if>
                            </xsl:with-param>
                        </xsl:call-template>
                        <xsl:apply-templates select="dri:head"/>
            
                        <ol>
                            <xsl:apply-templates select="*[not(name()='label' or name()='head')]" />
                        </ol>
                    </fieldset>
                </li>
            </xsl:when>
            <xsl:otherwise>
<!--                <div>-->
<!--                <fieldset>-->
<!--                    <xsl:call-template name="standardAttributes">
                        <xsl:with-param name="class">
                     <!- Provision for the sub list ->
                            <xsl:text>ds-form-</xsl:text>
                            <xsl:if test="ancestor::dri:list[@type='form']">
                                <xsl:text>sub</xsl:text>
                            </xsl:if>
                            <xsl:text>list </xsl:text>
                            <xsl:if test="count(dri:item) > 3">
                                <xsl:text>thick </xsl:text>
                            </xsl:if>
                        </xsl:with-param>
                    </xsl:call-template>
                    <xsl:apply-templates select="dri:head"/>-->

<!--                    <ol>-->
                        <xsl:apply-templates select="*[not(name()='label' or name()='head')]" />
<!--                    </ol>-->
<!--                </fieldset>-->
<!--                </div>-->
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- TODO: Account for the dri:hi/dri:field kind of nesting here and everywhere else... -->
    <xsl:template match="dri:list[@type='form']/dri:item" priority="3">
<!--        <li>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">
                    <xsl:text>ds-form-item </xsl:text>
                    <xsl:choose>
                     Makes sure that the dark always falls on the last item 
                        <xsl:when test="count(../dri:item) mod 2 = 0">
                            <xsl:if test="count(../dri:item) > 3">
                                <xsl:if test="(count(preceding-sibling::dri:item) mod 2 = 0)">even </xsl:if>
                                <xsl:if test="(count(preceding-sibling::dri:item) mod 2 = 1)">odd </xsl:if>
                            </xsl:if>
                        </xsl:when>
                        <xsl:when test="count(../dri:item) mod 2 = 1">
                            <xsl:if test="count(../dri:item) > 3">
                                <xsl:if test="(count(preceding-sibling::dri:item) mod 2 = 1)">even </xsl:if>
                                <xsl:if test="(count(preceding-sibling::dri:item) mod 2 = 0)">odd </xsl:if>
                            </xsl:if>
                        </xsl:when>
                    </xsl:choose>
                 The last row is special if it contains only buttons 
                    <xsl:if test="position()=last() and dri:field[@type='button'] and not(dri:field[not(@type='button')])">last </xsl:if>
                 The row is also tagged specially if it contains another "form" list 
                    <xsl:if test="dri:list[@type='form']">sublist </xsl:if>
                </xsl:with-param>
            </xsl:call-template>-->
            
            <xsl:choose>
                <xsl:when test="dri:field[@type='composite']">
                    <xsl:call-template name="pick-label"/>
                    <xsl:apply-templates mode="formComposite"/>
                </xsl:when>
                <xsl:when test="dri:list[@type='form']">
                    <xsl:apply-templates />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="pick-label"/>
                    <div id="adicionar-busca" class="ds-form-content2">
                        <xsl:apply-templates />
                        <!-- special name used in submission UI review page -->
                        <xsl:if test="@n = 'submit-review-field-with-authority'">
                            <xsl:call-template name="authorityConfidenceIcon">
                                <xsl:with-param name="confidence" select="substring-after(./@rend, 'cf-')"/>
                            </xsl:call-template>
                        </xsl:if>
<!--                        Botão BUSCAR-->
<!--                        <xsl:apply-templates select="//dri:div[@interactive='yes'][@n='general-query']/dri:p"/>-->
                    </div>
                </xsl:otherwise>
            </xsl:choose>
<!--        </li>-->
    </xsl:template>

    <xsl:template match="dri:list[@type='form']/dri:item" priority="3" mode="Corisco">
        <xsl:choose>
            <xsl:when test="dri:field[@type='composite']">
                <xsl:if test="dri:field/dri:label">
                    <div class="borda" id="titulo-busca">
                        <xsl:choose>
                            <xsl:when test="./dri:field/@id">
                                <xsl:attribute name="for">
                                    <xsl:value-of select="translate(./dri:field/@id,'.','_')"/>
                                </xsl:attribute>
                            </xsl:when>
                            <xsl:otherwise></xsl:otherwise>
                        </xsl:choose>
                        <h3>
                            <xsl:apply-templates select="dri:field/dri:label" mode="formComposite"/>
                        </h3>
                    </div>
                </xsl:if>

                <xsl:apply-templates mode="formComposite"/>
            </xsl:when>
            <xsl:when test="dri:list[@type='form']">
                <xsl:apply-templates />
            </xsl:when>
            <xsl:otherwise>
                <xsl:if test="dri:field/dri:label">
                    <div class="borda" id="titulo-busca">
                        <xsl:choose>
                            <xsl:when test="./dri:field/@id">
                                <xsl:attribute name="for">
                                    <xsl:value-of select="translate(./dri:field/@id,'.','_')"/>
                                </xsl:attribute>
                            </xsl:when>
                            <xsl:otherwise></xsl:otherwise>
                        </xsl:choose>
                        <xsl:apply-templates select="dri:field/dri:label" mode="formComposite"/>
                    </div>
                </xsl:if>

                <div class="ds-form-content2">
                    <xsl:apply-templates />
                        <!-- special name used in submission UI review page -->
                    <xsl:if test="@n = 'submit-review-field-with-authority'">
                        <xsl:call-template name="authorityConfidenceIcon">
                            <xsl:with-param name="confidence" select="substring-after(./@rend, 'cf-')"/>
                        </xsl:call-template>
                    </xsl:if>
                </div>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- An item in a nested "form" list -->
    <xsl:template match="dri:list[@type='form']//dri:list[@type='form']/dri:item" priority="3">
        <li>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">
                    <xsl:text>ds-form-item </xsl:text>

                <!-- Row counting voodoo, meant to impart consistent row alternation colors to the form lists.
                    Should probably be chnaged to a system that is more straitforward. -->
                    <xsl:choose>
                        <xsl:when test="(count(../../..//dri:item) - count(../../..//dri:list[@type='form'])) mod 2 = 0">
                        <!--<xsl:if test="count(../dri:item) > 3">-->
                            <xsl:if test="(count(preceding-sibling::dri:item | ../../preceding-sibling::dri:item/dri:list[@type='form']/dri:item) mod 2 = 0)">even </xsl:if>
                            <xsl:if test="(count(preceding-sibling::dri:item | ../../preceding-sibling::dri:item/dri:list[@type='form']/dri:item) mod 2 = 1)">odd </xsl:if>
                        
                        </xsl:when>
                        <xsl:when test="(count(../../..//dri:item) - count(../../..//dri:list[@type='form'])) mod 2 = 1">
                        <!--<xsl:if test="count(../dri:item) > 3">-->
                            <xsl:if test="(count(preceding-sibling::dri:item | ../../preceding-sibling::dri:item/dri:list[@type='form']/dri:item) mod 2 = 1)">even </xsl:if>
                            <xsl:if test="(count(preceding-sibling::dri:item | ../../preceding-sibling::dri:item/dri:list[@type='form']/dri:item) mod 2 = 0)">odd </xsl:if>
                        
                        </xsl:when>
                    </xsl:choose>
                <!--
                <xsl:if test="position()=last() and dri:field[@type='button'] and not(dri:field[not(@type='button')])">last</xsl:if>
                    -->
                </xsl:with-param>
            </xsl:call-template>
            
            <xsl:call-template name="pick-label"/>

            <xsl:choose>
                <xsl:when test="dri:field[@type='composite']">
                    <xsl:apply-templates mode="formComposite"/>
                </xsl:when>
                <xsl:otherwise>
                    <div class="ds-form-content">
                        <xsl:apply-templates />
                        <!-- special name used in submission UI review page -->
                        <xsl:if test="@n = 'submit-review-field-with-authority'">
                            <xsl:call-template name="authorityConfidenceIcon">
                                <xsl:with-param name="confidence" select="substring-after(./@rend, 'cf-')"/>
                            </xsl:call-template>
                        </xsl:if>
                    </div>
                </xsl:otherwise>
            </xsl:choose>
        </li>
    </xsl:template>
    
    <xsl:template name="pick-label">
        <xsl:choose>
            <xsl:when test="dri:field/dri:label">
<!--                <label class="ds-form-label">-->
                <div class="borda" id="titulo-busca">
                    <xsl:choose>
                        <xsl:when test="./dri:field/@id">
                            <xsl:attribute name="for">
                                <xsl:value-of select="translate(./dri:field/@id,'.','_')"/>
                            </xsl:attribute>
                        </xsl:when>
                        <xsl:otherwise></xsl:otherwise>
                    </xsl:choose>
                    <h3>
                        <xsl:apply-templates select="dri:field/dri:label" mode="formComposite"/>
                    </h3>
<!--                    <xsl:text>:</xsl:text>-->
<!--                </label>-->
                </div>
            </xsl:when>
            <xsl:when test="string-length(string(preceding-sibling::*[1][local-name()='label'])) > 0">
                <xsl:choose>
                    <xsl:when test="./dri:field/@id">
                        <label>
                            <xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>
                            <xsl:text>:</xsl:text>
                        </label>
                    </xsl:when>
                    <xsl:otherwise>
                        <span>
                            <xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>
                            <xsl:text>:</xsl:text>
                        </span>
                    </xsl:otherwise>
                </xsl:choose>
                
            </xsl:when>
            <xsl:when test="dri:field">
                <xsl:choose>
                    <xsl:when test="preceding-sibling::*[1][local-name()='label']">
                        <label class="ds-form-label">
                            <xsl:choose>
                                <xsl:when test="./dri:field/@id">
                                    <xsl:attribute name="for">
                                        <xsl:value-of select="translate(./dri:field/@id,'.','_')"/>
                                    </xsl:attribute>
                                </xsl:when>
                                <xsl:otherwise></xsl:otherwise>
                            </xsl:choose>
                            <xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>&#160;
                        </label>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>&#160;
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <!-- If the label is empty and the item contains no field, omit the label. This is to
                    make the text inside the item (since what else but text can be there?) stretch across
                    both columns of the list. -->
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="dri:list[@type='form']/dri:label" priority="3">
        <xsl:attribute name="class">
            <xsl:text>ds-form-label</xsl:text>
            <xsl:if test="@rend">
                <xsl:text> </xsl:text>
                <xsl:value-of select="@rend"/>
            </xsl:if>
        </xsl:attribute>
        <xsl:choose>
            <xsl:when test="following-sibling::dri:item[1]/dri:field/@id">
                <xsl:attribute name="for">
                    <xsl:value-of select="translate(following-sibling::dri:item[1]/dri:field/@id,'.','_')" />
                </xsl:attribute>
            </xsl:when>
            <xsl:otherwise>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:apply-templates />
    </xsl:template>
    
    
    <xsl:template match="dri:field/dri:label" mode="formComposite">
        <xsl:apply-templates />
    </xsl:template>
         
    <xsl:template match="dri:list[@type='form']/dri:head" priority="5">
        <legend>
            <xsl:apply-templates />
        </legend>
    </xsl:template>
    
    <!-- NON-instance composite fields (i.e. not repeatable) -->
    <xsl:template match="dri:field[@type='composite']" mode="formComposite">
        <div id="adicionar-busca" class="ds-form-content3">
            <xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
            <xsl:apply-templates select="dri:field" mode="compositeComponent"/>
            <xsl:choose>
                <xsl:when test="dri:params/@choicesPresentation = 'suggest'">
                    <xsl:message terminate="yes">
                        <xsl:text>ERROR: Input field with "suggest" (autocomplete) choice behavior is not implemented for Composite (e.g. "name") fields.</xsl:text>
                    </xsl:message>
                </xsl:when>
              <!-- lookup popup includes its own Add button if necessary. -->
                <xsl:when test="dri:params/@choicesPresentation = 'lookup'">
                    <xsl:call-template name="addLookupButton">
                        <xsl:with-param name="isName" select="'true'"/>
                        <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                    </xsl:call-template>
                </xsl:when>
            </xsl:choose>
            <xsl:if test="dri:params/@authorityControlled">
                <xsl:variable name="confValue" select="dri:field/dri:value[@type='authority'][1]/@confidence"/>
                <xsl:call-template name="authorityConfidenceIcon">
                    <xsl:with-param name="confidence" select="$confValue"/>
                    <xsl:with-param name="id" select="$confidenceIndicatorID"/>
                </xsl:call-template>
                <xsl:call-template name="authorityInputFields">
                    <xsl:with-param name="name" select="@n"/>
                    <xsl:with-param name="authValue" select="dri:field/dri:value[@type='authority'][1]/text()"/>
                    <xsl:with-param name="confValue" select="$confValue"/>
                </xsl:call-template>
            </xsl:if>
<!--            <div class="spacer">&#160;</div>-->
            <xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent"/>
            <xsl:apply-templates select="dri:error" mode="compositeComponent"/>
            <xsl:apply-templates select="dri:help" mode="compositeComponent"/>
        </div>
    </xsl:template>

    
    <xsl:template match="dri:field" mode="dados-item">
<!--        <div id="adicionar-busca" class="ds-form-content3">-->
<!--            <xsl:apply-templates select="dri:field" mode="compositeComponent"/>-->
<!--            <xsl:apply-templates select="dri:help" mode="compositeComponent"/>-->
<!--        </div>-->
        <xsl:choose>
            <xsl:when test="@type='checkbox' or @type='radio'">
<!--                <xsl:attribute name="id">
                    <xsl:value-of select="generate-id()"/>
                </xsl:attribute>-->
                <xsl:if test="dri:label">
                    <legend>
                        <xsl:apply-templates select="dri:label" mode="compositeComponent" />
                    </legend>
                </xsl:if>

                <xsl:for-each select="dri:option">
                    <xsl:if test="position() &gt; 1 or //dri:body//dri:field[@id='aspect.discovery.SimpleSearch.field.query']/dri:value != ''">
                        <span> + </span>
                    </xsl:if>
                    <xsl:apply-templates select="." mode="dados-item"/>
                </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
                <input>
                    <xsl:call-template name="fieldAttributes"/>
                    <xsl:if test="@type='button'">
                        <xsl:attribute name="type">submit</xsl:attribute>
                    </xsl:if>
                    <xsl:attribute name="value">
                        <xsl:choose>
                            <xsl:when test="./dri:value[@type='raw']">
                                <xsl:value-of select="./dri:value[@type='raw']"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="./dri:value[@type='default']"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <xsl:if test="dri:value/i18n:text">
                        <xsl:attribute name="i18n:attr">value</xsl:attribute>
                    </xsl:if>
                    <xsl:apply-templates />
                </input>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    


    <!-- Next, special handling is performed for lists under the options tag, making them into option sets to
        reflect groups of similar options (like browsing, for example). -->
    
    <!-- The template that applies to lists directly under the options tag that have other lists underneath
        them. Each list underneath the matched one becomes an option-set and is handled by the appropriate
        list templates. -->
    <xsl:template match="dri:options/dri:list[@n='browse']" priority="5">
<!--        <xsl:if test="not(../dri:list[@n='discovery'])">-->
            <xsl:apply-templates />
<!--            <xsl:apply-templates mode="nested"/>-->
<!--            <xsl:apply-templates select="*[not(dri:list[@n='global']/dri:head)]"/>-->
<!--        </xsl:if>-->
    </xsl:template>


<!-- CHANGED -->
    <xsl:template match="dri:options/dri:list[dri:list][@n='discovery']" priority="4">
<!--        <xsl:apply-templates select="dri:head"/>-->

<!--TESTE-->
<!--        <xsl:apply-templates select="//dri:div[@interactive='yes'][@n='general-query']/dri:list"/>-->
<!--                <xsl:apply-templates select="//dri:div[@interactive='yes'][@n='general-query']/dri:table"/>-->
<!--                <xsl:apply-templates select="//dri:div[@interactive='yes'][@n='general-query']/dri:p"/>-->


        <xsl:apply-templates select="//dri:div[@interactive='yes'][@n='general-query']/dri:list[@n='search-query']/dri:item[@n='search-filter-list']"
                             />
<!--                             mode="Corisco"-->

<!--        <xsl:call-template name="options-search-box"/>-->

        <!-- Once the search box is built, the other parts of the options are added -->

<!--        <div>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-option-set</xsl:with-param>
            </xsl:call-template>
            <ul class="ds-options-list">-->
        <div id="titulo-refinar" class="borda">
            <h3><i18n:text>xmlui.dri2xhtml.structural.search-filter</i18n:text></h3>
        </div>

        <xsl:apply-templates select="*[not(name()='head')]" mode="nested"/>
<!--            </ul>
        </div>-->
    </xsl:template>

    <xsl:template match="dri:options/dri:list/dri:list[@n='global']" priority="3">
        <!-- Only show global browse list if there is no context list. -->
        <xsl:if test="//dri:options/dri:list/dri:list[@n='context'][count(child::*)=0]">
        <!-- <xsl:apply-templates select="dri:head"/>-->
            <div class="caixa-listar">
                <ul>
                    <xsl:apply-templates select="*[not(name()='head')]" mode="Corisco"/>
                </ul>
            </div>
        </xsl:if>
    </xsl:template>


    <xsl:template match="dri:options/dri:list/dri:list[@n='context']" priority="3">
        <div class="caixa-listar">
            <ul>
                <xsl:apply-templates select="*[not(name()='head')]" mode="Corisco"/>
            </ul>
        </div>
    </xsl:template>


    <!-- Special case for nested options lists -->
    <xsl:template match="dri:options/dri:list/dri:list" priority="3" mode="nested">
        <div id="ano" class="borda">
            <xsl:apply-templates select="dri:head" mode="nested"/>
            <ul class="caixa">
                <xsl:apply-templates select="dri:item" mode="nested"/>
            </ul>
        </div>
    </xsl:template>

    <!-- coluna-filtros -->
<!--    TODO-->
    <xsl:template match="dri:options/dri:list" priority="3">
        <xsl:apply-templates select="dri:head"/>
        <div>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-option-set</xsl:with-param>
            </xsl:call-template>
            <ul class="ds-simple-list">
                <xsl:apply-templates select="dri:item" mode="nested"/>
            </ul>
        </div>
    </xsl:template>

    <xsl:template match="dri:options/dri:list[@n='discovery-location']" priority="10">
        <!-- HIDE -->
    </xsl:template>

    <xsl:template match="dri:options/dri:list[@n='top-search']" priority="10">
        <!-- HIDE -->
    </xsl:template>

    <xsl:template match="dri:options/dri:list[@n='top-search']" mode="header" priority="1">
        <fieldset>
<!--            <xsl:apply-templates select="dri:item/dri:field/*[not(name()='help') and not(name()='label')]" mode="header"/>-->
            <xsl:apply-templates select="dri:item/dri:field" mode="header"/>
        </fieldset>
    </xsl:template>


    <xsl:template match="dri:options/dri:list[@n='top-search']/dri:item/dri:field[@n='search-filter-controls']" mode="header" priority="1">

        <xsl:apply-templates select="dri:field" mode="header"/>

        <xsl:if test="contains(dri:params/@operations,'add')">
            <!-- Had to add a hidden input because there are other inputs with type 'submit', and only the first one is submitted when enter key is pressed in a text field. -->
            <!-- Add buttons should be named "submit_[field]_add" so that we can ignore errors from required fields when simply adding new values-->
            <input id="botao-enviar" type="submit" name="{concat('submit_',@n,'_add')}" i18n:attr="value" value="xmlui.general.go" >
              <!-- Make invisible if we have choice-lookup operation that provides its own Add. -->
<!--                <xsl:if test="dri:params/@choicesPresentation = 'lookup'">
                    <xsl:attribute name="style">
                        <xsl:text>display:none;</xsl:text>
                    </xsl:attribute>
                </xsl:if>-->
            </input>
        </xsl:if>
    </xsl:template>


    <!-- Quick patch to remove empty lists from options -->
    <xsl:template match="dri:options//dri:list[count(child::*)=0]" priority="5" mode="nested">
    </xsl:template>
    <xsl:template match="dri:options//dri:list[count(child::*)=0]" priority="5">
    </xsl:template>
    
    <xsl:template match="dri:options/dri:list/dri:head" priority="3">
        <div id="titulo-listar" class="borda">
<!--            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-option-set-head</xsl:with-param>
            </xsl:call-template>-->
            <h3>
<!--                <i18n:text>xmlui.dri2xhtml.structural.search-filter</i18n:text>-->
                <xsl:apply-templates />
            </h3>
        </div>
    </xsl:template>
    
    <!-- Items inside option lists are excluded from the "orphan roundup" mechanism -->
    <xsl:template match="dri:options//dri:item" mode="nested" priority="3">
        <li class="filtro-item">
<!--            <img>
                <xsl:attribute name="src">
                    <xsl:value-of select="$images-path"/>
                    <xsl:text>mais.png</xsl:text>
                </xsl:attribute>
            </img>-->
            <span>
                <xsl:if test="@rend='view-more'">
                    <xsl:attribute name="class">
                        <xsl:text>ver-mais</xsl:text>
                    </xsl:attribute>
                </xsl:if>
                <xsl:apply-templates />
            </span>
        </li>
    </xsl:template>


    <xsl:template match="dri:options//dri:item" mode="Corisco" priority="1">
        <xsl:choose>
            <xsl:when test="contains(dri:xref/@target, 'community-list')">
                <!-- SKIP -->
            </xsl:when>
            <xsl:when test="contains(dri:xref/@target, 'dc.title')">
                <li class="lista-item">
                    <span>
                        <a>
                            <xsl:attribute name="href">
                                <xsl:value-of select="$context-path"/>
                                <xsl:if test="//dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='container']">
                                    <xsl:text>/handle/</xsl:text>
                                    <xsl:value-of select="substring-after(//dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='container'], ':')"/>
                                </xsl:if>
<!--                                <xsl:value-of select="//dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI']"/>-->
                                <xsl:text>/search?order=ASC&amp;sort_by=dc.title</xsl:text>
                            </xsl:attribute>
                            <xsl:attribute name="class">
                                <xsl:value-of select="@rend"/>
                            </xsl:attribute>
                            <xsl:apply-templates select="dri:xref/*"/>
                        </a>
                    </span>
                </li>
            </xsl:when>
            <xsl:otherwise>
                <li class="lista-item">
                    <span><xsl:apply-templates /></span>
                </li>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    
    
    <!-- Finally, the following templates match list types not mentioned above. They work for lists of type
        'simple' as well as any unknown list types. -->
<!--    <xsl:template match="dri:list" priority="1">
        <xsl:apply-templates select="dri:head"/>
        <ul>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-simple-list</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="*[not(name()='head')]" mode="nested"/>
        </ul>
    </xsl:template>-->
    
    
    <!-- Generic label handling: simply place the text of the element followed by a period and space. -->
<!--    <xsl:template match="dri:label" priority="1" mode="nested">
        <xsl:copy-of select="./node()"/>
    </xsl:template>-->
    
    <!-- Generic item handling for cases where nothing special needs to be done -->
<!--    <xsl:template match="dri:item" mode="nested">
        <li>
            <xsl:apply-templates />
        </li>
    </xsl:template>-->
    
<!--    <xsl:template match="dri:list/dri:list" priority="1" mode="nested">
        <li>
            <xsl:apply-templates select="."/>
        </li>
    </xsl:template>-->
    
    
    
    <!-- From here on out come the templates for supporting elements that are contained within structural
        ones. These include head (in all its myriad forms), rich text container elements (like hi and figure),
        as well as the field tag and its related elements. The head elements are done first. -->
    
    <!-- The first (and most complex) case of the header tag is the one used for divisions. Since divisions can
        nest freely, their headers should reflect that. Thus, the type of HTML h tag produced depends on how
        many divisions the header tag is nested inside of. -->
    <!-- The font-sizing variable is the result of a linear function applied to the character count of the heading text -->
    <xsl:template match="dri:div/dri:head" priority="3">
        <!--HIDDEN-->
        <xsl:apply-templates />
    </xsl:template>
<!--        <xsl:variable name="head_count" select="count(ancestor::dri:div)"/>
         with the help of the font-sizing variable, the font-size of our header text is made continuously variable based on the character count 
        <xsl:variable name="font-sizing" select="365 - $head_count * 80 - string-length(current())"></xsl:variable>
        <xsl:element name="h{$head_count}">
             in case the chosen size is less than 120%, don't let it go below. Shrinking stops at 120% 
            <xsl:choose>
                <xsl:when test="$font-sizing &lt; 120">
                    <xsl:attribute name="style">font-size: 120%;</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="style">font-size:
                        <xsl:value-of select="$font-sizing"/>%;
                    </xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-div-head</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </xsl:element>
    </xsl:template>-->
    
    <!-- The second case is the header on tables, which always creates an HTML h3 element -->
<!--    <xsl:template match="dri:table/dri:head" priority="2">
        <h3>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table-head</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </h3>
    </xsl:template>-->
    
    <!-- The third case is the header on lists, which creates an HTML h3 element for top level lists and
        and h4 elements for all sublists. -->
<!--    <xsl:template match="dri:list/dri:head" priority="2" mode="nested">
        <h3>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-list-head</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </h3>
    </xsl:template>-->
    
<!--    <xsl:template match="dri:list/dri:list/dri:head" priority="3" mode="nested">
        <h4>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-sublist-head</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </h4>
    </xsl:template>-->


    <xsl:template match="dri:list/dri:list/dri:head" priority="4" mode="nested">
        <span>
<!--            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-sublist-head</xsl:with-param>
            </xsl:call-template>-->
            <xsl:apply-templates />
        </span>
        <span class="mais-filtro">
            <img>
                <xsl:attribute name="src">
                    <xsl:value-of select="$images-path"/>
                    <xsl:text>mais_filtro.png</xsl:text>
                </xsl:attribute>
            </img>
        </span>

    </xsl:template>

    <!-- The fourth case is the header on referenceSets, to be discussed below, which creates an HTML h2 element
        for all cases. The reason for this simplistic approach has to do with referenceSets being handled
        differently in many cases, making it difficult to treat them as either divs (with scaling headers) or
        lists (with static ones). In this case, the simplest solution was chosen, although it is subject to
        change in the future. -->
<!--    <xsl:template match="dri:referenceSet/dri:head" priority="2">
        <h3>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-list-head</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </h3>
    </xsl:template>-->
    
    <!-- Finally, the generic header element template, given the lowest priority, is there for cases not
        covered above. It assumes nothing about the parent element and simply generates an HTML h3 tag -->
<!--    <xsl:template match="dri:head" priority="1">
        <h3>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-head</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </h3>
    </xsl:template>-->


    <xsl:template match="dri:div[@n='search']/dri:div[@n='general-query']" priority="3">
        <!--HIDDEN-->
    </xsl:template>
    

    <xsl:template match="dri:div[@n='search']/dri:p[@n='result-query']" priority="3">
        <!--HIDDEN-->
    </xsl:template>


    <xsl:template match="dri:body/dri:div[starts-with(@n, 'browse-by')]" priority="1">
        <div class="alfabeto cor1">
            <xsl:apply-templates select="dri:div[@n='browse-navigation']/dri:list"/>
        </div>
        <div id="lista" class="caixa-borda2 cor1">
            <xsl:apply-templates select="dri:div[starts-with(@n, 'browse-by-')]/dri:table" />
            <xsl:comment>&#160;</xsl:comment>
        </div>
        <div class="alfabeto cor1">
            <xsl:apply-templates select="dri:div[@n='browse-navigation']/dri:list"/>
        </div>
    </xsl:template>

    
    <!-- Next come the components of rich text containers, namely: hi, xref, figure and, in case of interactive
        divs, field. All these can mix freely with text as well as contain text of their own. The templates for
        the first three elements are fairly straightforward, as they simply create HTML span, a, and img tags,
        respectively. -->
    
    <xsl:template match="dri:hi">
        <span>
            <xsl:attribute name="class">emphasis</xsl:attribute>
            <xsl:if test="@rend">
                <xsl:attribute name="class">
                    <xsl:value-of select="@rend"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </span>
    </xsl:template>
    
    <xsl:template match="dri:xref">
        <a>
            <xsl:attribute name="href">
                <xsl:value-of select="@target"/>
            </xsl:attribute>
            <xsl:attribute name="class">
                <xsl:value-of select="@rend"/>
            </xsl:attribute>
            <xsl:apply-templates />
        </a>
    </xsl:template>
    
    <xsl:template match="dri:figure">
        <xsl:if test="@target">
            <a>
                <xsl:attribute name="href">
                    <xsl:value-of select="@target"/>
                </xsl:attribute>
                <img>
                    <xsl:attribute name="src">
                        <xsl:value-of select="@source"/>
                    </xsl:attribute>
                    <xsl:attribute name="alt">
                        <xsl:apply-templates />
                    </xsl:attribute>
                </img>
            </a>
        </xsl:if>
        <xsl:if test="not(@target)">
            <img>
                <xsl:attribute name="src">
                    <xsl:value-of select="@source"/>
                </xsl:attribute>
                <xsl:attribute name="alt">
                    <xsl:apply-templates />
                </xsl:attribute>
            </img>
        </xsl:if>
    </xsl:template>
    
    
    
    
    
    
    
    
    
    
    
    <!-- The hadling of the special case of instanced composite fields under "form" lists -->
    <xsl:template match="dri:field[@type='composite'][dri:field/dri:instance | dri:params/@operations]" mode="formComposite" priority="2">
        <xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
        <div id="adicionar-busca">
            <xsl:apply-templates select="dri:field" mode="compositeComponent"/>
            <xsl:if test="contains(dri:params/@operations,'add')">
                <!-- Had to add a hidden input because there are other inputs with type 'submit', and only the first one is submitted when enter key is pressed in a text field. -->
                <!-- Add buttons should be named "submit_[field]_add" so that we can ignore errors from required fields when simply adding new values-->
                <input id="botao-enviar2" type="submit" i18n:attr="title" title="xmlui.general.add_filter" class="ds-button-field ds-add-button">
                  <!-- Make invisible if we have choice-lookup operation that provides its own Add. -->
                    <xsl:if test="dri:params/@choicesPresentation = 'lookup'">
                        <xsl:attribute name="style">
                            <xsl:text>display:none;</xsl:text>
                        </xsl:attribute>
                    </xsl:if>
                </input>
                <input type="hidden" i18n:attr="value" value="xmlui.general.add_filter" name="{concat('submit_',@n,'_add')}"/>
            </xsl:if>
            <!-- insert choice mechansim and/or Add button here -->
            <xsl:choose>
                <xsl:when test="dri:params/@choicesPresentation = 'suggest'">
                    <xsl:message terminate="yes">
                        <xsl:text>ERROR: Input field with "suggest" (autocomplete) choice behavior is not implemented for Composite (e.g. "name") fields.</xsl:text>
                    </xsl:message>
                </xsl:when>
              <!-- lookup popup includes its own Add button if necessary. -->
                <xsl:when test="dri:params/@choicesPresentation = 'lookup'">
                    <xsl:call-template name="addLookupButton">
                        <xsl:with-param name="isName" select="'true'"/>
                        <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                    </xsl:call-template>
                </xsl:when>
            </xsl:choose>
            <!-- place to store authority value -->
            <xsl:if test="dri:params/@authorityControlled">
                <xsl:call-template name="authorityConfidenceIcon">
                    <xsl:with-param name="confidence" select="dri:value[@type='authority']/@confidence"/>
                    <xsl:with-param name="id" select="$confidenceIndicatorID"/>
                </xsl:call-template>
                <xsl:call-template name="authorityInputFields">
                    <xsl:with-param name="name" select="@n"/>
                    <xsl:with-param name="authValue" select="dri:value[@type='authority']/text()"/>
                    <xsl:with-param name="confValue" select="dri:value[@type='authority']/@confidence"/>
                </xsl:call-template>
            </xsl:if>
<!--            <div class="spacer">&#160;</div>-->
            <xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent"/>
            <xsl:apply-templates select="dri:error" mode="compositeComponent"/>
<!--            <xsl:apply-templates select="dri:help" mode="compositeComponent"/>-->
            <xsl:if test="dri:instance or dri:field/dri:instance">
                <div class="ds-previous-values">
                    <xsl:call-template name="fieldIterator">
                        <xsl:with-param name="position">1</xsl:with-param>
                    </xsl:call-template>
                    <xsl:if test="contains(dri:params/@operations,'delete') and (dri:instance or dri:field/dri:instance)">
                        <!-- Delete buttons should be named "submit_[field]_delete" so that we can ignore errors from required fields when simply removing values-->
                        <input type="submit" value="Remove selected" name="{concat('submit_',@n,'_delete')}" class="ds-button-field ds-delete-button" />
                    </xsl:if>
                    <xsl:for-each select="dri:field">
                        <xsl:apply-templates select="dri:instance" mode="hiddenInterpreter"/>
                    </xsl:for-each>
                </div>
            </xsl:if>
        </div>
    </xsl:template>
    
    
    
    
    <!-- TODO: The field section works but needs a lot of scrubbing. I would say a third of the
        templates involved are either bloated or superfluous. -->
           
    
    <!-- Things I know:
        1. I can tell a field is multivalued if it has instances in it
        2. I can't really do that for composites, although I can check its
            component fields for condition 1 above.
        3. Fields can also be inside "form" lists, which is its own unique condition
    -->
        
    <!-- Fieldset (instanced) field stuff, in the case of non-composites -->
    <xsl:template match="dri:field[dri:field/dri:instance | dri:params/@operations]" priority="2">
        <!-- Create the first field normally -->
        <xsl:apply-templates select="." mode="normalField"/>
        <!-- Follow it up with an ADD button if the add operation is specified. This allows
            entering more than one value for this field. -->
        <xsl:if test="contains(dri:params/@operations,'add')">
            <!-- Add buttons should be named "submit_[field]_add" so that we can ignore errors from required fields when simply adding new values-->
            <input type="submit" value="Add" name="{concat('submit_',@n,'_add')}" class="ds-button-field ds-add-button">
              <!-- Make invisible if we have choice-lookup popup that provides its own Add. -->
                <xsl:if test="dri:params/@choicesPresentation = 'lookup'">
                    <xsl:attribute name="style">
                        <xsl:text>display:none;</xsl:text>
                    </xsl:attribute>
                </xsl:if>
            </input>
        </xsl:if>
        <br/>
        <xsl:apply-templates select="dri:help" mode="help"/>
        <xsl:apply-templates select="dri:error" mode="error"/>
        <xsl:if test="dri:instance">
            <div class="ds-previous-values">
                <!-- Iterate over the dri:instance elements contained in this field. The instances contain
                    stored values as either "interpreted", "raw", or "default" values. -->
                <xsl:call-template name="simpleFieldIterator">
                    <xsl:with-param name="position">1</xsl:with-param>
                </xsl:call-template>
                <!-- Conclude with a DELETE button if the delete operation is specified. This allows
                    removing one or more values stored for this field. -->
                <xsl:if test="contains(dri:params/@operations,'delete') and dri:instance">
                    <!-- Delete buttons should be named "submit_[field]_delete" so that we can ignore errors from required fields when simply removing values-->
                    <input type="submit" value="Remove selected" name="{concat('submit_',@n,'_delete')}" class="ds-button-field ds-delete-button" />
                </xsl:if>
                <!-- Behind the scenes, add hidden fields for every instance set. This is to make sure that
                    the form still submits the information in those instances, even though they are no
                    longer encoded as HTML fields. The DRI Reference should contain the exact attributes
                    the hidden fields should have in order for this to work properly. -->
                <xsl:apply-templates select="dri:instance" mode="hiddenInterpreter"/>
            </div>
        </xsl:if>
    </xsl:template>
    
    <!-- The iterator is a recursive function that creates a checkbox (to be used in deletion) for
        each value instance and interprets the value inside. It also creates a hidden field from the
        raw value contained in the instance. -->
    <xsl:template name="simpleFieldIterator">
        <xsl:param name="position"/>
        <xsl:if test="dri:instance[position()=$position]">
            <input type="checkbox" value="{concat(@n,'_',$position)}" name="{concat(@n,'_selected')}"/>
            <xsl:apply-templates select="dri:instance[position()=$position]" mode="interpreted"/>

            <!-- look for authority value in instance. -->
            <xsl:if test="dri:instance[position()=$position]/dri:value[@type='authority']">
                <xsl:call-template name="authorityConfidenceIcon">
                    <xsl:with-param name="confidence" select="dri:instance[position()=$position]/dri:value[@type='authority']/@confidence"/>
                </xsl:call-template>
            </xsl:if>
            <br/>
            <xsl:call-template name="simpleFieldIterator">
                <xsl:with-param name="position">
                    <xsl:value-of select="$position + 1"/>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>
    
    <!-- Authority: added fields for auth values as well. -->
    <!-- Common case: use the raw value of the instance to create the hidden field -->
    <xsl:template match="dri:instance" mode="hiddenInterpreter">
        <input type="hidden">
            <xsl:attribute name="name">
                <xsl:value-of select="concat(../@n,'_',position())"/>
            </xsl:attribute>
            <xsl:attribute name="value">
                <xsl:value-of select="dri:value[@type='raw']"/>
            </xsl:attribute>
        </input>
        <!-- XXX do we want confidence icon here?? -->
        <xsl:if test="dri:value[@type='authority']">
            <xsl:call-template name="authorityInputFields">
                <xsl:with-param name="name" select="../@n"/>
                <xsl:with-param name="position" select="position()"/>
                <xsl:with-param name="authValue" select="dri:value[@type='authority']/text()"/>
                <xsl:with-param name="confValue" select="dri:value[@type='authority']/@confidence"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>
    
    <!-- Select box case: use the selected options contained in the instance to create the hidden fields -->
    <xsl:template match="dri:field[@type='select']/dri:instance" mode="hiddenInterpreter">
        <xsl:variable name="position" select="position()"/>
        <xsl:for-each select="dri:value[@type='option']">
            <input type="hidden">
                <xsl:attribute name="name">
                    <xsl:value-of select="concat(../../@n,'_',$position)"/>
                </xsl:attribute>
                <!-- Since the dri:option and dri:values inside a select field are related by the return
                    value, encoded in @returnValue and @option attributes respectively, the option
                    attribute can be used directly instead of being resolved to the the correct option -->
                <xsl:attribute name="value">
                    <!--<xsl:value-of select="../../dri:option[@returnValue = current()/@option]"/>-->
                    <xsl:value-of select="@option"/>
                </xsl:attribute>
            </input>
        </xsl:for-each>
    </xsl:template>
      
    
    
    <!-- Composite instanced field stuff -->
    <!-- It is also the one that receives the special error and help handling -->
    <xsl:template match="dri:field[@type='composite'][dri:field/dri:instance | dri:params/@operations]" priority="3">
        <!-- First is special, so first we grab all the values from the child fields.
            We do this by applying normal templates to the field, which should ignore instances. -->
        <span class="ds-composite-field">
            <xsl:apply-templates select="dri:field" mode="compositeComponent"/>
        </span>
        <xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent"/>
        <xsl:apply-templates select="dri:error" mode="compositeComponent"/>
        <xsl:apply-templates select="dri:help" mode="compositeComponent"/>
        <!-- Insert choice mechanism here.
             Follow it up with an ADD button if the add operation is specified. This allows
            entering more than one value for this field. -->

        <xsl:if test="contains(dri:params/@operations,'add')">
            <!-- Add buttons should be named "submit_[field]_add" so that we can ignore errors from required fields when simply adding new values-->
            <input type="submit" value="Add" name="{concat('submit_',@n,'_add')}" class="ds-button-field ds-add-button">
              <!-- Make invisible if we have choice-lookup popup that provides its own Add. -->
                <xsl:if test="dri:params/@choicesPresentation = 'lookup'">
                    <xsl:attribute name="style">
                        <xsl:text>display:none;</xsl:text>
                    </xsl:attribute>
                </xsl:if>
            </input>
        </xsl:if>

        <xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
        <xsl:if test="dri:params/@authorityControlled">
          <!-- XXX note that this is wrong and won't get any authority values, but
             - for instanced inputs the entry box starts out empty anyway.
            -->
            <xsl:call-template name="authorityConfidenceIcon">
                <xsl:with-param name="confidence" select="dri:value[@type='authority']/@confidence"/>
                <xsl:with-param name="id" select="$confidenceIndicatorID"/>
            </xsl:call-template>
            <xsl:call-template name="authorityInputFields">
                <xsl:with-param name="name" select="@n"/>
                <xsl:with-param name="id" select="@id"/>
                <xsl:with-param name="authValue" select="dri:value[@type='authority']/text()"/>
                <xsl:with-param name="confValue" select="dri:value[@type='authority']/@confidence"/>
            </xsl:call-template>
        </xsl:if>
        <xsl:choose>
            <xsl:when test="dri:params/@choicesPresentation = 'suggest'">
                <xsl:call-template name="addAuthorityAutocomplete">
                    <xsl:with-param name="confidenceIndicatorID" select="$confidenceIndicatorID"/>
                </xsl:call-template>
            </xsl:when>
          <!-- lookup popup includes its own Add button if necessary. -->
          <!-- XXX does this need a Confidence Icon? -->
            <xsl:when test="dri:params/@choicesPresentation = 'lookup'">
                <xsl:call-template name="addLookupButton">
                    <xsl:with-param name="isName" select="'true'"/>
                    <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                </xsl:call-template>
            </xsl:when>
        </xsl:choose>
        <br/>
        <xsl:if test="dri:instance or dri:field/dri:instance">
            <div class="ds-previous-values">
                <xsl:call-template name="fieldIterator">
                    <xsl:with-param name="position">1</xsl:with-param>
                </xsl:call-template>
                <!-- Conclude with a DELETE button if the delete operation is specified. This allows
                    removing one or more values stored for this field. -->
                <xsl:if test="contains(dri:params/@operations,'delete') and (dri:instance or dri:field/dri:instance)">
                    <!-- Delete buttons should be named "submit_[field]_delete" so that we can ignore errors from required fields when simply removing values-->
                    <input type="submit" value="Remove selected" name="{concat('submit_',@n,'_delete')}" class="ds-button-field ds-delete-button" />
                </xsl:if>
                <xsl:for-each select="dri:field">
                    <xsl:apply-templates select="dri:instance" mode="hiddenInterpreter"/>
                </xsl:for-each>
            </div>
        </xsl:if>
    </xsl:template>
        
    <!-- The iterator is a recursive function that creates a checkbox (to be used in deletion) for
        each value instance and interprets the value inside. It also creates a hidden field from the
        raw value contained in the instance.
        
         What makes it different from the simpleFieldIterator is that it works with a composite field's
        components rather than a single field, which requires it to consider several sets of instances. -->
    <xsl:template name="fieldIterator">
        <xsl:param name="position"/>
        <!-- add authority value for this instance -->
        <xsl:if test="dri:instance[position()=$position]/dri:value[@type='authority']">
            <xsl:call-template name="authorityInputFields">
                <xsl:with-param name="name" select="@n"/>
                <xsl:with-param name="position" select="$position"/>
                <xsl:with-param name="authValue" select="dri:instance[position()=$position]/dri:value[@type='authority']/text()"/>
                <xsl:with-param name="confValue" select="dri:instance[position()=$position]/dri:value[@type='authority']/@confidence"/>
            </xsl:call-template>
        </xsl:if>
        <xsl:choose>
            <!-- First check to see if the composite itself has a non-empty instance value in that
                position. In that case there is no need to go into the individual fields. -->
            <xsl:when test="count(dri:instance[position()=$position]/dri:value[@type != 'authority'])">
                <input type="checkbox" value="{concat(@n,'_',$position)}" name="{concat(@n,'_selected')}"/>
                <xsl:apply-templates select="dri:instance[position()=$position]" mode="interpreted"/>
                <xsl:call-template name="authorityConfidenceIcon">
                    <xsl:with-param name="confidence" select="dri:instance[position()=$position]/dri:value[@type='authority']/@confidence"/>
                </xsl:call-template>
                <br/>
                <xsl:call-template name="fieldIterator">
                    <xsl:with-param name="position">
                        <xsl:value-of select="$position + 1"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <!-- Otherwise, build the string from the component fields -->
            <xsl:when test="dri:field/dri:instance[position()=$position]">
                <input type="checkbox" value="{concat(@n,'_',$position)}" name="{concat(@n,'_selected')}"/>
                <xsl:apply-templates select="dri:field" mode="compositeField">
                    <xsl:with-param name="position" select="$position"/>
                </xsl:apply-templates>
                <br/>
                <xsl:call-template name="fieldIterator">
                    <xsl:with-param name="position">
                        <xsl:value-of select="$position + 1"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:when>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="dri:field[@type='text' or @type='textarea']" mode="compositeField">
        <xsl:param name="position">1</xsl:param>
        <xsl:if test="not(position()=1)">
            <xsl:text>, </xsl:text>
        </xsl:if>
        <!--<input type="hidden" name="{concat(@n,'_',$position)}" value="{dri:instance[position()=$position]/dri:value[@type='raw']}"/>-->
        <xsl:choose>
            <xsl:when test="dri:instance[position()=$position]/dri:value[@type='interpreted']">
                <span class="ds-interpreted-field">
                    <xsl:apply-templates select="dri:instance[position()=$position]/dri:value[@type='interpreted']" mode="interpreted"/>
                </span>
            </xsl:when>
            <xsl:when test="dri:instance[position()=$position]/dri:value[@type='raw']">
                <span class="ds-interpreted-field">
                    <xsl:apply-templates select="dri:instance[position()=$position]/dri:value[@type='raw']" mode="interpreted"/>
                </span>
            </xsl:when>
            <xsl:when test="dri:instance[position()=$position]/dri:value[@type='default']">
                <span class="ds-interpreted-field">
                    <xsl:apply-templates select="dri:instance[position()=$position]/dri:value[@type='default']" mode="interpreted"/>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <span class="ds-interpreted-field">No value submitted.</span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    
    <xsl:template match="dri:field[@type='select']" mode="compositeField">
        <xsl:param name="position">1</xsl:param>
        <xsl:if test="not(position()=1)">
            <xsl:text>, </xsl:text>
        </xsl:if>
        <xsl:for-each select="dri:instance[position()=$position]/dri:value[@type='option']">
            <!--<input type="hidden" name="{concat(@n,'_',$position)}" value="{../../dri:option[@returnValue = current()/@option]}"/>-->
        </xsl:for-each>
        <xsl:choose>
            <xsl:when test="dri:instance[position()=$position]/dri:value[@type='interpreted']">
                <span class="ds-interpreted-field">
                    <xsl:apply-templates select="dri:instance[position()=$position]/dri:value[@type='interpreted']" mode="interpreted"/>
                </span>
            </xsl:when>
            <xsl:when test="dri:instance[position()=$position]/dri:value[@type='option']">
                <span class="ds-interpreted-field">
                    <xsl:for-each select="dri:instance[position()=$position]/dri:value[@type='option']">
                        <xsl:if test="position()=1">
                            <xsl:text>(</xsl:text>
                        </xsl:if>
                        <xsl:value-of select="../../dri:option[@returnValue = current()/@option]"/>
                        <xsl:if test="position()=last()">
                            <xsl:text>)</xsl:text>
                        </xsl:if>
                        <xsl:if test="not(position()=last())">
                            <xsl:text>, </xsl:text>
                        </xsl:if>
                    </xsl:for-each>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <span class="ds-interpreted-field">No value submitted.</span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- TODO: make this work? Maybe checkboxes and radio buttons should not be instanced... -->
    <xsl:template match="dri:field[@type='checkbox' or @type='radio']" mode="compositeField">
        <xsl:param name="position">1</xsl:param>
        <xsl:if test="not(position()=1)">
            <xsl:text>, </xsl:text>
        </xsl:if>
        <span class="ds-interpreted-field">Checkbox</span>
    </xsl:template>
    
    
    
<!--    <xsl:template match="dri:field[@type='button' and @n='submit']">-->
    <xsl:template match="dri:field[@type='button']">
        <input>
            <xsl:call-template name="fieldAttributes"/>
            <xsl:attribute name="type">submit</xsl:attribute>
            <xsl:attribute name="value">
                <xsl:choose>
                    <xsl:when test="./dri:value[@type='raw']">
                        <xsl:value-of select="./dri:value[@type='raw']"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="./dri:value[@type='default']"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:if test="dri:value/i18n:text">
                <xsl:attribute name="i18n:attr">value</xsl:attribute>
            </xsl:if>
            <xsl:attribute name="id">botao-enviar3</xsl:attribute>
            <!-- BUG: overriding button name because default templates use @n as the button name, but @n is 'submit',
                 which prevents the calling of this.form.submit(). -->
            <xsl:attribute name="name">submit-button</xsl:attribute>
<!--            <xsl:apply-templates />-->
        </input>

    </xsl:template>

    
    
    
    
    
    
    <xsl:template match="dri:field[@type='select']/dri:instance" mode="interpreted">
        <span class="ds-interpreted-field">
            <xsl:for-each select="dri:value[@type='option']">
                <!--<input type="hidden" name="{concat(../@n,'_',position())}" value="{../../dri:option[@returnValue = current()/@option]}"/>-->
                <xsl:if test="position()=1">
                    <xsl:text>(</xsl:text>
                </xsl:if>
                <xsl:value-of select="../../dri:option[@returnValue = current()/@option]"/>
                <xsl:if test="position()=last()">
                    <xsl:text>)</xsl:text>
                </xsl:if>
                <xsl:if test="not(position()=last())">
                    <xsl:text>, </xsl:text>
                </xsl:if>
            </xsl:for-each>
        </span>
    </xsl:template>
    
    
    <xsl:template match="dri:instance" mode="interpreted">
        <!--<input type="hidden" name="{concat(../@n,'_',position())}" value="dri:value[@type='raw']"/>-->
        <xsl:choose>
            <xsl:when test="dri:value[@type='interpreted']">
                <span class="ds-interpreted-field">
                    <xsl:apply-templates select="dri:value[@type='interpreted']" mode="interpreted"/>
                </span>
            </xsl:when>
            <xsl:when test="dri:value[@type='raw']">
                <span class="ds-interpreted-field">
                    <xsl:apply-templates select="dri:value[@type='raw']" mode="interpreted"/>
                </span>
            </xsl:when>
            <xsl:when test="dri:value[@type='default']">
                <span class="ds-interpreted-field">
                    <xsl:apply-templates select="dri:value[@type='default']" mode="interpreted"/>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <span class="ds-interpreted-field">No value submitted.</span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
        
    
    
    
    <xsl:template match="dri:value" mode="interpreted">
        <xsl:apply-templates />
    </xsl:template>
    
    <!--
    <xsl:template match="dri:field">
    
        Possible child elements:
        params(one), help(zero or one), error(zero or one), value(any), field(one or more – only with the composite type)
        
        Possible attributes:
        @n, @id, @rend
        @disabled
        @required
        @type =
            button: A button input control that when activated by the user will submit the form, including all the fields, back to the server for processing.
            checkbox: A boolean input control which may be toggled by the user. A checkbox may have several fields which share the same name and each of those fields may be toggled independently. This is distinct from a radio button where only one field may be toggled.
            file: An input control that allows the user to select files to be submitted with the form. Note that a form which uses a file field must use the multipart method.
            hidden: An input control that is not rendered on the screen and hidden from the user.
            password: A single-line text input control where the input text is rendered in such a way as to hide the characters from the user.
            radio:  A boolean input control which may be toggled by the user. Multiple radio button fields may share the same name. When this occurs only one field may be selected to be true. This is distinct from a checkbox where multiple fields may be toggled.
            select: A menu input control which allows the user to select from a list of available options.
            text: A single-line text input control.
            textarea: A multi-line text input control.
            composite: A composite input control combines several input controls into a single field. The only fields that may be combined together are: checkbox, password, select, text, and textarea. When fields are combined together they can posses multiple combined values.
    </xsl:template>
        -->
    
    
    
    <!-- The handling of component fields, that is fields that are part of a composite field type -->
    <xsl:template match="dri:field" mode="compositeComponent">
        <xsl:choose>
            <xsl:when test="@type='checkbox'  or @type='radio'">
                <xsl:apply-templates select="." mode="normalField"/>
                <xsl:apply-templates select="dri:label" mode="compositeComponent"/>
            </xsl:when>
            <xsl:otherwise>
<!--                <label class="ds-composite-component">-->
                    <xsl:if test="position()=last()">
                        <xsl:attribute name="class">ds-composite-component last</xsl:attribute>
                    </xsl:if>
                    <xsl:apply-templates select="." mode="normalField"/>
                    <xsl:apply-templates select="dri:label" mode="compositeComponent"/>
<!--                </label>-->
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="dri:error" mode="compositeComponent">
        <xsl:apply-templates select="." mode="error"/>
    </xsl:template>
    
    <xsl:template match="dri:help" mode="compositeComponent">
        <span class="composite-help">
            <xsl:apply-templates />
        </span>
    </xsl:template>
    
    
        
    <!-- The handling of the field element is more complex. At the moment, the handling of input fields in the
        DRI schema is very similar to HTML, utilizing the same controlled vocabulary in most cases. This makes
        converting DRI fields to HTML inputs a straightforward, if a bit verbose, task. We are currently
        looking at other ways of encoding forms, so this may change in the future. -->
    <!-- The simple field case... not part of a complex field and does not contain instance values -->
    <xsl:template match="dri:field">
        <xsl:apply-templates select="." mode="normalField"/>
        <xsl:if test="not(@type='composite') and ancestor::dri:list[@type='form']">
            <!--
            <xsl:if test="not(@type='checkbox') and not(@type='radio') and not(@type='button')">
                <br/>
            </xsl:if>
            -->
            <xsl:apply-templates select="dri:help" mode="help"/>
            <xsl:apply-templates select="dri:error" mode="error"/>
        </xsl:if>
    </xsl:template>


    <xsl:template match="dri:field[@type='select']" mode="header">
        <div>
            <select>
                <xsl:call-template name="fieldAttributes"/>
                <xsl:attribute name="id">selecionar-filtro</xsl:attribute>
                <xsl:attribute name="class">selecionar-display</xsl:attribute>
<!--            <xsl:attribute name="onchange">this.form.submit()</xsl:attribute>-->

                <xsl:apply-templates />
            </select>
        </div>
    </xsl:template>


    <xsl:template match="dri:field[@type='select']" mode="searchControls">
        <select>
            <xsl:call-template name="fieldAttributes"/>
            <xsl:attribute name="id">selecionar</xsl:attribute>
            <xsl:attribute name="onchange">this.form.submit()</xsl:attribute>

            <xsl:apply-templates mode="searchControls"/>
        </select>

    </xsl:template>
<!--        <select>
            <xsl:call-template name="fieldAttributes"/>
            <xsl:attribute name="class">
                <xsl:text>selecionar</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="onchange">this.form.submit()</xsl:attribute>
            <xsl:attribute name="onchange">
                <xsl:text>this.form.submit()</xsl:text>
                <xsl:text>javascript:tSubmit(document.getElementById('formulario-busca'));document.forms['formulario-busca'].submit()</xsl:text>
                <xsl:text>javascript:tSubmit(document.getElementById('formulario-busca'))</xsl:text>
            </xsl:attribute>
            <xsl:apply-templates select="//dri:table[@n='search-controls']//dri:cell/dri:field[@n='rpp']/*"/>
        </select>-->


    
    <xsl:template match="dri:field[@type='select']/dri:option" mode="searchControls">
        <option>
            <xsl:attribute name="value">
                <xsl:value-of select="@returnValue"/>
            </xsl:attribute>
            <xsl:if test="../dri:value[@type='option'][@option = current()/@returnValue]">
                <xsl:attribute name="selected">selected</xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </option>
    </xsl:template>

    <xsl:template match="dri:field/dri:option" mode="searchControlsImage">
        <xsl:param name="selected"/>
        <xsl:param name="deselected"/>

<!--        <option>
            <xsl:attribute name="value">
                <xsl:value-of select="@returnValue"/>
            </xsl:attribute>
            <xsl:if test="../dri:value[@type='option'][@option = current()/@returnValue]">
                <xsl:attribute name="selected">selected</xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </option>-->


        <input type="submit">
            <xsl:attribute name="class">
                <xsl:text>botao_barra </xsl:text>
                <xsl:value-of select="../@n"/>
                <xsl:text>_</xsl:text>
                <xsl:call-template name="convertToPlainLowCaps">
                    <xsl:with-param name="from_text" select="@returnValue"/>
                </xsl:call-template>
                <xsl:if test="../dri:value[@type='option'][@option = current()/@returnValue]">
                    <xsl:text> selected</xsl:text>
                </xsl:if>
            </xsl:attribute>
            <xsl:attribute name="name">
                <xsl:value-of select="../@n"/>
            </xsl:attribute>
            <xsl:attribute name="value">
                <xsl:value-of select="@returnValue"/>
            </xsl:attribute>
            <xsl:attribute name="title">
                <xsl:apply-templates select="."/>
            </xsl:attribute>
            <xsl:if test="i18n:text">
                <xsl:attribute name="i18n:attr">title</xsl:attribute>
            </xsl:if>

<!--            <xsl:choose>
                <xsl:when test="../dri:value[@type='option'][@option = current()/@returnValue]">
                    <xsl:attribute name="src">
                        <xsl:value-of select="$images-path"/>
                        <xsl:value-of select="$selected"/>
                    </xsl:attribute>
                </xsl:when>

                <xsl:otherwise>
                    <xsl:attribute name="src">
                        <xsl:value-of select="$images-path"/>
                        <xsl:value-of select="$deselected"/>
                    </xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>-->
        </input>
<!--        <input type="image">
            <xsl:attribute name="name">
                <xsl:value-of select="../@n"/>
            </xsl:attribute>
            <xsl:attribute name="value">
                <xsl:value-of select="@returnValue"/>
            </xsl:attribute>
            <xsl:attribute name="title">
                <xsl:apply-templates select="."/>
            </xsl:attribute>
            <xsl:if test="i18n:text">
                <xsl:attribute name="i18n:attr">title</xsl:attribute>
            </xsl:if>

            <xsl:choose>
                <xsl:when test="../dri:value[@type='option'][@option = current()/@returnValue]">
                    <xsl:attribute name="src">
                        <xsl:value-of select="$images-path"/>
                        <xsl:value-of select="$selected"/>
                    </xsl:attribute>
                </xsl:when>

                <xsl:otherwise>
                    <xsl:attribute name="src">
                        <xsl:value-of select="$images-path"/>
                        <xsl:value-of select="$deselected"/>
                    </xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
        </input>-->
    </xsl:template>


    <xsl:template match="dri:field" mode="normalField">
        <xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
        <xsl:choose>
            <!-- TODO: this has changed drammatically (see form3.xml) -->
            <xsl:when test="@type= 'select'">
                <select>
                    <xsl:call-template name="fieldAttributes"/>
                    <xsl:attribute name="id">filtertype</xsl:attribute>
                    <!-- Escondendo o tipo de filtro por enquanto. -->
<!--                    <xsl:attribute name="style">
                        <xsl:text>display:none;</xsl:text>
                    </xsl:attribute>-->

                    <xsl:apply-templates/>
                </select>
            </xsl:when>
            <xsl:when test="@type= 'textarea'">
                <textarea>
                    <xsl:call-template name="fieldAttributes"/>
                                    
                                    <!--
                                        if the cols and rows attributes are not defined we need to call
                                        the tempaltes for them since they are required attributes in strict xhtml
                                     -->
                    <xsl:choose>
                        <xsl:when test="not(./dri:params[@cols])">
                            <xsl:call-template name="textAreaCols"/>
                        </xsl:when>
                    </xsl:choose>
                    <xsl:choose>
                        <xsl:when test="not(./dri:params[@rows])">
                            <xsl:call-template name="textAreaRows"/>
                        </xsl:when>
                    </xsl:choose>
                                    
                    <xsl:apply-templates />
                    <xsl:choose>
                        <xsl:when test="./dri:value[@type='raw']">
                            <xsl:copy-of select="./dri:value[@type='raw']/node()"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:copy-of select="./dri:value[@type='default']/node()"/>
                        </xsl:otherwise>
                    </xsl:choose>
                    <xsl:if  test="string-length(./dri:value) &lt; 1">
                        <i18n:text>xmlui.dri2xhtml.default.textarea.value</i18n:text>
                    </xsl:if>
                </textarea>
                                    

              <!-- add place to store authority value -->
                <xsl:if test="dri:params/@authorityControlled">
                    <xsl:variable name="confidence">
                        <xsl:if test="./dri:value[@type='authority']">
                            <xsl:value-of select="./dri:value[@type='authority']/@confidence"/>
                        </xsl:if>
                    </xsl:variable>
                <!-- add authority confidence widget -->
                    <xsl:call-template name="authorityConfidenceIcon">
                        <xsl:with-param name="confidence" select="$confidence"/>
                        <xsl:with-param name="id" select="$confidenceIndicatorID"/>
                    </xsl:call-template>
                    <xsl:call-template name="authorityInputFields">
                        <xsl:with-param name="name" select="@n"/>
                        <xsl:with-param name="id" select="@id"/>
                        <xsl:with-param name="authValue" select="dri:value[@type='authority']/text()"/>
                        <xsl:with-param name="confValue" select="dri:value[@type='authority']/@confidence"/>
                        <xsl:with-param name="confIndicatorID" select="$confidenceIndicatorID"/>
                        <xsl:with-param name="unlockButton" select="dri:value[@type='authority']/dri:field[@rend='ds-authority-lock']/@n"/>
                        <xsl:with-param name="unlockHelp" select="dri:value[@type='authority']/dri:field[@rend='ds-authority-lock']/dri:help"/>
                    </xsl:call-template>
                </xsl:if>
              <!-- add choice mechanisms -->
                <xsl:choose>
                    <xsl:when test="dri:params/@choicesPresentation = 'suggest'">
                        <xsl:call-template name="addAuthorityAutocomplete">
                            <xsl:with-param name="confidenceIndicatorID" select="$confidenceIndicatorID"/>
                            <xsl:with-param name="confidenceName">
                                <xsl:value-of select="concat(@n,'_confidence')"/>
                            </xsl:with-param>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:when test="dri:params/@choicesPresentation = 'lookup'">
                        <xsl:call-template name="addLookupButton">
                            <xsl:with-param name="isName" select="'false'"/>
                            <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                        </xsl:call-template>
                    </xsl:when>
                </xsl:choose>
            </xsl:when>

            <!-- This is changing drammatically -->
            <xsl:when test="@type= 'checkbox' or @type= 'radio'">
                <fieldset>
                    <xsl:call-template name="standardAttributes">
                        <xsl:with-param name="class">
                            <xsl:text>ds-</xsl:text>
                            <xsl:value-of select="@type"/>
                            <xsl:text>-field </xsl:text>
                            <xsl:if test="dri:error">
                                <xsl:text>error </xsl:text>
                            </xsl:if>
                        </xsl:with-param>
                    </xsl:call-template>
                    <xsl:attribute name="id">
                        <xsl:value-of select="generate-id()"/>
                    </xsl:attribute>
                    <xsl:if test="dri:label">
                        <legend>
                            <xsl:apply-templates select="dri:label" mode="compositeComponent" />
                        </legend>
                    </xsl:if>
                    <xsl:apply-templates />
                </fieldset>
            </xsl:when>
            <!--
                <input>
                            <xsl:call-template name="fieldAttributes"/>
                    <xsl:if test="dri:value[@checked='yes']">
                                <xsl:attribute name="checked">checked</xsl:attribute>
                    </xsl:if>
                    <xsl:apply-templates/>
                </input>
                -->
            <xsl:when test="@type='composite'">
                <!-- TODO: add error and help stuff on top of the composite -->
                <div class="adicionar-busca">
                    <xsl:apply-templates select="dri:field" mode="compositeComponent"/>
                    <xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent"/>
                    <xsl:apply-templates select="dri:error" mode="compositeComponent"/>
                    <xsl:apply-templates select="dri:field/dri:help" mode="compositeComponent"/>
                </div>
                <!--<xsl:apply-templates select="dri:help" mode="compositeComponent"/>-->
            </xsl:when>
                    <!-- text, password, file, and hidden types are handled the same.
                        Buttons: added the xsl:if check which will override the type attribute button
                            with the value 'submit'. No reset buttons for now...
                    -->
            <xsl:otherwise>
                <input>
                    <xsl:call-template name="fieldAttributes"/>
                    <xsl:if test="@type='button'">
                        <xsl:attribute name="type">submit</xsl:attribute>
                    </xsl:if>
                    <xsl:attribute name="value">
                        <xsl:choose>
                            <xsl:when test="./dri:value[@type='raw']">
                                <xsl:value-of select="./dri:value[@type='raw']"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="./dri:value[@type='default']"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <xsl:if test="dri:value/i18n:text">
                        <xsl:attribute name="i18n:attr">value</xsl:attribute>
                    </xsl:if>
                    <xsl:apply-templates />
                </input>

                <xsl:variable name="confIndicatorID" select="concat(@id,'_confidence_indicator')"/>
                <xsl:if test="dri:params/@authorityControlled">
                    <xsl:variable name="confidence">
                        <xsl:if test="./dri:value[@type='authority']">
                            <xsl:value-of select="./dri:value[@type='authority']/@confidence"/>
                        </xsl:if>
                    </xsl:variable>
                          <!-- add authority confidence widget -->
                    <xsl:call-template name="authorityConfidenceIcon">
                        <xsl:with-param name="confidence" select="$confidence"/>
                        <xsl:with-param name="id" select="$confidenceIndicatorID"/>
                    </xsl:call-template>
                    <xsl:call-template name="authorityInputFields">
                        <xsl:with-param name="name" select="@n"/>
                        <xsl:with-param name="id" select="@id"/>
                        <xsl:with-param name="authValue" select="dri:value[@type='authority']/text()"/>
                        <xsl:with-param name="confValue" select="dri:value[@type='authority']/@confidence"/>
                    </xsl:call-template>
                </xsl:if>
                <xsl:choose>
                    <xsl:when test="dri:params/@choicesPresentation = 'suggest'">
                        <xsl:call-template name="addAuthorityAutocomplete">
                            <xsl:with-param name="confidenceIndicatorID" select="$confidenceIndicatorID"/>
                            <xsl:with-param name="confidenceName">
                                <xsl:value-of select="concat(@n,'_confidence')"/>
                            </xsl:with-param>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:when test="dri:params/@choicesPresentation = 'lookup'">
                        <xsl:call-template name="addLookupButton">
                            <xsl:with-param name="isName" select="'false'"/>
                            <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                        </xsl:call-template>
                    </xsl:when>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- A set of standard attributes common to all fields -->
    <xsl:template name="fieldAttributes">
        <xsl:call-template name="standardAttributes">
            <xsl:with-param name="class">
                <xsl:text>ds-</xsl:text>
                <xsl:value-of select="@type"/>
                <xsl:text>-field </xsl:text>
                <xsl:if test="dri:error">
                    <xsl:text>error </xsl:text>
                </xsl:if>
            </xsl:with-param>
        </xsl:call-template>
        <xsl:if test="@disabled='yes'">
            <xsl:attribute name="disabled">disabled</xsl:attribute>
        </xsl:if>
        <xsl:if test="@type != 'checkbox' and @type != 'radio' ">
            <xsl:attribute name="name">
                <xsl:value-of select="@n"/>
            </xsl:attribute>
        </xsl:if>
        <xsl:if test="@type != 'select' and @type != 'textarea' and @type != 'checkbox' and @type != 'radio' ">
            <xsl:attribute name="type">
                <xsl:value-of select="@type"/>
            </xsl:attribute>
        </xsl:if>
        <xsl:if test="@type= 'textarea'">
            <xsl:attribute name="onfocus">javascript:tFocus(this);</xsl:attribute>
        </xsl:if>
    </xsl:template>
        
    <!-- Since the field element contains only the type attribute, all other attributes commonly associated
        with input fields are stored on the params element. Rather than parse the attributes directly, this
        template generates a call to attribute templates, something that is not done in XSL by default. The
        templates for the attributes can be found further down. -->
    <xsl:template match="dri:params">
        <xsl:apply-templates select="@*"/>
    </xsl:template>
    
    
    <xsl:template match="dri:field[@type='select']/dri:option">
        <option>
            <xsl:attribute name="value">
                <xsl:value-of select="@returnValue"/>
            </xsl:attribute>
            <xsl:if test="../dri:value[@type='option'][@option = current()/@returnValue]">
                <xsl:attribute name="selected">selected</xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </option>
    </xsl:template>
    
    <xsl:template match="dri:field[@type='checkbox' or @type='radio']/dri:option">
        <label>
            <input>
                <xsl:attribute name="name">
                    <xsl:value-of select="../@n"/>
                </xsl:attribute>
                <xsl:attribute name="type">
                    <xsl:value-of select="../@type"/>
                </xsl:attribute>
                <xsl:attribute name="value">
                    <xsl:value-of select="@returnValue"/>
                </xsl:attribute>
                <xsl:if test="../dri:value[@type='option'][@option = current()/@returnValue]">
                    <xsl:attribute name="checked">checked</xsl:attribute>
                </xsl:if>
                <xsl:if test="../@disabled='yes'">
                    <xsl:attribute name="disabled">disabled</xsl:attribute>
                </xsl:if>
            </input>
            <xsl:apply-templates />
        </label>
    </xsl:template>

    <xsl:template match="dri:field[@type='checkbox' or @type='radio']/dri:option" mode="dados-item">
        <label>
<!--            <xsl:apply-templates select="*[not(name()='text')]"/>
            <xsl:apply-templates select="text()" mode="shortenedText"/>-->
            <xsl:apply-templates />
            <input>
                <xsl:attribute name="name">
                    <xsl:value-of select="../@n"/>
                </xsl:attribute>
                <xsl:attribute name="type">
                    <xsl:value-of select="../@type"/>
                </xsl:attribute>
                <xsl:attribute name="value">
                    <xsl:value-of select="@returnValue"/>
                </xsl:attribute>
                <xsl:if test="../dri:value[@type='option'][@option = current()/@returnValue]">
                    <xsl:attribute name="checked">checked</xsl:attribute>
                </xsl:if>
                <xsl:if test="../@disabled='yes'">
                    <xsl:attribute name="disabled">disabled</xsl:attribute>
                </xsl:if>

                <xsl:attribute name="onclick">
                    <xsl:text>javascript:submit();</xsl:text>
                </xsl:attribute>
            </input>
        </label>
    </xsl:template>

<!--    <xsl:template match="*[not(self::text())]" mode="shortenedText">
        <xsl:apply-templates />
    </xsl:template>-->

<!--    <xsl:template match="text()" mode="shortenedText">-->
    <xsl:template match="text()">
<!--    <xsl:template match="*[self::text()]">-->
<!--        <xsl:choose>
            <xsl:when test="self::text()">-->
                <xsl:choose>
                    <xsl:when test="$string-max-length and string-length(.) > $string-max-length">
                        <xsl:value-of select="substring(., 1, $string-max-length)"/>
                        <xsl:text> ... </xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="."/>
                    </xsl:otherwise>
                </xsl:choose>
<!--            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="*[not(child::text())]"/>
                <xsl:apply-templates select="child::text()" mode="shortenedText"/>
            </xsl:otherwise>
        </xsl:choose>-->
    </xsl:template>


    <xsl:template match="dri:field[@type='text']" priority="10">
        <input>
            <xsl:call-template name="fieldAttributes"/>
            <xsl:attribute name="id">
                <xsl:text>caixa-busca2</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="name">
                <xsl:value-of select="@n"/>
            </xsl:attribute>
            <xsl:attribute name="type">
                <xsl:value-of select="@type"/>
            </xsl:attribute>
            <xsl:attribute name="value">
                <xsl:choose>
                    <xsl:when test="./dri:value[@type='raw']">
                        <xsl:value-of select="./dri:value[@type='raw']"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="./dri:value[@type='default']"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:if test="dri:value/i18n:text">
                <xsl:attribute name="i18n:attr">value</xsl:attribute>
            </xsl:if>
        </input>

        <xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
        <xsl:if test="dri:params/@authorityControlled">
            <xsl:variable name="confidence">
                <xsl:if test="./dri:value[@type='authority']">
                    <xsl:value-of select="./dri:value[@type='authority']/@confidence"/>
                </xsl:if>
            </xsl:variable>
                          <!-- add authority confidence widget -->
            <xsl:call-template name="authorityConfidenceIcon">
                <xsl:with-param name="confidence" select="$confidence"/>
                <xsl:with-param name="id" select="$confidenceIndicatorID"/>
            </xsl:call-template>
            <xsl:call-template name="authorityInputFields">
                <xsl:with-param name="name" select="@n"/>
                <xsl:with-param name="id" select="@id"/>
                <xsl:with-param name="authValue" select="dri:value[@type='authority']/text()"/>
                <xsl:with-param name="confValue" select="dri:value[@type='authority']/@confidence"/>
            </xsl:call-template>
        </xsl:if>
        <xsl:choose>
            <xsl:when test="dri:params/@choicesPresentation = 'suggest'">
                <xsl:call-template name="addAuthorityAutocomplete">
                    <xsl:with-param name="confidenceIndicatorID" select="$confidenceIndicatorID"/>
                    <xsl:with-param name="confidenceName">
                        <xsl:value-of select="concat(@n,'_confidence')"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="dri:params/@choicesPresentation = 'lookup'">
                <xsl:call-template name="addLookupButton">
                    <xsl:with-param name="isName" select="'false'"/>
                    <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                </xsl:call-template>
            </xsl:when>
        </xsl:choose>
    </xsl:template>


    <xsl:template match="dri:field[@type='text']" mode="compositeComponent" priority="10">
        <input id="caixa-busca2">
            <xsl:attribute name="name">
                <xsl:value-of select="@n"/>
            </xsl:attribute>
            <xsl:attribute name="type">
                <xsl:value-of select="@type"/>
            </xsl:attribute>
<!--            <xsl:attribute name="value">
                <xsl:value-of select="@returnValue"/>
            </xsl:attribute>-->
<!--            <xsl:attribute name="onkeypress">
            </xsl:attribute>-->
        </input>
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="dri:field[@type='text']" mode="header" priority="10">
        <input id="caixa-busca" type="text">
            <xsl:attribute name="name">
                <xsl:value-of select="@n"/>
            </xsl:attribute>
            <xsl:attribute name="type">
                <xsl:value-of select="@type"/>
            </xsl:attribute>
            <xsl:choose>
<!--                <xsl:when test="//dri:field[@id='aspect.discovery.SimpleSearch.field.query']">
                    <xsl:attribute name="value">
                        <xsl:value-of select="//dri:field[@id='aspect.discovery.SimpleSearch.field.query']/dri:value"/>
                    </xsl:attribute>
                </xsl:when>-->
                <xsl:otherwise>
                    <xsl:attribute name="i18n:attr">value</xsl:attribute>
                    <xsl:attribute name="value">
                        <xsl:value-of select="../dri:help"/>
<!--                        xmlui.ArtifactBrowser.AbstractSearch.main_search_box_default-->
                    </xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:attribute name="onFocus"><xsl:text>clearText(this)</xsl:text></xsl:attribute>
            <xsl:attribute name="onBlur"><xsl:text>clearText(this)</xsl:text></xsl:attribute>
        </input>

        <xsl:apply-templates />

<!--            <xsl:attribute name="value">
                <xsl:value-of select="@returnValue"/>
            </xsl:attribute>-->
<!--            <xsl:attribute name="onkeypress">
            </xsl:attribute>-->

    </xsl:template>

    <xsl:template match="dri:field[@type='text']" mode="dados-item" priority="10">
<!--        <input id="caixa-busca">
            <xsl:attribute name="name">
                <xsl:value-of select="@n"/>
            </xsl:attribute>
            <xsl:attribute name="type">
                <xsl:value-of select="@type"/>
            </xsl:attribute>
            <xsl:attribute name="value">
                <xsl:value-of select="@returnValue"/>
            </xsl:attribute>
        </input>
        <xsl:apply-templates />
        -->
        <input>
            <xsl:call-template name="fieldAttributes"/>
            <xsl:attribute name="style">
                <xsl:text>display:none;</xsl:text>
            </xsl:attribute>
            <xsl:if test="@type='button'">
                <xsl:attribute name="type">submit</xsl:attribute>
            </xsl:if>
            <xsl:attribute name="value">
                <xsl:choose>
                    <xsl:when test="./dri:value[@type='raw']">
                        <xsl:value-of select="./dri:value[@type='raw']"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="./dri:value[@type='default']"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:if test="dri:value/i18n:text">
                <xsl:attribute name="i18n:attr">value</xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </input>

    </xsl:template>

    <!-- A special case for the value element under field of type 'select'. Instead of being used to create
        the value attribute of an HTML input tag, these are used to create selection options.
    <xsl:template match="dri:field[@type='select']/dri:value" priority="2">
        <option>
            <xsl:attribute name="value"><xsl:value-of select="@optionValue"/></xsl:attribute>
            <xsl:if test="@optionSelected = 'yes'">
                <xsl:attribute name="selected">selected</xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </option>
    </xsl:template>-->
    
    <!-- In general cases the value of this element is used directly, so the template does nothing. -->
    <xsl:template match="dri:value" priority="1">
    </xsl:template>
    
    <!-- The field label is usually invoked directly by a higher level tag, so this template does nothing. -->
    <xsl:template match="dri:field/dri:label" priority="2">
    </xsl:template>
    
    <xsl:template match="dri:field/dri:label" mode="compositeComponent">
        <xsl:apply-templates />
    </xsl:template>
    
    <!-- The error field handling -->
    <xsl:template match="dri:error">
        <xsl:attribute name="title">
            <xsl:value-of select="."/>
        </xsl:attribute>
        <xsl:if test="i18n:text">
            <xsl:attribute name="i18n:attr">title</xsl:attribute>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="dri:error" mode="error">
        <span class="error">*
            <xsl:apply-templates/>
        </span>
    </xsl:template>
    
    
    
    <!-- Help elementns are turning into tooltips. There might be a better way tot do this -->
    <xsl:template match="dri:help">
        <xsl:attribute name="title">
            <xsl:value-of select="."/>
        </xsl:attribute>
        <xsl:if test="i18n:text">
            <xsl:attribute name="i18n:attr">title</xsl:attribute>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="dri:help" mode="help">
        <!--Only create the <span> if there is content in the <dri:help> node-->
        <xsl:if test="./text() or ./node()">
            <span class="field-help">
                <xsl:apply-templates />
            </span>
        </xsl:if>
    </xsl:template>
    
    
    
    <!-- The last thing in the structural elements section are the templates to cover the attribute calls.
        Although, by default, XSL only parses elements and text, an explicit call to apply the attributes
        of children tags can still be made. This, in turn, requires templates that handle specific attributes,
        like the kind you see below. The chief amongst them is the pagination attribute contained by divs,
        which creates a new div element to display pagination information. -->
    
    <xsl:template match="@pagination">
        <xsl:param name="position"/>
        <xsl:choose>
            <xsl:when test=". = 'simple'">
                <div class="pagination {$position}">
                    <xsl:if test="parent::node()/@previousPage">
                        <a class="previous-page-link">
                            <xsl:attribute name="href">
                                <xsl:value-of select="parent::node()/@previousPage"/>
                            </xsl:attribute>
                            <i18n:text>xmlui.dri2xhtml.structural.pagination-previous</i18n:text>
                        </a>
                    </xsl:if>
                    <p class="pagination-info">
                        <i18n:translate>
                            <i18n:text>xmlui.dri2xhtml.structural.pagination-info</i18n:text>
                            <i18n:param>
                                <xsl:value-of select="parent::node()/@firstItemIndex"/>
                            </i18n:param>
                            <i18n:param>
                                <xsl:value-of select="parent::node()/@lastItemIndex"/>
                            </i18n:param>
                            <i18n:param>
                                <xsl:value-of select="parent::node()/@itemsTotal"/>
                            </i18n:param>
                        </i18n:translate>
                        <!--
                        <xsl:text>Now showing items </xsl:text>
                        <xsl:value-of select="parent::node()/@firstItemIndex"/>
                        <xsl:text>-</xsl:text>
                        <xsl:value-of select="parent::node()/@lastItemIndex"/>
                        <xsl:text> of </xsl:text>
                        <xsl:value-of select="parent::node()/@itemsTotal"/>
                            -->
                    </p>
                    <xsl:if test="parent::node()/@nextPage">
                        <a class="next-page-link">
                            <xsl:attribute name="href">
                                <xsl:value-of select="parent::node()/@nextPage"/>
                            </xsl:attribute>
                            <i18n:text>xmlui.dri2xhtml.structural.pagination-next</i18n:text>
                        </a>
                    </xsl:if>
                </div>
            </xsl:when>
            <xsl:when test=". = 'masked'">
                <div class="numero-resultados {$position}">
                    <xsl:if test="not(parent::node()/@firstItemIndex = 0 or parent::node()/@firstItemIndex = 1)">
                        <a class="previous-page-link">
                            <xsl:attribute name="href">
                                <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                <xsl:value-of select="parent::node()/@currentPage - 1"/>
                                <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                            </xsl:attribute>
                            <i18n:text>xmlui.dri2xhtml.structural.pagination-previous</i18n:text>
<!--                            <img>
                                <xsl:attribute name="src">
                                    <xsl:value-of select="$images-path"/>
                                    <xsl:text>esquerda.png</xsl:text>
                                </xsl:attribute>
                            </img>-->
                        </a>
                    </xsl:if>
<!--                    <p class="pagination-info">
                        <i18n:translate>
                            <i18n:text>xmlui.dri2xhtml.structural.pagination-info</i18n:text>
                            <i18n:param>
                                <xsl:value-of select="parent::node()/@firstItemIndex"/>
                            </i18n:param>
                            <i18n:param>
                                <xsl:value-of select="parent::node()/@lastItemIndex"/>
                            </i18n:param>
                            <i18n:param>
                                <xsl:value-of select="parent::node()/@itemsTotal"/>
                            </i18n:param>
                        </i18n:translate>
                    </p>-->
                    <ul class="links-paginacao">
                        <xsl:if test="(parent::node()/@currentPage - 4) &gt; 0">
                            <li class="first-page-link">
                                <a>
                                    <xsl:attribute name="href">
                                        <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                        <xsl:text>1</xsl:text>
                                        <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                                    </xsl:attribute>
                                    <xsl:text>1</xsl:text>
                                </a>
                                <xsl:text> . . . </xsl:text>
                            </li>
                        </xsl:if>
                        <xsl:call-template name="offset-link">
                            <xsl:with-param name="pageOffset">-3</xsl:with-param>
                        </xsl:call-template>
                        <xsl:call-template name="offset-link">
                            <xsl:with-param name="pageOffset">-2</xsl:with-param>
                        </xsl:call-template>
                        <xsl:call-template name="offset-link">
                            <xsl:with-param name="pageOffset">-1</xsl:with-param>
                        </xsl:call-template>
                        <xsl:call-template name="offset-link">
                            <xsl:with-param name="pageOffset">0</xsl:with-param>
                        </xsl:call-template>
                        <xsl:call-template name="offset-link">
                            <xsl:with-param name="pageOffset">1</xsl:with-param>
                        </xsl:call-template>
                        <xsl:call-template name="offset-link">
                            <xsl:with-param name="pageOffset">2</xsl:with-param>
                        </xsl:call-template>
                        <xsl:call-template name="offset-link">
                            <xsl:with-param name="pageOffset">3</xsl:with-param>
                        </xsl:call-template>
                        <xsl:if test="(parent::node()/@currentPage + 4) &lt;= (parent::node()/@pagesTotal)">
                            <li class="last-page-link">
                                <xsl:text> . . . </xsl:text>
                                <a>
                                    <xsl:attribute name="href">
                                        <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                        <xsl:value-of select="parent::node()/@pagesTotal"/>
                                        <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                                    </xsl:attribute>
                                    <xsl:value-of select="parent::node()/@pagesTotal"/>
                                </a>
                            </li>
                        </xsl:if>
                    </ul>
                    <xsl:if test="not(parent::node()/@lastItemIndex = parent::node()/@itemsTotal)">
                        <a class="next-page-link">
                            <xsl:attribute name="href">
                                <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                <xsl:value-of select="parent::node()/@currentPage + 1"/>
                                <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                            </xsl:attribute>
                            <i18n:text>xmlui.dri2xhtml.structural.pagination-next</i18n:text>
<!--                            <img>
                                <xsl:attribute name="src">
                                    <xsl:value-of select="$images-path"/>
                                    <xsl:text>direita.png</xsl:text>
                                </xsl:attribute>
                            </img>-->
                        </a>
                    </xsl:if>
                </div>
            </xsl:when>
        </xsl:choose>
    </xsl:template>
    
    <!-- A quick helper function used by the @pagination template for repetitive tasks -->
    <xsl:template name="offset-link">
        <xsl:param name="pageOffset"/>
        <xsl:if test="((parent::node()/@currentPage + $pageOffset) &gt; 0) and
            ((parent::node()/@currentPage + $pageOffset) &lt;= (parent::node()/@pagesTotal))">
            <li class="page-link">
                <xsl:if test="$pageOffset = 0">
                    <xsl:attribute name="class">current-page-link</xsl:attribute>
                </xsl:if>
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                        <xsl:value-of select="parent::node()/@currentPage + $pageOffset"/>
                        <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                    </xsl:attribute>
                    <xsl:value-of select="parent::node()/@currentPage + $pageOffset"/>
                </a>
            </li>
        </xsl:if>
    </xsl:template>
    
    
    <!-- checkbox and radio fields type uses this attribute -->
    <xsl:template match="@returnValue">
        <xsl:attribute name="value">
            <xsl:value-of select="."/>
        </xsl:attribute>
    </xsl:template>
    
    <!-- used for image buttons -->
    <xsl:template match="@source">
        <xsl:attribute name="src">
            <xsl:value-of select="."/>
        </xsl:attribute>
    </xsl:template>
    
    <!-- size and maxlength used by text, password, and textarea inputs -->
    <xsl:template match="@size">
        <xsl:attribute name="size">
            <xsl:value-of select="."/>
        </xsl:attribute>
    </xsl:template>
    
    <xsl:template match="@maxlength">
        <xsl:attribute name="maxlength">
            <xsl:value-of select="."/>
        </xsl:attribute>
    </xsl:template>
    
    <!-- "multiple" attribute is used by the <select> input method -->
    <xsl:template match="@multiple[.='yes']">
        <xsl:attribute name="multiple">multiple</xsl:attribute>
    </xsl:template>
    
    <!-- rows and cols attributes are used by textarea input -->
    <xsl:template match="@rows">
        <xsl:attribute name="rows">
            <xsl:value-of select="."/>
        </xsl:attribute>
    </xsl:template>
    
    <xsl:template match="@cols">
        <xsl:attribute name="cols">
            <xsl:value-of select="."/>
        </xsl:attribute>
    </xsl:template>
    
    <!-- The general "catch-all" template for attributes matched, but not handled above -->
    <xsl:template match="@*"></xsl:template>
    
    
    
    
    
    
    
<!-- This is the end of the structural elements section. From here to the end of the document come
    templates devoted to handling the referenceSet and reference elements. Although they are considered
    structural elements, neither of the two contains actual content. Instead, references contain references
    to object metadata under objectMeta, while referenceSets group references together.
-->
    
          
    <!-- Starting off easy here, with a summaryList -->
    
    <!-- Current issues:
        
        1. There is no check for the repository identifier. Need to fix that by concatenating it with the
            object identifier and using the resulting string as the key on items and reps.
        2. The use of a key index across the object store is cryptic and counterintuitive and most likely
            could benefit from better documentation.
    -->
    
    <!-- When you come to an referenceSet you have to make a decision. Since it contains objects, and each
        object is its own entity (and handled in its own template) the decision of the overall structure
        would logically (and traditionally) lie with this template. However, to accomplish this we would
        have to look ahead and check what objects are included in the set, which involves resolving the
        references ahead of time and getting the information from their METS profiles directly.
    
        Since this approach creates strong coupling between the set and the objects it contains, and we
        have tried to avoid that, we use the "pioneer" method. -->
    
    <!-- Summarylist case. This template used to apply templates to the "pioneer" object (the first object
        in the set) and let it figure out what to do. This is no longer the case, as everything has been
        moved to the list model. A special theme, called TableTheme, has beeen created for the purpose of
        preserving the pioneer model. -->
<!--CHANGED-->
    <xsl:template match="dri:referenceSet[@type = 'summaryList'][dri:reference[not(@type='DSpace Item')]]" priority="3">
        <!--NEVER SHOW RESULTS FOR COMMUNITIES AND COLLECTIONS NAMES.-->
    </xsl:template>

    <xsl:template match="dri:referenceSet[@type = 'summaryList' and dri:reference/@type='DSpace Item']" priority="2">
<!--        <xsl:apply-templates select="dri:head"/>-->
        <div id="resultados" class="borda">
            <xsl:choose>
<!--            <xsl:when test="//dri:div[@n='general-query']/dri:table[@n='search-controls']//dri:field[@n='view']">-->
                <xsl:when test="//dri:div[@id='aspect.discovery.SimpleSearch.div.search-results']/@rend = 'list'">
                <!--INICIO LISTA RESULTADOS-->
                    <ul id="lista-resultados">
                        <xsl:apply-templates select="*[not(name()='head')]" mode="summaryList"/>
                    </ul>
                <!--FIM LISTA RESULTADOS-->
                </xsl:when>
                <xsl:when test="//dri:div[@id='aspect.discovery.SimpleSearch.div.search-results']/@rend = 'grid'">
                <!--INICIO GRADE RESULTADOS-->
                    <table id="grade-resultados">
        <!--                <xsl:apply-templates select="*[not(name()='head')]" mode="summaryGrid"/>-->
                        <xsl:variable name="total-items" select="count(*[not(name()='head')])"/>
                        <xsl:variable name="offset">
                            <xsl:choose>
                                <xsl:when test="//dri:div[@id='aspect.discovery.SimpleSearch.div.search-results']">
                                    <xsl:value-of select="//dri:div[@id='aspect.discovery.SimpleSearch.div.search-results']/@firstItemIndex - 1"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="0"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:variable>

                        <xsl:for-each select="(*[not(name()='head')])">
                            <xsl:if test="position() mod 3 = 1">
                                <xsl:text disable-output-escaping="yes">&lt;tr&gt;</xsl:text>
                            </xsl:if>
                            <xsl:apply-templates select="." mode="summaryGrid">
                                <xsl:with-param name="position" select="position() + $offset"/>
                            </xsl:apply-templates>
                            <xsl:if test="position() mod 3 = 0 or position() = $total-items">
                                <xsl:text disable-output-escaping="yes">&lt;/tr&gt;</xsl:text>
                            </xsl:if>
                        </xsl:for-each>
                    </table>
            <!--FIM GRADE RESULTADOS-->
                </xsl:when>
                <xsl:otherwise>
                <!--INICIO LISTA RESULTADOS-->
                    <ul id="lista-resultados">
                        <xsl:apply-templates select="*[not(name()='head')]" mode="summaryList"/>
                    </ul>
                <!--FIM LISTA RESULTADOS-->
                </xsl:otherwise>
            </xsl:choose>
        </div>
    </xsl:template>



    <!-- First, the detail list case -->
    <xsl:template match="dri:referenceSet[@type = 'detailList']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <ul class="ds-referenceSet-list">
            <xsl:apply-templates select="*[not(name()='head')]" mode="detailList"/>
        </ul>
    </xsl:template>
    
    
    <!-- Next up is the summary view case that at this point applies only to items, since communities and
        collections do not have two separate views. -->
    <xsl:template match="dri:referenceSet[@type = 'summaryView']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <xsl:apply-templates select="*[not(name()='head')]" mode="summaryView"/>
    </xsl:template>
            
    <!-- Finally, we have the detailed view case that is applicable to items, communities and collections.
        In DRI it constitutes a standard view of collections/communities and a complete metadata listing
        view of items. -->
    <xsl:template match="dri:referenceSet[@type = 'detailView']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <xsl:apply-templates select="*[not(name()='head')]" mode="detailView"/>
    </xsl:template>
    
    
    
    
    
    <!-- The following options can be appended to the external metadata URL to request specific
        sections of the METS document:
        
        sections:
        
        A comma seperated list of METS sections to included. The possible values are: "metsHdr", "dmdSec",
        "amdSec", "fileSec", "structMap", "structLink", "behaviorSec", and "extraSec". If no list is provided then *ALL*
        sections are rendered.
        
        
        dmdTypes:
        
        A comma seperated list of metadata formats to provide as descriptive metadata. The list of avaialable metadata
        types is defined in the dspace.cfg, disseminationcrosswalks. If no formats are provided them DIM - DSpace
        Intermediate Format - is used.
        
        
        amdTypes:
        
        A comma seperated list of metadata formats to provide administative metadata. DSpace does not currently
        support this type of metadata.
        
        
        fileGrpTypes:
        
        A comma seperated list of file groups to render. For DSpace a bundle is translated into a METS fileGrp, so
        possible values are "THUMBNAIL","CONTENT", "METADATA", etc... If no list is provided then all groups are
        rendered.
        
        
        structTypes:
        
        A comma seperated list of structure types to render. For DSpace there is only one structType: LOGICAL. If this
        is provided then the logical structType will be rendered, otherwise none will. The default operation is to
        render all structure types.
    -->
    
    <!-- Then we resolve the reference tag to an external mets object -->
<!--    CHANGED-->
    <xsl:template match="dri:reference" mode="summaryList">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- Since this is a summary only grab the descriptive metadata, and the thumbnails -->
            <xsl:text>?sections=dmdSec,fileSec,structMap</xsl:text><!--&amp;fileGrpTypes=THUMBNAIL-->
            <!-- An example of requesting a specific metadata standard (MODS and QDC crosswalks only work for items)->
            <xsl:if test="@type='DSpace Item'">
                <xsl:text>&amp;dmdTypes=DC</xsl:text>
            </xsl:if>-->
        </xsl:variable>
        <xsl:comment> External Metadata URL:
            <xsl:value-of select="$externalMetadataURL"/>
            summaryList
        </xsl:comment>

        <xsl:variable name="offset">
            <xsl:choose>
                <xsl:when test="//dri:div[@id='aspect.discovery.SimpleSearch.div.search-results']">
                    <xsl:value-of select="//dri:div[@id='aspect.discovery.SimpleSearch.div.search-results']/@firstItemIndex - 1"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="0"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <li>
            <xsl:apply-templates select="document($externalMetadataURL)/mets:METS" mode="summaryList">
                <xsl:with-param name="position" select="position() + $offset"/>
            </xsl:apply-templates>
        </li>
        <!-- O "LI" SE REPETE PARA CADA RESULTADO-->
                
        <!-- Apply to next referenceSet and reference. -->
        <xsl:apply-templates />
    </xsl:template>


    <xsl:template match="dri:reference" mode="summaryGrid">
        <xsl:param name="position"/>
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- Since this is a summary only grab the descriptive metadata, and the thumbnails -->
            <xsl:text>?sections=dmdSec,fileSec,structMap</xsl:text><!--&amp;fileGrpTypes=THUMBNAIL-->
            <!-- An example of requesting a specific metadata standard (MODS and QDC crosswalks only work for items)->
            <xsl:if test="@type='DSpace Item'">
                <xsl:text>&amp;dmdTypes=DC</xsl:text>
            </xsl:if>-->
        </xsl:variable>
        <xsl:comment> External Metadata URL:
            <xsl:value-of select="$externalMetadataURL"/>
            summaryGrid
        </xsl:comment>

        <!-- O "TR" SE REPETE PARA CADA LINHA NA GRANDE DE RESULTADOS-->
        <!-- O "TD" SE REPETE PARA CADA RESULTADO-->
        <td class="resultado">
            <xsl:apply-templates select="document($externalMetadataURL)/mets:METS" mode="summaryGrid">
                <xsl:with-param name="position" select="$position"/>
            </xsl:apply-templates>
        </td>

        <!-- Apply to next referenceSet and reference. -->
        <xsl:apply-templates />
    </xsl:template>


    <xsl:template match="dri:reference" mode="detailList">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- No options selected, render the full METS document -->
        </xsl:variable>
        <xsl:comment> External Metadata URL:
            <xsl:value-of select="$externalMetadataURL"/>
            detailList
        </xsl:comment>
        <li>
            <xsl:apply-templates select="document($externalMetadataURL)" mode="detailList"/>
            <xsl:apply-templates />
        </li>
    </xsl:template>
    
    <xsl:template match="dri:reference" mode="summaryView">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- No options selected, render the full METS document -->
        </xsl:variable>
        <xsl:comment> External Metadata URL:
            <xsl:value-of select="$externalMetadataURL"/>
            summaryView
        </xsl:comment>
        <xsl:apply-templates select="document($externalMetadataURL)" mode="summaryView"/>
        <xsl:apply-templates />
    </xsl:template>

    
    <xsl:template match="dri:reference" mode="detailView">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- No options selected, render the full METS document -->
        </xsl:variable>
        <xsl:comment> External Metadata URL:
            <xsl:value-of select="$externalMetadataURL"/>
            detailView
        </xsl:comment>
        <xsl:choose>
            <xsl:when test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='viewer'][@qualifier='type'] = 'image'">
                <xsl:apply-templates select="document($externalMetadataURL)" mode="detailViewImage"/>
            </xsl:when>
            <xsl:when test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='viewer'][@qualifier='type'] = 'book'">
                <xsl:apply-templates select="document($externalMetadataURL)" mode="detailViewBook"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="document($externalMetadataURL)" mode="detailView"/>
            </xsl:otherwise>
        </xsl:choose>
<!--        <xsl:apply-templates />-->
    </xsl:template>
    

    <xsl:template match="dri:reference" mode="headDetailView">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- No options selected, render the full METS document -->
        </xsl:variable>
        <xsl:comment> External Metadata URL:
            <xsl:value-of select="$externalMetadataURL"/>
            headDetailView
        </xsl:comment>

        <xsl:choose>
            <xsl:when test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='viewer'][@qualifier='type'] = 'image'">
                <xsl:apply-templates select="document($externalMetadataURL)" mode="headDetailViewImage"/>
            </xsl:when>
            <xsl:when test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='viewer'][@qualifier='type'] = 'book'">
                <xsl:apply-templates select="document($externalMetadataURL)" mode="headDetailViewBook"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="document($externalMetadataURL)" mode="headDetailView"/>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>

    
    
    
    <!-- The standard attributes template -->
    <!-- TODO: should probably be moved up some, since it is commonly called -->
    <xsl:template name="standardAttributes">
        <xsl:param name="class"/>
        <xsl:if test="@id">
            <xsl:attribute name="id">
                <xsl:value-of select="translate(@id,'.','_')"/>
            </xsl:attribute>
        </xsl:if>
        <xsl:attribute name="class">
            <xsl:value-of select="normalize-space($class)"/>
            <xsl:if test="@rend">
                <xsl:text> </xsl:text>
                <xsl:value-of select="@rend"/>
            </xsl:if>
        </xsl:attribute>
        
    </xsl:template>
    
    <!-- templates for required textarea attributes used if not found in DRI document -->
    <xsl:template name="textAreaCols">
        <xsl:attribute name="cols">20</xsl:attribute>
    </xsl:template>
    
    <xsl:template name="textAreaRows">
        <xsl:attribute name="rows">5</xsl:attribute>
    </xsl:template>
    
    
    
    <!-- This does it for all the DRI elements. The only thing left to do is to handle Cocoon's i18n
        transformer tags that are used for text translation. The templates below simply push through
        the i18n elements so that they can translated after the XSL step. -->
    <xsl:template match="i18n:text">
        <xsl:copy-of select="."/>
    </xsl:template>
    
    <xsl:template match="i18n:translate">
        <xsl:copy-of select="."/>
    </xsl:template>
    
    <xsl:template match="i18n:param">
        <xsl:copy-of select="."/>
    </xsl:template>
    
    <!-- =============================================================== -->
    <!-- - - - - - New templates for Choice/Authority control - - - - -  -->
    
    <!-- choose 'hidden' for invisible auth, 'text' lets CSS control it. -->
    <xsl:variable name="authorityInputType" select="'text'"/>
    
    <!-- add button to invoke Choices lookup popup.. assume
      -  that the context is a dri:field, where dri:params/@choices is true.
     -->
    <xsl:template name="addLookupButton">
        <xsl:param name="isName" select="'missing value'"/>
      <!-- optional param if you want to send authority value to diff field -->
        <xsl:param name="authorityInput" select="concat(@n,'_authority')"/>
      <!-- optional param for confidence indicator ID -->
        <xsl:param name="confIndicator" select="''"/>
        <input type="button" name="{concat('lookup_',@n)}" class="ds-button-field ds-add-button" >
            <xsl:attribute name="value">
                <xsl:text>Lookup</xsl:text>
                <xsl:if test="contains(dri:params/@operations,'add')">
                    <xsl:text> &amp; Add</xsl:text>
                </xsl:if>
            </xsl:attribute>
            <xsl:attribute name="onClick">
                <xsl:text>javascript:DSpaceChoiceLookup('</xsl:text>
          <!-- URL -->
                <xsl:value-of select="concat($context-path,'/admin/lookup')"/>
                <xsl:text>', '</xsl:text>
          <!-- field -->
                <xsl:value-of select="dri:params/@choices"/>
                <xsl:text>', '</xsl:text>
          <!-- formID -->
                <xsl:value-of select="translate(ancestor::dri:div[@interactive='yes']/@id,'.','_')"/>
                <xsl:text>', '</xsl:text>
          <!-- valueInput -->
                <xsl:value-of select="@n"/>
                <xsl:text>', '</xsl:text>
          <!-- authorityInput, name of field to get authority -->
                <xsl:value-of select="$authorityInput"/>
                <xsl:text>', '</xsl:text>
          <!-- Confidence Indicator's ID so lookup can frob it -->
                <xsl:value-of select="$confIndicator"/>
                <xsl:text>', </xsl:text>
          <!-- Collection ID for context -->
                <xsl:choose>
                    <xsl:when test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='choice'][@qualifier='collection']">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='choice'][@qualifier='collection']"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>-1</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
                <xsl:text>, </xsl:text>
          <!-- isName -->
                <xsl:value-of select="$isName"/>
                <xsl:text>, </xsl:text>
          <!-- isRepating -->
                <xsl:value-of select="boolean(contains(dri:params/@operations,'add'))"/>
                <xsl:text>);</xsl:text>
            </xsl:attribute>
        </input>
    </xsl:template>

    <!-- Fragment to display an authority confidence icon.
       -  Insert an invisible 1x1 image which gets "covered" by background
       -  image as dictated by the CSS, so icons are easily adjusted in CSS.
       -  "confidence" param is confidence _value_, i.e. symbolic name
      -->
    <xsl:template name="authorityConfidenceIcon">
      <!-- default confidence value won't show any image. -->
        <xsl:param name="confidence" select="'blank'"/>
        <xsl:param name="id" select="''"/>
        <xsl:variable name="lcConfidence" select="translate($confidence,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')"/>
        <img i18n:attr="title">
            <xsl:if test="string-length($id) > 0">
                <xsl:attribute name="id">
                    <xsl:value-of select="$id"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:attribute name="src">
                <xsl:value-of select="concat($theme-path,'/images/invisible.gif')"/>
            </xsl:attribute>
            <xsl:attribute name="class">
                <xsl:text>ds-authority-confidence </xsl:text>
                <xsl:choose>
                    <xsl:when test="string-length($lcConfidence) > 0">
                        <xsl:value-of select="concat('cf-',$lcConfidence,' ')"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>cf-blank </xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:attribute name="title">
                <xsl:text>xmlui.authority.confidence.description.cf_</xsl:text>
                <xsl:value-of select="$lcConfidence"/>
            </xsl:attribute>
        </img>
    </xsl:template>

    <!-- Fragment to include an authority confidence hidden input
       - assumes @n is the name of the field.
       -  param is confidence _value_, i.e. integer 0-6
      -->
    <xsl:template name="authorityConfidenceInput">
        <xsl:param name="confidence"/>
        <xsl:param name="name"/>
        <input class="ds-authority-confidence-input" type="hidden">
            <xsl:attribute name="name">
                <xsl:value-of select="$name"/>
            </xsl:attribute>
            <xsl:attribute name="value">
                <xsl:value-of select="$confidence"/>
            </xsl:attribute>
        </input>
    </xsl:template>


    <!-- insert fields needed by Scriptaculous autocomplete -->
    <xsl:template name="addAuthorityAutocompleteWidgets">
      <!-- "spinner" indicator to signal "loading", managed by autocompleter -->
      <!--  put it next to input field -->
        <span style="display:none;">
            <xsl:attribute name="id">
                <xsl:value-of select="concat(translate(@id,'.','_'),'_indicator')"/>
            </xsl:attribute>
            <img alt="Loading...">
                <xsl:attribute name="src">
                    <xsl:value-of select="concat($theme-path,'/images/suggest-indicator.gif')"/>
                </xsl:attribute>
            </img>
        </span>
      <!-- This is the anchor for autocomplete popup, div id="..._container" -->
      <!--  put it below input field, give ID to autocomplete below -->
        <div class="autocomplete">
            <xsl:attribute name="id">
                <xsl:value-of select="concat(translate(@id,'.','_'),'_container')"/>
            </xsl:attribute>
            <xsl:text> </xsl:text>
        </div>
    </xsl:template>

    <!-- adds autocomplete fields and setup script to "normal" submit input -->
    <xsl:template name="addAuthorityAutocomplete">
        <xsl:param name="confidenceIndicatorID" select="''"/>
        <xsl:param name="confidenceName" select="''"/>
        <xsl:call-template name="addAuthorityAutocompleteWidgets"/>
        <xsl:call-template name="autocompleteSetup">
            <xsl:with-param name="formID"        select="translate(ancestor::dri:div[@interactive='yes']/@id,'.','_')"/>
            <xsl:with-param name="metadataField" select="@n"/>
            <xsl:with-param name="inputName"     select="@n"/>
            <xsl:with-param name="authorityName" select="concat(@n,'_authority')"/>
            <xsl:with-param name="containerID"   select="concat(translate(@id,'.','_'),'_container')"/>
            <xsl:with-param name="indicatorID"   select="concat(translate(@id,'.','_'),'_indicator')"/>
            <xsl:with-param name="isClosed"      select="contains(dri:params/@choicesClosed,'true')"/>
            <xsl:with-param name="confidenceIndicatorID" select="$confidenceIndicatorID"/>
            <xsl:with-param name="confidenceName" select="$confidenceName"/>
            <xsl:with-param name="collectionID">
                <xsl:choose>
                    <xsl:when test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='choice'][@qualifier='collection']">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='choice'][@qualifier='collection']"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>-1</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <!-- generate the script that sets up autocomplete feature on input field -->
    <!-- ..it has lots of params -->
    <xsl:template name="autocompleteSetup">
        <xsl:param name="formID" select="'missing value'"/>
        <xsl:param name="metadataField" select="'missing value'"/>
        <xsl:param name="inputName" select="'missing value'"/>
        <xsl:param name="authorityName" select="''"/>
        <xsl:param name="containerID" select="'missing value'"/>
        <xsl:param name="collectionID" select="'-1'"/>
        <xsl:param name="indicatorID" select="'missing value'"/>
        <xsl:param name="confidenceIndicatorID" select="''"/>
        <xsl:param name="confidenceName" select="''"/>
        <xsl:param name="isClosed" select="'false'"/>
        <script type="text/javascript">
            <xsl:text>var gigo = DSpaceSetupAutocomplete('</xsl:text>
            <xsl:value-of select="$formID"/>
            <xsl:text>', { metadataField: '</xsl:text>
            <xsl:value-of select="$metadataField"/>
            <xsl:text>', isClosed: '</xsl:text>
            <xsl:value-of select="$isClosed"/>
            <xsl:text>', inputName: '</xsl:text>
            <xsl:value-of select="$inputName"/>
            <xsl:text>', authorityName: '</xsl:text>
            <xsl:value-of select="$authorityName"/>
            <xsl:text>', containerID: '</xsl:text>
            <xsl:value-of select="$containerID"/>
            <xsl:text>', indicatorID: '</xsl:text>
            <xsl:value-of select="$indicatorID"/>
            <xsl:text>', confidenceIndicatorID: '</xsl:text>
            <xsl:value-of select="$confidenceIndicatorID"/>
            <xsl:text>', confidenceName: '</xsl:text>
            <xsl:value-of select="$confidenceName"/>
            <xsl:text>', collection: </xsl:text>
            <xsl:value-of select="$collectionID"/>
            <xsl:text>, contextPath: '</xsl:text>
            <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
            <xsl:text>'});</xsl:text>
        </script>
    </xsl:template>

    <!-- add the extra _authority{_n?} and _confidence input fields -->
    <xsl:template name="authorityInputFields">
        <xsl:param name="name" select="''"/>
        <xsl:param name="id" select="''"/>
        <xsl:param name="position" select="''"/>
        <xsl:param name="authValue" select="''"/>
        <xsl:param name="confValue" select="''"/>
        <xsl:param name="confIndicatorID" select="''"/>
        <xsl:param name="unlockButton" select="''"/>
        <xsl:param name="unlockHelp" select="''"/>
        <xsl:variable name="authFieldID" select="concat(translate(@id,'.','_'),'_authority')"/>
        <xsl:variable name="confFieldID" select="concat(translate(@id,'.','_'),'_confidence')"/>
      <!-- the authority key value -->
        <input>
            <xsl:attribute name="class">
                <xsl:text>ds-authority-value </xsl:text>
                <xsl:if test="$unlockButton">
                    <xsl:text>ds-authority-visible </xsl:text>
                </xsl:if>
            </xsl:attribute>
            <xsl:attribute name="type">
                <xsl:value-of select="$authorityInputType"/>
            </xsl:attribute>
            <xsl:attribute name="readonly">
                <xsl:text>readonly</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="name">
                <xsl:value-of select="concat($name,'_authority')"/>
                <xsl:if test="$position">
                    <xsl:value-of select="concat('_', $position)"/>
                </xsl:if>
            </xsl:attribute>
            <xsl:if test="$id">
                <xsl:attribute name="id">
                    <xsl:value-of select="$authFieldID"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:attribute name="value">
                <xsl:value-of select="$authValue"/>
            </xsl:attribute>
        <!-- this updates confidence after a manual change to authority value -->
            <xsl:attribute name="onChange">
                <xsl:text>javascript: return DSpaceAuthorityOnChange(this, '</xsl:text>
                <xsl:value-of select="$confFieldID"/>
                <xsl:text>','</xsl:text>
                <xsl:value-of select="$confIndicatorID"/>
                <xsl:text>');</xsl:text>
            </xsl:attribute>
        </input>
      <!-- optional "unlock" button on (visible) authority value field -->
        <xsl:if test="$unlockButton">
            <input type="image" class="ds-authority-lock is-locked ">
                <xsl:attribute name="onClick">
                    <xsl:text>javascript: return DSpaceToggleAuthorityLock(this, '</xsl:text>
                    <xsl:value-of select="$authFieldID"/>
                    <xsl:text>');</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="src">
                    <xsl:value-of select="concat($theme-path,'/images/invisible.gif')"/>
                </xsl:attribute>
                <xsl:attribute name="i18n:attr">title</xsl:attribute>
                <xsl:attribute name="title">
                    <xsl:value-of select="$unlockHelp"/>
                </xsl:attribute>
            </input>
        </xsl:if>
        <input class="ds-authority-confidence-input" type="hidden">
            <xsl:attribute name="name">
                <xsl:value-of select="concat($name,'_confidence')"/>
                <xsl:if test="$position">
                    <xsl:value-of select="concat('_', $position)"/>
                </xsl:if>
            </xsl:attribute>
            <xsl:if test="$id">
                <xsl:attribute name="id">
                    <xsl:value-of select="$confFieldID"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:attribute name="value">
                <xsl:value-of select="$confValue"/>
            </xsl:attribute>
        </input>
    </xsl:template>
    
    <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  -->
    <!-- Special Transformations for Choice Authority lookup popup page -->

    <!-- indicator spinner -->
    <xsl:template match="dri:item[@id='aspect.general.ChoiceLookupTransformer.item.select']/dri:figure">
        <img id="lookup_indicator_id" alt="Loading..." style="display:none;">
            <xsl:attribute name="src">
                <xsl:value-of select="concat($theme-path,'/images/lookup-indicator.gif')"/>
            </xsl:attribute>
        </img>
    </xsl:template>
    
    <!-- This inline JS must be added to the popup page for choice lookups -->
    <xsl:template name="choiceLookupPopUpSetup">
        <script type="text/javascript">
        var form = document.getElementById('aspect_general_ChoiceLookupTransformer_div_lookup');
        DSpaceChoicesSetup(form);
        </script>
    </xsl:template>

    <!-- Special select widget for lookup popup -->
    <xsl:template match="dri:field[@id='aspect.general.ChoiceLookupTransformer.field.chooser']">
        <div>
            <select onChange="javascript:DSpaceChoicesSelectOnChange();">
                <xsl:call-template name="fieldAttributes"/>
                <xsl:apply-templates/>
                <xsl:comment>space filler because "unclosed" select annoys browsers</xsl:comment>
            </select>
            <img class="choices-lookup" id="lookup_indicator_id" alt="Loading..." style="display:none;">
                <xsl:attribute name="src">
                    <xsl:value-of select="concat($theme-path,'/images/lookup-indicator.gif')"/>
                </xsl:attribute>
            </img>
        </div>
    </xsl:template>

    <!-- Generate buttons with onClick attribute, since it is the easiest
       - way to set a single event handler in a browser-independent manner.
      -->

    <!-- choice popup "accept" button -->
    <xsl:template match="dri:field[@id='aspect.general.ChoiceLookupTransformer.field.accept']">
        <xsl:call-template name="choiceLookupButton">
            <xsl:with-param name="onClick" select="'javascript:DSpaceChoicesAcceptOnClick();'"/>
        </xsl:call-template>
    </xsl:template>

    <!-- choice popup "more" button -->
    <xsl:template match="dri:field[@id='aspect.general.ChoiceLookupTransformer.field.more']">
        <xsl:call-template name="choiceLookupButton">
            <xsl:with-param name="onClick" select="'javascript:DSpaceChoicesMoreOnClick();'"/>
        </xsl:call-template>
    </xsl:template>

    <!-- choice popup "cancel" button -->
    <xsl:template match="dri:field[@id='aspect.general.ChoiceLookupTransformer.field.cancel']">
        <xsl:call-template name="choiceLookupButton">
            <xsl:with-param name="onClick" select="'javascript:DSpaceChoicesCancelOnClick();'"/>
        </xsl:call-template>
    </xsl:template>

    <!-- button markup: special handling needed because these must not be <input type=submit> -->
    <xsl:template name="choiceLookupButton">
        <xsl:param name="onClick"/>
        <input type="button" onClick="{$onClick}">
            <xsl:call-template name="fieldAttributes"/>
            <xsl:attribute name="value">
                <xsl:choose>
                    <xsl:when test="./dri:value[@type='raw']">
                        <xsl:value-of select="./dri:value[@type='raw']"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="./dri:value[@type='default']"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:if test="dri:value/i18n:text">
                <xsl:attribute name="i18n:attr">value</xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </input>
    </xsl:template>

    <!-- - - - - - End templates for Choice/Authority control - - - - -  -->
    <!-- =============================================================== -->


    <!-- - - - - - template for harvesting - - - - -  -->
    <xsl:template match="dri:field[@id='aspect.administrative.collection.SetupCollectionHarvestingForm.field.oai-set-comp' and @type='composite']" mode="formComposite" priority="2">
        <xsl:for-each select="dri:field[@type='radio']">
            <div class="ds-form-content">
                <xsl:for-each select="dri:option">
                    <input type="radio">
                        <xsl:attribute name="id">
                            <xsl:value-of select="@returnValue"/>
                        </xsl:attribute>
                        <xsl:attribute name="name">
                            <xsl:value-of select="../@n"/>
                        </xsl:attribute>
                        <xsl:attribute name="value">
                            <xsl:value-of select="@returnValue"/>
                        </xsl:attribute>
                        <xsl:if test="../dri:value[@type='option'][@option = current()/@returnValue]">
                            <xsl:attribute name="checked">checked</xsl:attribute>
                        </xsl:if>
                    </input>
                    <label>
                        <xsl:attribute name="for">
                            <xsl:value-of select="@returnValue"/>
                        </xsl:attribute>
                        <xsl:value-of select="text()"/>
                    </label>
                    <xsl:if test="@returnValue = 'specific'">
                        <xsl:apply-templates select="../../dri:field[@n='oai_setid']"/>
                    </xsl:if>
                    <br/>
                </xsl:for-each>
            </div>
        </xsl:for-each>
    </xsl:template>




</xsl:stylesheet>

