package com.regressiongaming.bukkit.plugins.beacon

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
  val uuid = UUID.fromString("5d200b4a-5427-4374-b345-6203a3e585d1")
  val beacons = Beacon("FirstBeacon", "PlayerOne", uuid, BeaconLoc(1,1,1), "Test description") :: 
                Beacon("SecondBeacon", "PlayerTwo", uuid, BeaconLoc(1,1,1), "Test description") ::
                Nil
  
  val tmpDir = Path.createTempDirectory()
  val filePath = tmpDir./("beacons.json")
  val filePathName = filePath.toAbsolute.path
  
  describe("A BeaconFileActor") {
    it("should start up properly") {
      val actorRef = TestActorRef[BeaconFileActor].start()

      actorRef.stop()
    }
    
    it("should return an error if a filename has not been set") {
      val actorRef = TestActorRef[BeaconFileActor].start()
      
      (actorRef ? Load()).as[BeaconFileMessage] must be (Some(ErrorResult("Filename is not set")))
      (actorRef ? Save(beacons)).as[BeaconFileMessage] must be (Some(ErrorResult("Filename is not set")))
      actorRef.stop()
    }

    it("should accept a filename and create the file upon write if it does not yet exist") {
      filePath.delete(true)
      val actorRef = TestActorRef[BeaconFileActor].start()
      ( actorRef ? SetFileName(filePathName) ).as[BeaconFileMessage] must be (Some(SuccessfulResult()))
      
      val result = (actorRef ? Save(beacons)).as[BeaconFileMessage]
      result must be (Some(SuccessfulResult()))
      filePath.exists must be (true)
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
      
      val result = (actorRef ? Load()).as[LoadResults]
      val beaconResults = result.get.beacons
      beaconResults must be (beacons)
    }

    it("should accept a filename and return an empty list of beacons if the file wasn't created yet") {
      filePath.deleteIfExists(true)
      val actorRef = TestActorRef[BeaconFileActor].start()
      ( actorRef ? SetFileName(filePathName) ).as[BeaconFileMessage] must be (Some(SuccessfulResult()))
      
      val result = (actorRef ? Load()).as[LoadResults]
      val beaconResults = result.get.beacons
      beaconResults must be (List())
    }
  }
}


