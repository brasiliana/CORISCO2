<?xml version="1.0" encoding="UTF-8"?>

<!--
  DS-METS-1.0-DIM.xsl
-->

<!--
    TODO: Describe this XSL file    
-->    

<xsl:stylesheet 
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:mets="http://www.loc.gov/METS/"
    xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" 
    xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:atom="http://www.w3.org/2005/Atom"
    xmlns:ore="http://www.openarchives.org/ore/terms/"
    xmlns:oreatom="http://www.openarchives.org/ore/atom/"
    xmlns:fn="http://www.w3.org/2005/xpath-functions"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xalan="http://xml.apache.org/xalan" 
    xmlns:encoder="xalan://java.net.URLEncoder"
    exclude-result-prefixes="xalan encoder i18n dri mets dim  xlink xsl fn oreatom ore atom">
    <!--  
    the above should be replaced with if Saxon is going to be used.
    
     -->
    <xsl:output indent="yes"/>
    
       
    <!-- Some issues:
        - The named templates that are used to break up the monolithic top-level cases (like detailList, for
            example) could potentially conflict with named templates in other metadata handlers. So if, for
            example, I have a MODS and a DIM handler, they will match their respective object templates 
            correctly, since those check for the profile. However, if those templates then break the processing
            up between named templates, and those named templates happen to have the same name between the two
            handlers, a conflict will occur. You will have called a template that is expecting a different 
            profile, which will in turn lead to it not finding the metadata it is expecting. 
        
          The solution to this issue (which would be a pain to debug if it were to happen) is to make sure that
            if you do use named templates, you make their names unique. It would have been a clean and simple 
            solution to just place the name of the profile into the name template's mode, but alas XSL does not
            allow that. 
    -->
    
    
    
    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="summaryGrid">
        <xsl:param name="position"/>
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemSummaryGrid-DIM">
                    <xsl:with-param name="position" select="$position"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Collection'">
                <xsl:call-template name="collectionSummaryGrid-DIM">
                    <xsl:with-param name="position" select="$position"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Community'">
                <!--
                <xsl:call-template name="communitySummaryGrid-DIM"/>
                -->
                <xsl:call-template name="collectionSummaryGrid-DIM">
                    <xsl:with-param name="position" select="$position"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <!--
        The templates that handle the respective cases of summaryList: item, collection, and community
    -->

    <!-- An item rendered in the summaryList pattern. Commonly encountered in various browse-by pages
        and search results. -->
    <xsl:template name="itemSummaryGrid-DIM">
        <xsl:param name="position"/>
        <!-- Generate the thumbnail, if present, from the file section -->
        <div class="thumb-resultado">
            <xsl:apply-templates select="./mets:fileSec" mode="artifact-preview">
                <xsl:with-param name="primaryBitstream" select="./mets:structMap/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
            </xsl:apply-templates>
        </div>
<!--                
                    <a>
                        <img>
                            <xsl:attribute name="src">
                                <xsl:value-of select="$images-path"/>
                                <xsl:text>visualizador_pg1.gif</xsl:text>
                            </xsl:attribute>
                        </img>
                    </a>
                </div>-->
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim" mode="itemSummaryGrid-DIM">
            <xsl:with-param name="position" select="$position"/>
        </xsl:apply-templates>
    </xsl:template>
    

    <xsl:template match="dim:dim" mode="itemSummaryGrid-DIM">
        <xsl:param name="position"/>
        <xsl:variable name="itemWithdrawn" select="@withdrawn" />
        <div class="info-resultado">
            <p class="titulo-resultado">
                <span class="numero-resultado"><xsl:value-of select="$position"/>. </span>
<!--                    <a>título do resultado</a>-->
                <xsl:element name="a">
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="$itemWithdrawn">
                                <xsl:value-of select="ancestor::mets:METS/@OBJEDIT" />
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="ancestor::mets:METS/@OBJID" />
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <span class="Z3988">
                        <!--
                        <xsl:attribute name="title">
                            <xsl:call-template name="renderCOinS"/>
                        </xsl:attribute>
                        -->
                        <xsl:choose>
                            <xsl:when test="dim:field[@element='title']">
                                <xsl:value-of select="dim:field[@element='title'][1]/node()"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                            </xsl:otherwise>
                        </xsl:choose>
                    </span>
                </xsl:element>
            </p>
        </div>
    </xsl:template>



    <!-- 
        The summaryList display type; used to generate simple surrogates for the item involved 
    -->
        
    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="summaryList">
        <xsl:param name="position"/>
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemSummaryList-DIM">
                    <xsl:with-param name="position" select="$position"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Collection'">
                <xsl:call-template name="collectionSummaryList-DIM">
                    <xsl:with-param name="position" select="$position"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Community'">
                <!--
                <xsl:call-template name="communitySummaryList-DIM">
                -->
                <xsl:call-template name="collectionSummaryList-DIM">
                    <xsl:with-param name="position" select="$position"/>
                </xsl:call-template>
            </xsl:when>                
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
        
    <!-- 
        The templates that handle the respective cases of summaryList: item, collection, and community 
    -->

    <!-- An item rendered in the summaryList pattern. Commonly encountered in various browse-by pages
        and search results. -->
    <xsl:template name="itemSummaryList-DIM">
        <xsl:param name="position"/>
        <!-- Generate the thumbnail, if present, from the file section -->
        <div class="caixa">
            <xsl:apply-templates select="./mets:fileSec" mode="artifact-preview">
                <xsl:with-param name="primaryBitstream" select="./mets:structMap/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
            </xsl:apply-templates>
        </div>
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim" mode="itemSummaryList-DIM">
            <xsl:with-param name="position" select="$position"/>
        </xsl:apply-templates>
    </xsl:template>

    
    <!-- Generate the info about the item from the metadata section -->
    <xsl:template match="dim:dim" mode="itemSummaryList-DIM">
        <xsl:param name="position"/>
        <!-- adicionei uma div para separar a coluna do thumb com a coluna das informações -->
        <div class="info">
            <xsl:call-template name="info-resultado">
                <xsl:with-param name="position" select="$position"/>
            </xsl:call-template>

            <xsl:call-template name="funcoes-resultado">
                <xsl:with-param name="mets-context" select="ancestor::mets:METS"/>
            </xsl:call-template>

            <xsl:call-template name="colecao-resultado"/>

            <div class="borda">
                <p>
                    <img>
                        <xsl:attribute name="src">
                            <xsl:value-of select="$images-path"/>
                            <xsl:text>mais.png</xsl:text>
                        </xsl:attribute>
                    </img>
                    <i18n:text>xmlui.ArtifactBrowser.ItemViewer.show_full</i18n:text>
                </p>
            </div>

            <div class="caixa-borda">
<!--                <xsl:call-template name="ficha-metadados"/>-->
                <xsl:apply-templates select="." mode="itemMetadata" />
            </div>

        </div>

        <!-- div para dar um clear no float -->
        <div class="clear">
            <xsl:comment>Empty</xsl:comment>
        </div>
    </xsl:template>


    <xsl:template name="info-resultado">
        <xsl:param name="position"/>
        <xsl:variable name="itemWithdrawn" select="@withdrawn" />
        <div class="info-resultado">
            <!-- inseri span classe "numero-resultado" e coloquei texto pra visualizar-->
            <h2 class="titulo-resultado">
                <span class="numero-resultado"><xsl:value-of select="$position"/>. </span>
<!--                    <a>título do resultado</a>-->
                <span class="cor1">
                    <xsl:element name="a">
                        <xsl:attribute name="href">
                            <xsl:choose>
                                <xsl:when test="$itemWithdrawn">
                                    <xsl:value-of select="ancestor::mets:METS/@OBJEDIT" />
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="ancestor::mets:METS/@OBJID" />
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:attribute>
                        <span class="Z3988">
                            <!--
                            <xsl:attribute name="title">
                                <xsl:call-template name="renderCOinS"/>
                            </xsl:attribute>
                            -->
                            <xsl:choose>
                                <xsl:when test="dim:field[@element='title']">
                                    <xsl:value-of select="dim:field[@element='title'][1]/node()"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                                </xsl:otherwise>
                            </xsl:choose>
                        </span>
                    </xsl:element>
                </span>
            </h2>

            <p class="autor-resultado">
                <xsl:choose>
                    <xsl:when test="dim:field[@element='contributor'][@qualifier='author']">
                        <xsl:for-each select="dim:field[@element='contributor'][@qualifier='author']">
                            <span>
                              <xsl:if test="@authority">
                                <xsl:attribute name="class"><xsl:text>ds-dc_contributor_author-authority</xsl:text></xsl:attribute>
                              </xsl:if>
                              <xsl:copy-of select="node()"/>
                            </span>
                            <xsl:if test="count(following-sibling::dim:field[@element='contributor'][@qualifier='author']) != 0">
                                <xsl:text>; </xsl:text>
                            </xsl:if>
                        </xsl:for-each>
                    </xsl:when>
                    <xsl:when test="dim:field[@element='creator']">
                        <xsl:for-each select="dim:field[@element='creator']">
                            <xsl:copy-of select="node()"/>
                            <xsl:if test="count(following-sibling::dim:field[@element='creator']) != 0">
                                <xsl:text>; </xsl:text>
                            </xsl:if>
                        </xsl:for-each>
                    </xsl:when>
                    <xsl:when test="dim:field[@element='contributor']">
                        <xsl:for-each select="dim:field[@element='contributor']">
                            <xsl:copy-of select="node()"/>
                            <xsl:if test="count(following-sibling::dim:field[@element='contributor']) != 0">
                                <xsl:text>; </xsl:text>
                            </xsl:if>
                        </xsl:for-each>
                    </xsl:when>
                    <xsl:otherwise>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
                    </xsl:otherwise>
                </xsl:choose>
            </p>

            <p class="local-resultado">
                <xsl:if test="dim:field[@element='date' and @qualifier='issued'] or dim:field[@element='publisher']">
                    <span class="publisher-date">
                        <xsl:text>(</xsl:text>
                        <xsl:if test="dim:field[@element='publisher']">
                            <span class="publisher">
                                <xsl:copy-of select="dim:field[@element='publisher']/node()"/>
                            </span>
                            <xsl:text>, </xsl:text>
                        </xsl:if>
                        <span class="date">
                            <xsl:value-of select="substring(dim:field[@element='date' and @qualifier='issued']/node(),1,10)"/>
                        </span>
                        <xsl:text>)</xsl:text>
                    </span>
                </xsl:if>
            </p>
        </div>
    </xsl:template>


    <xsl:template name="funcoes-resultado">
        <xsl:param name="mets-context"/>
        
        <div class="funcoes-resultado">
            <div class="visualizar-item">
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="$mets-context/@OBJID" />
                    </xsl:attribute>
                    <img>
                        <xsl:attribute name="src">
                            <xsl:value-of select="$images-path"/>
                            <xsl:text>visualizar.png</xsl:text>
                        </xsl:attribute>
                    </img>
                    <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-view</i18n:text>
                </a>
            </div>
            <div class="baixar-item">
                <xsl:variable name="primaryBitstream" select="//mets:structMap/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
                <xsl:variable name="fileid">
                    <xsl:choose>
                        <xsl:when test="$primaryBitstream != ''">
                            <xsl:value-of select="$primaryBitstream"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="//mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']/mets:file[1]/@ID"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>

                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="substring-before($mets-context/mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']/mets:file[@ID=$fileid]/mets:FLocat[@LOCTYPE='URL']/@xlink:href, '?')"/>
                    </xsl:attribute>
                    <xsl:attribute name="title">
                        <xsl:value-of select="$mets-context/mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']/mets:file[@ID=$fileid]/mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                    </xsl:attribute>

                    <img>
                        <xsl:attribute name="src">
                            <xsl:value-of select="$images-path"/>
                            <xsl:text>baixar.png</xsl:text>
                        </xsl:attribute>
                    </img>
                    <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-download</i18n:text>
                </a>
            </div>
        </div>
    </xsl:template>


    <xsl:template name="colecao-resultado">
        <div class="colecao-resultado">
            <span class="tipo-documento">
                <xsl:choose>
                    <xsl:when test="dim:field[@element='type' and not(@qualifier)]">
                        <xsl:value-of select="dim:field[@element='type' and not(@qualifier)]"/>
                        <xsl:call-template name="getDocumentTypeThumbnail">
                            <xsl:with-param name="document_type_name" select="dim:field[@element='type' and not(@qualifier)]"/>
                        </xsl:call-template>
                    </xsl:when>
                </xsl:choose>
            </span>
        </div>
    </xsl:template>


<!--    <xsl:template name="ficha-metadados">-->
    <xsl:template match="dim:dim" mode="itemMetadata">
