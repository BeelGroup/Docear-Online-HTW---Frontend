define ['routers/DocearRouter', 'views/RootNodeView', 'views/NodeView', 'views/HtmlView', 'views/CanvasView', 'views/MinimapView', 'views/ZoomPanelView', 'models/Node', 'models/RootNode'],  (DocearRouter, RootNodeView, NodeView, HtmlView, CanvasView, MinimapView, ZoomPanelView, NodeModel,RootNodeModel) ->  
  module = ->

  class MapView extends Backbone.View

    tagName  : 'div'
    className: 'mindmap-viewport'


    constructor:(@id)->


    positionNodes:()->
      jsPlumb.reset()
      @rootView = new RootNodeView @rootNode
      
      # remove old html elements
      @rootView.getElement().remove();
      # create and append new html 
      @$rootHtml = $(@rootView.render().el).html()
      @canvas.getElement().append @$rootHtml      
      @rootView.connectChildren()
      @rootView.centerInContainer()
      @rootView.refreshDom()
      jsPlumb.repaintEverything()
      @minimap.updatePosition()
      @canvas.setRootView(@rootView)


    loadMap: (mapId) ->
      console.log "call: loadMap #{mapId} (MapController)"
      href = jsRoutes.controllers.MindMap.map(mapId).url
      $.get(href, @createJSONMap, "json")
      

    createJSONMap: (data)=>
      #id, folded, nodeText, containerID, isHTML, xPos, yPos, hGap, shiftY, locked
      @rootNode = new RootNodeModel(data.root.id, false, data.root.nodeText, document.canvasID ,data.root.isHtml, 0,0,0,0,false) 
      document.rootID = data.root.id
      if data.root.leftChildren != undefined
        leftNodes = getRecursiveChildren(data.root.leftChildren, @rootNode)
        @rootNode.set 'leftChildren', leftNodes
      
      if data.root.rightChildren != undefined
        rightNodes = getRecursiveChildren(data.root.rightChildren, @rootNode)
        @rootNode.set 'rightChildren', rightNodes

      @positionNodes()
      @canvas.center()
      @rootNode


    getRecursiveChildren = (childrenData, parent)->
      children = []
      if childrenData.id != undefined && childrenData.id != null
        #id, folded, nodeText, isHTML, xPos, yPos, hGap, shiftY, locked
        newChild = new NodeModel(childrenData.id, childrenData.folded, childrenData.nodeText, childrenData.isHtml,0,0,0,0,false, parent)
        children.push newChild
      else if childrenData != undefined
        for child in childrenData
          if child.nodeText != ""
            newChild = new NodeModel(child.id, child.folded, child.nodeText, child.isHtml,0,0,0,0,false, parent)
            if child.children != undefined
              newChild.set 'children', getRecursiveChildren(child.children, newChild)
            children.push newChild
      children


    renderAndAppendTo:($element)->
      #$element.append(@el)
      @renderSubviews()
      #@afterAppend()
      @


    renderSubviews:()->
      $viewport = $("##{@id}")

      @canvas = new CanvasView(document.canvasID)
      @canvas.renderAndAppendTo($viewport)

      # pass related viewport-element and canvas-view
      @minimap = new MinimapView(document.minimapCanvasID, $viewport, @canvas)
      @minimap.renderAndAppendTo($viewport, true)

      @zoomPanel = new ZoomPanelView('zoomPanel', @canvas)
      @zoomPanel.renderAndAppendTo $viewport


    renderMap:(mapId)->
      ## called in router
      @loadMap(mapId)


  module.exports = MapView  