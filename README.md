StreamCorpusIndexer
===================

Indexing TREC StreamCorpus using Terrier. Test with Stream Item version  0.3.0 and Terrier 4.0

This software includes a patch for Terrier HadoopIndexing to support reading .xz files in the collection.



How to install and run
===========
(More details come soon. If you can't wait to get the instruction, feel free to contact ttran AT L3S DOT de)

1. Compile: To compile the project, simply add Terrier-[version]-core.jar to the classpath. 


2. Configure: To index the StreamCorpus, follow the same steps for configuring Terrier in Hadoop settings (http://terrier.org/docs/v4.0/hadoop_configuration.html), including the index path ("terrier.index.path") and collection path ("collection.spec"). Set the collection class with:

     <code> trec.collection.class=de.l3s.streamcorpus.StreamCorpusCollection</code>

3. Run: Call HadoopIndexing with the similar set of arguments. That's it !
