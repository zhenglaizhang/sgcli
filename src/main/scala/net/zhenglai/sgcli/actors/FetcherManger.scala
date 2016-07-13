package net.zhenglai.sgcli.actors

import akka.actor.{Props, Actor, ActorLogging}
import net.zhenglai.sgcli.actors.FetcherManger.{GiveMeWork, AddToQueue}

/**
  * Created by Zhenglai on 7/13/16.
  */
object FetcherManger {

  // message
  case object GiveMeWork

  case class AddToQueue(login: String)

  // props factory
  def props(token: Option[String], numFetchers: Int) =
    Props(classOf[FetcherManger], token, numFetchers)
}


class FetcherManger(val token: Option[String],
                    val numFetchers: Int) extends Actor with ActorLogging {

  // usernames whose followers to be fetched
  val fetchQueue = scala.collection.mutable.Queue.empty[String]

  // set of usernames we have fetched
  val fetchedUsers = scala.collection.mutable.Set.empty[String]


  // init woker actors
  val followerExtractor = context.actorOf(FollowerExtractor.props(self))
  val responseInterpreter = context.actorOf(ResponseInterpreter.props(followerExtractor))
  val fetchers = (0 until numFetchers) map { i =>
    context.actorOf(Fetcher.props(token, self, responseInterpreter)) // self as FetcherManager
  }

  // start with empty queue
  override def receive = receiveWhileEmpty


  def receiveWhileEmpty: Receive = {
    case AddToQueue(login) =>
      queueIfNotFetched(login)
      context.become(receiveWhileNotEmpty)
      fetchers foreach {
        _ ! Fetcher.WorkAvailabie
      } // send work available to every fetchers
    case GiveMeWork => // do nothing
  }

  def receiveWhileNotEmpty: Receive = {
    case AddToQueue(login) => queueIfNotFetched(login)
    case GiveMeWork =>
      val login = fetchQueue.dequeue
      sender ! Fetcher.Fetch(login)
      if (fetchQueue.isEmpty) {
        context.become(receiveWhileEmpty)
      }
  }


  def queueIfNotFetched(login: String) = {
    if (!fetchedUsers(login)) {
      log.info(s"Publishing $login onto queue")
      fetchQueue += login
      fetchedUsers += login
    }
  }
}
