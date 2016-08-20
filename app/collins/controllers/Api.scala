package collins.controllers

import java.util.Date

import play.api.libs.EventSource
import play.api.libs.EventSource.EventNameExtractor
import play.api.libs.json.JsArray
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results

import collins.controllers.actors.TestProcessor
import collins.firehose.Firehose
import collins.firehose.FirehoseConfig
import collins.models.Asset
import collins.util.BashOutput
import collins.util.HtmlOutput
import collins.util.JsonOutput
import collins.util.OutputType
import collins.util.TextOutput
import collins.util.concurrent.BackgroundProcessor
import collins.util.views.Formatter

private[controllers] case class ResponseData(status: Results.Status, data: JsValue, headers: Seq[(String, String)] = Nil, attachment: Option[AnyRef] = None) {
  def asResult(implicit req: Request[AnyContent]): Result =
    ApiResponse.formatResponseData(this)(req)
}

trait Api extends ApiResponse with AssetApi with AssetTypeApi with AssetManagementApi with AssetWebApi with AssetLogApi with IpmiApi with TagApi with IpAddressApi with AssetStateApi with AdminApi {
  this: SecureController =>

  lazy protected implicit val securitySpec = Permissions.LoggedIn

  def timestamp = Action { implicit req =>
    val time: Long = System.currentTimeMillis / 1000
    OutputType(req).getOrElse(TextOutput()) match {
      case o: TextOutput =>
        Results.Ok(time.toString).as(o.contentType)
      case o: BashOutput =>
        Results.Ok("TIMESTAMP=%s".format(time.toString)).as(o.contentType)
      case o: JsonOutput =>
        val json = JsObject(Seq("timestamp" -> JsNumber(time)))
        Results.Ok(Json.stringify(json)).as(o.contentType)
      case o: HtmlOutput =>
        Results.Ok(time.toString).as(o.contentType)
    }
  }

  def firehose = Action { implicit request =>
    if (FirehoseConfig.enabled) {
      authenticate(request) match {
        case None =>
          logger.debug("Authentication required and NOT successful for streams")
          Results.Forbidden
        case Some(user) => authorize(user, Permissions.Firehose.Stream) match {
          case false =>
            logger.debug("Authorization required and NOT successful for streams")
            Results.Forbidden
          case true =>
            // specify an name extractor, all events are dispatched with a name
            implicit val eventNameExtractor = EventNameExtractor[JsValue](_.\("name").asOpt[String])
            Results.Ok.feed(Firehose.out &> EventSource()).as("text/event-stream")
        }
      }
    } else {
      Results.ServiceUnavailable
    }
  }

  def ping = Action { implicit req =>
    formatResponseData(ResponseData(Results.Ok, JsObject(Seq(
      "Data" -> JsObject(Seq(
        "Timestamp" -> JsString(Formatter.dateFormat(new Date())),
        "TestObj" -> JsObject(Seq(
          "TestString" -> JsString("test"),
          "TestList" -> JsArray(List(JsNumber(1), JsNumber(2))))),
        "TestList" -> JsArray(List(
          JsObject(Seq("id" -> JsNumber(123), "name" -> JsString("foo123"), "key-with-dash" -> JsString("val-with-dash"))),
          JsObject(Seq("id" -> JsNumber(124), "name" -> JsString("foo124"))),
          JsObject(Seq("id" -> JsNumber(124), "name" -> JsString("foo124"))))))),
      "Status" -> JsString("Ok")))))
  }

  def asyncPing(sleepMs: Long) = Action.async { implicit req =>
    BackgroundProcessor.send(TestProcessor(sleepMs)) { r =>
      r match {
        case Left(_)    => formatResponseData(Api.statusResponse(false))
        case Right(res) => formatResponseData(Api.statusResponse(res))
      }
    }
  }

  def errorPing(id: Int) = Action { implicit req =>
    req.queryString.map {
      case (k, v) =>
        k match {
          case "foo" =>
            formatResponseData(ResponseData(Results.Ok, JsObject(Seq("Result" -> JsBoolean(true)))))
        }
    }.head
  }
}

object Api {
  def withAssetFromTag[T](tag: String)(f: Asset => Either[ResponseData, T]): Either[ResponseData, T] = {
    Asset.isValidTag(tag) match {
      case false => Left(getErrorMessage("Invalid tag specified"))
      case true => Asset.findByTag(tag) match {
        case Some(asset) => f(asset)
        case None        => Left(getErrorMessage("Could not find specified asset", Results.NotFound))
      }
    }
  }

  def statusResponse(status: Boolean, code: Results.Status = Results.Ok) =
    ResponseData(code, JsObject(Seq("SUCCESS" -> JsBoolean(status))))

  def errorResponse(m: String, s: Results.Status = Results.BadRequest, e: Option[Throwable] = None) =
    getErrorMessage(m, s, e)

  def getErrorMessage(msg: String, status: Results.Status = Results.BadRequest, exception: Option[Throwable] = None) = {
    val json = ApiResponse.formatJsonError(msg, exception)
    ResponseData(status, json)
  }
}
