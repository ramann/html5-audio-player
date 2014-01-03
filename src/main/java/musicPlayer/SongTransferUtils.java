package musicPlayer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.bson.types.ObjectId;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

import song.Song;

/**
 * utility class
 * @author robert
 *
 */
public class SongTransferUtils {
	//TODO refactor
	final static Logger log = LoggerFactory.getLogger(SongTransferUtils.class);
	
	/**
	 * Parse the tmp file for tag data and create associated song
	 * @param dfi
	 * @return song created from tag data
	 */
	public static Song parseFileForSong(DiskFileItem dfi) { 	// TODO Think about where to move this.
		File audioFile = new File(dfi.getStoreLocation().toString().replace(".tmp",
				".mp3"));
			//	"."+dfi.getContentType().substring(dfi.getContentType().indexOf("/")+1)));

		if (!dfi.getStoreLocation().renameTo(audioFile)) {
			log.error("rename failed - store location:" +
					dfi.getStoreLocation()+", audio file:"+audioFile);
		}

		AudioFile f = null;
		try {
			f = AudioFileIO.read(audioFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Tag tag = f.getTag();
		if (tag == null) {
			tag = f.getTagOrCreateAndSetDefault();
		}
		Song song = new Song();
		try {
			song.setArtist(tag.getFirst(FieldKey.ARTIST));
			song.setAlbum(tag.getFirst(FieldKey.ALBUM));
			song.setTitle(tag.getFirst(FieldKey.TITLE));
			song.setYear(tag.getFirst(FieldKey.YEAR));
			song.setTrack(tag.getFirst(FieldKey.TRACK));
			song.setFilename(dfi.getName());
		} catch (KeyNotFoundException knfe) {
			log.error("song key not found", knfe);
		}
		return song;
	}
	
	/**
	 * Update the GridFSDBFile in the associated DB with the key/values in updateKeys
	 * @param updateKeys Map of new tag data
	 * @param file GridFSDBFile to update with tag data
	 * @param db
	 * @param songId ID of Song to update with tag data
	 * @return
	 */
	public static boolean updateFile(Map<String, String> updateKeys, GridFSDBFile file,
			DB db, ObjectId songId) { 	//TODO updateKeys?
		File audioFile = null;
		try {
			audioFile = File.createTempFile("tmp", ".mp3");
		} catch (IOException e) {
			log.error("tmp file not created", e);
		}
		
		audioFile.deleteOnExit();
		AudioFile f = null;
		ObjectId id = (ObjectId) file.getId();
		ObjectId oid = null;
		try {
			file.writeTo(audioFile);
			f = AudioFileIO.read(audioFile);
			Tag tag = f.getTagOrCreateAndSetDefault();
			DBObject q = new BasicDBObject("_id",songId);
			DBObject o = new BasicDBObject("$set", new BasicDBObject(updateKeys));
	    	
			if(updateKeys.get("artist") != null) {
				tag.setField(FieldKey.ARTIST,updateKeys.get("artist"));
			}
			if(updateKeys.get("album") != null) {
				tag.setField(FieldKey.ALBUM,updateKeys.get("album"));
			}
			if(updateKeys.get("title") != null) {
				tag.setField(FieldKey.TITLE,updateKeys.get("title"));
			}
			if(updateKeys.get("track") != null) {
				tag.setField(FieldKey.TRACK,updateKeys.get("track"));
			}
			if(updateKeys.get("year") != null) {
				tag.setField(FieldKey.YEAR,updateKeys.get("year"));
			}
			AudioFileIO.write(f);
			GridFS myFS = new GridFS(db);
			myFS.remove(id);
			GridFSInputFile inputFile = putSongFileInDB(f.getFile(), db,
					file.getContentType(), file.getFilename(), id);
			oid = (ObjectId) inputFile.getId();
			if(oid.equals(id)) {
				db.getCollection("songs").update(q, o);
			}
		} catch (KeyNotFoundException knfe) {
			log.error("key not found", knfe);
		} catch (FieldDataInvalidException fdie) {
			log.error("tried to set field with invalid value", fdie);
		} catch (Exception e) {
			log.error("error reading/writing file", e);
		}
		return (oid.equals(id));
	}
	
	/**
	 * 
	 * @param file
	 * @param db
	 * @param contentType
	 * @param filename
	 * @param id
	 * @return
	 */
	private static GridFSInputFile putSongFileInDB(File file, DB db, String contentType, 
			String filename, Object id) {
		byte[] songAsBytes = null;
		GridFS myFS = new GridFS(db);
		GridFSInputFile gridFSInputFile = null;
		try {
			songAsBytes = FileUtils.readFileToByteArray(file);
			gridFSInputFile = myFS.createFile(songAsBytes);
			gridFSInputFile.setFilename(filename);
			gridFSInputFile.setContentType(contentType);
			if(id != null) {
				gridFSInputFile.put("_id", id);
			}
			
			gridFSInputFile.save(); // insert file
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return gridFSInputFile;
	}

	/**
	 * Put the Song and data file associated with fileLocation in the database.
	 * @param song
	 * @param fileLocation
	 * @param contentType
	 * @param db
	 * @return
	 */
	public static GridFSInputFile putSongInDB(Song song, String fileLocation, String contentType, DB db) {
		DBCollection collection = db.getCollection("songs");
	
		Object id = null;
		File file = new File(fileLocation.replace(".tmp", 
				".mp3"));
			//	"."+contentType.substring(contentType.indexOf("/")+1)));
		
		GridFSInputFile gridFSInputFile = putSongFileInDB(file,
				db,contentType,song.getFilename(),id);
	
		song.setFileId(gridFSInputFile.getId());
		collection.insert(song); // insert corresponding json doc
		log.info("song inserted: "+song.toString());

		if(!file.delete()) { //delete the tmp file
			log.warn("file not deleted: "+file.getPath());
		}
		return gridFSInputFile;
	}

}
