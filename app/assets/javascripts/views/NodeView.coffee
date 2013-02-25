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
      parentCenterTop = @getCenterCoordinates(element).top

      currentTop = 0
      
      totalChildrenHeight = 0
      if $children.length > 0
        for child in $children
          childHeight = @alignChildrenofElement($(child).children('.children'), sideOfTree, i+1)
          heightOfChildren[$(child).attr('id')] = childHeight
        
        lastChild = null
        for child in $children
          totalChildrenHeight += heightOfChildren[$(child).attr('id')] + @verticalSpacer
          if lastChild == null
            currentTop = -$(child).outerHeight()/2
          currentTop += heightOfChildren[$(child).attr('id')]/2
          $(child).css('top', currentTop)
          
          if sideOfTree == 'left'
            $(child).addClass('left')
            $(child).css('left', -$(child).outerWidth() - @horizontalSpacer)
          else
            $(child).addClass('right')	
            $(child).css('left', @horizontalSpacer) 
          lastChild = child
          currentTop += heightOfChildren[$(child).attr('id')]/2 + @verticalSpacer
        # to correct the addition on the last run we subtract the last added height
        currentTop = currentTop - heightOfChildren[$(lastChild).attr('id')] - @verticalSpacer
        
        $(childrenContainer).css('top', -(totalChildrenHeight/2 - elementHeight/2))
        $(childrenContainer).css('height', Math.max(totalChildrenHeight, elementHeight))
        
        if sideOfTree == 'left'
          $(childrenContainer).css('left', -elementWidth+'px')
        else
          $(childrenContainer).css('left', elementWidth+'px');	
        
      Math.max(totalChildrenHeight, elementHeight)


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


    selectNextChild: (selectedNode, side = 'left')->
      $selectedNode = $('#'+(selectedNode.get 'id')) 
      
      nextNode = null
      if selectedNode instanceof RootNode
        if side == 'left'
          nextNode = selectedNode.getNextLeftChild()
        else
          nextNode = selectedNode.getNextRightChild()
      else
          nextNode = selectedNode.getNextChild()

      if nextNode != null
        selectedNode.set 'selected', false
        nextNode.set 'selected', true
        return true
      false
        
    selectParent: (selectedNode)->
      selectedNode.get('parent').set 'selected', true
      selectedNode.set 'selected', false
    
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
          selectedNode.get('parent').findById(id).set 'previouslySelected', true
          selectedNode.set 'selected', false
          selectedNode.set 'previouslySelected', false
          return true
      false
    
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
          
          if code in document.navigation.key.fold
            selectedNode.set 'folded', true
          else if code in document.navigation.key.unfold
            selectedNode.set 'folded', false
        else
          @model.set 'selected', true
          
        event.preventDefault()
      
      


  module.exports = NodeView