package org.seekloud.octo.ptcl

/**
  * Created by sky
  * Date on 2019/8/16
  * Time at 17:48
  */
object IceProtocol {
  case class CandidateInfo(
                            candidate: String,
                            sdpMid: String,
                            sdpMLineIndex: Int
                          )
}
