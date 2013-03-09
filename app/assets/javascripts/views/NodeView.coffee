define ['views/AbstractNodeView', 'models/RootNode'], (AbstractNodeView, RootNode) ->
  module = ->
  
  class NodeView extends AbstractNodeView

    template: Handlebars.templates['Node']

    constructor: (model) ->
      super model


    recursiveRender: (parent, nodes)->
      $.each(nodes, (index, node)=>
        nodeView = new NodeView(node)
        $nodeHtml = $($(nodeView.render().el).html())
        $(parent).append($nodeHtml)
        
        children = node.get 'children'
        if children != undefined
          @recursiveRender($nodeHtml.find('.children:first'), children)
      )      
    
    adjustNodeHierarchy: (parent, children, treeIdentifier)->
      parentId = parent.get 'id'
      $parent = $('#'+parentId)

      $.each(children, (index, node)=>
        childId = node.get 'id' 
        $child = $('#'+childId)
        $child.addClass(treeIdentifier)
        children = node.get 'children'
        if children != undefined
          @adjustNodeHierarchy(node, children)
        $parent.children('.children:first').append($child)
        connectNodes $parent, $child
      )
      
      
    getCenterCoordinates: ($element) ->
      leftCenter = $element.position().left + $element.width() / 2
      topCenter = $element.position().top + $element.height() / 2
      top: topCenter, left: leftCenter

    
    alignChildrenofElement:(childrenContainer, sideOfTree, i = 1) ->
      element = $(childrenContainer).parent()
      $children = $(childrenContainer).children('.node')
      elementHeight = $(element).outerHeight()
      elementWidth = $(element).outerWidth()
      heightOfChildren = {}
      widthOfChildren = {}
      parentCenterTop = @getCenterCoordinates(element).top

      currentTop = 0
      
      totalChildrenHeight = 0
      totalChildrenWidth = 0;
      if $children.length > 0
        for child in $children
          childSize = @alignChildrenofElement($(child).children('.children'), sideOfTree, i+1)
          heightOfChildren[$(child).attr('id')] = childSize[0]
          widthOfChildren[$(child).attr('id')] = childSize[1]
          totalChildrenWidth = Math.max(totalChildrenWidth, $(child).outerWidth() + childSize[1])
        
        lastChild = null
        for child in $children
          totalChildrenHeight += heightOfChildren[$(child).attr('id')] + @verticalSpacer
          
          if lastChild == null
            currentTop = -$(child).outerHeight()/2
          currentTop += heightOfChildren[$(child).attr('id')]/2
          $(child).css('top', currentTop)
          
          if sideOfTree == 'left'
            $(child).addClass('left')
            $(child).css('right', @horizontalSpacer)
          else
            $(child).addClass('right')	
            $(child).css('left', @horizontalSpacer) 
          lastChild = child
          currentTop += heightOfChildren[$(child).attr('id')]/2 + @verticalSpacer
        # to correct the addition on the last run we subtract the last added height
        currentTop = currentTop - heightOfChildren[$(lastChild).attr('id')] - @verticalSpacer
        totalChildrenWidth += @horizontalSpacer
        $(childrenContainer).css('top', -(totalChildrenHeight/2 - elementHeight/2))
        $(childrenContainer).css('height', Math.max(totalChildrenHeight, elementHeight))
        $(childrenContainer).css('width', totalChildrenWidth)
          
        if sideOfTree == 'left'
          #$(childrenContainer).css('left', -elementWidth+'px')
          $(childrenContainer).css('left', -totalChildrenWidth+'px')
        else
          $(childrenContainer).css('left', elementWidth+'px');	
        
      [Math.max(totalChildrenHeight, elementHeight), totalChildrenWidth]


    destroy: ->
      @model?.off null, null, @

      # destroy all subviews
      for viewId, view of @subViews
        view.destroy()

      @$el.remove()

    # pass a final function, if u want to
    leave: (done = ->) ->
      @destroy()
      done()


    
    

      


  module.exports = NodeView