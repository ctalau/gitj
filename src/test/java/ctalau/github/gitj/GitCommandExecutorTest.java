package ctalau.github.gitj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * Unit test for the Git repository class.
 */
public class GitCommandExecutorTest {

  
  private File repoDir;

  /**
   * Create the scratch dir.
   */
  @Before
  public void setUp() {
    repoDir = Files.createTempDir();
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
   * Test the command execution for no args commands.
   * 
   * @throws Exception
   */
  @Test
  public void testCommandExecutionWithoutArgs() throws Exception {
    GitCommandExecutor executor = new GitCommandExecutor(repoDir);
    String output = executor.runGitCommand("init");
    assertTrue(output.startsWith("Initialized empty Git repository"));
  }

  /**
   * Test the command execution for commands with arguments.
   * 
   * @throws Exception
   */
  @Test
  public void testCommandExecutionWithArguments() throws Exception {
    GitCommandExecutor executor = new GitCommandExecutor(repoDir);
    String output = executor.runGitCommand("init");
    assertTrue(output.startsWith("Initialized empty Git repository"));

    File content = new File(repoDir, "README.md");
    Files.write("Some *bold* stuff", content, Charsets.UTF_8);
    executor.runGitCommand("add", content.getPath());

    String commitMessage = "Some message\u2014";
    executor.runGitCommand("commit", "-a", "-m", commitMessage);

    String gitlog = executor.runGitCommand("log", "--oneline");
    String actualCommitMessage = gitlog.substring(gitlog.indexOf(" ")).trim();

    assertEquals(commitMessage, actualCommitMessage);
  }
}
