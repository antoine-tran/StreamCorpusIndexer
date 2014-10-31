package de.l3s.streamcorpus;

import java.io.Reader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.terrier.indexing.Document;


/**
 * Wrap the streamcorpus' StreamItem object by Terrier's Document interface
 * 
 * @author tuan
 *
 */
public class StreamItemDocument implements Document {
	
	/** Use a check-ahead pointer to get to the end of the content in current fields */
	private boolean endOfField;
	
	/** Check the current field */
	private int curFieldNo;
	
	/**
	 * Fields to be indexed:
	 * - Document title / anchor,... (in other_content field)
	 * - domain of the url (either abs_url or original_url)
	 * - Sentences in raw, after applying Morphadorner lemmatizer
	 * - POS tags annotated by Serif Tagger
	 */
	private Set<String> _fields;
	
	/** How many sentences this doc has */
	private int sentenceNo;
	


	@Override
	public Set<String> getFields() {
		if (_fields == null) {
			_fields = new HashSet<String>();
			_fields.add("other_content");
			_fields.add("domain");
			_fields.add("raw");
			_fields.add("serif");
		}
		return _fields;
	}

	@Override
	public boolean endOfDocument() {		
		return (endOfField) && (curFieldNo == _fields.size());
	}

	@Override
	public Reader getReader() {		
		/*throw new UnsupportedOperationException("StreamItem document is not"
				+ " backed by a java.io.Reader stream");*/
		return null;
	}
	
	@Override
	public String getProperty(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> getAllProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNextTerm() {
		// TODO Auto-generated method stub
		return null;
	}
}
