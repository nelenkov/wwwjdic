<%@ taglib uri='http://java.sun.com/jstl/core' prefix='c' %>
<%@ page
	import="com.google.appengine.api.blobstore.BlobstoreServiceFactory"%>
<%@ page import="com.google.appengine.api.blobstore.BlobstoreService"%>

<%
    BlobstoreService blobstoreService = BlobstoreServiceFactory
            .getBlobstoreService();
%>


<html>
<head>
<title>Upload Test</title>
</head>
<body>
Blob key: <c:out value='${param.blobKey}'/>, 
<form action="<%=blobstoreService.createUploadUrl("/upload")%>"
	method="post" enctype="multipart/form-data"><input type="file"
	name="kanjivg"> <input type="submit" value="Submit"></form>
</body>
</html>
