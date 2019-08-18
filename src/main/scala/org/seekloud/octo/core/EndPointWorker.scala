package org.seekloud.octo.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import org.seekloud.octo.ptcl.{BrowserMsg, EpInfo}
import org.slf4j.LoggerFactory

/**
  * Created by sky
  * Date on 2019/8/15
  * Time at 16:09
  * 对接一路RtcPeerConnect
  */
object EndPointWorker {
  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command

  case class WebSocketMsg(msg: Option[BrowserMsg.WsJsonMsg]) extends Command

  case object CompleteMsgFront extends Command

  case class FailMsgFront(ex: Throwable) extends Command

  case class UserFrontActor(actor: ActorRef[BrowserMsg.WsMsg]) extends Command

  case class UserLeft[U](actorRef: ActorRef[U]) extends Command

  case object Stop extends Command

  private def sink(actor: ActorRef[Command]) = ActorSink.actorRef[Command](
    ref = actor,
    onCompleteMessage = CompleteMsgFront,
    onFailureMessage = FailMsgFront.apply
  )

  def flow(actor: ActorRef[Command]): Flow[WebSocketMsg, BrowserMsg.WsMsg, Any] = {
    val in = Flow[WebSocketMsg].to(sink(actor))
    val out =
      ActorSource.actorRef[BrowserMsg.WsMsg](
        completionMatcher = {
          case BrowserMsg.Complete =>
        },
        failureMatcher = {
          case BrowserMsg.Fail(e) => e
        },
        bufferSize = 128,
        overflowStrategy = OverflowStrategy.dropHead
      ).mapMaterializedValue(outActor => actor ! UserFrontActor(outActor))
    Flow.fromSinkAndSource(in, out)
  }

  def create(epInfo: EpInfo): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      log.debug(s"${ctx.self.path} is starting...")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        init4web(epInfo, s"${epInfo.id} |")
      }
    }
  }

  private def init4web(epInfo: EpInfo, logPrefix: String)(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case UserFrontActor(f) =>
          log.info(s"$logPrefix Ws connect success")
          wait4web(liveInfo, f)

        case Stop =>
          log.info(s"$logPrefix stop")
          Behaviors.stopped

        case unKnow =>
          stashBuffer.stash(unKnow)
          Behavior.same
      }
    }


}
