<%@ page contentType="text/html" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page isELIgnored="false" %>

<html>
<head>
    <title>KVG Playground</title>
    <script type="text/javascript" src="/kanjiviewer/jquery-1.6.1.min.js"></script>
    <script type="text/javascript" src="/kanjiviewer/raphael.js"></script>
    <script type="text/javascript" src="/kanjiviewer/raphael.zoom.js"></script>
    <c:choose>
        <c:when test="${empty param.unicode}">
            <c:set var="unicode" value="91d1"/>
        </c:when>
        <c:otherwise>
            <c:set var="unicode" value="${param.unicode}"/>
        </c:otherwise>
    </c:choose>
    
    <script type="text/javascript" src="/kanji/<c:out value='${unicode}' />?f=json"></script>
    <script type="text/javascript" src="/kanjiviewer/kanjiviewer.js"></script>
    <script type="text/javascript" src="/kanjiviewer/app.js"></script>
    <link href="/kanjiviewer/reset.css" rel="stylesheet" type="text/css"/>
    <link href="/kanjiviewer/style.css" rel="stylesheet" type="text/css"/>
</head>

<body>
    <p>
    <h2>Stroke order diagram for <c:out value="${unicode}" /></h2>
    </p>
    
    <p>
    <form id="kanjiViewerParams" action="#">
        <table>
            <tr>
                <td><label for="kanji">Kanji</label></td>
                <td><input type="text" value="雨" id="kanji"/></td>
            </tr>
            <tr>
                <td><label for="strokeWidth">Stroke width</label></td>
                <td><input type="text" value="3" id="strokeWidth"/></td>
            </tr>
            <tr>
                <td><label for="fontSize">Font size</label></td>
                <td><input type="text" value="10" id="fontSize"/></td>
            </tr>
            <tr>
                <td><label for="zoomFactor">Zoom factor</label></td>
                <td><input type="text" value="3" id="zoomFactor"/></td>
            </tr>
            <tr>           
                <td><input id="redraw" type="submit" value="Redraw"/></td>
                <td><input id="clear" type="submit" value="Clear"/></td>
            </tr>
        </table>
    </form>
    </p>
    <script>
    $("#redraw").click(function () { 
    	var kanji = $("#kanji").val();
    	var kanjiUrl = "/kanji/" + kanji + "?f=json";
    	$.ajax({
    		   type: "GET",
    		   url: kanjiUrl,
    		   dataType: "json", 
    		   success: function(kanji){
    			   var kanjis = new Array();
    			   kanjis.push(kanji);
    			   
                   KanjiViewer.setStrokeWidth($('#strokeWidth').val());
                   KanjiViewer.setFontSize($('#fontSize').val());
    			   KanjiViewer.draw(kanjis);
    			   KanjiViewer.setZoom($("#zoomFactor").val());
    		   },
    	       error: function(jqXHR, textStatus, errorThrown) {
    	    	 alert(textStatus);
    	       }
    		 });
    });
    $("#clear").click(function() {
    	KanjiViewer.clear();
    });
   </script>
   <br/>
   <div id="kanjiViewer"></div>
   </body>
</html>
        
