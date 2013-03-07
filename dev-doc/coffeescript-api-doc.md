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

