package com.regressiongaming.bukkit.plugins.beacon

import com.twitter.json.Json
import akka.actor.scala2ActorRef
import akka.actor.Actor
import akka.actor.ActorRef
import akka.util.duration._
import scalax.file.Path
import scalax.io.Resource
import scalax.io.StandardOpenOption._
import scalax.io.SeekableResource
import java.io.Closeable
import java.net.URI
import akka.actor.Actor.Timeout

sealed trait BeaconFileMessage

case class SetFileName(loc:String) extends BeaconFileMessage

case class Save(beacons : List[Beacon]) extends BeaconFileMessage

case class Load extends BeaconFileMessage
case class LoadResults(beacons : List[Beacon]) extends BeaconFileMessage

case class SuccessfulResult extends BeaconFileMessage
case class ErrorResult(error:String) extends BeaconFileMessage


// Note: We should probably ask for the file from the plugin manager
class BeaconFileActor extends Actor {
  var file : Path = null

  implicit val timeout = Timeout(60 seconds)
  
  def receive = {
    case SetFileName(name) => {
      file = Path(name)
      become(receiveConfigured)
      self reply SuccessfulResult()
    }
    case _ => self reply ErrorResult("Filename is not set")
  }
  
  def receiveConfigured : Receive = {
    case Save(beacons) => {
      BeaconFileActor.saveBeaconsToFile(beacons,file)
      self reply SuccessfulResult() // perform the work
    }
    case Load() => self reply LoadResults(BeaconFileActor.readBeaconsFromFile(file))
    case SetFileName(name) => {
      file = Path(name)
      self reply SuccessfulResult()
    }
  }

}

object BeaconFileActor {
  def saveBeaconsToFile(beacons:List[Beacon], file:Path) = {
    val beaconList : List[Map[String,Any]] = beacons
    val s = Json.build(beaconList).toString
    
    file.truncate(0)
    file.write(s)
  }
  
  def readBeaconsFromFile(file:Path) = {
      val s = file.slurpString
      val jo = Json.parse(s)
      val b = jo match {
        case l : List[Map[String,Any]] => Beacon(l)
        case m : Map[String,Any] => Beacon(m) :: Nil
        case _ => Beacon(jo.asInstanceOf[List[Map[String,Any]]])
      }
      b    
  }
}