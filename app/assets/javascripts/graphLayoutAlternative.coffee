cssForClass = (className, property) ->
  className = className.replace(".","")
  if $("." + className).length < 1
    $("<div id='remove-tmp-element' class='#{className}'>").attr('type','hidden').appendTo('body');
  result = $("." + className).css(property)
  $("#remove-tmp-element").remove()
  result

isTest = $("body").hasClass("test-mode")

connectWrapper = ->
  if !isTest
    jsPlumb.connect.apply(this, arguments)

initializeJsPlumb = ->
  STROKE_COLOR = "#ff0000" #TODO style information should only be in style.less, idea $("#not-visible-example-element").css('color')

  jsPlumb.Defaults.PaintStyle =
    lineWidth: 3,
    strokeStyle: STROKE_COLOR
  jsPlumb.Defaults.Endpoint = ["Dot", { radius:1 }]
  jsPlumb.Defaults.EndpointStyle = { fillStyle:STROKE_COLOR }
  jsPlumb.Defaults.Anchor = ["RightMiddle","LeftMiddle"]
  jsPlumb.Defaults.PaintStyle = { lineWidth: 2, strokeStyle:STROKE_COLOR }
  jsPlumb.Defaults.Connector = [ "StateMachine", { curviness:10 } ] # Bezier causes drawing errors on Firefox 16.0.2 ubuntu
  $(window).resize ->
    jsPlumb.repaintEverything()

_height = ($element) -> $element.outerHeight()  #correct to choose ,height() <-- too small, innerHeight(), outerHeight()

class MindMap
  #
  # Constructs a MindMap.
  # @param [String] content of the root node (text or HTML)
  #
  constructor: (@content = "") ->
    @leftChildren = []
    @rightChildren = []
    @root = {}

  getContent: () -> @content

  _append: (children, node) -> children.push node

  #
  # Appends a childnode on the left side.
  # @param [Node] node the node to appand
  #
  appendLeft: (node) -> @_append @leftChildren, node

  #
  # Appends a childnode on the right side.
  # @param [Node] node the node to appand
  #
  appendRight: (node) -> @_append @rightChildren, node

#
#  A node in a MindMap
#
class Node
  #
  # Constructs a node.
  # @param [String] content of the node (text or HTML)
  # @param [Node[]] children childnotes
  #
  constructor: (@content = "", @children = []) ->
    @view = null

  getContent: () -> @content

$.fn.extend
  docear: ->
    $element = this

    dimension: -> width: $element.width(), height: _height($element)

    coordinatesForHorizontalAlignmentInBox: ($box) -> $box.width() / 2 - $element.width()
    coordinatesForVerticalAlignmentInBox: ($box) -> _height($box) / 2 - _height($element)
    centerInBox: ($box) ->
      newX = @coordinatesForHorizontalAlignmentInBox $box
      newY = @coordinatesForVerticalAlignmentInBox $box
      @move left: newX, top: newY

    #
    # moves this element to new coordinates
    # @param [Object] target coordinates like {left: 0, top: 0}
    move: (coordinates = {left: 0, top: 0}) ->
      if coordinates.left then $element.css("left", coordinates.left)
      if coordinates.top then $element.css("top", coordinates.top)

