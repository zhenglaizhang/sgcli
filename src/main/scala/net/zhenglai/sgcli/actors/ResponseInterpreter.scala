package net.zhenglai.sgcli.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.json4s.JsonAST.JArray

import scala.util.{Failure, Success, Try}
import scalaj.http.HttpResponse
import org.json4s.native.JsonMethods._

/**
  * Created by Zhenglai on 7/13/16.
  */
object ResponseInterpreter {

  // messages
  case class InterpretResponse(
                                login: String,
                                response: Try[HttpResponse[String]]
                              )

  // props factory
  def props(followerExtractor: ActorRef) =
    Props(classOf[ResponseInterpreter], followerExtractor)
}

case class ResponseInterpreter(followerExtractor: ActorRef) extends Actor with ActorLogging {
  import ResponseInterpreter._

  private def responseToJson(body: String): Try[JArray] = {
    val jvalue = Try { parse(body)}
    jvalue flatMap {
      case a: JArray => Success(a)
      case _ => Failure(new IllegalStateException("Incorrect formatted JSON: not an array"))
        // @TODO re-add the failed login to the queue manager
    }
  }

  def interpret(login: String, response: Try[HttpResponse[String]]) = response match {
    case Success(r) => responseToJson(r.body) match {
      case Success(jsonResponse) =>
        followerExtractor ! FollowerExtractor.Extract(login, jsonResponse)
      case Failure(e) =>
        log.error(s"Error parsing response to JSON for $login: $e")
    }

    case Failure(e) => log.error(s"Error fetching URL for $login: $e")
  }

  override def receive: Receive = {
    case InterpretResponse(login, r) => interpret(login, r)
  }
}
