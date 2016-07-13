package net.zhenglai.sgcli.actors

import akka.actor.ActorSystem
import net.zhenglai.sgcli.util.Credentials

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by Zhenglai on 7/13/16.
  */
object Sgcli extends App {

  val token = Credentials.get("GHTOKEN")

  val system = ActorSystem("Sgcli")

  val fetcherManager = system.actorOf(FetcherManger.props(token, 2))

  fetcherManager ! FetcherManger.AddToQueue("zhenglaizhang")

  system.scheduler.scheduleOnce(15 seconds) {
    system terminate
  }
}
