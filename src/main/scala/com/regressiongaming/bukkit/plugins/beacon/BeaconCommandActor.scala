package com.regressiongaming.bukkit.plugins.beacon

import scala.collection.TraversableOnce.flattenTraversableOnce
import scala.collection.mutable.HashMap
import org.bukkit.entity.Player
import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.actor.ActorRef
import org.bukkit.command.CommandSender

class BeaconCommandActor extends Actor {
  private var beacons = HashMap[String,HashMap[String,Beacon]]()
  private var beaconFileActor = Actor.actorOf[BeaconFileActor]
    
  override def preStart = {
    val config = Actor.registry.actorFor[BeaconConfigActor].get
    ( config ? GetValue("beacon.json_file") ).as[BeaconConfigMessage] match {
      case Some(GetValueResult(value)) => ( beaconFileActor ? SetFileName(value) ).as[BeaconFileMessage] match {
        case Some(SuccessfulResult()) => ( beaconFileActor ? Load() ).as[BeaconFileMessage] match {
          case Some(LoadResults(beaconList)) => beaconList.foreach(b =>
            beacons.getOrElseUpdate(b.playerName,HashMap[String,Beacon]())(b.name) = b
          )
          case Some(ErrorResult(error)) => throw new RuntimeException("Error starting Beacon command actor: " + error)
          case _ => throw new RuntimeException("Error starting Beacon command actor")
        }
        case _ => throw new RuntimeException("Error starting Beacon command actor")
      }
      case _ => throw new RuntimeException("Error starting Beacon command actor")
    }
  }
  
  def receive = {
    case cmd : AddBeaconCommand => addBeacon(cmd)
    case cmd : DeleteBeaconCommand => delBeacon(cmd)
    case cmd : ListBeaconCommand => listBeacons(cmd.player)
    case cmd : UsageBeaconCommand => usage(cmd.player)
    case cmd : BeaconCommand => usage(cmd.player)
  }

  def addBeacon( cmd:AddBeaconCommand ) : Unit = addBeacon(cmd.player, cmd.beaconType, cmd.beaconName, cmd.desc)
  
  def addBeacon( player:Player, beaconType:String, beaconName:String, desc:String) : Unit = {
    val curr = beacons.getOrElse(player.getName,HashMap[String,Beacon]()).getOrElse(beaconName, null)
//    if ( curr != null ) {
//      val perm = this.getServer().getPluginManager().getPermission("beacon.overwrite")
//    }
    val playerId = player.getUniqueId
    val loc = player.getLocation
    val beacon = Beacon(beaconName, player.getName, playerId, loc.getX, loc.getY, loc.getZ, desc)
    beacons.getOrElseUpdate(player.getName,HashMap[String,Beacon]())(beaconName) = beacon
    val beaconsList = beacons.valuesIterator.map( _.values ).flatten
    (beaconFileActor ? Save(beacons.valuesIterator.map( _.values ).flatten.toList)).onResult {
      case SuccessfulResult => player.sendRawMessage("[beacon] - Added beacon named " + beaconName + " at " + beacon.loc )
    }
    
  }
  
  def delBeacon( player: Player, name: String ) : Unit = {
    beacons.getOrElse(player.getName,HashMap[String,Beacon]()).remove(name);
  }
  
  def delBeacon( cmd:DeleteBeaconCommand ) : Unit = delBeacon(cmd.player, cmd.beaconName) 
  
  def listBeacons ( player: Player ) : Unit = {
      beacons.getOrElse(player.getName,HashMap[String,Beacon]()).values.foreach( beacon => player.sendMessage("[beacon] " + beacon.name + " @" + beacon.loc + " - " + beacon.desc ))
  }
 
  def usage( player: CommandSender ) = usages.foreach(msg => player.sendMessage("[beacon]: "+msg))
  
  def usage( player: CommandSender, error: String ) : Unit = {
    player.sendMessage("[beacon]: Error - " + error)
    usage(player)
  }
  
  val usages =
           "/beacon add name [type=type] [description] - Create a beacon at your current position" :: 
           "/beacon update name [type=type] [description] - Will modify type and/or description of a named beacon." ::
           "/beacon delete name - Will delete the beacon with the specified name." ::
           "/beacon list - Will list all beacons you own." ::
           Nil

}