class MindMapDrawer
  constructor: (@mindMap, @$target) ->
    #jsPlumb.Defaults.Container = $target;
    @x = ""
    @childId = 1 #TODO use node id
    @verticalSpacer = 10

  _drawBox = (content, className, attributes = {}) ->
    "<div #{asXmlAttributes(attributes)} class='node #{className}'><div class='inner-node'>#{content}</div><div class='children'></div><i class='icon-minus-sign fold'></i></div>"


  getCenterCoordinates = ($element) ->
    left = $element.position().left + $element.width() / 2
    top = $element.position().top + _height($element) / 2
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
  # Draws the mind map into a jQuery selected field
  # @param [jQuery] $target selected field where to draw the mind map
  #
  draw: ($target = @$target) ->
    $root = @drawRoot $target
    returnVal = @drawChildren $root, $target
    returnVal
    
  #
  # Refresh the mind map an reposition the dom elements
  #
  refreshDom: () ->
    root = $('#root')
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
  
  #draws children without layouting for graph
  _drawRecursiveChildren: ($relativeRootNode, children, $target, treeIdentifier) ->
    for child in children
      id = "child-#{@childId}"
      
      className = treeIdentifier
      className = if child.folded then className+", folded" else className
      childNode = _drawBox(child.getContent(), className, {id: id, style: "font-size: "+fontSize*zoom+"px; line-height: "+fontSize*zoom+"px;"})
      $(childNode).addClass(treeIdentifier)
      
      $relativeRootNode.find('.children:first').append childNode
      $child = $("#" + id)
      child.view = $child
      @childId++
      @_drawRecursiveChildren $child, child.children, $target, treeIdentifier
      
      

  _getRecursiveHeight: (element, childrenOfElement) =>
    heightOfAllChildren = _.reduce(childrenOfElement, ((memo, child) => memo + @_getRecursiveHeight(child, child.children)), 0) + (childrenOfElement.length - 1) * @verticalSpacer
    elementHeight = _height(element.view)
    result = Math.max(elementHeight, heightOfAllChildren)
    result


  # draws children of mind map root node
  # @param [jQuery] mind map root node
  # @param [jQuery] $target selected field where to draw the mind map
  drawChildren: ($root, $target) ->
    horizontalSpacer = 40

    connectNodes = (source, target) -> 
      $container = $('#'+source.view.attr("id")+" .children:first")
      connectWrapper({ source:source.view, target:target.view, container:$container })

    #precondition: parent node has correct position
    positionFromTopRecursive = (parent, children) =>
      treeHeight = @_getRecursiveHeight parent, children
      parentCenterTop = getCenterCoordinates(parent.view).top
      newTop = -parent.view.height()/2 - 0.5 * treeHeight
      currentTop = newTop
      $.each children, (indexInArray, child) =>
        subTreeHeight = @_getRecursiveHeight child, child.children
        top = currentTop + subTreeHeight / 2 - _height(child.view) / 2 #put node in the middle of the sub tree
        child.view.css("top", top)
        connectNodes parent, child
        currentTop += subTreeHeight + @verticalSpacer
        positionFromTopRecursive child, child.children

    positionHorizontalFromParentRecursive = (parent, children, direction) =>
      for child in children
        $foldIcon = $(child.view).children('i.fold:first')
        $foldIcon.css("top", child.view.height()/2)
        if direction == "left"
          left = - horizontalSpacer - child.view.width()
          if child.children.length > 0
            $foldIcon.css("left", -$foldIcon.width()/2)
          else
            $foldIcon.remove()
        else
          left = parent.view.width() + horizontalSpacer
          if child.children.length > 0
            $foldIcon.css("right", -$foldIcon.width()/2)
          else
            $foldIcon.remove()
        child.view.css("left", left)
        positionHorizontalFromParentRecursive child, child.children, direction

    #TODO hide, position, then unhide
    @_drawRecursiveChildren $root, @mindMap.rightChildren, $target, 'rightTree'
    @_drawRecursiveChildren $root, @mindMap.leftChildren, $target, 'leftTree'

    positionHorizontalFromParentRecursive @mindMap.root, @mindMap.rightChildren, "right"
    positionHorizontalFromParentRecursive @mindMap.root, @mindMap.leftChildren, "left"
    positionFromTopRecursive @mindMap.root, @mindMap.rightChildren
    positionFromTopRecursive @mindMap.root, @mindMap.leftChildren



  # draws the root node and returns it
  # @param [jQuery] target element where to draw the elements
  # @return [jQuery] the root note
  drawRoot: ($target) ->
    rootNodeId = "root" #TODO find better system for ids
    rootNode = _drawBox(@mindMap.getContent(), "root-node",{id:rootNodeId, style: "font-size: "+fontSize*zoom+"px; line-height: "+fontSize*zoom+"px;"})
    $target.append(rootNode)
    $root = $("#" + rootNodeId)
    $root.docear().centerInBox $root.parent()
    root.view = $root
    @mindMap.root.view = $root
    $root

#
#  Converts a flat object to a xml style list of attributes.
#  Example:
#    input: {"href":"/index.html", "title" : "the title"}
#    output: "href='/index.html' title='the title""
#
asXmlAttributes = (attributeDocument) ->
  result = for key, value of attributeDocument
    "#{key}='#{value}'"
  iterator = (x, y) -> x + " " + y
  _.reduceRight(result,  iterator, "")