package org.seekloud.octo

/**
  * Created by sky
  * Date on 2019/8/13
  * Time at 下午7:24
  */
package object ptcl {
  trait CommonRsp {
    val errCode: Int
    val msg: String
  }

  final case class ErrorRsp(
                             errCode: Int,
                             msg: String
                           ) extends CommonRsp

  final case class SuccessRsp(
                               errCode: Int = 0,
                               msg: String = "ok"
                             ) extends CommonRsp

  final case class ComRsp(
                           errCode: Int = 0,
                           msg: String = "ok"
                         ) extends CommonRsp
}
