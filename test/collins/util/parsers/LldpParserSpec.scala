package collins.util.parsers

import org.specs2._
import specification._
import play.api.test.WithApplication
import play.api.test.FakeApplication
import collins.util.LldpRepresentation

class LldpParserSpec extends mutable.Specification {

  class LldpParserHelper(val filename: String, val ac: Map[String, _ <: Any] = Map.empty)
      extends WithApplication(FakeApplication(additionalConfiguration = ac)) with CommonParserSpec[LldpRepresentation] {
    override def getParser(txt: String) = new LldpParser(txt)
    def parsed() = getParseResults(filename)
  }

  "The Lldp Parser" should {
    "Parse XML with one network interface" in new LldpParserHelper("lldpctl-single.xml") {
      val parseResult = parsed()
      parseResult must beRight
      parseResult.right.toOption must beSome.which { rep =>
        rep.interfaceCount mustEqual (1)
        rep.macAddresses must contain(exactly(Seq("78:19:f7:88:60:c0"): _*))
        rep.interfaceNames must contain(exactly(Seq("eth0"): _*))
        rep.localPorts must contain(exactly(Seq(616): _*))
        rep.chassisNames must contain(exactly(Seq("core01.dfw01"): _*))
        rep.vlanNames must contain(exactly(Seq("DFW-LOGGING"): _*))
        rep.vlanIds must contain(exactly(Seq(106): _*))
      }
    }

    "Parse XML with two network interfaces" in new LldpParserHelper("lldpctl-two-nic.xml") {
      val parseResult = parsed()
      parseResult must beRight
      parseResult.right.toOption must beSome.which { rep =>
        rep.interfaceCount mustEqual (2)
        rep.macAddresses must contain(exactly("78:19:f7:88:60:c0", "5c:5e:ab:68:a5:80"))
        rep.interfaceNames must contain(exactly("eth0", "eth1"))
        rep.localPorts.toSet mustEqual (Set(608))
        rep.chassisNames must contain(exactly("core01.dfw01", "core02.dfw01"))
        rep.vlanNames.toSet mustEqual (Set("DFW-LOGGING"))
        rep.vlanIds.toSet mustEqual (Set(106))
      }
    }

    "missing vlan config fail" in new LldpParserHelper("lldpctl-no-name.xml") {
      val parseResult = parsed()
      parseResult must beLeft
    }

    "missing vlan config ok" in new LldpParserHelper("lldpctl-no-name.xml", Map("lldp.requireVlanName" -> "false")) {
      val parseResult = parsed()
      parseResult must beRight
      parseResult.right.toOption must beSome.which { rep =>
        rep.interfaceCount mustEqual (2)
        rep.vlanNames.toSet mustEqual (Set(""))
        rep.vlanIds.toSet mustEqual (Set(100, 101))
      }
    }

    "Parse XML with four network interfaces" in new LldpParserHelper("lldpctl-four-nic.xml") {
      val parseResult = parsed()
      parseResult must beRight
      parseResult.right.toOption must beSome.which { rep =>
        rep.interfaceCount mustEqual (3)
        rep.macAddresses must contain(exactly(
          "2c:21:72:96:93:00", "28:c0:da:b9:5b:f0", "84:18:88:9c:57:f0"))
        rep.interfaceNames must contain(exactly("eth0", "eth4", "eth5"))
        rep.localPorts.toSet mustEqual (Set(588, 2113, 1488))
        rep.chassisNames must contain(exactly(
          "oob-switch013.ewr01", "re0.access-switch01.ewr01", "re0.access-switch02.ewr01"))
        rep.vlanNames.toSet mustEqual (Set("EWR-PROVISIONING", "OOB-NETWORK", "OOB-POWER", "OOB-SERVERS"))
        rep.vlanIds.toSet mustEqual (Set(104, 115, 114, 108))
      }
    }

    "Parse a generated XML file" in new LldpParserHelper("lldpctl-empty.xml") {
      parsed() must beRight
    }

    "Fail to parse wrong XML" in new LldpParserHelper("lshw-basic.xml") {
      parsed() must beLeft
    }

    "Fail to parse text" in new LldpParserHelper("hello world") {
      getParser(filename).parse() must beLeft
    }

    "Fail to parse invalid XML" in {

      "Missing chassis name" in new LldpParserHelper("lldpctl-bad.xml") {
        val invalidXml = getResource(filename)
        override def getParseResults(data: String): Either[Throwable, LldpRepresentation] = {
          getParser(data).parse()
        }

        getParseResults(invalidXml.replace("""<name label="SysName">core01.dfw01</name>""", "")) must beLeft
      }
      "Missing chassis description" in new LldpParserHelper("lldpctl-bad.xml") {

        val invalidXml = getResource(filename)
        override def getParseResults(data: String): Either[Throwable, LldpRepresentation] = {
          getParser(data).parse()
        }

        val s = """<descr label="SysDescr">Juniper Networks, Inc. ex4500-40f , version 11.1S1 Build date: 2011-04-21 08:03:12 UTC </descr>"""
        val r = ""
        getParseResults(invalidXml.replace(s, r)) must beLeft
      }
      "Missing chassis id type" in new LldpParserHelper("lldpctl-bad.xml") {
        val invalidXml = getResource(filename)
        override def getParseResults(data: String): Either[Throwable, LldpRepresentation] = {
          getParser(data).parse()
        }
        val s = """<id label="ChassisID" type="mac">78:19:f7:88:60:c0</id>"""
        val r = """<id label="ChassisID">78:19:f7:88:60:c0</id>"""
        getParseResults(invalidXml.replace(s, r)) must beLeft
      }
      "Missing chassis id value" in new LldpParserHelper("lldpctl-bad.xml") {
        val invalidXml = getResource(filename)
        override def getParseResults(data: String): Either[Throwable, LldpRepresentation] = {
          getParser(data).parse()
        }
        val s = """<id label="ChassisID" type="mac">78:19:f7:88:60:c0</id>"""
        val r = """<id label="ChassisID" type="mac"/>"""
        getParseResults(invalidXml.replace(s, r)) must beLeft
      }

      "Missing port id type" in new LldpParserHelper("lldpctl-bad.xml") {
        val invalidXml = getResource(filename)
        override def getParseResults(data: String): Either[Throwable, LldpRepresentation] = {
          getParser(data).parse()
        }
        val s = """<id label="PortID" type="local">616</id>"""
        val r = """<id label="PortID">616</id>"""
        getParseResults(invalidXml.replace(s, r)) must beLeft
      }
      "Missing port id value" in new LldpParserHelper("lldpctl-bad.xml") {
        val invalidXml = getResource(filename)
        override def getParseResults(data: String): Either[Throwable, LldpRepresentation] = {
          getParser(data).parse()
        }
        val s = """<id label="PortID" type="local">616</id>"""
        val r = """<id label="PortID" type="local"/>"""
        getParseResults(invalidXml.replace(s, r)) must beLeft
      }
      "Missing port description" in new LldpParserHelper("lldpctl-bad.xml") {
        val invalidXml = getResource(filename)
        override def getParseResults(data: String): Either[Throwable, LldpRepresentation] = {
          getParser(data).parse()
        }
        val s = """<descr label="PortDescr">ge-0/0/7.0</descr>"""
        val r = ""
        getParseResults(invalidXml.replace(s, r)) must beLeft
      }

      "Missing vlan name" in new LldpParserHelper("lldpctl-bad.xml") {
        val invalidXml = getResource(filename)
        override def getParseResults(data: String): Either[Throwable, LldpRepresentation] = {
          getParser(data).parse()
        }
        val s = """<vlan label="VLAN" vlan-id="106" pvid="yes">DFW-LOGGING</vlan>"""
        val r = """<vlan label="VLAN" vlan-id="106" pvid="yes"/>"""
        getParseResults(invalidXml.replace(s, r)) must beLeft
      }
      "Missing vlan id" in new LldpParserHelper("lldpctl-bad.xml") {
        val invalidXml = getResource(filename)
        override def getParseResults(data: String): Either[Throwable, LldpRepresentation] = {
          getParser(data).parse()
        }
        val s = """<vlan label="VLAN" vlan-id="106" pvid="yes">DFW-LOGGING</vlan>"""
        val r = """<vlan label="VLAN" pvid="yes">DFW-LOGGING</vlan>"""
        getParseResults(invalidXml.replace(s, r)) must beLeft
      }
    }
  }

}
