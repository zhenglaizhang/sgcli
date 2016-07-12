package net.zhenglai.sgcli

import org.json4s.native.JsonMethods._
import org.json4s.{DefaultFormats, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import scalaj.http.Http

/**
  * Created by Zhenglai on 7/12/16.
  */

case class User(id:Long, userName: String)

object GithubUser {
  implicit val formats = DefaultFormats

  lazy val token:Option[String] = sys.env.get("GHTOKEN") orElse {
    println("No token found: trying to continue without authentication")
    None
  }

  def extractUser(jsonResp: JValue): User = {
    val transformed = jsonResp.transformField {
      case ("login", name) => ("userName", name)
    }
    transformed.extract[User]
  }

  def fetchFromUrl(url: String): Future[User] = {
    val baseRequest = Http(url)
    // TODO parse status code to give better error message if possible
    val request = token match {
      case Some(t) => baseRequest.header("Authorization", s"token $t")
      case None => baseRequest
    }
    val resp = Future { request.asString.body }
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
