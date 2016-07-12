package net.zhenglai.sgcli

import org.json4s.native.JsonMethods._
import org.json4s.{DefaultFormats, _}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io.Source
import scala.util.{Failure, Success}

/**
  * Created by Zhenglai on 7/12/16.
  */

case class User(id:Long, userName: String)

object GithubUser {
  implicit val formats = DefaultFormats


  def extractUser(jsonResp: JValue): User = {
    val transformed = jsonResp.transformField {
      case ("login", name) => ("userName", name)
    }
    transformed.extract[User]
  }

  def fetchFromUrl(url: String): Future[User] = {
    val resp = Future { Source.fromURL(url).mkString }
    val parsedResp = resp map { parse(_)}
    parsedResp map extractUser
  }
}

object TheApp {
  def main(args: Array[String]) {
//    val name = args.headOption.getOrElse {
//      throw new IllegalArgumentException(
//        "Missing command line argument for user"
//      )
//    }

    val names = args.toList
    val maps  = for {
      name <- names
      url = s"https://api.github.com/users/$name"
      user = GithubUser.fetchFromUrl(url)
    } yield name -> user

    maps foreach {
      case (name, user) =>
        user.onComplete {
          case Success(u) => println(s" ** Extracted for $name: $u")
          case Failure(e) => println(s" ** Error fetching $name: $e")
        }
    }

    Await.ready(Future.sequence(maps.map { _._2}), 1 minute)
  }
}
