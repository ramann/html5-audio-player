# html5 audio player

## what is this?
A Java-based, [MongoDB](http://www.mongodb.com)-backed, HTML5 audio (mp3) player. The [Spark](http://www.sparkjava.com) web framework (a very lightweight Sinatra-like library) is used to route requests to the appropriate handler and the open source [jPlayer](http://www.jplayer.org) library (a jQuery plugin) is used to bind HTML5 Audio DOM events to elements. [Jaudiotagger](http://www.jthink.net/jaudiotagger/) is used to extract and transform ID3 tags. Text is made pretty with an open source Google Font and the control panel uses the scalable vector icons from [Font Awesome](http://www.fontawesome.io) (part of Bootstrap). You can pay a few dollars to [Uploadify](http://www.uploadify.com) and drop in HTML5-based bulk uploading support. There is no Flash fallback here (why? [this](http://www.cvedetails.com/vulnerability-list/vendor_id-53/product_id-6761/Adobe-Flash-Player.html)), so make sure your browser supports HTML5 mp3 sources. 

### notes:
* MusicPlayer implements SparkApplication for a context environment but you can easily make a few tweaks and run it as a standalone (embedded Jetty).
* to deploy on (to/through/in/with?) [OpenShift](http://www.openshift.com), just plug in your Mongo connection/db info and add the maven dependencies to the pom
* all static resources are contained within the project (you may run it locally without depending on an external internet connection)
* this is a work in progress

### to do:
* refactor (dao)
* playlists
* ogg support
* mobile browser support
* refactor
* draggable songs
* text filter
* input handling
* refactor
* album art

## author:
Robert Mann

