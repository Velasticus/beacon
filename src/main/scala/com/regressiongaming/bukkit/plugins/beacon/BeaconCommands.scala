package com.regressiongaming.bukkit.plugins.beacon

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.command.Command
import org.bukkit.Server
import org.bukkit.plugin.java.JavaPlugin

abstract trait BeaconCommandMsg
sealed case class BeaconCommandSuccess extends BeaconCommandMsg
sealed case class BeaconCommandError(error:String = null) extends BeaconCommandMsg
sealed case class BeaconCommand(val player:CommandSender) extends BeaconCommandMsg
sealed case class UsageBeaconCommand(override val player:CommandSender, error:String) extends BeaconCommand(player)

case class AddBeaconCommand(override val player:Player, beaconType:String, beaconName:String, desc:String) extends BeaconCommand(player)

case class DeleteBeaconCommand(override val player:Player, beaconName:String) extends BeaconCommand(player)

case class ListBeaconCommand(override val player:Player) extends BeaconCommand(player)

case class PlayerDeathBeaconCommand(override val player:Player) extends BeaconCommand(player)

object BeaconCommand {
  def apply (sender:CommandSender, command:Command, commandLabel:String, args:Array[String]) : BeaconCommand = {
    var argsBuffer = List.fromArray(args)
    val cmd = commandLabel match {
      case "beacon" => sender match {
          case player : Player => {
            argsBuffer match {
              case "add" :: opts => {
                var beaconType = "default"
                var beaconName = opts.head
                argsBuffer = opts.tail
                if (beaconName.startsWith("type=")) {
                  beaconType = beaconName.splitAt(5)._2
                  beaconName = argsBuffer.head
                  argsBuffer = argsBuffer.tail
                }
                val desc = if(argsBuffer.length > 0) argsBuffer.reduce(( a:String, b:String ) => a.concat(" ".concat(b))) else ""
                AddBeaconCommand(player, beaconType, beaconName, desc)
              }
              case "delete" :: opts => DeleteBeaconCommand(player, opts.head)
              case "list" :: opts => ListBeaconCommand(player)
              case _ => UsageBeaconCommand(player,null)
            }
          }
          case _ => UsageBeaconCommand(sender, "This command cannot be used from the console");
      }
      // if (args.length < 1){return UsageBeaconCommand();}
      //build landmark name from args 0+
      // val name = args(0);
    }
    cmd
  }
}