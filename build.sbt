import mimabuild._

inThisBuild(Seq(
  organization := "com.typesafe",
  licenses := Seq("Apache License v2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("http://github.com/lightbend/mima")),
  developers := List(
    Developer("mdotta", "Mirco Dotta", "@dotta", url("https://github.com/dotta")),
    Developer("jsuereth", "Josh Suereth", "@jsuereth", url("https://github.com/jsuereth")),
    Developer("dwijnand", "Dale Wijnand", "@dwijnand", url("https://github.com/dwijnand")),
  ),
  scmInfo := Some(ScmInfo(url("https://github.com/lightbend/mima"), "scm:git:git@github.com:lightbend/mima.git")),
  dynverVTagPrefix := false,
  scalacOptions := Seq("-feature", "-deprecation", "-Xlint"),
//resolvers += stagingResolver,
))

// Useful to self-test releases
val stagingResolver = "Sonatype OSS Staging" at "https://oss.sonatype.org/content/repositories/staging"

val root = project.in(file(".")).disablePlugins(BintrayPlugin).settings(
  name := "mima",
  crossScalaVersions := Nil,
  mimaFailOnNoPrevious := false,
  skip in publish := true,
)
aggregateProjects(core, sbtplugin, functionalTests)

val core = project.disablePlugins(BintrayPlugin).settings(
  name := "mima-core",
  libraryDependencies ++= Seq(
    "org.scala-lang" %  "scala-compiler" % scalaVersion.value,
    "org.scalatest"  %% "scalatest"      % "3.0.8" % Test,
  ),
  MimaSettings.mimaSettings,
  apiMappings ++= {
    // WORKAROUND https://github.com/scala/bug/issues/9311
    // from https://stackoverflow.com/a/31322970/463761
    sys.props("sun.boot.class.path")
      .split(java.io.File.pathSeparator)
      .collectFirst { case str if str.endsWith(java.io.File.separator + "rt.jar") =>
        file(str) -> url("http://docs.oracle.com/javase/8/docs/api/index.html")
      }
      .toMap
  },
  publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging),

)

val sbtplugin = project.enablePlugins(SbtPlugin).dependsOn(core).settings(
  name := "sbt-mima-plugin",
  // drop the previous value to drop running Test/compile
  scriptedDependencies := Def.task(()).dependsOn(publishLocal, publishLocal in core).value,
  scriptedLaunchOpts += s"-Dplugin.version=${version.value}",
  scriptedLaunchOpts += s"-Dsbt.boot.directory=${file(sys.props("user.home")) / ".sbt" / "boot"}",
  MimaSettings.mimaSettings,
  bintrayOrganization := Some("typesafe"),
  bintrayReleaseOnPublish := false,
)

val functionalTests = Project("functional-tests", file("functional-tests"))
  .dependsOn(core)
  .enablePlugins(TestsPlugin)
  .disablePlugins(BintrayPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe"           %  "config"                  % "1.4.0",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.2",
    ),
    mimaFailOnNoPrevious := false,
    skip in publish := true,
  )
