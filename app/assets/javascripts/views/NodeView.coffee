define ['logger','views/AbstractNodeView','views/ConnectionView', 'views/NodeControlsView'], (logger, AbstractNodeView, ConnectionView, NodeControlsView) ->
  module = ->
  
  class NodeView extends AbstractNodeView

    tagName: 'div'
    className: 'node' 
    template: Handlebars.templates['Node']
    id: 99

    constructor:(@model, @rootView) ->
      @id = @model.get 'id'
      super()


    recursiveRender: (parent, nodes, @rootView)->
      if not document.cancel_loading
        $.each nodes, (index, node)=>
          nodeView = new NodeView(node, @rootView)
          nodeView.renderAndAppendTo(parent, @rootView)
        

    changeChildren: ->
      $node = $(@$el)
      $childrenContainer = $node.children('.children')
      previousHeight = $childrenContainer.outerHeight()
      
      newChild = @model.get 'lastAddedChild'
      nodeView = new NodeView(newChild, @rootView)
      $nodeHtml = $($(nodeView.render().el))
      
      $childrenContainer.append($nodeHtml)
      $nodeHtml.show()
      
      if $.inArray('ANIMATE_TREE_RESIZE', document.features) > -1
        side = 'right'
        if $node.hasClass('left')
          side = 'left'
        @alignChildrenofElement($node.children('.children'), side)
        diff = previousHeight - $childrenContainer.outerHeight()
        @resizeTree $node, @model, diff
      else
        @get('rootNodeModel').trigger 'refreshDomConnectionsAndBoundaries'
      
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

      if $children.length > 0
        for child in $children
          childSize = @alignChildrenofElement($(child).children('.children'), sideOfTree, i+1)
          heightOfChildren[$(child).attr('id')] = childSize[0]
          widthOfChildren[$(child).attr('id')] = childSize[1]
          totalChildrenWidth = Math.max(totalChildrenWidth, $(child).outerWidth() + childSize[1])
        
        lastChild = null
        for child in $children
          id = $(child).attr('id')
          totalChildrenHeight += heightOfChildren[id] + @verticalSpacer
          
          if lastChild == null
            currentTop = -$(child).outerHeight()/2
          currentTop += heightOfChildren[id]/2
          $(child).css('top', currentTop)

          
          if sideOfTree == 'left'
            $(child).addClass('left')
            $(child).css('right', @horizontalSpacer)
          else
            $(child).addClass('right')	
            $(child).css('left', @horizontalSpacer) 
          lastChild = child
          currentTop += heightOfChildren[id]/2 + @verticalSpacer

        # to correct the addition on the last run we subtract the last added height
        currentTop = currentTop - heightOfChildren[$(lastChild).attr('id')] - @verticalSpacer
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
        #diff = Math.max(totalChildrenHeight, elementHeight) - Math.min(totalChildrenHeight, elementHeight)
        [elementHeight + 0, totalChildrenWidth]
        #[elementHeight + @verticalSpacer, totalChildrenWidth]
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

      @controls = new NodeControlsView(@model)
      @controls.renderAndAppendToNode(@)
    
      @



    renderAndAppendTo:($element, @rootView)->
      @render(@rootView)
      @renderOnExpand = false 
      
      if @controls.movable
        $(@$el).draggable({ opacity: 0.7, helper: "clone", handle: ".action-move" });
        $(@$el).find('.inner-node:first').droppable({
          accept: '.node',
          hoverClass: 'droppable-hover'
          drop: ( event, ui )->
            newParentId = $( this ).closest('.node').attr('id')
        })

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


        
      

    destroy: ->
      @model?.off null, null, @

      # destroy all subviews
      for viewId, view of @subViews
        view.destroy()

      $('#'+@get('id')).fadeOut(document.fadeDuration, ()=>
        $('#'+@.get('id')).remove()
        @.get('rootNodeModel').trigger 'refreshDomConnectionsAndBoundaries'
      )
  

    # pass a final function, if u want to
    leave: (done = ->) ->
      @destroy()
      done()




  module.exports = NodeView