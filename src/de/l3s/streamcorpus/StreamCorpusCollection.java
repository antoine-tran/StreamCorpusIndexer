package de.l3s.streamcorpus;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.terrier.indexing.Collection;
import org.terrier.indexing.Document;
import org.terrier.utility.io.CountingInputStream;

import streamcorpus.StreamItem;

/**
 * Models a collections of StreamCorpus .sc files (possibly compressed, either
 * in XZ or other formats) using Terrier Collection model
 * 
 * @author tuan
 *
 */
public class StreamCorpusCollection implements Collection, Iterator<Document> {

	private static final Logger logger = Logger.getLogger(StreamCorpusCollection.class);

	/** Filename of current file */
	protected String currentFilename;

	/** properties for the current document */	
	protected Map<String,String> DocProperties = null;

	/** Cached stream item */
	private StreamItem item;

	/** The index in the FilesToProcess of the currently processed file.*/
	protected int FileNumber = -1;

	/** list of files to be processed */
	private ArrayList<Path> files = new ArrayList<>();

	/** Indicates whether the end of the collection has been reached.*/
	protected boolean endOfCollection = false;

	/** The reference to raw input stream to move ahead in the collection */
	protected CountingInputStream br;

	/** The registered protocol to read Thrift data */
	private TTransport transport;
	private TBinaryProtocol tp;

	/** Underlying reading streams */
	private FSDataInputStream fis;
	private BufferedInputStream bis;
	private XZCompressorInputStream xzis;

	/** file system in Hadoop cluster */
	private FileSystem fs;

	public StreamCorpusCollection(FileSystem fileSystem, List<FileStatus> inputs) {
		this.fs = fileSystem;
		files = new ArrayList<>(inputs.size());
		for (FileStatus input : inputs) {
			files.add(input.getPath());
		}

		//open the first file
		try {
			openNextFile();
		} catch (IOException ioe) {
			logger.error("IOException opening first file of collection " +
					"- is the collection.spec correct?", ioe);
		}
	}

	/**
	 * Check whether it is the end of the collection
	 * @return boolean
	 */
	public boolean hasNext() {
		return !endOfCollection();
	}

	/**
	 * Return next document
	 * @return next document
	 */
	public Document next() {
		if (!nextDocument()) {
			return null;
		}
		return getDocument();
	}

	/**
	 * This is unsupported by this Collection implementation, and
	 * any calls will throw UnsupportedOperationException
	 * Throws UnsupportedOperationException on all invocations */
	public void remove() {
		throw new UnsupportedOperationException("Iterator.remove() not supported");
	}

	@Override
	public Document getDocument() {

		// after calling nextDocument() and get true,
		// the stream item should not be null
		if (item == null) {
			logger.warn("Moved to the next document but found an " +
					"invalid stream item. Doc: " + currentFilename);
			return null;
		}

		// load property
		DocProperties = new HashMap<String, String>();
		DocProperties.put("stream-id", item.getStream_id());
		DocProperties.put("url", item.getSchost());
		DocProperties.put("type", item.getSource());
		DocProperties.put("time", item.getStream_time().getZulu_timestamp());

		return new StreamItemDocument(item, DocProperties);
	}

	@Override
	public boolean nextDocument() {
		//move the stream to the start of the next document
		//try next file if error / eof encountered. (and set endOfCOllection if
		// needed)
		boolean bScanning = true;
		scanning:
			while (bScanning) {

				try {
					// value
					if (fis.available() > 0) {
						try {
							item.read(tp);
						} catch (TTransportException e) {				
							int type = e.getType();
							if (type == TTransportException.END_OF_FILE) {
								if (openNextFile()) {
									continue scanning;
								} else {
									endOfCollection = true;
									return false;
								}
							}
						} catch (TException e) {
							logger.warn("Error reading " + files.get(FileNumber)
									+ ": " + currentFilename 
									+ ". Skip the rest..");
							continue scanning;
						}			

					} else if (openNextFile()) {
						continue scanning;
					} else {
						endOfCollection = true;
						return false;
					}
				} catch (IOException e) {
					logger.warn("Error reading " + files.get(FileNumber)
							+ ": " + currentFilename + ". Skip");
					continue scanning;
				}
				bScanning = false;
			}

		if (item != null) {
			return true;
		}
		else if (endOfCollection) {
			return false;
		}
		else {
			throw new RuntimeException("Error getting next doc in file "
					+ currentFilename);
		}
	}

	@Override
	public boolean endOfCollection() {
		return endOfCollection;
	}

	protected boolean openNextFile() throws IOException {
		// close the currently open file
		if (br != null && files.size() > 0) {
			close();
		}

		// keep trying files
		boolean tryFile = true;
		boolean rtr = false;

		while (tryFile) {
			if (FileNumber < files.size() - 1) {
				FileNumber++;
				Path p = files.get(FileNumber);

				if (!fs.exists(p)) {
					logger.warn("File " + p + " not found");
				}
				else {
					fis = fs.open(p);
					br = new CountingInputStream(fis);
					bis = new BufferedInputStream(fis);
					xzis = new XZCompressorInputStream(bis);
					transport = new TIOStreamTransport(xzis);

					try {
						transport.open();
					} catch (TTransportException e) {
						logger.error("Couldn't open file " + p.toString());
						e.printStackTrace();
						transport = null;
					}

					if (transport != null) {
						tp = new TBinaryProtocol(transport);
						currentFilename = p.toString();
						tryFile = false;
						rtr = true;
					}
				}
			} else {
				//last file of the collection has been read, EOC
				endOfCollection = true;
				rtr = false;
				tryFile = false;
			}
		}
		return rtr;
	}

	@Override
	public void reset() {
		FileNumber = -1; 
		endOfCollection = false;
		try {
			openNextFile();
		} catch (IOException ioe) {
			logger.warn("IOException while resetting collection - ie re-opening first file", ioe);
		}

	}


	@Override
	public void close() throws IOException {
		if (transport != null) transport.close();
		if (xzis != null) xzis.close();
		if (bis != null) bis.close();
		if (fis != null) fis.close();
		if (br != null) br.close();
	}
}
