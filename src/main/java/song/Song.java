package song;

import java.util.HashMap;
import com.mongodb.util.JSON;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;

import com.mongodb.DBObject;

/**
 * 
 * @author robert
 *
 */
public class Song implements DBObject {
	private Map<String,Object> song;
	private boolean isPartialObject = false;
	
	public Song() {
		song = new HashMap<String,Object>();
		song.put("album", "");
		song.put("artist", "");
		song.put("title", "");
		song.put("track", "");
		song.put("year", "");
	}
	
	public Song(String album, String artist, String title, String track, String year) {
		this();
		song.put("album", album);
		song.put("artist", artist);
		song.put("title", title);
		song.put("track", track);
		song.put("year", year);
	}
	
	public Song(String album, String artist, String title, String track, String year, String filename, String fileId) {
		this(album,artist,title,track,year);
		song.put("filename",filename);
		song.put("fileId", fileId);
	}
	
	public Object put(String key, Object v) {
		return song.put(key, v);
	}

	public void putAll(BSONObject o) {
		putAll(o.toMap());
	}

	public void putAll(Map m) {
		for(Object s : m.keySet()) {
			song.put((String)s, m.get(s));
		}
	}

	public Object get(String key) {
		return song.get(key);
	}

	public Map toMap() {
		return this.song;
	}

	public Object removeField(String key) {
		return song.remove(key);
	}

	public boolean containsKey(String s) {
		if(song.containsKey(s))
			return true;
		else
			return false;
	}

	public boolean containsField(String s) {
		if(song.containsKey(s))
			return true;
		else
			return false;
	}

	public Set<String> keySet() {
		return song.keySet();
	}

	public void markAsPartialObject() {
		isPartialObject = true;
	}

	public boolean isPartialObject() {
		return isPartialObject;
	}
	
	public String toString() {
		return JSON.serialize(this.song);
	}

	public String getAlbum() {
		return (String) this.get("album");
	}
	public String getArtist() {
		return (String) this.get("artist");
	}
	public String getTitle() {
		return (String) this.get("title");
	}
	public String getTrack() {
		return (String) this.get("track");
	}
	public String getYear() {
		return (String) this.get("year");
	}
	public String getFileId() {
		return (String) this.get("file-id");
	}
	public String getFilename() {
		return (String) this.get("filename");
	}
	
	public void setAlbum(String v) {
		this.put("album",v);
	}
	public void setArtist(String v) {
		this.put("artist",v);
	}
	public void setTitle(String v) {
		this.put("title",v);
	}
	public void setTrack(String v) {
		this.put("track",v);
	}
	public void setYear(String v) {
		this.put("year",v);
	}
	public void setFileId(Object v) {
		this.put("file-id", v);
	}
	public void setFilename(String v) {
		this.put("filename", v);
	}
}
