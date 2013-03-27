define ['views/RootNodeView', 'views/NodeView', 'views/CanvasView', 'views/MinimapView', 'views/ZoomPanelView', 'models/Node', 'models/RootNode'],  (RootNodeView, NodeView, CanvasView, MinimapView, ZoomPanelView, NodeModel, RootNodeModel) ->  
  module = ->

  class MapView extends Backbone.View

    tagName  : 'div'
    className: 'mindmap-viewport, no-selecting-highlight'


    constructor:(@id)->
      super()
      $(window).on('resize', @resizeViewport)

    positionNodes:()->

      @rootView = new RootNodeView @rootNode

      # create and append new html 
      @$rootHtml = $(@rootView.render().el).html()
      @canvas.getElement().append @$rootHtml      
      @rootView.alignControls @rootView.model, true

      @rootView.refreshDom()
      
      @rootView.connectChildren()
      
      @rootView.centerInContainer()
      
      @canvas.setRootView(@rootView)
      

    resizeViewport:=>
      console.log 'resize'

      @updateWidthAndHeight()
      if typeof @minimap != 'undefined'
        @minimap.setViewportSize()



    loadMap: (@mapId) ->
      if typeof @rootView != 'undefined'
        # remove old html elements
        @rootView.getElement().remove();
      console.log "call: loadMap #{mapId} (MapController)"
      @href = jsRoutes.controllers.MindMap.map(@mapId).url
      @$el.parent().find(".loading-map-overlay").fadeIn(400, =>
        $.get(@href, @createJSONMap, "json")
      )
      

    createJSONMap: (data)=>
      $('#current-mindmap-name').text(data.name)
      #id, folded, nodeText, containerID, isHTML, xPos, yPos, hGap, shiftY, locked
      @rootNode = new RootNodeModel(data.root.id, false, data.root.nodeText, "#{@id}_canvas" ,data.root.isHtml, 0,0,0,0,false,@mapId) 
      document.rootID = data.root.id
      if data.root.leftChildren != undefined
        leftNodes = getRecursiveChildren(data.root.leftChildren, @rootNode, @rootNode)
        @rootNode.set 'leftChildren', leftNodes
      
      if data.root.rightChildren != undefined
        rightNodes = getRecursiveChildren(data.root.rightChildren, @rootNode, @rootNode)
        @rootNode.set 'rightChildren', rightNodes

      @positionNodes()
      
      @canvas.center()
      
      setTimeout( => 
        @minimap.drawMiniNodes @rootView.setChildPositions(), true
      , 500)

      @rootView.getElement().on 'newFoldAction', => setTimeout( => 
        @minimap.drawMiniNodes @rootView.setChildPositions()
      , 500)
      @$el.parent().find(".loading-map-overlay").fadeOut()
      @rootNode


    getRecursiveChildren = (childrenData, parent, root)->
      children = []
      if childrenData.id != undefined && childrenData.id != null
        #id, folded, nodeText, isHTML, xPos, yPos, hGap, shiftY, locked
        newChild = new NodeModel(childrenData.id, childrenData.folded, childrenData.nodeText, childrenData.isHtml,0,0,0,0,false, parent, root)
        children.push newChild
      else if childrenData != undefined
        for child in childrenData
          if child.nodeText != ""
            newChild = new NodeModel(child.id, child.folded, child.nodeText, child.isHtml,0,0,0,0,false, parent, root)
            if child.children != undefined
              newChild.set 'children', getRecursiveChildren(child.children, newChild, root)
            children.push newChild
      children

    toggleLoadingOverlay:->

    addLoadingOverlay:->
      div = document.createElement("div")
      loaderGifRoute = jsRoutes.controllers.Assets.at('images/loader-bar.gif').url
      div.className = 'loading-map-overlay'
      $(div).css 
        'background-image'  : "url(#{loaderGifRoute})"
        'background-repeat' : 'no-repeat' 
        'background-attachment' : 'fixed' 
        'background-position'   : 'center' 

      $(div).hide()
      @$el.parent().append div


    renderAndAppendTo:($element, forceFullscreen = true)->
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

      @canvas.center()

      @addLoadingOverlay()

    renderMap:(mapId)->
      ## called in router
      @loadMap(mapId)


  module.exports = MapView  