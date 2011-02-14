<?xml version="1.0" encoding="UTF-8"?>

<!--
  static.xsl
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


    <xsl:template name="cabecalho-menu">
        <div id="cabecalho-menu">
            <div id="menu-esquerdo">
		<ul>
			<li class="borda first"><a href="http://www.brasiliana.usp.br/node/503" title="Sobre a Brasiliana">SOBRE A BRASILIANA</a></li>
			<li class="borda"><a href="http://www.brasiliana.usp.br/node/613" title="Contato">CONTATO</a></li>
			<li class="borda"><a href="http://www.brasiliana.usp.br/node/507" title="Orientações e direitos">ORIENTAÇÕES E DIREITOS</a></li>
			<li class="borda"><a href="http://www4.usp.br/" title="USP">USP</a></li>
		</ul>
            </div>
            <div id="menu-direito">
                <a href="http://www.brasiliana.usp.br/dicionario">CONSULTA AOS DICIONÁRIOS</a>
            </div>
        </div>
    </xsl:template>


</xsl:stylesheet>
