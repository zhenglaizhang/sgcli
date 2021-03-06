package net.zhenglai.sgcli.actors

import akka.actor._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaj.http.Http

/**
  * Created by Zhenglai on 7/13/16.
  */
object Fetcher {

  // messages
  case class Fetch(login: String)

  case object WorkAvailabie

  // props factory
  def props(token: Option[String],
            fetcherManager: ActorRef,
            responseInterpreter: ActorRef): Props =
    Props(classOf[Fetcher], token, fetcherManager, responseInterpreter)
}

case class Fetcher(val token: Option[String],
                   val fetcherManager: ActorRef,
                   val responseInterpreter: ActorRef) extends Actor with ActorLogging {

  import Fetcher._

  override def receive: Receive = {
    case Fetch(login) => fetchFollowers(login)
    case WorkAvailabie => fetcherManager ! FetcherManger.GiveMeWork
  }

  private def fetchFollowers(login: String): Unit = {
    val unauthorizedRequest = Http(
      s"https://api.github.com/users/$login/followers"
    )

    val authorizedRequest = token map { t =>
      unauthorizedRequest.header("Authorization", s"token $t")
    }

    val request = authorizedRequest.getOrElse(unauthorizedRequest)

    val response = Future {
      request asString
    }
    response.onComplete { r =>
      responseInterpreter !
        ResponseInterpreter.InterpretResponse(login, r)
      fetcherManager ! FetcherManger.GiveMeWork
    }
  }
}

//object FetcherManualRoutingTest extends App {
//
//  // import messages
//  import Fetcher._
//
//  val system = ActorSystem("FetcherTest")
//
//  val token = Credentials.get("GHTOKEN")
//
//  val fetchers = (0 to 3) map { i =>
//    system.actorOf(Fetcher.props(token))
//  }
//
//  fetchers(0) ! Fetch("odersky")
//  fetchers(0) ! Fetch("zhenglaizhang")
//  fetchers(0) ! Fetch("scala")
//  fetchers(0) ! Fetch("rkuhn")
//
//  system.scheduler.scheduleOnce(5 seconds) {
//    system terminate
//  }
//}

/*
object FetcherAutoRoutingTest extends App {

  import Fetcher._

  val system = ActorSystem("FetcherAutoRoutingTest")

  val token = Credentials.get("GHTOKEN")

  val router = system.actorOf(
    RoundRobinPool(4).props(Fetcher.props(token))
  )

  List("odersky", "zhenglaizhang", "junlaizhang", "rkuhn") foreach {
    login => router ! Fetch(login)
  }

  system.scheduler.scheduleOnce(2 seconds) {
    system terminate
  }
}
*/
