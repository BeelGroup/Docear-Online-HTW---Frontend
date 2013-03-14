define ->
  module = () ->

  class Canvas extends Backbone.View

    tagName: 'div'
    className: 'mindmap-canvas'


    constructor:(@id, @width = 8000, @height = 8000, @zoomAmount = 100)->
      super()
      @calculateBrowserZoom()


    moreEvents:()=>
      @$el.mousewheel (event, delta, deltaX, deltaY)=>
        $viewport = @$el.parent()          
        x = event.pageX - $viewport.offset().left - $viewport.width()/2
        y = event.pageY - $viewport.offset().top - $viewport.height()/2
        shift = 'x': x, 'y': y
        if deltaY > 0 then @zoomIn(event, shift) else @zoomOut(event, shift)
        event.preventDefault() 

      $(document).keydown (event)=>
        if !($(event.target).is('input, textarea')) and typeof @rootView != "undefined"
          @rootView.userKeyInput event


    getElement:()->
      $("##{@id}")

    calculateBrowserZoom:->
      @browserZoom = Math.round(window.outerWidth / window.innerWidth * 100)/100

    afterAppend:()->
      @$el.draggable
        start:(evt, ui)=>
          @calculateBrowserZoom()
          #console.log @browserZoom
          if($.browser.chrome && (@browserZoom < 0.95 || @browserZoom > 1.05))
            #console.log 'using chrome workaround'
            @dragCounter = 0          
        drag:(evt,ui)=>
          if $.browser.chrome
            if @browserZoom < 0.95 || @browserZoom > 1.05
              canvasHeight = $(@).parent().height()
              canvasWidth = $(@).parent().width()

              ui.position.top = Math.round(ui.position.top / @browserZoom )
              ui.position.left = Math.round(ui.position.left / @browserZoom )

              if @dragCounter > 0
                position=
                  x: parseFloat(@$el.css('left')) * 1/@browserZoom
                  y: parseFloat(@$el.css('top'))* 1/@browserZoom
                @$el.trigger 'canvasWasMovedTo', position, true

            else
              @$el.trigger 'dragging'


            @dragCounter++

        cancel: "a.ui-icon, .inner-node, :input"
        containment: @$el.parent().attr('id')
        cursor: "move"
        handle: @id


    move:(delta)->
      pos=
        x: parseFloat(@$el.css 'left') + delta.x
        y: parseFloat(@$el.css 'top')  + delta.y

      @moveTo pos, true


    moveTo:(position, animated)->

      if $.browser.chrome
        @calculateBrowserZoom()
        if @browserZoom != 1.0 then animated = false
        position=
          x: position.x * 1/@browserZoom
          y: position.y * 1/@browserZoom

      if animated
        @$el.stop().animate
         'left'  : "#{position.x}px"
         'top'   : "#{position.y}px" 
      else
        @$el.stop()
        @$el.css
          'left'  : "#{position.x}px"
          'top'   : "#{position.y}px" 

      @$el.trigger 'canvasWasMovedTo', position, true


    zoomIn:(event)=>
      if(@zoomAmount+document.zoomStep <= document.maxZoom)
        @zoomAmount += document.zoomStep
        @zoom(@zoomAmount)
       

    zoomOut:(event, shift)=>
      if(@zoomAmount-document.zoomStep >= document.minZoom)
        @zoomAmount -= document.zoomStep
        @zoom(@zoomAmount)


    zoom:(amount, animate = true)=>
      if(typeof @rootView != "undefined")
        console.log "zoom:#{amount}%"
        @rootView.scale amount/100, animate
        @$el.trigger 'zoom', amount/100
        #jsPlumb.repaintEverything()


    zoomCenter:()=>
      if(typeof @rootView != "undefined")
        @zoomAmount = 100
        @zoom(@zoomAmount)
        @center()

    canvasPivot: ->
      pos=
        x: (@width  / 2) * -1
        y: (@height / 2) * -1

    center:->
      if typeof(@rootView) != 'undefined'
        if @rootView.model.get 'selected'
          @centerViewTo @rootView.model
        else
          # will throw an event which is cached by this class 
          @rootView.model.set 'selected', true
      else
        canvasPivot = @canvasPivot()
        # left upper corner
        canvasPivot.x += @$el.parent().width()  / 2
        canvasPivot.y += @$el.parent().height()  / 2
        @moveTo canvasPivot, true


    setRootView:(@rootView)->
      @rootView.getElement().on 'newSelectedNode', (event, selectedNode)=> @centerViewTo(selectedNode)
      @rootView.getElement().on 'newFoldedNode', (event, selectedNode)=> @foldNode(selectedNode)
      @zoomAmount = 100  


    foldNode:(@selectedNode)->
      @$overlay = @$el.parent().parent().find(".loading-map-overlay")
      @$overlay.fadeIn(200, =>
        @zoom(100, false)
        $selectedNode = $('#'+(@selectedNode.get 'id'))
        @selectedNode.set 'folded', $selectedNode.children('.children').is(':visible')
        @zoom(@zoomAmount, false)
        @$overlay.fadeOut(400)
      )
      

    centerViewTo:(selectedNode)->
      $element = $("##{selectedNode.id}")

      # position relative to canvas
      elementPos=
        x: ($element.offset().left + ($element.width()  * @zoomAmount / 200))
        y: ($element.offset().top  + ($element.height() * @zoomAmount / 200))
      
      delta= 
        x: @$el.parent().width()  / 2 - elementPos.x + @$el.parent().offset().left
        y: @$el.parent().height() / 2 - elementPos.y + @$el.parent().offset().top

      @move delta


    renderAndAppendTo:($element)->
      $element.append(@render().el)

      @$el.css 
        'width' : "#{@width}px"
        'height': "#{@height}px"

      @center()
      @moreEvents()
      @afterAppend()



  module.exports = Canvas