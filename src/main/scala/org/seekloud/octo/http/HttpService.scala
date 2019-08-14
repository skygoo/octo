package org.seekloud.octo.http

import akka.actor.{ActorSystem, Scheduler}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.util.Timeout
import akka.actor.typed.scaladsl.AskPattern._

import scala.concurrent.Future
import scala.concurrent.ExecutionContextExecutor
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._

/**
  * User: Taoz
  * Date: 8/26/2016
  * Time: 10:27 PM
  */
trait HttpService extends ResourceService
  with ServiceUtils
  with SocketService {

  implicit val system: ActorSystem

  implicit val executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val timeout: Timeout

  implicit val scheduler: Scheduler

  lazy val httpRoutes: Route = ignoreTrailingSlash {
    pathPrefix("octo") {
      pathPrefix("index") {
        pathEndOrSingleSlash {
          getFromResource("html/index.html")
        }
      } ~ resourceRoutes
    }
  }

  lazy val httpsRoutes: Route =
    ignoreTrailingSlash {
      pathPrefix("octo") {
        pathPrefix("index") {
          pathEndOrSingleSlash {
            getFromResource("html/test.html")
          }
        } ~ resourceRoutes ~ joinRoute
      }
    }


}
