package org.seekloud.octo.bridge

import org.seekloud.octo.ptcl.WebSocketSession

/**
  * Created by sky
  * Date on 2019/8/16
  * Time at 13:25
  */
object MediaHandler {
  case object MediaType{
    val VIDEO = "video"
    val AUDIO = "audio"
  }
}
class MediaHandler(session:WebSocketSession){

}