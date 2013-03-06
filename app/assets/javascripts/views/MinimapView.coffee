define ->
  module = () ->

  class Minimap extends Backbone.View
  
    tagName  : 'div'
    className: 'minimap-canvas'
    template : Handlebars.templates['Minimap']

    events:
      'click': (event)-> @updatePositionClick(event)


    constructor:(@id, @relatedViewport, @relatedCanvasView, @ratio = 70)->
      super()
      @relatedCanvas = @relatedCanvasView.getElement()
      @relatedCanvas.on 'drag', @updatePositionEvent
      @relatedCanvas.on 'center', @centerPosition

    element:-> @$el

    drawMiniNodes:(nodePositions)->
      $.each $('.mini-node'), -> @remove()
      @createMiniNode stats for stats in nodePositions

    createMiniNode:(stats)->
      width  = stats.width / @ratio
      width = if width > 1.0 then width else 1
      height = stats.height / @ratio
      height = if height > 1.0 then height else 1

      div = document.createElement("div")
      div.style.position = "absolute"
      div.style.width  = width + "px"
      div.style.height = height + "px"
      div.style.left = stats.pos.left / @ratio  + 'px'
      div.style.top  = stats.pos.top  / @ratio  + 'px'
      div.className = 'mini-node'
      div.style.background = "green"

      #@$el.append div

     
    afterAppend:()->
      minimapViewport =  @$el.find('.minimap-viewport')
      minimapViewport.draggable
        cancel: "a.ui-icon, .node"
        containment: "parent"
        cursor: "move"
        drag: (event, ui)=>
          @updateRelatedCanvasPositionDrag(event, ui)

      minimapViewport.hover (e)=> $(e.currentTarget).toggleClass('highlight')
      minimapViewport.css('opacity','.6');


    draggable:->
      @$el.draggable
        cancel:      "a.ui-icon, .inner-node, :input"
        containment: "parent"
        cursor:      "move"


    renderAndAppendTo:($element, @itsDraggable = false)->
      stats = 
        width:  @relatedViewport.width() / @ratio
        height: @relatedViewport.height() / @ratio
        left: 0
        top:  0
        viewport_class: 'minimap-viewport'

      @$el.html @template stats
      @$el.css('opacity','.6');
      $element.append(@el)
      @draggable() if @itsDraggable

      @$el.css 
        'width'    : @relatedCanvas.width() / @ratio
        'height'   : @relatedCanvas.height() / @ratio
      @afterAppend()
      @


    updateRelatedCanvasPositionDrag:(event, ui)->
      # position of minimap viewport in % (is set to px value due drag :P)
      xPos = @relatedCanvas.width()  * (ui.position.left / @$el.width()  * 100) / 100  
      yPos = @relatedCanvas.height() * (ui.position.top  / @$el.height() * 100) / 100  
      @updateRelatedCanvasPosition(xPos, yPos)     


    updateRelatedCanvasPosition:(xPos, yPos, animate = false)->
      stats =         
        'left'  : "#{-xPos}px",
        'top'   : "#{-yPos}px"

      if animate then @relatedCanvas.animate stats else @relatedCanvas.css stats


    updatePositionClick:(event)->
      $minimapViewport = @$el.find('.minimap-viewport')
      mouseX = event.pageX - @$el.offset().left
      mouseY = event.pageY - @$el.offset().top
      #console.log event
      xPos = @relatedCanvas.width()  * ((mouseX - $minimapViewport.width() / 2) / @$el.width()  * 100) / 100  
      yPos = @relatedCanvas.height() * ((mouseY - $minimapViewport.height() / 2) / @$el.height() * 100) / 100        
      @updateRelatedCanvasPosition(xPos, yPos, true)

      xPosMini = mouseX - $minimapViewport.width()/2
      yPosMini = mouseY - $minimapViewport.height()/2

      pos =
        x: (xPosMini/@$el.width()*100)
        y: (yPosMini/@$el.height()*100)

      @updatePosition pos, true


    updatePositionEvent:=>
      posX = ((parseFloat(@relatedCanvas.css('left'))  + @relatedCanvas.width() ) / @relatedCanvas.width()  ) * 100
      posY = ((parseFloat(@relatedCanvas.css('top'))   + @relatedCanvas.height()) / @relatedCanvas.height() ) * 100

      pos=
        x: -posX + 100
        y: -posY + 100

      @updatePosition pos

    updatePosition:(pos, animated = false)->
      $minimapViewport = @$el.find('.minimap-viewport')

      stats=
        'left' : "#{pos.x}%"
        'top'  : "#{pos.y}%"   

      if animated then $minimapViewport.animate stats else $minimapViewport.css stats

    computeCenterPosition:->
      $minimapViewport = @$el.find('.minimap-viewport')
      x = (@$el.width() / 2 - $minimapViewport.width() / 2) / @$el.width() * 100
      y = (@$el.height() / 2 - $minimapViewport.height() / 2) / @$el.height() * 100

      pos = x: x, y: y


    centerPosition:(animate = false)=>
      @updatePosition @computeCenterPosition(), animate

  module.exports = Minimap