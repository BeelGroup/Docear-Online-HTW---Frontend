# Demo
* http://desolate-wildwood-9229.herokuapp.com/ with credentials docear freeplane537

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
* Play Documentation: http://www.playframework.org/documentation/2.0.4/Home
    * Java Developing: http://www.playframework.org/documentation/2.0.4/JavaHome
    * Java API: http://www.playframework.org/documentation/api/2.0.4/java/index.html
* Generate IDE Project Files
      * `sbt eclipse` #Eclipse
      * `sbt idea` #IntelliJ IDEA
      * Netbeans: http://www.playframework.org/documentation/2.0.4/IDE
* run unit tests in batch mode: `sbt test`
* run QUnit tests in Browser [http://localhost:9000/qunit](http://localhost:9000/qunit)
* Writing QUnit tests:
     * currently: add a coffee file in app/assets/javascripts/test and add it to app/views/qunit/test.scala.html as JavaScript link
* don't use CSS directly, use [less](http://lesscss.org/) in the folder app/assets/stylesheets
* don't use JavaScript directly, use [CoffeeScript](http://coffeescript.org/) in the folder app/assets/javascripts
* [Debugging](https://github.com/Docear/HTW-Frontend/blob/master/dev-doc/debug.md)
* recover from hard build errors: `sbt clean`

## Backend mock
* default: it delivers static files to easy frontend development
* using docear2 in development `sbt -Dconfig.file=conf/backenddevdocear2.conf ~run`
* static files are currently in conf/rest/v1

# API DOC
## CoffeeScript
### View and usage
* http://141.45.146.249:8080/job/Frontend/ws/coffeedoc/index.html with credentials docear freeplane537
    * updates with every successful build with jenkins
* how to make doc comments: https://github.com/netzpirat/codo

### Installation Debian

    #node installation
    apt-get install python
    cd /tmp
    git clone https://github.com/joyent/node.git --depth 1
    cd node
    NODE_JS_VERSION_TAG="v0.8.14"
    git checkout $NODE_JS_VERSION_TAG
    ./configure
    make
    make install

    #installation codo
    npm install -g coffee-script
    npm install -g codo
### Installation Ubuntu

    sudo apt-get install npm
    sudo npm install -g codo



### Install sublime, node and coffee-script on ur windows platform

1. [Download](http://www.sublimetext.com/2) and install Sublime 

2. Support CoffeeScript
to support syntax highlighting, navigate to 
'"C:\Users\[USER]\AppData\Roaming\Sublime Text 2\Packages"'
add the folder "CoffeeScript" and add [this](https://github.com/jashkenas/coffee-script-tmbundle/blob/master/Syntaxes/CoffeeScript.tmLanguage) file to the new folder:


3. [Download](http://nodejs.org/#download) and install node 

4. maybe you have to restart ur computer 
(path variable needs to be updated)

5. get & install json and finally CoffeeScript
open console (cmd) and type:
    `npm install json
    npm install -g coffee-script`
(folder will be here: C:\Users\[USER]\AppData\Roaming\npm\node_modules\coffee-script)

6. Create build systen in Sublime (so u can compile with strg+b)
Open Sublime an navigate to:
Tools -> Build-System -> New Build System... 
insert:
    `{
    "shell" : true,
    "cmd": ["coffee","-c","$file"],
    "file_regex": "^(...*?):([0-9]*):?([0-9]*)",
    "selector": "source.coffee"
    }`
and save as "Coffee-Script.sublime-build"
(will be saved here: C:\Users\[USER]\AppData\Roaming\Sublime Text 2\Packages\User)


restart sublime

have fun!


also u can have a look [here](http://wesbos.com/sublime-text-build-scripts/) and [here](http://kevinpelgrims.wordpress.com/2011/12/28/building-coffeescript-with-sublime-on-windows/):



# Deployment

## Heroku
### Auto Deployment
Nach jedem Push zu Github wird die App automatisch deployed.

### Manuelles Deployment
1. get invitation for collaboration from Michael Schleichardt with your e-mail adress
1. install the heroku toolbelt: https://toolbelt.herokuapp.com/
1. checkout the project from github
1. git remote add heroku git@heroku.com:desolate-wildwood-9229.git
1. git push heroku # deploys into the cloud, may take a while

### Restart
* in HTW-Frontend folder: `heroku restart`

# Play Benefits
* Hot deployment changed Java classes and CoffeeScript, JavaScript, HTML files
* Play compiles CoffeeScript
* Minifies JavaScript files
* simple setup
* IDE support: just an editor, Eclipse, IDEA, Netbeans (a little)
* Dependency Management (one line to receive JARs)
* Test infrastructure (Selenium, JUnit, Specs2, fest assert)
* Supports Long Polling, WebSockets
* Asynchronous Jobs
* easy to develop modules
* deployment standalone, in cloud, as war in Tomcat
* works well for frontends and backends


# Links
## CoffeeScript
* http://autotelicum.github.com/Smooth-CoffeeScript/interactive/interactive-coffeescript.html
