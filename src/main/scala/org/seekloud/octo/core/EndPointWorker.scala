package org.seekloud.octo.core

import java.io.FileInputStream

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import javax.sdp.{MediaDescription, SdpParseException, SessionDescription}
import org.opentelecoms.javax.sdp.NistSdpFactory
import org.seekloud.octo.bridge.DtlsHandler
import org.seekloud.octo.bridge.DtlsHandler.MediaType
import org.seekloud.octo.ptcl.{BrowserMsg, EpInfo, WebSocketSession}
import org.seekloud.octo.utils.FileUtil
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
      log.info(s"${epInfo.id}| is starting...")
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
          val h = new DtlsHandler(WebSocketSession(epInfo.id, ctx.self, f))
          DtlsHandler.addHandler(epInfo.id, h)
          wait4web(epInfo, h, s"${epInfo.id}|work|")

        case Stop =>
          log.info(s"$logPrefix stop")
          Behaviors.stopped

        case unKnow =>
          stashBuffer.stash(unKnow)
          Behavior.same
      }
    }

  import collection.JavaConverters._

  val answerDescription="v=0\no=mozilla...THIS_IS_SDPARTA-43.0.4 5452113973299446729 0 IN IP4 0.0.0.0\ns=-\nt=0 0\na=fingerprint:sha-256 90:7D:7B:AF:23:9B:12:24:8E:62:F2:E4:02:63:EB:3B:6C:D9:89:13:E4:5B:A2:60:44:2D:C2:59:4B:65:07:DC\na=ice-options:trickle\na=msid-semantic:WMS *\nm=video 9 UDP/TLS/RTP/SAVPF 100\nc=IN IP4 0.0.0.0\na=sendrecv\na=fmtp:100 max-f\ns=12288;max-f\na=ice-pwd:7e340e733da38a1c9f055dd5961717f0\na=ice-ufrag:8fda67d3\na=mid:sdparta_0\na=rtcp-mux\na=rtcp-fb:100 nack\na=rtcp-fb:100 nack pli\na=rtcp-fb:100 ccm fir\na=rtpmap:100 VP8/90000\na=setup:active"

  private def wait4web(epInfo: EpInfo, handler: DtlsHandler, logPrefix: String)(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case msg: WebSocketMsg =>
          msg.msg.foreach {
            case m: BrowserMsg.AnchorSdpOffer =>
              try {
                val offerSdp = new NistSdpFactory().createSessionDescription(m.sdpOffer)
                val md = offerSdp.getMediaDescriptions(false).asScala.map(_.asInstanceOf[MediaDescription]).filter(_.getMedia.getMediaType == MediaType.VIDEO)
                md.foreach { r =>
                  val mediaType = r.getMedia.getMediaType
                  handler.addStream(mediaType, r.getAttribute("mid").toInt, true)
                  val remoteUfrag = r.getAttribute("ice-ufrag")
                  val remotePasswd = r.getAttribute("ice-pwd")
                  handler.setupFragPasswd(remoteUfrag, remotePasswd)
                  handler.startConnectivityEstablishment()
                  val answerSdp = new NistSdpFactory().createSessionDescription(answerDescription)
                  handler.prepareAnswer(offerSdp, answerSdp)
                  handler.session.session ! BrowserMsg.ProcessSdpAnswer(answerSdp.toString)
                  handler.openStream(mediaType)
                }
              } catch {
                case e: SdpParseException =>
                  log.error(logPrefix + " " + e.getMessage)
                  log.error(logPrefix + " " + m.sdpOffer)
              }
            case m: BrowserMsg.AddIceCandidate =>
              handler.processRemoteCandidate(m.candidateInfo)
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
