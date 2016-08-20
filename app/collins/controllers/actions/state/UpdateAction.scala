package collins.controllers.actions.state

import scala.concurrent.Future

import play.api.data.Form
import play.api.data.Forms.tuple
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import collins.controllers.validators.ParamValidation
import collins.controllers.SecureController
import collins.controllers.Api
import collins.controllers.actions.SecureAction
import collins.controllers.actions.RequestDataHolder
import collins.validation.StringUtil
import collins.models.State
import collins.models.{ Status => AssetStatus }
import collins.util.security.SecuritySpecification

/**
 * Update a state
 *
 * @apigroup AssetState
 * @apimethod POST
 * @apiurl /api/state/:name
 * @apiparam :name String Old name, reference
 * @apiparam name Option[String] new name, between 2 and 32 characters
 * @apiparam status Option[String] Status name to bind this state to, or Any to bind to all status
 * @apiparam label Option[String] A friendly display label between 2 and 32 characters
 * @apiparam description Option[String] A longer description of the state between 2 and 255 characters
 * @apirespond 200 success
 * @apirespond 400 invalid input
 * @apirespond 404 invalid state name
 * @apirespond 409 name already in use or trying to modify system name
 * @apirespond 500 error saving state
 * @apiperm controllers.AssetStateApi.updateState
 * @collinsshell {{{
 *  collins-shell state update OLDNAME [--name=NAME --label=LABEL --description='DESCRIPTION' --status=Status]
 * }}}
 * @curlexample {{{
 *  curl -v -u blake:admin:first --basic \
 *    -d name='NEW_NAME' \
 *    http://localhost:9000/api/state/OLD_NAME
 * }}}
 */
case class UpdateAction(
    name: String,
    spec: SecuritySpecification,
    handler: SecureController) extends SecureAction(spec, handler) with ParamValidation {

  import CreateAction.Messages._
  import DeleteAction.Messages.systemName

  case class ActionDataHolder(state: State) extends RequestDataHolder

  val stateForm = Form(tuple(
    "status" -> validatedOptionalText(2),
    "name" -> validatedOptionalText(2, 32),
    "label" -> validatedOptionalText(2, 32),
    "description" -> validatedOptionalText(2, 255)))

  override def validate(): Validation = stateForm.bindFromRequest()(request).fold(
    err => Left(RequestDataHolder.error400(fieldError(err))),
    form => {
      val (statusOpt, nameOpt, labelOpt, descriptionOpt) = form
      StringUtil.trim(name).filter(s => s.length > 1 && s.length <= 32).flatMap { s =>
        State.findByName(s)
      }.map { state =>
        val statusId = getStatusId(statusOpt)
        if (State.isSystemState(state)) {
          Left(RequestDataHolder.error409(systemName))
        } else if (statusOpt.isDefined && !statusId.isDefined) {
          Left(RequestDataHolder.error400(invalidStatus))
        } else {
          validateName(nameOpt)
            .right.map { validatedNameOpt =>
              val named = stateWithName(state, validatedNameOpt)
              val stated = stateWithStatus(named, statusId)
              val labeled = stateWithLabel(stated, labelOpt)
              ActionDataHolder(stateWithDescription(labeled, descriptionOpt))
            }
        }
      }.getOrElse {
        Left(RequestDataHolder.error404(invalidName))
      }
    })

  override def execute(rdh: RequestDataHolder) = Future {
    rdh match {
      case ActionDataHolder(state) => State.update(state) match {
        case ok if ok >= 0 => Api.statusResponse(true, Status.Ok)
        case notok         => Api.statusResponse(false, Status.InternalServerError)
      }
    }
  }

  protected def fieldError(f: Form[_]) = f match {
    case e if e.error("name").isDefined        => invalidName
    case e if e.error("label").isDefined       => invalidLabel
    case e if e.error("description").isDefined => invalidDescription
    case e if e.error("status").isDefined      => invalidStatus
    case n                                     => fuck
  }

  protected def stateWithName(state: State, name: Option[String]): State =
    name.map(s => state.copy(name = s)).getOrElse(state)
  protected def stateWithStatus(state: State, status: Option[Int]): State =
    status.map(id => state.copy(status = id)).getOrElse(state)
  protected def stateWithLabel(state: State, label: Option[String]): State =
    label.map(l => state.copy(label = l)).getOrElse(state)
  protected def stateWithDescription(state: State, desc: Option[String]): State =
    desc.map(d => state.copy(description = d)).getOrElse(state)

  protected def validateName(nameOpt: Option[String]): Either[RequestDataHolder, Option[String]] = {
    val validatedName: Either[String, Option[String]] = nameOpt match {
      case None =>
        Right(None)
      case Some(n) =>
        StringUtil.trim(n).filter(s => s.length > 1 && s.length <= 32) match {
          case None    => Left(invalidName)
          case Some(s) => Right(Some(s))
        }
    }
    validatedName match {
      case Left(err) =>
        Left(RequestDataHolder.error400(err))
      case Right(None) => Right(None)
      case Right(Some(s)) => State.findByName(s) match {
        case None    => Right(Some(s))
        case Some(_) => Left(RequestDataHolder.error409(invalidName))
      }
    }
  }

  protected def getStatusId(status: Option[String]): Option[Int] = status.flatMap { s =>
    (s.toUpperCase == State.ANY_NAME.toUpperCase) match {
      case true  => Some(State.ANY_STATUS)
      case false => AssetStatus.findByName(s).map(_.id)
    }
  }

}
