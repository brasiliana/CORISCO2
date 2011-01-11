// ---------- funções ----------

var fs_path = "../";
var arr = jQuery.makeArray(document.getElementsByTagName("script"));
jQuery.each(arr, function(i, s) {
    if (s.src.match(/brasiliana\.js$/)) {
        fs_path = s.src.replace(/brasiliana\.js$/,'');
        return fs_path;
    } else {
        return "/";
    }
});

var images_path = fs_path + '../img/';

//jQuery.noConflict();

//function checarModo (){
//	if ($('#lista-resultados').is(':visible')){
//		$('.listagem').children('img').attr('src', images_path + 'modo_listagemON.png');
//		$('.mosaico').children('img').attr('src', images_path + 'modo_mosaico.png');
//		$('.listagem').addClass('modoON');
//		$('.mosaico').removeClass('modoON');
//	} else if ($('#grade-resultados').is(':visible')){
//		$('.listagem').children('img').attr('src', images_path + 'modo_listagem.png');
//		$('.mosaico').children('img').attr('src', images_path + 'modo_mosaicoON.png');
//		$('.listagem').removeClass('modoON');
//		$('.mosaico').addClass('modoON');
//	}
//}

function modosHover (){
$('.modos').children('span').children('img').hover(
		function(){	
			if ($(this).parent().not('.modoON')){
				$(this).attr('src', images_path + 'modo_'+$(this).attr('alt')+'OVER'+'.png');
			}
		},
		function(){
			if ($(this).parent().not('.modoON')){
				$(this).attr('src', images_path + 'modo_'+$(this).attr('alt')+'.png');
			}
		}
	);
}

/*
function checarModoVisu (){
	if ($('#pagina-unica').is(':visible')){
		$('.pagina-unica').children('img').attr('src', images_path + 'modo_pg_unicaON.png');
		$('.mosaico').children('img').attr('src', images_path + 'modo_mosaico.png');
		$('.pagina-dupla').children('img').attr('src', images_path + 'modo_pg_dupla.png');
	} else if ($('#pagina-mosaico').is(':visible')){
		$('.pagina-unica').children('img').attr('src', images_path + 'modo_pg_unica.png');
		$('.mosaico').children('img').attr('src', images_path + 'modo_mosaicoON.png');
		$('.pagina-dupla').children('img').attr('src', images_path + 'modo_pg_dupla.png');
	} else if ($('#pagina-dupla').is(':visible')){
		$('.pagina-unica').children('img').attr('src', images_path + 'modo_pg_unica.png');
		$('.mosaico').children('img').attr('src', images_path + 'modo_mosaico.png');
		$('.pagina-dupla').children('img').attr('src', images_path + 'modo_pg_duplaON.png');
	}
}
*/

function clearText(field){
    if (field.defaultValue == field.value) field.value = '';
    else if (field.value == '') field.value = field.defaultValue;
}

function alternarFichaLink(){
( function($) {
    if ($('#ficha-link').is(':visible')){
        $('#ficha-link').stop().fadeOut(500);
    } else {
        $('#ficha-link').stop().fadeIn(500);
        $('#ficha-link').stop().fadeTo(500, 1);
    }
})(jQuery);
}

function alternarMaisMetadados(){
( function($) {
//    $('#mais-metadados').click(
//            function(){
                if ($('#ficha-meta').is(':visible')){
                    $('#ficha-meta').stop().fadeOut(500);
                    $('#mais-metadados').children('img').attr('src', images_path + 'mais_filtro.png');
                } else {
                    $('#ficha-meta').stop().fadeIn(500);
                    $('#ficha-meta').stop().fadeTo(500, 1);
                    $('#mais-metadados').children('img').attr('src', images_path + 'menos_filtro.png');
                }
//            }
//        );
})(jQuery);
}


