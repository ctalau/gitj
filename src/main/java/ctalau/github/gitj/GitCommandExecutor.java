package ctalau.github.gitj;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

public class GitCommandExecutor {
  /**
   * The folder where the local repository clone is located.
   */
  private File repoLocation;

  /**
   * Constructor.
   * 
   * @param repoLocation
   *          The location of the repository where the commands should be run.
   */
  public GitCommandExecutor(File repoLocation) {
    this.repoLocation = repoLocation;
  }

  /**
   * Runs a git command and returns the output.
   * 
   * @param command
   *          The git command name: "commit", "branch", etc.
   * @param args
   *          The arguments of that command.
   * 
   * @return The output of the git tool.
   * 
   * @throws IOException
   *           If could not read the process output.
   * @throws InterruptedException
   */
  public String runGitCommand(String command, String... args) throws IOException, InterruptedException {
    List<String> arguments = Lists.newArrayListWithCapacity(2 + args.length);
    arguments.add("git");
    arguments.add(command);
    arguments.addAll(Arrays.asList(args));
    Process process = new ProcessBuilder().command(arguments).directory(this.repoLocation).start();
    String output = readUtf8InputStreamStream(process.getInputStream());
    String error = readUtf8InputStreamStream(process.getErrorStream());

    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new IOException(error);
    }

    return output;
  }

  /**
   * Read an UTF8 input stream to a string.
   * 
   * @param stream
   *          The input stream.
   * 
   * @return The content of the stream.
   * 
   * @throws IOException
   */
  private String readUtf8InputStreamStream(InputStream stream) throws IOException {
    InputStreamReader stdoutReader = new InputStreamReader(stream, Charsets.UTF_8);
    String output;
    try {
      output = CharStreams.toString(stdoutReader);
    } finally {
      stdoutReader.close();
    }
    return output;
  }
}
