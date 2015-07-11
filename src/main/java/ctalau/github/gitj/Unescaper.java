package ctalau.github.gitj;

import java.util.List;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class Unescaper {

  private static final Map<Character, Byte> escape = 
      new ImmutableMap.Builder<Character, Byte>()
       .put('a', (byte)7)
       .put('b', (byte)8)
       .put('f', (byte)0xc)
       .put('n',  (byte)0x0A)
       .put('r',  (byte)0x0D)
       .put('t',  (byte)9)
       .put('v',  (byte)0xB)
       .put('\\',  (byte)0x5C)
       .put('\'',  (byte)0x27)
       .put('"',  (byte)0x22)
       .put('?',  (byte)0x3F)
       .build();
  
  public static String unescapeCStringLiteral(String literal) {
    List<Byte> sb = Lists.newArrayListWithCapacity(literal.length());
    
    if (literal.charAt(0) != '"' || literal.charAt(literal.length() - 1) != '"') {
      throw new IllegalArgumentException("The input string is not a C string literal.");
    }
    
    for (int i = 1; i < literal.length() - 1; i++) {
      char crtChar = literal.charAt(i);
      if (crtChar != '\\') {
        sb.add((byte)crtChar);
      } else {
        i++;
        char firstChar = literal.charAt(i);
        if (Character.isDigit(firstChar)) {
          // octal escaped
          int codePoint = Integer.parseInt(literal.substring(i, i + 3), 8);
          sb.add((byte)codePoint);
          i += 2;
        } else if (firstChar == 'x') {
          // hexa escaped.
          int codePoint = Integer.parseInt(literal.substring(i + 1, i + 3), 16);
          sb.add((byte)codePoint);
          i += 2;
        } else if (escape.containsKey(firstChar)){
          sb.add(escape.get(firstChar));
        } else {
          throw new IllegalStateException("String: " + literal + ", offset " + i + " char: " + firstChar);
        }
      }
    }
    byte[] bytes = new byte[sb.size()];
    for (int i = 0; i < sb.size(); i++) {
      bytes[i] = sb.get(i);
    }
    return new String(bytes, Charsets.UTF_8);
  }
}
