define ['views/NodeView'], (NodeView) ->
  module = ->
  
  class RootNodeView extends NodeView

    template: Handlebars.templates['RootNode']

    constructor: (model) ->
      super model

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

    #TODO: add translate

    scale:(amount)->      
      #possibilities = document.body.style

      #if($.inArray('WebkitTransform', possibilities) or 
      #   $.inArray('MozTransform', inpossibilities) or 
      #   $.inArray('OTransform', possibilities) or 
      #   $.inArray('transform', possibilities))

      #  @getElement().css
      #    '-moz-transform'    : "scale(#{amount})"  #/* Firefox */
      #    '-webkit-transform' : "scale(#{amount})"  #/* Safari and Chrome */
      #    '-ms-transform'     : "scale(#{amount})"  #/* IE 9 */
      #    '-o-transform'      : "scale(#{amount})"  #/* Opera */  
      el = @getElement()
      el.zoomTo({targetsize:amount*(el.outerWidth()/el.parent().parent().width()), duration:600, root: el.parent()});


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