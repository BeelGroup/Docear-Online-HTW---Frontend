define ['logger', 'MapLoader', 'views/mindmap/RootNodeView', 'views/mindmap/NodeView', 'views/mindmap/CanvasView', 'views/mindmap/MinimapView', 'views/mindmap/ZoomPanelView', 'models/mindmap/Node', 'models/mindmap/RootNode', 'collections/workspace/Workspace', 'views/workspace/WorkspaceView', 'handlers/WorkspaceUpdateHandler'],  (logger, MapLoader, RootNodeView, NodeView, CanvasView, MinimapView, ZoomPanelView, NodeModel, RootNodeModel, Workspace, WorkspaceView, WorkspaceUpdateHandler) ->  
  module = ->

  class MapView extends Backbone.View

    tagName  : 'div'
    className: 'mindmap-viewport, no-selecting-highlight'


    constructor:(@id)->
      super()
      $(window).on('resize', => 
        @resizeWorkspace(@resizeViewport())
        )


    resizeViewport:=>
      computetStats = @updateWidthAndHeight()
      if typeof @minimap != 'undefined'
        @minimap.setViewportSize()
      computetStats

    resizeWorkspace:(widthAndHeight)=>
      @workspaceView.resize(widthAndHeight)


    loadMap: (@projectId, @mapId) ->
      if @mapLoader isnt undefined
        @mapLoader.stop()
      document.log "Load map #{@mapId} from project #{@projectId} (Mapview.loadMap())" 
      @href = jsRoutes.controllers.MindMap.mapAsJson(@projectId, encodeURIComponent(@mapId), document.initialLoadChunkSize).url

      $.ajax(
        url: @href
        success: @initMapLoading
        error: @showMapLoadingError
        dataType: "json"
      )
    
    closeMap: (mapId = @mapId)->
      document.log "Close map #{@mapId}"
      @minimap.clear()
      if @mapLoader isnt undefined
        @mapLoader.stop()
      if @rootView isnt undefined
        $warning = $(@$el).find('.mindmap-closed-warning')
        if $warning.size() == 0
          $warning = $(@$el).parent().find('.mindmap-closed-warning')
          $(@$el).append($warning)
        $warning.slideDown()

        @canvas.zoomCenter(false)
        @rootView.getElement().remove()
        @rootView.destroy()
        $('.current-mindmap-name').text('')
        
        # close edit view if opend (defined in main.coffee)
        $editNodeContainer = $('.node-edit-container')
        $editNodeContainer.addClass('close-and-destroy').hide() 

    showMapLoadingError:(a)=>
      # if answere doesn't contain redirect, show error message
      if a.responseText.indexOf("<head>") is -1
        result = $.parseJSON( a.responseText );
        $warning = $("#mindmap-container").find('.server-warning')

        $warning.find(".message.type").html result.type + ': '
        $warning.find(".message.content").html result.message
        $warning.slideDown()
      # otherwise redirect to welcome map
      else
        @loadMap '-1', 'welcome'


    initMapLoading:(data)=>
      if @rootView isnt undefined
        document.log 'delete old map'
        @canvas.zoomCenter(false)
        @rootView.getElement().remove()
        
        # close edit view if opend (defined in main.coffee)
        $editNodeContainer = $('.node-edit-container')
        $editNodeContainer.addClass('close-and-destroy').hide()    

      @$el.parent().find(".loading-map-overlay").fadeIn 400, =>
        @parseAndRenderMapByJsonData(data)


    parseAndRenderMapByJsonData: (data)=>
      mapId = @mapId.substring(@mapId.lastIndexOf('/')+1);
      $('.current-mindmap-name').text(mapId)
     
      @mapLoader = new MapLoader data, @projectId, @mapId

      @rootView = new RootNodeView @mapLoader.firstLoad()
      document.rootView = @rootView
      @mapLoader.injectRootView @rootView
      @rootView.renderAndAppendTo(@canvas.getElement())

      @rootView.centerInContainer()
      @rootView.refreshDom()
      @rootView.getModel().updateAllConnections()
      @canvas.setRootView(@rootView)
      @canvas.center()

      setTimeout( => 
        @minimap.drawMiniNodes @rootView.setChildPositions(), @
      , 500)

      @rootView.getElement().on 'updateMinimap', => setTimeout( => 
        @minimap.drawMiniNodes @rootView.setChildPositions()
      , 500)

      @rootView.model.on 'updateMinimap', => setTimeout( => 
        @minimap.drawMiniNodes @rootView.setChildPositions()
      , 500)


      # first part of map is loaded - fadeout
      @$el.parent().find(".loading-map-overlay").fadeOut()
      document.initialLoad = true
      @mapLoader.continueLoading()
      @rootView.model.on 'refreshSize', @refreshSizeOfCanvasAndMinimap
      @rootView.model.on 'refreshMinimap', @minimap.drawMiniNodes @rootView.setChildPositions()
      @rootView.model.on 'refreshDomConnectionsAndBoundaries', @refreshDomConnectionsAndBoundaries


    refreshDomConnectionsAndBoundaries: (side = null)=>
      document.log 'refresh'
      @rootView.refreshDom(side)
      @canvas.checkBoundaries()
      @minimap.drawMiniNodes @rootView.setChildPositions()
      @rootView.getModel().updateAllConnections()

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
      height

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

      if height <= 0
        height = $(mindmapContainer).outerHeight()
      
      stats=
        width:  width+'px'
        height: height+'px'
      plainStats=
        width:  width
        height: height
        

      container.css stats
      workspaceWidth = $('#workspace-container').outerWidth()
      mindMapStats=
        width:  (width-workspaceWidth)+'px'
        height: height+'px'

      mindmapContainer.css mindMapStats
      @$el.css mindMapStats

      if typeof @canvas isnt 'undefined'
        @canvas.updateDragBoundaries()

      plainStats

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

      if $.inArray('WORKSPACE', document.features) and $('body').hasClass('is-authenticated')
        @workspace = new Workspace()
        @workspaceView = new WorkspaceView(@workspace);
        $('#mindmap-container').before(@workspaceView.render().element())
        callback = =>
          @workspaceUpdateHandler = new WorkspaceUpdateHandler(@workspace)
          @workspaceUpdateHandler.listen()
        @workspace.loadAllUserProjects(callback)
        
        @resizeWorkspace(@resizeViewport())

      @addLoadingOverlay()



  module.exports = MapView  