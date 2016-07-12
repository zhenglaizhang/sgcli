package net.zhenglai.sgcl

import org.json4s.DefaultFormats
import org.json4s._
import org.json4s.native.JsonMethods._
import scala.io.Source

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

  def fetchFromUrl(url: String): User = {
    val resp = Source.fromURL(url).mkString
    val jsonResp = parse(resp)
    extractUser(jsonResp)
  }
}

object TheApp {
  def main(args: Array[String]) {
    val name = args.headOption.getOrElse {
      throw new IllegalArgumentException(
        "Missing command line argument for user"
      )
    }

    val user = GithubUser.fetchFromUrl(s"https://api.github.com/users/$name")
    println(s"*** Extracted for $name:")
    println()
    println(user)
  }
}
