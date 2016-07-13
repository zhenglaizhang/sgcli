package net.zhenglai.sgcli.actors

import java.io.{File, FileWriter, BufferedWriter}

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor._
import net.zhenglai.sgcli.actors.FetcherManger.{GiveMeWork, AddToQueue}

import scala.io.Source
import scala.util.Try

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

  // properties, @TODO store in conf file
  val fetchedUsersFileName = "fetched-users.txt"
  val fetchQueueFileName = "fetch-queue.txt"
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

  override val supervisorStrategy = AllForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Restart
    case _: DeathPactException => Restart
    case _: Exception => Restart
  }

  def loadFetchedUsers = {
    val fetchedUsersSource = Try {
      Source.fromFile(FetcherManger.fetchedUsersFileName)
    }

    fetchedUsersSource foreach { s =>
      try s.getLines foreach { l => fetchedUsers += l }
      finally s.close
    }
  }

  def loadFetchQueue = Try {
    Try {
      Source.fromFile(FetcherManger.fetchQueueFileName)
    } foreach { s =>
      try s.getLines.foreach { l => fetchQueue += l }
      finally s.close
    }
  }

  // load saved state
  override def preStart: Unit = {
    log.info("Running preStart on fetcher manager")
    loadFetchedUsers
    log.info(s"Read ${fetchedUsers.size} visited users from source")

    loadFetchQueue
    log.info(s"Read ${fetchQueue.size} users in queue from source")

    if (fetchQueue.nonEmpty) {
      context.become(receiveWhileNotEmpty)
      fetchers foreach {
        _ ! Fetcher.WorkAvailabie
      }
    }
  }

  def saveFetchedUsers = {
    val writer = new BufferedWriter(
      new FileWriter(new File(FetcherManger.fetchedUsersFileName))
    )
    fetchedUsers foreach { user => writer.write(user + "\n") }
    writer.close
  }

  def saveFetchQueue = {
    val writer = new BufferedWriter(new FileWriter(new File(FetcherManger.fetchQueueFileName)))
    fetchQueue foreach { user => writer.write(user + "\n") }
    writer.close
  }

  // dump current state
  override def postStop: Unit = {
    log.info("Running postStop on fetcher manager")
    saveFetchedUsers
    saveFetchQueue
  }

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
