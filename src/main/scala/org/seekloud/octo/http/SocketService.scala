package org.seekloud.octo.http

import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import org.slf4j.LoggerFactory
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Flow

import scala.concurrent.Future
/**
  * Created by sky
  * Date on 2019/6/14
  * Time at 下午3:52
  * 本文件与前端建立socket连接
  */
trait SocketService extends ServiceUtils {
  private val log = LoggerFactory.getLogger(this.getClass)

  private def userJoin = path("userJoin") {
    parameter(
      'userId.as[String].?
    ) {userIdOpt =>
      complete("ok")
    }
  }

  val joinRoute: Route = userJoin
}
