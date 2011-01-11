var urlVer = "?url_ver=Z39.88-2004"
var rftBase = "&rft_id=";
var svcId = "&svc_id=info:lanl-repo/svc/getRegion";
var svcFmt = "&svc_val_fmt=info:ofi/fmt:kev:mtx:jpeg2000";
var svcInfo = svcId + svcFmt;
var svcFormatBase = "&svc.format=";
var svcLevelBase = "&svc.level=";
var svcRotateBase = "&svc.rotate=";
var svcRegionBase = "&svc.region=";
var svcRegionComma = ",";

function getOpenURL(baseUrl, rft_id, format, rotate, level, regionY , regionX, regionH, regionW, regionEnabled ) {
    if (!rft_id) {
        errorMessage();
    } else {
        if (regionEnabled)
            return baseUrl + urlVer + rftBase +  encode(rft_id) + svcInfo + svcFormatBase + format + svcLevelBase + level + svcRotateBase + rotate + svcRegionBase + regionY + svcRegionComma + regionX + svcRegionComma + regionH + svcRegionComma + regionW;
        else
            return baseUrl + urlVer + rftBase +  encode(rft_id) + svcInfo + svcFormatBase + format + svcLevelBase + level + svcRotateBase + rotate;          
    }
}

function openImageInNewWindow(baseUrl, rft_id, format, level, rotate, regionY , regionX, regionH, regionW, regionEnabled) {
        openUrl = getOpenURL(baseUrl, rft_id, format, level, rotate, regionY , regionX, regionH, regionW, regionEnabled );
        jp2k = window.open(openUrl, 'djatoka OpenURL', 'left=20,top=20,width=500,height=500,toolbar=1,resizable=1,location=1,scrollbars=1');
        if (window.focus) {jp2k.focus()}
        var openURL_div = document.getElementById("openURL_div");
        var display = baseUrl + urlVer + "<br\>&nbsp;&nbsp;&nbsp;&nbsp;" + 
                      rftBase +  "<b>" + decode(rft_id) + "</b>" + "<br\>&nbsp;&nbsp;&nbsp;&nbsp;" + 
                      svcId + "<br\>&nbsp;&nbsp;&nbsp;&nbsp;" + 
                      decode(svcFmt) + "<br\>&nbsp;&nbsp;&nbsp;&nbsp;" + 
                      svcFormatBase +  "<b>" + format + "</b>" + "<br\>&nbsp;&nbsp;&nbsp;&nbsp;" + 
                      svcLevelBase +  "<b>" + rotate + "</b>" + "<br\>&nbsp;&nbsp;&nbsp;&nbsp;" + 
                      svcRotateBase +  "<b>" + level + "</b>" + "<br\>&nbsp;&nbsp;&nbsp;&nbsp;" + 
                      svcRegionBase +  "<b>" + regionY + svcRegionComma + regionX + svcRegionComma + regionH + svcRegionComma + regionW + "</b>";
        if (openURL_div) {
           openURL_div.innerHTML = display;
           openURL_div.style.display = 'block';
        }
}

function errorMessage( info ) {
    var errorMessage_div = document.getElementById("errorMessage_div");
    errorMessage_div.innerHTML = info;
}

function updateImage(baseUrl, rft_id, format, level, rotate, regionY , regionX, regionH, regionW, regionEnabled) {
    openUrl = getOpenURL(baseUrl, rft_id, format, level, rotate, regionY , regionX, regionH, regionW, regionEnabled );
    var div = "<img src='" + openUrl + "' border='0'></img>";
    var div_jp2Image = document.getElementById("jp2Image_div");
    if (div_jp2Image) {
        div_jp2Image.innerHTML = div;
        div_jp2Image.style.display = 'block';
    }
    
    var openURL_div = document.getElementById("openURL_div");
    var display = baseUrl + urlVer + "<br\>&nbsp;&nbsp;&nbsp;&nbsp;" + 
                  rftBase +  "<b>" + decode(rft_id) + "</b>" + "<br\>&nbsp;&nbsp;&nbsp;&nbsp;" + 
                  svcId + "<br\>&nbsp;&nbsp;&nbsp;&nbsp;" + 
                  decode(svcFmt) + "<br\>&nbsp;&nbsp;&nbsp;&nbsp;" + 
                  svcFormatBase +  "<b>" + format + "</b>" + "<br\>&nbsp;&nbsp;&nbsp;&nbsp;" + 
                  svcLevelBase +  "<b>" + rotate + "</b>" + "<br\>&nbsp;&nbsp;&nbsp;&nbsp;" +
                  svcRotateBase +  "<b>" + level + "</b>" + "<br\>&nbsp;&nbsp;&nbsp;&nbsp;" + 
                  svcRegionBase +  "<b>" + regionY + svcRegionComma + regionX + svcRegionComma + regionH + svcRegionComma + regionW + "</b>";
        if (openURL_div) {
           openURL_div.innerHTML = display;
           openURL_div.style.display = 'block';
        }
}

function encode( uri ) {
    if (encodeURIComponent) {
        return encodeURIComponent(uri);
    }

    if (escape) {
        return escape(uri);
    }
}

function decode( uri ) {
    if (decodeURIComponent) {
        return decodeURIComponent(uri);
    }

    if (unescape) {
        return unescape(uri);
    }
}

function disableField(form) {
    document.getElementById(form).disabled=true;
}

function enableField(form) {
    document.getElementById(form).disabled=false;
}