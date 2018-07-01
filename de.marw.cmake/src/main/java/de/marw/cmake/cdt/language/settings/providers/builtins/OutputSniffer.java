/*******************************************************************************
 * Copyright (c) 2018 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/

package de.marw.cmake.cdt.language.settings.providers.builtins;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * An OutputStream that passes each line written to it to a BuiltinsOutputProcessor.
 *
 * @author Martin Weber
 */
class OutputSniffer extends OutputStream {

  private static final String SEP = System.lineSeparator();
  private final StringBuilder buffer;
  private final BuiltinsOutputProcessor processor;

  public OutputSniffer(BuiltinsOutputProcessor processor) {
    this.processor = Objects.requireNonNull(processor);
    buffer = new StringBuilder(512);
  }

  @Override
  public synchronized void write(int c) throws IOException {
    buffer.append(new String(new byte[] { (byte) c }));
    splitLines();
  }

  @Override
  public synchronized void write(byte[] b, int off, int len) throws IOException {
    buffer.append(new String(b, off, len));
    splitLines();
  }

  @Override
  public synchronized void flush() throws IOException {
    splitLines();
    // process remaining bytes
    String line = buffer.toString();
    buffer.setLength(0);
    processLine(line);
  }

  @Override
  public void close() throws IOException {
    flush();
  }

  /**
   * Splits the buffer into separate lines and sends these to the parsers.
   *
   */
  private synchronized void splitLines() {
    int idx;
    while ((idx = buffer.indexOf(SEP)) != -1) {
      String line = buffer.substring(0, idx);
      buffer.delete(0, idx + SEP.length());
      processLine(line);
    }
  }

  /**
   * @param line
   */
  private void processLine(String line) {
    processor.processLine(line);
  }
}