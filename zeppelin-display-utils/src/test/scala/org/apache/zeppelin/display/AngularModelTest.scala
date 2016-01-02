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
package org.apache.zeppelin.display

import org.apache.zeppelin.interpreter.{InterpreterContextRunner, InterpreterContext, InterpreterGroup}
import org.scalatest.concurrent.Eventually
import org.scalatest.{Matchers, BeforeAndAfter, BeforeAndAfterEach, FlatSpec}

/**
  * Test for AngularModel
  */
class AngularModelTest extends FlatSpec
with BeforeAndAfter with BeforeAndAfterEach with Eventually with Matchers {
  override def beforeEach() {
    val intpGroup = new InterpreterGroup()
    val context = new InterpreterContext("note", "id", "title", "text",
      new java.util.HashMap[String, Object](), new GUI(), new AngularObjectRegistry(
        intpGroup.getId(), null),
      new java.util.LinkedList[InterpreterContextRunner]())

    InterpreterContext.set(context)
    super.beforeEach() // To be stackable, must call super.beforeEach
  }

  "AngularModel" should "able to create AngularObject" in {
    val registry = InterpreterContext.get().getAngularObjectRegistry
    registrySize should be(0)

    AngularModel("model1")() should be(None)
    registrySize should be(0)

    AngularModel("model1", "value1")() should be("value1")
    registrySize should be(1)

    AngularModel("model1")() should be("value1")
    registrySize should be(1)
  }

  "AngularModel" should "able to update AngularObject" in {
    val registry = InterpreterContext.get().getAngularObjectRegistry

    val model1 = AngularModel("model1", "value1")
    model1() should be("value1")
    registrySize should be(1)

    model1.value("newValue1")
    model1() should be("newValue1")
    registrySize should be(1)

    AngularModel("model1", "value2")() should be("value2")
    registrySize should be(1)
  }

  "AngularModel" should "able to remove AngularObject" in {
    AngularModel("model1", "value1")
    registrySize should be(1)

    AngularModel("model1").remove()
    registrySize should be(0)
  }


  def registry() = {
    InterpreterContext.get().getAngularObjectRegistry
  }

  def registrySize() = {
    registry().getAll(InterpreterContext.get().getNoteId).size
  }
}
