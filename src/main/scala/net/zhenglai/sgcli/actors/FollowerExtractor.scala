package net.zhenglai.sgcli.actors

import akka.actor.{Actor, ActorLogging, Props}
import org.json4s.JsonAST.{JString, JField, JObject, JArray}

/**
  * Created by Zhenglai on 7/13/16.
  */

object FollowerExtractor {
  // messages
  case class Extract(login: String, jsonResponse: JArray)

  // props factory
  def props = Props[FollowerExtractor]
}

class FollowerExtractor extends Actor with ActorLogging {
  import FollowerExtractor._
  override def receive: Receive = {
    case Extract(login: String, followerArray: JArray) =>
      val followers = for {
        JObject(follower) <- followerArray
        JField("login", JString(login)) <- follower
      } yield login
      log.info(s"$login -> ${followers.mkString(", ")}")
    case unknown => log.info(s"Unknown message: $unknown")
  }
}
