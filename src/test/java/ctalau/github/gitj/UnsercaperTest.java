package ctalau.github.gitj;

import static org.junit.Assert.*;

import org.junit.Test;

public class UnsercaperTest {

  /**
   * Test that octal characters are unescaped correctly
   * @throws Exception
   */
  @Test
  public void testUnescapeOctal() throws Exception {
    assertEquals("a\u2014b", Unescaper.unescapeCStringLiteral("\"a\\342\\200\\224b\""));
  }
  
  /**
   * Test that question mark is unescaped correctly
   * @throws Exception
   */
  @Test
  public void testUnescapeQuestionMark() throws Exception {
    assertEquals("a\"", Unescaper.unescapeCStringLiteral("\"a\\\"\""));
  }
}
