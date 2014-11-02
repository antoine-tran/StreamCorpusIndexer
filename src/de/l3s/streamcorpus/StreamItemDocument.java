package de.l3s.streamcorpus;

import java.io.Reader;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.terrier.indexing.Document;

import streamcorpus.ContentItem;
import streamcorpus.Sentence;
import streamcorpus.StreamItem;
import streamcorpus.Token;


/**
 * Wrap the streamcorpus' StreamItem object by Terrier's Document interface
 * 
 * @author tuan
 *
 */
public class StreamItemDocument implements Document {

	/** 
	 * The tokens of are organized in 3 dimensions: 
	 * Fields, sentences in each field, and tokens in each sentences
	 * Use the pointers to check the position of the cursor in each
	 * dimensions, to mark when reaching the end of document
	 */
	private ContentItem curField;
	private int fieldCursor;

	private Sentence curSentence;
	private int sentenceCursor;

	private Token curToken;
	private int tokenCursor;
	
	/** Are we in the lemma part or the raw part of the field set ? */
	private boolean isLemma;

	/**
	 * Fields to be indexed:
	 * - Document title / anchor,... (in other_content field)
	 * - Sentences in raw
	 * - POS tags annotated by Serif Tagger
	 */
	private LinkedHashSet<String> fields;

	/** document meta-data */
	protected Map<String, String> properties;	

	/** reference to the root stream item to move the cursors along the 3 dimensions */
	private StreamItem item; 

	public StreamItemDocument(StreamItem item) {
		this.item = item;
		initFields();
		tokenCursor = -1;
		sentenceCursor = -1;
		fieldCursor = -1;
	}

	@Override
	public Set<String> getFields() {
		if (fields == null) {
			initFields();
		}
		return fields;
	}

	private void initFields() {
		fields = new LinkedHashSet<String>();

		Set<String> fieldsInItem = item.getOther_content().keySet();

		// Double check this
		if (fieldsInItem.contains("title")) {
			fields.add("title");
		}

		if (fieldsInItem.contains("anchor")) {
			fields.add("anchor");
		}

		fields.add("content");

		// Lemma part
		if (fieldsInItem.contains("title")) {
			fields.add("title_lemma");
		}

		if (fieldsInItem.contains("anchor")) {
			fields.add("anchor_lemma");
		}

		fields.add("content_lemma");
		
		// Serif-annotated part. Optional: Only applied to StreamCorpus 
		// collections that have (Serif) annotations
		if (fieldsInItem.contains("title")) {
			fields.add("title_serif");
		}

		if (fieldsInItem.contains("anchor")) {
			fields.add("anchor_serif");
		}

		fields.add("content_serif");
	}

	@Override
	public boolean endOfDocument() {		
		return (endOfSentence() && endOfField() && (fieldCursor == fields.size()));
	}

	private boolean endOfSentence() {
		return (tokenCursor == curSentence.tokens.size());
	}

	private boolean endOfField() {
		return (sentenceCursor == curField.getSentencesSize());
	}

	@Override
	public Reader getReader() {		
		/*throw new UnsupportedOperationException("StreamItem document is not"
				+ " backed by a java.io.Reader stream");*/
		return null;
	}

	/** Allows access to a named property of the Document. Examples might be URL, filename etc.
	 * @param name Name of the property. It is suggested, but not required that this name
	 * should not be case insensitive.
	 * @since 1.1.0 */
	public String getProperty(String name)
	{
		return properties.get(name.toLowerCase());
	}

	/** Allows a named property to be added to the Document. Examples might be URL, filename etc.
	 * @param name Name of the property. It is suggested, but not required that this name
	 * should not be case insensitive.
	 * @param value The value of the property
	 * @since 1.1.0 */
	public void setProperty(String name, String value)
	{
		properties.put(name.toLowerCase(),value);
	}

	/** Returns the underlying map of all the properties defined by this Document.
	 * @since 1.1.0 */	
	public Map<String,String> getAllProperties()
	{
		return properties;
	}

	@Override
	// Order of traversing: title, anchor, raw, serif
	// Lazy move of cursors in 3 dimensions in-side out
	public String getNextTerm() {
		if (curToken == null) {
			if (!hashNextToken()) {
				return null;
			}			
		}
		return (isLemma) ? curToken.getLemma() : curToken.getToken();
	}
	
	private boolean hasNextToken() {
		if (endOfSentence()) {
			if (!hasNextSentence()) {
				return false;
			} else tokenCursor = -1;
		}
		tokenCursor++;
		curToken = curSentence.getTokens().get(tokenCursor);
	}
	
	private boolean hasNextSentence() {
		if (endOfField()) {
			if (!hasNextField()) {
				return false;
			} else sentenceCursor = -1;
		}
		sentenceCursor++;
		curSentence = curField.getSentences().get("").get(sentenceCursor);
	}
	
	private boolean hashNextField() {
		if (endOfDocument()) {
			return false;
		}
	}
}
