package net.zhenglai.sgcli.actors

import akka.actor.ActorSystem
import akka.routing.RoundRobinPool
import net.zhenglai.sgcli.util.Credentials
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by Zhenglai on 7/13/16.
  */
object Sgcli extends App {

  // messages and factory
  import Fetcher._

  val token = Credentials.get("GHTOKEN")

  val system = ActorSystem("Sgcli")

  // actors...
  val followerExtractor = system.actorOf(FollowerExtractor.props, name = "follwerExtractor")
  val responseInterpreter = system.actorOf(ResponseInterpreter.props(followerExtractor), name = "responseInterpreter")

  // router
  val router = system.actorOf(RoundRobinPool(4).props(
    Fetcher.props(token, responseInterpreter)
  ))

  List("odersky", "rkuhn", "zhenglaizhang") foreach {
    login => router ! Fetch(login)
  }

  system.scheduler.scheduleOnce(15 seconds) {
    system terminate
  }
}
