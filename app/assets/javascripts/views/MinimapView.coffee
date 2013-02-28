define ->
  module = () ->

  class Minimap extends Backbone.View
  
    tagName  : 'div'
    className: 'minimap-canvas'
    template : Handlebars.templates['Minimap']

    events:
      'click': -> @updatePositionClick(event)


    constructor:(@id, @relatedViewport, @relatedCanvasView)->
      super()
      @relatedCanvas = @relatedCanvasView.getElement()
      @relatedCanvas.on 'drag', @updatePosition
      @relatedCanvas.on 'center', @updatePosition

    element:-> @$el


    afterAppend:()->
     minimapViewport =  @$el.find('.minimap-viewport')
     minimapViewport.draggable
        cancel: "a.ui-icon, .node"
        containment: "parent"
        cursor: "move"
        drag: (event, ui)=>
          @updateRelatedCanvasPositionDrag(event, ui)

      minimapViewport.hover (e)=> $(e.currentTarget).toggleClass('highlight')


    draggable:->
      @$el.draggable
        cancel:      "a.ui-icon, .node"
        containment: "parent"
        cursor:      "move"


    renderAndAppendTo:($element, @itsDraggable = false)->
      stats = 
        width:  @relatedViewport.width() / 70
        height: @relatedViewport.height() / 70
        left: 0
        top:  0
        viewport_class: 'minimap-viewport'

      @$el.html @template stats
      $element.append(@el)
      @draggable() if @itsDraggable

      @$el.css 
        'width'    : @relatedCanvas.width() / 70
        'height'   : @relatedCanvas.height() / 70
      @afterAppend()
      @


    updateRelatedCanvasPositionDrag:(event, ui)->
      # position of minimap viewport in % (is set to px value due drag :P)
      xPos = @relatedCanvas.width()  * (ui.position.left / @$el.width()  * 100) / 100  
      yPos = @relatedCanvas.height() * (ui.position.top  / @$el.height() * 100) / 100  
      @updateRelatedCanvasPosition(xPos, yPos)     


    updateRelatedCanvasPosition:(xPos, yPos)->
      @relatedCanvas.css
        'left'  : "#{-xPos}px",
        'top'   : "#{-yPos}px"


    updatePositionClick:(event)->
      $minimapViewport = @$el.find('.minimap-viewport')
      mouseX = event.pageX - @$el.offset().left
      mouseY = event.pageY - @$el.offset().top

      xPos = @relatedCanvas.width()  * ((mouseX - $minimapViewport.width() / 2) / @$el.width()  * 100) / 100  
      yPos = @relatedCanvas.height() * ((mouseY - $minimapViewport.height() / 2) / @$el.height() * 100) / 100        
      @updateRelatedCanvasPosition(xPos, yPos)

      xPosMini = mouseX - $minimapViewport.width()/2
      yPosMini = mouseY - $minimapViewport.height()/2
      $minimapViewport.css
        'left' : "#{(xPosMini/@$el.width()*100)}%"
        'top'  : "#{(yPosMini/@$el.height()*100)}%" 


    updatePosition:=>
      $minimapViewport = @$el.find('.minimap-viewport')
      
      posX = ((parseFloat(@relatedCanvas.css('left'))  + @relatedCanvas.width() ) / @relatedCanvas.width()  ) * 100
      posY = ((parseFloat(@relatedCanvas.css('top'))   + @relatedCanvas.height()) / @relatedCanvas.height() ) * 100
      
      $minimapViewport.css
        'left' : "#{-(posX-100)}%"
        'top'  : "#{-(posY-100)}%"      

  module.exports = Minimap