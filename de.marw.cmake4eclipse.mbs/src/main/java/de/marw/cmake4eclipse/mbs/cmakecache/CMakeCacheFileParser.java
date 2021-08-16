/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
 *
 * Content is provided to you under the terms and conditions of the Eclipse Public License Version 2.0 "EPL".
 * A copy of the EPL is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package de.marw.cmake4eclipse.mbs.cmakecache;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple parser for CMake cache files ({@code CMakeCache.txt}). This
 * implementation extracts only key-value-pairs corresponding to an entry. It
 * does not extract any help texts nor entry types.
 *
 * @author Martin Weber
 */
public class CMakeCacheFileParser {

  // input line is: key:type=value
  private static final Pattern reg = Pattern
      .compile("([^=:]*):([^=]*)=(.*[^\t ]|[\t ]*)[\t ]*");
  // input line is: "key":type=value
  private static final Pattern regQuoted = Pattern
      .compile("\"([^=:]*)\":([^=]*)=(.*[^\t ]|[\t ]*)[\t ]*");
  // input line is: key=value
  private static final Pattern regNoType = Pattern
      .compile("([^=]*)=(.*[^\t ]|[\t ]*)[\t ]*");
  // input line is: "key"=value
  private static final Pattern regQuotedNoType = Pattern
      .compile("\"([^=]*)\"=(.*[^\t ]|[\t ]*)[\t ]*");

  /**
   * Parses the content of the specified input stream as a CMake cache file
   * content. <br>
   * This implementation is inspired by <a href=
   * "https://github.com/Kitware/CMake/blob/master/Source/cmCacheManager.cxx"
   * >cmCacheManager.cxx</a>.
   *
   * @param is
   *        the input stream that serves the content of the CMake cache file
   * @param filter
   *        an optional filter for CMake cache file entries or {@code null} if
   *        all entries are of interest
   * @param parsedEntries
   *        receives the parsed cache file entries. Specify {@code null}, if you
   *        want to verify the correct syntax of the cache file only. Specify an
   *        instance of {@link List}, if you expect multiple cache entires of
   *        the same key int the file. Normally, you would specify an instance
   *        of {@link Set} here.
   * @param errorLog
   *        receives messages concerning parse errors. Specify {@code null}, if
   *        you are not interested in error messages.
   * @return {@code true} if the file could be parsed without errors, otherwise
   *         {@code false}
   * @throws IOException
   *         if an operation on the input stream failed
   */
  public boolean parse(final InputStream is, final EntryFilter filter,
      Collection<SimpleCMakeCacheEntry> parsedEntries, List<String> errorLog)
      throws IOException {

    final LineNumberReader reader = new LineNumberReader(new InputStreamReader(
        is));
    boolean hasErrors = false;

    Map<String, SimpleCMakeCacheEntry> uniqueMap = null;
    if (parsedEntries != null && parsedEntries instanceof Set) {
      // avoid returning duplicate keys
      uniqueMap = new HashMap<String, SimpleCMakeCacheEntry>();
    }

    for (String line; null != (line = reader.readLine());) {
      int idx = 0;
      // skip leading whitespaces...
      for (; idx < line.length(); idx++) {
        final char c = line.charAt(idx);
        if (!Character.isWhitespace(c))
          break;
      }
      if (!(idx < line.length()))
        continue; // skip blank lines

      if (line.charAt(idx) == '#')
        continue; // skip cmake comment lines

      if (idx < line.length()) {
        line = line.substring(idx);

        if (line.startsWith("//"))
          continue; // ignore help string

        // parse cache entry...
        String key = null;
        String value = null;
        Matcher matcher;

        if ((matcher = reg.matcher(line)).matches()
            || (matcher = regQuoted.matcher(line)).matches()) {
          // input line is: key:type=value
          // input line is: "key":type=value
          key = matcher.group(1);
          // we do not need the type from group(2)
          value = matcher.group(3);
        } else if ((matcher = regNoType.matcher(line)).matches()
            || (matcher = regQuotedNoType.matcher(line)).matches()) {
          // input line is: key=value
          // input line is: "key"=value
          key = matcher.group(1);
          value = matcher.group(2);
        } else {
          hasErrors |= true;
          // add error message
          if (errorLog != null) {
            final String msg = MessageFormat.format(
                "Error: Line {0,number,integer}: Offending entry: {1}",
                reader.getLineNumber(), line);
            errorLog.add(msg);
          }
        }

        if (filter != null && parsedEntries != null) {
          // no need to call the filter if nothing is to be returned
          if (!filter.accept(key))
            continue; // uninteresting entry, get next line
        }

        // if value is enclosed in single quotes ('foo') then remove them
        // it is used to enclose trailing space or tab
        if (key != null && value != null && value.length() >= 2
            && value.charAt(0) == '\''
            && value.charAt(value.length() - 1) == '\'') {

          value = value.substring(1, value.length());
        }

        // store entry
        if (parsedEntries != null) {
          final SimpleCMakeCacheEntry entry = new SimpleCMakeCacheEntry(key,
              value);
          if (uniqueMap != null)
            uniqueMap.put(key, entry);
          else
            parsedEntries.add(entry);
        }
      }
    }
    if (parsedEntries != null && uniqueMap != null)
      parsedEntries.addAll(uniqueMap.values());
    return hasErrors;
  }

  ////////////////////////////////////////////////////////////////////
  // inner classes
  ////////////////////////////////////////////////////////////////////
  /**
   * A filter for CMake cache file entry keys.
   * <p>
   * Instances of this interface may be passed to the
   * {@link CMakeCacheFileParser#CMakeCacheFileParser()} constructor of the
   * {@code CMakeCacheFileParser}</code> class.
   *
   * @author Martin Weber
   */
  public interface EntryFilter {
    /**
     * Tests whether or not the specified entry key should be included in a set
     * returned by {@link CMakeCacheFileParser#parse}.
     *
     * @param key
     *        The entry key to be tested. Never {@code null}
     * @return {@code true} if and only if {@code key} should be
     *         included
     */
    boolean accept(String key);
  }
}
