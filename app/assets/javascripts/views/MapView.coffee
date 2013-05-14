define ['logger', 'MapLoader', 'views/RootNodeView', 'views/NodeView', 'views/CanvasView', 'views/MinimapView', 'views/ZoomPanelView', 'models/Node', 'models/RootNode'],  (logger, MapLoader, RootNodeView, NodeView, CanvasView, MinimapView, ZoomPanelView, NodeModel, RootNodeModel) ->  
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
      @href = jsRoutes.controllers.MindMap.mapAsJson(@mapId).url

      $.ajax(
        url: @href
        success: @initMapLoading
        error: @showMapLoadingError
        dataType: "json"
      )
      

    showMapLoadingError:(a,b,c)=>
      # if answere doesn't contain redirect, show error message
      if a.responseText.indexOf("<head>") is -1
        alert a.responseText
      # otherwise redirect to welcome map
      else
        @loadMap 'welcome'


    initMapLoading:(data)=>
      if @rootView isnt undefined
        document.log 'delete old map'
        @canvas.zoomCenter(false)
        @rootView.getElement().remove()

      document.log "call: loadMap #{data.id} (MapController)"     

      @$el.parent().find(".loading-map-overlay").fadeIn 400, =>
        @parseAndRenderMapByJsonData(data)


    parseAndRenderMapByJsonData: (data)=>
      $('.current-mindmap-name').text(data.name)


      mapLoader = new MapLoader data, @mapId

      @rootView = new RootNodeView mapLoader.firstLoad()
      document.rootView = @rootView
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

      @rootView.getElement().on 'updateMinimap', => setTimeout( => 
        @minimap.drawMiniNodes @rootView.setChildPositions()
      , 500)

      # first part of map is loaded - fadeout
      @$el.parent().find(".loading-map-overlay").fadeOut()
      document.initialLoad = true
      mapLoader.continueLoading()
      @rootView.model.on 'refreshSize', @refreshMiniNodes



    refreshMiniNodes:=>
      @canvas.checkBoundaries()
      @minimap.drawMiniNodes @rootView.setChildPositions()

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