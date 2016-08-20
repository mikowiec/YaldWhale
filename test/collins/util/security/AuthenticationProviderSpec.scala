package collins.util.security

import play.api.Configuration
import collins.models.User
import org.specs2.mutable._
import java.io.File
import play.api.test.WithApplication
import play.api.test.FakeApplication
import play.api.Play

object AuthenticationProviderSpec extends Specification with collins.ResourceFinder {

  "Authentication Providers" should {
    "work with default authentication" >> {
      val provider = new MockAuthenticationProvider
      provider.authenticate("blake", "admin:first") must beSome[User]
      provider.authenticate("no", "suchuser") must beNone
    }
    val authFile = findResource("htpasswd_users")
    "work with file based auth" in new WithApplication(FakeApplication(additionalConfiguration = Map(
      "authentication.file.userfile" -> authFile.getAbsolutePath))) {
      val provider = AuthenticationProvider.get(List("file"))

      val users = Seq(
        ("blake", "password123", Set("engineering")),
        ("testuser", "FizzBuzzAbc", Set("ny", "also")))

      users.foreach {
        case (username, password, roles) =>
          val user = provider.authenticate(username, password)
          user must beSome[User]
          user.get.username mustEqual username
          user.get.password mustNotEqual password
          user.get.isAuthenticated must beTrue
          user.get.roles mustEqual roles
      }
      provider.authenticate("blake", "abbazabba") must beNone
    }
  }
}

object GlobalAuthenticationAccessorSpec extends Specification with collins.ResourceFinder {
  "Global object" should {
    val authFile = findResource("htpasswd_users")
    "have only a singleton for auth provider" in new WithApplication(FakeApplication(additionalConfiguration = Map(
      "authentication.file.userfile" -> authFile.getAbsolutePath))) {
      val authAccessor = Play.current.global.asInstanceOf[AuthenticationAccessor]
      val provider = authAccessor.getAuthentication()
      val another = authAccessor.getAuthentication()
      provider.authType mustEqual List("default")
      another.authType mustEqual List("default")
      provider must be equalTo another
    }
  }
}
