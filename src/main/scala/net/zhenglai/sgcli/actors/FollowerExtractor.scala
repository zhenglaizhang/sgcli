package net.zhenglai.sgcli.actors

import akka.actor.{ActorRef, Actor, ActorLogging, Props}
import org.json4s.JsonAST.{JString, JField, JObject, JArray}

/**
  * Created by Zhenglai on 7/13/16.
  */

object FollowerExtractor {

  // messages
  case class Extract(login: String, jsonResponse: JArray)

  // props factory
  def props(manager: ActorRef) = Props(classOf[FollowerExtractor], manager)
}

class FollowerExtractor(fetcherManager: ActorRef) extends Actor with ActorLogging {

  import FollowerExtractor._

  override def receive: Receive = {
    case Extract(login: String, followerArray: JArray) =>
      val followers = extractFollowers(followerArray)
      followers foreach { f =>
        fetcherManager ! FetcherManger.AddToQueue(f)
      }
      log.info(s"$login -> ${followers.mkString(", ")}")
      // @TODO store nodes in db or draw graph to screen
    case unknown => log.info(s"Unknown message: $unknown")
  }

  def extractFollowers(followerArray: JArray) = for {
    JObject(follower) <- followerArray
    JField("login", JString(login)) <- follower
  } yield login

}
