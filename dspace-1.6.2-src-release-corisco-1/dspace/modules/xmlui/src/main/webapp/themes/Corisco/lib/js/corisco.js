// Remember to change the matching string below so that it matches this file name.

// ---------- funções ----------

var fs_path = "../";
var arr = jQuery.makeArray(document.getElementsByTagName("script"));
jQuery.each(arr, function(i, s) {
    if (s.src.match(/corisco\.js$/)) {
        fs_path = s.src.replace(/corisco\.js$/,'');
        return fs_path;
    } else {
        return "/";
    }
});

var images_path = fs_path + '../img/';

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
    if ($('#ficha-meta').is(':visible')){
        $('#ficha-meta').stop().fadeOut(500);
        $('#mais-metadados').children('img').attr('src', images_path + 'mais_filtro.png');
    } else {
        $('#ficha-meta').stop().fadeIn(500);
        $('#ficha-meta').stop().fadeTo(500, 1);
        $('#mais-metadados').children('img').attr('src', images_path + 'menos_filtro.png');
    }
})(jQuery);
}


//$.noConflict();


// ---------- funções fim ----------

( function($) {
$(document).ready(function(){

$('.mais-filtro').next('ul').hide();
$('.info').children('.borda').next('div').hide();

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

if ($('#fechar-link') != null && $('#fechar-link').length) {
    $('#fechar-link').click(
        function(){
            $('#ficha-link').fadeOut(500);
        }
    );
}

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


this.printURL = function(URL) {
    try {
        var iframe = document.createElement('iframe');
        iframe.id = 'printFrame';
        iframe.name = 'printFrame';
        $(iframe).load(function() {
            $(this).hide();
            var outer = (iframe.contentWindow || iframe.contentDocument);
            var doc = (outer.document || outer);

            $('body', doc).append(img);
        });

        var img = new Image();
        $(img)
            .load(function() {
                var iframe = $("#printFrame");
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
    $(elementSelector).printArea({mode: "popup", popClose: false});
    return false;

    x.printMe();
    x.contentWindow.print();
    return false;
    window.frames[0].focus(); window.frames[0].print(); return false;

    var iframeStr = '<html style="padding: 0; border: 0; margin: 0"><head>' + $("html head").html() + '</head><body style="padding: 0; border:0; margin: 0">';
    iframeStr += '<div style="text-align: center; width: 99%; height: 99%; overflow: hidden;" onload="window.print()">';
    iframeStr += htmlToPrint;
    iframeStr += '&#160;</div>';
    iframeStr += '</body></html>';

    w = window.open(document.URL+'#print', document.title, "width=800, height=500, resizable=yes, scrollbars=yes, toolbar=yes, location=yes");
    try {
        w.document.open();
        w.document.write(iframeStr);
    } catch (err) {
        console.log(err);
    }
};

});
}) (jQuery);

