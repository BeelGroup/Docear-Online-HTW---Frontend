define ['models/Node', 'models/RootNode'],  (NodeModel, RootNodeModel) ->  
  module = ->

  class ConnectionView extends Backbone.View

    tagName  : 'div'
    className: 'connection'

    constructor: (@sourceModel, @targetModel) ->
      super()
      @targetModel.bind "change:connectionUpdated",@repaintConnection , @
      

    repaintConnection: ()->
      @$sourceNode = $('#'+@sourceModel.get('id'))
      @$targetNode = $('#'+@targetModel.get('id'))
      @isRight = $(@$targetNode).hasClass('right')
      
      @calculateEndpoints()
      @positionContainer()
      @drawConnection()
      
    
      
    getCurrentZoomAmount: ()->
      zoom = document.currentZoom
      if $.browser.msie and $.browser.version < 9
        zoom = 1
      return zoom
      
    getModelBoundingBox: ($node, zoom = 1)->
      modelBB =
        x :  $($node).offset().left/zoom
        y : $($node).offset().top/zoom
        width : $($node).outerWidth()
        height : $($node).outerHeight()

    calculateEndpoints: ()->
      strokeWidth = document.graph.defaultWidth
      zoom = @getCurrentZoomAmount()
      
      sourceBB = @getModelBoundingBox @$sourceNode, zoom
      targetBB = @getModelBoundingBox @$targetNode, zoom

      @sourceEndpoint = 
        x : 0
        y : sourceBB.height/2
      @targetEndpoint = 
        x : 0
        y : targetBB.height/2
      
      
      @absoluteSourceEndpoint =
        x : sourceBB.x
        y : sourceBB.y+@sourceEndpoint.y
      @absoluteTargetEndpoint =
        x : targetBB.x
        y : targetBB.y+@targetEndpoint.y

          
      @connectionContainer = 
        x : 0 
        y : 0 
        width : 0 
        height : 0
        
      @connection = 
        startX : 0
        startY : strokeWidth
        endX : 0
        endY : strokeWidth
        
      
      isTop = @absoluteSourceEndpoint.y > @absoluteTargetEndpoint.y
      
      if @isRight
        @sourceEndpoint.x = sourceBB.width
        @absoluteSourceEndpoint.x = sourceBB.x + sourceBB.width
        @connectionContainer.x = @absoluteSourceEndpoint.x - @absoluteTargetEndpoint.x
        
        # top right
        if isTop
          @connectionContainer.y = @targetEndpoint.y
        else
          @connectionContainer.y = @absoluteSourceEndpoint.y - @absoluteTargetEndpoint.y + @targetEndpoint.y
      else
        @targetEndpoint.x = targetBB.width
        @absoluteTargetEndpoint.x = targetBB.x + targetBB.width
        @connectionContainer.x = @targetEndpoint.x
        
        # top left
        if isTop
          @connectionContainer.y = @targetEndpoint.y
        else
          @connectionContainer.y = @absoluteSourceEndpoint.y - @absoluteTargetEndpoint.y + @targetEndpoint.y

      @connectionContainer.y -= strokeWidth
          
      @connectionContainer.width = Math.abs(@absoluteSourceEndpoint.x - @absoluteTargetEndpoint.x)
      @connectionContainer.height = Math.abs(@absoluteSourceEndpoint.y - @absoluteTargetEndpoint.y) + strokeWidth

      @connection.endX = @connectionContainer.width
      if (@isRight and !isTop) or (!@isRight and isTop)
        @connection.endY = @connectionContainer.height - strokeWidth
      else
        @connection.startY = @connectionContainer.height - strokeWidth

    positionContainer:()->
      $connectionContainer = $(@$targetNode).children('.connection:first')
      if @isRight
        $($connectionContainer).css(  'left', """#{@connectionContainer.x}px""")
      else
        $($connectionContainer).css(  'right', """-#{@connectionContainer.width}px""")
      $($connectionContainer).css(   'top', """#{@connectionContainer.y}px""")
      $($connectionContainer).css( 'width', """#{@connectionContainer.width}px""")
      $($connectionContainer).css( 'height', """#{Math.max(@connectionContainer.height, 15)}px""")
      
    drawConnection: ()->
      $connectionContainer = $(@$targetNode).children('.connection:first')
      strokeWidth = document.graph.defaultWidth
      strokeColor = document.graph.defaultColor
      
      middleX = @connection.endX/2
      isTop = @absoluteSourceEndpoint.y > @absoluteTargetEndpoint.y
      if (@isRight and !isTop) or (!@isRight and isTop)
        middleY = @connection.endY/2
        control1Y = @connection.startY + Math.min(middleX, middleY)
        control2Y = @connection.endY - Math.min(middleX, middleY)
      else
        middleY = @connection.startY/2
        control1Y = @connection.startY - Math.min(middleX, middleY)
        control2Y = @connection.endY + Math.min(middleX, middleY)
      
      if Raphael.svg

        $($connectionContainer).svg('destroy');
        $($connectionContainer).svg()
        svg = $($connectionContainer).svg('get'); 
        path = svg.createPath();
        
        bezierControls = [[middleX, @connection.startY, middleX, @connection.startY, middleX, control1Y],[middleX, control1Y, middleX, control2Y, middleX, control2Y],[middleX, @connection.endY, middleX, @connection.endY, @connection.endX, @connection.endY]]
        pathNode = svg.path(null, path.move(@connection.startX, @connection.startY).curveC(bezierControls), {fill: 'none', stroke: strokeColor, strokeWidth: strokeWidth}, true)
      else
        $($connectionContainer).empty()
        pathString = """M#{@connection.startX},#{@connection.startY}C#{middleX},#{@connection.startY} #{middleX},#{@connection.startY} #{middleX},#{control1Y} #{middleX},#{control1Y} #{middleX},#{control2Y} #{middleX},#{control2Y} #{middleX},#{@connection.endY} #{middleX},#{@connection.endY} #{@connection.endX},#{@connection.endY}"""
        
        # even if size of $connectionContainer is 1 DOM element must be passed via ".get(0)"
        paper = Raphael($($connectionContainer).get(0), @connectionContainer.width, @connectionContainer.height)
        pathNode = paper.path(pathString).attr({
          "stroke" : strokeColor
          "stroke-width" : strokeWidth
        })
    
    
    renderAndAppendToNode:($target)->
      $($target).append(@render().el)
      @

  module.exports = ConnectionView  