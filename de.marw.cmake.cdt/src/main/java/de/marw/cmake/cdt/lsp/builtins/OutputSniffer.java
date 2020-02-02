/*******************************************************************************
 * Copyright (c) 2018-2019 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/

package de.marw.cmake.cdt.lsp.builtins;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import de.marw.cmake.cdt.lsp.builtins.IBuiltinsOutputProcessor.IProcessingContext;

/**
 * An OutputStream that passes each line written to it to a IBuiltinsOutputProcessor.
 *
 * @author Martin Weber
 *
 * @implNote this is visible for testing only
 */
public class OutputSniffer extends OutputStream {

  private static final String SEP = System.lineSeparator();
  private final StringBuilder buffer;
  private final IBuiltinsOutputProcessor processor;
  private final IProcessingContext processingContext;
  private final OutputStream os;

  /**
   * @param outputStream
   *          the OutputStream to write to or {@code null}
   * @param processingContext
   *          the processing context
   */
  public OutputSniffer(IBuiltinsOutputProcessor processor, OutputStream outputStream, IProcessingContext processingContext) {
    this.processor = Objects.requireNonNull(processor, "processor");
    this.processingContext = Objects.requireNonNull(processingContext, "processingContext");
    this.os= outputStream;
    buffer = new StringBuilder(512);
  }

  @Override
  public void write(int c) throws IOException {
    if (os != null)
      os.write(c);
    synchronized (this) {
      buffer.append(new String(new byte[] { (byte) c }));
      splitLines();
    }
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    if (os != null)
      os.write(b, off, len);
    synchronized (this) {
      buffer.append(new String(b, off, len));
      splitLines();
    }
  }

  @Override
  public void flush() throws IOException {
    if (os != null)
      os.flush();
    synchronized (this) {
      splitLines();
      // process remaining bytes
      String line = buffer.toString();
      buffer.setLength(0);
      processLine(line);
    }
  }

  @Override
  public void close() throws IOException {
    if (os != null)
      os.close();
    flush();
  }

  /**
   * Splits the buffer into separate lines and sends these to the parsers.
   *
   */
  private void splitLines() {
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
    processor.processLine(line, processingContext);
  }
}