<!--        <div class="caixa-borda">-->
            <!-- @kepler Listar primeiro alguns campos específicos. -->
            <xsl:apply-templates select="dim:field[(@element='contributor' and @qualifier='author') or @element='creator' or @element='contributor']" mode="itemSummaryList-DIM"/>
            <xsl:apply-templates select="dim:field[@element='title' and not(@qualifier)]" mode="itemSummaryList-DIM"/>
            <xsl:apply-templates select="dim:field[@element='title' and @qualifier='alternative']" mode="itemSummaryList-DIM"/>
            <xsl:apply-templates select="dim:field[@element='relation' and @qualifier='isversionof']" mode="itemSummaryList-DIM"/>
            <xsl:apply-templates select="dim:field[@element='publisher' and not(@qualifier)]" mode="itemSummaryList-DIM"/>
            <xsl:apply-templates select="dim:field[@element='date' and @qualifier='issued']" mode="itemSummaryList-DIM"/>
            <xsl:apply-templates select="dim:field[@element='format' and @qualifier='medium']" mode="itemSummaryList-DIM"/>
            <xsl:apply-templates select="dim:field[@element='relation' and @qualifier='ispartofseries']" mode="itemSummaryList-DIM"/>
            <xsl:apply-templates select="dim:field[@element='language' and not(@qualifier)]" mode="itemSummaryList-DIM"/>
            <xsl:apply-templates select="dim:field[@element='description' and not(@qualifier)]" mode="itemSummaryList-DIM"/>
            <xsl:apply-templates select="dim:field[@element='description' and @qualifier='abstract']" mode="itemSummaryList-DIM"/>
            <xsl:apply-templates select="dim:field[@element='relation' and not(@qualifier)]" mode="itemSummaryList-DIM"/>
            <xsl:apply-templates select="dim:field[@element='relation' and @qualifier='ispartof']" mode="itemSummaryList-DIM"/>

            <!--
            <xsl:apply-templates select="*[not(name()='head')]" mode="detailList"/>
            -->

            <!-- @kepler Listar campos que não precisam de uma ordem específica. -->
            <xsl:apply-templates select="dim:field[not(
                ((@element='contributor' and @qualifier='author') or @element='creator' or @element='contributor')
                or
                (@element='title' and not(@qualifier))
                or
                (@element='title' and @qualifier='alternative')
                or
                (@element='relation' and @qualifier='isversionof')
                or
                (@element='publisher' and not(@qualifier))
                or
                (@element='date' and @qualifier='issued')
                or
                (@element='format' and @qualifier='medium')
                or
                (@element='relation' and @qualifier='ispartofseries')
                or
                (@element='language' and not(@qualifier))
                or
                (@element='description' and not(@qualifier))
                or
                (@element='description' and @qualifier='abstract')
                or
                (@element='relation' and not(@qualifier))
                or
                (@element='relation' and @qualifier='ispartof')

                or
                (@element='subject')
                or
                (@element='identifier' and @qualifier='uri')
                or
                (@element='type' and not(@qualifier))
                or
                (@qualifier='tableofcontents')
                )]" mode="itemSummaryList-DIM"/>

            <!-- @kepler Listar campos que devem aparecer no fim. -->
            <xsl:if test="dim:field[@element='subject' and not(@qualifier)]">
                <xsl:apply-templates select="dim:field[@element='subject' and not(@qualifier)]" mode="itemSummaryList-DIM"/>
            </xsl:if>
            <xsl:if test="dim:field[@element='subject' and @qualifier='lcsh']">
                <xsl:apply-templates select="dim:field[@element='subject' and @qualifier='lcsh']" mode="itemSummaryList-DIM"/>
            </xsl:if>
            <!--<xsl:apply-templates select="dim:field[@element='subject' and not(@qualifier)]" mode="itemSummaryList-DIM"/>-->
            <xsl:if test="dim:field[@element='identifier' and @qualifier='uri']">
                <xsl:apply-templates select="dim:field[@element='identifier' and @qualifier='uri']" mode="itemSummaryList-DIM"/>
            </xsl:if>
            <xsl:apply-templates select="dim:field[@element='type' and not(@qualifier)]" mode="itemSummaryList-DIM"/>
            <xsl:apply-templates select="dim:field[@element='description' and @qualifier='tableofcontents']" mode="itemSummaryList-DIM"/>
<!--        </div>-->
    </xsl:template>


    <xsl:template match="dim:field" mode="itemSummaryList-DIM">
        <p>
            <xsl:choose>
                <!-- Processando especificamente a lista de autores. -->
                <xsl:when test="(./@element='contributor' and ./@qualifier='author') or ./@element='creator' or ./@element='contributor'">
                    <xsl:call-template name="dimField-DcAuthorContributorCreator">
                        <xsl:with-param name="field" select="."/>
                    </xsl:call-template>
                </xsl:when>

                <xsl:otherwise>
                    <!-- Header column -->
                    <span class="tipo-dado">
                        <!--
                        <xsl:value-of select="./@mdschema"/>
                        <xsl:text>.</xsl:text>
                        <b><xsl:value-of select="./@element"/></b>
                        <xsl:if test="./@qualifier">
                            <xsl:text>.</xsl:text>
                            <xsl:value-of select="./@qualifier"/>
                        </xsl:if>
                        -->
                        <xsl:choose>
                            <xsl:when test="./@qualifier">
                                <i18n:text>xmlui.dri2xhtml.METS-1.0.item-<xsl:value-of select="./@element"/>-<xsl:value-of select="./@qualifier"/></i18n:text>:
                            </xsl:when>
                            <xsl:otherwise>
                                <i18n:text>xmlui.dri2xhtml.METS-1.0.item-<xsl:value-of select="./@element"/></i18n:text>:
                            </xsl:otherwise>
                        </xsl:choose>
                    </span>
                    <!-- Content column -->
                    <span class="dado-item">
                        <xsl:choose>
                            <!-- @kepler 2009-12-02
                                 - Transformando quebras de linhas '\n' em <br>, para aparecerem no HTML.
                                 @kepler 2009-12-09
                                 - Se o campo contiver handles, transforma-os em link.
                            -->
                            <xsl:otherwise>
                                <xsl:copy-of select="./node()"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </span>
                    <!--<td><xsl:value-of select="./@language"/></td>-->
                </xsl:otherwise>
            </xsl:choose>
        </p>
    </xsl:template>


    <!-- @kepler 2009-05-29 12:51:37 -->
    <!--
        Escondendo três campos DC em que o DSpace interfere: dc.date.accessioned, dc.date.available, e dc.description.provenance.
        Na visualização de registro completo.
    -->
    <!--
    <xsl:template match="dim:dim/dim:field[@element='description' and @qualifier='provenance']" mode="itemDetailView-DIM" priority="1">

    </xsl:template>
    -->

    <xsl:template match="dim:field[@element='date' and @qualifier='accessioned']" mode="itemSummaryList-DIM" priority="10">
        <!-- Hide. -->
    </xsl:template>

    <xsl:template match="dim:field[@element='date' and @qualifier='available']" mode="itemSummaryList-DIM" priority="10">
        <!-- Hide. -->
    </xsl:template>

    <xsl:template match="dim:field[@element='language' and @qualifier='iso']" mode="itemSummaryList-DIM" priority="10">
        <!-- Hide. -->
    </xsl:template>


    <!-- @kepler 2009-10-29 15:30:57 -->
    <!--
        Adiciona cada autor como link na visualização de registro simples e completo.
    -->
    <xsl:template name="dimField-DcAuthorContributorCreator">
        <xsl:param name="field" select="."/>
        <!--<td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-author</i18n:text>:</span></td>-->
        <span class="tipo-dado">
            <xsl:choose>
                <xsl:when test="$field/@qualifier">
                    <i18n:text>xmlui.dri2xhtml.METS-1.0.item-<xsl:value-of select="$field/@element"/>-<xsl:value-of select="$field/@qualifier"/></i18n:text>:
                </xsl:when>
                <xsl:otherwise>
                    <i18n:text>xmlui.dri2xhtml.METS-1.0.item-<xsl:value-of select="$field/@element"/></i18n:text>:
                </xsl:otherwise>
            </xsl:choose>
        </span>

        <span class="dado-item">
            <xsl:choose>
                <xsl:when test="$field[@element='contributor'][@qualifier='author']">
                    <xsl:for-each select="$field[@element='contributor'][@qualifier='author']">
                        <!-- Colocando link no nome de autor.
                        <xsl:copy-of select="node()"/>-->
                        <xsl:call-template name="makeBrowseLink">
                            <xsl:with-param name="type">dc.contributor.author</xsl:with-param>
                            <xsl:with-param name="value" select="node()"/>
                        </xsl:call-template>
                        <xsl:if test="count(following-sibling::dim:field[@element='contributor'][@qualifier='author']) != 0">
                            <xsl:text>; </xsl:text>
                        </xsl:if>
                    </xsl:for-each>
                </xsl:when>
                <xsl:when test="$field[@element='creator']">
                    <xsl:for-each select="$field[@element='creator']">
                        <!--<xsl:copy-of select="node()"/>-->
                        <xsl:call-template name="makeBrowseLink">
                            <xsl:with-param name="type">dc.creator</xsl:with-param>
                            <xsl:with-param name="value" select="node()"/>
                        </xsl:call-template>
                        <xsl:if test="count(following-sibling::dim:field[@element='creator']) != 0">
                            <xsl:text>; </xsl:text>
                        </xsl:if>
                    </xsl:for-each>
                </xsl:when>
                <xsl:when test="$field[@element='contributor'][@qualifier='other']">
                    <xsl:for-each select="$field[@element='contributor'][@qualifier='other']">
                        <!--<xsl:copy-of select="node()"/>-->
                        <xsl:call-template name="makeBrowseLink">
                            <xsl:with-param name="type">dc.contributor.other</xsl:with-param>
                            <xsl:with-param name="value" select="node()"/>
                        </xsl:call-template>
                        <xsl:if test="count(following-sibling::dim:field[@element='contributor'][@qualifier='other']) != 0">
                            <xsl:text>; </xsl:text>
                        </xsl:if>
                    </xsl:for-each>
                </xsl:when>
                <xsl:otherwise>
                    <i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
                </xsl:otherwise>
            </xsl:choose>
        </span>
    </xsl:template>


    <!-- @kepler 2009-10-29 15:07:57 -->
    <!--
        Adiciona navegação de algum tipo (author, subject, etc) como link na visualização de registro simples e completo.
    -->
    <xsl:template name="makeBrowseLink">
        <xsl:param name="type" select="."/>
        <xsl:param name="value" select="."/>
        <a>
            <xsl:attribute name="href">
                <xsl:value-of select="$context-path"/>
                <xsl:text>/search?fq=</xsl:text>
                <xsl:value-of select="$type"/>
                <xsl:text>:"</xsl:text>
                <xsl:call-template name="replace-string">
                    <xsl:with-param name="text" select="$value"/>
                    <xsl:with-param name="replace" select="' '"/>
                    <xsl:with-param name="with" select="'+'"/>
                </xsl:call-template>
                <xsl:text>"</xsl:text>
            </xsl:attribute>
            <xsl:copy-of select="$value"/>
        </a>
    </xsl:template>


    <!-- @kepler 2009-06-01 15:33:07 -->
    <!-- Adiciona o URI como link na visualização de registro completo. -->
    <xsl:template match="dim:field[@element='identifier' and @qualifier='uri']" mode="itemSummaryList-DIM" priority="10">
        <xsl:param name="field" select="."/>
        <xsl:choose>
        <xsl:when test="position() = 1">
            <xsl:variable name="uri">
                <xsl:choose>
                    <xsl:when test="contains(string(.), 'handle.net/')">
                        <xsl:value-of select="$absolute-base-url"/>
                        <xsl:text>/handle/</xsl:text>
                        <xsl:value-of select="substring-after(./node(), 'handle.net/')"/>
                    </xsl:when>
                    <xsl:when test="contains(string(.), '/handle/')">
                        <xsl:value-of select="$absolute-base-url"/>
                        <xsl:text>/handle/</xsl:text>
                        <xsl:value-of select="substring-after(./node(), '/handle/')"/>
                    </xsl:when>
                    <xsl:when test="fn:matches(string(.), '[0-9]{4}/[0-9-]+')">
                        <xsl:value-of select="$absolute-base-url"/>
                        <xsl:text>/handle/</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:when>
                    <xsl:otherwise>
                        <!--<xsl:copy-of select="./node()"/>-->
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>

            <xsl:if test="$uri != ''">
                <p>
        <span class="tipo-dado">
            <i18n:text>xmlui.dri2xhtml.METS-1.0.item-uri</i18n:text>:
        </span>
        <span class="dado-item">
                <a>
                    <xsl:attribute name="href">
                                <xsl:value-of select="$uri"/>
                            </xsl:attribute>
                            <xsl:value-of select="$uri"/>
                </a>
                    </span>
                </p>
            </xsl:if>
            <!--
                <xsl:if test="count(following-sibling::dim:field[@element='identifier' and @qualifier='uri']) != 0">
                    <br/>
                </xsl:if>
            -->
            <!--
            <xsl:for-each select="following-sibling::dim:field[@element='subject' and not(@qualifier)]">
                <br/>
                <xsl:apply-templates select="." mode="DcSubject-DIM"/>
            </xsl:for-each>
            -->
        </xsl:when>
        </xsl:choose>
    </xsl:template>


    <!-- @kepler 2009-06-10 11:37:47 -->
    <!--
        Agora o metadado dc:relation:ispartof é um link com dois possíveis nomes e destinos:
        1. Se o valor do elemento contiver "handle.net/":
            - O nome/texto do link será o título do item referenciado, e o destino do link será o registro do respectivo item.
        2. Caso contrário:
            - O nome/texto do link será o valor do elemento, e o destino do link será uma busca avançada por título pelo valor do elemento.
    -->
    <xsl:template match="dim:field[@element='relation' and @qualifier='ispartof']" mode="itemSummaryList-DIM" priority="10">
        <p>
            <span class="tipo-dado">
                <i18n:text>xmlui.dri2xhtml.METS-1.0.item-relation-ispartof</i18n:text>:
            </span>
            <span class="dado-item">
                <xsl:variable name="ispartof-handle_path">
                    <!--<xsl:value-of select="substring-after(./node(), 'handle.net/')"/>-->
                    <!-- @kepler 2009-12-09
                        Verifica se o campo contém algo no padrão 0000/0+, o que significa que é um handle.
                    -->
                    <xsl:choose>
                        <xsl:when test="contains(string(.), 'handle.net/')">
                            <xsl:value-of select="substring-after(./node(), 'handle.net/')"/>
                        </xsl:when>
                        <xsl:when test="fn:matches(string(.), '[0-9]{4}/[0-9-]+')">
                        <xsl:value-of select="."/>
                        </xsl:when>
                        <xsl:otherwise>
                            <!--<xsl:copy-of select="./node()"/>-->
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:variable name="ispartof-mets_path">
                    <xsl:value-of select="$absolute-base-url"/>
                    <xsl:text>/metadata/handle/</xsl:text>
                    <xsl:value-of select="$ispartof-handle_path"/>
                    <xsl:text>/mets.xml</xsl:text>
                </xsl:variable>
                <xsl:variable name="ispartof-item_path">
                    <xsl:value-of select="$context-path"/>
                    <xsl:text>/handle/</xsl:text>
                    <xsl:value-of select="$ispartof-handle_path"/>
                </xsl:variable>

                <!--<xsl:for-each select="$field[@element='relation' and @qualifier='ispartof']">-->
                <a>
                    <xsl:attribute name="href">
                        <!--<xsl:copy-of select="./node()"/>-->
                        <xsl:choose>
                            <xsl:when test="$ispartof-handle_path != ''">
                                <xsl:value-of select="$ispartof-item_path"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="$context-path"/>
                                <xsl:text>/search?query=</xsl:text>
                                <xsl:copy-of select="./node()"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="document($ispartof-mets_path)/mets:METS/mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@element='title'][1]">
                            <xsl:value-of select="document($ispartof-mets_path)/mets:METS/mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@element='title'][1]"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:copy-of select="./node()"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </a>
                <xsl:if test="count(following-sibling::dim:field[@element='relation' and @qualifier='ispartof']) != 0">
                    <br/>
                </xsl:if>
                <!--</xsl:for-each>-->
            </span>
        </p>
    </xsl:template>


    <!-- @kepler 2010-02-25 18:17:47 -->
    <!--
        O campo dc:relation:uri é transformado em link:
        1. Se o valor do campo começar com "http://":
            - O nome/texto do link será o valor do campo.
        2. Caso contrário:
            - O nome/texto do link será o valor do campo em texto puro.
    -->
    <xsl:template match="dim:field[@element='relation' and @qualifier='uri']" mode="itemSummaryList-DIM" priority="10">
        <p>
        <span class="tipo-dado">
            <i18n:text>xmlui.dri2xhtml.METS-1.0.item-relation-uri</i18n:text>:
        </span>
        <span class="dado-item">
            <xsl:choose>
                <xsl:when test="substring-before(., '://') = 'http'">
                    <a>
                        <xsl:attribute name="href">
                            <xsl:copy-of select="."/>
                        </xsl:attribute>
                        <xsl:copy-of select="."/>
                    </a>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy-of select="."/>
                </xsl:otherwise>
            </xsl:choose>
        </span>
        </p>
    </xsl:template>


    <!-- @kepler 2010-05-13 16:23:47 -->
    <!--
        O campo dc:relation.requires:
        1. É transformado em link se contiver um handle. O nome/texto do link será o título do item referenciado; se não for encontrado, será o mesmo que em 2.
        2. Caso contrário, o nome/texto do link será o valor do campo em texto puro.
    -->
    <xsl:template match="dim:field[@element='relation' and @qualifier='requires']" mode="itemSummaryList-DIM" priority="10">
        <p>
        <span class="tipo-dado">
            <i18n:text>xmlui.dri2xhtml.METS-1.0.item-relation-requires</i18n:text>:
        </span>
        <span class="dado-item">
            <!-- @kepler
                Verifica se o campo contém algo no padrão 0000/0+, o que significa que é um handle.
            -->
            <xsl:choose>
                <xsl:when test="fn:matches(string(.), '[0-9]{4}/[0-9-]+')">
                    <xsl:variable name="title">
                        <xsl:call-template name="getItemTitleFromHandle">
                            <xsl:with-param name="handle" select="."/>
                        </xsl:call-template>
                    </xsl:variable>

                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of select="$context-path"/>
                            <xsl:text>/handle/</xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:attribute>
                        <xsl:choose>
                            <xsl:when test="$title != ''">
                                <xsl:value-of select="$title"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:copy-of select="."/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </a>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy-of select="."/>
                </xsl:otherwise>
            </xsl:choose>
        </span>
        </p>
    </xsl:template>



    <!-- @kepler 2009-10-29 14:07:57 -->
    <!--
        Adiciona o assunto como link na visualização de registro simples e completo.
    -->
    <xsl:template match="dim:field[@element='subject' and not(@qualifier)]" mode="itemSummaryList-DIM" priority="10">
        <xsl:choose>
        <xsl:when test="position() = 1">
            <p>
                <span class="tipo-dado">
                    <i18n:text>xmlui.dri2xhtml.METS-1.0.item-subject-other</i18n:text>:
                    <br/>
                </span>
                <span class="dado-item">
                    <!--<a>
                        <xsl:attribute name="href">
                            <xsl:value-of select="$context-path"/>
                            <xsl:text>/browse?type=subject&amp;value=</xsl:text>
                            <xsl:copy-of select="./node()"/>
                        </xsl:attribute>
                        <xsl:copy-of select="./node()"/>
                    </a>-->
                    <xsl:call-template name="makeBrowseLink">
                        <xsl:with-param name="type">dc.subject</xsl:with-param>
                        <xsl:with-param name="value" select="."/>
                    </xsl:call-template>
                    <xsl:for-each select="following-sibling::dim:field[@element='subject' and not(@qualifier)]">
                        <br/>
                        <xsl:apply-templates select="." mode="DcSubject-DIM"/>
                    </xsl:for-each>
                </span>
            </p>
        </xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="dim:field[@element='subject' and @qualifier='lcsh']" mode="itemSummaryList-DIM" priority="10">
        <xsl:choose>
        <xsl:when test="position() = 1">
            <p>
                <span class="tipo-dado">
                    <i18n:text>xmlui.dri2xhtml.METS-1.0.item-subject-other</i18n:text>:
                    <br/>
                </span>
                <span class="dado-item">
                    <!--<a>
                        <xsl:attribute name="href">
                            <xsl:value-of select="$context-path"/>
                            <xsl:text>/browse?type=subject&amp;value=</xsl:text>
                            <xsl:copy-of select="./node()"/>
                        </xsl:attribute>
                        <xsl:copy-of select="./node()"/>
                    </a>-->
                    <xsl:call-template name="makeBrowseLink">
                        <xsl:with-param name="type">dc.subject.lcsh</xsl:with-param>
                        <xsl:with-param name="value" select="."/>
                    </xsl:call-template>
                    <xsl:for-each select="following-sibling::dim:field[@element='subject' and @qualifier='lcsh']">
                        <br/>
                        <xsl:apply-templates select="." mode="DcSubject-DIM"/>
                    </xsl:for-each>
                </span>
            </p>
        </xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="dim:field[@element='subject' and (not(@qualifier) or @qualifier='lcsh')]" mode="DcSubject-DIM">
            <!--
                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of select="$context-path"/>
                            <xsl:text>/browse?type=subject&amp;value=</xsl:text>
                            <xsl:copy-of select="./node()"/>
                        </xsl:attribute>
                        <xsl:copy-of select="./node()"/>
                    </a>
            -->
        <xsl:call-template name="makeBrowseLink">
            <xsl:with-param name="type">dc.subject<xsl:if test="@qualifier">.<xsl:value-of select="@qualifier"/></xsl:if></xsl:with-param>
            <xsl:with-param name="value" select="."/>
        </xsl:call-template>
    </xsl:template>


    <xsl:template match="dim:field[@element='description' and @qualifier='tableofcontents']" mode="itemSummaryList-DIM" priority="10">
        <xsl:choose>
            <!-- If toc has gravuras -->
            <xsl:when test="contains(string(.), '(Gravura')">
                <!-- Skipping, since we use this in another place in the page. -->
