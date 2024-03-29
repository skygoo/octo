package org.seekloud.octo.ptcl

import org.seekloud.octo.ptcl.IceProtocol.CandidateInfo

/**
  * Created by sky
  * Date on 2019/8/16
  * Time at 16:56
  */
object BrowserMsg {

  object MsgId {
    val PING = "PING" //前端定时发送
    val PONG = "PONG" //后台回复
    val Anchor_SDP_OFFER = "Anchor_SDP_OFFER" //主播连入
    val Audience_SDP_OFFER = "Audience_SDP_OFFER" //connect 消息之后发送
    val PROCESS_SDP_ANSWER = "PROCESS_SDP_ANSWER" //自动处理
    val ADD_ICE_CANDIDATE = "ADD_ICE_CANDIDATE" //自动处理
    val CONNECT = "CONNECT" //建立连线
    val DISCONNECT = "DISCONNECT" //断连
  }

  trait WsMsg

  case object Complete extends WsMsg

  case class Fail(ex: Throwable) extends WsMsg

  sealed trait WsJsonMsg extends WsMsg

  case class Test(
                   test: String
                 ) extends WsJsonMsg

  case class AnchorSdpOffer(
                             sdpOffer: String
                           ) extends WsJsonMsg

  case class AddIceCandidate(
                              candidateInfo: CandidateInfo
                            ) extends WsJsonMsg

  case class ProcessSdpAnswer(
                             sdpAnswer: String
                             )extends WsJsonMsg
}
