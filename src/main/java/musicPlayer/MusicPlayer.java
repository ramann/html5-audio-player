package musicPlayer;

import httpUtils.HttpUtils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import song.Song;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;
import spark.servlet.SparkApplication;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * MusicPlayer
 * @author robert mann
 *
 */
public class MusicPlayer implements SparkApplication {
	
	//TODO refactor 
	private static final String MONGO_CONNECTION = "127.0.0.1:27017";
	private static final String MONGO_DB_NAME = "test";
	private static final String MONGO_USERNAME = null;
	private static final String MONGO_PASSWORD = "yourPassword";
	
	public void init() {
		final Logger log = LoggerFactory.getLogger(MusicPlayer.class);
		Mongo conn = null;
				
		try {
			conn = new MongoClient(MONGO_CONNECTION);
		} catch (UnknownHostException e) {
			log.error("unable to connect to mongo",e);
		}
		final DB db = conn.getDB(MONGO_DB_NAME);
		if (MONGO_USERNAME != null && !db.authenticate(MONGO_USERNAME, MONGO_PASSWORD.toCharArray())) 
		{
			  throw new MongoException("unable to authenticate");
		}
		
		/*
		 * Landing page
		 */
		Spark.get(new Route("/") {
			@Override
			public Object handle(Request request, Response response) {

				final Configuration configuration = new Configuration();
				configuration.setClassForTemplateLoading(MusicPlayer.class, "/");
				StringWriter writer = new StringWriter();
				
				try {
					Template helloTemplate = configuration.getTemplate("landing.ftl");
					helloTemplate.process(new HashMap<String,String>(), writer);
				} catch (Exception e) {
					e.printStackTrace();
				}
				response.header("Content-Type", "text/html; charset=utf-8");
				return writer;
			}
		});
		
		/*
		 * Asset handler
		 */
		Spark.get(new Route("/assets/:folderName/:assetName"){ 
			@Override
			public Object handle(Request request, Response response) {
				String assetName = request.params(":assetName");
				String folderName = request.params(":folderName");
				
				// why not return getResourceAsStream? these are assets and caching is our friend.
				URL assetUrl = MusicPlayer.class.getClassLoader().getResource("assets/"+folderName+"/"+assetName);
				if (assetUrl == null) {
					log.info("asset not found: folder:" + folderName + ", name:" + assetName);
					response.status(404);
					return "404 - Resource Not Found";
				}
				
				File file;
				try {
					file = new File(assetUrl.toURI());
				} catch(URISyntaxException e) {
					file = new File(assetUrl.getPath());
				}
				
				return HttpUtils.getAssetAsStream(request.raw(), response.raw(), file);			
			}
		});
		
		/*
		 * stream back the song with given 'songId'
		 */
		Spark.get(new Route("/song") {
			@Override
			public Object handle(Request request, Response response) {
				DBCollection collection = db.getCollection("songs");
				String songId = request.queryParams("songId");
				DBObject matchOnId = new BasicDBObject().append("_id", new ObjectId(songId));
				DBObject theChosenOne = collection.findOne(matchOnId);

				GridFS myFS = new GridFS(db);
				GridFSDBFile file = myFS.findOne((ObjectId) theChosenOne.get("file-id"));
				
				return HttpUtils.getGridFSDBFileAsStream(request.raw(), response.raw(), file);
			}
		});
		
		/*
		 * edit song with given 'songId' and tag data
		 */
		Spark.get(new Route("/editSong") {
			@Override
			public Object handle(Request request, Response response) {
				DBCollection collection = db.getCollection("songs");
				String songId = request.queryParams("songId");
				ArrayList<String> possibleKeys = new ArrayList<String>(); //clean this up
				possibleKeys.add("title");
				possibleKeys.add("album");
				possibleKeys.add("artist");
				possibleKeys.add("year");
				possibleKeys.add("track");
				Map<String,String> updateKeys = new HashMap<String,String>();
				for (String s : possibleKeys) {
					if(request.queryParams(s) != null) {
						updateKeys.put(s,request.queryParams(s));
					}
				}
				
				DBObject matchOnId = new BasicDBObject().append("_id", new ObjectId(songId));
				DBObject theChosenOne = collection.findOne(matchOnId);

				GridFS myFS = new GridFS(db);
				GridFSDBFile file = myFS.findOne((ObjectId) theChosenOne.get("file-id"));
				boolean isFileUpdated = SongTransferUtils.updateFile(updateKeys,file,db,new ObjectId(songId));
				log.debug("file is updated? "+isFileUpdated);
				if (!isFileUpdated) {
					response.status(500);
				}
				return collection.findOne(matchOnId).toString(); //return json
			}
		});

		/*
		 * get all songs, either as playlist if 'isForPlaylist', else json
		 */
		Spark.get(new Route("/getAllSongs") {
			@Override
			public Object handle(Request request, Response response) {
				boolean isForPlaylist = Boolean.parseBoolean(request.queryParams("isForPlaylist"));
				DBCollection collection = db.getCollection("songs");
				DBCursor dbc = collection.find();
				ArrayList<String> allSongs = new ArrayList<String>();
				DBObject dbo = null;
				while (dbc.hasNext()) {
					dbo = dbc.next();
					allSongs.add(dbo.toString());
				}
				
				final Configuration configuration = new Configuration();
				configuration.setClassForTemplateLoading(MusicPlayer.class, "/");

				StringWriter writer = new StringWriter();

				try {
					Template allSongsTemplate;
					
					if(isForPlaylist) {
						allSongsTemplate = configuration.getTemplate("playlist.ftl");
					} else {
						allSongsTemplate = configuration.getTemplate("allSongs.ftl");
					}
					Map<String,Object> songs = new HashMap<String,Object>();
					songs.put("allSongs", allSongs);
					allSongsTemplate.process(songs,writer);
				
				} catch (Exception e) {
					e.printStackTrace();
				}
				response.header("Content-Type", "text/html; charset=utf-8");
				return writer;
			}
		});

		/*
		 * upload file.
		 */
		Spark.post(new Route("/uploadFile") { //TODO input handling
			@Override
			public Object handle(Request request, Response response) {
				
				HttpServletRequestWrapper httpServletRequestWrapper = null;
				httpServletRequestWrapper = new HttpServletRequestWrapper(request.raw());

				// put file in temp dir & parse for tag
				DiskFileItem dfi = HttpUtils.parseRequestForFileItem(httpServletRequestWrapper);
				Song song = SongTransferUtils.parseFileForSong(dfi);

				// put file in db
				GridFSInputFile gridFSInputFile = SongTransferUtils.putSongInDB(song, 
						dfi.getStoreLocation().toString(),
						dfi.getContentType(),
						db);
				dfi.delete();

				//you can toss in ajax handling for uploadifive
				
				final Configuration configuration = new Configuration();
				configuration.setClassForTemplateLoading(MusicPlayer.class, "/");

				StringWriter writer = new StringWriter();

				try {
					Template uploadFileTemplate = configuration.getTemplate("uploadFile.ftl");

					Map<String, Object> uploadFileMap = new HashMap<String, Object>();
					uploadFileMap.put("song", song.toString());
					uploadFileMap.put("file", gridFSInputFile.toString());

					uploadFileTemplate.process(uploadFileMap, writer);
				} catch (Exception e) {
					e.printStackTrace();
				}

				return writer;
			}
		});
	}

}