/*jQuery.fn.supersleight = function(settings) {
	settings = jQuery.extend({
		imgs: true,
		backgrounds: true,
		shim: 'x.gif',
		apply_positioning: false
	}, settings);
	
	return this.each(function(){
		if (jQuery.browser.msie && parseInt(jQuery.browser.version, 10) < 7 && parseInt(jQuery.browser.version, 10) > 4) {
			jQuery(this).find('*').andSelf().each(function(i,obj) {
				var self = jQuery(obj);
				// background pngs
				if (settings.backgrounds && self.css('background-image').match(/\.png/i) !== null) {
					var bg = self.css('background-image');
					var src = bg.substring(5,bg.length-2);
					var mode = (self.css('background-repeat') == 'no-repeat' ? 'crop' : 'scale');
					var styles = {
						'filter': "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + src + "', sizingMethod='" + mode + "')",
						'background-image': 'url('+settings.shim+')'
					};
					self.css(styles);
				};
				// image elements
				if (settings.imgs && self.is('img[src$=png]')){
					var styles = {
						'width': self.width() + 'px',
						'height': self.height() + 'px',
						'filter': "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + self.attr('src') + "', sizingMethod='scale')"
					};
					self.css(styles).attr('src', settings.shim);
				};
				// apply position to 'active' elements
				if (settings.apply_positioning && self.is('a, input') && (self.css('position') === '' || self.css('position') == 'static')){
					self.css('position', 'relative');
				};
			});
		};
	});
};*/



//$.noConflict();


