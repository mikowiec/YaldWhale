package collins.models

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema

import play.api.libs.json.Json

import collins.callbacks.CallbackDatum
import collins.models.cache.Cache
import collins.models.conversions.StateFormat
import collins.models.shared.AnormAdapter
import collins.models.shared.ValidatedEntity
import collins.validation.Pattern.isAlphaNumericString
import collins.validation.Pattern.isNonEmptyString

object State extends Schema with AnormAdapter[State] with StateKeys {

  /** Status value that can apply to any asset, regardless of status **/
  val ANY_STATUS = 0
  val ANY_NAME = "Any"

  def New = findByName("NEW")
  def Running = findByName("RUNNING")
  def Starting = findByName("STARTING")
  def Terminated = findByName("TERMINATED")

  object StateOrdering extends Ordering[State] {
    override def compare(a: State, b: State) = a.getDisplayLabel compare b.getDisplayLabel
  }

  override val tableDef = table[State]("state")
  on(tableDef)(s => declare(
    s.id is (autoIncremented, primaryKey),
    s.name is (unique),
    s.status is (indexed)))

  override def delete(state: State): Int = inTransaction {
    afterDeleteCallback(state) {
      tableDef.deleteWhere(s => s.id === state.id)
    }
  }

  def empty = new State(0, ANY_STATUS, "INVALID", "Invalid Label", "Invalid Description")

  def find(): List[State] = Cache.get(findKey, inTransaction {
    from(tableDef)(s => select(s)).toList
  })

  def findById(id: Int): Option[State] = Cache.get(findByIdKey(id), inTransaction {
    tableDef.lookup(id)
  })

  def findByName(name: String): Option[State] = Cache.get(findByNameKey(name), inTransaction {
    tableDef.where(s =>
      s.name.toLowerCase === name.toLowerCase).headOption
  })

  def findByAnyStatus(): List[State] = Cache.get(findByAnyStatusKey, inTransaction {
    tableDef.where(s =>
      s.status === ANY_STATUS).toList
  })

  def findByStatus(status: Status): List[State] = Cache.get(findByStatusKey(status.id), inTransaction {
    tableDef.where(s =>
      s.status === status.id).toList
  })

  override def get(state: State): State = findById(state.id).get

  def isSystemState(state: State): Boolean = state.id < 7
}

case class State(
    id: Int, // unique PK
    status: Int = 0, // FK to status, or 0 to apply to any status
    name: String, // Name, should be tag like (alpha numeric and _-)
    label: String, // A visual (short) label to accompany the state
    description: String // A longer description of the state
    ) extends ValidatedEntity[Int] with CallbackDatum {
  def getId(): Int = id
  def getDisplayLabel(): String = "%s - %s".format(getStatusName, label)
  def getStatusName(): String = status match {
    case 0 => State.ANY_NAME
    case n => Status.findById(n).map(_.name).getOrElse("Unknown")
  }
  override def validate() {
    require(status >= 0, "status must be >= 0")
    require(isAlphaNumericString(name), "State name must be alphanumeric")
    require(name.length > 1 && name.length <= 32, "length of name must between 1 and 32")
    require(name == name.toUpperCase, "name must be uppercase")
    require(isNonEmptyString(label), "label must be specified")
    require(label.length > 1 && label.length <= 32, "length of label must be between 1 and 32 characters")
    require(isNonEmptyString(description), "Description must be specified")
    require(description.length > 1 && description.length <= 255, "length of description must be between 1 and 255 characters")
  }
  override def asJson: String = Json.toJson(this).toString

  override def compare(z: Any): Boolean = {
    if (z == null)
      return false
    val ar = z.asInstanceOf[AnyRef]
    if (!ar.getClass.isAssignableFrom(this.getClass))
      false
    else {
      val other = ar.asInstanceOf[State]
      this.status == other.status && this.name == other.name &&
        this.label == other.label && this.description == other.description
    }
  }
}
