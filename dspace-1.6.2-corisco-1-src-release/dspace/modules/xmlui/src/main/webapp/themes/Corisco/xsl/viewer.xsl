<?xml version="1.0" encoding="UTF-8"?>

<!--
  viewer.xsl
-->

<!--
    Author: Fabio N. Kepler
    Description: .
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


    <!-- Build a single row in the bitsreams table of the item view page -->
    <xsl:template match="mets:file" mode="image-viewer">
        <xsl:param name="context" select="."/>
        <xsl:variable name="fileid">
            <xsl:value-of select="@ID"/>
        </xsl:variable>

        <div id="visualizador-imagem">
            <div id="IIPMooViewer">
                <xsl:comment>IIPMooViewer</xsl:comment>
            </div>

            <script type="text/javascript">
                var server = '<xsl:value-of select="$djatoka-resolver-url"/>';
                //var images = '<xsl:value-of select="$server-url"/><xsl:value-of select="substring-before(mets:FLocat[@LOCTYPE='URL']/@xlink:href, '?')"/>';
                var images = '<xsl:value-of select="//mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']//mets:fptr[@FILEID=$fileid]/@FILEINTERNALID"/>';

                // Copyright or information message
                var credit = 'Brasiliana Digital';

                // Obtain URL Parameters if present
                var query = location.href.substring(location.href.indexOf("?")+1);
                var vars = query.split("<xsl:text disable-output-escaping="yes">&amp;</xsl:text>");
                for (var i=0;i <xsl:text disable-output-escaping="yes">&lt;</xsl:text> vars.length;i++) {
                  var pair = vars[i].split("=");
                  if (pair[0] == "url" || pair[0] == "rft_id")
                     images = pair[1];
                     if (images.indexOf("#") <xsl:text disable-output-escaping="yes">></xsl:text> 0)
                       images = images.substring(0,images.length-1);
                }

                // Create our viewer object - note: must assign this to the 'iip' variable.
                // See documentation for more details of options
                    //scale:100,
                    //credit: credit,
                iip = new IIP( "IIPMooViewer", {
                    image: images,
                    server: server,
                    zoom: 1,
                    imagesPath: '<xsl:value-of select="$djatoka-path"/>/images/',
                    render: 'random',
                    showNavButtons: true,
                    i18n: {
                        'loading': 'Carregando',
                        'toolbar tip': '* Arraste para mover<br/>* Duplo clique para mostrar/esconder botões de navegação'
                    },
                    i18n_titles: {
                        'zoomIn': 'Maior',
                        'zoomOut': 'Menor',
                        'reset': 'Melhor ajuste',
                        'snapshot': 'Salvar visualização',
                        'shiftLeft': 'Mover à esquerda',
                        'shiftUp': 'Mover para cima',
                        'shiftDown': 'Mover para baixo',
                        'shiftRight': 'Mover à direita'
                    }
                });
              </script>

        </div>

<!--  /*
   * Create the OpenURL for the current viewport
   */
  setOpenURL : function() {
    var w = this.rgn_w;
    if (this.wid < this.rgn_w)
        w = this.wid;
    var h = this.rgn_h;
    if (this.hei < this.rgn_h)
    h = this.hei;
    this.openUrl = this.server + "?url_ver=Z39.88-2004&rft_id="
        + this.images[0].src + "&svc_id=" + this.svc_id + "&svc_val_fmt="
        + this.svc_val_fmt
        + "&svc.format=image/jpeg&svc.level=" + this.res
        + "&svc.rotate=0&svc.region=" + top_left_y + ","
        + top_left_x + "," + h + "," + w;
   },-->

    </xsl:template>


    <xsl:template match="mets:file" mode="book-viewer">
        <xsl:param name="context" select="."/>
        <xsl:variable name="fileid">
            <xsl:value-of select="@ID"/>
        </xsl:variable>

        <div id="visualizador-livro">
