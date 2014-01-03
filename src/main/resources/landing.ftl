<html>
<head>
<title>Landing</title>
</head>
<body>
	<h1>Upload a file</h1>
	<span>Upload an mp3</span>
	<form action="uploadFile" method="post" enctype="multipart/form-data">
		<input name="theFile" type="file"/>
		<input type="submit" value="Submit"/>
	</form>
	<br>
	<span>Get A Song</span>
	<form action="/song">
		<input name="songId" type="text">
		<input type="submit" value="Submit"/>
	</form>
	
	<a href="getAllSongs">get json list of songs</a>
	<a href="getAllSongs?isForPlaylist=true">get all (playlist)</a>
	
</body>
</html>
