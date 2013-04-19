define ['views/AbstractNodeView','views/ConnectionView', 'views/NodeControlsView'], (AbstractNodeView, ConnectionView, NodeControlsView) ->
  module = ->
  
  class NodeView extends AbstractNodeView

    tagName: 'div'
    className: 'node' 
    template: Handlebars.templates['Node']
    id: 99

    constructor:(@model) ->
      @id = @model.get 'id'
      super()


    recursiveRender: (parentView, parent, nodes)->
      $.each nodes, (index, node)=>
        nodeView = new NodeView(node)
        nodeView.renderAndAppendTo(parent)
        


    changeChildren: ->
      ## TODO -> render and align new child
      console.log "TODO: render child"
      newChild = @model.get 'lastAddedChild'
      nodeView = new NodeView(newChild)
      $nodeHtml = $($(nodeView.render().el))
      $node = @$el
      
      $node.children('.children').append($nodeHtml)
      
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
        

      [Math.max(totalChildrenHeight, elementHeight), totalChildrenWidth]


    render: ->
      @$el.html @template @getRenderData()
      console.log @$el.height()
      @$el.attr('folded', @model.get 'folded')

      @$el.append(@model.get 'purehtml')

      if @model.get('parent') isnt undefined and @model.get('parent') isnt null
        @connection = new ConnectionView(@model.get('parent'), @model)
        @connection.renderAndAppendToNode(@$el)

      @controls = new NodeControlsView(@model)
      @controls.renderAndAppendToNode(@)

      @


    afterAppend: ->
      @alignButtons()
      @initialFoldStatus()


    renderAndAppendTo:($element)->
      $element.append(@render().$el)

      children = @model.get 'children'
      if children isnt undefined and children.length > 0
        @recursiveRender(@, @$el.find('.children:first'), children)
      else
        @$el.find('.action-fold').hide()

      @afterAppend()

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