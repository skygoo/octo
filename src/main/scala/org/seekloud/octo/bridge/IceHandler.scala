package org.seekloud.octo.bridge

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.io.IOException

import javax.sdp.{MediaDescription, SdpException, SessionDescription}
import org.ice4j.{Transport, TransportAddress}
import org.ice4j.ice.{Agent, CandidatePair, CandidateType, Component, IceMediaStream, IceProcessingState, RemoteCandidate}
import org.ice4j.ice.harvest.StunCandidateHarvester
import org.seekloud.octo.ptcl.IceProtocol.CandidateInfo
import org.seekloud.octo.ptcl.{BrowserMsg, WebSocketSession}
import org.slf4j.LoggerFactory
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import collection.JavaConverters._

/**
  * Created by sky
  * Date on 2019/8/15
  * Time at 17:06
  * use for ice and save info
  */
object IceHandler {

  case class IceStreamInfo(
                            mid: String,
                            mIndex: Int
                          )

  val turnInfo = new StunCandidateHarvester(new TransportAddress("stun.stunprotocol.org", 3478, Transport.UDP))
}

class IceHandler(
                  val session: WebSocketSession
                ) {
  protected val log = LoggerFactory.getLogger(this.getClass)

  protected var logPrefix: String = session.id + " |"

  import IceHandler._

  private val iceAgent = new Agent
  private var iceAgentStateIsRunning: Boolean = IceProcessingState.RUNNING == iceAgent.getState
  private val iceMediaStreamMap: mutable.HashMap[IceStreamInfo, IceMediaStream] = mutable.HashMap.empty

  iceAgent.addCandidateHarvester(turnInfo)
  iceAgent.setControlling(false)
  iceAgent.addStateChangeListener(new PropertyChangeListener() {
    override def propertyChange(evt: PropertyChangeEvent): Unit = {
      val oldState = evt.getOldValue.asInstanceOf[IceProcessingState]
      val newState = evt.getNewValue.asInstanceOf[IceProcessingState]
      log.info(logPrefix + s"change state from ${oldState.toString} to ${newState.toString}")
      iceAgentStateIsRunning = IceProcessingState.RUNNING == newState
    }
  })

  protected def getIceState() = iceAgent.getState

  protected def close() = iceMediaStreamMap.foreach(stream => iceAgent.removeStream(stream._2))

  def getICEMediaStream(mediaType: String): Option[IceMediaStream] = iceMediaStreamMap.find(_._1.mid == mediaType).map(_._2)

  def addStream(mediaType: String, mid: Int, rtcpmux: Boolean): Unit = {
    val mediaStream = iceAgent.createMediaStream(mediaType + session.id)
    //    mediaStream.addPairChangeListener(new ICEManager#ICEHandler#PairChangeListener)
    iceMediaStreamMap.put(IceStreamInfo(mediaType, mid), mediaStream)
    //For each Stream create two components (RTP & RTCP)
    try {
      val rtp = iceAgent.createComponent(mediaStream, Transport.UDP, 10000, 10000, 11000)
      if (!rtcpmux) {
        val rtcp = iceAgent.createComponent(mediaStream, Transport.UDP, 10001, 10001, 11000)
      }
    } catch {
      case e@(_: IllegalArgumentException | _: IOException) =>
        // TODO Auto-generated catch block
        e.printStackTrace()
    }
  }

  private def getLocalCandidates = {
    val localCandidates = new ArrayBuffer[CandidateInfo]
    iceMediaStreamMap.foreach { stream =>
      stream._2.getComponents.forEach(cmp =>
        cmp.getLocalCandidates.forEach(lc =>
          localCandidates.append(CandidateInfo(lc.toString, stream._1.mid, stream._1.mIndex))
        )
      )
    }
    localCandidates.toList
  }

  private var remoteUfrag: String = null
  private var remotePassword: String = null

  def setupFragPasswd(remoteUfrag: String, remotePassword: String): Unit = {
    this.remoteUfrag = remoteUfrag
    this.remotePassword = remotePassword
  }


  def startConnectivityEstablishment(): Unit = {
    iceMediaStreamMap.foreach { s =>
      s._2.setRemoteUfrag(this.remoteUfrag)
      s._2.setRemotePassword(this.remotePassword)
    }
    try {
      getLocalCandidates.foreach(lc => session.session ! BrowserMsg.AddIceCandidate(lc))
      iceAgent.startConnectivityEstablishment()
    } catch {
      case e: IOException =>
        throw new RuntimeException(e)
    }
  }

  def processRemoteCandidate(candidateInfo: CandidateInfo): Unit = {
    var tokens: Array[String] = candidateInfo.candidate.split(":")
    if ("candidate".equalsIgnoreCase(tokens(0))) {
      //      val stream: IceMediaStream = iceMediaStreamMap.find(_._1.mIndex == candidateInfo.sdpMLineIndex).get._2
      iceMediaStreamMap.find(_._1.mIndex == candidateInfo.sdpMLineIndex) match {
        case Some(stream) =>
          println ("find--stream")
          tokens = tokens(1).split(" ")
          var i: Int = 0
          val foundation: String = tokens({
            i += 1
            i - 1
          }).trim
          val cmpId: Int = tokens({
            i += 1
            i - 1
          }).trim.toInt
          val parentComponent: Component = stream._2.getComponent(cmpId)
          if (parentComponent != null) {
            val transport: Transport = Transport.parse(tokens({
              i += 1
              i - 1
            }).trim.toLowerCase)
            val priority: Long = tokens({
              i += 1
              i - 1
            }).trim.toLong
            val hostaddress: String = tokens({
              i += 1
              i - 1
            }).trim
            val port: Int = tokens({
              i += 1
              i - 1
            }).trim.toInt
            val transportAddress: TransportAddress = new TransportAddress(hostaddress, port, transport)
            var `type`: CandidateType = null
            if ("typ".equalsIgnoreCase(tokens(i).trim)) `type` = CandidateType.parse(tokens({
              i += 1
              i
            }).trim.toLowerCase)
            if (tokens.length > i && "generation" == tokens(i)) {
              val generation: Int = tokens({
                i += 1
                i
              }).trim.toInt
              i += 1
            }
            var related: RemoteCandidate = null
            var rAddr: String = null
            if (tokens.length > i && "raddr".equalsIgnoreCase(tokens(i))) {
              rAddr = tokens({
                i += 1
                i
              }).trim
              i += 1
            }
            var rport: Int = -1
            if (tokens.length > i && "rport".equalsIgnoreCase(tokens(i))) {
              rport = tokens({
                i += 1
                i
              }).trim.toInt
              i += 1
            }
            if (rAddr != null) {
              val rAddress: TransportAddress = new TransportAddress(rAddr, rport, transport)
              related = new RemoteCandidate(rAddress, parentComponent, `type`, foundation, priority, null)
            }
            val rc: RemoteCandidate = new RemoteCandidate(transportAddress, parentComponent, `type`, foundation, priority, related)
            if (iceAgentStateIsRunning) {
              parentComponent.addUpdateRemoteCandidates(rc)
            } else {
              parentComponent.addRemoteCandidate(rc)
            }
          }
        case None =>
          println("none--stream")
      }
    } else throw new IllegalArgumentException("Does not start with candidate:")
  }

  def prepareAnswer(offerSdp: SessionDescription, answerSdp: SessionDescription): SessionDescription = {
    try {
      val localDescriptions = answerSdp.getMediaDescriptions(false).asScala
      println(localDescriptions.size)
      localDescriptions.map(_.asInstanceOf[MediaDescription]).foreach(md =>
        try {
          if ("audio" == md.getMedia.getMediaType) {
            //md.setAttribute("mid", audiomediaStream.getName());
          }
          else if ("video" == md.getMedia.getMediaType) {
            //md.setAttribute("mid", videomediaStream.getName());
          }
          md.setAttribute("ice-ufrag", iceAgent.getLocalUfrag)
          md.setAttribute("ice-pwd", iceAgent.getLocalPassword)
        } catch {
          case e: SdpException =>
            throw new RuntimeException(e)
        }
      )
    } catch {
      case e: SdpException =>
        throw new RuntimeException(e)
    }
    answerSdp
  }
}
