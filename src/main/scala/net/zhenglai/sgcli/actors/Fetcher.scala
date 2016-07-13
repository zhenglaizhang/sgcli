package net.zhenglai.sgcli.actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import net.zhenglai.sgcli.util.Credentials

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaj.http.Http

/**
  * Created by Zhenglai on 7/13/16.
  */
object Fetcher {

  case class Fetch(login: String)

  def props(token: Option[String]): Props = Props(classOf[Fetcher], token)

  def props(): Props = Props(classOf[Fetcher], None)

}

case class Fetcher(val token: Option[String]) extends Actor with ActorLogging {

  import Fetcher._

  override def receive: Receive = {
    case Fetch(login) => fetchUrl(login)
  }

  private def fetchUrl(login: String): Unit = {
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
      log.info(s"Response from $login: $r")
    }
  }
}

object FetcherTest extends App {

  // import messages
  import Fetcher._

  val system = ActorSystem("FetcherTest")

  val token = Credentials.get("GHTOKEN")

  val fetchers = (0 to 3) map { i =>
    system.actorOf(Fetcher.props(token))
  }

  fetchers(0) ! Fetch("odersky")
  fetchers(0) ! Fetch("zhenglaizhang")
  fetchers(0) ! Fetch("scala")
  fetchers(0) ! Fetch("rkuhn")

  Thread.sleep(5000)
  system.terminate()
}
