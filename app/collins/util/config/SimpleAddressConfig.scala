package collins.util.config

import collins.models.shared.AddressPool.poolName

case class SimpleAddressConfig(
  override val source: TypesafeConfiguration,
  orName: Option[String] = None,
  orStrict: Option[Boolean] = None
) extends ConfigAccessor with ConfigSource {

  // Default pool to use, if configured, hidden since we may end up with a naked config which will
  // still end up with the DefaultPoolName
  def defaultPoolName: Option[String] = getString("defaultPoolName").map(poolName(_)).filter(_.nonEmpty)
  def name = getString("name").orElse(orName)
  def strict = orStrict.orElse(getBoolean("strict")).getOrElse(true)
  def pools = getObjectMap("pools")
  def startAddress = getString("startAddress")
  def gateway = getString("gateway")
  def network = getString("network")
}
