package com.regressiongaming.bukkit.plugins.beacon

import org.junit.runner.RunWith
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.scalatest.matchers.MustMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.BeforeAndAfter
import org.scalatest.Spec

import com.twitter.json.Json
import Beacon.BeaconsToList
import akka.actor.Actor.Timeout
import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.testkit.TestActorRef
import akka.util.duration.intToDurationInt
import scalax.file.Path
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class BeaconCommandActorTest extends Spec with MustMatchers with MockitoSugar with BukkitMocks with BeforeAndAfter {
  val beacons = (1 to 100) map { i =>
    Beacon("Beacon"+i, "Player"+i%10, defaultUUID, BeaconLoc(i,i,i), "Test description")
  }

  val tmpDir = Path.createTempDirectory()
  val filePath = tmpDir / "beacons.json"
  val plugin = mockPlugin(mockConfiguration(Map("beacon.json_file" -> filePath.toAbsolute.path)))
  val beaconConfigActor = Actor.actorOf(new BeaconConfigActor(plugin)).start()

  before {
    filePath.truncate(0)
    filePath.write(Json.build(BeaconsToList(beacons)).toString)
  }
  
  describe("A BeaconCommandActor") {

    it("should start up properly") {
      val actorRef = TestActorRef[BeaconCommandActor].start()

      actorRef.stop()
    }

    it("should accept a command to add a beacon") {
      val loc = mockLocation()
      val player = mockPlayer(loc)
      val actorRef = Actor.actorOf[BeaconCommandActor].start()

      implicit val timeout = Timeout(60 seconds)
      val value = actorRef ? AddBeaconCommand(player, "default", "NEW TEST BEACON", "NEW TEST BEACON DESC")
      val ret = value.as[BeaconCommandMsg]
      ret match {
          case Some(BeaconCommandSuccess()) => {
		      verify(player).sendRawMessage("[beacon] - Added beacon named NEW TEST BEACON at (1,1,1)")
		      verify(player, times(3)).getName // Are these really passing conditions? They seem extraneous
		      verify(player).getLocation
		      verify(loc).getX
		      verify(loc).getY
		      verify(loc).getZ
		      
		      val beaconsWritten = BeaconFileActor.readBeaconsFromFile(filePath)
		      beaconsWritten must contain (Beacon("NEW TEST BEACON", player.getName, player.getUniqueId, BeaconLoc(1,1,1), "NEW TEST BEACON DESC"))
          }
          case msg : Any => throw new RuntimeException("Failed" + msg.toString)
      }
      
      actorRef.stop()
      
    }

    it("should accept a command to delete a beacon") {
      val loc = mockLocation()
      val player = mockPlayer(loc)
      val actorRef = Actor.actorOf[BeaconCommandActor].start()

      implicit val timeout = Timeout(60 seconds)
      val value = actorRef ? DeleteBeaconCommand(player, "Beacon1")
      val ret = value.as[BeaconCommandMsg]
      ret match {
          case Some(BeaconCommandSuccess()) => {
		      verify(player).sendRawMessage("[beacon] - Removed beacon Beacon1 at (1,1,1)")
		      verify(player).getName // Is this really a passing condition? It seems extraneous
		      
		      val beaconsWritten = BeaconFileActor.readBeaconsFromFile(filePath)
		      beaconsWritten must not contain Beacon("Beacon1", "Player1", defaultUUID, BeaconLoc(1,1,1), "Test description")
          }
          case msg : Any => throw new RuntimeException("Failed" + msg.toString)
      }
      
      actorRef.stop()
      
    }
    
  }
  
}
