package org.terrier.structures.indexing.singlepass.hadoop;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.lib.CombineFileSplit;
import org.apache.log4j.Logger;
import org.terrier.indexing.Collection;
import org.terrier.indexing.CollectionFactory;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.io.CountingInputStream;

public class XZFileCollectionRecordReader extends FileCollectionRecordReader {

	 /** The logger used */
    protected static final Logger logger = Logger.getLogger(XZFileCollectionRecordReader.class);
    
	public XZFileCollectionRecordReader(JobConf jobConf,
			PositionAwareSplit<CombineFileSplit> split) throws IOException {
		super(jobConf, split);
	}

	/** Opens a collection on the next file. */
	@Override
	protected Collection openCollectionSplit(int index) throws IOException
	{
		if (index >= ((CombineFileSplit)split.getSplit()).getNumPaths())
		{
			//no more splits left to process
			return null;
		}
		Path file = ((CombineFileSplit)split.getSplit()).getPath(index);
		logger.info("Opening "+file);
		long offset = 0;//TODO populate from split?
		FileSystem fs = file.getFileSystem(config);


		//WT2G collection has incorrectly named extensions. Terrier can deal with this,
		//Hadoop cant
		CompressionCodec codec = compressionCodecs.getCodec(
			new Path(file.toString().replaceAll("\\.GZ$", ".gz")));
		
		length = fs.getFileStatus(file).getLen();
		FSDataInputStream _input = fs.open(file); //TODO: we could use utility.Files here if
		//no codec was found	
		InputStream internalInputStream = null;
		start = offset;
		
		if (codec !=null)
		{
			start = 0;
			inputStream = new CountingInputStream(_input);
			internalInputStream = codec.createInputStream(inputStream);
		} 
		
		// Tuan: Add this part
		// If the file is LZMA2 compressed, invoke the XZCompressionInput
		else if (file.toString().endsWith(".xz")) {
			inputStream = new CountingInputStream(_input);
			internalInputStream = new XZCompressorInputStream(new BufferedInputStream(_input));			
		}
		
		// Invoke the normal Collection (default TRECCollection) here
		else 
		{
			if (start != 0) //TODO: start is always zero? 
			{
		        --start;
		        _input.seek(start);
			}
			internalInputStream = inputStream = new CountingInputStream(_input, start);
		}
		Collection rtr = CollectionFactory.loadCollection(
			ApplicationSetup.getProperty("trec.collection.class", "StreamCorpusCollection"), 
			new Class[]{FileSystem.class}, 
			new Object[]{internalInputStream});

		if (rtr == null)
		{
			throw new IOException("Collection did not load properly");
		}
		return rtr;
	}
}
