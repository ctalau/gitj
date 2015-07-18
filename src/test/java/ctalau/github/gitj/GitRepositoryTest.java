package ctalau.github.gitj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

/**
 * Test for different operations on a Git repository.
 * 
 * @author ctalau
 */
public class GitRepositoryTest {
  
  /**
   * The repository to be used in tests.
   */
  private File repoDir;
  
  /**
   * The command executor for that repository.
   */
  private GitCommandExecutor executor;


  /**
   * Creates a new repository for testing purposes.
   * 
   * @throws Exception 
   */
  @Before
  public void setUp() throws Exception {
    repoDir = Files.createTempDir();
    executor = new GitCommandExecutor(repoDir);
    executor.runGitCommand("init");
    
    addFileOnCurrentBranch("README.md", "text");
  }
  
  /**
   * Converts the - char to a long dash unicode char.
   * 
   * @param src The string containing -.
   * @return The string with the long dash replaced.
   */
  private static String mkUnicode(String src) {
    return src.replace("-", "\u2014");
  }

  /**
   * Adds a file on the current branch.
   * 
   * @param path The path of the file.
   * @param content The content of the file.
   * @throws IOException
   * @throws InterruptedException
   */
  private void addFileOnCurrentBranch(String path, String content) throws IOException, InterruptedException {
    // Create a readme file.
    File readmeFile = new File(repoDir, path);
    Files.createParentDirs(readmeFile);
    Files.write(content, readmeFile, Charsets.UTF_8);
    executor.runGitCommand("add", readmeFile.getPath());
    
    // And an initial commit.
    executor.runGitCommand("commit", "-m", "Added: " + path);
  }
  
  /**
   * Delete the scratch dir.
   * @throws IOException 
   */
  @After
  public void tearDown() throws IOException {
    FileUtils.deleteDirectory(repoDir);
  }
  
  /**
   * Test the branch listing operation.
   * 
   * @throws Exception
   */
  @Test
  public void testBranchListing() throws Exception {
    String branchName = mkUnicode("newbranch-");
    executor.runGitCommand("branch", branchName);
    List<String> branches = new GitRepository(repoDir).listBranches();
    assertEquals(ImmutableList.of("master", branchName), branches);
  }
  
  /**
   * Test the file reading operation.
   * 
   * @throws Exception
   */
  @Test
  public void testFileReading() throws Exception {
    String branchName = mkUnicode("newbranch-");
    executor.runGitCommand("branch", branchName);

    // Create the file on the master branch.
    String filePath = mkUnicode("folder-1/file.xml");
    String content = mkUnicode("<root>-<\root>");
    addFileOnCurrentBranch(filePath, content);
    
    // Switch to another branch.
    executor.runGitCommand("checkout", branchName);
    // The file does not exist on this branch
    assertFalse(new File(repoDir, filePath).exists());
    
    
    GitRepository repository = new GitRepository(repoDir);
    String masterSha = repository.getLatestCommitSha("master");
    String readContent = repository.readFile(masterSha, filePath);
    assertEquals(content, readContent);
  }

  /**
   * Test the directory listing operation.
   * 
   * @throws Exception
   */
  @Test
  public void testDirectoryListing() throws Exception {
    String branchName = mkUnicode("newbranch-");
    executor.runGitCommand("branch", branchName);

    // Create the files on the master branch.
    String folderPath = mkUnicode("folder-\"\\1/");
    String content = mkUnicode("<root>-<\root>");
    List<String> files = ImmutableList.of(
        mkUnicode("file-1.xml"), 
        mkUnicode("file-2.xml"));
    for (String file: files) {
      addFileOnCurrentBranch(folderPath + file, content);
    }
    
    // Switch to another branch.
    executor.runGitCommand("checkout", branchName);
    // The folder does not exist on this branch
    assertFalse(new File(repoDir, folderPath).exists());
    
    GitRepository repository = new GitRepository(repoDir);
    String masterSha = repository.getLatestCommitSha("master");
    List<String> gotFiles = repository.listFiles(masterSha, folderPath);
    assertEquals(files, gotFiles);
  }
  
  /**
   * Test file deletion operation.
   * 
   * @throws Exception
   */
  @Test
  public void testFileDeletion() throws Exception {
    String branchName = mkUnicode("newbranch-");
    executor.runGitCommand("branch", branchName);

    // Create the files on the master branch.
    String content = mkUnicode("<root>-<\root>");
    String fileToDelete = mkUnicode("f-1/f-2/file-1.xml");
    List<String> files = ImmutableList.of(
        fileToDelete,
        mkUnicode("f-1/f-2/other-1.xml"),
        mkUnicode("f-1/f-2/other-dir/file.xml"));
    for (String file: files) {
      addFileOnCurrentBranch(file, content);
    }
    
    // Launch the delete operation.
    GitRepository repository = new GitRepository(repoDir);
    String branchSha = repository.getLatestCommitSha("master");
    String newSha = repository.deleteFile(branchSha,
        fileToDelete, "Deleted file");
    
    // Check the files in the parent of the deleted file.
    List<String> gotFiles = repository.listFiles(newSha, mkUnicode("f-1/f-2"));
    assertEquals(ImmutableList.of(
        mkUnicode("other-1.xml"),
        mkUnicode("other-dir")), gotFiles);
  }
  
  /**
   * Test file update operation.
   * 
   * @throws Exception
   */
  @Test
  public void testFileUpdate() throws Exception {
    String branchName = mkUnicode("newbranch-");
    executor.runGitCommand("branch", branchName);

    // Create the files on the master branch.
    String content = mkUnicode("<root>-<\root>");
    String fileToUpdate = mkUnicode("f-1/f-2/file-1.xml");
    addFileOnCurrentBranch(fileToUpdate, content);
    
    // Launch the delete operation.
    GitRepository repository = new GitRepository(repoDir);
    String branchSha = repository.getLatestCommitSha("master");
    String newContent = mkUnicode("<root></b>-</b><\root>");
    String newSha = repository.writeFile(branchSha,
        fileToUpdate, newContent, "Updated file");
    
    // Check the files in the parent of the deleted file.
    String gotContent = repository.readFile(newSha, fileToUpdate);
    assertEquals(newContent, gotContent);
  }

}

