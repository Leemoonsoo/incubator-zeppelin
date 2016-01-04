/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.zeppelin.interpreter;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * InterpreterOutput is OutputStream that supposed to print content on notebook
 * in addition to InterpreterResult which used to return from Interpreter.interpret().
 */
public class InterpreterOutput extends OutputStream {
  Logger logger = LoggerFactory.getLogger(InterpreterOutput.class);
  private final int NEW_LINE_CHAR = '\n';

  ByteArrayOutputStream buffer = new ByteArrayOutputStream();

  private final List<Object> outList = new LinkedList<Object>();
  private InterpreterOutputChangeWatcher watcher;
  private final InterpreterOutputNewlineListener flushListener;

  public InterpreterOutput(InterpreterOutputNewlineListener flushListener) {
    this.flushListener = flushListener;
    clear();
  }

  public InterpreterOutput(InterpreterOutputNewlineListener flushListener,
                           InterpreterOutputChangeListener listener) throws IOException {
    this.flushListener = flushListener;
    clear();
    watcher = new InterpreterOutputChangeWatcher(listener);
    watcher.start();
  }

  public void clear() {
    synchronized (outList) {
      outList.clear();
      if (watcher != null) {
        watcher.clear();
      }
    }
  }
  @Override
  public void write(int b) throws IOException {
    synchronized (outList) {
      buffer.write(b);
      if (b == NEW_LINE_CHAR) {
        buffer.flush();
        byte[] byteArray = buffer.toByteArray();
        outList.add(byteArray);
        flushListener.onNewLineDetected(byteArray);
        buffer.reset();
      }
    }
  }

  @Override
  public void write(byte [] b) throws IOException {
    write(b, 0, b.length);
  }

  @Override
  public void write(byte [] b, int off, int len) throws IOException {
    synchronized (outList) {
      for (int i = off; i < len; i++) {
        write(b[i]);
      }
    }
  }

  /**
   * In dev mode, it monitors file and update ZeppelinServer
   * @param file
   * @throws IOException
   */
  public void write(File file) throws IOException {
    outList.add(file);
    if (watcher != null) {
      watcher.watch(file);
    }
  }

  public void write(String string) throws IOException {
    write(string.getBytes());
  }

  /**
   * write contents in the resource file in the classpath
   * @param url
   * @throws IOException
   */
  public void write(URL url) throws IOException {
    if ("file".equals(url.getProtocol())) {
      write(new File(url.getPath()));
    } else {
      outList.add(url);
    }
  }

  public void writeResource(String resourceName) throws IOException {
    // search file under resource dir first for dev mode
    File mainResource = new File("./src/main/resources/" + resourceName);
    File testResource = new File("./src/test/resources/" + resourceName);
    if (mainResource.isFile()) {
      write(mainResource);
    } else if (testResource.isFile()) {
      write(testResource);
    } else {
      // search from classpath
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      if (cl == null) {
        cl = this.getClass().getClassLoader();
      }
      if (cl == null) {
        cl = ClassLoader.getSystemClassLoader();
      }

      write(cl.getResource(resourceName));
    }
  }

  public byte[] toByteArray() throws IOException {
    return toByteArray(false);
  }

  public byte[] toByteArray(boolean clear) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    List<Object> all = new LinkedList<Object>();

    synchronized (outList) {
      all.addAll(outList);
    }

    for (Object o : all) {
      if (o instanceof File) {
        File f = (File) o;
        FileInputStream fin = new FileInputStream(f);
        copyStream(fin, out);
        fin.close();
      } else if (o instanceof byte[]) {
        out.write((byte[]) o);
      } else if (o instanceof Integer) {
        out.write((int) o);
      } else if (o instanceof URL) {
        InputStream fin = ((URL) o).openStream();
        copyStream(fin, out);
        fin.close();
      } else {
        // can not handle the object
      }
    }

    if (clear) {
      clear();
    }

    out.close();
    return out.toByteArray();
  }

  private void copyStream(InputStream in, OutputStream out) throws IOException {
    int bufferSize = 8192;
    byte[] buffer = new byte[bufferSize];

    while (true) {
      int bytesRead = in.read(buffer);
      if (bytesRead == -1) {
        break;
      } else {
        out.write(buffer, 0, bytesRead);
      }
    }
  }

  @Override
  public void close() throws IOException {
    synchronized (outList) {
      buffer.flush();
      byte[] bytes = buffer.toByteArray();
      outList.add(bytes);
      flushListener.onNewLineDetected(bytes);
      buffer.close();
    }

    if (watcher != null) {
      watcher.clear();
      watcher.shutdown();
    }
  }
}
