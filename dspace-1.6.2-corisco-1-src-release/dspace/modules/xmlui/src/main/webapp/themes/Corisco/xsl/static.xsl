<?xml version="1.0" encoding="UTF-8"?>

<!--
  static.xsl

  Version: 1
 
  Date: 2011-02-15 09:30:00 -0200 (Tue, 15 Feb 2011)
 
  Copyright (c) 2011, Brasiliana Digital Library (http://brasiliana.usp.br).
  All rights reserved. Modified BSD License.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met:
      * Redistributions of source code must retain the above copyright
        notice, this list of conditions and the following disclaimer.
      * Redistributions in binary form must reproduce the above copyright
        notice, this list of conditions and the following disclaimer in the
        documentation and/or other materials provided with the distribution.
      * Neither the name of the <organization> nor the
        names of its contributors may be used to endorse or promote products
        derived from this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  DISCLAIMED. IN NO EVENT SHALL Brasiliana Digital Library BE LIABLE FOR ANY
  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-->

<!--
    Description: Templates to render static links.
    Author: Fabio N. Kepler
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
