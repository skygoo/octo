package org.seekloud.octo.bridge

import java.io.IOException

import javax.sdp.{MediaDescription, SdpException, SdpParseException, SessionDescription}
import org.ice4j.ice.{CandidatePair, Component, IceMediaStream, IceProcessingState}
import org.seekloud.octo.bridge.dtls.mock.Connect
import org.seekloud.octo.ptcl.WebSocketSession
import scala.collection.mutable
import collection.JavaConverters._

/**
  * Created by sky
  * Date on 2019/8/16
  * Time at 13:25
  * todo implement dtls protocol
  * todo exchange srtp password
  */
object DtlsHandler {

  case object MediaType {
    val VIDEO = "video"
    val AUDIO = "audio"
  }

  private val handlers: mutable.HashMap[String, DtlsHandler] = mutable.HashMap.empty

  def addHandler(id:String,h:DtlsHandler) = handlers.put(id,h)

  def removeHandlers(id: String) = handlers.remove(id).foreach(_.close())
}

class DtlsHandler(
                   override val session: WebSocketSession
                 ) extends IceHandler(session) {
  protected var rtcpMux = false
  private val connect = new Connect()

  protected def getLocalFingerPrint: String = connect.getLocalFingerPrint

  protected def notifyRemoteFingerprint(mediaType: String, remoteFingerprint: String): Unit = {
    log.info(s"$logPrefix remoteFingerprint $remoteFingerprint")
  }

  override def prepareAnswer(offerSdp: SessionDescription, answerSdp: SessionDescription): SessionDescription = {
    super.prepareAnswer(offerSdp, answerSdp)
    try {
      answerSdp.setAttribute("fingerprint", getLocalFingerPrint)
      val globalFingerPrint = offerSdp.getAttribute("fingerprint")
      if (globalFingerPrint != null) notifyRemoteFingerprint(null, globalFingerPrint)
      else {
        val mids = new mutable.HashMap[String, mutable.HashMap[String, String]]
        offerSdp.getMediaDescriptions(false).asScala.map(_.asInstanceOf[MediaDescription]).foreach(md => {
          try {
            mids.put(md.getMedia.getMediaType, new mutable.HashMap[String, String])
            mids(md.getMedia.getMediaType).put("mid", md.getAttribute("mid"))
            mids(md.getMedia.getMediaType).put("msid", md.getAttribute("msid"))
            mids(md.getMedia.getMediaType).put("ssrc", md.getAttribute("ssrc"))
            val fingerPrint = md.getAttribute("fingerprint")
            notifyRemoteFingerprint(md.getMedia.getMediaType, fingerPrint)
          } catch {
            case e: SdpParseException =>
              throw new RuntimeException(e)
          }
        })
        answerSdp.getMediaDescriptions(false).asScala.map(_.asInstanceOf[MediaDescription]).foreach(md => {
          try {
            md.setAttribute("mid", mids(md.getMedia.getMediaType)("mid"))
            md.setAttribute("msid", mids(md.getMedia.getMediaType)("msid"))
            md.setAttribute("ssrc", mids(md.getMedia.getMediaType)("ssrc"))
            if ("audio".equalsIgnoreCase(md.getMedia.getMediaType)) md.setAttribute("fingerprint", getLocalFingerPrint)
            else if ("video".equalsIgnoreCase(md.getMedia.getMediaType)) answerSdp.setAttribute("fingerprint", getLocalFingerPrint)
          } catch {
            case e: SdpException =>
              throw new RuntimeException(e)
          }
        })
      }
    } catch {
      case e: SdpException =>
        throw new RuntimeException(e)
    }
    answerSdp
  }

  @throws[IOException]
  protected def doOpenMediaStream(mediaType: String, rtpPair: CandidatePair, rtcpPair: CandidatePair, rtcpmux: Boolean) =
    if (rtpPair != null) { //client.connect(rtpPair.getDatagramSocket());
      println("fsfashfiashf",rtcpPair.getDatagramSocket)
      connect.connect(rtpPair.getDatagramSocket)
    }

  def openStream(mediaType: String): Unit = {
    //fixme 此处需要修改为iceMediaStream.getComponent(Component.RTP).getSocket
    try {
      val iceMediaStreamOpt = getICEMediaStream(mediaType)
      iceMediaStreamOpt match {
        case Some(iceMediaStream) =>
          val rtp = iceMediaStream.getComponent(Component.RTP)
          val rtpPair = rtp.getSelectedPair
          var rtcpPair: CandidatePair = null
          if (!rtcpMux) {
            val rtcp = iceMediaStream.getComponent(Component.RTP)
            rtcpPair = rtcp.getSelectedPair
          }
          doOpenMediaStream(mediaType, rtpPair, rtcpPair, rtcpMux)
        case None=>
          println("not init stream")
      }
    } catch {
      case e@(_: InterruptedException | _: IOException) =>
        throw new RuntimeException(e)
    }
  }
}