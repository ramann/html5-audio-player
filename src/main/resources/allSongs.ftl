<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
<head>
<script type="text/javascript" src="assets/js/jquery.min.js"></script>
<script type="text/javascript">
jQuery(document).ready(function() {
console.log("testing 123");
console.log(jQuery(".song").first().text());
var txt = JSON.stringify(JSON.parse(jQuery(".song").first().text()), null,'\t');
console.log(txt);
jQuery(".song").each(function() {
	var prettyText = JSON.stringify(JSON.parse(jQuery(this).text()), null,'\t');
	jQuery(this).text(prettyText);
});
});
</script>
<title>All Songs</title>
</head>
<body>
<h1>All Songs</h1>
<br/>
<#list allSongs as x>
<pre class="song">${x}</pre>
</#list>
</body>
</html>