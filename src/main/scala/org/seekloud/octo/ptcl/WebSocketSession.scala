package org.seekloud.octo.ptcl

import akka.actor.typed.ActorRef
import org.seekloud.octo.core.EndPointWorker

/**
  * Created by sky
  * Date on 2019/8/16
  * Time at 15:16
  */
case class WebSocketSession(
                             id: String,
                             actor:ActorRef[EndPointWorker.Command],
                             session:ActorRef[BrowserMsg.WsMsg]
                           )
