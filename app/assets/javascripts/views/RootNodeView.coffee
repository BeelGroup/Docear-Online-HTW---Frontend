define ['views/NodeView'], (NodeView) ->
  module = ->
  
  class RootNodeView extends NodeView

    template: Handlebars.templates['RootNode']

    constructor: (model) ->
      super model
      @lastScaleAmount = 1
      @currentScale = 100

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

    centerInContainer: ->
      node = $('#'+@model.get 'id')

      posX = $(node).parent().parent().width()  / 2  - $(node).outerWidth()  / 2
      posY = $(node).parent().parent().height() / 2  - $(node).outerHeight() / 2
      
      node.css 'left', posX + 'px'
      node.css 'top' , posY + 'px'
 

    scale:(amount)->      
      possibilities = document.body.style
      fallback = false

      console.log  $.browser.version
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
        @getElement().animate {'scale' : amount}, 100

      else
        #console.log $.browser
        fallback = true

      # ultra fallback
      if fallback
        scaleDiff = 0
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