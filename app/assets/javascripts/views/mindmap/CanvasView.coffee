define ['logger'], (logger) ->
  module = () ->

  class Canvas extends Backbone.View

    tagName: 'div'
    className: 'mindmap-canvas'

    events:
      'mousedown': (event)-> document.wasDragged = false
      # 'mouseup': if not document.wasDragged then @rootView.model.selectNone()

    constructor:(@id)->
      super()
      @size = @minSize = 3000
      @zoomAmount = 100
      @calculateBrowserZoom()


    moreEvents:()=>
      @$el.mousewheel (event, delta, deltaX, deltaY)=>
        if document.strgPressed is off
          if deltaY > 0 then dir = 1 else dir = -1 
          @move({x: false, y: document.scrollStep*dir}, false, document.scrollDuration)
          event.preventDefault() 

      Mousetrap.bind document.navigation.key.strg, (event)=>
        document.strgPressed = on
        document.log 'strg on'
      , 'keydown'

      Mousetrap.bind document.navigation.key.strg, (event)=>
        document.strgPressed = off
        document.log 'strg off'
      , 'keyup'
      
      Mousetrap.bind document.navigation.key.centerMap, (event)=>
        if !($(event.target).is('input, textarea')) and typeof @rootView != "undefined"
          @center(true)
      
      Mousetrap.bind document.navigation.key.left, (event)=>
        selectedNode = @rootView.model.getSelectedNode()
        if selectedNode != null
          $selectedNode = $('#'+(selectedNode.get 'id')) 
          if $($selectedNode).hasClass('right')  
            @rootView.selectParent selectedNode
          else
            @rootView.selectNextChild selectedNode, 'left'
        else
          @rootView.model.set 'selected', true

      Mousetrap.bind document.navigation.key.right, (event)=>
        selectedNode = @rootView.model.getSelectedNode()
        if selectedNode != null
          $selectedNode = $('#'+(selectedNode.get 'id')) 
          if $($selectedNode).hasClass('left')  
            @rootView.selectParent selectedNode
          else
            @rootView.selectNextChild selectedNode, 'right'
        else
          @rootView.model.set 'selected', true

      Mousetrap.bind document.navigation.key.up, (event)=>
        selectedNode = @rootView.model.getSelectedNode()
        if selectedNode != null
          @rootView.selectBrother selectedNode, false
        else
          @rootView.model.set 'selected', true
          
      Mousetrap.bind document.navigation.key.down, (event)=>
        selectedNode = @rootView.model.getSelectedNode()
        if selectedNode != null
          @rootView.selectBrother selectedNode, true
        else
          @rootView.model.set 'selected', true

      Mousetrap.bind document.navigation.key.fold, (event)=>
        selectedNode = @rootView.model.getSelectedNode()
        if selectedNode != null
          if selectedNode.typeName is 'rootModel' 
            @rootView.changeFoldedStatus 'both'
          else
            selectedNode.set 'folded', not selectedNode.get 'folded'

      Mousetrap.bind document.navigation.key.addSibling, (event)=>
        if !@rootView.model.get('isReadonly') and $('.node-edit-container').size() <= 0
          selectedNode = @rootView.model.getSelectedNode()
          if selectedNode != null and @rootView.model.get('id') isnt selectedNode.get('id')
            parent = selectedNode.get('parent')
            if parent.get('id') == @rootView.model.get('id')
              side = 'Left'
              if $("##{selectedNode.get('id')}").hasClass('right')  
                side = 'Right'
              selectedNode.get('parent').createAndAddChild(side)
            else
              selectedNode.get('parent').createAndAddChild()


      Mousetrap.bind document.navigation.key.addChild, (event)=>
        if !@rootView.model.get('isReadonly')
          selectedNode = @rootView.model.getSelectedNode()
          if selectedNode.get('folded')
            selectedNode.set 'folded', false
          if selectedNode != null
            selectedNode.createAndAddChild()


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
        document.log 'Resize canvas to: '+ @size + ' px'

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
        @move({x: -xdiff/2, y: -ydiff/2}, false)
        # doesnt work anymore... the resize function might be callen on unfold
        # @center()


    getElement:()->
      $("##{@id}")

    calculateBrowserZoom:->
      @browserZoom = Math.round(window.outerWidth / window.innerWidth * 100)/100

    updateDragBoundaries:->
      @$el.draggable "option", "containment", @getUpdatedDragBoundaries()

    getUpdatedDragBoundaries:->
      dragBoundaries =
        x1: -@size+@$el.parent().width()+@$el.parent().offset().left
        y1: -@size+@$el.parent().height()+@$el.parent().offset().top
        x2: @$el.parent().offset().left
        y2: @$el.parent().offset().top

      [dragBoundaries.x1, dragBoundaries.y1, dragBoundaries.x2, dragBoundaries.y2]

    afterAppend:()->
      dragBoundaries =
        x1: -@size-@$el.parent().width()
        y1: -@size-@$el.parent().height()
        x2: @$el.parent().offset().left
        y2: @$el.parent().offset().top

      @$el.draggable 
          cancel: "a.ui-icon, .inner-node, :input"
          cursor: "move"
          handle: @id
          containment: @getUpdatedDragBoundaries()
          drag:->
            document.wasDragged = true

      # overwrite settings, if chrome is used      
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
            document.wasDragged = true
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


    move:(delta, animated = true, time = 200)->
      pos=
        x: if delta.x is false then false else parseFloat(@$el.css 'left') + delta.x
        y: if delta.y is false then false else parseFloat(@$el.css 'top')  + delta.y

      @moveTo pos, animated, time


    moveTo:(position, animated, time = 200)->
      newPos = new Object()
      if position.x isnt false
        newPos.left = "#{position.x}px"
      if position.y isnt false
        newPos.top =  "#{position.y}px"

      if $.browser.chrome
        @calculateBrowserZoom()
        if(@browserZoom > 1.05 && @browserZoom < 0.95) then animated = false
        position.x = if position.x is false then false else position.x * 1/@browserZoom
        position.y = if position.y is false then false else position.y * 1/@browserZoom

      if animated
        @$el.animate newPos, {duration: time, queue: true}
      else
        @$el.stop()
        @$el.css newPos

      @$el.trigger 'canvasWasMovedTo', stats= position: position, animated: true


    zoomIn:(event)=>
      if(@zoomAmount+document.zoomStep <= document.maxZoom)
        @oldZoomAmount = @zoomAmount
        @zoomAmount += document.zoomStep
        @repositionViewportOnZoom(true)
        @zoom(@zoomAmount)
       

    zoomOut:(event)=>
      if(@zoomAmount-document.zoomStep >= document.minZoom)
        @oldZoomAmount = @zoomAmount
        @zoomAmount -= document.zoomStep
        @repositionViewportOnZoom(false)
        @zoom(@zoomAmount)


    zoom:(amount, animate = true)=>
      if(typeof @rootView != "undefined")
        document.log "zoom:#{amount}%"
        
        document.currentZoom = amount/100

        @previousMapSize.x = @currentMapSize.x
        @previousMapSize.y = @currentMapSize.y
        @currentMapSize.x = @totalMapsize.x * amount/100
        @currentMapSize.y = @totalMapsize.y * amount/100
        
        @rootView.scale amount/100, true
        @$el.trigger 'zoom', amount/100


    zoomCenter:(selectRoot = true)=>
      if(typeof @rootView != "undefined")
        @zoomAmount = 100
        @zoom(@zoomAmount)
        @center(selectRoot)


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

    center:(selectRoot = false)->
      if typeof(@rootView) != 'undefined' and $("##{@rootView.model.get('id')}").size() > 0
        if (not @rootView.model.get 'selected') and selectRoot
          @rootView.model.set 'selected', true

        @centerViewTo @rootView.model, true, true
          # will throw an event which is cached by this class 
      else
        canvasPivot = @canvasPivot()
        # left upper corner
        canvasPivot.x += @$el.parent().width()  / 2
        canvasPivot.y += @$el.parent().height() / 2
        @moveTo canvasPivot, true


    setRootView:(@rootView)->
      @rootView.getElement().on 'newSelectedNode', (event, selectedNode)=> @centerViewTo(selectedNode, false, false)
      
      @zoomAmount = 100   
      @currentMapSize = @rootView.getTotalSize()
      @previousMapSize = @rootView.getTotalSize()
      @checkBoundaries()


    centerViewTo:(selectedNode, shiftInAnyCase = true, center = true)=>
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
      spacer = 15

      # node is in the right corner
      if delta.x < 0.0
        if (Math.abs(delta.x) + halfElementWidth + spacer) >= halfParentWidth
          iCanSeeU = false
          # if centering is not required, move the not visibe part (plus space) only
          if not center
            delta.x = delta.x + halfParentWidth - halfElementWidth - spacer
        # if node is horizontaly full visible and centering is not required, dont move it horizontally
        else if not center
          delta.x = 0
      # node is in the left corner
      else 
        if (Math.abs(delta.x) + halfElementWidth + spacer) >= halfParentWidth
          iCanSeeU = false
          if not center
            delta.x = delta.x - halfParentWidth + halfElementWidth + spacer
        else if not center
          delta.x = 0
      # node is in the lower corner
      if delta.y < 0.0
        if (Math.abs(delta.y) + halfElementHeight + spacer) >= halfParentHeight
          iCanSeeU = false
          if not center
            delta.y = delta.y + halfParentHeight - halfElementHeight - spacer
        else if not center
          delta.y = 0
      # node is in the upper corner
      else 
        if (Math.abs(delta.y) + halfElementHeight + spacer) >= halfParentHeight
          iCanSeeU = false
          if not center
            delta.y = delta.y - halfParentHeight + halfElementHeight + spacer
        else if not center
          delta.y = 0




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