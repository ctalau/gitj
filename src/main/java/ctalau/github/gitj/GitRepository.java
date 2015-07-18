package ctalau.github.gitj;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

import ctalau.github.gitj.GitCommandExecutor.ProcessExitException;
import ctalau.github.gitj.GitTree.EntryType;

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
   * Return the SHA of the latest commit on the given branch.
   * 
   * @param branch The name of the local branch.
   * @return The SHA of the latest commit.
   * 
   * @throws IOException
   * @throws InterruptedException
   */
  public String getLatestCommitSha(String branch) throws IOException, InterruptedException {
    String sha = null;
    String branchOutput = null;
    try {
      branchOutput = executor.runGitCommand("show-ref", "refs/heads/" + branch);
    } catch (ProcessExitException e) {
      // The branch does not exist.
    }
    if (branchOutput != null && branchOutput.trim().length() != 0) {
      sha = branchOutput.split(" ")[0];
    }
    return sha;
  }
  
  
  /**
   * Writes the content of the specified file and commits it.
   * 
   * @param sourceCommitSha The commit from which we should start.
   * @param filePath The path of the file to update.
   * @param fileContent The content of the file to commit.
   * 
   * @param commitMessage The commit message.
   * 
   * @return The SHA of the new commit.
   * 
   * @throws IOException
   * @throws InterruptedException
   */
  public String writeFile(String sourceCommitSha, String filePath, String fileContent, 
      String commitMessage) throws IOException, InterruptedException {
    String rootTreeId = getRootTreeSha(sourceCommitSha);

    String[] filePathParts = filePath.split("/");
    List<GitTree> trees = computePathToRoot(rootTreeId, filePathParts);
    
    String blobSha = this.hashBlob(fileContent, filePath);

    GitTree fileParent = trees.get(trees.size() - 1);
    fileParent.updateEntry(filePathParts[filePathParts.length - 1], blobSha, EntryType.BLOB);
    String ancestorSha = mkTree(fileParent);
    
    ancestorSha = updateAncestors(filePathParts, trees, ancestorSha);
    return commitTree(ancestorSha, sourceCommitSha, commitMessage);
  }
  

  /**
   * Update the ancestors of the modified file.
   * 
   * @param filePathParts The file path entries.
   * @param trees The trees on the path to root.
   * @param ancestorSha The SHA of the parent of the file.
   * 
   * @return the new root SHA.
   * 
   * @throws IOException
   * @throws InterruptedException
   */
  private String updateAncestors(String[] filePathParts, List<GitTree> trees, String ancestorSha)
      throws IOException, InterruptedException {
    for (int i = filePathParts.length - 2; i >= 0; i--) {
      trees.get(i).updateEntry(
          filePathParts[i], ancestorSha, EntryType.TREE);
      ancestorSha = mkTree(trees.get(i));
    }
    return ancestorSha;
  }
  
  /**
   * Hashes a file into the Git database.
   * 
   * @param contents The contents of the file.
   * @param filePath The path of the file.
   * 
   * @return The SHA of the generated blob.
   * @throws IOException
   * @throws InterruptedException
   */
  private String hashBlob(String contents, String filePath) throws IOException, InterruptedException {
    return executor.pipeIntoGitCommand(
        contents, "hash-object", "-w", "--stdin", "--path", filePath).trim();
  }
  

  /**
   * Deletes the content of the specified file and commits.
   * 
   * @param sourceCommitSha The commit from which we should start.
   * @param filePath The path of the file to update.
   * 
   * @param commitMessage The commit message.
   * 
   * @return The SHA of the new commit.
   * 
   * @throws IOException
   * @throws InterruptedException
   */
  public String deleteFile(String sourceCommitSha, String filePath, 
      String commitMessage) throws IOException, InterruptedException {
    String rootTreeId = getRootTreeSha(sourceCommitSha);

    String[] filePathParts = filePath.split("/");
    List<GitTree> trees = computePathToRoot(rootTreeId, filePathParts);
    
    GitTree fileParent = trees.get(trees.size() - 1);
    fileParent.removeEntry(filePathParts[filePathParts.length - 1]);
    String ancestorSha = mkTree(fileParent);
    
    ancestorSha = updateAncestors(filePathParts, trees, ancestorSha);
    
    return commitTree(ancestorSha, sourceCommitSha, commitMessage);
  }

  /**
   * Computes a list of trees that for the path from the root tree to the file.
   * 
   * @param rootTreeId The if of the root tree.
   * @param filePathParts The array of file path entries.
   * 
   * @return The list of Git tree objects.
   * 
   * @throws IOException
   * @throws InterruptedException
   */
  private List<GitTree> computePathToRoot(String rootTreeId, 
      String[] filePathParts) throws IOException, InterruptedException {
    String crtTreeId = rootTreeId;
    List<GitTree> trees = Lists.newArrayListWithExpectedSize(filePathParts.length);
    for (int i = 0; i < filePathParts.length; i++) {
      GitTree crtTree = null;
      if (crtTreeId != null) {
        String treeContent = executor.runGitCommand("ls-tree", crtTreeId);
        crtTree = new GitTree(splitInLines(treeContent));
        crtTreeId = crtTree.getEntrySha(filePathParts[i]);
      } else {
        crtTree = new GitTree(new String[0]);
      }
      trees.add(crtTree);
    }
    return trees;
  }

  /**
   * Creates a tree object and returns its SHA.
   * @param tree The content of the tree object.
   * @return The SHA of the tree object.
   * 
   * @throws IOException
   * @throws InterruptedException
   */
  private String mkTree(GitTree tree) throws IOException, InterruptedException {
    return executor.pipeIntoGitCommand(tree.toString(), "mktree").trim();
  }
  
  /**
   * Commits the a tree.
   * 
   * @param treeSha The SHA of the tree.
   * @param parentSha The SHA of the parent commit.
   * @param commitMessage The commit message.
   * 
   * @return The commit SHA.
   * 
   * @throws IOException
   * @throws InterruptedException
   */
  private String commitTree(String treeSha, String parentSha, String commitMessage) throws IOException, InterruptedException {
    return executor.runGitCommand("commit-tree", treeSha, "-p", parentSha, "-m", commitMessage).trim();
  }
  

  /**
   * Returns the SHA root tree of the commit.
   *  
   * @param sourceCommitSha The SHA of the commit.
   * 
   * @return The SHA of the root tree.
   *
   * @throws IOException
   * @throws InterruptedException
   */
  private String getRootTreeSha(String sourceCommitSha) throws IOException, InterruptedException {
    String commitDetails = executor.runGitCommand("cat-file", "commit", sourceCommitSha);
    String commitTreeDetails = splitInLines(commitDetails)[0];
    String rootTreeId = commitTreeDetails.split(" ")[1];
    return rootTreeId;
  }
  
  /**
   * Moves the specified branch to point to the given commit. 
   * 
   * If the branch already exists, and if one of the parents of the 
   * given commit is not the latest commit on that branch, this method fails.
   * 
   * Note: This method is the only one that is not atomic. It should not be called
   * from multiple processes simultaneously. Calling it from multiple threads is OK.
   *
   * @param branch The name of the branch.
   * @param commitSha The commit at which to point the branch to.
   *
   * @return <code>true</code> if the branch was moved.
   * 
   * @throws IOException
   * @throws InterruptedException
   */
  public boolean moveBranch(String branch, String commitSha) throws IOException, InterruptedException {
    List<String> commitParents = this.getCommitParents(commitSha);
    boolean moved = false;
    synchronized (this) {
      String branchCommitSha = this.getLatestCommitSha(branch);
      if (branchCommitSha == null || commitParents.contains(branchCommitSha)) {
        executor.runGitCommand("update-ref", "refs/heads/" + branch, commitSha);
        moved = true;
      }
    }
    return moved;
  }
  
  /**
   * Return the parents of a commit.
   * 
   * @param commitSha The commit SHA.
   * 
   * @return The parent commits.
   * 
   * @throws IOException
   * @throws InterruptedException
   */
  private List<String> getCommitParents(String commitSha) throws IOException, InterruptedException {
    String commitDetails = executor.runGitCommand("cat-file", "commit", commitSha);
    List<String> parents = Lists.newArrayList();
    for (String line : splitInLines(commitDetails)) {
      if (line.startsWith("parent")) {
        parents.add(line.split(" ")[1]);
      }
    }
    return parents;
  }


  /**
   * Split the string in lines.
   * 
   * @param string The string to split.
   * 
   * @return The lines array.
   */
  private String[] splitInLines(String string) {
    String[] split = string.split("\\r?\\n");
    if (split.length == 1 && split[0].length() == 0) {
      split = new String[0];
    }
    return split;
  }

  
  /**
   * Reads the content of a file at a specific commit.
   * 
   * @param sha The SHA of the commit in which we are interested.
   * @param path The path of the file that we want to read.
   * 
   * @return The content of the file.
   * 
   * @throws IOException
   * @throws InterruptedException
   */
  public String readFile(String sha, String path) throws IOException, InterruptedException {
    return executor.runGitCommand("show", sha + ":" + path);
  }
  
  /**
   * Lists the directory contents on a given branch.
   * 
   * @param sha The SHA of the commit at on which we want to list the folder entries.
   * @param dirPath The path to the directory.
   * 
   * @return The list of directory entries.
   * 
   * @throws IOException
   * @throws InterruptedException
   */
  public List<String> listFiles(String sha, String dirPath) throws IOException, InterruptedException {
    String fileList = executor.runGitCommand("ls-tree", sha, dirPath + "/", "--name-only");
    List<String> escapedFilePaths = Arrays.asList(splitInLines(fileList));
    List<String> fileNames = Lists.newArrayListWithCapacity(escapedFilePaths.size());
    for (String escapedFilePath: escapedFilePaths) {
      String filePath = escapedFilePath;
      filePath = Unescaper.unescapeCStringLiteral(escapedFilePath);
      int nameStartIndex = filePath.lastIndexOf(File.separatorChar) + 1;
      fileNames.add(filePath.substring(nameStartIndex));
    }
    return fileNames;
  }
}
