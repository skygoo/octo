package org.seekloud.octo.bridge

import org.ice4j.{Transport, TransportAddress}
import org.ice4j.ice.{Agent, IceMediaStream}
import org.ice4j.ice.harvest.TurnCandidateHarvester

import scala.collection.mutable.HashMap

/**
  * Created by sky
  * Date on 2019/8/15
  * Time at 17:06
  * use for ice and save info
  */
object IceHandler {
  val turnInfo = new TurnCandidateHarvester(new TransportAddress("123.56.108.66", 41640, Transport.UDP))
}

class IceHandler {
  import IceHandler._
  private val iceAgent = new Agent
//  private val iceMediaStreamMap:HashMap[MediaType, IceMediaStream]=HashMap.empty

  def this(){
    this()
    iceAgent.addCandidateHarvester(turnInfo)
    iceAgent.setControlling(false)
  }

}
