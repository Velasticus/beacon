/**
 *
 */
package com.regressiongaming.bukkit.plugins.beacon

import org.bukkit.event.player.PlayerListener
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.event.player.PlayerJoinEvent

/**
 * @author jeckhart
 *
 */
class BeaconPlayerListener(val plugin : JavaPlugin) extends PlayerListener {
	override def onPlayerJoin(event:PlayerJoinEvent) = {
		event.getPlayer().sendMessage("Beacons welcomes you!")
	}
}