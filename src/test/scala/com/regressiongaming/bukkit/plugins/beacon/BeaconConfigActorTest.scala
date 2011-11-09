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
import org.mockito.Mockito._
import org.bukkit.plugin.java.JavaPlugin
import org.mockito.stubbing.Answer
import org.mockito.invocation.InvocationOnMock

@RunWith(classOf[JUnitRunner])
class BeaconConfigActorTest extends Spec with MustMatchers  {
  val uuid = UUID.fromString("5d200b4a-5427-4374-b345-6203a3e585d1")
  val tmpDir = Path.createTempDirectory()
  val configPath = tmpDir./("configuration.yml").createFile()
  val config = YamlConfiguration.loadConfiguration(new File(configPath.toURI))
  config.set("beaconsfile","/beacons.json")
  config.save(configPath.toAbsolute.path)
  
  val plugin = mock(classOf[JavaPlugin])
  when(plugin.getConfig()).thenReturn(config)
  when(plugin.saveConfig()).thenAnswer(new Answer[Any]() {
      override def answer(invocation:InvocationOnMock) : Object = {
          config.save(configPath.toAbsolute.path)
          null
      }
  })
  
  describe("A BeaconConfigActor") {
    
    it("should start up properly") {
      val actorRef = TestActorRef(new BeaconConfigActor(plugin)).start()

      actorRef.stop()
    }
    
    it("should return a pointer to the creating plugin when requested") {
      val actorRef = TestActorRef(new BeaconConfigActor(plugin)).start()

      val value = (actorRef ? BCM_GetPlugin()).as[BeaconConfigMessage]
      value must be (Some(BCM_GetPluginResult(plugin)))
      
      actorRef.stop()
    }

    it("should return a default value if a requested key is not in the config and a default is provided") {
      val actorRef = TestActorRef(new BeaconConfigActor(plugin)).start()

      val value = (actorRef ? BCM_GetValueOrElse("missing.config.item","default_result")).as[BeaconConfigMessage]
      value must be (Some(BCM_GetValueResult("default_result")))
      
      actorRef.stop()
    }

    it("should return None if a requested key is not in the config and a default is not provided") {
      val actorRef = TestActorRef(new BeaconConfigActor(plugin)).start()

      val value = (actorRef ? BCM_GetValue("missing.config.item")).as[BeaconConfigMessage]
      value must be (Some(BCM_ItemNotFound()))
      
      actorRef.stop()
    }

    it("should set a key to the requested value") {
      val actorRef = TestActorRef(new BeaconConfigActor(plugin)).start()

      (actorRef ? BCM_SetValue("new.config.item","new_value")).as[BeaconConfigMessage] must be (Some(BCM_Success()))
      
      val value = (actorRef ? BCM_GetValue("new.config.item")).as[BeaconConfigMessage]
      value must be (Some(BCM_GetValueResult("new_value")))

      actorRef.stop()
    }

    it("should save the config when requested") {
      val plugin = mock(classOf[JavaPlugin])
      when(plugin.getConfig()).thenReturn(config)
      when(plugin.saveConfig()).thenAnswer(new Answer[Any]() {
        override def answer(invocation:InvocationOnMock) : Object = {
        config.save(configPath.toAbsolute.path)
        null
      }})
  
      val actorRef = TestActorRef(new BeaconConfigActor(plugin)).start()

      (actorRef ? BCM_SetValue("new.config.item","new_value")).as[BeaconConfigMessage] must be (Some(BCM_Success()))
      
      val value = (actorRef ? BCM_GetValue("new.config.item")).as[BeaconConfigMessage]
      value must be (Some(BCM_GetValueResult("new_value")))

      (actorRef ? BCM_SaveConfig()).as[BeaconConfigMessage] must be (Some(BCM_Success()))
      
      // Make sure saveConfig was called once
      verify(plugin).saveConfig()
      
      actorRef.stop()
    }

  }
}
