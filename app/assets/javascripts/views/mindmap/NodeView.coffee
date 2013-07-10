define ['logger','views/mindmap/AbstractNodeView','views/mindmap/ConnectionView', 'views/mindmap/NodeControlsView'], (logger, AbstractNodeView, ConnectionView, NodeControlsView) ->
  module = ->
  
  class NodeView extends AbstractNodeView

    tagName: 'div'
    className: 'node' 
    template: Handlebars.templates['Node']
    id: 99

    constructor:(@model, @rootView) ->
      @id = @model.get 'id'
      super()

    getRootView:->
      @rootView

    recursiveRender: (parent, nodes, @rootView)->
      if not document.cancel_loading
        $.each nodes, (index, node)=>
          nodeView = new NodeView(node, @rootView)
          nodeView.renderAndAppendTo(parent, @rootView)
        

    changeChildren: (lastAddedChild = null)->
      refreshConnections = false
      newChild = lastAddedChild
      if !lastAddedChild or lastAddedChild.get('id') is @model.get('id')
        newChild = @model.get 'lastAddedChild'
        refreshConnections = true

      $node = $(@$el)
      if @model.typeName is 'rootModel'
        if @model.get('lastAddedChildSide') is 'Left'
          $childrenContainer = $node.children('.leftChildren:first')
          @$el.children('.inner-node').find('.action-fold.left').show()
        else
          $childrenContainer = $node.children('.rightChildren:first')
          @$el.children('.inner-node').find('.action-fold.right').show()
      else
        @$el.children('.inner-node').find('.action-fold').show()
        $childrenContainer = $node.children('.children:first')

      previousHeight = $childrenContainer.outerHeight()
      
      nodeView = @
      $child = $("##{newChild.get('id')}")
      if newChild.get('id') isnt @model.get('id')
        if $child.size() > 0
          $child.remove()
        nodeView = new NodeView(newChild, @rootView)
        $nodeHtml = $(nodeView.renderAndAppendTo($childrenContainer, @rootView).el)
        $nodeHtml.show()
      
      side = null
      if $node.hasClass('left') or @model.get('lastAddedChildSide') is 'Left'
        side = 'left'
      else if $node.hasClass('right') or @model.get('lastAddedChildSide') is 'Right'
        side = 'right'

      if $.inArray('ANIMATE_TREE_RESIZE', document.features) > -1
        @alignChildrenofElement($node.children('.children'), side)
        diff = previousHeight - $childrenContainer.outerHeight()
        @resizeTree $node, @model, diff
      else
        @model.get('rootNodeModel').trigger 'refreshDomConnectionsAndBoundaries', side

      children = newChild.get 'children'
      if children.length > 0
        for child in children
          nodeView.changeChildren child

      if refreshConnections
        document.log "refreshing connections"
        @model.updateConnectionsToRoot()
        $.each(@model.get('children'), (index, child)->
          child.updateConnection()
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
      totalChildrenWidth = 0

      if $(element).attr('folded') isnt "true"
        if $children.length > 0
          for child in $children
            childSize = @alignChildrenofElement($(child).children('.children'), sideOfTree, i+1)
            heightOfChildren[$(child).attr('id')] = childSize[0]
            widthOfChildren[$(child).attr('id')] = childSize[1]
            totalChildrenWidth = Math.max(totalChildrenWidth, $(child).outerWidth() + childSize[1])
          
          lastChild = null
          for child in $children
            id = $(child).attr('id')
            childWidth = $(child).outerWidth()
            childHeight = $(child).outerHeight()
            
            diffHeight = 0
            if heightOfChildren[id] > childHeight
              diffHeight = (heightOfChildren[id] - childHeight) / 2
              currentTop += diffHeight
            $(child).css('top', currentTop)
            currentTop += (heightOfChildren[id] - diffHeight) + @verticalSpacer
            
            totalChildrenHeight += heightOfChildren[id] + @verticalSpacer
            
            if sideOfTree == 'left'
              $(child).addClass('left')
              $(child).css('right', @horizontalSpacer)
            else
              $(child).addClass('right')	
              $(child).css('left', @horizontalSpacer) 
            lastChild = child

          # to correct the addition on the last run we subtract the last added height
          currentTop -= @verticalSpacer
          totalChildrenWidth += @horizontalSpacer

          left = elementWidth
          if sideOfTree == 'left'
            left = -totalChildrenWidth
          top = -(totalChildrenHeight/2 - elementHeight/2)
          height = Math.max(totalChildrenHeight, elementHeight)
          width = totalChildrenWidth

          $(childrenContainer).css('left', left+'px')
          $(childrenContainer).css('top', top)
          $(childrenContainer).css('height', height)
          $(childrenContainer).css('width', width)


      if $(element).attr('folded') is 'true'      
        [elementHeight + 0, totalChildrenWidth]
      else
        [Math.max(totalChildrenHeight, elementHeight), totalChildrenWidth]


    render: (@rootView)->
      document.nodeCount++
      @$el.html @template @getRenderData()
      @$el.append(@model.get 'purehtml')
      @$el.attr('folded', @model.get 'folded')

      # in first step: from root to its childs
      if @model.get('parent') isnt undefined and @model.get('parent') isnt null
        @connection = new ConnectionView(@model.get('parent'), @model)
        @connection.renderAndAppendToNode(@$el)

      
      rootModel = @model.get('rootNodeModel')
      if !rootModel.get('isReadonly')
        @controls = new NodeControlsView(@model)
        @controls.renderAndAppendToNode(@)
      
      @makeDraggable()
      
      @

      
    makeDraggable: ()->
      model = @model
      
      $innerNode = $(@$el).find('.inner-node:first')
      if $.inArray('MOVE_NODE', document.features) > -1
        $(@$el).draggable({ opacity: 0.7, helper: "clone", handle: $innerNode.find(".action-move") });
      $innerNode.droppable({
        accept: '.node',
        hoverClass: 'droppable-hover'
        drop: ( event, ui )->
          rootModel = model.get('rootNodeModel')
          
          nodeId = ui.draggable.attr("id")
          node = rootModel.findById(nodeId)
          
          newParentId = $( this ).closest('.node').attr('id')
          
          if node.get('parent').get('id') isnt newParentId
            newParent = rootModel.findById(newParentId)
            $(ui.draggable).remove()
            node.move(newParent)
      })

      

    renderAndAppendTo:($element, @rootView)->
      @render(@rootView)
      @renderOnExpand = false 
        
      $element.append(@$el)
      @alignButtons()

      children = @model.get 'children'
      childsToLoad = @model.get 'childsToLoad'

      
      # if there are already informations about cholds in the model: render them
      if children isnt undefined and children.length > 0
        @recursiveRender(@$el.find('.children:first'), children, @rootView)
        # if this element is not within a folded subtree: update fold status
        if $element.is ':visible'
          @initialFoldStatus()
        else if @model.get 'folded'
          @switchFoldButtons()
          @$el.find('.children:first').toggle()
          @renderOnExpand = true
      # if there will more childs be rendered later: set flag
      else if childsToLoad isnt undefined and childsToLoad.length > 0    
        if @model.get 'folded'
          @switchFoldButtons()
          @$el.find('.children:first').toggle()
          @renderOnExpand = true
          @$el.find('.expand-icon').hide()

          @$el.children('.inner-node').children('.action-fold.loading-icon').toggleClass 'invisible'

      # no childs now nor later: hide fold buttons
      else
        @$el.find('.action-fold').hide()
      @
        
      

    destroy: ->
      @model?.off null, null, @

      # destroy all subviews
      for viewId, view of @subViews
        view.destroy()
      
      $('#'+@model.get('id')).fadeOut(document.fadeDuration, ()=>
        $('#'+@model.get('id')).remove()
        @model.get('rootNodeModel').trigger 'refreshDomConnectionsAndBoundaries'
      )
  

    # pass a final function, if u want to
    leave: (done = ->) ->
      @destroy()
      done()




  module.exports = NodeView