<!--                <xsl:call-template name="dimSpecialField-DcDescriptionTableofcontents-pictures"/>-->
            </xsl:when>
            <xsl:otherwise>
                <p>
                    <!-- Header column -->
                    <span class="tipo-dado">
                        <xsl:choose>
                            <xsl:when test="./@qualifier">
                                <i18n:text>xmlui.dri2xhtml.METS-1.0.item-<xsl:value-of select="./@element"/>-<xsl:value-of select="./@qualifier"/></i18n:text>:
                            </xsl:when>
                            <xsl:otherwise>
                                <i18n:text>xmlui.dri2xhtml.METS-1.0.item-<xsl:value-of select="./@element"/></i18n:text>:
                            </xsl:otherwise>
                        </xsl:choose>
                    </span>
                    <!-- Content column -->
                    <span class="dado-item">
                        <xsl:variable name="content">
                            <xsl:call-template name="replaceNewLines">
                                <xsl:with-param name="stringIn" select="."/>
                            </xsl:call-template>
                        </xsl:variable>
                        <xsl:copy-of select="$content"/>
                    </span>
                </p>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>



    <xsl:template match="dim:field[@element='description' and not(@qualifier)]" mode="itemSummaryList-DIM" priority="10">
        <xsl:choose>
            <xsl:otherwise>
                <p>
                    <!-- Header column -->
                    <span class="tipo-dado">
                        <xsl:choose>
                            <xsl:when test="./@qualifier">
                                <i18n:text>xmlui.dri2xhtml.METS-1.0.item-<xsl:value-of select="./@element"/>-<xsl:value-of select="./@qualifier"/></i18n:text>:
                            </xsl:when>
                            <xsl:otherwise>
                                <i18n:text>xmlui.dri2xhtml.METS-1.0.item-<xsl:value-of select="./@element"/></i18n:text>:
                            </xsl:otherwise>
                        </xsl:choose>
                    </span>
                    <!-- Content column -->
                    <span class="dado-item">
                        <xsl:copy-of select="./node()"/>

                        <!--
                        <xsl:copy>
                            <xsl:call-template name="getHTML">
                                <xsl:with-param name="content" select="."/>
                            </xsl:call-template>
                        </xsl:copy>
                        <xsl:variable name="content">
                            <xsl:call-template name="replaceNewLines">
                                <xsl:with-param name="stringIn" select="."/>
                            </xsl:call-template>
                        <xsl:value-of select="$content" disable-output-escaping="yes"/>
                        <xsl:copy-of select="$content"/>
                        </xsl:variable>
                        -->
                    </span>
                </p>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="getHTML">
        <xsl:param name="content"/>
        <xsl:copy-of select="$content"/>
    </xsl:template>


    <xsl:template name="dimSpecialField-DcDescriptionTableofcontents-pictures">
        <xsl:param name="field" select="."/>

        <table id="item-tableofcontents">
            <tr>
                <th class="cor1" style="padding: 5px 2px;"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-description-tableofcontents.pictures-head1</i18n:text></th>
                <th class="cor1"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-description-tableofcontents.pictures-head2</i18n:text></th>
            </tr>
            <xsl:call-template name="putLinksInGravuras">
                <xsl:with-param name="stringIn" select="concat($field, '&#xa;')"/>
            </xsl:call-template>
        </table>
    </xsl:template>


    <!-- @kepler 2009-12-02
        Substitui quebras de linhas '\n' por '<br/>'.
    -->
    <xsl:template name="replaceNewLines">
        <xsl:param name="stringIn"/>
        <xsl:variable name="cr" select="'&#xa;'"/>
        <xsl:choose>
            <xsl:when test="contains($stringIn, $cr)">
                <xsl:copy-of select="substring-before($stringIn, $cr)" />
                <br/>
                <xsl:call-template name="replaceNewLines">
                    <xsl:with-param name="stringIn" select="substring-after($stringIn, $cr)" />
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="$stringIn" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <xsl:template name="replace-string">
        <xsl:param name="text"/>
        <xsl:param name="replace"/>
        <xsl:param name="with"/>
        <xsl:choose>
            <xsl:when test="contains($text,$replace)">
                <xsl:value-of select="substring-before($text,$replace)"/>
                <xsl:value-of select="$with"/>
                <xsl:call-template name="replace-string">
                    <xsl:with-param name="text" select="substring-after($text,$replace)"/>
                    <xsl:with-param name="replace" select="$replace"/>
                    <xsl:with-param name="with" select="$with"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="$text"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <xsl:template name="getItemTitleFromHandle">
        <xsl:param name="handle"/>

        <xsl:if test="fn:matches(string($handle), '[0-9]{4}/[0-9-]+')">
            <xsl:variable name="ispartof-mets_path">
                <xsl:value-of select="$absolute-base-url"/>
                <xsl:text>/metadata/handle/</xsl:text>
                <xsl:value-of select="$handle"/>
                <xsl:text>/mets.xml</xsl:text>
            </xsl:variable>

            <xsl:variable name="title" select="document($ispartof-mets_path)/mets:METS/mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@element='title' and not(@qualifier)]"/>
            <xsl:value-of select="$title"/>
        </xsl:if>
    </xsl:template>


    <xsl:template name="getItemDcElementQualifierFromHandle">
        <xsl:param name="handle"/>
        <xsl:param name="element"/>
        <xsl:param name="qualifier"/>

        <xsl:if test="fn:matches(string($handle), '[0-9]{4}/[0-9-]+')">
            <xsl:variable name="ispartof-mets_path">
                <xsl:value-of select="$absolute-base-url"/>
                <xsl:text>/metadata/handle/</xsl:text>
                <xsl:value-of select="$handle"/>
                <xsl:text>/mets.xml</xsl:text>
            </xsl:variable>

            <xsl:variable name="field" select="document($ispartof-mets_path)/mets:METS/mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@element=$element and @qualifier=$qualifier]"/>
            <xsl:value-of select="$field"/>
        </xsl:if>
    </xsl:template>


    <xsl:template name="getItemFirstImageBitstreamURL">
        <xsl:param name="handle"/>

        <xsl:if test="fn:matches(string($handle), '[0-9]{4}/[0-9-]+')">
            <xsl:variable name="ispartof-mets_path">
                <xsl:value-of select="$absolute-base-url"/>
                <xsl:text>/metadata/handle/</xsl:text>
                <xsl:value-of select="$handle"/>
                <xsl:text>/mets.xml</xsl:text>
            </xsl:variable>

            <xsl:variable name="image_url" select="document($ispartof-mets_path)/mets:METS/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file[substring-before(@MIMETYPE, '/') = 'image']/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
            <xsl:value-of select="substring-before($image_url, '?')"/>
        </xsl:if>
    </xsl:template>


    <xsl:template name="putLinksInGravuras">
        <xsl:param name="stringIn"/>
        <xsl:variable name="gravuraPattern" select="'\(Gravura [0-9]+\) - [0-9]{4}/[0-9-]+'"/>
        <xsl:variable name="br" select="';'"/>

        <xsl:variable name="gravura" select="normalize-space(substring-before($stringIn, $br))"/>

        <tr>
            <xsl:choose>
                <xsl:when test="fn:matches($gravura, $gravuraPattern)">
                    <td style="text-align: center;">
                        <xsl:value-of select="substring-before(substring-after(normalize-space(substring-before($gravura, ' - ')), 'Gravura '), ')')"/>
                    </td>

                    <xsl:variable name="handleNumber" select="normalize-space(substring-after($gravura, ' - '))"/>

                    <td style="padding-left:0;margin-left:0;">
                        <a>
                            <xsl:attribute name="href">
                                <xsl:value-of select="$context-path"/>
                                <xsl:text>/handle/</xsl:text>
                                <xsl:value-of select="$handleNumber"/>
                            </xsl:attribute>

                            <!--<xsl:value-of select="$gravura"/>-->
                            <xsl:variable name="title">
                                <!--
                                <xsl:value-of select="$handleNumber"/>
                                -->
                                <xsl:call-template name="getItemTitleFromHandle">
                                    <xsl:with-param name="handle" select="$handleNumber"/>
                                </xsl:call-template>
                            </xsl:variable>
                            <xsl:choose>
                            <xsl:when test="$title != ''">
                                <xsl:value-of select="$title"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="$gravura"/>
                            </xsl:otherwise>
                            </xsl:choose>
                        </a>
                    </td>

