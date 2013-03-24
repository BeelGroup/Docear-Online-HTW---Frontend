define ['models/Node'],  (NodeModel) ->  
    module = ->
    class MindMapPositioner extends Backbone.Model 
      constructor: (target, rootNode) ->
        super()
        @set 'target', target
        @set 'rootNode', rootNode	
        @verticalSpacer = 10
        @sup = MindMapPositioner.__super__
        
        childrenRight = rootNode.get 'rightChildren'
        childrenLeft = rootNode.get 'leftChildren'
        
        adjustNodeHierarchy(rootNode, childrenRight, 'rightTree')
        adjustNodeHierarchy(rootNode, childrenLeft, 'leftTree')
        
        rootId = rootNode.get 'id'
        $root = $('#'+rootId)
        
        $root.addClass('root')
        centerNodeInContainer(target, $root)
        
        @refreshDom()
    
      adjustNodeHierarchy = (parent, children, treeIdentifier)->
        parentId = parent.get 'id'
        $parent = $('#'+parentId)
        
        $.each(children, (index, node)->
          childId = node.get 'id'	
          $child = $('#'+childId)
          $child.addClass(treeIdentifier)
          children = node.get 'children'
          if children != undefined
            adjustNodeHierarchy(node, children)
          $parent.children('.children:first').append($child)
          connectNodes $parent, $child
        )
        
        
      centerNodeInContainer = (container, node)->
        containerWidth = $(container).width();
        containerHeight = $(container).width();
        nodeWidth = $(node).width()
        nodeHeight = $(node).width()
        $(node).css('left', containerWidth/2-nodeWidth/2)
        $(node).css('top', containerHeight/2-nodeHeight/2)
      
      getCenterCoordinates = ($element) ->
        left = $element.position().left + $element.width() / 2
        top = $element.position().top + $element.height() / 2
        top: top, left: left
    
      fontSize = 14
      # fontSize = parseInt($(".inner-node:first").css("font-size").replace("px", ""))
      zoom = 1
      setZoom: (zoomFactor) ->
        zoom = zoomFactor
        $('#mindmap .node').css("font-size", fontSize*zoom+"px")
        $('#mindmap .node').css("line-height", fontSize*zoom+"px")
        @refreshDom
      
      #
      # Refresh the mind map an reposition the dom elements
      #
      refreshDom: () ->
        root = $('.root:first')
        height = @_alignChildren root
        jsPlumb.repaintEverything()
        height
      
      _alignChildren: (element) ->
        horizontalSpacer = 20
        $children = $(element).children('.children').children('.node')
        elementHeight = $(element).outerHeight()
        elementWidth = $(element).outerWidth()
        heightOfChildren = {}
        parentCenterTop = getCenterCoordinates($(element)).top
        totalChildrenHeight = 0
        
        currentLeftTop = 0
        currentRightTop = 0
        currentTop = 0
        if $children.length > 0
          for child in $children
            childHeight = @_alignChildren(child) 
            heightOfChildren[$(child).attr('id')] = childHeight
            totalChildrenHeight = totalChildrenHeight + childHeight + @verticalSpacer
          
          lastChild = null
          for child in $children
            $(child).css('border', "3px dotted #0000FF")
            if $(child).hasClass('leftTree')
              $(child).css("left", -$(child).outerWidth() - horizontalSpacer)
              $(child).css("top", currentLeftTop)
              currentLeftTop = currentLeftTop + heightOfChildren[$(child).attr('id')]
            else
              $(child).css("left", elementWidth + horizontalSpacer) 
              $(child).css("top", currentRightTop)
              currentRightTop = currentRightTop + heightOfChildren[$(child).attr('id')]
            lastChild = child
          currentTop = Math.max(currentRightTop, currentLeftTop) + heightOfChildren[$(lastChild).attr('id')]
          $(element).children('.children:first').css('top', -currentTop/2 + elementHeight/2)
        Math.max(currentTop, elementHeight)
  
    module.exports = MindMapPositioner