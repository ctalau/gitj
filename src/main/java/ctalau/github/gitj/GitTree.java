package ctalau.github.gitj;

import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

/**
 * Representation of a Git tree object.
 * 
 * @author ctalau
 */
public class GitTree {
  /**
   * A Git tree has two kinds of entries: blobs and other trees.
   */
  public static enum EntryType {
    TREE,
    BLOB
  }
  
  /**
   * Mapping from an entry name to the line in the tree descriptor.
   */
  private final Map<String, String> entries;

  /**
   * Constructor.
   * 
   * @param treeEntires The lines of the tree descriptor.
   */
  public GitTree(String[] treeEntires) {
    entries = Maps.newHashMapWithExpectedSize(treeEntires.length + 1);
    for (String treeEntry : treeEntires) {
      String escapedEntryName = treeEntry.split("\t")[1];
      String entryName = Unescaper.unescapeCStringLiteral(escapedEntryName);
      entries.put(entryName, treeEntry);
    }
  }
  
  /**
   * Removes an entry from the tree.
   * 
   * @param name The name of the entry.
   */
  public void removeEntry(String name) {
    entries.remove(name);
  }

  /**
   * Return the SHA of an entry.
   *
   * @param name The name of the entry.
   * @return The SHA of the entry.
   */
  public String getEntrySha(String name) {
    String sha = null;
    String entry = entries.get(name);
    if (entry != null) {
      String details = entry.split("\t")[0];
      sha = details.split(" ")[2];
    }
    return sha;
  }
  
  /**
   * Updates a new entry with the given details.
   * 
   * @param name The name of the new entry.
   * @param sha The SHA of the new entry.
   * @param type The type of the new entry.
   */
  public void updateEntry(String name, String sha, EntryType type) {
    String escapedName;
    if (entries.containsKey(name)) {
      escapedName = entries.get(name).split("\t")[1];
    } else {
      escapedName = name;
    }
    String entryDescriptor = getEntryDescriptor(sha, type, escapedName);
    entries.put(name, entryDescriptor);
  }

  /**
   * Create an entry descriptor.
   * 
   * @param sha The SHA of the entry.
   * @param type The type of the entry.
   * @param escapedName The escaped name of the entry.
   * @return
   */
  private String getEntryDescriptor(String sha, EntryType type, String escapedName) {
    String entryDescriptor = null;
    switch (type) {
    case TREE:
      entryDescriptor = "040000 tree " + sha  + "\t" + escapedName;
      break;
    case BLOB:
      entryDescriptor = "100644 blob " + sha  + "\t" + escapedName;      
      break;
    }
    return entryDescriptor;
  }


  /**
   * Returns a string representation of the Git tree.
   */
  @Override
  public String toString() {
    return Joiner.on("\n").join(entries.values());
  }
}
