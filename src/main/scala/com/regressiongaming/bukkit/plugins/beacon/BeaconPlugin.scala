package com.regressiongaming.bukkit.plugins.beacon

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.util.parsing.json.JSONArray
import scala.util.parsing.json.JSONObject
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.Event.Priority
import org.bukkit.event.Event
import org.bukkit.plugin.java.JavaPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.util.ArrayStack
import akka.actor.Actor
import org.bukkit.event.Listener
import akka.actor.ActorRef

/**
 * @author ${user.name}
 */
class PermissionDeniedException(message : String) extends RuntimeException(message) {
	def this() = this(null)
}

class BeaconPlugin extends JavaPlugin {
  
  val logger = LoggerFactory.getLogger("Minecraft.Beacons")
  var playerListener : Listener = null
  var entityListener : Listener = null
  
  private var beaconConfigActor = Actor.actorOf(new BeaconConfigActor(this))
  private var beaconCommandActor = Actor.actorOf[BeaconCommandActor]

  override def onEnable = {
    beaconConfigActor.start()
    beaconCommandActor.start()
  
    playerListener = BeaconPlayerListener(this)
    entityListener = BeaconEntityListener(beaconCommandActor)
    
    getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Monitor, this)
    getServer().getPluginManager().registerEvent(Event.Type.ENTITY_DEATH, entityListener, Priority.Monitor, this)

    logInfo("Beacon version " + getDescription().getVersion() + " enabled")
  }
  
  override def onDisable = {
    
    entityListener = null
    playerListener = null
    
    beaconCommandActor.stop()

    logInfo("Beacon is disabled.")
  }
  
  override def onCommand(sender:CommandSender, command:Command, commandLabel:String, args:Array[String]) : Boolean = {
    val cmd = BeaconCommand(sender, command, commandLabel, args)
    (beaconCommandActor ? cmd) onResult {
      case BeaconCommandSuccess() => true
      case BeaconCommandError(msg) => sender.sendMessage("[beacon] Error:" + msg)
    }
    true
  }
   
  def logInfo(msg:String) = logger.info(msg)
  def logWarning(msg:String) = logger.warn(msg)

}
