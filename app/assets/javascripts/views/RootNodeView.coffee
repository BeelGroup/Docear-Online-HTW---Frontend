define ['views/NodeView', 'models/RootNode'], (NodeView, RootNode) ->
  module = ->
  
  class RootNodeView extends NodeView

    template: Handlebars.templates['RootNode']

    constructor: (model) ->
      super model
      model.bind 'change:property1', -> alert("change pty1")
      @lastScaleAmount = 1
      @currentScale = 100
      @scaleAmount = 1.0

    collapsFoldedNodes:()->      
      foldedNodes = $('.node.folded')
      $(foldedNodes).children('.children').hide()
      $(foldedNodes).find("i.fold").toggleClass('icon-minus-sign')
      $(foldedNodes).find("i.fold").toggleClass('icon-plus-sign')

    #
    # Refresh the mind map an reposition the dom elements
    #
    refreshDom: () ->
      height1 = @alignChildrenofElement($('#'+@model.get 'id').children('.leftChildren:first'), 'left')
      height2 = @alignChildrenofElement($('#'+@model.get 'id').children('.rightChildren:first'), 'right')
      height = (height1 > height2) ? height1 : height2
      
      jsPlumb.repaintEverything()
      height
      
    connectChildren: ->
      @recursiveConnectNodes $(@$el).find('.rightChildren:first')
      @recursiveConnectNodes $(@$el).find('.leftChildren:first')


    recursiveConnectNodes: (childrenContainer)->
      parent = $(childrenContainer).parent()
      children = childrenContainer.children('.node')
      if $(children).size() > 0
        $.each(children, (index, child)=>
          connectNodes "#"+parent.attr('id'), "#"+$(child).attr('id')
          @recursiveConnectNodes $(child).children('.children:first')
        ) 


    getTotalSize:()->
      $me = $('#'+@model.get 'id')

      sizeOfLeftChilds=
        x: $me.children('.rightChildren:first').outerWidth()
        y: $me.children('.rightChildren:first').outerHeight()

      sizeOfRightChilds=
        x: $me.children('.leftChildren:first').outerWidth()
        y: $me.children('.leftChildren:first').outerHeight()

      leftSize = $me.outerWidth() / 2 + sizeOfLeftChilds.x
      rightSize = $me.outerWidth() / 2 + sizeOfRightChilds.x

      if sizeOfLeftChilds.y > sizeOfRightChilds.y
        height = sizeOfLeftChilds.y
      else
        height = sizeOfRightChilds.y

      totalSizes=
        x: sizeOfLeftChilds.x + $me.outerWidth() + sizeOfRightChilds.x
        y: height
        xRight: rightSize
        xLeft : leftSize
        xMaxHalf  : if leftSize > rightSize then leftSize else rightSize
        yMaxHalf  : height / 2


    setChildPositions: ->
      $me = $('#'+@model.get 'id')
      canvas = $me.parent().parent()

      leftChilds = new Array()
      rightChilds = new Array()

      # root
      @positions=
        pos:
          left: ($me.offset().left + ($me.width() / 2) - $(canvas).offset().left) 
          top: ($me.offset().top + ($me.height() / 2) - $(canvas).offset().top)
        width: $me.width()
        height: $me.height() 
        display: 'block'
        leftChilds: @childPositions $me.find('.leftChildren:first'), leftChilds, canvas
        rightChilds: @childPositions $me.find('.rightChildren:first'), rightChilds, canvas 
      @positions

    childPositions: (childrenContainer, positions, canvas)->
      children = childrenContainer.children('.node')
      if childrenContainer.css('display') == 'block'
        if $(children).size() > 0
          $.each(children, (index, child)=>
            positions.push 
              pos:
                left: ($(child).offset().left - $(canvas).offset().left - $(canvas).width() / 2) / @scaleAmount
                top: ($(child).offset().top - $(canvas).offset().top - $(canvas).height() / 2) / @scaleAmount
              width: $(child).width()
              height: $(child).height()
            @childPositions $(child).children('.children:first'), positions, canvas
          )
      positions

    userKeyInput: (event)->
      if event.keyCode == 0
        code = event.charCode
      else
        code = event.keyCode
      if (code) in document.navigation.key.allowed
        selectedNode = @model.getSelectedNode()
        if selectedNode != null
          $selectedNode = $('#'+(selectedNode.get 'id')) 
          switch (event.keyCode)
            when document.navigation.key.selectLeftChild
              if $($selectedNode).hasClass('right')  
                @selectParent selectedNode
              else
                @selectNextChild selectedNode, 'left'
            when document.navigation.key.selectPrevBrother #TOP
              @selectBrother selectedNode, false
            when document.navigation.key.selectRightChild #RIGHT
              if $($selectedNode).hasClass('left')  
                @selectParent selectedNode
              else
                @selectNextChild selectedNode, 'right'
            when document.navigation.key.selectNextBrother #DOWN
              @selectBrother selectedNode, true
            when document.navigation.key.fold #F
              $("##{@model.get 'id'}").trigger 'newFoldedNode', selectedNode
        else
          @model.set 'selected', true

        event.preventDefault()
      
    selectNextChild: (selectedNode, side = 'left')->
      $selectedNode = $('#'+(selectedNode.get 'id')) 
      if $selectedNode.children('.children').is(':visible')
        nextNode = null
        if selectedNode instanceof RootNode
          if side == 'left'
            nextNode = selectedNode.getNextLeftChild()
          else
            nextNode = selectedNode.getNextRightChild()
        else
            nextNode = selectedNode.getNextChild()
  
        if nextNode != null
          #selectedNode.set 'selected', false
          nextNode.set 'selected', true
          return true
      false
        
    selectParent: (selectedNode)->
      selectedNode.get('parent').set 'selected', true
      #selectedNode.set 'selected', false
    
    selectBrother: (selectedNode, next = true)->
      $selectedNode = $('#'+(selectedNode.get 'id'))
      if not (selectedNode instanceof RootNode)
        prevNode = null
        nextNode = null
        
        if next
          $brother = $($selectedNode).next('.node')
        else
          $brother = $($selectedNode).prev('.node')
        if $($brother).size() > 0
          id = $($brother).attr('id')
          selectedNode.get('parent').findById(id).set 'selected', true
          #selectedNode.get('parent').findById(id).set 'previouslySelected', true
          #selectedNode.set 'selected', false
          #selectedNode.set 'previouslySelected', false
          return true
      false

    centerInContainer: ->
      node = $('#'+@model.get 'id')

      posX = $(node).parent().parent().width()  / 2  - $(node).outerWidth()  / 2
      posY = $(node).parent().parent().height() / 2  - $(node).outerHeight() / 2
      
      node.css 'left', posX + 'px'
      node.css 'top' , posY + 'px'
 

    scale:(amount, animate = true)->  
      @scaleAmount = amount    
      possibilities = document.body.style
      fallback = false

      #console.log  $.browser.version
      # IE
      if $.browser.msie 
        if $.browser.version > 8
          #console.log 'IE 9 & 10'
          @getElement().css
            '-ms-transform': "scale(#{amount})" 

        else if $.browser.version <= 8 
          #console.log 'IE 7 & 8'
          fallback = true

      # Safari, Firefox and Chrome with CSS3 support 
      else if($.inArray('WebkitTransform', possibilities) or 
      $.inArray('MozTransform', inpossibilities) or 
      $.inArray('OTransform', possibilities)) 
        #console.log 'Webkit, Moz, O'
        if animate
          @getElement().animate {'scale' : amount}, 100
        else
          @getElement().animate {'scale' : amount}, 0

      else
        #console.log $.browser
        fallback = true

      # ultra fallback
      if fallback
        scaleDiff = 0
        if @lastScaleAmount != amount
          if amount > @lastScaleAmount then scaleDiff = 25 else scaleDiff = -25
          @getElement().parent().effect 'scale', {percent: 100 + scaleDiff, origin: ['middle','center']}, 1, => @refreshDom()
          @lastScaleAmount = amount
          @currentScale += scaleDiff


    render: ->
      @$el.html @template @getRenderData()
      @recursiveRender $(@$el).find('.rightChildren:first'), (@model.get 'rightChildren')
      @recursiveRender $(@$el).find('.leftChildren:first'), (@model.get 'leftChildren')
      
      # render the subviews
      for viewId, view of @subViews
        html = view.render().el
        $(html).appendTo(@el)
      
      # extend the ready rendered htlm element
      @afterRender()
      @



  module.exports = RootNodeView