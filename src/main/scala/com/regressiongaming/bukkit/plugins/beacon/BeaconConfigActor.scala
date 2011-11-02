package com.regressiongaming.bukkit.plugins.beacon

import java.io.File
import java.net.URI

import org.bukkit.configuration.file.YamlConfiguration

import akka.actor.scala2ActorRef
import akka.actor.Actor

sealed trait BeaconConfigMessage

case class BeaconConfigNotInitialized extends BeaconConfigMessage
case class BeaconConfigSuccess extends BeaconConfigMessage
case class SetConfigFile(loc:String) extends BeaconConfigMessage
case class SetConfigURI(uri:URI) extends BeaconConfigMessage
case class SaveConfig extends BeaconConfigMessage
case class LoadConfig extends BeaconConfigMessage
case class ReloadConfig extends BeaconConfigMessage
case class GetValue(key:String) extends BeaconConfigMessage
case class GetValueOrElse(key:String, default:String) extends BeaconConfigMessage
case class GetValueResult(value:String) extends BeaconConfigMessage
case class ItemNotFound extends BeaconConfigMessage
case class SetValue(key:String,value:String) extends BeaconConfigMessage

class BeaconConfigActor extends Actor {
  var beaconConfig : YamlConfiguration = new YamlConfiguration()
  var file : File = null
  
  def receive = {
    case SetConfigFile(name) => setConfigFile(name)
    case SetConfigURI(uri) => setConfigFile(uri)
    case _ => self reply BeaconConfigNotInitialized()
  }
  
  def receiveConfigured : Receive = {
    case SetConfigFile(name) => {
      file = new File(name)
      beaconConfig = YamlConfiguration.loadConfiguration(file)
      self reply BeaconConfigSuccess()
    }
    case SetConfigURI(uri) => beaconConfig = YamlConfiguration.loadConfiguration(uri.toURL().openStream())
    case SaveConfig => {
      if (file != null)
        beaconConfig.save(file)
    }
    case GetValue(key) => { val result = beaconConfig.getString(key) 
      self reply (if (result!=null) GetValueResult(result) else ItemNotFound())
    }
    case GetValueOrElse(key,default) => self reply GetValueResult(beaconConfig.getString(key,default))
    case SetValue(key,value) => {
      beaconConfig.set(key,value)
      self reply BeaconConfigSuccess()
    }
  }
  
  def setConfigFile(name:String) = {
    file = new File(name)
    beaconConfig = YamlConfiguration.loadConfiguration(file)
    self reply BeaconConfigSuccess()
    become(receiveConfigured orElse receive)
  }
  
  def setConfigFile(uri:URI) = {
    beaconConfig = YamlConfiguration.loadConfiguration(uri.toURL().openStream())
    self reply BeaconConfigSuccess()
    become(receiveConfigured orElse receive)    
  }
  
}