package ctalau.github.gitj;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * Hello world!
 *
 */
public class GitRepository {
  /**
   * The git command executor for the current folder.
   */
  private GitCommandExecutor executor;

  public GitRepository(File location) {
    this.executor = new GitCommandExecutor(location);
  }

  /**
   * Returns a list of all available branches in the repository.
   * 
   * @return The list of all available branches.
   * 
   * @throws IOException
   * @throws InterruptedException
   */
  public List<String> listBranches() throws IOException, InterruptedException {
    String branchesOutput = executor.runGitCommand("branch");
    String[] branchesLines = splitInLines(branchesOutput);
    List<String> ret = Lists.newArrayListWithCapacity(branchesLines.length);
    for (String branchLine : branchesLines) {
      if (branchLine.length() >= 2) {
        ret.add(branchLine.substring(2));
      }
    }
    return ret;
  }

  /**
   * Split the string in lines.
   * 
   * @param string The string to split.
   * 
   * @return The lines array.
   */
  private String[] splitInLines(String string) {
    return string.split("\\r?\\n");
  }

  
  /**
   * Reads the content of a file on a branch without switching to it.
   * 
   * @param branch The branch in which we are interested.
   * @param path The path of the file that we want to read.
   * 
   * @return The content of the file.
   * 
   * @throws IOException
   * @throws InterruptedException
   */
  public String readFile(String branch, String path) throws IOException, InterruptedException {
    return executor.runGitCommand("show", branch + ":" + path);
  }
  
  /**
   * Lists the directory contents on a given branch.
   * 
   * @param branch The branch on which we want to list the folder entrie.
   * @param dirPath The path to the directory.
   * 
   * @return The list of directory entries.
   * 
   * @throws IOException
   * @throws InterruptedException
   */
  public List<String> listFiles(String branch, String dirPath) throws IOException, InterruptedException {
    String fileList = executor.runGitCommand("ls-tree", branch, dirPath + "/", "--name-only");
    List<String> escapedFilePaths = Arrays.asList(splitInLines(fileList));
    List<String> fileNames = Lists.newArrayListWithCapacity(escapedFilePaths.size());
    for (String escapedFilePath: escapedFilePaths) {
      String filePath = escapedFilePath;
      if (filePath.startsWith("\"")) {
        filePath = Unescaper.unescapeCStringLiteral(escapedFilePath);
      }
      int nameStartIndex = filePath.lastIndexOf(File.separatorChar) + 1;
      fileNames.add(filePath.substring(nameStartIndex));
    }
    return fileNames;
  }
  
  
  
}
