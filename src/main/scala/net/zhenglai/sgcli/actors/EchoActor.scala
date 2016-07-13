package net.zhenglai.sgcli.actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import net.zhenglai.sgcli.actors.EchoActor.{EchoMessage, EchoHello}

/**
  * Created by Zhenglai on 7/12/16.
  */

object EchoActor {

  case object EchoHello

  case class EchoMessage(message: String)

  def props: Props = Props[EchoActor]
}

class StatefulActor(words: String) extends Actor with ActorLogging {
  override def receive = {
    case EchoHello => log.info(words)
    case EchoMessage(s) =>
      // delegate to the new child actor of current actor's context
      val echo4 = context.actorOf(EchoActor.props, name = "echo4$in$stateful$actor")
      echo4 ! EchoMessage(s)
  }
}

class EchoActor extends Actor with ActorLogging {
  import EchoActor._        // import message definitions
  override def receive = {
    case EchoHello => log.info("hello")
    case EchoMessage(s) => log.info(s)
  }
}

object HelloAkka extends App {
  val system = ActorSystem("HelloActors")

  val echo1 = system.actorOf(EchoActor.props, name = "echo1")
  val echo2 = system.actorOf(EchoActor.props, name = "echo2")

  val props = Props(classOf[StatefulActor], "I am an actor with state")
  val echo3 = system.actorOf(props, name="echo3")

  echo1 ! EchoActor.EchoHello
  echo2 ! EchoActor.EchoMessage("Hello Akka")
  echo3 ! EchoHello
  echo3 ! EchoMessage("Hello Actor3")


  Thread.sleep(1000)
  //  system.shutdown()
  system.terminate()
}
