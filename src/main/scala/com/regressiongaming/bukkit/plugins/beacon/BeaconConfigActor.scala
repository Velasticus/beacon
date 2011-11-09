package com.regressiongaming.bukkit.plugins.beacon

import java.io.File
import java.net.URI
import org.bukkit.configuration.file.YamlConfiguration
import akka.actor.scala2ActorRef
import akka.actor.Actor
import akka.util.duration._
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin
import akka.actor.Actor.Timeout

sealed trait BeaconConfigMessage

case class BCM_NotInitialized extends BeaconConfigMessage
case class BCM_Success extends BeaconConfigMessage
case class BCM_SaveConfig extends BeaconConfigMessage
case class BCM_ReloadConfig extends BeaconConfigMessage
case class BCM_GetPlugin extends BeaconConfigMessage
case class BCM_GetPluginResult(plugin:JavaPlugin) extends BeaconConfigMessage
case class BCM_GetValue(key:String) extends BeaconConfigMessage
case class BCM_GetValueOrElse(key:String, default:String) extends BeaconConfigMessage
case class BCM_GetValueResult(value:String) extends BeaconConfigMessage
case class BCM_ItemNotFound extends BeaconConfigMessage
case class BCM_SetValue(key:String,value:String) extends BeaconConfigMessage

class BeaconConfigActor(plugin : JavaPlugin) extends Actor {
  var beaconConfig : FileConfiguration = null

  override def preStart = {
    beaconConfig = plugin.getConfig()
  }
  
  implicit val timeout = Timeout(60 seconds)

  def receive = {
    case BCM_GetPlugin() => self reply BCM_GetPluginResult(plugin)
    case BCM_SaveConfig() => {
      plugin.saveConfig()
      self reply BCM_Success()
    }
    case BCM_ReloadConfig() => {
      plugin.reloadConfig()
      self reply BCM_Success()
    }
    case BCM_GetValue(key) => { 
      val result = beaconConfig.getString(key) 
      self reply (if (result!=null) BCM_GetValueResult(result) else BCM_ItemNotFound())
    }
    case BCM_GetValueOrElse(key,default) => self reply BCM_GetValueResult(beaconConfig.getString(key,default))
    case BCM_SetValue(key,value) => {
      beaconConfig.set(key,value)
      self reply BCM_Success()
    }
  }
  
}