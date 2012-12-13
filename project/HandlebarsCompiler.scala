/*
inspiration and some copied code: https://gist.github.com/2483787
 */

package sbt.handlebars

import java.io._
import play.api._

class HandlebarsCompiler(handlebarsJsFilename: String) {

  import org.mozilla.javascript._
  import org.mozilla.javascript.tools.shell._


  import scalax.file._

  /**
   * find a file with the given name in the current directory or any subdirectory
   */
  private def findFile(name: String): Option[File] = {
    def findIn(dir: File): Option[File] = {
      for (file <- dir.listFiles) {
        if (file.isDirectory) {
          findIn(file) match {
            case Some(file) => return Some(file)
            case None => // keep trying
          }
        } else if (file.getName == name) {
          return Some(file)
        }
      }
      None
    }
    findIn(new File("."))
  }

  private lazy val compiler = {
    val ctx = Context.enter; ctx.setOptimizationLevel(-1)
    val global = new Global; global.init(ctx)
    val scope = ctx.initStandardObjects(global)

    // set up global objects that emulate a browser context
    ctx.evaluateString(scope,
      """
        // make window an alias for the global object
        var window = this,
            document = {
              createElement: function(type) {
                return {
                  firstChild: {}
                };
              },
              getElementById: function(id) {
                return [];
              },
              getElementsByTagName: function(tagName) {
                return [];
              }
            },
            location = {
              protocol: 'file:',
              hostname: 'localhost',
              href: 'http://localhost:80',
              port: '80'
            },
            console = {
              log: function() {},
              info: function() {},
              warn: function() {},
              error: function() {}
            }

        // make a dummy jquery object just to make ember happy
        var jQuery = function() { return jQuery; };
        jQuery.ready = function() { return jQuery; };
        jQuery.inArray = function() { return jQuery; };
        jQuery.jquery = "1.7.1";
        var $ = jQuery;

        // our precompile function uses Ember to do the precompilation,
        // then converts the compiled function to its string representation
        function precompile(string) {
          return Handlebars.precompile(string).toString();
        }
      """,
      "browser.js",
      1, null)

    // load ember
    val emberFile = findFile(handlebarsJsFilename).getOrElse(throw new Exception("handlebars: could not find " + handlebarsJsFilename))
    ctx.evaluateString(scope, Path(emberFile).slurpString, handlebarsJsFilename, 1, null)

    val precompileFunction = scope.get("precompile", scope).asInstanceOf[Function]

    Context.exit

    (source: File) => {
      val handlebarsCode = Path(source).slurpString.replace("\r", "")
      Context.call(null, precompileFunction, scope, scope, Array(handlebarsCode)).asInstanceOf[String]
    }
  }

  def compileDir(root: File, options: Seq[String]): (String, Seq[File]) = {
    val dependencies = Seq.newBuilder[File]

    val output = new StringBuilder
    output ++= "(function() {\n" +
      "var template = Handlebars.template,\n" +
      "    templates = {};\n\n"

    def addTemplateDir(dir: File, path: String) {
      for {
        file <- dir.listFiles.toSeq.sortBy(_.getName)
        name = file.getName
      } {
        if (file.isDirectory) {
          addTemplateDir(file, path + name + "/")
        }
        else if (file.isFile && name.endsWith(".handlebars")) {
          val templateName = path + name.replace(".handlebars", "")
          println("ember: processing template %s".format(templateName))
          val jsSource = compile(file, options)
          dependencies += file
          output ++= "templates['%s'] = template(%s);\n\n".format(templateName, jsSource)
        }
      }
    }
    addTemplateDir(root, "")

    output ++= "})();\n"
    (output.toString, dependencies.result)
  }

  private def compile(source: File, options: Seq[String]): String = {
    try {
      compiler(source)
    } catch {
      case e: JavaScriptException =>

        val line = """.*on line ([0-9]+).*""".r
        val error = e.getValue.asInstanceOf[Scriptable]

        throw ScriptableObject.getProperty(error, "message").asInstanceOf[String] match {
          case msg @ line(l) => CompilationException(
            msg,
            source,
            Some(Integer.parseInt(l)))
          case msg => CompilationException(
            msg,
            source,
            None)
        }

      case e =>
        throw CompilationException(
          "unexpected exception during Ember compilation (file=%s, options=%s, ember=%s): %s".format(
            source, options, handlebarsJsFilename, e
          ),
          source,
          None)
    }
  }
}

case class CompilationException(message: String, file: File, atLine: Option[Int]) extends PlayException(
  "Compilation error", message) with PlayException.ExceptionSource {
  def line = atLine
  def position = None
  def input = Some(scalax.file.Path(file))
  def sourceName = Some(file.getAbsolutePath)
}