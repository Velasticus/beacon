package com.regressiongaming.bukkit.plugins.beacon

import org.junit.runner.RunWith
import org.scalatest.Spec
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.junit.Test
import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import java.io.File
import java.util.UUID
import akka.testkit.TestActorRef
import scalax.file.Path
import scalax.file.ramfs.RamFileSystem
import org.bukkit.configuration.file.YamlConfiguration

@RunWith(classOf[JUnitRunner])
class BeaconConfigActorTest extends Spec with MustMatchers  {
  val uuid = UUID.randomUUID()
  val tmpDir = Path.createTempDirectory()
  val configPath = tmpDir./("configuration.yml").createFile()
  val config = YamlConfiguration.loadConfiguration(new File(configPath.toURI))
  config.set("beaconsfile","/beacons.json")
  config.save(new File(configPath.toURI))
  
  describe("A BeaconConfigActor") {
    it("should return BeaconConfigNotInitialized for all functions except SetConfig___ when no config is loaded") {
      val actorRef = TestActorRef[BeaconConfigActor].start()

      (actorRef ? SaveConfig()).as[BeaconConfigMessage] must be (Some(BeaconConfigNotInitialized()))
      (actorRef ? LoadConfig()).as[BeaconConfigMessage] must be (Some(BeaconConfigNotInitialized()))
      (actorRef ? ReloadConfig()).as[BeaconConfigMessage] must be (Some(BeaconConfigNotInitialized()))
      (actorRef ? GetValue("beaconsfile")).as[BeaconConfigMessage] must be (Some(BeaconConfigNotInitialized()))
      (actorRef ? GetValueOrElse("beaconsfile","test")).as[BeaconConfigMessage] must be (Some(BeaconConfigNotInitialized()))
      (actorRef ? SetValue("beaconsfile","test")).as[BeaconConfigMessage] must be (Some(BeaconConfigNotInitialized()))
      
      actorRef.stop()
    }

    it("should start up and accept a path to a config file and accept a query for an item in the config") {
      val actorRef = TestActorRef[BeaconConfigActor].start()

      val result = (actorRef ? SetConfigURI(configPath.toURI)).as[BeaconConfigMessage]
      result must be (Some(BeaconConfigSuccess()))
      val value = (actorRef ? GetValue("beaconsfile")).as[BeaconConfigMessage]
      value must be (Some(GetValueResult("/beacons.json")))
      
      actorRef.stop()
    }

    it("should return a default value if a requested key is not in the config and a default is provided") {
      val actorRef = TestActorRef[BeaconConfigActor].start()

      val result = (actorRef ? SetConfigURI(configPath.toURI)).as[BeaconConfigMessage]
      result must be (Some(BeaconConfigSuccess()))

      val value = (actorRef ? GetValueOrElse("missing.config.item","default_result")).as[BeaconConfigMessage]
      value must be (Some(GetValueResult("default_result")))
      
      actorRef.stop()
    }

    it("should return None if a requested key is not in the config and a default is not provided") {
      val actorRef = TestActorRef[BeaconConfigActor].start()

      val result = (actorRef ? SetConfigURI(configPath.toURI)).as[BeaconConfigMessage]
      result must be (Some(BeaconConfigSuccess()))

      val value = (actorRef ? GetValue("missing.config.item")).as[BeaconConfigMessage]
      value must be (Some(ItemNotFound()))
      
      actorRef.stop()
    }

    it("should set a key to the requested value") {
      val actorRef = TestActorRef[BeaconConfigActor].start()

      val result = (actorRef ? SetConfigURI(configPath.toURI)).as[BeaconConfigMessage]
      result must be (Some(BeaconConfigSuccess()))

      (actorRef ? SetValue("new.config.item","new_value")).as[BeaconConfigMessage] must be (Some(BeaconConfigSuccess()))
      
      val value = (actorRef ? GetValue("new.config.item")).as[BeaconConfigMessage]
      value must be (Some(GetValueResult("new_value")))

      actorRef.stop()
    }

  }
}
