define ['views/NodeView', 'models/RootNode', 'views/NodeControlsView'], (NodeView, RootNode, NodeControlsView) ->
  module = ->
  
  class RootNodeView extends NodeView

    
    tagName: 'div'
    template: Handlebars.templates['RootNode']

    constructor: (model) ->
      super model
      @lastScaleAmount = 1
      @currentScale = 100
      @scaleAmount = 1.0

    addEvents:()->
      @$el.click (event)=> 
        @handleClick(event)
        @handleRootClick(event)
        event.stopPropagation()
        false

    handleRootClick:(event)->
      if $(event.target).hasClass 'action-fold-right'
        @changeFoldedStatus 'right' 
      else if $(event.target).hasClass 'action-fold-left'
        @changeFoldedStatus 'left'


    collapsFoldedNodes:()->      
      foldedNodes = $('.node.folded')
      $(foldedNodes).children('.children').hide()


    changeFoldedStatus:(side)->
      if side is 'left' or side is 'both'
        @$el.children('.inner-node').children('.action-fold.left').toggleClass('invisible')
        @$el.children('.leftChildren').fadeToggle(document.fadeDuration)
      if side is 'right' or side is 'both'
        @$el.children('.inner-node').children('.action-fold.right').toggleClass('invisible')
        @$el.children('.rightChildren').fadeToggle(document.fadeDuration)



    # Refresh the mind map and reposition the dom elements
    refreshDom: () ->
      height1 = @alignChildrenofElement(@$el.children('.leftChildren:first'), 'left')
      height2 = @alignChildrenofElement(@$el.children('.rightChildren:first'), 'right')
      height = (height1 > height2) ? height1 : height2
      
      height
      
    connectChildren: ->
      @model.updateAllConnections()

    getTotalSize:()->
      sizeOfLeftChilds=
        x: @$el.children('.rightChildren:first').outerWidth()
        y: @$el.children('.rightChildren:first').outerHeight()

      sizeOfRightChilds=
        x: @$el.children('.leftChildren:first').outerWidth()
        y: @$el.children('.leftChildren:first').outerHeight()

      leftSize = @$el.outerWidth() / 2 + sizeOfLeftChilds.x
      rightSize = @$el.outerWidth() / 2 + sizeOfRightChilds.x

      if sizeOfLeftChilds.y > sizeOfRightChilds.y
        height = sizeOfLeftChilds.y
      else
        height = sizeOfRightChilds.y

      totalSizes=
        x: sizeOfLeftChilds.x + @$el.outerWidth() + sizeOfRightChilds.x
        y: height
        xRight: rightSize
        xLeft : leftSize
        xMaxHalf  : if leftSize > rightSize then leftSize else rightSize
        yMaxHalf  : height / 2


    setChildPositions: ->
      @$el = @$el
      canvas = @$el.parent()

      leftChilds = new Array()
      rightChilds = new Array()

      # root
      @positions=
        pos:
          left: (@$el.offset().left + (@$el.width() / 2) - $(canvas).offset().left) 
          top: (@$el.offset().top + (@$el.height() / 2) - $(canvas).offset().top)
        width: @$el.width()
        height: @$el.height() 
        display: 'block'
        leftChilds: @childPositions @$el.find('.leftChildren:first'), leftChilds, canvas
        rightChilds: @childPositions @$el.find('.rightChildren:first'), rightChilds, canvas 
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
      code = if event.keyCode == 0 then event.charCode  else event.keyCode
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
              selectedNode.set 'folded', not selectedNode.get 'folded' 
        else
          @model.set 'selected', true
        event.preventDefault()
      
    selectNextChild: (selectedNode, side = 'left')->
      $selectedNode = $('#'+(selectedNode.get 'id')) 
      if $selectedNode.children('.children').is(':visible')
        nextNode = null
        if typeof(selectedNode.get 'leftChildren') != 'undefined'
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
      if typeof(selectedNode.get 'leftChildren') == 'undefined'
        prevNode = null
        nextNode = null
        
        if next
          $brother = $($selectedNode).next('.node')
        else
          $brother = $($selectedNode).prev('.node')
        if $($brother).size() > 0
          id = $($brother).attr('id')
          brother = selectedNode.get('parent').findById(id)
          brother.set 'selected', true
          brother.set 'previouslySelected', true
          selectedNode.set 'selected', false
          selectedNode.set 'previouslySelected', false
          return true
      false

    centerInContainer: ->
      node = @$el

      posX = $(node).parent().width()  / 2  - $(node).outerWidth()  / 2
      posY = $(node).parent().height() / 2  - $(node).outerHeight() / 2
      
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
          @getElement().animate {'scale' : amount}, {duration: 100, queue: false}
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
          @getElement().effect 'scale', {percent: 100 + scaleDiff, origin: ['middle','center']}, 1, => @refreshDom()
          @lastScaleAmount = amount
          @currentScale += scaleDiff

    render: ->
      @$el.html @template @getRenderData()
      @$el.addClass('root')

      @controls = new NodeControlsView(@model)
      @controls.renderAndAppendToNode(@)
      
      @

    # USE THIS FUNCTION instead of render
    renderAndAppendTo:($element)->
      $element.append @render().el 
      @alignButtons()
      @recursiveRender @, $(@$el).find('.rightChildren:first'), (@model.get 'rightChildren'), @
      @recursiveRender @, $(@$el).find('.leftChildren:first'), (@model.get 'leftChildren'), @
      
      # render the subviews
      for viewId, view of @subViews
        html = view.render().el
        $(html).appendTo(@el)
      


  module.exports = RootNodeView