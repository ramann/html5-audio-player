package httpUtils;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.gridfs.GridFSDBFile;

/**
 * Utility class to handle range (and non-range) http requests. based on balusC's FileServlet. LGPL.
 * @author robert
 * 
 ***************************************************************************************************
 * Copyright (C) 2009 BalusC
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 */
public class HttpUtils {
	private static final int DEFAULT_BUFFER_SIZE = 20480; // ..bytes = 20KB.
	private static final long DEFAULT_EXPIRE_TIME = 1; // = 604800000L; // ..ms // = 1 week.
	private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";
	private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);
	
	/**
	 * Returns true if the given accept header accepts the given value.
	 * 
	 * @param acceptHeader
	 *            The accept header.
	 * @param toAccept
	 *            The value to be accepted.
	 * @return True if the given accept header accepts the given value.
	 */
	private static boolean accepts(String acceptHeader, String toAccept) {
		String[] acceptValues = acceptHeader.split("\\s*(,|;)\\s*");
		Arrays.sort(acceptValues);
		return Arrays.binarySearch(acceptValues, toAccept) > -1 || 
				Arrays.binarySearch(acceptValues, toAccept.replaceAll("/.*$", "/*")) > -1 || 
				Arrays.binarySearch(acceptValues, "*/*") > -1;
	}

	/**
	 * Returns true if the given match header matches the given value.
	 * 
	 * @param matchHeader
	 *            The match header.
	 * @param toMatch
	 *            The value to be matched.
	 * @return True if the given match header matches the given value.
	 */
	private static boolean matches(String matchHeader, String toMatch) {
		String[] matchValues = matchHeader.split("\\s*,\\s*");
		Arrays.sort(matchValues);
		return Arrays.binarySearch(matchValues, toMatch) > -1 || Arrays.binarySearch(matchValues, "*") > -1;
	}

	/**
	 * Close the given resource.
	 * 
	 * @param resource
	 *            The resource to be closed.
	 */
	private static void close(Closeable resource) {
		if (resource != null) {
			try {
				resource.close();
			} catch (IOException ignore) {
				// Ignore IOException. If you want to handle this anyway, it
				// might be useful to know
				// that this will generally only be thrown when the client
				// aborted the request.
			}
		}
	}

	/**
	 * Copy the given byte range of the given input to the given output.
	 * 
	 * @param input
	 *            The input to copy the given range to the given output for.
	 * @param output
	 *            The output to copy the given range from the given input for.
	 * @param start
	 *            Start of the byte range.
	 * @param length
	 *            Length of the byte range.
	 * @throws IOException
	 *             If something fails at I/O level.
	 */
	private static void copy(InputStream input, OutputStream output, long inputSize, 
			long start, long length) throws IOException {
		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		int read;

		if (inputSize == length) {
			// Write full range.
			while ((read = input.read(buffer)) > 0) {
				output.write(buffer, 0, read);
				output.flush();
			}
		} else {
			input.skip(start);
			long toRead = length;

			while ((read = input.read(buffer)) > 0) {
				if ((toRead -= read) > 0) {
					output.write(buffer, 0, read);
					output.flush();
				} else {
					output.write(buffer, 0, (int) toRead + read);
					output.flush();
					break;
				}
			}
		}
	}

	/**
	 * Returns a substring of the given string value from the given begin index
	 * to the given end index as a long. If the substring is empty, then -1 will
	 * be returned
	 * 
	 * @param value
	 *            The string value to return a substring as long for.
	 * @param beginIndex
	 *            The begin index of the substring to be returned as long.
	 * @param endIndex
	 *            The end index of the substring to be returned as long.
	 * @return A substring of the given string value as long or -1 if substring
	 *         is empty.
	 */
	private static long sublong(String value, int beginIndex, int endIndex) {
		String substring = value.substring(beginIndex, endIndex);
		return (substring.length() > 0) ? Long.parseLong(substring) : -1;
	}
	
	/**
	 * Get the file as an HttpServletResponse
	 * @param request
	 * @param response
	 * @param file
	 * @return
	 */
	public static HttpServletResponse getAssetAsStream(HttpServletRequest request, HttpServletResponse response, File file) {
		InputStream dataStream = null;
		try {
			dataStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			log.warn("file not found", e);
		}
		long fileLength = file.length();
		String fileName = file.getName();
		long fileLastModified = file.lastModified();
		String contentType = null;
		
		Path path = FileSystems.getDefault().getPath("",file.getPath());
		try {
			contentType = Files.probeContentType(path);
		} catch (IOException e) {
			log.warn("content type determination failed", e);
		}

		//TODO make this better
		if(fileName.endsWith("js") && contentType == null) {
			contentType = "application/javascript";
		} else if (fileName.endsWith("css") && contentType == null) {
			contentType = "text/css";
		}
		
		return getFileAsStream(request,response,dataStream,fileLength,fileName,fileLastModified,contentType);
	}
	
	/**
	 * Get the GridFSDBFile wrapped in a HttpServletResponse
	 * @param request
	 * @param response
	 * @param file
	 * @return
	 */
	public static HttpServletResponse getGridFSDBFileAsStream(HttpServletRequest request, HttpServletResponse response, GridFSDBFile file) {
		//TODO GridFSDBFile or GridFSFile
		InputStream dataStream = file.getInputStream();
		long fileLength = file.getLength();
		String fileName = file.getFilename();
		Date dateLastModified = file.getUploadDate();
		String contentType = file.getContentType();
		long fileLastModified = 0L;
		
		if(dateLastModified != null) {
			fileLastModified = dateLastModified.getTime();
		}
		
		return getFileAsStream(request,response,dataStream,fileLength,fileName,fileLastModified,contentType);
	}
	
	/**
	 * Parse the httpServletRequest for the file item that was received within a 
	 * multipart/form-data POST request
	 * @param httpServletRequest
	 * @return the file item
	 */
	public static DiskFileItem parseRequestForFileItem(HttpServletRequest httpServletRequest) {
		DiskFileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);

		// Parse the request for file
		List<FileItem> items = null;
		try {
			items = upload.parseRequest(httpServletRequest);
		} catch (FileUploadException fue) {
			log.error("Error reading/parsing file", fue);
		}

		if (items.size() != 1) {
			log.debug("there should only be one file here.");
			for(FileItem x : items) {
				log.debug("item: "+x.toString());
			}
		}
		DiskFileItem dfi = (DiskFileItem) items.get(0);
		log.debug("parseRequestForFile - store location: `"+dfi.getStoreLocation()+"'");		
		log.debug("dfi content type: "+dfi.getContentType());
		return dfi;
	}

	/**
	 * Parse the request headers, build the response, stream back file
	 * @param request
	 * @param response
	 * @param dataStream
	 * @param fileLength
	 * @param fileName
	 * @param fileLastModified
	 * @param contentType
	 * @return
	 */
	private static HttpServletResponse getFileAsStream(HttpServletRequest request, HttpServletResponse response,
			InputStream dataStream, long fileLength, String fileName, long fileLastModified, String contentType) {
		if (dataStream == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return response;
		}

		if (StringUtils.isEmpty(fileName) || fileLastModified == 0L) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return response;
		}

		String ifNoneMatch = request.getHeader("If-None-Match");
		if (ifNoneMatch != null && matches(ifNoneMatch, fileName)) {
			response.setHeader("ETag", fileName);
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return response;
		}

		long ifModifiedSince = request.getDateHeader("If-Modified-Since");
		if (ifNoneMatch == null && ifModifiedSince != -1 && ifModifiedSince + 1000 > fileLastModified) {
			response.setHeader("ETag", fileName);
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return response;
		}

		String ifMatch = request.getHeader("If-Match");
		if (ifMatch != null && !matches(ifMatch, fileName)) {
			response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
			return response;
		}

		long ifUnmodifiedSince = request.getDateHeader("If-Unmodified-Since");
		if (ifUnmodifiedSince != -1 && ifUnmodifiedSince + 1000 <= fileLastModified) {
			response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
			return response;
		}

		Range full = new Range(0, fileLength - 1, fileLength);
		List<Range> ranges = new ArrayList<Range>();
		String range = request.getHeader("Range");
		if (range != null) {
			if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
				response.setHeader("Content-Range", "bytes */" + fileLength);
				response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
				return response;
			}

			String ifRange = request.getHeader("If-Range");
			if (ifRange != null && !ifRange.equals(fileName)) {
				try {
					long ifRangeTime = request.getDateHeader("If-Range");
					if (ifRangeTime != -1) {
						ranges.add(full);
					}
				} catch (IllegalArgumentException ignore) {
					ranges.add(full);
				}
			}

			if (ranges.isEmpty()) {
				for (String part : range.substring(6).split(",")) {
					// Assuming a file with length of 100, the following
					// examples returns bytes at:
					// 50-80 (50 to 80), 40- (40 to length=100), -20
					// (length-20=80 to length=100).
					long start = sublong(part, 0, part.indexOf("-"));
					long end = sublong(part, part.indexOf("-") + 1, part.length());

					if (start == -1) {
						start = fileLength - end;
						end = fileLength - 1;
					} else if (end == -1 || end > fileLength - 1) {
						end = fileLength - 1;
					}

					// Check if Range is syntactically valid. If not, then
					// return 416.
					if (start > end) {
						response.setHeader("Content-Range", "bytes */" + fileLength); // Required
																						// in
																						// 416.
						response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
						return response;
					}

					// Add range.
					ranges.add(new Range(start, end, fileLength));
				}
			}
		}

		// Get content type by file name and set content disposition.
		String disposition = "inline";

		// If content type is unknown, then set the default value.
		// For all content types, see:
		// http://www.w3schools.com/media/media_mimeref.asp
		// To add new content types, add new mime-mapping entry in web.xml.
		if (contentType == null) {
			contentType = "application/octet-stream";
		} else if (!contentType.startsWith("image")) {
			// Else, expect for images, determine content disposition. If
			// content type is supported by
			// the browser, then set to inline, else attachment which will pop a
			// 'save as' dialogue.
			String accept = request.getHeader("Accept");
			disposition = accept != null && accepts(accept, contentType) ? "inline" : "attachment";
		}

		// Initialize response.
		response.reset();
		response.setBufferSize(HttpUtils.DEFAULT_BUFFER_SIZE);
		response.setHeader("Content-Disposition", disposition + ";filename=\"" + fileName + "\"");
		response.setHeader("Accept-Ranges", "bytes");
		response.setHeader("ETag", fileName);
		response.setDateHeader("Last-Modified", fileLastModified);
		response.setDateHeader("Expires", System.currentTimeMillis() + HttpUtils.DEFAULT_EXPIRE_TIME);

		// Send requested file (part(s)) to client
		// ------------------------------------------------

		// Prepare streams.
		InputStream input = null;
		OutputStream output = null;

		try {
			// Open streams.
			input = new BufferedInputStream(dataStream);
			output = response.getOutputStream();

			if (ranges.isEmpty() || ranges.get(0) == full) {

				// Return full file.
				Range r = full;
				response.setContentType(contentType);
				response.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);
				response.setHeader("Content-Length", String.valueOf(r.length));
				copy(input, output, fileLength, r.start, r.length);

			} else if (ranges.size() == 1) {

				// Return single part of file.
				Range r = ranges.get(0);
				response.setContentType(contentType);
				response.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);
				response.setHeader("Content-Length", String.valueOf(r.length));
				response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

				// Copy single part range.
				copy(input, output, fileLength, r.start, r.length);

			} else {

				// Return multiple parts of file.
				response.setContentType("multipart/byteranges; boundary=" + HttpUtils.MULTIPART_BOUNDARY);
				response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

				// Cast back to ServletOutputStream to get the easy println
				// methods.
				ServletOutputStream sos = (ServletOutputStream) output;

				// Copy multi part range.
				for (Range r : ranges) {
					// Add multipart boundary and header fields for every range.
					sos.println();
					sos.println("--" + HttpUtils.MULTIPART_BOUNDARY);
					sos.println("Content-Type: " + contentType);
					sos.println("Content-Range: bytes " + r.start + "-" + r.end + "/" + r.total);

					// Copy single part range of multi part range.
					copy(input, output, fileLength, r.start, r.length);
				}

				// End with multipart boundary.
				sos.println();
				sos.println("--" + HttpUtils.MULTIPART_BOUNDARY + "--");
			}
		} catch (Exception e) {
			log.error("get file as stream failed", e);
		} finally {
			// Gently close streams.
			close(output);
			close(input);
			close(dataStream);
		}
		return response;
	}
}