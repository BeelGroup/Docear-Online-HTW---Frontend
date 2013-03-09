import sbt._
import sbt.Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "Docear-Frontend"
    val appVersion      = "0.1-SNAPSHOT"

    val appDependencies = {
      val seleniumVersion = "2.29.1"
      Seq(
        "info.schleichardt" %% "play-2-basic-auth" % "0.3-SNAPSHOT"
        , "commons-io" % "commons-io" % "2.4"//heroku does not find it without the explicit dependency
        , "com.fasterxml.jackson.datatype" % "jackson-datatype-json-org" % "2.0.2"
        , "commons-lang" % "commons-lang" % "2.6"
        , "org.springframework" % "spring-context" % "3.1.2.RELEASE"
        , "cglib" % "cglib" % "2.2.2"
        , "org.seleniumhq.selenium" % "selenium-firefox-driver" % seleniumVersion % "test" //find new versions on http://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-firefox-driver
        , "org.seleniumhq.selenium" % "selenium-chrome-driver" % seleniumVersion % "test" //find new versions on http://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-firefox-driver
        , "org.seleniumhq.selenium" % "selenium-htmlunit-driver" % seleniumVersion % "test"
        , "org.apache.httpcomponents" % "httpclient" % "4.2.3" //needed by selenium, must be in compile scope
        , "com.novocode" % "junit-interface" % "0.9" % "test"
        , "org.reflections" % "reflections" % "0.9.8"//fix for error: NoSuchMethodError: com.google.common.cache.CacheBuilder.maximumSize(I)Lcom/google/common/cache/CacheBuilder;
        , "joda-time" % "joda-time" % "2.1"
        , jdbc
        , javaCore
        , javaJdbc
        , javaEbean
        , "org.webjars" % "webjars-play" % "2.1.0"
        , "org.webjars" % "bootstrap" % "2.1.1"
        , "com.typesafe.akka" %% "akka-remote" % "2.1.0"
        , "info.schleichardt" %% "play-2-mailplugin" % "0.8"
      )
    }

    val handlebarsOptions = SettingKey[Seq[String]]("ember-options")
    val handlebarsEntryPoints = SettingKey[PathFinder]("ember-entry-points")

    //TODO impove performance: in Play 2.1 the asset compilation mechanism changed, the current implementation compiles
    // for every handlebars file all other files again
    def HandlebarsPrecompileTask(handlebarsJsFilename: String) = {
      val compiler = new sbt.handlebars.HandlebarsCompiler(handlebarsJsFilename)

      AssetsCompiler("handlebars-precompile", (_ ** "*.handlebars"), handlebarsEntryPoints, { (name, min) => "javascripts/views/templates.pre" + (if (min) ".min.js" else ".js") },
      { (handlebarsFile, options) =>
        val (jsSource, dependencies) = compiler.compileDir(handlebarsFile.getParentFile, options)
        // Any error here would be because of Handlebars, not the developer;
        // so we don't want compilation to fail.
        import scala.util.control.Exception._
        val minified = catching(classOf[CompilationException])
          .opt(play.core.jscompile.JavascriptCompiler.minify(jsSource, Some(handlebarsFile.getName())))
        (jsSource, minified, dependencies)
      },
      handlebarsOptions
      )
    }

    val main = play.Project(appName, appVersion, appDependencies).settings(
      coffeescriptOptions := Seq("bare")//coffee script code will not be wrapped in an anonymous function, necessary for tests
      , resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
      , templatesImport += "views.TemplateUtil._"
      , handlebarsEntryPoints <<= (sourceDirectory in Compile)(base => base / "assets" / "javascripts" / "views" / "templates" ** "*.handlebars")
      , handlebarsOptions := Seq.empty[String]
      , resourceGenerators in Compile <+= HandlebarsPrecompileTask("handlebars-1.0.rc.1.js")
      , logBuffered in Test := false
      , parallelExecution in Test := false
      , testOptions in Test += Tests.Argument("sequential", "true")
      , javacOptions ++= Seq("-source", "1.6", "-target", "1.6")//for compatibility with Debian Squeeze
      , cleanFiles <+= baseDirectory {base => base / "h2"} //clean up h2 data files
    )

}
