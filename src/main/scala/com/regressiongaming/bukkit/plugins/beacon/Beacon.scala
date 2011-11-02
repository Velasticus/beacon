package com.regressiongaming.bukkit.plugins.beacon

import java.util.UUID
import scala.util.parsing.json.JSONObject
import scala.util.parsing.json.JSONArray

class BeaconLoc( val x:Double, val y:Double, val z:Double ) {
  override def toString() : String = {
    return "(" + x.toInt + "," + y.toInt + "," + z.toInt + ")" 
  }

  override def equals(that: Any) = that match {
    case loc : BeaconLoc => loc.x == x && loc.y == y && loc.z == z
    case _ => false    
  }

}

class Beacon( val name: String, val playerName: String, val playerId: UUID, val loc: BeaconLoc, val desc:String = "" ) {  
  override def equals(that: Any) = that match {
    case beacon : Beacon => beacon.name == name && beacon.playerName == playerName && beacon.playerId == playerId && beacon.desc == desc && beacon.loc == loc
    case _ => false    
  }
}

object BeaconLoc {
  def apply( x:Double, y:Double, z:Double ) : BeaconLoc = {
    return new BeaconLoc(x,y,z)
  }
  
  def apply( obj : Map[String,Any] ) : BeaconLoc = {
    val x = toDouble(obj("x"))
    val y = toDouble(obj("y"))
    val z = toDouble(obj("z"))
    return new BeaconLoc(x,y,z)
  }
     
  implicit def BeaconLocToMap( loc : BeaconLoc ) : Map[String,Any]= {
    Map[String,Any](
      "x" -> loc.x,
      "y" -> loc.y,
      "z" -> loc.z
    )
  }
  
  private def toDouble( value : Any ) : Double = {
    value match {
      case s : String => s.toDouble
      case d : Double => d
      case f : Float  => f.toDouble
      case i : Int    => i.toDouble
      case _          => value.toString().toDouble
    }
  }
}

object Beacon {
  def apply ( name: String, playerName: String, playerId: UUID, x: Double, y: Double, z: Double, desc:String = "" ) : Beacon = Beacon(name, playerName, playerId, new BeaconLoc(x,y,z), desc)

  def apply ( name: String, playerName: String, playerId: UUID, loc : BeaconLoc, desc:String ) : Beacon = {
    return new Beacon(name, playerName, playerId, loc, desc)
  }
  def apply ( obj : Map[String,Any] ) : Beacon = {
    val name = obj("name").asInstanceOf[String]
    val playerName = obj("player_name").asInstanceOf[String]
    val playerId = UUID.fromString(obj("playerId").asInstanceOf[String])
    val loc = BeaconLoc(obj("loc").asInstanceOf[Map[String,Any]])
    val desc = obj.getOrElse("desc","").asInstanceOf[String]
    return Beacon(name, playerName, playerId, loc, desc)
  }

  def apply ( obj : List[Any] ) : List[Beacon] = obj.map( o => o match {
      case m : Map[String,Any] => Beacon(m)
  })
  
  implicit def BeaconToMap( beacon : Beacon ) = Map[String,Any]( 
      "name" -> beacon.name,
      "player_name" -> beacon.playerName,
      "playerId" -> beacon.playerId.toString(),
      "desc" -> beacon.desc,
      "loc" -> BeaconLoc.BeaconLocToMap(beacon.loc)
    )

  implicit def BeaconsToList( beacons : Iterable[Beacon] ) : List[Map[String,Any]] = beacons.map( b => BeaconToMap(b)).toList

}