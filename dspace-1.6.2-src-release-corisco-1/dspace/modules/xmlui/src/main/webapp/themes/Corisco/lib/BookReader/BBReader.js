
// Create the BookReader object
br = new BookReader();

//// Make AJAX call here to get an object containing page information
var metadata = null;
var resolverURL = '/djatoka/resolver?url_ver=Z39.88-2004';
var baseURL = 'http://' + location.host + resolverURL;
var getMetadataSVC_ID = 'info:lanl-repo/svc/getMetadata';
var imagesGetFormat = 'image/png'; //'image/jpeg';

if (qsParm["serverURL"] != "") {
    baseURL = qsParm["serverURL"] + '?url_ver=Z39.88-2004';
}

if (qsParm["item"] != "") {
    var url = baseURL + "&svc_id=" + getMetadataSVC_ID + "&rft_id=" + qsParm["item"];
    $.getJSON(url,
        function(data) {
            metadata = data;
            InitializeViewer();
        }
        );
}

function InitializeViewer() {

    // Return the width of a given page.
    br.getPageWidth = function(index) {
        try {
            var pageKey = "Page " + br.getPageNum(index);
            var size = br.pageSizes[pageKey];
            var w = size.split(" ")[0];
            return w;
        } catch (e) {
            console.log(e);
            console.log("index: " + index);
            return 400;
        }
    }
 
    // Return the height of a given page.
    br.getPageHeight = function(index) {
        try {
            var pageKey = "Page " + br.getPageNum(index);
            var size = br.pageSizes[pageKey];
            var h = size.split(" ")[1];
            return h;
        } catch (e) {
            console.log(e);
            console.log("index: " + index);
            return 600;
        }
    }
 
    // We load the images from archive.org -- you can modify this function to retrieve images
    // using a different URL structure
    br.getPageURI = function(index, reduce, rotate) {
        var url = "";

        url = baseURL +
        '&rft_id=' + metadata.identifier +
        '&svc_id=info:lanl-repo/svc/getRegion' + 
        //'&svc_val_fmt=info:ofi/fmt:kev:mtx:jpeg2000' +
        '&svc_val_fmt=info:ofi/fmt:kev:mtx:pdf' +
        '&svc.format=' + imagesGetFormat +
        '&svc.clayer=' + index +
        //'&svc.scale=' + scale.toFixed(4);
        //'&svc.level=4';
        '';
        //$("#messagebar").append('reduce: '+reduce+"; scale: "+ scale.toFixed(4)+"; ");

        var level;
        //var scale;
        // $$$ we make an assumption here that the scales are available pow2 (like kakadu)
        if (reduce < 2) {
            level = metadata.levels;
            //scale = 1;
        } else if (reduce < 4) {
            level = metadata.levels - 1;
            //scale = 2;
        } else if (reduce < 8) {
            level = metadata.levels - 2;
            //scale = 4;
        } else if (reduce < 16) {
            level = metadata.levels - 3;
            //scale = 8;
        } else  if (reduce < 32) {
            level = metadata.levels - 4;
            //scale = 16;
        } else {
            level = metadata.levels - 5;
            //scale = 32;
        }

        if ('undefined' != typeof(level)) {
            url = url + '&svc.level=' + level;
        }

        if ('undefined' != typeof(rotate) && rotate != 0) {
            url = url + '&svc.rotate=' + rotate;
        }

        return url;
    }

    br.canRotatePage = function(index) {
        return true;
    }

    // Return which side, left or right, that a given page should be displayed on
    br.getPageSide = function(index) {
        if (0 == (index & 0x1)) {
            return 'R';
        } else {
            return 'L';
        }
    }
 
    // This function returns the left and right indices for the user-visible
    // spread that contains the given index.  The return values may be
    // null if there is no facing page or the index is invalid.
    br.getSpreadIndices = function(pindex) {
        var spreadIndices = [null, null];
        if ('rl' == this.pageProgression) {
            // Right to Left
            if (this.getPageSide(pindex) == 'R') {
                spreadIndices[1] = pindex;
                spreadIndices[0] = pindex + 1;
            } else {
                // Given index was LHS
                spreadIndices[0] = pindex;
                spreadIndices[1] = pindex - 1;
            }
        } else {
            // Left to right
            if (this.getPageSide(pindex) == 'L') {
                spreadIndices[0] = pindex;
                spreadIndices[1] = pindex + 1;
            } else {
                // Given index was RHS
                spreadIndices[1] = pindex;
                spreadIndices[0] = pindex - 1;
            }
        }
 
        return spreadIndices;
    }

    br.toolbarSelector = qsParm["toolbarSelector"] || '#visualizador-barra';
    br.toolbarParentSelector = qsParm["toolbarParentSelector"] || '#conteudo-alto';

    br.addToolbar = qsParm["addToolbar"] || true;

    br.toolbarTitles = qsParm["toolbarTitles"] || { '.logo': 'Inicial',
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

                       '.fechar': 'Fechar'
        };

    br.toolbarLabels = qsParm["toolbarLabels"] || {
        '.barra-texto': 'Modos de visualização:',
        '.paginacao .label': 'Navegação:',
        '.resultados-pagina .label': 'Ir à página:',
        '.funcional #ficha-link .label': 'Link permanente para o documento:'
        };

    br.toolbarHTMLContent = function () {
        if (this.addToolbar == false) {
            return null;
        }
        var html =
            "<div id='visualizador-barra' class='caixa'>"
                + "<div class='modos_visualizador'>"
                +   " <span class='barra-texto'></span>"
                +   " <span class='pagina-unica'><button class='icone_barra rollover one_page_mode' onclick='br.switchMode(1); return false;'/></span>"
                +   " <span class='mosaico'><button class='icone_barra rollover thumbnail_mode' onclick='br.switchMode(3); return false;'/></span>"
                +   " <span class='pagina-dupla'><button class='icone_barra rollover two_page_mode' onclick='br.switchMode(2); return false;'/></span>"
                +   " <span class='ocr'><button class='icone_barra ocr_mode' onclick='br.switchMode(4); return false;'/></span>"
                + "</div>"

                + "<div class='zoom'>"
                +   " <span><button class='icone_barra rollover zoom_out' onclick='br.zoom(-1); return false;'/></span>"
                +   " <span><button class='icone_barra rollover zoom_in' onclick='br.zoom(1); return false;'/></span>"
//                +   " <span class='label'>Zoom: <span id='BRzoom'>"+parseInt(100/this.reduce)+"</span></span>"
                + "</div>"

                + "<div class='paginacao'>"
                +   "<span class='label'></span>"
                +   "<div class='BRtoolbarmode2' style='display: none'><button class='icone_barra rollover book_leftmost' /><button class='icone_barra rollover book_left' /><button class='icone_barra rollover book_right' /><button class='icone_barra rollover book_rightmost' /></div>"
                +   "<div class='BRtoolbarmode1' style='display: none'><button class='icone_barra rollover book_top' /><button class='icone_barra rollover book_up' /> <button class='icone_barra rollover book_down' /><button class='icone_barra rollover book_bottom' /></div>"
                +   "<div class='BRtoolbarmode3' style='display: none'><button class='icone_barra rollover book_top' /><button class='icone_barra rollover book_up' /> <button class='icone_barra rollover book_down' /><button class='icone_barra rollover book_bottom' /></div>"
    //            +   "<button class='BRicon rollover play' /><button class='BRicon rollover pause' style='display: none' />"
                + "</div>"

                + "<div class='resultados-pagina'>"
                +   " <form class='BRpageform' action='javascript:' onsubmit='br.jumpToPage(this.elements[0].value)'> <span class='label'></span><input id='BRpagenum' type='text' size='3' onfocus='br.autoStop();'></input></form>"
                + "</div>"

                + "<div class='funcional'>"
    //            +   "<button class='icone_barra rollover embed' />"
                +   "<span class='imprimir'><a class='icone_barra print rollover' /></span>"
                +   "<span class='baixar'><a class='icone_barra rollover download' href='" + qsParm["downloadURL"] + "'/></span>"
                +   "<span class='citar'><a class='icone_barra rollover cite' /></span>"
                +   "<span class='linkar'><a class='icone_barra rollover link linkar'  onclick='alternarFichaLink();' /></span>"
                +   "<div class='caixa-borda3 fichas' id='ficha-link'>"
                +   "   <img class='icone_peq fechar' id='fechar-link' onclick='$(\"#ficha-link\").fadeOut(500)'/>"
                +   "   <p><span class='cor1 label'>Link</span></p> <br />"
                +   "   <p><span href='" + this.bookUrl + "'>" + document.location.protocol+'//'+document.location.host+this.bookUrl + "</span></p>"
                +   "</div>"
                + "</div>"

    //            + "<span id='#BRbooktitle'>"
    //            +   "&nbsp;&nbsp;<a class='BRblack title' href='"+this.bookUrl+"' target='_blank'>"+this.bookTitle+"</a>"
    //            + "</span>"
                + "</div>";
            return html;
    };


    // For a given "accessible page index" return the page number in the book.
    //
    // For example, index 5 might correspond to "Page 1" if there is front matter such
    // as a title page and table of contents.
    br.getPageNum = function(index) {
        return index + 1;
    }
 
    // Total number of leafs (i.e., number of pages).
    br.numLeafs = metadata.compositingLayerCount;

    // Page sizes in the format:
    // "Page 1": "WWW.WW HHH.HH", "Page 2": "www.ww hhh.hh"
    br.pageSizes = metadata.instProps;
 
    // Book title and the URL used for the book title link
    br.bookTitle = qsParm["title"];
    br.bookUrl = qsParm["url"];

    // We link to index.php to avoid redirect which breaks back button
    br.logoURL = '/'; //http://www.archive.org/index.php';

    // Base URL for images
    br.imagesBaseURL = qsParm["libimg"];

    // URL for book's OCR
    br.OCRURL = qsParm["OCRURL"];

    //br.toolbarHTML = qsParm["toolbarHTML"];
    br.addToolbar = false;
    
//    br.ui = "embed";
    br.ui = "full";
 
    // Let's go!
    br.init();
}
