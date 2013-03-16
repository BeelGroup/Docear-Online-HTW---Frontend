define ->
  module = () ->

  class Canvas extends Backbone.View

    tagName: 'div'
    className: 'mindmap-canvas'


    constructor:(@id)->
      super()
      @size = @minSize = 3000
      @zoomAmount = 100
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


    checkBoundaries:->
      @totalSize = @rootView.getTotalSize()

      if @totalSize.xMaxHalf * document.maxZoom > @totalSize.yMaxHalf * document.maxZoom 
        greaterValue = @totalSize.xMaxHalf * document.maxZoom / 50 
      else
        greaterValue = @totalSize.yMaxHalf * document.maxZoom / 50

      if greaterValue > @minSize
        # distance between root and first child is currently not noted - so +200 as workaround
        @resize greaterValue + 500
      else
        @resize @minSize



    resize:(size)->

      if size != @size
        @size = size
        console.log 'Resize canvas to: '+ @size + ' px'

        curWidth = @$el.width()
        curHeight = @$el.height()
        curX = @$el.css 'left'
        curY = @$el.css 'top'

        xdiff = @size - curWidth 
        ydiff = @size - curHeight 

        @$el.css 
          'width' : "#{@size}px"
          'height': "#{@size}px"
          'left'  : "#{curX - xdiff/2}px"
          'top'   : "#{curY - ydiff/2}px"

        @$el.trigger 'resize'
        @rootView.centerInContainer()
        @center()




    getElement:()->
      $("##{@id}")

    calculateBrowserZoom:->
      @browserZoom = Math.round(window.outerWidth / window.innerWidth * 100)/100

    afterAppend:()->
      if $.browser.chrome
        @$el.draggable
          start:(evt, ui)=>
            @calculateBrowserZoom()
            if @browserZoom < 0.95 || @browserZoom > 1.05
              @fallback = true
            else
              @fallback = false
            @dragCounter = 0          
          drag:(evt,ui)=>
            if(@fallback)
              ui.position.top = Math.round(ui.position.top / @browserZoom )
              ui.position.left = Math.round(ui.position.left / @browserZoom )

              if @dragCounter > 0
                position=
                  x: parseFloat(@$el.css('left')) * 1/@browserZoom
                  y: parseFloat(@$el.css('top'))* 1/@browserZoom
                @$el.trigger 'canvasWasMovedTo', stats= position: position, animated: false

            else
              @$el.trigger 'dragging'


            @dragCounter++

          cancel: "a.ui-icon, .inner-node, :input"
          containment: @$el.parent().attr('id')
          cursor: "move"
          handle: @id
      else
        @$el.draggable
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
        if(@browserZoom > 1.05 && @browserZoom < 0.95) then animated = false
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

      @$el.trigger 'canvasWasMovedTo', stats= position: position, animated: true


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
        x: (@size / 2) * -1
        y: (@size / 2) * -1

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
      @rootView.getElement().on 'newSelectedNode', (event, selectedNode)=> @centerViewTo(selectedNode, false)
      @rootView.getElement().on 'newFoldedNode', (event, selectedNode)=> @foldNode(selectedNode)
      
      @zoomAmount = 100  

      @checkBoundaries()


    foldNode:(@selectedNode)->
      @$overlay = @$el.parent().parent().find(".loading-map-overlay")
      @$overlay.fadeIn(200, =>
        oldZoomAmount = @zoomAmount
        @zoomAmount = 100
        @zoom(@zoomAmount, false)
        $selectedNode = $('#'+(@selectedNode.get 'id'))
        @selectedNode.set 'folded', $selectedNode.children('.children').is(':visible')
        #TODO: check size after fold
        #@checkBoundaries()
        @zoomAmount = oldZoomAmount
        @zoom(@zoomAmount, false)
        @$overlay.fadeOut(400)
      )
      

    centerViewTo:(selectedNode, shiftInAnyCase = true)->
      $element = $("##{selectedNode.id}")

      canvasWidth = $element.width()  * @zoomAmount
      canvasHeight = $element.height() * @zoomAmount

      halfParentWidth = @$el.parent().width()  / 2
      halfParentHeight = @$el.parent().height() / 2

      halfElementWidth = $element.width() * @zoomAmount/200
      halfElementHeight = $element.height() * @zoomAmount/200

      elementPos =
        x: ($element.offset().left + (canvasWidth / 200))
        y: ($element.offset().top  + (canvasHeight / 200))

      delta = 
        x: halfParentWidth - elementPos.x + @$el.parent().offset().left
        y: halfParentHeight - elementPos.y + @$el.parent().offset().top

      iCanSeeU = true

      #console.log 'delta x ' + Math.abs(delta.x) + ' elementWidth ' + elementWidth + ' half parent width ' + halfParentWidth
      #console.log 'delta y ' + Math.abs(delta.y) + ' elementHeight ' + elementHeight + ' half parent height ' + halfParentHeight

      # right corner
      if delta.x < 0.0
        if (Math.abs(delta.x) + halfElementWidth) >= halfParentWidth
          iCanSeeU = false
      else # left corner
        if (Math.abs(delta.x) + halfElementWidth) >= halfParentWidth
          iCanSeeU = false
      # lower corner
      if delta.y < 0.0
        if (Math.abs(delta.y) + halfElementHeight) >= halfParentHeight
          iCanSeeU = false
      else # upper corner
        if (Math.abs(delta.y) + halfElementHeight) >= halfParentHeight
          iCanSeeU = false

      if shiftInAnyCase or not iCanSeeU
        @move delta


    renderAndAppendTo:($element)->
      $element.append(@render().el)

      @$el.css 
        'width' : "#{@size}px"
        'height': "#{@size}px"

      @center()
      @moreEvents()
      @afterAppend()



  module.exports = Canvas