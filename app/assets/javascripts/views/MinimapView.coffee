define ->
  module = () ->

  class Minimap extends Backbone.View
  
    id       : document.minimapCanvasID
    tagName  : 'div'
    className: 'minimap-canvas'
    template : Handlebars.templates['Minimap']


    constructor:(@relatedViewport, @relatedCanvasView)->
      super()
      @relatedCanvas = @relatedCanvasView.element()
      @relatedCanvasView.addDragDependency(@updatePosition)


    element:-> @$el


    afterAppend:()->
      @$el.find(".#{document.minimapViewportCN}").draggable
        cancel: "a.ui-icon, .node"
        containment: "parent"
        cursor: "move"
        drag: (event, ui)=>
          # position of minimap viewport in % (is set to px value due drag :P)
          xPos = @relatedCanvas.width()  * (ui.position.left / @$el.width()  * 100) / 100  
          yPos = @relatedCanvas.height() * (ui.position.top  / @$el.height() * 100) / 100 

          @relatedCanvas.css
            'left'  : "#{-xPos}px",
            'top'   : "#{-yPos}px"


    draggable:->
      console.log @itsDraggable
      @$el.draggable
        cancel:      "a.ui-icon, .node"
        containment: "parent"
        cursor:      "move"


    renderAndAppendTo:($element, @itsDraggable)->
      
      stats = 
        width:  @relatedViewport.width() / 70
        height: @relatedViewport.height() / 70
        left: 0
        top:  0

      @$el.html @template stats
      $element.append(@el)
      @draggable() if @itsDraggable

      @$el.css 
        'position' : 'absolute'
        'background-color':'rgba(190,190,190, 0.6)'
        'left'     : '90%'
        'top'      : '1%'
        'width'    : @relatedCanvas.width() / 70
        'height'   : @relatedCanvas.height() / 70
      @afterAppend()
      @


    updatePosition:=>
      $minimapViewport = @$el.find(".#{document.minimapViewportCN}")
      
      posX = ((parseFloat(@relatedCanvas.css('left'))  + @relatedCanvas.width() ) / @relatedCanvas.width()  ) * 100
      posY = ((parseFloat(@relatedCanvas.css('top'))   + @relatedCanvas.height()) / @relatedCanvas.height() ) * 100
      
      $minimapViewport.css
        'left' : "#{-(posX-100)}%"
        'top'  : "#{-(posY-100)}%"      

  module.exports = Minimap