package de.l3s.streamcorpus;

import java.io.Reader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.terrier.indexing.Document;

import streamcorpus.ContentItem;
import streamcorpus.EntityType;
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
	 * Sections, sentences in each field, and tokens in each sentences
	 * Use the pointers to check the position of the cursor in each
	 * dimensions, to mark when reaching the end of document
	 */
	private ContentItem curSection;

	private Sentence curSentence;
	private int sentenceCursor;

	private Token curToken;
	private int tokenCursor;

	/** Currently supported taggers: Serif, Lingpipe */
	private static enum TAGGER { Serif, Lingpipe };
	private TAGGER curTagger;	

	/**
	 * Fields to be indexed:
	 * - Document title (in other_content field)
	 * - Sentences in raw
	 * - POS tags annotated by Serif Tagger
	 */
	private static enum INDEXABLE {Title, Body, Lemma,
		Serif_PER, Serif_ORG, Serif_LOC, Serif_MISC,
		Lingpipe_PER, Lingpipe_ORG, Lingpipe_LOC, Lingpipe_MISC}; 

		// The stack of fields that the current term should be indexed to
		// For all the tokens, we first index into "Lemma" field, for which the constant Lemma is used.
		// Then we	
		private Set<String> curFields;
		private String titleOrBody;


		// A flag to keep the states of current token indexing: 0 - none indexed, 1 - lemma checked
		// (indexed or not), 2 - token checked
		private int tokenCheckState;


		/** document meta-data */
		protected Map<String, String> properties;	

		/** reference to the root stream item to move the cursors along the 3 dimensions */
		private StreamItem item; 

		public StreamItemDocument(StreamItem item) {
			this.item = item;
			curFields = new HashSet<>();
			tokenCursor = -1;
			sentenceCursor = -1;
			tokenCheckState = -1;
		}

		@Override
		public Set<String> getFields() {
			return curFields;
		}

		@Override
		public boolean endOfDocument() {		
			return (endOfSentence() && endOfSection() && (curSection == item.getBody()));
		}

		private boolean endOfSentence() {
			return (tokenCursor == curSentence.tokens.size());
		}

		private boolean endOfSection() {
			return (sentenceCursor == curSection.getSentencesSize());
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
			curFields.clear();
			String t = null;

			// keep fetching the next valid token
			if (internalNextToken()) {
				if (tokenCheckState == -1) {
					tokenCheckState = 0;
					t = curToken.getLemma();
					if (t.length() > 1 || t.equals("i")) {
						curFields.add(INDEXABLE.Lemma.toString());
						return t;
					} else curFields.clear();
				}
				if (tokenCheckState == 0) {
					if (curTagger == TAGGER.Serif) {
						curFields.add(titleOrBody);
					}
					EntityType type = curToken.getEntity_type();
					addField(type);
					t = curToken.getToken();
					tokenCheckState = 1;
					return t;
				}
				else throw new RuntimeException("Invalid state when checking token: "
						+ curToken + ", " + tokenCheckState);	
			}
			
			// This happens only if the document is empty
			return null;
		}

		private void addField(EntityType type) {
			if (type == EntityType.PER) curFields.add((curTagger == TAGGER.Serif) 
					? INDEXABLE.Serif_PER.toString()
							: INDEXABLE.Lingpipe_PER.toString());
			if (type == EntityType.ORG) curFields.add((curTagger == TAGGER.Serif) 
					? INDEXABLE.Serif_ORG.toString()
							: INDEXABLE.Lingpipe_ORG.toString());
			if (type == EntityType.LOC) curFields.add((curTagger == TAGGER.Serif) 
					? INDEXABLE.Serif_LOC.toString()
							: INDEXABLE.Lingpipe_LOC.toString());
			if (type == EntityType.MISC) curFields.add((curTagger == TAGGER.Serif) 
					? INDEXABLE.Serif_MISC.toString()
							: INDEXABLE.Lingpipe_MISC.toString());
		}

		// Keep fetching the next valid token 
		private boolean internalNextToken() {
			if (tokenCheckState == 0 && curToken != null) {
				return true;
			}
			else if (tokenCheckState == 1) {
				if (endOfSentence()) {
					if (!internalNextSentence()) {
						return false;
					} else {
						tokenCursor = -1;
						tokenCheckState = -1;
					}
				}
				tokenCursor++;
				curToken = curSentence.getTokens().get(tokenCursor);
				return true;
			}
			
			// Check StreamItem for the first time
			else if (tokenCheckState == -1 || curToken == null) {
				if (!internalNextSentence()) {
					return false;
				} else {
					tokenCursor = -1;
					tokenCheckState = -1;
				}
				tokenCursor++;
				curToken = curSentence.getTokens().get(tokenCursor);
				return true;
			}
			
			else throw new RuntimeException("Error fetching the next token: "
					+ curToken + ", " + tokenCheckState);
		}

		// Fetch the next sentence, or get the first sentence 
		private boolean internalNextSentence() {
			
			if (sentenceCursor == -1 || curSentence == null) {
				if (!internalNextSection()) {
					return false;
				} else {
					sentenceCursor = -1;
					
				}
			}
			
			if (endOfSection()) {
				if (!internalNextSection()) {
					return false;
				} else sentenceCursor = -1;
			}
			sentenceCursor++;
			curSentence = curSection.getSentences().get("").get(sentenceCursor);
			return true;
			
		}

		private boolean internalNextSection() {
			if (endOfDocument()) {
				return false;
			}
		}
}
