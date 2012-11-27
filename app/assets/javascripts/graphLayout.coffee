cssForClass = (className, property) ->
  className = className.replace(".","")
  if $("." + className).length < 1
    $("<div id='remove-tmp-element' class='#{className}'>").attr('type','hidden').appendTo('body');
  result = $("." + className).css(property)
  $("#remove-tmp-element").remove()
  result

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

    dimension: -> width: $element.width(), height: $element.height()

    coordinatesForHorizontalAlignmentInBox: ($box) -> $box.width() / 2 - $element.width()
    coordinatesForVerticalAlignmentInBox: ($box) -> $box.height() / 2 - $element.height()
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
    @x = ""
    @childId = 1 #TODO use node id

  _drawBox = (content, attributes = {}) ->
    "<div #{asXmlAttributes(attributes)} class='node'>#{content}</div>"


  getCenterCoordinates = ($element) ->
    left = $element.position().left + $element.width() / 2
    top = $element.position().top + $element.height() / 2
    top: top, left: left

  #
  # Draws the mind map into a jQuery selected field
  # @param [jQuery] $target selected field where to draw the mind map
  #
  draw: ($target = @$target) ->
    $root = @drawRoot $target
    @drawRight $root, $target

  #draws children without layouting for graph
  _drawRecursiveChildren: ($relativeRootNode, children, $target) ->
    for child in children
      id = "child-#{@childId}"
      $target.append _drawBox(child.getContent(), {id: id})
      $child = $("#" + id)
      child.view = $child
      @childId++
      @_drawRecursiveChildren $child, child.children, $target

  _getRecursiveHeight: (element, childrenOfElement) ->
    self = this
    heightOfAllChildren = _.reduce(childrenOfElement, ((memo, child) -> memo + self._getRecursiveHeight(child, child.children)), 0)
    elementHeight = element.view.innerHeight()
    result = Math.max(elementHeight, heightOfAllChildren)
    result


  # draws right children of mind map root node
  # @param [jQuery] mind map root node
  # @param [jQuery] $target selected field where to draw the mind map
  drawRight: ($root, $target) ->
    children = @mindMap.rightChildren
    horizontalSpacer = 40
    verticalSpacer = 40
    moveRightOfParentNode = ($parent, $child) ->
      left = $parent.position().left + $parent.width() + horizontalSpacer
      $child.css("left", left)

    #TODO hide, position, then unhide
    @_drawRecursiveChildren $root, children, $target

    for child in children
      moveRightOfParentNode $root, child.view

    heightOfAllChildren = @_getRecursiveHeight @mindMap.root, @mindMap.rightChildren
    topPoisitionFirstChild = getCenterCoordinates($root).top - heightOfAllChildren / 2
    currentTop = topPoisitionFirstChild

    connectNodes = (source, target) -> jsPlumb.connect({ source:source.view, target:target.view })

    parent = @mindMap.root
    $.each children, (indexInArray, child) =>
      $child = child.view
      $parent = parent.view
      $child.css("top", currentTop)
      currentTop += @_getRecursiveHeight child, child.children
      connectNodes parent, child
      for subchild in child.children
        moveRightOfParentNode $child, subchild.view


  # draws the root node and returns it
  # @param [jQuery] target element where to draw the elements
  # @return [jQuery] the root note
  drawRoot: ($target) ->
    rootNodeId = "root" #TODO find better system for ids
    $target.append(_drawBox(@mindMap.getContent(), {id:rootNodeId, style: ""}))
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
  result.reduceRight ((x, y) -> x + " " + y ), ""