// ---------- funções fim ----------
( function($) {
$(document).ready(function(){

//$('body').supersleight();

//$('#lista-resultados').hide();
//$('#grade-resultados').hide();

//$('#pagina-unica').hide();
//$('#pagina-mosaico').hide();
//$('#pagina-dupla').hide();

//$('#visualizador-imagem').hide();

$('.mais-filtro').next('ul').hide();

$('.info').children('.borda').next('div').hide();

//checarModo ();
//checarModoVisu ();
//modosHover ();


if ($('#fechar-meta') != null && $('#fechar-meta').length) {
    $('#fechar-meta').click(
		function(){
			$('#ficha-meta').fadeOut(500);
			$('#mais-metadados').children('img').attr('src', images_path + 'mais_filtro.png');
		}
	);
}

if ($('#mais-imagens') != null && $('#mais-imagens').length) {
    $('#mais-imagens').click(
		function(){
			if ($('#ficha-imagens').is(':visible')){
				$('#ficha-imagens').stop().fadeOut(500);
				$(this).children('img').attr('src', images_path + 'mais2.png');
			} else {
				$('#ficha-imagens').stop().fadeIn(500);
				$('#ficha-imagens').stop().fadeTo(500, 1);
				$(this).children('img').attr('src', images_path + 'menos2.png');
			}
		}
	);
}

if ($('#fechar-imagens') != null && $('#fechar-imagens').length) {
    $('#fechar-imagens').click(
		function(){
			$('#ficha-imagens').fadeOut(500);
			$('#mais-imagens').children('img').attr('src', images_path + 'mais2.png');
		}
	);
}

// Dando conflito. Utilizando atualmente a funcao 'alternarFichaLink', que precisa
// ser registrada via 'onclick' nos elementos (e.g., em '.linkar').
//if ($('.linkar') != null && $('.linkar').length) {
//    $('.linkar').click(
//		function(){
//			if ($('#ficha-link').is(':visible')){
//				$('#ficha-link').stop().fadeOut(500);
//			} else {
//				$('#ficha-link').stop().fadeIn(500);
//				$('#ficha-link').stop().fadeTo(500, 1);
//			}
//		}
//	);
//}

if ($('#fechar-link') != null && $('#fechar-link').length) {
    $('#fechar-link').click(
		function(){
			$('#ficha-link').fadeOut(500);
		}
	);
}
//$('.modos').children('.listagem').click(
//		function(){
//			//$('.listagem').addClass('modoON');
//			$('.listagem').children('img').attr('src', images_path + 'modo_listagemON.png');
//			//$('.mosaico').removeClass('modoON');
//			$('.mosaico').children('img').attr('src', images_path + 'modo_mosaico.png');
//			$('#grade-resultados').hide();
//			$('#lista-resultados').show();
//			checarModo();
//			//modosHover();
//		}
//	);
//
//$('.modos').children('.mosaico').click(
//		function(){
//			//$('.listagem').removeClass('modoON');
//			$('.listagem').children('img').attr('src', images_path + 'modo_listagem.png');
//			//$('.mosaico').addClass('modoON');
//			$('.mosaico').children('img').attr('src', images_path + 'modo_mosaicoON.png');
//			$('#grade-resultados').show();
//			$('#lista-resultados').hide();
//			checarModo();
//			//modosHover();
//		}
//	);

if ($('.mais-filtro') != null && $('.mais-filtro').length) {
    $('.mais-filtro').toggle(
        function(){
            $(this).children('img').attr('src', images_path + 'menos_filtro.png');
            $(this).next('ul').slideToggle();
        },
        function(){
            $(this).children('img').attr('src', images_path + 'mais_filtro.png');
            $(this).next('ul').slideToggle();
        }
    );
}

if ($('.info') != null && $('.info').length) {
    $('.info').children('.borda').toggle(
        function(){
            $(this).children('p').children('img').attr('src', images_path + 'menos.png');
            $(this).next('div').slideToggle();
        },
        function(){
            $(this).children('p').children('img').attr('src', images_path + 'mais.png');
            $(this).next('div').slideToggle();
        }
    );
}


/*
$('.modos_visualizador').children('.pagina-unica').click(
		function(){	
			$('#pagina-unica').show();
			$('#pagina-mosaico').hide();
			$('#pagina-dupla').hide();
			checarModoVisu ();
		}
	);
$('.modos_visualizador').children('.mosaico').click(
		function(){	
			$('#pagina-unica').hide();
			$('#pagina-mosaico').show();
			$('#pagina-dupla').hide();
			checarModoVisu ();
		}
	);
$('.modos_visualizador').children('.pagina-dupla').click(
		function(){	
			$('#pagina-unica').hide();
			$('#pagina-mosaico').hide();
			$('#pagina-dupla').show();
			checarModoVisu ();
		}
	);
*/

//<a class="icone_barra print rollover">

//$("a.print").click(function(){
this.printURL = function(URL) {
    try {
        var iframe = document.createElement('iframe');
        iframe.id = 'printFrame';
        iframe.name = 'printFrame';
//        iframe.width = '233px'; // 8.5 x 11 aspect
//        iframe.height = '300px';

    //    var self = this; // closure

        $(iframe).load(function() {
            $(this).hide();
            var outer = (iframe.contentWindow || iframe.contentDocument);
            var doc = (outer.document || outer);

            $('body', doc).append(img);
        });

        var img = new Image();
        $(img)
            .load(function() {
                var iframe = $("#printFrame"); //$(this).parents("#printFrame");
                var outer = (iframe[0].contentWindow || iframe[0].contentDocument);
                outer.print();
                $('body').remove(iframe);
            })
            .attr('src', URL);

        $('body').append(iframe);
    } catch (err) {
        console.log(err);
    }
    return false;
};

this.printElement = function(elementSelector) {
    //var htmlToPrint = $(elementSelector).html();

    //var x = document.getElementById(elementSelector);
    //x.focus();

    //$("div#print_button").click(function(){
    $(elementSelector).printArea({mode: "popup", popClose: false});
    return false;

    x.printMe();
    x.contentWindow.print();
    return false;
    window.frames[0].focus(); window.frames[0].print(); return false;

//    var imageURL = this._getPageURI(index, 1, rotate);
//    var iframeStr = '<html style="padding: 0; border: 0; margin: 0"><head><title>' + document.title + '</title></head><body style="padding: 0; border:0; margin: 0">';
    var iframeStr = '<html style="padding: 0; border: 0; margin: 0"><head>' + $("html head").html() + '</head><body style="padding: 0; border:0; margin: 0">';
    iframeStr += '<div style="text-align: center; width: 99%; height: 99%; overflow: hidden;" onload="window.print()">';
//    iframeStr +=   '<img src="' + imageURL + '" ' + fitAttrs + ' onload="window.print()" />';
    iframeStr += htmlToPrint;
    iframeStr += '&#160;</div>';
    iframeStr += '</body></html>';
//
//    return iframeStr;

    w = window.open(document.URL+'#print', document.title, "width=800, height=500, resizable=yes, scrollbars=yes, toolbar=yes, location=yes");
    try {
        w.document.open();
//        $(window).load(function() {
//            self.print();
//        });
        w.document.write(iframeStr);
//        w.document.write(htmlToPrint);
//        w.document.close();
//        w.print();
//        setTimeout(function(){w.close();},100);
    } catch (err) {
        console.log(err);
//        w.close();
    }
};

});
} ) (jQuery);