package collins.models.lldp

import play.api.libs.json.Format
import play.api.libs.json.JsObject
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Json

object PortId {
  implicit object PortIdFormat extends Format[PortId] {
    override def reads(json: JsValue) = JsSuccess(PortId(
      (json \ "TYPE").as[String],
      (json \ "VALUE").as[String]))
    override def writes(pid: PortId) = JsObject(Seq(
      "TYPE" -> Json.toJson(pid.idType),
      "VALUE" -> Json.toJson(pid.value)))
  }
}
case class PortId(idType: String, value: String) extends LldpAttribute {
  import PortId._
  override def toJsValue() = Json.toJson(this)
}
