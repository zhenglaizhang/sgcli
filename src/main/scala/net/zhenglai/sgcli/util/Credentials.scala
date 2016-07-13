package net.zhenglai.sgcli.util

/**
  * Created by Zhenglai on 7/13/16.
  */
object Credentials {

  def get(key: String): Option[String] = {
//    Option(sys.env.getOrElse(key, throw new IllegalStateException(
//      s"Need a $key variable in the environment"
//    )))
    return sys.env.get(key)
  }
}
