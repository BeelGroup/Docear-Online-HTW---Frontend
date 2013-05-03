define ['MapLoader', 'views/RootNodeView', 'views/NodeView', 'views/CanvasView', 'views/MinimapView', 'views/ZoomPanelView', 'models/Node', 'models/RootNode'],  (MapLoader, RootNodeView, NodeView, CanvasView, MinimapView, ZoomPanelView, NodeModel, RootNodeModel) ->  
  module = ->

  class MapView extends Backbone.View

    tagName  : 'div'
    className: 'mindmap-viewport, no-selecting-highlight'


    constructor:(@id)->
      super()
      $(window).on('resize', @resizeViewport)


    resizeViewport:=>
      @updateWidthAndHeight()
      if typeof @minimap != 'undefined'
        @minimap.setViewportSize()


    loadMap: (@mapId) ->
      if @rootView isnt undefined
        document.log 'delete old map'
        @canvas.zoomCenter(false)
        @rootView.getElement().remove()

      document.log "call: loadMap #{mapId} (MapController)"

      @href = jsRoutes.controllers.MindMap.mapAsJson(@mapId).url
      # start loading after fadein
      @$el.parent().find(".loading-map-overlay").fadeIn 400, =>
        $.get(@href, @parseAndRenderMapByJsonData, "json")
      


    parseAndRenderMapByJsonData: (data)=>

      #DEBUGGING!
      # response from:
      # https://staging.my.docear.org/map/3/json?nodeCount=50
      data = {"id":"136749844057868.mm","isReadonly":false,"root":{"id":"ID_1723255651","nodeText":"Welcome","isHtml":false,"folded":false,"icons":[],"leftChildren":[{"id":"ID_1201323859","nodeText":"Are you new to Docear?","isHtml":false,"folded":false,"icons":[],"children":[{"id":"ID_949131562","nodeText":"Then spend 5 minutes looking at this mind map and its information","isHtml":false,"folded":false,"icons":[],"children":[],"hGap":0,"shiftY":0},{"id":"ID_1136372076","nodeText":"This will give you a good overview of Docears capabilities...","isHtml":false,"folded":false,"icons":[],"children":[],"hGap":0,"shiftY":0},{"id":"ID_396380598","nodeText":"... and hence enable you to benefit from Docears full functionality","isHtml":false,"folded":false,"icons":[],"children":[],"hGap":0,"shiftY":0},{"id":"ID_1443707089","nodeText":"<html>\n  <head>\n    \n  </head>\n  <body>\n    <p>\n      That will make your work much more effective<br>and save you lots of time\n    </p>\n  </body>\n</html>","isHtml":true,"folded":false,"icons":[],"children":[],"hGap":0,"shiftY":0},{"id":"ID_1314196426","nodeText":"Promised!","isHtml":false,"folded":false,"icons":["ksmiletris"],"children":[],"hGap":0,"shiftY":0}],"edgeStyle":{"style":"EDGESTYLE_HIDDEN"},"hGap":-163,"shiftY":-470},{"id":"ID_1046976431","nodeText":"Help","isHtml":false,"folded":false,"icons":[],"children":[{"id":"ID_402486912","nodeText":"User Manual","isHtml":false,"folded":false,"icons":[],"link":"http://www.docear.org/support/user-manual/","children":[],"hGap":0,"shiftY":0},{"id":"ID_647468086","nodeText":"Tell us your ideas","isHtml":false,"folded":false,"icons":[],"link":"http://www.docear.org/support/idea-tracker/","children":[],"hGap":0,"shiftY":0},{"id":"ID_1698803512","nodeText":"Ask a question","isHtml":false,"folded":false,"icons":[],"link":"http://www.docear.org/support/get-support/","children":[],"hGap":0,"shiftY":0},{"id":"ID_1791784277","nodeText":"Submit a Bug Report","isHtml":false,"folded":false,"icons":[],"link":"http://www.docear.org/support/bug-report/","children":[],"hGap":0,"shiftY":0},{"id":"ID_418884785","nodeText":"FAQ","isHtml":false,"folded":false,"icons":[],"link":"http://www.docear.org/support/faq-frequently-asked-questions/","children":[],"hGap":0,"shiftY":0},{"id":"ID_850664765","nodeText":"Buy a Feature","isHtml":false,"folded":false,"icons":[],"link":"http://www.docear.org/support/buy-a-feature/","children":[],"hGap":0,"shiftY":0},{"id":"ID_1137752462","nodeText":"Watch a video","isHtml":false,"folded":false,"icons":[],"link":"http://www.youtube.com/watch?v=3T5V4wTo2uo","children":[],"hGap":0,"shiftY":0}],"edgeStyle":{"width":2,"color":-16751002},"hGap":0,"shiftY":0},{"id":"ID_703448322","nodeText":"Examples","isHtml":false,"folded":false,"icons":[],"children":[{"id":"ID_1297465718","nodeText":"Major Mind Mapping Features","isHtml":false,"folded":false,"icons":[],"children":[{"id":"ID_1886246636","nodeText":"Attributes","isHtml":false,"folded":false,"icons":[],"childrenIds":["ID_566654208","ID_460451549","ID_349142527","ID_691117196","ID_282700923","ID_1422384115"],"hGap":0,"shiftY":0},{"id":"ID_1845462789","nodeText":"Images","isHtml":false,"folded":true,"icons":[],"childrenIds":["ID_1616796999","ID_1891755637","ID_452528799","ID_1279466317"],"hGap":0,"shiftY":0},{"id":"ID_1513162719","nodeText":"LaTeX","isHtml":false,"folded":true,"icons":[],"childrenIds":["ID_701321157","ID_1117394712"],"hGap":0,"shiftY":0},{"id":"ID_130342491","nodeText":"Summary Nodes","isHtml":false,"folded":true,"icons":[],"childrenIds":["ID_1287672899","ID_304901584","ID_1649686681","ID_1380349306"],"hGap":0,"shiftY":0},{"id":"ID_491553564","nodeText":"Free (Floating) Nodes","isHtml":false,"folded":true,"icons":[],"childrenIds":["ID_1226802166","ID_649461033","ID_971039626","ID_525837283"],"hGap":0,"shiftY":0},{"id":"ID_1915898063","nodeText":"Icons","isHtml":false,"folded":true,"icons":[],"childrenIds":["ID_1318535933","ID_1957467098","ID_1434886095"],"hGap":0,"shiftY":0},{"id":"ID_1580203669","nodeText":"Links","isHtml":false,"folded":true,"icons":[],"childrenIds":["ID_1850169993","ID_634699852","ID_1921629847","ID_33520894","ID_401086023"],"hGap":0,"shiftY":0},{"id":"ID_1227135545","nodeText":"Clouds","isHtml":false,"folded":true,"icons":[],"childrenIds":["ID_577805129"],"hGap":0,"shiftY":0},{"id":"ID_1241321237","nodeText":"Notes","isHtml":false,"folded":true,"icons":[],"childrenIds":["ID_744937414","ID_1640385861","ID_1887373749","ID_1467097879"],"hGap":0,"shiftY":0},{"id":"ID_1591431726","nodeText":"Node Formatting","isHtml":false,"folded":true,"icons":[],"childrenIds":["ID_1452058670","ID_1721266520","ID_1080334653","ID_1153374301"],"hGap":0,"shiftY":0},{"id":"ID_195571125","nodeText":"Auto Numbering","isHtml":false,"folded":true,"icons":[],"childrenIds":["ID_1899637700","ID_493071469","ID_200987983","ID_1576241944","ID_150087640"],"hGap":0,"shiftY":0}],"hGap":0,"shiftY":0},{"id":"ID_836153271","nodeText":"Keyboard Shortcuts","isHtml":false,"folded":true,"icons":[],"children":[{"id":"ID_1641903022","nodeText":"Using keyboard shortcuts makes your life much easier","isHtml":false,"folded":false,"icons":[],"childrenIds":[],"hGap":0,"shiftY":0},{"id":"ID_1533218274","nodeText":"Here are the most important ones","isHtml":false,"folded":false,"icons":[],"childrenIds":["ID_184652233","ID_484280163","ID_115851811","ID_73557042","ID_1622116177","ID_1871601649"],"hGap":0,"shiftY":0}],"hGap":0,"shiftY":0},{"id":"ID_156487929","nodeText":"Mouse Actions","isHtml":false,"folded":true,"icons":[],"children":[{"id":"ID_1710120997","nodeText":"You can move nodes very quick with the mouse in various ways","isHtml":false,"folded":false,"icons":[],"childrenIds":[],"hGap":0,"shiftY":0},{"id":"ID_1620693421","nodeText":"Drag&Drop the left corner of a node","isHtml":false,"folded":false,"icons":[],"childrenIds":["ID_1932019998"],"hGap":0,"shiftY":0},{"id":"ID_448465592","nodeText":"Drag&Drop the center of a node","isHtml":false,"folded":false,"icons":[],"childrenIds":["ID_43166963"],"hGap":0,"shiftY":0}],"hGap":0,"shiftY":0},{"id":"ID_1247812247","nodeText":"Managing your\nPapers","isHtml":false,"folded":true,"icons":[],"childrenIds":["ID_76148427"],"hGap":0,"shiftY":0},{"id":"ID_213818615","nodeText":"Managing your\nReferences","isHtml":false,"folded":true,"icons":[],"childrenIds":["ID_1504972219"],"hGap":0,"shiftY":0},{"id":"ID_822106860","nodeText":"Creating/Drafting Academic\nLiterature (Paper, Books, ...)","isHtml":false,"folded":true,"icons":[],"childrenIds":["ID_1954347648"],"hGap":0,"shiftY":0}],"edgeStyle":{"width":2,"color":-16750951},"hGap":0,"shiftY":0}],"rightChildren":[{"id":"ID_684647090","nodeText":"What is Docear?","isHtml":false,"folded":false,"icons":[],"children":[{"id":"ID_958748268","nodeText":"An Academic\nLiterature Suite","isHtml":false,"folded":false,"icons":[],"childrenIds":["ID_669246343","ID_671957684","ID_635125153","ID_492177429"],"hGap":0,"shiftY":0},{"id":"ID_359495161","nodeText":"It is based on Freeplane","isHtml":false,"folded":false,"icons":[],"childrenIds":["ID_1818652651","ID_1286991743","ID_1563401899"],"hGap":0,"shiftY":0},{"id":"ID_1145413517","nodeText":"Who is behind Docear?","isHtml":false,"folded":false,"icons":[],"childrenIds":["ID_695547925","ID_251122884"],"hGap":0,"shiftY":0},{"id":"ID_1373336333","nodeText":"What does it cost?","isHtml":false,"folded":false,"icons":[],"childrenIds":["ID_1850957007"],"hGap":0,"shiftY":0}],"edgeStyle":{"width":2,"color":-3355393},"hGap":0,"shiftY":0},{"id":"ID_897735871","nodeText":"This is a free node","isHtml":false,"folded":false,"icons":[],"children":[{"id":"ID_455171254","nodeText":"<html>\n  <head>\n    \n  </head>\n  <body>\n    <p>\n      A free node is like<br>a new root node\n    </p>\n  </body>\n</html>","isHtml":true,"folded":false,"icons":[],"childrenIds":[],"hGap":0,"shiftY":0},{"id":"ID_1947531230","nodeText":"It can contain...","isHtml":false,"folded":false,"icons":[],"childrenIds":["ID_1916608265"],"hGap":-300,"shiftY":-50},{"id":"ID_414996855","nodeText":"A free node can be placed anywhere in a mind map","isHtml":false,"folded":false,"icons":[],"childrenIds":["ID_1141289532"],"hGap":-178,"shiftY":50}],"edgeStyle":{"style":"EDGESTYLE_HIDDEN"},"hGap":-1167,"shiftY":140},{"id":"ID_1880424274","nodeText":"Important to note","isHtml":false,"folded":false,"icons":[],"children":[{"id":"ID_1085988171","nodeText":"Latest Version?","isHtml":false,"folded":false,"icons":[],"childrenIds":["ID_1896466450","ID_25606850"],"hGap":0,"shiftY":0},{"id":"ID_210411846","nodeText":"Feedback","isHtml":false,"folded":false,"icons":[],"childrenIds":["ID_1039608161","ID_734818819"],"hGap":0,"shiftY":0},{"id":"ID_182288749","nodeText":"Usability","isHtml":false,"folded":false,"icons":[],"childrenIds":["ID_1866937594","ID_549788626","ID_166372850"],"hGap":0,"shiftY":0},{"id":"ID_1643291026","nodeText":"Reference Docear","isHtml":false,"folded":false,"icons":[],"childrenIds":["ID_658844051"],"hGap":0,"shiftY":0}],"edgeStyle":{"width":2,"color":-13395457},"hGap":0,"shiftY":0},{"id":"ID_657573964","nodeText":"<html>\n  <head>\n    \n  </head>\n  <body>\n    <p>\n      To unfold/open a node, select it and press <font face=\"Courier New\">SPACE</font>,\n    </p>\n    <p>\n      or select the node and click on it again,\n    </p>\n    <p>\n      or select it and press&#160; <font face=\"Courier New\">ARROW </font>in the direction of the little &quot;bubble&quot;\n    </p>\n  </body>\n</html>","isHtml":true,"folded":false,"icons":[],"children":[],"edgeStyle":{"width":3,"color":-256},"hGap":-150,"shiftY":7}]},"name":"3.mm","revision":0}

      $('#current-mindmap-name').text(data.name)
      #document.rootID = data.root.id
      
      mapLoader = new MapLoader(data);

      @rootView = new RootNodeView mapLoader.firstLoad()
      mapLoader.injectRootView @rootView
      @rootView.renderAndAppendTo(@canvas.getElement())

      @rootView.centerInContainer()
      @rootView.refreshDom()
      @rootView.connectChildren()
      @canvas.setRootView(@rootView)
      @canvas.center()
      
      setTimeout( => 
        @minimap.drawMiniNodes @rootView.setChildPositions(), @
      , 500)

      @rootView.getElement().on 'newFoldAction', => setTimeout( =>
        @minimap.drawMiniNodes @rootView.setChildPositions()
      , 500)

      # first part of map is loaded - fadeout
      @$el.parent().find(".loading-map-overlay").fadeOut()

      mapLoader.continueLoading()

      @rootNode


    addLoadingOverlay:->
      div = document.createElement("div")
      loaderGifRoute = jsRoutes.controllers.Assets.at('images/loader-bar.gif').url
      div.className = 'loading-map-overlay'
      $(div).css 
        'background-image'  : "url(#{loaderGifRoute})"
        'background-repeat' : 'no-repeat' 
        'background-attachment' : 'fixed' 
        'background-position'   : 'center' 
      @$el.parent().append div

      wrap = document.createElement("div")
      $(wrap).css
        'text-align': 'center'
        'padding-top': $(div).height()/2 + 20 + 'px'

      link = document.createElement("a")
      link.className = "btn btn-primary btn-medium"
      $(link).attr 'href','/#welcome'
      link.id = "cancel-loading"
      $(link).html "cancel"

      #$(button).append(link)
      $(wrap).append(link)  
      $(div).append(wrap)

      $("#cancel-loading").on 'click', -> 
        document.cancel_loading = true
        document.location.reload(true)
        document.log 'cancel loading'
      $(div).hide()


    renderAndAppendTo:($element, forceFullscreen = true)->
      if $.browser.msie and $.browser.version < 9 then forceFullscreen = false 

      $element.append(@el)
      @render(forceFullscreen)
      @renderSubviews()
      @

    computeHeight:->
      container = @$el.parent().parent()
      verticalMargin = parseFloat(container.css('margin-top'))+parseFloat(container.css('margin-bottom'))
      height = Math.round(window.innerHeight)-@$el.parent().position().top-verticalMargin

    computeWidth:->
      container = @$el.parent().parent()
      mmContainer = @$el.parent()
      horizontalContainerMargin = parseFloat(container.css('margin-left'))+parseFloat(container.css('margin-right'))
      horizontalMmContainerMargin = parseFloat(mmContainer.css('margin-left'))+parseFloat(mmContainer.css('margin-right'))
      width = Math.round(window.innerWidth)-horizontalContainerMargin-horizontalMmContainerMargin

    updateWidthAndHeight:->
      mindmapContainer = @$el.parent()
      container = @$el.parent().parent()

      if @forceFullscreen
        width = @computeWidth()
        height = @computeHeight()
      else
        width = container.width()
        height = container.height()

      container.css
        width:  width+'px'
        height: height+'px'

      mindmapContainer.css
        width:  width+'px'
        height: height+'px'

      @$el.css
        width:  width+'px'
        height: height+'px'

      if typeof @canvas isnt 'undefined'
        @canvas.updateDragBoundaries()


    render:(@forceFullscreen)->
      @$el.parent().fadeIn()
      @updateWidthAndHeight()


    renderSubviews:()->
      $viewport = @$el

      @canvas = new CanvasView("#{@id}_canvas")
      @canvas.renderAndAppendTo($viewport)

      # pass related viewport-element and canvas-view
      @minimap = new MinimapView("#{@id}_minimap-canvas", $viewport, @canvas)
      @minimap.renderAndAppendTo($viewport)

      @zoomPanel = new ZoomPanelView("#{@id}_zoompanel", @canvas)
      @zoomPanel.renderAndAppendTo $viewport

      @addLoadingOverlay()

    renderMap:(mapId)->
      ## called in router
      @loadMap(mapId)


  module.exports = MapView  