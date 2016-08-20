package collins.models

import collins.util._
import collins.util.parsers.LshwParser
import org.specs2._
import specification._
import play.api.test.WithApplication

class LshwHelperSpec extends mutable.Specification {

  "LSHW Helper Specification".title

  args(sequential = true)

  "The LSHW Helper" should {
    "Parse and reconstruct data" in {
      "containing a 10-gig card" in new LshwCommonHelper("lshw-10g.xml") {
        val lshw = parsed()
        val stub = getStub()
        val constructed: Seq[AssetMetaValue] = LshwHelper.construct(stub, lshw)
        val reconstructed = LshwHelper.reconstruct(stub, metaValue2metaWrapper(constructed))._1
        lshw mustEqual reconstructed
      }
      "containing basic info" in new LshwCommonHelper("lshw-basic.xml") {
        val lshw = parsed()
        val stub = getStub()
        val constructed: Seq[AssetMetaValue] = LshwHelper.construct(stub, lshw)
        val reconstructed = LshwHelper.reconstruct(stub, metaValue2metaWrapper(constructed))._1
        lshw mustEqual reconstructed
      }
     "with an intel CPU" in new LshwCommonHelper("lshw-intel.xml") {
        val lshw = parsed()
        val stub = getStub()
        val constructed: Seq[AssetMetaValue] = LshwHelper.construct(stub, lshw)
        val reconstructed = LshwHelper.reconstruct(stub, metaValue2metaWrapper(constructed))._1
        lshw mustEqual reconstructed
      }
      "with modern hardware but old lshw" in new LshwCommonHelper("lshw-new-web-old-lshw.xml") {
        val lshw = parsed()
        val stub = getStub()
        val constructed: Seq[AssetMetaValue] = LshwHelper.construct(stub, lshw)
        val reconstructed = LshwHelper.reconstruct(stub, metaValue2metaWrapper(constructed))._1
        lshw mustEqual reconstructed
      }
      "with old hardware and old lshw" in new LshwCommonHelper("lshw-old-web.xml") {
        val lshw = parsed()
        val stub = getStub()
        val constructed: Seq[AssetMetaValue] = LshwHelper.construct(stub, lshw)
        val reconstructed = LshwHelper.reconstruct(stub, metaValue2metaWrapper(constructed))._1
        lshw mustEqual reconstructed
      }
      "with an older (B.02.12) format" in new LshwCommonHelper("lshw-old.xml") {
        val lshw = parsed()
        val stub = getStub()
        val constructed: Seq[AssetMetaValue] = LshwHelper.construct(stub, lshw)
        val reconstructed = LshwHelper.reconstruct(stub, metaValue2metaWrapper(constructed))._1
        lshw mustEqual reconstructed
      }
      "with a quad NIC" in new LshwCommonHelper("lshw-quad.xml") {
        val lshw = parsed()
        val stub = getStub()
        val constructed: Seq[AssetMetaValue] = LshwHelper.construct(stub, lshw)
        val reconstructed = LshwHelper.reconstruct(stub, metaValue2metaWrapper(constructed))._1
        lshw mustEqual reconstructed
      }
      "containing a virident card" in new LshwCommonHelper("lshw-virident.xml") {
        val lshw = parsed()
        val stub = getStub()
        val constructed: Seq[AssetMetaValue] = LshwHelper.construct(stub, lshw)
        val reconstructed = LshwHelper.reconstruct(stub, metaValue2metaWrapper(constructed))._1
        lshw mustEqual reconstructed
      }
      "with an LVM volume on a disk" in new LshwCommonHelper("lshw-lvm.xml") {
        val lshw = parsed()
        val stub = getStub()
        val constructed: Seq[AssetMetaValue] = LshwHelper.construct(stub, lshw)
        val reconstructed = LshwHelper.reconstruct(stub, metaValue2metaWrapper(constructed))._1
        lshw mustEqual reconstructed
      }
      "create asset" in new LshwCommonHelper("lshw-basic.xml") {
        val asset = Asset.create(Asset("lshw-asset", Status.Incomplete.get, AssetType.ServerNode.get))
        LshwHelper.updateAsset(asset, parsed())
        asset.getMetaAttribute(AssetMeta.Enum.DiskType.toString) must beSome
      }
      "update asset LSHW with smaller profile" in new LshwCommonHelper("lshw-small.xml") {
        val asset = Asset.create(Asset("lshw-asset", Status.Incomplete.get, AssetType.ServerNode.get))
        //lshw-small.xml has no disks
        LshwHelper.updateAsset(asset, parsed())
        asset.getMetaAttribute(AssetMeta.Enum.DiskType.toString) must beNone
      }
    }
  }

  class LshwCommonHelper(txt: String) extends WithApplication with CommonHelperSpec[LshwRepresentation] {
    def getParser(str: String) = new LshwParser(str)
    override def parsed(): LshwRepresentation = getParsed(txt)
  }

}
