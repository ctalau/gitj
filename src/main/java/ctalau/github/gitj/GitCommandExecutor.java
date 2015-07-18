package ctalau.github.gitj;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;

public class GitCommandExecutor {
  
  /**
   * Exception thrown when the process exits with a non-zero exit code.
   */
  @SuppressWarnings("serial")
  static class ProcessExitException extends IOException {
    public ProcessExitException(String msg) {
      super(msg);
    }
  }
  
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
   * @param input The input stream of the command.
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
  public String pipeIntoGitCommand(String input, String command, String... args) throws IOException, InterruptedException {
    List<String> arguments = Lists.newArrayListWithCapacity(2 + args.length);
    arguments.add("git");
    arguments.add(command);
    arguments.addAll(Arrays.asList(args));
    Process process = new ProcessBuilder().command(arguments).directory(this.repoLocation).start();
    if (input != null) {
      injectInputStream(input, process);
    }
    String output = readAsciiInputStreamStream(process.getInputStream());
    String error = readAsciiInputStreamStream(process.getErrorStream());

    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new ProcessExitException(error);
    }

    return output;
    
  }

  /**
   * Inject the given string as the input stream of the process.
   * 
   * @param input The string to pipe in the content.
   * @param process The process.
   * 
   * @throws IOException
   */
  private void injectInputStream(String input, Process process) throws IOException {
    OutputStream outputStream = process.getOutputStream();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(input
        .getBytes(Charsets.UTF_8));
    try {
      ByteStreams.copy(inputStream, outputStream);
    } finally {
      inputStream.close();
    }
    outputStream.flush();
    outputStream.close();
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
    return pipeIntoGitCommand(null, command, args);
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
  private String readAsciiInputStreamStream(InputStream stream) throws IOException {
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
