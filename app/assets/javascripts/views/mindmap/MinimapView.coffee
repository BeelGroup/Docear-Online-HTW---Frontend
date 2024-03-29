define ->
  module = () ->

  class Minimap extends Backbone.View
  
    tagName  : 'div'
    className: 'minimap-canvas'
    template : Handlebars.templates['Minimap']

    events:
      'click': (event)-> @updatePositionClick(event)


    constructor:(@id, @relatedViewport, @relatedCanvasView, @maxWidth = 200.0)->
      super()
      @scale = 1
      @relatedCanvas = @relatedCanvasView.getElement()
      # for chrome, there is an individual eventhandling (see @ CanvasView.afterAppend <- throws 'canvasWasMovedTo')
      if !$.browser.chrome 
        @relatedCanvas.on 'drag', @updatePositionOnCanvasDrag
      else
        @relatedCanvas.on 'dragging', @updatePositionOnCanvasDrag
      @relatedCanvas.on 'canvasWasMovedTo', @updatePositionFromCanvas
      @relatedCanvas.on 'zoom', @resizeMiniViewport
      @relatedCanvas.on 'resize', @resize
      @minimapViewportWidthOffset  = 0.0
      @minimapViewportHeightOffset = 0.0
      @lastScaleAmount = 1
      @currentScale = 100
      @scaleAmount = 1.0

      @ratio = @relatedCanvas.width() / @maxWidth


    element:-> @$el


    resize:()=>
      @ratio = @relatedCanvas.width() / @maxWidth
      @setCanvasSize()
      @setViewportSize()


    setCanvasSize:->
      @width = Math.round(@relatedCanvas.width() / @ratio)
      @height = Math.round(@relatedCanvas.height() / @ratio)

      @$el.css 
        'width'    : @width
        'height'   : @height


    setViewportSize:->
      @minimapViewportOriginWidth = Math.round(@relatedViewport.width() / @ratio)
      @minimapViewportOriginHeight = Math.round(@relatedViewport.height() / @ratio)
      @minimapViewport.css
        'width' : @minimapViewportOriginWidth
        'height': @minimapViewportOriginHeight


    resizeMiniViewport:(event, @scaleAmount, duration = 100)=>
      possibilities = document.body.style
      fallback = false
      element = @$root

      # IE
      if $.browser.msie 
        if $.browser.version > 8
          #document.log 'IE 9 & 10'
          element.css
            '-ms-transform': "scale(#{@scaleAmount})" 

        else if $.browser.version <= 8 
          #document.log 'IE 7 & 8'
          fallback = true

      # Safari, Firefox and Chrome with CSS3 support 
      else if($.inArray('WebkitTransform', possibilities) or 
      $.inArray('MozTransform', inpossibilities) or 
      $.inArray('OTransform', possibilities)) 
        #document.log 'Webkit, Moz, O'
        element.animate {'scale' : @scaleAmount}, duration

      else
        #document.log $.browser
        fallback = true 

      # ultra fallback
      #if fallback
        #scaleDiff = 0
        #if amount > @lastScaleAmount then scaleDiff = 25 else scaleDiff = -25
        #element.parent().effect 'scale', {percent: 100 + scaleDiff, origin: ['middle','center']}, 1
        #@lastScaleAmount = amount
        #@currentScale += scaleDiff
      

    clear:->
      $.each $('.mini-node'), -> 
        $(@).remove()


    drawMiniNodes:(@nodePositions, firstDraw = false)->
      if firstDraw 
        @scaleAmount = 1.0

      @miniNodesContainer = @$el.find('.mini-nodes-container')

      @clear()

      @$root = @createMiniNode @nodePositions, @miniNodesContainer
 
      @createMiniNode stats, @$root for stats in @nodePositions.leftChilds
      @createMiniNode stats, @$root for stats in @nodePositions.rightChilds

      @resizeMiniViewport(null, @scaleAmount, 0)


    createMiniNode:(stats, $container)->
      width  = stats.width / @ratio
      width = if width > 1.0 then width else 1
      height = stats.height / @ratio
      height = if height > 1.0 then height else 1

      div = document.createElement("div")
      div.className = 'mini-node'
      div.style.width  = width  + "px"
      div.style.height = height + "px"
      div.style.left = stats.pos.left / @ratio + 'px'
      div.style.top  = stats.pos.top  / @ratio + 'px'

      $container.append div
      $(div)

     
    afterAppend:()->
      @minimapViewport =  @$el.find('.minimap-viewport')
      @minimapViewport.draggable
        cancel: "a.ui-icon, .node"
        containment: "parent"
        cursor: "move"
        drag: (event, ui)=>
          @updateRelatedCanvasPositionOnDrag(event, ui)

      @minimapViewport.hover (e)=> $(e.currentTarget).toggleClass('highlight')
      @minimapViewport.css 'opacity', '.6'


    draggable:->
      @$el.draggable
        cancel:      "a.ui-icon, .inner-node, :input"
        containment: "parent"
        cursor:      "move"


    renderAndAppendTo:($element, @itsDraggable = false)->
      @minimapViewportOriginWidth = Math.round(@relatedViewport.width() / @ratio)
      @minimapViewportOriginHeight = Math.round(@relatedViewport.height() / @ratio)

      stats = 
        width:  @minimapViewportOriginWidth
        height: @minimapViewportOriginHeight
        left: 0
        top:  0
        viewport_class: 'minimap-viewport'

      @$el.html @template stats
      @$el.css('opacity','.6');
      $element.append(@el)
      @draggable() if @itsDraggable

      @setCanvasSize()

      @afterAppend()
      @


    updateRelatedCanvasPositionOnDrag:(event, ui)->
      xPos = (ui.position.left - @minimapViewportWidthOffset) * @ratio
      yPos = (ui.position.top - @minimapViewportHeightOffset) * @ratio
      @updateRelatedCanvasPosition(xPos, yPos)     


    updateRelatedCanvasPosition:(xPos, yPos, animate = false)->
      stats =         
        'left'  : "#{-xPos}px",
        'top'   : "#{-yPos}px"

      if animate then @relatedCanvas.stop().animate stats else @relatedCanvas.css stats

    updatePositionFromCanvas:(event, stats)=>
      resizedPos= 
        x: if stats.position.x is false then false else -stats.position.x /@ratio
        y: if stats.position.y is false then false else -stats.position.y /@ratio
      @updatePosition(resizedPos, stats.animated)

    updatePositionClick:(event)->
      $minimapViewport = @$el.find('.minimap-viewport')
      mouseX = event.pageX - @$el.offset().left
      mouseY = event.pageY - @$el.offset().top

      halfViewportOuterWidth  = $minimapViewport.outerWidth()/2
      halfViewportWidth  = $minimapViewport.width()/2
      halfViewportOuterHeight = $minimapViewport.outerHeight()/2
      halfViewportHeight = $minimapViewport.height()/2

      if mouseX < halfViewportOuterWidth
        mouseX = halfViewportWidth
      else if mouseX > (@$el.width() - halfViewportOuterWidth)
        mouseX = @$el.width() - halfViewportOuterWidth

      if mouseY < halfViewportOuterHeight
        mouseY = halfViewportHeight
      else if mouseY > @$el.height() - halfViewportOuterHeight
        mouseY = @$el.height() - halfViewportOuterHeight

      xPos = @relatedCanvas.width()  * ((mouseX - $minimapViewport.width() / 2) / @$el.width()  * 100) / 100  
      yPos = @relatedCanvas.height() * ((mouseY - $minimapViewport.height() / 2) / @$el.height() * 100) / 100        
      @updateRelatedCanvasPosition(xPos, yPos, true)

      xPosMini = mouseX - $minimapViewport.width()/2
      yPosMini = mouseY - $minimapViewport.height()/2

      pos =
        x: xPosMini
        y: yPosMini

      @updatePosition pos, true


    updatePositionOnCanvasDrag:=>
      posX = ((parseFloat(@relatedCanvas.css('left'))  + @relatedCanvas.width() ) / @relatedCanvas.width()  ) 
      posY = ((parseFloat(@relatedCanvas.css('top'))   + @relatedCanvas.height()) / @relatedCanvas.height() ) 
      pos=
        x: (-posX  + 1)*@$el.width() 
        y: (-posY  + 1)*@$el.height() 

      @updatePosition pos

    updatePosition:(pos, animated = false)->
      $minimapViewport = @$el.find('.minimap-viewport')

      stats =  new Object()
      if pos.x isnt false
        stats.left = "#{pos.x}px"
      if pos.y isnt false
        stats.top = "#{pos.y}px"   

      if animated then $minimapViewport.stop().animate stats else $minimapViewport.css stats

  module.exports = Minimap