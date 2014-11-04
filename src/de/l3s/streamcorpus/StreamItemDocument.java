package de.l3s.streamcorpus;

import java.io.Reader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static Logger logger = LoggerFactory.getLogger(StreamItemDocument.class);
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
		private boolean contentIndexed;


		// A flag to keep the states of current token indexing: -1 - none indexed, 0 - lemma checked
		// (indexed or not), 1 - token checked
		private int tokenCheckState;

		/** document meta-data */
		protected Map<String, String> properties;	

		/** reference to the root stream item to move the cursors along the 3 dimensions */
		private StreamItem item; 

		public StreamItemDocument(StreamItem item, Map<String, String> docProps) {
			this.item = item;
			this.properties = docProps;

			curFields = new HashSet<>();
			tokenCursor = -1;
			sentenceCursor = -1;
			tokenCheckState = -1;
			curTagger = null;
			contentIndexed = false;
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

			// We've not yet checked any token of the sentence, assumming the sentence
			// will not be empty, then we are not at the end of it.
			if (tokenCursor == -1) {
				return false;
			}
			if (curSentence != null && curSentence.tokens != null) {
				return (tokenCursor == curSentence.tokens.size());
			} 

			// NOTE: Cases are that the whole document is empty (and so getNextTerm()
			// will have to detect that)
			else {
				return false;
			}
		}

		private boolean endOfSection() {

			// We've not yet checked any token of the sentence, assumming the sentence
			// will not be empty, then we are not at the end of it.
			if (sentenceCursor == -1) {
				return false;
			}
			if (curSection != null) {
				return (sentenceCursor == curSection.getSentencesSize());	
			}
			
			// NOTE: Cases are that the whole document is empty (and so getNextTerm()
			// will have to detect that)
			else {
				return false;
			}
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
			// Note: After successfully calling internalNextToken(), the
			// tokenCheckState should never be 1
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
					if (!contentIndexed) {
						curFields.add(titleOrBody);
					}
					EntityType type = curToken.getEntity_type();
					addField(type);
					t = curToken.getToken();
					tokenCheckState = 1;
					return t;
				}
				else {
					
					throw new RuntimeException("Invalid state when checking token: "
							+ curToken + ", " + tokenCheckState);					
				}
						
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

		// Fetch the next token, and at the same time move the token cursor
		private boolean internalNextToken() {			
			if (tokenCheckState == 0 && curToken != null) {
				return true;
			}
			else if (tokenCheckState == 1) {

				if (endOfSentence()) {

					// if endOfSentence() returns true, the token 
					// list should never be empty
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

		// Repeatedly fetch the next non-empty sentence, 
		// and change curSentence and sentenceCursor at the same time
		private boolean internalNextSentence() {

			while (curSentence == null || curSentence.getTokens().size() == 0) {
				if (curSentence == null || endOfSection()) {

					// NOTE: If internalNextSection() returns true, curTagger should never be null
					if (!internalNextSection()) {
						return false;
					} else {
						sentenceCursor = -1;
					}
				}
				sentenceCursor++;
				if (curTagger == TAGGER.Serif) {
					curSentence = curSection.getSentences().get("serif").get(sentenceCursor);
				} else if (curTagger == TAGGER.Lingpipe) {
					curSentence = curSection.getSentences().get("lingpipecounter").get(sentenceCursor);
				} else{
					throw new RuntimeException("Unknown tagger: " + curTagger);
				}
			}
			return false;
		}

		private boolean internalNextSection() {
			if (endOfDocument()) {
				return false;
			}

			// First time --> go to other_anchor
			if (curSection == null) {
				Map<String, ContentItem> metas = item.getOther_content();
				if (metas.containsKey("title")) {
					curSection = metas.get("title");
					titleOrBody = "title";
				} else {
					curSection = item.getBody();
					titleOrBody = "body";
				}
				contentIndexed = false;
				curTagger = null;
			}
			if (curTagger == null) {
				if (curSection.getSentences().containsKey("serif")) {
					curTagger = TAGGER.Serif;
					return true;
				}
				else if (curSection.getSentences().containsKey("lingpipecounter")) {
					curTagger = TAGGER.Lingpipe;
					return true;
				}
				else if (curSection == item.getBody()) {
					return false;
				}
				else {
					// what to do if the body has no tagger ?
					logger.info(" We are " + ((curSection == item.getBody()) ? " in body" : " not in body" ));
					
					logger.info("Taggers available: ");
					for (String k : curSection.getSentences().keySet()) logger.info("[" + k + "]");
					
					throw new RuntimeException("Unknown tagger: " + curTagger);
				}
			}
			else if (curTagger == TAGGER.Serif 
					&& curSection.getSentences().containsKey("lingpipecounter")) {
				curTagger = TAGGER.Lingpipe;
				contentIndexed = true;
				return true;
			}
			else if ((curTagger == TAGGER.Serif 
					&& !curSection.getSentences().containsKey("lingpipecounter"))
					|| (curTagger == TAGGER.Lingpipe)) {
				if (curSection != item.getBody()) {
					curSection = item.getBody();
					contentIndexed = false;
					titleOrBody = "body";
					curTagger = null;
					return true;
				} else {
					return false;
				}
			}
			else throw new RuntimeException("Unknown tagger transition (Old: " + curTagger + ")");
		}
}