<!--                    <td>
                        <a class="plain-image-link">

                            <xsl:attribute name="href">
                                <xsl:call-template name="getItemFirstImageBitstreamURL">
                                    <xsl:with-param name="handle" select="$handleNumber"/>
                                </xsl:call-template>
                            </xsl:attribute>

                            <xsl:attribute name="title">
                                <xsl:call-template name="getItemTitleFromHandle">
                                    <xsl:with-param name="handle" select="$handleNumber"/>
                                </xsl:call-template>
                            </xsl:attribute>

                            <xsl:attribute name="rel">
		                        <xsl:text>lightbox-tour</xsl:text>
                            </xsl:attribute>

                            <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-view</i18n:text>

                        </a>

                    </td>-->
                </xsl:when>
                <xsl:otherwise>
                    <td>
                        <xsl:value-of select="$gravura"/>
                    </td>
                </xsl:otherwise>
            </xsl:choose>
        </tr>

        <xsl:if test="contains(substring-after($stringIn, $br), $br)">
            <xsl:call-template name="putLinksInGravuras">
                <xsl:with-param name="stringIn" select="substring-after($stringIn, $br)" />
            </xsl:call-template>
        </xsl:if>
    </xsl:template>



<!--CHANGED-->
    <!-- A collection rendered in the summaryList pattern. Encountered on the community-list page -->
    <xsl:template name="collectionSummaryList-DIM">
        <xsl:param name="position" select="''"/>
        <xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim"/>
<!--        <span>cole&ccedil;&atilde;o 1 </span><span class="total-colecao">(100)</span>-->

        <!-- Generate the thumbnail, if present, from the file section -->
        <div class="caixa">
            <xsl:comment>collectionSummaryList-DIM</xsl:comment>
            <xsl:apply-templates select="./mets:fileSec" mode="artifact-preview"/>
        </div>
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim" mode="communityCollectionSummaryList-DIM">
            <xsl:with-param name="position" select="$position"/>
        </xsl:apply-templates>

        <!--
        <span>
            <a href="{@OBJID}">
                <xsl:choose>
                    <xsl:when test="string-length($data/dim:field[@element='title'][1]) &gt; 0">
                        <xsl:value-of select="$data/dim:field[@element='title'][1]"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                    </xsl:otherwise>
                </xsl:choose>
            </a>
        </span>
        -->
		<!--Display collection strengths (item counts) if they exist-->
        <!--
        <span class="total-colecao">
            <xsl:if test="string-length($data/dim:field[@element='format'][@qualifier='extent'][1]) &gt; 0">
                <xsl:text>(</xsl:text>
                <xsl:value-of select="$data/dim:field[@element='format'][@qualifier='extent'][1]"/>
                <xsl:text>)</xsl:text>
            </xsl:if>
        </span>
        -->
    </xsl:template>

<!--CHANGED-->
    <!-- A community rendered in the summaryList pattern. Encountered on the community-list and on 
        on the front page. -->
    <xsl:template name="communitySummaryList-DIM">
        <xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim"/>
        <span>
            <a href="{@OBJID}">
                <xsl:choose>
                    <xsl:when test="substring-after(@ID, '/') = 1">
                        <i18n:text>xmlui.general.main_tab</i18n:text>
                    </xsl:when>
                    <xsl:when test="string-length($data/dim:field[@element='title'][1]) &gt; 0">
                        <xsl:value-of select="$data/dim:field[@element='title'][1]"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                    </xsl:otherwise>
                </xsl:choose>
            </a>
        </span>
        <span class="total-colecao">
		<!--Display community strengths (item counts) if they exist-->
            <xsl:if test="string-length($data/dim:field[@element='format'][@qualifier='extent'][1]) &gt; 0">
                <xsl:text> (</xsl:text>
                <xsl:value-of select="$data/dim:field[@element='format'][@qualifier='extent'][1]"/>
                <xsl:text>)</xsl:text>
            </xsl:if>
        </span>
    </xsl:template>
    


    <!-- Generate the info about the item from the metadata section -->
    <xsl:template match="dim:dim" mode="communityCollectionSummaryList-DIM">
        <xsl:param name="position" select="''"/>
        <!-- adicionei uma div para separar a coluna do thumb com a coluna das informações -->
        <div class="info">
            <xsl:call-template name="commColl-info-resultado">
                <xsl:with-param name="position" select="$position"/>
            </xsl:call-template>

            <xsl:call-template name="commColl-funcoes-resultado">
                <xsl:with-param name="mets-context" select="ancestor::mets:METS"/>
            </xsl:call-template>

            <xsl:call-template name="commColl-colecao-resultado"/>

            <div class="borda">
                <p>
                    <img>
                        <xsl:attribute name="src">
                            <xsl:value-of select="$images-path"/>
                            <xsl:text>mais.png</xsl:text>
                        </xsl:attribute>
                    </img>
                    <i18n:text>xmlui.ArtifactBrowser.ItemViewer.show_full</i18n:text>
                </p>
            </div>

            <div class="caixa-borda">
<!--                <xsl:call-template name="ficha-metadados"/>-->
                <xsl:apply-templates select="." mode="communityCollectionMetadata" />
            </div>

        </div>

        <!-- div para dar um clear no float -->
        <div class="clear">
            <xsl:comment>Empty</xsl:comment>
        </div>
    </xsl:template>



    <xsl:template name="commColl-info-resultado">
        <xsl:param name="position" select="''"/>
        <xsl:variable name="itemWithdrawn" select="@withdrawn" />
        <div class="info-resultado">
            <!-- inseri span classe "numero-resultado" e coloquei texto pra visualizar-->
            <h2 class="titulo-resultado">
                <span class="numero-resultado"><xsl:if test="$position != ''"><xsl:value-of select="$position"/>. </xsl:if></span>
<!--                    <a>título do resultado</a>-->
                <span class="cor1">
                    <xsl:element name="a">
                        <xsl:attribute name="href">
                            <xsl:choose>
                                <xsl:when test="$itemWithdrawn">
                                    <xsl:value-of select="ancestor::mets:METS/@OBJEDIT" />
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="ancestor::mets:METS/@OBJID" />
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:attribute>
                        <span class="Z3988">
                            <!--
                            <xsl:attribute name="title">
                                <xsl:call-template name="renderCOinS"/>
                            </xsl:attribute>
                            -->
                            <xsl:choose>
                                <xsl:when test="dim:field[@element='title']">
                                    <xsl:value-of select="dim:field[@element='title'][1]/node()"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                                </xsl:otherwise>
                            </xsl:choose>

                            <span class="total-colecao">
                                <xsl:if test="string-length(dim:field[@element='format'][@qualifier='extent'][1]) &gt; 0">
                                    <xsl:text> (</xsl:text>
                                    <xsl:value-of select="dim:field[@element='format'][@qualifier='extent'][1]"/>
                                    <xsl:text>)</xsl:text>
                                </xsl:if>
                            </span>

                        </span>
                    </xsl:element>
                </span>
            </h2>

            <p class="autor-resultado">
                <xsl:choose>
                    <xsl:when test="dim:field[@element='contributor'][@qualifier='author']">
                        <xsl:for-each select="dim:field[@element='contributor'][@qualifier='author']">
                            <span>
                              <xsl:if test="@authority">
                                <xsl:attribute name="class"><xsl:text>ds-dc_contributor_author-authority</xsl:text></xsl:attribute>
                              </xsl:if>
                              <xsl:copy-of select="node()"/>
                            </span>
                            <xsl:if test="count(following-sibling::dim:field[@element='contributor'][@qualifier='author']) != 0">
                                <xsl:text>; </xsl:text>
                            </xsl:if>
                        </xsl:for-each>
                    </xsl:when>
                    <xsl:when test="dim:field[@element='creator']">
                        <xsl:for-each select="dim:field[@element='creator']">
                            <xsl:copy-of select="node()"/>
                            <xsl:if test="count(following-sibling::dim:field[@element='creator']) != 0">
                                <xsl:text>; </xsl:text>
                            </xsl:if>
                        </xsl:for-each>
                    </xsl:when>
                    <xsl:when test="dim:field[@element='contributor']">
                        <xsl:for-each select="dim:field[@element='contributor']">
                            <xsl:copy-of select="node()"/>
                            <xsl:if test="count(following-sibling::dim:field[@element='contributor']) != 0">
                                <xsl:text>; </xsl:text>
                            </xsl:if>
                        </xsl:for-each>
                    </xsl:when>
                    <xsl:otherwise>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.various-authors</i18n:text>
                    </xsl:otherwise>
                </xsl:choose>
            </p>

            <p class="local-resultado">
                <xsl:if test="dim:field[@element='date' and @qualifier='issued'] or dim:field[@element='publisher']">
                    <span class="publisher-date">
                        <xsl:text>(</xsl:text>
                        <xsl:if test="dim:field[@element='publisher']">
                            <span class="publisher">
                                <xsl:copy-of select="dim:field[@element='publisher']/node()"/>
                            </span>
                            <xsl:text>, </xsl:text>
                        </xsl:if>
                        <span class="date">
                            <xsl:value-of select="substring(dim:field[@element='date' and @qualifier='issued']/node(),1,10)"/>
                        </span>
                        <xsl:text>)</xsl:text>
                    </span>
                </xsl:if>
            </p>
        </div>
    </xsl:template>


    <xsl:template name="commColl-funcoes-resultado">
        <xsl:param name="mets-context"/>
        
        <div class="funcoes-resultado">
            <div class="visualizar-item">
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="$mets-context/@OBJID" />
                    </xsl:attribute>
                    <img>
                        <xsl:attribute name="src">
                            <xsl:value-of select="$images-path"/>
                            <xsl:text>visualizar.png</xsl:text>
                        </xsl:attribute>
                    </img>
                    <i18n:text>xmlui.dri2xhtml.METS-1.0.collection-view</i18n:text>
                </a>
            </div>
        </div>
    </xsl:template>


    <xsl:template name="commColl-colecao-resultado">
        <div class="colecao-resultado">
            <span class="tipo-documento">
                <xsl:choose>
                    <xsl:when test="dim:field[@element='type' and not(@qualifier)]">
                        <xsl:value-of select="dim:field[@element='type' and not(@qualifier)]"/>
                        <xsl:call-template name="getDocumentTypeThumbnail">
                            <xsl:with-param name="document_type_name" select="dim:field[@element='type' and not(@qualifier)]"/>
                        </xsl:call-template>
                    </xsl:when>
                </xsl:choose>
            </span>
        </div>
    </xsl:template>


