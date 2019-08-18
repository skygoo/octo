package org.seekloud.octo.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import javax.sdp.{MediaDescription, SdpParseException, SessionDescription}
import org.opentelecoms.javax.sdp.NistSdpFactory
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
      log.debug(s"${epInfo.id}| is starting...")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        init4web(epInfo, s"${epInfo.id}|init|")
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
          wait4web(epInfo, f, s"${epInfo.id}|work|")

        case Stop =>
          log.info(s"$logPrefix stop")
          Behaviors.stopped

        case unKnow =>
          stashBuffer.stash(unKnow)
          Behavior.same
      }
    }

  import collection.JavaConverters._
  private def wait4web(epInfo: EpInfo, frontActor: ActorRef[BrowserMsg.WsMsg], logPrefix: String)(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case msg: WebSocketMsg =>
          msg.msg.foreach {
            case m:BrowserMsg.AnchorSdpOffer=>
              if (m.sdpOffer != null) try {
                val offerSdp = new NistSdpFactory().createSessionDescription(m.sdpOffer)
                val md = offerSdp.getMediaDescriptions(false).asScala
                println(md)
//                  .asInstanceOf[Buffer[MediaDescription]]
//                iceHandler.initStream(MediaType.VIDEO, true)
//                val remoteUfrag = md.getAttribute("ice-ufrag")
//                val remotePasswd = md.getAttribute("ice-pwd")
//                iceHandler.setupFragPasswd(remoteUfrag, remotePasswd)
//                val remoteCandidates = payMap.getCandidates
//                if (remoteCandidates != null && remoteCandidates.size > 0) iceHandler.processRemoteCandidates(remoteCandidates)
//                val answerDescription = IOUtils.toString(getClass.getResourceAsStream("/mozsdp_videoonly.answer"))
//                val answerSdp = new NistSdpFactory().createSessionDescription(answerDescription)
//                mediaHandler.prepareAnswer(offerSdp, answerSdp)
//                answerMsg.setSdp(answerSdp.toString)
//                session.sendMessage(new TextMessage(mapper.writeValueAsBytes(answerMsg)))
//                mediaHandler.openStream(MediaType.VIDEO)
              } catch {
                case e: SdpParseException =>
                  log.error(logPrefix+" "+e.getMessage)
                  log.error(logPrefix+" "+m.sdpOffer)
              }
            case m:BrowserMsg.AddIceCandidate=>

          }
          Behaviors.same

        case Stop =>
          log.info(s"$logPrefix stop")
          Behaviors.stopped
        case unKnow =>
          stashBuffer.stash(unKnow)
          Behavior.same
      }
    }

}
