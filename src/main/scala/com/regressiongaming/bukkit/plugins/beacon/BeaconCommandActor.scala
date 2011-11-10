package com.regressiongaming.bukkit.plugins.beacon

import scala.collection.TraversableOnce.flattenTraversableOnce
import scala.collection.mutable.HashMap
import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.actor.Actor._
import akka.actor.ActorRef
import akka.util.duration._
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import akka.actor.UntypedActor
import akka.actor.UntypedChannel
import org.bukkit.Material

class BeaconCommandActor extends Actor {
  private var beacons = HashMap[String,HashMap[String,Beacon]]()
  private var beaconFileActor = Actor.actorOf[BeaconFileActor].start()
  private var beaconConfigActor : ActorRef = null
  private var plugin : JavaPlugin = null
  
  override def preStart = {
    implicit val timeout = Timeout(60 seconds)
    beaconConfigActor = Actor.registry.actorFor[BeaconConfigActor].get
    plugin = (beaconConfigActor ? BCM_GetPlugin()).as[BCM_GetPluginResult].get.plugin

    ( beaconConfigActor ? BCM_GetValue("beacon.json_file") ).as[BeaconConfigMessage] match {
      case Some(BCM_GetValueResult(value)) => ( beaconFileActor ? SetFileName(value) ).as[BeaconFileMessage] match {
        case Some(SuccessfulResult()) => ( beaconFileActor ? Load() ).as[BeaconFileMessage] match {
          case Some(LoadResults(beaconList)) => beaconList.foreach(b =>
            beacons.getOrElseUpdate(b.playerName,HashMap[String,Beacon]())(b.name) = b
          )
          case Some(ErrorResult(error)) => throw new RuntimeException("Error starting Beacon command actor: " + error)
          case x : Any => throw new RuntimeException("Error starting Beacon command actor: " + x.toString)
        }
        case x : Any => throw new RuntimeException("Error starting Beacon command actor: " + x.toString)
      }
      case x : Any => throw new RuntimeException("Error starting Beacon command actor: " + x.toString)
    }
  }
  
  def receive = {
    case cmd : AddBeaconCommand => addBeacon(self.channel, cmd)
    case cmd : DeleteBeaconCommand => delBeacon(self.channel, cmd)
    case cmd : ListBeaconCommand => listBeacons(self.channel, cmd.player)
    case cmd : PlayerDeathBeaconCommand => handlePlayerDeath(self.channel, cmd.player)
    case cmd : UsageBeaconCommand => usage(cmd.player)
    case cmd : BeaconCommand => usage(cmd.player)
  }

  def addBeacon( sender:UntypedChannel, cmd:AddBeaconCommand ) : Unit = addBeacon(sender, cmd.player, cmd.beaconType, cmd.beaconName, cmd.desc)
  
  def addBeacon( sender:UntypedChannel, player:Player, beaconType:String, beaconName:String, desc:String) : Unit = {
    val curr = beacons.getOrElse(player.getName,HashMap[String,Beacon]()).getOrElse(beaconName, null)
//    if ( curr != null ) {
//      val perm = this.getServer().getPluginManager().getPermission("beacon.overwrite")
//    }

    val loc = player.getLocation
    val beacon = Beacon(beaconName, player.getName, player.getUniqueId, loc.getX, loc.getY, loc.getZ, desc)
    beacons.getOrElseUpdate(player.getName,HashMap[String,Beacon]())(beaconName) = beacon
    save(sender, player, "Added beacon named " + beaconName + " at " + beacon.loc)    
  }
  
  def delBeacon( sender:UntypedChannel, player: Player, name: String ) : Unit = {
    beacons.getOrElse(player.getName,HashMap[String,Beacon]()).remove(name) match {
      case Some(beacon) => save(sender, player, "Removed beacon " + name + " at " + beacon.loc)
      case None => sender ! BeaconCommandError("Beacon " + name + " not found")
    }
  }
  
  def delBeacon( sender:UntypedChannel, cmd:DeleteBeaconCommand ) : Unit = delBeacon(sender, cmd.player, cmd.beaconName) 
  
  def listBeacons ( sender:UntypedChannel, player: Player ) : Unit = {
    // This is one heck of a line: It gets the beacons for a player (defaulting to an empty map),
    // converts them to a list which it then sorts and then sends a message to the player for each one
    beacons.getOrElse(player.getName,HashMap[String,Beacon]()).values.toList.sortBy(b=>b.name).foreach( beacon => player.sendMessage("[beacon] " + beacon.name + " @" + beacon.loc + " - " + beacon.desc ))
    sender ! BeaconCommandSuccess()
  }
 
  def handlePlayerDeath ( sender:UntypedChannel, player: Player ) : Unit = {
    var loc = player.getLocation
    var inv = player.getInventory
    if (!inv.contains(Material.COMPASS))
      inv.addItem(new ItemStack(Material.COMPASS,1))
    var tgt = player.getCompassTarget()
    
    // Create a beacon to represent the players old compass target and add it to the beacon map IF
    // one hasn't already been added (getOrElseUpdate trickery). We want to track their initial
    // compass target, without wiping it out if they die more than once before retrieving their corpse.
    val tgtBeacon = Beacon(player.getName, player.getName, player.getUniqueId, tgt.getX, tgt.getY, tgt.getZ, "Initial compass target")
    beacons.getOrElseUpdate("compass_targets",HashMap[String,Beacon]()).getOrElseUpdate(player.getName,tgtBeacon)
    
    // TODO I think I need to either track multiple corpse targets for a player or their first one
    // It stands to reason that the first corpse target would be the one with the most valuable gear
    player.setCompassTarget(loc)
    val beacon = Beacon("CORPSE", player.getName, player.getUniqueId, loc.getX, loc.getY, loc.getZ, "Corpse marker")
    beacons.getOrElseUpdate(player.getName,HashMap[String,Beacon]())("CORPSE") = beacon
    
    save(sender, player, "A beacon was created where you died and a compass pointing there has been added to your inventory")
  }
 
  def usage( player: CommandSender ) = usages.foreach(msg => player.sendMessage("[beacon] "+msg))
  
  def usage( player: CommandSender, error: String ) : Unit = {
    player.sendMessage("[beacon] Error - " + error)
    usage(player)
  }
  
  def save(sender:UntypedChannel, player:Player, msg:String) = {
    (beaconFileActor ? Save(beacons.valuesIterator.map( _.values ).flatten.toList)).onResult ({
      case res : SuccessfulResult => {
        if (msg != null) player.sendMessage("[beacon] " + msg )
        sender ! BeaconCommandSuccess()
      }
      case res : Any => {
        player.sendMessage("[beacon] Error: " + res.toString)
        sender ! BeaconCommandError()
      }
    })
  }
  
  val usages =
           "/beacon add name [type=type] [description] - Create a beacon at your current position" :: 
           "/beacon update name [type=type] [description] - Will modify type and/or description of a named beacon." ::
           "/beacon delete name - Will delete the beacon with the specified name." ::
           "/beacon list - Will list all beacons you own." ::
           Nil

}