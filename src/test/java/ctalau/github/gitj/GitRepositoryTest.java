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
    String branchName = "newbranch\u2014";
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
    String branchName = "newbranch\u2014";
    executor.runGitCommand("branch", branchName);

    // Create the file on the master branch.
    String filePath = "folder\u20141/file.xml";
    String content = "<root>\u2014<\root>";
    addFileOnCurrentBranch(filePath, content);
    
    // Switch to another branch.
    executor.runGitCommand("checkout", branchName);
    // The file does not exist on this branch
    assertFalse(new File(repoDir, filePath).exists());
    
    String readContent = new GitRepository(repoDir).readFile(
        "master", filePath);
    assertEquals(content, readContent);
  }

  /**
   * Test the directory listing operation.
   * 
   * @throws Exception
   */
  @Test
  public void testDirectoryListing() throws Exception {
    String branchName = "newbranch\u2014";
    executor.runGitCommand("branch", branchName);

    // Create the files on the master branch.
    String folderPath = "folder\u20141/";
    String content = "<root>\u2014<\root>";
    List<String> files = ImmutableList.of(
        folderPath + "file\u20141.xml", 
        folderPath + "file\u20142.xml");
    for (String file: files) {
      addFileOnCurrentBranch(file, content);
    }
    
    // Switch to another branch.
    executor.runGitCommand("checkout", branchName);
    // The folder does not exist on this branch
    assertFalse(new File(repoDir, folderPath).exists());
    
    List<String> gotFiles = new GitRepository(repoDir).listFiles("master", folderPath);
    assertEquals(files, gotFiles);
  }
}

