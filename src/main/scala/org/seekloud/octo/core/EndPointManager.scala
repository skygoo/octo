package org.seekloud.octo.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.{ActorAttributes, Supervision}
import akka.stream.scaladsl.Flow
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import org.seekloud.octo.Boot.{executor, materializer}
import org.seekloud.octo.ptcl.{BrowserMsg, EpInfo}

/**
  * Created by sky
  * Date on 2019/8/15
  * Time at 16:09
  * 管理多路rtc
  */
object EndPointManager {
  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command

  final case class GetWebSocketFlow(replyTo: ActorRef[Flow[Message, Message, Any]], epInfo: EpInfo) extends Command

  def create(): Behavior[Command] = {
    log.info(s"LiveManager start...")
    Behaviors.setup[Command] {
      ctx =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            idle()
        }
    }
  }

  private def idle()
                  (
                    implicit timer: TimerScheduler[Command]
                  ): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case msg: GetWebSocketFlow =>
          val userActor = getLiveActor(ctx, msg.epInfo)
          msg.replyTo ! getWebSocketFlow(userActor)
          Behaviors.same

        case unKnow =>
          log.error(s"${ctx.self.path} receive a unknow msg when idle:$unKnow")
          Behaviors.same
      }
    }
  }

  private def getWebSocketFlow(userActor: ActorRef[EndPointWorker.Command]): Flow[Message, Message, Any] = {
    import scala.language.implicitConversions
    import io.circe.generic.auto._
    import io.circe.parser._
    import io.circe.syntax._

    implicit def parseS2Json(s:String):Option[BrowserMsg.WsJsonMsg] =
      try{
        val msg = decode[BrowserMsg.WsJsonMsg](s).right.get
        Some(msg)
      }catch {
        case e:Exception=>
          log.error(s"decode $s as WsJson error with ${e.getMessage}")
          None
      }

    Flow[Message]
      .collect {
        case TextMessage.Strict(m) =>
          Future.successful(EndPointWorker.WebSocketMsg(m))
        case TextMessage.Streamed(sm) =>
          sm.runFold(new StringBuilder().result()) {
            case (s, str) => s.++(str)
          }.map { s =>
            EndPointWorker.WebSocketMsg(s)
          }
      }.mapAsync(parallelism = 3)(identity) //同时处理Strict和Stream
      .via(EndPointWorker.flow(userActor))
      .map {
        case t: BrowserMsg.WsJsonMsg =>
          TextMessage.Strict(t.asJson.noSpaces)
        case x =>
          log.debug(s"akka stream receive unknown msg=$x")
          TextMessage.apply("")
      }.withAttributes(ActorAttributes.supervisionStrategy(decider))

  }

  private val decider: Supervision.Decider = {
    e: Throwable =>
      e.printStackTrace()
      log.error(s"WS stream failed with $e")
      Supervision.Resume
  }

  private def getLiveActor(ctx: ActorContext[Command],epInfo: EpInfo): ActorRef[EndPointWorker.Command] = {
    val childName = s"LiveActor-${epInfo.id}"
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(EndPointWorker.create(epInfo), childName)
      //      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.unsafeUpcast[EndPointWorker.Command]
  }
}
