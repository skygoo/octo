package org.seekloud.octo.bridge.trans

import org.ice4j.ice._
import org.slf4j.LoggerFactory

/**
  * Created by sky
  * Date on 2019/8/13
  * Time at 下午7:45
  */
class IceTransport {
  private val log = LoggerFactory.getLogger(this.getClass)

  var agent: Agent = _

  private val localSdp = null

  private val remoteSdp = null

  private val turnServers = Array[String]("stun.jitsi.net:3478")

  private val stunServers = Array[String]("stun.stunprotocol.org:3478")


}
