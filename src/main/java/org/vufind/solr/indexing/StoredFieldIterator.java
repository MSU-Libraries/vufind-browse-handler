package org.vufind.solr.indexing;

// Build a browse list by walking the docs in an index and extracting sort key
// and values from a pair of stored fields.
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiBits;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.vufind.util.BrowseEntry;
import org.vufind.util.Utils;

public class StoredFieldIterator extends SolrFieldIterator
{
    int currentDoc = 0;
    LinkedList<BrowseEntry> buffer;

    String sortField;
    String valueField;

    private Set<String> fieldSelection;

    private Bits liveDocsBitSet;

    public StoredFieldIterator(String indexPath, String field) throws Exception
    {
        super(indexPath, field);

        sortField = Utils.getEnvironment("SORTFIELD");
        valueField = Utils.getEnvironment("VALUEFIELD");

        if (sortField == null || valueField == null) {
            throw new IllegalArgumentException("Both SORTFIELD and " +
                                               "VALUEFIELD environment " +
                                               "variables must be set.");
        }

        fieldSelection = new HashSet<String>();
        fieldSelection.add(sortField);
        fieldSelection.add(valueField);
        fieldSelection.add("id");   // make Solr id available for error messages

        reader = DirectoryReader.open(FSDirectory.open(new File(indexPath).toPath()));

        // Will be null if the index contains no deletes.
        liveDocsBitSet = MultiBits.getLiveDocs(reader);

        buffer = new LinkedList<BrowseEntry> ();
    }


    private void loadDocument(IndexReader reader, int docid) throws IOException
    {
        Document doc = reader.storedFields().document(currentDoc, fieldSelection);

        String[] sort_key = doc.getValues(sortField);
        String[] value = doc.getValues(valueField);

        if (sort_key.length == value.length) {
            for (int i = 0; i < value.length; i++) {
                buffer.add(new BrowseEntry(buildSortKey(sort_key[i]),
                                           sort_key[i],
                                           value[i]));
            }
        } else {
            String id = null;
            IndexableField idField = doc.getField("id");
            if (idField != null) {
                /*
                 * Assumes id is defined as type string in Solr schema.
                 * Should be safe for VuFind.
                 */
                id = idField.stringValue();
            }
            System.err.println("Skipped entries for doc #" + docid +
                               " (id:" + id + "):" +
                               " the number of sort keys didn't" +
                               " match the number of stored values.");
        }
    }


    protected BrowseEntry readNext() throws IOException
    {
        while (buffer.isEmpty()) {
            if (currentDoc < reader.maxDoc()) {
                if (this.liveDocsBitSet == null || this.liveDocsBitSet.get(currentDoc)) {
                    loadDocument(reader, currentDoc);
                }
                currentDoc++;
            } else {
                return null;
            }
        }

        return buffer.remove();
    }
}

