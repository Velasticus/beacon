/*
 * Copyright 2001-2009 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.regressiongaming.bukkit.plugins.beacon

/*
ScalaTest facilitates different styles of testing by providing traits you can mix
together to get the behavior and syntax you prefer.  A few examples are
included here.  For more information, visit:

http://www.scalatest.org/

One way to use ScalaTest is to help make JUnit or TestNG tests more
clear and concise. Here's an example:
*/
import org.junit.runner.RunWith
import org.scalatest.Spec
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.junit.Test
import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import java.util.UUID
import akka.testkit.TestActorRef
import scalax.file.Path
import scalax.file.ramfs.RamFileSystem

@RunWith(classOf[JUnitRunner])
class BeaconFileActorTest extends Spec with MustMatchers  {
  val uuid = UUID.randomUUID()
  val beacons = Beacon("FirstBeacon", "PlayerOne", uuid, BeaconLoc(1,1,1), "Test description") :: 
                Beacon("SecondBeacon", "PlayerTwo", uuid, BeaconLoc(1,1,1), "Test description") ::
                Nil
  
  val tmpDir = Path.createTempDirectory()
  val filePath = tmpDir./("beacons.json").createFile()
  val filePathName = filePath.toAbsolute.path
  
  describe("A BeaconFileActor") {
    it("should return an error if a filename has not been set") {
      val actorRef = TestActorRef[BeaconFileActor].start()
      
      (actorRef ? Load()).as[BeaconFileMessage] must be (Some(ErrorResult("Filename is not set")))
      (actorRef ? Save(beacons)).as[BeaconFileMessage] must be (Some(ErrorResult("Filename is not set")))
      actorRef.stop()
    }

    it("should accept a filename and be able to write a list of beacons to a json file") {
      val actorRef = TestActorRef[BeaconFileActor].start()
      ( actorRef ? SetFileName(filePathName) ).as[BeaconFileMessage] must be (Some(SuccessfulResult()))
      
      val result = (actorRef ? Save(beacons)).as[BeaconFileMessage]
      result must be (Some(SuccessfulResult()))
      actorRef.stop()
    }

    it("should accept a filename and be able to read a list of beacons from a json file") {
      val actorRef = TestActorRef[BeaconFileActor].start()
      ( actorRef ? SetFileName(filePathName) ).as[BeaconFileMessage] must be (Some(SuccessfulResult()))
      
      val result = (actorRef ? Load).as[LoadResults]
      val beaconResults = result.get.beacons
      beaconResults must be (beacons)
    }
  }
}


