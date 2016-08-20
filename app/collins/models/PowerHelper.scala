package collins.models

import scala.collection.immutable.SortedSet

import collins.models.shared.CommonHelper
import collins.util.power.PowerComponent
import collins.util.power.PowerComponentValue
import collins.util.power.PowerConfiguration
import collins.util.power.PowerUnit
import collins.util.power.PowerUnits
import collins.util.power.PowerUnits

object PowerHelper extends CommonHelper[PowerUnits] {
  def Config = PowerConfiguration.get()
  def Units = PowerUnits(Config)
  def Components: Set[Symbol] = Config.components
  def SymbolMap: Map[Long, Symbol] =
    (for (unit <- Units; component <- unit) yield (component.meta.id, component.componentType)).toMap
  def ComponentPriorities: Map[Symbol, Int] = Config.components.zipWithIndex.toMap

  def construct(asset: Asset, units: PowerUnits): Seq[AssetMetaValue] = {
    throw new UnsupportedOperationException("construct not used for power")
  }

  val managedTags = Set[AssetMeta.Enum]()

  case class Intermediary(units: Seq[PowerUnit] = Seq(), remaining: Seq[MetaWrapper] = Seq())
  def reconstruct(asset: Asset, allMeta: Seq[MetaWrapper]): Reconstruction = {
    val metaMap = allMeta.groupBy(_.getGroupId)
    val Intermediary(units, seq) = metaMap.foldLeft(Intermediary()) {
      case (intermediary, map) =>
        val (groupId, wrapSeq) = map
        val Intermediary(unitseq, allremaining) = intermediary
        val (pc, remaining) = reconstructComponents(groupId, wrapSeq)
        val unit = reconstructUnit(groupId, pc)
        if (unit.isDefined)
          Intermediary(unitseq ++ Seq(unit.get), allremaining ++ remaining)
        else
          Intermediary(unitseq, allremaining ++ remaining)
    }
    (PowerUnits(units), seq)
  }

  def reconstructUnit(groupId: Int, seq: Seq[PowerComponent]): Option[PowerUnit] = {
    if (seq.size == 0)
      None
    else
      Some(PowerUnit(Config, groupId, SortedSet(seq: _*)))
  }

  def reconstructComponents(groupId: Int, meta: Seq[MetaWrapper]): (Seq[PowerComponent], Seq[MetaWrapper]) = {
    val (found, notFound) = meta.partition(m => SymbolMap.contains(m.getMetaId))
    val components = found.map { mv =>
      val symbol = SymbolMap(mv.getMetaId)
      val priority = ComponentPriorities.get(symbol).getOrElse(0)
      PowerComponentValue(symbol, Config, groupId, priority, Some(mv.getValue))
    }
    (components, notFound)
  }

}
