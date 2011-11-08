/**
 *
 */
package com.regressiongaming.bukkit.plugins.beacon

import org.bukkit.event.player.PlayerListener
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.entity.EntityListener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.entity.Player
import akka.actor.ActorRef

/**
 * @author jeckhart
 *
 */
case class BeaconPlayerListener(val plugin : JavaPlugin) extends PlayerListener {

  override def onPlayerJoin(event:PlayerJoinEvent) = {
		event.getPlayer().sendMessage("Beacon welcomes you!")
	}

}

case class BeaconEntityListener(val beaconCommandActor : ActorRef) extends EntityListener {
  
	override def onEntityDeath(event:EntityDeathEvent) = {
	  event.getEntity() match {
	    case p:Player => p.sendMessage("I'm sorry you died")
	  }
	}

}