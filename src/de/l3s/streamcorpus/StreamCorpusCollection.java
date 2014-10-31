package de.l3s.streamcorpus;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransport;
import org.terrier.indexing.Collection;
import org.terrier.indexing.Document;
import org.terrier.utility.io.CountingInputStream;

/**
 * Models a collections of StreamCorpus .sc files (possibly compressed, either
 * in XZ or other types) using Terrier Collection model
 * 
 * @author tuan
 *
 */
public class StreamCorpusCollection implements Collection {

	private static final Logger logger = Logger.getLogger(StreamCorpusCollection.class);

	/** Filename of current file */
	protected String currentFilename;

	/** Counts the number of documents that have been found in this file. */
	protected int documentsInThisFile = 0;

	/** properties for the current document */	
	protected Map<String,String> DocProperties = null;

	/** Tag names for tags that should be added as properties **/
	String[] propertyTags = new String[0];

	/**
	 * Counts the documents that are found in the collection, ignoring those
	 * documents that appear in the black list
	 */
	protected int documentCounter = 0;

	/** The index in the FilesToProcess of the currently processed file.*/
	protected int FileNumber = -1;

	/** The string identifier of the current document.*/
	protected String ThisDocID;

	/** Indicates whether the end of the collection has been reached.*/
	protected boolean endOfCollection = false;

	/** The reference to raw input stream to move ahead in the collection */
	protected CountingInputStream br;

	/** The registered protocol to read Thrift data */
	TTransport transport;
	TBinaryProtocol tp;

	@Override
	public Document getDocument() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean nextDocument() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean endOfCollection() {
		return endOfCollection;
	}
	
	protected boolean openNextFile() throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void reset() {
		FileNumber = -1; 
		endOfCollection = false;
		ThisDocID = "";
		try {
			openNextFile();
		} catch (IOException ioe) {
			logger.warn("IOException while resetting collection - ie re-opening first file", ioe);
		}

	}


	@Override
	public void close() throws IOException {
		if (transport != null) transport.close();
		if (br != null) br.close();
	}
}
