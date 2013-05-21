import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.{ObjectId, RepositoryCache}
import org.eclipse.jgit.storage.file.ReflogEntry
import org.eclipse.jgit.util.FS
import sbt._
import sbt.ExclusionRule
import sbt.Keys._
import play.Project._
import sbt.PlayExceptions.CompilationException
import scala.Some
import System._

object ApplicationBuild extends Build {

    val appName         = "Docear-Frontend"
    val appVersion      = "0.1-SNAPSHOT"

    val frontendDependencies = {
      val seleniumVersion = "2.32.0"
      Seq(
        "org.seleniumhq.selenium" % "selenium-java" % seleniumVersion % "test" //find new versions on http://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-firefox-driver
        , "org.seleniumhq.selenium" % "selenium-firefox-driver" % seleniumVersion % "test" //find new versions on http://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-firefox-driver
        , "org.seleniumhq.selenium" % "selenium-chrome-driver" % seleniumVersion % "test" //find new versions on http://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-firefox-driver
        , "org.seleniumhq.selenium" % "selenium-htmlunit-driver" % seleniumVersion % "test"
        , "org.apache.httpcomponents" % "httpclient" % "4.2.3" //needed by selenium, must be in compile scope
        , "org.webjars" % "webjars-play" % "2.1.0"
        , "org.webjars" % "bootstrap" % "2.1.1"
        , "org.webjars" % "font-awesome" % "3.0.2"
        , "org.webjars" % "requirejs" % "2.1.5"
      )
    }

    val backendDependencies = {
      val hadoopVersion = "0.23.7"
      Seq(
        "org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion excludeAll(
          ExclusionRule(organization = "com.sun.jdmk"),
          ExclusionRule(organization = "com.sun.jmx"),
          ExclusionRule(organization = "javax.jms"),
          ExclusionRule(name = "commons-daemon"),
          ExclusionRule(organization = "org.codehaus.jettison")
          )
        , "org.apache.hadoop" % "hadoop-common" % hadoopVersion excludeAll(ExclusionRule(organization = "javax.activation"), ExclusionRule(organization = "org.codehaus.jettison"))
        , "com.typesafe.akka" %% "akka-remote" % "2.1.2"
        , "info.schleichardt" %% "play-embed-mongo" % "0.2"
        , "org.mongojack" % "mongojack" % "2.0.0-RC4" //working with JSON and POJOs using a MongoDB http://mongojack.org/
      )
    }

    val appDependencies = frontendDependencies ++ backendDependencies ++ {
      Seq(
        "junit" % "junit-dep" % "4.11" % "test"
        , "com.fasterxml.jackson.datatype" % "jackson-datatype-json-org" % "2.0.2"
        , "commons-lang" % "commons-lang" % "2.6"
        , "org.springframework" % "spring-context" % "3.1.2.RELEASE"
        , "cglib" % "cglib" % "2.2.2"
        , "com.novocode" % "junit-interface" % "0.9" % "test"
        , "org.reflections" % "reflections" % "0.9.8"//fix for error: NoSuchMethodError: com.google.common.cache.CacheBuilder.maximumSize(I)Lcom/google/common/cache/CacheBuilder;
        , "joda-time" % "joda-time" % "2.1"
        , javaCore
        , "info.schleichardt" %% "play-2-mailplugin" % "0.9-SNAPSHOT"
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

   lazy val gitInfos = {
     try {
       val repo = RepositoryCache.open(RepositoryCache.FileKey.lenient(new File(".git"), FS.DETECTED), true)
       val git = new Git(repo)
       val iterator = git.reflog().call().iterator()
       val entry = iterator.next
       """git.newId=%s
         |git.oldId=%s
         |git.comment=%s
         |git.who=%s
       """.stripMargin.format(ObjectId.toString(entry.getNewId), ObjectId.toString(entry.getOldId), entry.getComment, entry.getWho.toString)
     } catch {
       case e: RepositoryNotFoundException => "git.info.missing=true"
     }
   }

  lazy val nativeRequireJSinstalled = {
    import scala.sys.process._
    "which r.js".! == 0
  }

    val main = play.Project(appName, appVersion, appDependencies).settings(
      coffeescriptOptions := Seq("bare")//coffee script code will not be wrapped in an anonymous function, necessary for tests
     // , resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots" //commented out because sonatype is offline on 21.03.2013 12:28
      , resolvers += "schleichardts Github" at "http://schleichardt.github.com/jvmrepo/" //temporary. delete if sonatype is up again
      , templatesImport += "views.TemplateUtil._"
      , handlebarsEntryPoints <<= (sourceDirectory in Compile)(base => base / "assets" / "javascripts" / "views" / "templates" ** "*.handlebars")
      , handlebarsOptions := Seq.empty[String]
      , resourceGenerators in Compile <+= HandlebarsPrecompileTask("handlebars-min-1.0.beta.js")
      , logBuffered in Test := false
      , parallelExecution in Test := false
      , testOptions in Test += Tests.Argument("sequential", "true")
      , javacOptions ++= Seq("-source", "1.6", "-target", "1.6")//for compatibility with Debian Squeeze
      , cleanFiles <+= baseDirectory {base => base / "h2"} //clean up h2 data files
      , resourceGenerators in Compile <+= (resourceManaged in Compile) map { dir =>
        val file = dir / "buildInfo.properties"
        IO.write(file, gitInfos)
        Seq(file)
      }
      , requireJs += "main.js"
      , javacOptions ++= Seq("-Xlint:-options")
      , javacOptions ++= Seq("-Xlint:deprecation")
    )

}
