package de.l3s.streamcorpus;

import java.io.IOException;

import org.terrier.indexing.Collection;
import org.terrier.indexing.Document;

public class STCollection implements Collection {

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean endOfCollection() {
		// TODO Auto-generated method stub
		return false;
	}

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
	public void reset() {
		// TODO Auto-generated method stub

	}

}