<!--    <xsl:template name="ficha-metadados">-->
    <xsl:template match="dim:dim" mode="communityCollectionMetadata">
        <xsl:if test="dim:field[@element='description' and not(@qualifier)] != ''">
            <xsl:apply-templates select="dim:field[@element='description' and not(@qualifier)]" mode="itemSummaryList-DIM"/>
        </xsl:if>
        <xsl:if test="dim:field[@element='description' and @qualifier='abstract'] != ''">
            <xsl:apply-templates select="dim:field[@element='description' and @qualifier='abstract']" mode="itemSummaryList-DIM"/>
        </xsl:if>
        <xsl:if test="dim:field[@element='description' and @qualifier='tableofcontents'] != ''">
            <xsl:apply-templates select="dim:field[@element='description' and @qualifier='tableofcontents']" mode="itemSummaryList-DIM"/>
        </xsl:if>
        <xsl:if test="dim:field[@element='identifier' and @qualifier='uri'] != ''">
            <xsl:apply-templates select="dim:field[@element='identifier' and @qualifier='uri']" mode="itemSummaryList-DIM"/>
        </xsl:if>
        <xsl:if test="dim:field[@element='provenance' and not(@qualifier)] != ''">
            <xsl:apply-templates select="dim:field[@element='provenance' and not(@qualifier)]" mode="itemSummaryList-DIM"/>
        </xsl:if>
        <xsl:if test="dim:field[@element='rights' and not(@qualifier)] != ''">
            <xsl:apply-templates select="dim:field[@element='rights' and not(@qualifier)]" mode="itemSummaryList-DIM"/>
        </xsl:if>
        <xsl:if test="dim:field[@element='rights' and @qualifier='license'] != ''">
            <xsl:apply-templates select="dim:field[@element='rights' and @qualifier='license']" mode="itemSummaryList-DIM"/>
        </xsl:if>
    </xsl:template>


    
    <xsl:template name="collectionSummaryGrid-DIM">
        <xsl:param name="position"/>
        <!-- Generate the thumbnail, if present, from the file section -->
        <!-- Generate the thumbnail, if present, from the file section -->
        <div class="caixa">
            <xsl:comment>collectionSummaryList-DIM</xsl:comment>
            <xsl:apply-templates select="./mets:fileSec" mode="artifact-preview"/>
        </div>

        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim" mode="collectionSummaryGrid-DIM">
            <xsl:with-param name="position" select="$position"/>
        </xsl:apply-templates>
    </xsl:template>
    

    <xsl:template match="dim:dim" mode="collectionSummaryGrid-DIM">
        <xsl:param name="position"/>
        <xsl:variable name="itemWithdrawn" select="@withdrawn" />
        <div class="info-resultado">
            <p class="titulo-resultado">
                <span class="numero-resultado"><xsl:value-of select="$position"/>. </span>
<!--                    <a>título do resultado</a>-->
                <xsl:element name="a">
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="$itemWithdrawn">
                                <xsl:value-of select="ancestor::mets:METS/@OBJEDIT" />
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="ancestor::mets:METS/@OBJID" />
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <span>
                        <!-- class="Z3988">
                        <xsl:attribute name="title">
                            <xsl:call-template name="renderCOinS"/>
                        </xsl:attribute>
                        -->
                        <xsl:choose>
                            <xsl:when test="dim:field[@element='title']">
                                <xsl:value-of select="dim:field[@element='title'][1]/node()"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                            </xsl:otherwise>
                        </xsl:choose>
                    </span>
                </xsl:element>
            </p>
        </div>
    </xsl:template>
    
    
    
    
    <!-- 
        The detailList display type; used to generate simple surrogates for the item involved, but with
        a slightly higher level of information provided. Not commonly used. 
    -->
    
    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="detailList">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemDetailList-DIM"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Collection'">
                <xsl:call-template name="collectionDetailList-DIM"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Community'">
                <xsl:call-template name="communityDetailList-DIM"/>
            </xsl:when>                
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
        
    <!-- An item rendered in the detailList pattern. Currently Manakin does not have a separate use for 
        detailList on items, so the logic of summaryList is used in its place. --> 
    <xsl:template name="itemDetailList-DIM">
        
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim" mode="itemSummaryList-DIM"/>
        
        <!-- Generate the thunbnail, if present, from the file section -->
        <xsl:apply-templates select="./mets:fileSec" mode="artifact-preview"/>
    </xsl:template>
    
    
    <!-- A collection rendered in the detailList pattern. Encountered on the item view page as 
        the "this item is part of these collections" list -->
    <xsl:template name="collectionDetailList-DIM">
        <xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim"/>
        <a href="{@OBJID}">
            <xsl:choose>
	            <xsl:when test="string-length($data/dim:field[@element='title'][1]) &gt; 0">
	                <xsl:value-of select="$data/dim:field[@element='title'][1]"/>
	            </xsl:when>
	            <xsl:otherwise>
	                <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
	            </xsl:otherwise>
            </xsl:choose>
        </a>
		<!--Display collection strengths (item counts) if they exist-->
		<xsl:if test="string-length($data/dim:field[@element='format'][@qualifier='extent'][1]) &gt; 0">
            <xsl:text> [</xsl:text>
            <xsl:value-of select="$data/dim:field[@element='format'][@qualifier='extent'][1]"/>
            <xsl:text>]</xsl:text>
        </xsl:if>
        <br/>
        <xsl:choose>
            <xsl:when test="$data/dim:field[@element='description' and @qualifier='abstract']">
                <xsl:copy-of select="$data/dim:field[@element='description' and @qualifier='abstract']/node()"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="$data/dim:field[@element='description'][1]/node()"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- A community rendered in the detailList pattern. Not currently used. -->
    <xsl:template name="communityDetailList-DIM">
        <xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim"/>
        <span class="bold">
            <a href="{@OBJID}">
                <xsl:choose>
		            <xsl:when test="string-length($data/dim:field[@element='title'][1]) &gt; 0">
		                <xsl:value-of select="$data/dim:field[@element='title'][1]"/>
		            </xsl:when>
		            <xsl:otherwise>
		                <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
		            </xsl:otherwise>
           		</xsl:choose>
            </a>
			<!--Display community strengths (item counts) if they exist-->
			<xsl:if test="string-length($data/dim:field[@element='format'][@qualifier='extent'][1]) &gt; 0">
                <xsl:text> [</xsl:text>
                <xsl:value-of select="$data/dim:field[@element='format'][@qualifier='extent'][1]"/>
                <xsl:text>]</xsl:text>
            </xsl:if>
            <br/>
            <xsl:choose>
                <xsl:when test="$data/dim:field[@element='description' and @qualifier='abstract']">
                    <xsl:copy-of select="$data/dim:field[@element='description' and @qualifier='abstract']/node()"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy-of select="$data/dim:field[@element='description'][1]/node()"/>
                </xsl:otherwise>
            </xsl:choose>
        </span>
    </xsl:template>
    
    
    
    
    
    
    
    
    
    
    
    <!-- 
        The summaryView display type; used to generate a near-complete view of the item involved. It is currently
        not applicable to communities and collections. 
    -->
    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="summaryView">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemSummaryView-DIM"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Collection'">
                <xsl:call-template name="collectionSummaryView-DIM"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Community'">
                <xsl:call-template name="communitySummaryView-DIM"/>
            </xsl:when>                
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- An item rendered in the summaryView pattern. This is the default way to view a DSpace item in Manakin. -->
    <xsl:template name="itemSummaryView-DIM">
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim" mode="itemSummaryView-DIM"/>
        
        <!-- Generate the bitstream information from the file section -->
        <xsl:choose>
            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">
                <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">
                    <xsl:with-param name="context" select="."/>
                    <xsl:with-param name="primaryBitstream" select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
                </xsl:apply-templates>
            </xsl:when>
            <!-- Special case for handling ORE resource maps stored as DSpace bitstreams -->
            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='ORE']">
                <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='ORE']"/>
            </xsl:when>
            <xsl:otherwise>
                <h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h2> 
                <table class="ds-table file-list">
                    <tr class="ds-table-header-row">
                        <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-file</i18n:text></th>
                        <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-size</i18n:text></th>
                        <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-format</i18n:text></th>
                        <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-view</i18n:text></th>
                    </tr>
                    <tr>
                        <td colspan="4">
                            <p><i18n:text>xmlui.dri2xhtml.METS-1.0.item-no-files</i18n:text></p>
                        </td>
                    </tr>
                </table>
            </xsl:otherwise>
        </xsl:choose>

        <!-- Generate the Creative Commons license information from the file section (DSpace deposit license hidden by default)-->
        <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CC-LICENSE']"/>

    </xsl:template>
    
    
    <!-- Generate the info about the item from the metadata section -->
    <xsl:template match="dim:dim" mode="itemSummaryView-DIM">
        <table class="ds-includeSet-table">
         <xsl:call-template name="itemSummaryView-DIM-fields">
         </xsl:call-template>
        </table>
    </xsl:template>

    <!-- render each field on a row, alternating phase between odd and even -->
    <!-- recursion needed since not every row appears for each Item. -->
    <xsl:template name="itemSummaryView-DIM-fields">
      <xsl:param name="clause" select="'1'"/>
      <xsl:param name="phase" select="'even'"/>
      <xsl:variable name="otherPhase">
            <xsl:choose>
              <xsl:when test="$phase = 'even'">
                <xsl:text>odd</xsl:text>
              </xsl:when>
              <xsl:otherwise>
                <xsl:text>even</xsl:text>
              </xsl:otherwise>
            </xsl:choose>
      </xsl:variable>

      <xsl:choose>

            <!--  artifact?
            <tr class="ds-table-row odd">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-preview</i18n:text>:</span></td>
                <td>
                    <xsl:choose>
                        <xsl:when test="mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']">
                            <a class="image-link">
                                <xsl:attribute name="href"><xsl:value-of select="@OBJID"/></xsl:attribute>
                                <img alt="Thumbnail">
                                    <xsl:attribute name="src">
                                        <xsl:value-of select="mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/
                                            mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                                    </xsl:attribute>
                                </img>
                            </a>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-preview</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </td>
            </tr>-->
            
          <!-- Title row -->
          <xsl:when test="$clause = 1">
            <tr class="ds-table-row {$phase}">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-title</i18n:text>: </span></td>
                <td>
                    <span class="Z3988">
                        <!--
                        <xsl:attribute name="title">
                            <xsl:call-template name="renderCOinS"/>
                        </xsl:attribute>
                        -->
                        <xsl:choose>
                            <xsl:when test="count(dim:field[@element='title'][not(@qualifier)]) &gt; 1">
                                <xsl:for-each select="dim:field[@element='title'][not(@qualifier)]">
                            	   <xsl:value-of select="./node()"/>
                            	   <xsl:if test="count(following-sibling::dim:field[@element='title'][not(@qualifier)]) != 0">
	                                    <xsl:text>; </xsl:text><br/>
	                                </xsl:if>
                                </xsl:for-each>
                            </xsl:when>
                            <xsl:when test="count(dim:field[@element='title'][not(@qualifier)]) = 1">
                                <xsl:value-of select="dim:field[@element='title'][not(@qualifier)][1]/node()"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                            </xsl:otherwise>
                        </xsl:choose>
                    </span>
                </td>
            </tr>
            <xsl:call-template name="itemSummaryView-DIM-fields">
              <xsl:with-param name="clause" select="($clause + 1)"/>
              <xsl:with-param name="phase" select="$otherPhase"/>
            </xsl:call-template>
          </xsl:when>

          <!-- Author(s) row -->
          <xsl:when test="$clause = 2 and (dim:field[@element='contributor'][@qualifier='author'] or dim:field[@element='creator'] or dim:field[@element='contributor'])">
                    <tr class="ds-table-row {$phase}">
	                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-author</i18n:text>:</span></td>
	                <td>
	                    <xsl:choose>
	                        <xsl:when test="dim:field[@element='contributor'][@qualifier='author']">
	                            <xsl:for-each select="dim:field[@element='contributor'][@qualifier='author']">
                                        <span>
                                          <xsl:if test="@authority">
                                            <xsl:attribute name="class"><xsl:text>ds-dc_contributor_author-authority</xsl:text></xsl:attribute>
                                          </xsl:if>
	                                <xsl:copy-of select="node()"/>
                                        </span>
	                                <xsl:if test="count(following-sibling::dim:field[@element='contributor'][@qualifier='author']) != 0">
	                                    <xsl:text>; </xsl:text>
	                                </xsl:if>
	                            </xsl:for-each>
	                        </xsl:when>
	                        <xsl:when test="dim:field[@element='creator']">
	                            <xsl:for-each select="dim:field[@element='creator']">
	                                <xsl:copy-of select="node()"/>
	                                <xsl:if test="count(following-sibling::dim:field[@element='creator']) != 0">
	                                    <xsl:text>; </xsl:text>
	                                </xsl:if>
	                            </xsl:for-each>
	                        </xsl:when>
	                        <xsl:when test="dim:field[@element='contributor']">
	                            <xsl:for-each select="dim:field[@element='contributor']">
	                                <xsl:copy-of select="node()"/>
	                                <xsl:if test="count(following-sibling::dim:field[@element='contributor']) != 0">
	                                    <xsl:text>; </xsl:text>
	                                </xsl:if>
	                            </xsl:for-each>
	                        </xsl:when>
	                        <xsl:otherwise>
	                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
	                        </xsl:otherwise>
	                    </xsl:choose>
	                </td>
	            </tr>
              <xsl:call-template name="itemSummaryView-DIM-fields">
                <xsl:with-param name="clause" select="($clause + 1)"/>
                <xsl:with-param name="phase" select="$otherPhase"/>
              </xsl:call-template>
          </xsl:when>

          <!-- Abstract row -->
          <xsl:when test="$clause = 3 and (dim:field[@element='description' and @qualifier='abstract'])">
                    <tr class="ds-table-row {$phase}">
	                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-abstract</i18n:text>:</span></td>
	                <td>
	                <xsl:if test="count(dim:field[@element='description' and @qualifier='abstract']) &gt; 1">
	                	<hr class="metadata-seperator"/>
	                </xsl:if>
	                <xsl:for-each select="dim:field[@element='description' and @qualifier='abstract']">
		                <xsl:copy-of select="./node()"/>
		                <xsl:if test="count(following-sibling::dim:field[@element='description' and @qualifier='abstract']) != 0">
	                    	<hr class="metadata-seperator"/>
	                    </xsl:if>
	              	</xsl:for-each>
	              	<xsl:if test="count(dim:field[@element='description' and @qualifier='abstract']) &gt; 1">
	                	<hr class="metadata-seperator"/>
	                </xsl:if>
	                </td>
	            </tr>
              <xsl:call-template name="itemSummaryView-DIM-fields">
                <xsl:with-param name="clause" select="($clause + 1)"/>
                <xsl:with-param name="phase" select="$otherPhase"/>
              </xsl:call-template>
          </xsl:when>

          <!-- Description row -->
          <xsl:when test="$clause = 4 and (dim:field[@element='description' and not(@qualifier)])">
                    <tr class="ds-table-row {$phase}">
	                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-description</i18n:text>:</span></td>
	                <td>
	                <xsl:if test="count(dim:field[@element='description' and not(@qualifier)]) &gt; 1 and not(count(dim:field[@element='description' and @qualifier='abstract']) &gt; 1)">
	                	<hr class="metadata-seperator"/>
	                </xsl:if>
	                <xsl:for-each select="dim:field[@element='description' and not(@qualifier)]">
		                <xsl:copy-of select="./node()"/>
		                <xsl:if test="count(following-sibling::dim:field[@element='description' and not(@qualifier)]) != 0">
	                    	<hr class="metadata-seperator"/>
	                    </xsl:if>
	               	</xsl:for-each>
	               	<xsl:if test="count(dim:field[@element='description' and not(@qualifier)]) &gt; 1">
	                	<hr class="metadata-seperator"/>
	                </xsl:if>
	                </td>
	            </tr>
              <xsl:call-template name="itemSummaryView-DIM-fields">
                <xsl:with-param name="clause" select="($clause + 1)"/>
                <xsl:with-param name="phase" select="$otherPhase"/>
              </xsl:call-template>
          </xsl:when>

          <!-- identifier.uri row -->
          <xsl:when test="$clause = 5 and (dim:field[@element='identifier' and @qualifier='uri'])">
                    <tr class="ds-table-row {$phase}">
	                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-uri</i18n:text>:</span></td>
	                <td>
	                	<xsl:for-each select="dim:field[@element='identifier' and @qualifier='uri']">
		                    <a>
		                        <xsl:attribute name="href">
		                            <xsl:copy-of select="./node()"/>
		                        </xsl:attribute>
		                        <xsl:copy-of select="./node()"/>
		                    </a>
		                    <xsl:if test="count(following-sibling::dim:field[@element='identifier' and @qualifier='uri']) != 0">
		                    	<br/>
		                    </xsl:if>
	                    </xsl:for-each>
	                </td>
	            </tr>
              <xsl:call-template name="itemSummaryView-DIM-fields">
                <xsl:with-param name="clause" select="($clause + 1)"/>
                <xsl:with-param name="phase" select="$otherPhase"/>
              </xsl:call-template>
          </xsl:when>

          <!-- date.issued row -->
          <xsl:when test="$clause = 6 and (dim:field[@element='date' and @qualifier='issued'])">
                    <tr class="ds-table-row {$phase}">
	                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-date</i18n:text>:</span></td>
	                <td>
		                <xsl:for-each select="dim:field[@element='date' and @qualifier='issued']">
		                	<xsl:copy-of select="substring(./node(),1,10)"/>
		                	 <xsl:if test="count(following-sibling::dim:field[@element='date' and @qualifier='issued']) != 0">
	                    	<br/>
	                    </xsl:if>
		                </xsl:for-each>
	                </td>
	            </tr>
              <xsl:call-template name="itemSummaryView-DIM-fields">
                <xsl:with-param name="clause" select="($clause + 1)"/>
                <xsl:with-param name="phase" select="$otherPhase"/>
              </xsl:call-template>
          </xsl:when>

          <!-- recurse without changing phase if we didn't output anything -->
          <xsl:otherwise>
            <!-- IMPORTANT: This test should be updated if clauses are added! -->
            <xsl:if test="$clause &lt; 7">
              <xsl:call-template name="itemSummaryView-DIM-fields">
                <xsl:with-param name="clause" select="($clause + 1)"/>
                <xsl:with-param name="phase" select="$phase"/>
              </xsl:call-template>
            </xsl:if>
          </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    
    <!-- The summaryView of communities and collections is undefined. -->
    <xsl:template name="collectionSummaryView-DIM">
        <i18n:text>xmlui.dri2xhtml.METS-1.0.collection-not-implemented</i18n:text>
    </xsl:template>
    
    <xsl:template name="communitySummaryView-DIM">
        <i18n:text>xmlui.dri2xhtml.METS-1.0.community-not-implemented</i18n:text>
    </xsl:template>
    
    
    
    
    
    
    
    
    
    <!-- 
        The detailView display type; used to generate a complete view of the object involved. It is currently
        used with the "full item record" view of items as well as the default views of communities and collections. 
    -->
    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="detailView">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemDetailView-DIM"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Collection'">
                <xsl:call-template name="collectionDetailView-DIM"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Community'">
                <xsl:call-template name="communityDetailView-DIM"/>
            </xsl:when>                
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    
    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="detailViewImage">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemDetailViewImage-DIM"/>
            </xsl:when>
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="detailViewBook">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemDetailViewBook-DIM"/>
            </xsl:when>
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="headDetailView">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemHeadDetailView-DIM"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Collection'">
                <xsl:call-template name="collectionHeadDetailView-DIM"/>
            </xsl:when>
            <!--
            <xsl:when test="@LABEL='DSpace Community'">
                <xsl:call-template name="communityDetailView-DIM"/>
            </xsl:when>
            -->
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="headDetailViewBook">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemHeadDetailView-DIM"/>
                <xsl:call-template name="itemToolbarBook"/>
            </xsl:when>
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="headDetailViewImage">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemHeadDetailView-DIM"/>
                <xsl:call-template name="itemToolbarImage"/>
            </xsl:when>
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>




    <!-- An item rendered in the detailView pattern, the "full item record" view of a DSpace item in Manakin. -->
    <xsl:template name="itemDetailView-DIM">
        <!-- Output all of the metadata about the item from the metadata section -->
