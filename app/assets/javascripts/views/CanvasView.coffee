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
          if event.keyCode == 27
            @centerViewTo @rootView.model
            @rootView.model.set 'selected', true
          else
            @rootView.userKeyInput event


    checkBoundaries:->
      @totalMapsize = @rootView.getTotalSize()

      if @totalMapsize.xMaxHalf * document.maxZoom > @totalMapsize.yMaxHalf * document.maxZoom 
        greaterValue = @totalMapsize.xMaxHalf * document.maxZoom / 50 
      else
        greaterValue = @totalMapsize.yMaxHalf * document.maxZoom / 50

      if greaterValue > @minSize
        # distance between root and first child is currently not noted - so +500 as workaround
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


    move:(delta, animated = true, time = 200)->
      pos=
        x: parseFloat(@$el.css 'left') + delta.x
        y: parseFloat(@$el.css 'top')  + delta.y

      @moveTo pos, animated, time


    moveTo:(position, animated, time = 200)->
      if $.browser.chrome
        @calculateBrowserZoom()
        if(@browserZoom > 1.05 && @browserZoom < 0.95) then animated = false
        position=
          x: position.x * 1/@browserZoom
          y: position.y * 1/@browserZoom

      if animated
        @$el.animate {'left':"#{position.x}px",'top':"#{position.y}px"}, {duration: time, queue: true}
      else
        @$el.stop()
        @$el.css
          'left'  : "#{position.x}px"
          'top'   : "#{position.y}px" 

      @$el.trigger 'canvasWasMovedTo', stats= position: position, animated: true


    zoomIn:(event)=>
      if(@zoomAmount+document.zoomStep <= document.maxZoom)
        @oldZoomAmount = @zoomAmount
        @zoomAmount += document.zoomStep
        @repositionViewportOnZoom(true)
        @zoom(@zoomAmount)
       

    zoomOut:(event, shift)=>
      if(@zoomAmount-document.zoomStep >= document.minZoom)
        @oldZoomAmount = @zoomAmount
        @zoomAmount -= document.zoomStep
        @repositionViewportOnZoom(false)
        @zoom(@zoomAmount)


    zoom:(amount, animate = true)=>
      if(typeof @rootView != "undefined")
        console.log "zoom:#{amount}%"

        @previousMapSize.x = @currentMapSize.x
        @previousMapSize.y = @currentMapSize.y
        @currentMapSize.x = @totalMapsize.x * amount/100
        @currentMapSize.y = @totalMapsize.y * amount/100
        
        @rootView.scale amount/100, true
        @$el.trigger 'zoom', amount/100
        5500

    zoomCenter:()=>
      if(typeof @rootView != "undefined")
        @zoomAmount = 100
        @zoom(@zoomAmount)
        @center()

    repositionViewportOnZoom:(zoomIn)->   

      xGrow = @totalMapsize.x * @zoomAmount - @totalMapsize.x * @oldZoomAmount
      yGrow = @totalMapsize.y * @zoomAmount - @totalMapsize.y * @oldZoomAmount


      pos = @getPosition()
      xRelDist = (Math.abs(pos.x) / @size) * 2 
      yRelDist = (Math.abs(pos.y) / @size) * 2

      diff=
       x: xGrow/100 * xRelDist
       y: yGrow/100 * yRelDist

      quadrant = @getQuadrant() 

      if quadrant == 1
        @move pos= x: -diff.x, y: diff.y, true, 100
      if quadrant == 2
        @move pos= x:  diff.x, y: diff.y, true, 100 
      if quadrant == 3
        @move pos= x:  diff.x, y: -diff.y, true, 100  
      if quadrant == 4
        @move pos= x: -diff.x, y: -diff.y, true, 100

    getDistanceToCenter:->
      pos = @getPosition()
      dist = Math.sqrt(pos.x*pos.x + pos.y*pos.y)

    getRelativeDistanceToCenter:->
      dist = @getDistanceToCenter()
      relDist = dist / @$el.width() * 2

    getPosition:->
      pos= 
        x: (@$el.width() / 2) + (@$el.parent().width() / -2) + parseFloat @$el.css('left')  
        y: (@$el.height() / 2) + (@$el.parent().height() / -2) + parseFloat @$el.css('top') 

    getQuadrant: ->
      error = 0
      pos = @getPosition()

      if pos.x < -error && pos.y > error
        quadrant = 1
      else if pos.x > error && pos.y > error
        quadrant = 2
      else if pos.x > error && pos.y < -error
        quadrant = 3
      else if pos.x < -error && pos.y < -error
        quadrant = 4
      else
        quadrant = 0

      quadrant

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
      @currentMapSize = @rootView.getTotalSize()
      @previousMapSize = @rootView.getTotalSize()
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