<!--            <div id="BookReader" style="right:10px;left:10px; top: 10px; bottom:2em;">x</div>-->

            <xsl:variable name="toolbarHTML">
                <xsl:call-template name="itemToolbarBook"/>
            </xsl:variable>

            <script type="text/javascript">
                // Read the querystring parameters
                var qsParm = new Array();
                qsParm["serverURL"] = "<xsl:value-of select="$djatoka-resolver-url"/>";
                qsParm["item"] = "<xsl:value-of select="//mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']//mets:fptr[@FILEID=$fileid]/@FILEINTERNALID"/>";
                qsParm["alt"] = "Visualizador de livros";
                qsParm["title"] = "<xsl:value-of select="/mets:METS/mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@element='title' and not(@qualifier)]"/>";
                qsParm["url"] = "<xsl:value-of select="/mets:METS/@OBJID"/>";
                qsParm["libimg"] = "<xsl:value-of select="$bookreader-path"/>images/";
                qsParm["downloadURL"] = "<xsl:value-of select="substring-before(mets:FLocat[@LOCTYPE='URL']/@xlink:href, '?')"/>";
                qsParm["OCRURL"] = "<xsl:value-of select="substring-before(mets:FLocat[@LOCTYPE='URL']/@xlink:href, '?')"/>.txt";

<!--                qsParm["toolbarHTML"] = \"<xsl:copy-of select="$toolbarHTML" />\";-->
                qsParm["toolbarSelector"] = "#visualizador-barra";
                qsParm["toolbarParentSelector"] = "#conteudo-alto";

                qsParm["addToolbar"] = false;

                qsParm["toolbarTitles"] = {
<!--                                   '.logo': 'Inicial',
                                   '.zoom_in': 'Maior',
                                   '.zoom_out': 'Menor',
                                   '.one_page_mode': 'Ver uma página',
                                   '.two_page_mode': 'Ver duas páginas',
                                   '.thumbnail_mode': 'Ver mosaico',
                                   '.ocr_mode': 'Ver OCR',
                                   '.embed': 'Embed bookreader',
                                   '.print': 'Imprimir página',
                                   '.download': 'Baixar arquivo',
                                   '.cite': 'Citar documento',
                                   '.link': 'Ver link permanente para este documento',
                                   '.book_left': 'Folhear à esquerda',
                                   '.book_right': 'Folhear à direita',
                                   '.book_up': 'Página acima',
                                   '.book_down': 'Página abaixo',
                                   '.play': 'Play',
                                   '.pause': 'Pausa',
                                   '.book_top': 'Primeira página',
                                   '.book_bottom': 'Última página',

                                   '.fechar': 'Fechar'-->
                    };

                qsParm["toolbarLabels"] = {
<!--                    '.modos_visualizador .label': 'Modos de visualização:',
                    '.paginacao .label': 'Navegação:',
                    '.resultados-pagina .label': 'Ir à página:',
                    '.funcional #ficha-link .label': 'Link permanente para o documento:'-->
                    };

                var query = window.location.search.substring(1);
                var parms = query.split('<xsl:text disable-output-escaping="yes">&amp;</xsl:text>');
                for (var i = 0; i <xsl:text disable-output-escaping="yes">&lt;</xsl:text> parms.length; i++) {
                    var pos = parms[i].indexOf('=');
                    if (pos <xsl:text disable-output-escaping="yes">&gt;</xsl:text> 0) {
                        var key = parms[i].substring(0, pos);
                        var val = parms[i].substring(pos + 1);
                        qsParm[key] = val;
                    }
                }
            </script>

<!--            <div id="BookReader" style="position: relative; height: 600px; overflow: hidden;">x</div>-->
            <div id="BookReader">
                <xsl:comment>BookReader</xsl:comment>
            </div>
            <script type="text/javascript" src="{$bookreader-path}/BBReader.js">x</script>
        </div>

    </xsl:template>


</xsl:stylesheet>