<!--        <xsl:apply-templates select="mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim" mode="itemDetailView-DIM"/>-->
		<!-- Generate the bitstream information from the file section -->
        <xsl:choose>
            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">
                <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']/mets:file[1]">
                    <xsl:with-param name="context" select="."/>
                    <xsl:with-param name="primaryBitstream" select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
                </xsl:apply-templates>
            </xsl:when>
            <xsl:otherwise>
                <h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h2> 
            </xsl:otherwise>
        </xsl:choose>
        <!-- Generate the Creative Commons license information from the file section (DSpace deposit license hidden by default) -->
<!--        <xsl:apply-templates select="mets:fileSec/mets:fileGrp[@USE='CC-LICENSE']"/>-->
    </xsl:template>


    <xsl:template name="itemDetailViewImage-DIM">
        <!-- Output all of the metadata about the item from the metadata section -->
<!--        <xsl:apply-templates select="mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim" mode="itemDetailView-DIM"/>-->
		<!-- Generate the bitstream information from the file section -->
        <xsl:choose>
            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">
                <xsl:choose>
                    <xsl:when test="./mets:structMap/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID">
                        <xsl:variable name="primaryBitstream" select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>

                        <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']/mets:file[@ID=$primaryBitstream]" mode="image-viewer">
                            <xsl:with-param name="context" select="."/>
                        </xsl:apply-templates>
                    </xsl:when>

                    <xsl:otherwise>
                <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']/mets:file[1]" mode="image-viewer">
                    <xsl:with-param name="context" select="."/>
<!--                    <xsl:with-param name="primaryBitstream" select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>-->
                </xsl:apply-templates>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h2>
            </xsl:otherwise>
        </xsl:choose>
        <!-- Generate the Creative Commons license information from the file section (DSpace deposit license hidden by default) -->
<!--        <xsl:apply-templates select="mets:fileSec/mets:fileGrp[@USE='CC-LICENSE']"/>-->
    </xsl:template>

    
    <xsl:template name="itemDetailViewBook-DIM">
        <!-- Output all of the metadata about the item from the metadata section -->
<!--        <xsl:apply-templates select="mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim" mode="itemDetailView-DIM"/>-->

		<!-- Generate the bitstream information from the file section -->
        <xsl:choose>
            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">
                <xsl:choose>
                    <xsl:when test="./mets:structMap/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID">
                        <xsl:variable name="primaryBitstream" select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>

                        <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']/mets:file[@ID=$primaryBitstream]" mode="book-viewer">
                            <xsl:with-param name="context" select="."/>
                        </xsl:apply-templates>
                    </xsl:when>

                    <xsl:otherwise>
                <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']/mets:file[1]" mode="book-viewer">
                    <xsl:with-param name="context" select="."/>
<!--                    <xsl:with-param name="primaryBitstream" select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>-->
                </xsl:apply-templates>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>

            <xsl:otherwise>
                <h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h2>
            </xsl:otherwise>
        </xsl:choose>
        <!-- Generate the Creative Commons license information from the file section (DSpace deposit license hidden by default) -->
<!--        <xsl:apply-templates select="mets:fileSec/mets:fileGrp[@USE='CC-LICENSE']"/>-->
    </xsl:template>


    <xsl:template name="itemHeadDetailView-DIM">

        <div id="dados-item">
            <h3>
                <span id="nome-item">
                    <xsl:value-of select="//dim:field[@element='title' and not(@qualifier)]"/>
                    <xsl:text>.</xsl:text>
                </span>
                <span id="autoria-item">
                    <xsl:if test="//dim:field[(@element='contributor' and @qualifier='author') or @element='creator' or @element='contributor']">
                        <xsl:value-of select="//dim:field[(@element='contributor' and @qualifier='author') or @element='creator' or @element='contributor']"/>
                        <xsl:text>.</xsl:text>
                    </xsl:if>
                </span>
                <span id="mais-metadados" onclick="alternarMaisMetadados();">
                    <img src="{$images-path}/mais_filtro.png"/>
                </span>
            </h3>
        </div>


        <div class="caixa-borda3 fichas" id="ficha-meta">
            <img class="icone_peq fechar" id="fechar-meta" />
<!--             <img class='icone_peq fechar' id='fechar-link'/>-->
            <xsl:apply-templates select="mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim" mode="itemMetadata"/>
<!--            <xsl:call-template name="ficha-metadados"/>-->
        </div>


        <div id="colecao-item">
            <xsl:variable name="type">
<!--                            <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='type']"/>-->
                <xsl:value-of select="//dim:dim/dim:field[@element='type' and not(@qualifier)]"/>
            </xsl:variable>
					<!-- estruturei de forma semelhante ao tipo-documento do resultado de busca -->
            <span id="nome-colecao">
                <xsl:value-of select="$type"/>
<!--                        <img src="{$images-path}/livro.png"/>-->
            </span>
            <xsl:call-template name="getDocumentTypeThumbnail">
                <xsl:with-param name="document_type_name" select="$type"/>
            </xsl:call-template>
        </div>


        <xsl:if test="//dim:field[@element='description' and @qualifier='tableofcontents'][contains(text(), '(Gravura')] or //dim:field[@element='relation' and @qualifier='ispartof']">
        <div id="caixas">
            <xsl:comment>Info</xsl:comment>
                    <!-- tirei classe modos, troquei span por div, adicionei tags span e h4 e adicionei img -->
<!--                    <div class="sumario"><span class="cor2"><a>sumário <img src="{$images-path}/mais2.png"/></a></span></div>-->
<!--                    <div class="pesquisa-interna"><span class="cor2"><a>pesquisa neste item <img src="{$images-path}/mais2.png"/></a></span></div>-->

<!--            <xsl:if test="contains(string(//dim:field[@element='description' and @qualifier='tableofcontents']), '(Gravura')">-->
            <xsl:if test="//dim:field[@element='description' and @qualifier='tableofcontents'][contains(text(), '(Gravura')]">
                <div class="imagens-item">
                    <span class="cor2" id="mais-imagens">
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.item-description-tableofcontents.pictures</i18n:text><img src="{$images-path}/mais2.png"/>
                    </span>
                    <div class="caixa-borda3 fichas" id="ficha-imagens">
                        <img src="{$images-path}/fechar.png" class="fechar" id="fechar-imagens" />
                        <xsl:call-template name="dimSpecialField-DcDescriptionTableofcontents-pictures">
                            <xsl:with-param name="field" select="//dim:field[@element='description' and @qualifier='tableofcontents'][contains(text(), '(Gravura')]"/>
                        </xsl:call-template>
                    </div>
                </div>
            </xsl:if>
            
            <xsl:if test="//dim:field[@element='relation' and @qualifier='ispartof']">
                <div class="info-visualizador">
                    <span class="barra-texto">
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.item-relation-ispartof.long</i18n:text>
                    </span>
                    <span class="info-item cor1">
                        <a>
                            <xsl:variable name="handle">
                                <xsl:choose>
                                    <xsl:when test="contains(string(//dim:field[@element='relation' and @qualifier='ispartof']), 'handle.net/')">
                                        <xsl:value-of select="substring-after(//dim:field[@element='relation' and @qualifier='ispartof'], 'handle.net/')"/>
                                    </xsl:when>
                                    <xsl:when test="fn:matches(string(//dim:field[@element='relation' and @qualifier='ispartof']), '[0-9]{4}/[0-9-]+')">
                                        <xsl:value-of select="//dim:field[@element='relation' and @qualifier='ispartof']"/>
                                    </xsl:when>
                                    <xsl:otherwise>

                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:variable>
                            <xsl:attribute name="href">
                                <xsl:choose>
                                    <xsl:when test="$handle != ''">
                                        <xsl:value-of select="$context-path"/>
                                        <xsl:text>/handle/</xsl:text>
                                        <xsl:value-of select="$handle"/>                                        
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="$context-path"/>
                                        <xsl:text>/search?query=</xsl:text>
                                        <xsl:copy-of select="//dim:field[@element='relation' and @qualifier='ispartof']/node()"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:attribute>

                            <xsl:variable name="title">
                                <xsl:call-template name="getItemTitleFromHandle">
                                    <xsl:with-param name="handle" select="$handle"/>
                                </xsl:call-template>
                            </xsl:variable>
                            <xsl:choose>
                                <xsl:when test="$title != ''">
                                    <xsl:value-of select="$title"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:copy-of select="//dim:field[@element='relation' and @qualifier='ispartof']/node()"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </a>
