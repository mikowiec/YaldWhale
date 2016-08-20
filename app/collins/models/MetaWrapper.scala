package collins.models

import collins.util.config.Feature
import collins.util.CryptoCodec

/**
 * Provide a convenience wrapper on top of a row of meta/value data
 */
case class MetaWrapper(_meta: AssetMeta, _value: AssetMetaValue) {
  def getAssetId(): Long = _value.assetId
  def getMetaId(): Long = _meta.id
  def getId(): (Long, Long) = (getAssetId(), getMetaId())
  def getName(): String = _meta.name
  def getGroupId(): Int = _value.groupId
  def getPriority(): Int = _meta.priority
  def getLabel(): String = _meta.label
  def getDescription(): String = _meta.description
  def getValueType(): AssetMeta.ValueType = _meta.getValueType()
  def getValue(): String = Feature.encryptedTags.contains(getName) match {
    case true  => CryptoCodec.withKeyFromFramework.Decode(_value.value).getOrElse(_value.value)
    case false => _value.value
  }
  override def toString(): String = getValue()
  def valueEquals(m: MetaWrapper) = getValue() == m.getValue()
}

object MetaWrapper {
  def apply(amv: AssetMetaValue): MetaWrapper = MetaWrapper(amv.meta, amv)
  def createMeta(asset: Asset, metas: Map[String, String], groupId: Option[Int] = None) = {
    val metaValues = metas.map {
      case (k, v) =>
        val meta = AssetMeta.findOrCreateFromName(k)
        groupId.map(AssetMetaValue(asset, meta.id, _, v))
          .getOrElse(AssetMetaValue(asset, meta.id, v))
    }.toSeq
    AssetMetaValue.purge(metaValues, groupId)
    val values = metaValues.filter(v => v.value != null && v.value.nonEmpty)
    values.size match {
      case 0 =>
      case n =>
        AssetMetaValue.create(values)
    }
  }

}
