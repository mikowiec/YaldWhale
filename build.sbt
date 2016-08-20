resolvers += "Twitter Repository" at "http://maven.twttr.com/"

resolvers += "Sonatype-public" at "http://oss.sonatype.org/content/groups/public"

resolvers += "Restlet repository" at "http://maven.restlet.org"

Keys.fork in Test := true

javaOptions in Test := Seq("-Dconfig.file=conf/test.conf", "-XX:MaxPermSize=512M", "-Xms512m", "-Xmx512m")

coverageExcludedPackages := "views.html.*;collins.graphs.templates.html.*;collins.controllers.ref.*;collins.controllers.javascript.*;collins.app.ref.*;collins.app.javascript.*;controllers.*;collins.app.*;collins.DbUtil;controllers.javascript.*;controllers.ref.*"

parallelExecution in Test := false

parallelExecution in IntegrationTest := false

scalaVersion := "2.11.7"

autoScalaLibrary := true

scalacOptions ++= Seq("-deprecation","-unchecked", "-feature")

scalacOptions += "-feature"

scalacOptions += "-language:postfixOps"

scalacOptions += "-language:implicitConversions"

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2" % "2.3.13" % "test",
  "com.google.guava" % "guava" % "18.0",
  "com.jolbox" % "bonecp" % "0.8.0.RELEASE",
  "nl.grons" %% "metrics-scala" % "2.1.5",
  "com.addthis.metrics" % "reporter-config" % "2.3.1",
  "mysql" % "mysql-connector-java" % "5.1.36",
  "com.h2database" % "h2" % "1.4.188",
  "org.apache.solr" % "solr-solrj" % "5.3.0",
  "org.apache.solr" % "solr-core"  % "5.3.0",
  "org.apache.httpcomponents" % "httpclient" % "4.5",
  "org.apache.httpcomponents" % "httpmime" % "4.5",
  "commons-net" % "commons-net" % "3.3",
  "org.bouncycastle" % "bcprov-jdk16" % "1.46",
  "com.twitter" %% "util-core" % "6.23.0",
  "org.jsoup" % "jsoup" % "1.8.3",
  "org.squeryl" %% "squeryl" % "0.9.5-7",
  "com.gilt" %% "jerkson" % "0.6.6",
  "org.yaml" % "snakeyaml" % "1.16",
  "com.typesafe.play" %% "play-jdbc" % "2.3.9",
  "com.typesafe.play" %% "play-cache" % "2.3.9",
  "com.google.code.findbugs" % "jsr305" % "3.0.0",
  "org.webjars" %% "webjars-play" % "2.3.0-3",
  "org.webjars" % "bootstrap" % "3.3.5",
  "org.webjars" % "bootstrap-datepicker" % "1.4.0",
  "org.webjars" % "datatables" % "1.10.7",
  "org.webjars" % "datatables-plugins" % "1.10.7",
  "org.webjars" % "jquery" % "2.1.4",
  "com.hazelcast" % "hazelcast" % "3.5.2",
  ws
)
