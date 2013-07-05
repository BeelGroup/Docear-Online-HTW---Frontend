# Demo
* https://staging.my.docear.org/ with credentials docear fki-H-12072013


# Continuous Integration
* https://ci.docear.org/ci/job/Frontend/

# Installation 
1. make sure you have installed a JDK (6 or 7) correctly
    * open a console and enter `javac -version`, that should print your java version, for example `javac 1.6.0_37`
    * you may have to add your Java *bin* folder to the PATH environment variable and open a new console window
1. checkout the project
1. go to the project folder with the console
    * check: `dir` (or `ls` on UNIX) should show you the folders app, conf, project and some more
1. then enter the command `run` or `run.bat`, this may take a while the first time because it downloads a lot of JARs, you may even think that it hangs
    * if this is the first time you run this script, check your firewall if it's blocking the download of libraries
1. you can view the application in the browser after it printed something like `play - Listening for HTTP on port 9000...`
1. open [http://localhost:9000](http://localhost:9000) in your browser
1. use Ctrl + D or Ctrl + C to stop the application

# Programming
* Play Documentation: http://www.playframework.org/documentation/2.1.0/Home
    * Java Developing: http://www.playframework.org/documentation/2.1.0/JavaHome
    * Java API: http://www.playframework.org/documentation/api/2.1.0/java/index.html
* Generate IDE Project Files
      * `sbt eclipse` #Eclipse
      * `sbt idea` #IntelliJ IDEA
      * Netbeans: http://www.playframework.org/documentation/2.1.0/IDE
* run unit tests in batch mode: `sbt test`
* don't use CSS directly, use [less](http://lesscss.org/) in the folder app/assets/stylesheets
* don't use JavaScript directly, use [CoffeeScript](http://coffeescript.org/) in the folder app/assets/javascripts
* [Debugging](https://github.com/Docear/HTW-Frontend/blob/master/dev-doc/debug.md)
* recover from hard build errors: `sbt clean`
* using initial data for the projects: see ProjectFixturesPlugin.java
* using a specific configuration: `sbt -Dconfig.file=conf/backenddevdocear2.conf compile ~run`

# Packaging
`sbt dist`
