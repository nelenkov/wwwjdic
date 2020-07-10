<%@ taglib uri='http://java.sun.com/jsp/jstl/core' prefix='c' %>
<%@ page isELIgnored="false" %>

<%@ page
        import="com.google.appengine.api.blobstore.BlobstoreServiceFactory" %>
<%@ page import="com.google.appengine.api.blobstore.BlobstoreService" %>

<%
    BlobstoreService blobstoreService = BlobstoreServiceFactory
            .getBlobstoreService();
%>

<html>
<head>
    <title>Upload KanjiVG XML</title>
</head>
<body>

<p>Uploaded file info</p>
<p>
    Blob key:  <c:out value="${param.blobKey}"/><br/>
    Filename:  <c:out value="${param.filename}"/><br/>
    Size:      <c:out value="${param.size}"/><br/>
    Num kanji: <c:out value="${param.numKanji}"/><br/>
</p>

</br>
<form action="<%=blobstoreService.createUploadUrl("/upload")%>"
      method="post" enctype="multipart/form-data">
    <fieldset>
        <legend>KanjiVG file</legend>
        <label>File: </label><input type="file" name="kanjivg"/><br/><br/>
        <hr/>
        <input type="submit" value="Upload"/>
        <input type="reset" value="Reset"/>
    </fieldset>
</form>
</body>
</html>
