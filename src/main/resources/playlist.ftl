<!DOCTYPE html>
<html>
<head>
<meta charset=utf-8 />

<title>Audio Player</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<link href="assets/css/playlist.css" rel="stylesheet" type="text/css">
<link rel="stylesheet" href="assets/css/font-awesome.css" type="text/css">
<script type="text/javascript" src="assets/js/jquery.min.js"></script>
<script type="text/javascript" src="assets/js/jquery.uid.js"></script>
<script type="text/javascript" src="assets/js/jquery.jplayer.min.js"></script>
<script type="text/javascript" src="assets/js/jplayer.playlist.js"></script>
<script type="text/javascript">
//<![CDATA[

var songData = [<#list allSongs as x>${x}<#if x_has_next>,</#if></#list>];
var songList = [];
jQuery(songData).each(function(index) {
	songList[index] = {};
	songList[index].title = this.title;
	
	//if(songList[index].title == "") {
	//	songList[index].title = this.filename;
	//}
	
	songList[index].artist = this.artist;
	songList[index].album = this.album;
	songList[index].mp3 = "song?songId="+this._id.$oid;
	songList[index].id = this._id.$oid;
	songList[index].year = this.year;
	songList[index].filename = this.filename;
});
var playlist = {};
$(document).ready(function(){
	playlist = new jPlayerPlaylist({
		jPlayer: "#jquery_jplayer_1",
		cssSelectorAncestor: "#jp_container_1"
	}, songList, 
	{ playlistOptions: {
    	enableRemoveControls: true,
    	removeItemClass: "delete-song"
  	}, 
		swfPath: "js",
		supplied: "mp3",
		wmode: "window"
	});
});	

//]]>
</script>
</head>
<body>

		<div id="jquery_jplayer_1" class="jp-jplayer"></div>

		<div id="jp_container_1" class="jp-audio">
			<div class="jp-type-playlist">
				<div class="jp-gui jp-interface">
					<ul class="jp-controls">
						<li><a href="javascript:;" class="jp-previous" tabindex="1"><i class="fa fa-fw fa-backward fa-2x"></i></a></li>
						<li><a href="javascript:;" class="jp-play" tabindex="1"><i class="fa fa-fw fa-play fa-2x"></i></a></li>
						<li><a href="javascript:;" class="jp-pause" tabindex="1" style="display: none;"><i class="fa fa-fw fa-pause fa-2x"></i></a></li>
						<li><a href="javascript:;" class="jp-next" tabindex="1"><i class="fa fa-fw fa-forward fa-2x"></i></a></li>
						<li><a href="javascript:;" class="jp-stop" tabindex="1"><i class="fa fa-fw fa-stop fa-2x"></i></a></li>
						<li class="progress-control">
							<div class="jp-progress">
								<div class="jp-seek-bar">
									<div class="jp-play-bar"></div>
								</div>
							</div>
						</li>
						<li class="volume-control"><a href="javascript:;" class="jp-volume-max" tabindex="1" title="max volume"><i class="fa fa-fw fa-volume-up fa-2x"></i></a></li>
						<li class="volume-control">
							<div class="jp-volume-bar">
								<div class="jp-volume-bar-value"></div>
							</div>
						</li>
						<li class="volume-control"><a href="javascript:;" class="jp-mute" tabindex="1" title="mute"><i class="fa fa-fw fa-volume-off fa-2x"></i></a></li>
						<li class="volume-control"><a href="javascript:;" class="jp-unmute" tabindex="1" title="unmute" style="display: none;"><i class="fa fa-fw fa-volume-down fa-2x"></i></a></li>						
					<!--	<li><input id="search" name="search" type="text"></li> -->
					</ul>
					<div class="jp-time-holder hidden">
						<div class="jp-current-time"></div>
						<div class="jp-duration"></div>
					</div>
					<ul class="jp-toggles hidden">
						<li><a href="javascript:;" class="jp-shuffle" tabindex="1" title="shuffle">shuffle</a></li>
						<li><a href="javascript:;" class="jp-shuffle-off" tabindex="1" title="shuffle off">shuffle off</a></li>
						<li><a href="javascript:;" class="jp-repeat" tabindex="1" title="repeat">repeat</a></li>
						<li><a href="javascript:;" class="jp-repeat-off" tabindex="1" title="repeat off">repeat off</a></li>
					</ul>
				</div>
				<div class="jp-playlist">
					<ul class="playlist">
						<li></li>
					</ul>
				</div>
				<div class="jp-no-solution">
					<span>Update Required</span>
					There is no Flash fallback here (why? this: <a href="http://www.cvedetails.com/vulnerability-list/vendor_id-53/product_id-6761/Adobe-Flash-Player.html">Critical Security Vulnerabilites in Flash</a>), so try upgrading your browser.
				</div>
			</div>
		</div>
</body>

</html>
