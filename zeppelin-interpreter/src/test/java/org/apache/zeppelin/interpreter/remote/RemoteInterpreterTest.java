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

package org.apache.zeppelin.interpreter.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.thrift.transport.TTransportException;
import org.apache.zeppelin.display.AngularObjectRegistry;
import org.apache.zeppelin.display.GUI;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterContextRunner;
import org.apache.zeppelin.interpreter.InterpreterGroup;
import org.apache.zeppelin.interpreter.InterpreterOutput;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResult.Code;
import org.apache.zeppelin.interpreter.remote.RemoteInterpreterServer.InterpretJob;
import org.apache.zeppelin.interpreter.remote.mock.MockInterpreterA;
import org.apache.zeppelin.interpreter.remote.mock.MockInterpreterB;
import org.apache.zeppelin.resource.ResourcePool;
import org.apache.zeppelin.scheduler.Job;
import org.apache.zeppelin.scheduler.Job.Status;
import org.apache.zeppelin.scheduler.Scheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RemoteInterpreterTest {
  String localRepo = System.getProperty("java.io.tmpdir") + "/localrepo";

  private InterpreterGroup intpGroup;
  private HashMap<String, String> env;

  @Before
  public void setUp() throws Exception {
    intpGroup = new InterpreterGroup();
    env = new HashMap<String, String>();
    env.put("ZEPPELIN_CLASSPATH", new File("./target/test-classes").getAbsolutePath());
  }

  @After
  public void tearDown() throws Exception {
    intpGroup.close();
    intpGroup.destroy();
  }

  private InterpreterContext createInterpreterContext() {
    return createInterpreterContext("id");
  }
  private InterpreterContext createInterpreterContext(String paragraphId) {
    return new InterpreterContext(
        "note",
        paragraphId,
        "title",
        "text",
        new HashMap<String, Object>(),
        new GUI(),
        new AngularObjectRegistry(intpGroup.getId(), null),
        new LinkedList<InterpreterContextRunner>(),
        new InterpreterOutput(),
        new ResourcePool(null),
        null);
  }


  @Test
  public void testRemoteInterperterCall() throws TTransportException, IOException {
    Properties p = new Properties();

    RemoteInterpreter intpA = new RemoteInterpreter(
        p,
        MockInterpreterA.class.getName(),
        new File("../bin/interpreter.sh").getAbsolutePath(),
        "fake",
        env,
        10 * 1000,
        localRepo
        );

    intpGroup.add(intpA);
    intpA.setInterpreterGroup(intpGroup);

    RemoteInterpreter intpB = new RemoteInterpreter(
        p,
        MockInterpreterB.class.getName(),
        new File("../bin/interpreter.sh").getAbsolutePath(),
        "fake",
        env,
        10 * 1000,
        localRepo
        );

    intpGroup.add(intpB);
    intpB.setInterpreterGroup(intpGroup);


    InterpreterConnectionFactory process = intpA.getInterpreterConnectionFactory();
    process.equals(intpB.getInterpreterConnectionFactory());

    assertFalse(process.isRunning());
    assertEquals(0, process.getNumIdleClient());
    assertEquals(0, process.referenceCount());

    intpA.open();
    assertTrue(process.isRunning());
    assertEquals(1, process.getNumIdleClient());
    assertEquals(1, process.referenceCount());

    intpA.interpret("1", createInterpreterContext());

    intpB.open();
    assertEquals(2, process.referenceCount());

    intpA.close();
    assertEquals(1, process.referenceCount());
    intpB.close();
    assertEquals(0, process.referenceCount());

    assertFalse(process.isRunning());

  }

  @Test
  public void testRemoteInterperterErrorStatus() throws TTransportException, IOException {
    Properties p = new Properties();

    RemoteInterpreter intpA = new RemoteInterpreter(
        p,
        MockInterpreterA.class.getName(),
        new File("../bin/interpreter.sh").getAbsolutePath(),
        "fake",
        env,
        10 * 1000,
        localRepo
        );

    intpGroup.add(intpA);
    intpA.setInterpreterGroup(intpGroup);

    intpA.open();
    InterpreterResult ret = intpA.interpret("non numeric value", createInterpreterContext());

    assertEquals(Code.ERROR, ret.code());
  }

  @Test
  public void testRemoteSchedulerSharing() throws TTransportException, IOException {
    Properties p = new Properties();

    RemoteInterpreter intpA = new RemoteInterpreter(
        p,
        MockInterpreterA.class.getName(),
        new File("../bin/interpreter.sh").getAbsolutePath(),
        "fake",
        env,
        10 * 1000,
        localRepo
        );

    intpGroup.add(intpA);
    intpA.setInterpreterGroup(intpGroup);

    RemoteInterpreter intpB = new RemoteInterpreter(
        p,
        MockInterpreterB.class.getName(),
        new File("../bin/interpreter.sh").getAbsolutePath(),
        "fake",
        env,
        10 * 1000,
        localRepo
        );

    intpGroup.add(intpB);
    intpB.setInterpreterGroup(intpGroup);

    intpA.open();
    intpB.open();

    long start = System.currentTimeMillis();
    InterpreterResult ret = intpA.interpret("500", createInterpreterContext());
    assertEquals("500", ret.message());

    ret = intpB.interpret("500", createInterpreterContext());
    assertEquals("1000", ret.message());
    long end = System.currentTimeMillis();
    assertTrue(end - start >= 1000);


    intpA.close();
    intpB.close();

    InterpreterConnectionFactory process = intpA.getInterpreterConnectionFactory();
    assertFalse(process.isRunning());
  }

  @Test
  public void testRemoteSchedulerSharingSubmit() throws TTransportException, IOException, InterruptedException {
    Properties p = new Properties();

    final RemoteInterpreter intpA = new RemoteInterpreter(
        p,
        MockInterpreterA.class.getName(),
        new File("../bin/interpreter.sh").getAbsolutePath(),
        "fake",
        env,
        10 * 1000,
        localRepo
        );

    intpGroup.add(intpA);
    intpA.setInterpreterGroup(intpGroup);

    final RemoteInterpreter intpB = new RemoteInterpreter(
        p,
        MockInterpreterB.class.getName(),
        new File("../bin/interpreter.sh").getAbsolutePath(),
        "fake",
        env,
        10 * 1000,
        localRepo
        );

    intpGroup.add(intpB);
    intpB.setInterpreterGroup(intpGroup);

    intpA.open();
    intpB.open();

    long start = System.currentTimeMillis();
    Job jobA = new Job("jobA", "jobA", null, 300) {

      @Override
      public int progress() {
        return 0;
      }

      @Override
      public Map<String, Object> info() {
        return null;
      }

      @Override
      protected Object jobRun() throws Throwable {
        return intpA.interpret("500", createInterpreterContext("jobA"));
      }

      @Override
      protected boolean jobAbort() {
        return false;
      }

    };
    intpA.getScheduler().submit(jobA);

    Job jobB = new Job("jobB", "jobA", null, 300) {

      @Override
      public int progress() {
        return 0;
      }

      @Override
      public Map<String, Object> info() {
        return null;
      }

      @Override
      protected Object jobRun() throws Throwable {
        return intpB.interpret("500", createInterpreterContext("jobB"));
      }

      @Override
      protected boolean jobAbort() {
        return false;
      }

    };
    intpB.getScheduler().submit(jobB);

    // wait until both job finished
    while (jobA.getStatus() != Status.FINISHED ||
           jobB.getStatus() != Status.FINISHED) {
      Thread.sleep(100);
    }

    long end = System.currentTimeMillis();
    assertTrue(end - start >= 1000);

    assertEquals("1000", ((InterpreterResult) jobB.getReturn()).message());

    intpA.close();
    intpB.close();
    InterpreterConnectionFactory process = intpA.getInterpreterConnectionFactory();
    assertFalse(process.isRunning());
  }

  @Test
  public void testRunOrderPreserved() throws InterruptedException {
    Properties p = new Properties();

    final RemoteInterpreter intpA = new RemoteInterpreter(
        p,
        MockInterpreterA.class.getName(),
        new File("../bin/interpreter.sh").getAbsolutePath(),
        "fake",
        env,
        10 * 1000,
        localRepo
        );

    intpGroup.add(intpA);
    intpA.setInterpreterGroup(intpGroup);

    intpA.open();

    int concurrency = 3;
    final List<String> results = new LinkedList<String>();

    Scheduler scheduler = intpA.getScheduler();
    for (int i = 0; i < concurrency; i++) {
      final String jobId = Integer.toString(i);
      scheduler.submit(new Job(jobId, Integer.toString(i), null, 200) {

        @Override
        public int progress() {
          return 0;
        }

        @Override
        public Map<String, Object> info() {
          return null;
        }

        @Override
        protected Object jobRun() throws Throwable {
          InterpreterResult ret = intpA.interpret(getJobName(), createInterpreterContext(jobId));

          synchronized (results) {
            results.add(ret.message());
            results.notify();
          }
          return null;
        }

        @Override
        protected boolean jobAbort() {
          return false;
        }

      });
    }

    // wait for job finished
    synchronized (results) {
      while (results.size() != concurrency) {
        results.wait(300);
      }
    }

    int i = 0;
    for (String result : results) {
      assertEquals(Integer.toString(i++), result);
    }
    assertEquals(concurrency, i);

    intpA.close();
  }


  @Test
  public void testRunParallel() throws InterruptedException {
    Properties p = new Properties();
    p.put("parallel", "true");

    final RemoteInterpreter intpA = new RemoteInterpreter(
        p,
        MockInterpreterA.class.getName(),
        new File("../bin/interpreter.sh").getAbsolutePath(),
        "fake",
        env,
        10 * 1000,
        localRepo
        );

    intpGroup.add(intpA);
    intpA.setInterpreterGroup(intpGroup);

    intpA.open();

    int concurrency = 4;
    final int timeToSleep = 1000;
    final List<String> results = new LinkedList<String>();
    long start = System.currentTimeMillis();

    Scheduler scheduler = intpA.getScheduler();
    for (int i = 0; i < concurrency; i++) {
      final String jobId = Integer.toString(i);
      scheduler.submit(new Job(jobId, Integer.toString(i), null, 300) {

        @Override
        public int progress() {
          return 0;
        }

        @Override
        public Map<String, Object> info() {
          return null;
        }

        @Override
        protected Object jobRun() throws Throwable {
          String stmt = Integer.toString(timeToSleep);
          InterpreterResult ret = intpA.interpret(stmt, createInterpreterContext(jobId));
          synchronized (results) {
            results.add(ret.message());
            results.notify();
          }
          return stmt;
        }

        @Override
        protected boolean jobAbort() {
          return false;
        }

      });
    }

    // wait for job finished
    synchronized (results) {
      while (results.size() != concurrency) {
        results.wait(300);
      }
    }

    long end = System.currentTimeMillis();

    assertTrue(end - start < timeToSleep * concurrency);

    intpA.close();
  }

  @Test
  public void testInterpreterGroupResetBeforeProcessStarts() {
    Properties p = new Properties();

    RemoteInterpreter intpA = new RemoteInterpreter(
        p,
        MockInterpreterA.class.getName(),
        new File("../bin/interpreter.sh").getAbsolutePath(),
        "fake",
        env,
        10 * 1000,
        localRepo
        );

    intpA.setInterpreterGroup(intpGroup);
    InterpreterConnectionFactory processA = intpA.getInterpreterConnectionFactory();

    intpA.setInterpreterGroup(new InterpreterGroup(intpA.getInterpreterGroup().getId()));
    InterpreterConnectionFactory processB = intpA.getInterpreterConnectionFactory();

    assertNotSame(processA.hashCode(), processB.hashCode());
  }

  @Test
  public void testInterpreterGroupResetAfterProcessFinished() {
    Properties p = new Properties();

    RemoteInterpreter intpA = new RemoteInterpreter(
        p,
        MockInterpreterA.class.getName(),
        new File("../bin/interpreter.sh").getAbsolutePath(),
        "fake",
        env,
        10 * 1000,
        localRepo
        );

    intpA.setInterpreterGroup(intpGroup);
    InterpreterConnectionFactory processA = intpA.getInterpreterConnectionFactory();
    intpA.open();

    processA.dereference();    // intpA.close();

    intpA.setInterpreterGroup(new InterpreterGroup(intpA.getInterpreterGroup().getId()));
    InterpreterConnectionFactory processB = intpA.getInterpreterConnectionFactory();

    assertNotSame(processA.hashCode(), processB.hashCode());
  }

  @Test
  public void testInterpreterGroupResetDuringProcessRunning() {
    Properties p = new Properties();

    RemoteInterpreter intpA = new RemoteInterpreter(
        p,
        MockInterpreterA.class.getName(),
        new File("../bin/interpreter.sh").getAbsolutePath(),
        "fake",
        env,
        10 * 1000,
        localRepo
        );

    intpA.setInterpreterGroup(intpGroup);
    InterpreterConnectionFactory processA = intpA.getInterpreterConnectionFactory();
    intpA.open();

    intpA.setInterpreterGroup(new InterpreterGroup(intpA.getInterpreterGroup().getId()));
    InterpreterConnectionFactory processB = intpA.getInterpreterConnectionFactory();

    assertEquals(processA.hashCode(), processB.hashCode());

    processA.dereference();     // intpA.close();

  }
}
