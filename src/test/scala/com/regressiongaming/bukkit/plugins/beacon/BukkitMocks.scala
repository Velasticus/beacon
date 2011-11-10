package com.regressiongaming.bukkit.plugins.beacon

import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar
import org.bukkit.entity.Player
import org.bukkit.Location
import java.util.UUID
import org.bukkit.configuration.file.YamlConfiguration
import org.mockito.stubbing.Answer
import org.mockito.invocation.InvocationOnMock
import scala.collection.generic.MutableMapFactory
import scala.collection.mutable.HashMap
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.PlayerInventory

trait BukkitMocks extends MockitoSugar {
  
  protected val defaultPlayerName = "Player1"
  
  protected val defaultUUID = UUID.fromString("5d200b4a-5427-4374-b345-6203a3e585d1")
  protected val defaultLoc = mockLocation(1.0)
  protected val defaultPlayer = mockPlayer()
  protected val defaultInv = mockInventory()
  
  protected def mockPlayer(loc:Location) : Player = mockPlayer(defaultPlayerName, defaultUUID, loc)
  
  protected def mockPlayer(name : String = defaultPlayerName, uuid : UUID = defaultUUID, loc : Location = defaultLoc, inv : PlayerInventory = defaultInv) : Player = {
    val player = mock[Player]
    when(player.getName).thenReturn(name)
    when(player.getLocation).thenReturn(loc)
    when(player.getUniqueId).thenReturn(uuid)
    when(player.getInventory).thenReturn(inv)
    when(player.getCompassTarget).thenReturn(loc)
    player
  }
  
  protected def mockInventory() : PlayerInventory = {
    val inv = mock[PlayerInventory]
    inv
  }
  
  protected def mockLocation(i:Double = 1.0) : Location = mockLocation(i,i,i)
  
  protected def mockLocation(x:Double,y:Double,z:Double) : Location = {
      val loc = mock[org.bukkit.Location]
      when(loc.getX).thenReturn(1.0)
      when(loc.getY).thenReturn(1.0)
      when(loc.getZ).thenReturn(1.0)
      loc
  }
  
  protected def mockConfiguration() : YamlConfiguration = mockConfiguration(Map())
  
  protected def mockConfiguration(map:Map[String,String]) : YamlConfiguration = {
    val cfg = mock[YamlConfiguration]
    var cfgInner = HashMap[String,String]() ++= map
    when(cfg.getString(anyString())).thenAnswer(new Answer[String]() {
      override def answer(invocation:InvocationOnMock) : String = {
          cfgInner.getOrElse(invocation.getArguments()(0).toString,null)
      }
    })
    when(cfg.set(anyString(),anyString())).thenAnswer(new Answer[Unit]() {
      override def answer(invocation:InvocationOnMock) = {
        cfgInner.put(invocation.getArguments()(0).toString,invocation.getArguments()(1).toString)
      }
    })
    cfg
  }
  
  protected def mockPlugin() : JavaPlugin = mockPlugin(mockConfiguration())
  
  protected def mockPlugin(config:YamlConfiguration) : JavaPlugin = {
    val plugin = mock[JavaPlugin]
    when(plugin.getConfig()).thenReturn(config)
    plugin
  }

}