<!--                        <xsl:if test="count(following-sibling::dim:field[@element='relation' and @qualifier='ispartof']) != 0">
                            <br/>
                        </xsl:if>-->
                    </span>
                </div>
            </xsl:if>
        </div>
        </xsl:if>
    </xsl:template>


    <xsl:template name="itemToolbarImage">
        <div id='visualizador-barra' class='caixa'>
            <div class='modos_visualizador'>&#160;</div>
            <div class='zoom'>&#160;</div>
            <div class='paginacao'>&#160;</div>
            <div class='resultados-pagina'>&#160;</div>

            <xsl:variable name="primaryBitstream" select="//mets:structMap/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
            <xsl:variable name="fileid">
                <xsl:choose>
                    <xsl:when test="$primaryBitstream != ''">
                        <xsl:value-of select="$primaryBitstream"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="//mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']/mets:file[1]/@ID"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>

            <div class="funcional">
                <span class="imprimir">
<!--                    <a class="icone_barra print rollover" onclick='printElement("#visualizador-imagem #target");'>-->
                    <a class="icone_barra print rollover">
<!--                    <a class="icone_barra print rollover">-->
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.print</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>

                        <xsl:variable name="action">
                            <xsl:value-of select="//mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']/mets:file[@ID=$fileid]/@MIMETYPE"/>
                        </xsl:variable>

                        <xsl:variable name="event">
                            <xsl:text>_gaq.push(['_trackEvent',</xsl:text>
                            <xsl:text>'Item prints','</xsl:text>
                            <xsl:value-of select="$action"/>
                            <xsl:text>','</xsl:text>
                            <xsl:value-of select="//dim:dim/dim:field[@element='title'][1]"/>
                            <xsl:text>']);</xsl:text>
                        </xsl:variable>
                    
                        <xsl:attribute name="onClick">
                            <xsl:value-of select="$event"/>
                            <xsl:value-of select="'printURL(iip.getCompleteImageOpenURL()); return false;'"/>
                        </xsl:attribute>
                        &#160;
                    </a>
                </span>
                <span class="baixar">
                    <a class="icone_barra rollover download">
                        <xsl:attribute name="href">
                            <xsl:value-of select="substring-before(//mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']/mets:file[@ID=$fileid]/mets:FLocat[@LOCTYPE='URL']/@xlink:href, '?')"/>
                        </xsl:attribute>

                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.download</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>

                        <xsl:variable name="action">
                            <xsl:value-of select="//mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']/mets:file[@ID=$fileid]/@MIMETYPE"/>
                        </xsl:variable>

                        <xsl:variable name="event">
                            <xsl:text>_gaq.push(['_trackEvent',</xsl:text>
                            <xsl:text>'Item downloads','</xsl:text>
                            <xsl:value-of select="$action"/>
                            <xsl:text>','</xsl:text>
                            <xsl:value-of select="//dim:dim/dim:field[@element='title'][1]"/>
                            <xsl:text>']);</xsl:text>
                        </xsl:variable>
                    
                        <xsl:attribute name="onClick">
                            <xsl:value-of select="$event"/>
                        </xsl:attribute>
                        &#160;
                    </a>
                </span>
                <span class="citar">
                    <a class="icone_barra rollover cite">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.cite</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </a>
                </span>
                <span class="linkar">
                    <a class="icone_barra rollover link linkar" onclick="alternarFichaLink();">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.permalink</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </a>
                </span>
                <div class="caixa-borda3 fichas" id="ficha-link">
                    <img class="icone_peq fechar" id="fechar-link" onclick="$('#ficha-link').fadeOut(500)"/>
                    <p>
                        <span class="cor1 label"><i18n:text>xmlui.Discovery.ItemViewer.toolbar.permalink_head</i18n:text></span>
                    </p>
                    <p>
                        <span>
                            <xsl:attribute name="href">
                                <xsl:value-of select="concat($server-url, /mets:METS/@OBJID)"/>
                            </xsl:attribute>
                            <xsl:value-of select="concat($server-url, /mets:METS/@OBJID)"/>
                        </span>
                    </p>
                </div>
            </div>

        </div>
    </xsl:template>


    <xsl:template name="itemToolbarBook">
        <div id="visualizador-barra" class="caixa">
            <div class="modos_visualizador">
                <span class="barra-texto label"><i18n:text>xmlui.Discovery.ItemViewer.toolbar.view_modes</i18n:text></span>
                <span class="pagina-unica">
                    <button class="icone_barra rollover one_page_mode" onclick="br.switchMode(1); return false;">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.one_page_mode</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </button>
                </span>
                <span class="mosaico">
                    <button class="icone_barra rollover thumbnail_mode" onclick="br.switchMode(3); return false;">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.thumbnail_mode</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </button>
                </span>
                <span class="pagina-dupla">
                    <button class="icone_barra rollover two_page_mode" onclick="br.switchMode(2); return false;">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.two_page_mode</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </button>
                </span>
                <span class="ocr">
                    <button class="icone_barra ocr_mode" onclick="br.switchMode(4); return false;">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.ocr_mode</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </button>
                </span>
            </div>

            <div class="zoom">
                <span>
                    <button class="icone_barra rollover zoom_out" onclick="br.zoom(-1); return false;">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.zoom_out</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </button>
                </span>
                <span>
                    <button class="icone_barra rollover zoom_in" onclick="br.zoom(1); return false;">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.zoom_in</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </button>
                </span>
            </div>

            <div class="paginacao">
                <span class="barra-texto label"><i18n:text>xmlui.Discovery.ItemViewer.toolbar.navigation</i18n:text></span>
                <div class="BRtoolbarmode2" style="display: none">
                    <button class="icone_barra rollover book_leftmost">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.book_leftmost</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </button>
                    <button class="icone_barra rollover book_left">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.book_left</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </button>
                    <button class="icone_barra rollover book_right">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.book_right</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </button>
                    <button class="icone_barra rollover book_rightmost">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.book_rightmost</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </button>
                </div>
                <div class="BRtoolbarmode1" style="display: none">
                    <button class="icone_barra rollover book_top">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.book_top</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </button>
                    <button class="icone_barra rollover book_up">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.book_up</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </button>
                    <button class="icone_barra rollover book_down">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.book_down</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </button>
                    <button class="icone_barra rollover book_bottom">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.book_bottom</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </button>
                </div>
                <div class="BRtoolbarmode3" style="display: none">
                    <button class="icone_barra rollover book_top">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.book_top</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </button>
                    <button class="icone_barra rollover book_up">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.book_up</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </button>
                    <button class="icone_barra rollover book_down">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.book_down</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </button>
                    <button class="icone_barra rollover book_bottom">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.book_bottom</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </button>
                </div>
            </div>

            <div class="resultados-pagina">
                <form class="BRpageform" action="javascript:" onsubmit="br.jumpToPage(this.elements[1].value);">
                    <span class="barra-texto label"><i18n:text>xmlui.Discovery.ItemViewer.toolbar.jump_to_page</i18n:text></span>
                    <input id="BRpagenum" type="text" size="3" onfocus="br.autoStop();" onblur="br.jumpToPage(this.value);" onkeydown="e = event || window.event; var unicode = e.charCode ? e.charCode : e.keyCode ? e.keyCode : 0; if (unicode == 13) {{ br.jumpToPage(this.value); return false; }} else return true;"></input>
                </form>
            </div>

            <xsl:variable name="primaryBitstream" select="//mets:structMap/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
            <xsl:variable name="fileid">
                <xsl:choose>
                    <xsl:when test="$primaryBitstream != ''">
                        <xsl:value-of select="$primaryBitstream"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="//mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']/mets:file[1]/@ID"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>

            <div class="funcional">
                <span class="imprimir">
                    <a class="icone_barra print rollover">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.print</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>

                        <xsl:variable name="action">
                            <xsl:value-of select="//mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']/mets:file[@ID=$fileid]/@MIMETYPE"/>
                        </xsl:variable>

                        <xsl:variable name="event">
                            <xsl:text>_gaq.push(['_trackEvent',</xsl:text>
                            <xsl:text>'Item prints','</xsl:text>
                            <xsl:value-of select="$action"/>
                            <xsl:text>','</xsl:text>
                            <xsl:value-of select="//dim:dim/dim:field[@element='title'][1]"/>
                            <xsl:text>']);</xsl:text>
                        </xsl:variable>
                    
                        <xsl:attribute name="onClick">
                            <xsl:value-of select="$event"/>
                            <xsl:value-of select="'; printURL(br.getPrintURI()); return false;'"/>
                        </xsl:attribute>
                        &#160;
                    </a>
                </span>
                <span class="baixar">
                    <a class="icone_barra rollover download">
                        <xsl:attribute name="href">
                            <xsl:value-of select="substring-before(//mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']/mets:file[@ID=$fileid]/mets:FLocat[@LOCTYPE='URL']/@xlink:href, '?')"/>
                        </xsl:attribute>

                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.download</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>

                        <xsl:variable name="action">
                            <xsl:value-of select="//mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']/mets:file[@ID=$fileid]/@MIMETYPE"/>
                        </xsl:variable>

                        <xsl:variable name="event">
                            <xsl:text>_gaq.push(['_trackEvent',</xsl:text>
                            <xsl:text>'Item downloads','</xsl:text>
                            <xsl:value-of select="$action"/>
                            <xsl:text>','</xsl:text>
                            <xsl:value-of select="//dim:dim/dim:field[@element='title'][1]"/>
                            <xsl:text>']);</xsl:text>
                        </xsl:variable>
                    
                        <xsl:attribute name="onClick">
                            <xsl:value-of select="$event"/>
                        </xsl:attribute>
                        &#160;
                    </a>
                </span>
                <span class="citar">
                    <a class="icone_barra rollover cite">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.cite</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </a>
                </span>
                <span class="linkar">
                    <a class="icone_barra rollover link linkar" onclick="alternarFichaLink();">
                        <xsl:attribute name="title">
                            <xsl:text>xmlui.Discovery.ItemViewer.toolbar.permalink</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="i18n:attr">
                            <xsl:text>title</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </a>
                </span>
                <div class="caixa-borda3 fichas" id="ficha-link">
                    <img class="icone_peq fechar" id="fechar-link" onclick="$('#ficha-link').fadeOut(500)"/>
                    <p>
                        <span class="cor1 label"><i18n:text>xmlui.Discovery.ItemViewer.toolbar.permalink_head</i18n:text></span>
                    </p>
                    <p>
                        <span>
                            <xsl:attribute name="href">
                                <xsl:value-of select="concat($server-url, /mets:METS/@OBJID)"/>
                            </xsl:attribute>
                            <xsl:value-of select="concat($server-url, /mets:METS/@OBJID)"/>
                        </span>
                    </p>
                </div>
            </div>
        </div>
    </xsl:template>


    <!-- The block of templates used to render the complete DIM contents of a DRI object -->
    <xsl:template match="dim:dim" mode="itemDetailView-DIM">
        <!--
        <span class="Z3988">
            <xsl:attribute name="title">
                 <xsl:call-template name="renderCOinS"/>
            </xsl:attribute>

        </span>                
        -->
		<table class="ds-includeSet-table">
		    <xsl:apply-templates mode="itemDetailView-DIM"/>
		</table>
    </xsl:template>
            
    <xsl:template match="dim:field" mode="itemDetailView-DIM">
        <xsl:if test="not(@element='description' and @qualifier='provenance')">
            <tr>
                <xsl:attribute name="class">
                    <xsl:text>ds-table-row </xsl:text>
                    <xsl:if test="(position() div 2 mod 2 = 0)">even </xsl:if>
                    <xsl:if test="(position() div 2 mod 2 = 1)">odd </xsl:if>
                </xsl:attribute>
                <td>
                    <xsl:value-of select="./@mdschema"/>
                    <xsl:text>.</xsl:text>
                    <xsl:value-of select="./@element"/>
                    <xsl:if test="./@qualifier">
                        <xsl:text>.</xsl:text>
                        <xsl:value-of select="./@qualifier"/>
                    </xsl:if>
                </td>
            <td>
              <xsl:copy-of select="./node()"/>
              <xsl:if test="./@authority and ./@confidence">
                <xsl:call-template name="authorityConfidenceIcon">
                  <xsl:with-param name="confidence" select="./@confidence"/>
                </xsl:call-template>
              </xsl:if>
            </td>
                <td><xsl:value-of select="./@language"/></td>
            </tr>
        </xsl:if>
    </xsl:template>

	
	
	
    <!-- A collection rendered in the detailView pattern; default way of viewing a collection. -->
    <xsl:template name="collectionDetailView-DIM">
        <div class="detail-view">&#160;
            <!-- Generate the logo, if present, from the file section -->
            <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='LOGO']"/>
            <!-- Generate the info about the collections from the metadata section -->
            <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
                mode="collectionDetailView-DIM"/>
        </div>
    </xsl:template>
    
    <xsl:template name="collectionHeadDetailView-DIM">
        <div id="dados-item">
            <h3>
                <span id="nome-item">
                    <xsl:value-of select="//dim:field[@element='title' and not(@qualifier)]"/>
                    <xsl:text>.</xsl:text>
                </span>
                <span id="mais-metadados" onclick="alternarMaisMetadados();">
                    <img src="{$images-path}/mais_filtro.png"/>
                </span>
            </h3>
        </div>


        <div class="caixa-borda3 fichas" id="ficha-meta">
            <img class="icone_peq fechar" id="fechar-meta" />
<!--             <img class='icone_peq fechar' id='fechar-link'/>-->
            <xsl:apply-templates select="mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim" mode="communityCollectionMetadata"/>
<!--            <xsl:call-template name="ficha-metadados"/>-->
        </div>


        <div id="colecao-item">
            <xsl:variable name="type">
<!--                            <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='type']"/>-->
                <xsl:value-of select="//dim:dim/dim:field[@element='type' and not(@qualifier)]"/>
            </xsl:variable>
                    <!-- estruturei de forma semelhante ao tipo-documento do resultado de busca -->
            <span id="nome-colecao">
                <xsl:value-of select="$type"/>
<!--                        <img src="{$images-path}/livro.png"/>-->
            </span>
            <xsl:call-template name="getDocumentTypeThumbnail">
                <xsl:with-param name="document_type_name" select="$type"/>
            </xsl:call-template>
        </div>


        <xsl:if test="//dim:field[@element='description' and @qualifier='tableofcontents'][contains(text(), '(Gravura')] or //dim:field[@element='relation' and @qualifier='ispartof']">
        <div id="caixas">&#160;
                    <!-- tirei classe modos, troquei span por div, adicionei tags span e h4 e adicionei img -->
<!--                    <div class="sumario"><span class="cor2"><a>sumário <img src="{$images-path}/mais2.png"/></a></span></div>-->
<!--                    <div class="pesquisa-interna"><span class="cor2"><a>pesquisa neste item <img src="{$images-path}/mais2.png"/></a></span></div>-->

<!--            <xsl:if test="contains(string(//dim:field[@element='description' and @qualifier='tableofcontents']), '(Gravura')">-->
            <xsl:if test="//dim:field[@element='description' and @qualifier='tableofcontents'][contains(text(), '(Gravura')]">
                <div class="imagens-item">
                    <span class="cor2" id="mais-imagens">
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.item-description-tableofcontents.pictures</i18n:text><img src="{$images-path}/mais2.png"/>
                    </span>
                    <div class="caixa-borda3 fichas" id="ficha-imagens">
                        <img src="{$images-path}/fechar.png" class="fechar" id="fechar-imagens" />
                        <xsl:call-template name="dimSpecialField-DcDescriptionTableofcontents-pictures">
                            <xsl:with-param name="field" select="//dim:field[@element='description' and @qualifier='tableofcontents'][contains(text(), '(Gravura')]"/>
                        </xsl:call-template>
                    </div>
                </div>
            </xsl:if>
            
            <xsl:if test="//dim:field[@element='relation' and @qualifier='ispartof']">
                <div class="info-visualizador">
                    <span class="barra-texto">
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.item-relation-ispartof.long</i18n:text>
                    </span>
                    <span class="info-item cor1">
                        <a>
                            <xsl:attribute name="href">
                                <xsl:choose>
                                    <xsl:when test="fn:matches(string(//dim:field[@element='relation' and @qualifier='ispartof']), '[0-9]{4}/[0-9-]+')">
                                        <xsl:value-of select="$context-path"/>
                                        <xsl:text>/handle/</xsl:text>
                                        <xsl:value-of select="//dim:field[@element='relation' and @qualifier='ispartof']"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="$context-path"/>
                                        <xsl:text>/search?fq=dc.title_t:</xsl:text>
                                        <xsl:copy-of select="//dim:field[@element='relation' and @qualifier='ispartof']/node()"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:attribute>

                            <xsl:variable name="title">
                                <xsl:call-template name="getItemTitleFromHandle">
                                    <xsl:with-param name="handle" select="//dim:field[@element='relation' and @qualifier='ispartof']"/>
                                </xsl:call-template>
                            </xsl:variable>
                            <xsl:choose>
                                <xsl:when test="$title != ''">
                                    <xsl:value-of select="$title"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:copy-of select="//dim:field[@element='relation' and @qualifier='ispartof']/node()"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </a>
<!--                        <xsl:if test="count(following-sibling::dim:field[@element='relation' and @qualifier='ispartof']) != 0">
                            <br/>
                        </xsl:if>-->
                    </span>
                </div>
            </xsl:if>
        </div>
        </xsl:if>
    </xsl:template>

    
    <!-- Generate the info about the collection from the metadata section -->
    <xsl:template match="dim:dim" mode="collectionDetailView-DIM"> 
        <xsl:if test="string-length(dim:field[@element='description'][not(@qualifier)])&gt;0">
            <p class="intro-text">
                <xsl:copy-of select="dim:field[@element='description'][not(@qualifier)]/node()"/>
            </p>
        </xsl:if>
        
        <xsl:if test="string-length(dim:field[@element='description'][@qualifier='tableofcontents'])&gt;0">
        	<div class="detail-view-news">
        		<h3><i18n:text>xmlui.dri2xhtml.METS-1.0.news</i18n:text></h3>
        		<p class="news-text">
        			<xsl:copy-of select="dim:field[@element='description'][@qualifier='tableofcontents']/node()"/>
        		</p>
        	</div>
        </xsl:if>
        
        <xsl:if test="string-length(dim:field[@element='rights'][not(@qualifier)])&gt;0">
        	<div class="detail-view-rights-and-license">
		        <xsl:if test="string-length(dim:field[@element='rights'][not(@qualifier)])&gt;0">
		            <p class="copyright-text">
		                <xsl:copy-of select="dim:field[@element='rights'][not(@qualifier)]/node()"/>
		            </p>
		        </xsl:if>
        	</div>
        </xsl:if>
    </xsl:template>
    
    
	
    <!-- Rendering the file list from an Atom ReM bitstream stored in the ORE bundle -->
    <xsl:template match="mets:fileGrp[@USE='ORE']">
        <xsl:variable name="AtomMapURL" select="concat('cocoon:/',substring-after(mets:file/mets:FLocat[@LOCTYPE='URL']//@*[local-name(.)='href'],$context-path))"/>
        <h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h2>
        <table class="ds-table file-list">
            <thead>
                <tr class="ds-table-header-row">
                    <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-file</i18n:text></th>
                    <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-size</i18n:text></th>
                    <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-format</i18n:text></th>
                    <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-view</i18n:text></th>
                </tr>
            </thead>
            <tbody>
                <xsl:apply-templates select="document($AtomMapURL)/atom:entry/atom:link[@rel='http://www.openarchives.org/ore/terms/aggregates']">
                    <xsl:sort select="@title"/>
                </xsl:apply-templates>
            </tbody>
        </table>
    </xsl:template>    
	
	
    <!-- Iterate over the links in the ORE resource maps and make them into bitstream references in the file section -->
    <xsl:template match="atom:link[@rel='http://www.openarchives.org/ore/terms/aggregates']">
        <tr>
            <xsl:attribute name="class">
                <xsl:text>ds-table-row </xsl:text>
                <xsl:if test="(position() mod 2 = 0)">even </xsl:if>
                <xsl:if test="(position() mod 2 = 1)">odd </xsl:if>
            </xsl:attribute>
            <td>
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="@href"/>
                    </xsl:attribute>
                    <xsl:attribute name="title">
                        <xsl:choose>
                            <xsl:when test="@title">
                                <xsl:value-of select="@title"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="@href"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="string-length(@title) > 50">
                            <xsl:variable name="title_length" select="string-length(@title)"/>
                            <xsl:value-of select="substring(@title,1,15)"/>
                            <xsl:text> ... </xsl:text>
                            <xsl:value-of select="substring(@title,$title_length - 25,$title_length)"/>
                        </xsl:when>
                        <xsl:when test="@title">
                            <xsl:value-of select="@title"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="@href"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </a>
            </td>
            <!-- File size always comes in bytes and thus needs conversion --> 
            <td>
                <xsl:choose>
                    <xsl:when test="@length &lt; 1000">
                        <xsl:value-of select="@length"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-bytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="@length &lt; 1000000">
                        <xsl:value-of select="substring(string(@length div 1000),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-kilobytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="@length &lt; 1000000001">
                        <xsl:value-of select="substring(string(@length div 1000000),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-megabytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="@length &gt; 1000000000">
                        <xsl:value-of select="substring(string(@length div 1000000000),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-gigabytes</i18n:text>
                    </xsl:when>
                    <!-- When one isn't available -->
                    <xsl:otherwise><xsl:text>n/a</xsl:text></xsl:otherwise>
                </xsl:choose>
            </td>
            <!-- Currently format carries forward the mime type. In the original DSpace, this 
                would get resolved to an application via the Bitstream Registry, but we are
                constrained by the capabilities of METS and can't really pass that info through. -->
            <td>
                <xsl:value-of select="substring-before(@type,'/')"/>
                <xsl:text>/</xsl:text>
                <xsl:value-of select="substring-after(@type,'/')"/>
            </td>
            <td>
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="@href"/>
                    </xsl:attribute>
                    <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-viewOpen</i18n:text>
                </a>
            </td>
        </tr>
    </xsl:template>
    
    
    
    <!-- A community rendered in the detailView pattern; default way of viewing a community. -->
    <xsl:template name="communityDetailView-DIM">
        <div class="detail-view">
            <xsl:comment>communityDetailView-DIM</xsl:comment>
            <!-- Generate the logo, if present, from the file section -->
            <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='LOGO']"/>
            <!-- Generate the info about the collections from the metadata section -->
            <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
                mode="communityDetailView-DIM"/>
        </div>
    </xsl:template>
    
    <!-- Generate the info about the community from the metadata section -->
    <xsl:template match="dim:dim" mode="communityDetailView-DIM"> 
        <xsl:if test="string-length(dim:field[@element='description'][not(@qualifier)])&gt;0">
            <p class="intro-text">
                <xsl:copy-of select="dim:field[@element='description'][not(@qualifier)]/node()"/>
            </p>
        </xsl:if>
        
        <xsl:if test="string-length(dim:field[@element='description'][@qualifier='tableofcontents'])&gt;0">
        	<div class="detail-view-news">
        		<h3><i18n:text>xmlui.dri2xhtml.METS-1.0.news</i18n:text></h3>
        		<p class="news-text">
        			<xsl:copy-of select="dim:field[@element='description'][@qualifier='tableofcontents']/node()"/>
        		</p>
        	</div>
        </xsl:if>
        
        <xsl:if test="string-length(dim:field[@element='rights'][not(@qualifier)])&gt;0">
        	<div class="detail-view-rights-and-license">
	            <p class="copyright-text">
	                <xsl:copy-of select="dim:field[@element='rights'][not(@qualifier)]/node()"/>
	            </p>
            </div>
        </xsl:if>
    </xsl:template>
   
    
    
       <!--  
    *********************************************
    OpenURL COinS Rendering Template
    *********************************************
 
    COinS Example:
    
    <span class="Z3988" 
    title="ctx_ver=Z39.88-2004&amp;
    rft_val_fmt=info%3Aofi%2Ffmt%3Akev%3Amtx%3Adc&amp;
    rfr_id=info%3Asid%2Focoins.info%3Agenerator&amp;
    rft.title=Making+WordPress+Content+Available+to+Zotero&amp;
    rft.aulast=Kraus&amp;
    rft.aufirst=Kari&amp;
    rft.subject=News&amp;
    rft.source=Zotero%3A+The+Next-Generation+Research+Tool&amp;
    rft.date=2007-02-08&amp;
    rft.type=blogPost&amp;
    rft.format=text&amp;
    rft.identifier=http://www.zotero.org/blog/making-wordpress-content-available-to-zotero/&amp;
    rft.language=English"></span>

    This Code does not parse authors names, instead relying on dc.contributor to populate the
    coins
     -->

    <xsl:template name="renderCOinS">
       <xsl:text>ctx_ver=Z39.88-2004&amp;rft_val_fmt=info%3Aofi%2Ffmt%3Akev%3Amtx%3Adc&amp;</xsl:text>
       <xsl:for-each select=".//dim:field[@element = 'identifier']">
            <xsl:text>rft_id=</xsl:text>
            <xsl:value-of select="encoder:encode(string(.))"/>
            <xsl:text>&amp;</xsl:text>
        </xsl:for-each>
        <xsl:text>rfr_id=info%3Asid%2Fdatadryad.org%3Arepo&amp;</xsl:text>
        <xsl:for-each select=".//dim:field[@element != 'description' and @mdschema !='dc' and @qualifier != 'provenance']">
            <xsl:value-of select="concat('rft.', @element,'=',encoder:encode(string(.))) "/>
            <xsl:if test="position()!=last()">
                <xsl:text>&amp;</xsl:text>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>
    
    
</xsl:stylesheet>

