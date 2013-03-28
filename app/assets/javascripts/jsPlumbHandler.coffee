isTest = $("body").hasClass("test-mode")

getCurrentZoomAmount = ($element)->
  zoom = 1

  if !$.browser.msie 
    zoomAttributes = []
    zoomAttributes.push $($element).css('-webkit-transform')
    zoomAttributes.push $($element).css('-o-transform')
    zoomAttributes.push $($element).css('-moz-transform')
    zoomAttributes.push $($element).css('-ms-transform')
    
    for attr in zoomAttributes
      if attr != 'none' and attr != undefined
        startSub = attr.indexOf("(")+1
        zoom = parseFloat(attr.substr(startSub).split(',')[0].trim())
        break
  return zoom
  
connectNodes = (sourceIdentifier, targetIdentifier, zoom = 1) ->
  $connectionContainer = $(targetIdentifier).children('.connection')
  
  sourceWidth = $(sourceIdentifier).outerWidth()
  sourceHeight = $(sourceIdentifier).outerHeight()
  sourceLeft = $(sourceIdentifier).offset().left/zoom
  sourceTop = $(sourceIdentifier).offset().top/zoom

  targetHeight = $(targetIdentifier).outerHeight()
  targetWidth = $(targetIdentifier).outerWidth()
  targetLeft = $(targetIdentifier).offset().left/zoom
  targetTop = $(targetIdentifier).offset().top/zoom

  strokeWidth = document.graph.defaultWidth
  strokeColor = document.graph.defaultColor
  drawBezier = true
  
  sourceEndpoint = 
    x : 0
    y : sourceHeight/2
  targetEndpoint = 
    x : 0
    y : targetHeight/2
  
  
  absoluteSourceEndpoint =
    x : sourceLeft
    y : sourceTop+sourceEndpoint.y
  absoluteTargetEndpoint =
    x : targetLeft
    y : targetTop+targetEndpoint.y

      
  connectionContainer = 
    x : 0 
    y : 0 
    width : 0 
    height : 0
    
  connection = 
    startX : 0
    startY : strokeWidth
    endX : 0
    endY : strokeWidth
    
  isRight = $(targetIdentifier).hasClass('right')
  isTop = absoluteSourceEndpoint.y > absoluteTargetEndpoint.y
  
  if isRight
    sourceEndpoint.x = sourceWidth
    absoluteSourceEndpoint.x = sourceLeft + sourceWidth
    connectionContainer.x = absoluteSourceEndpoint.x - absoluteTargetEndpoint.x
    
    # top right
    if isTop
      connectionContainer.y = targetEndpoint.y
    else
      connectionContainer.y = absoluteSourceEndpoint.y - absoluteTargetEndpoint.y + targetEndpoint.y
  else
    targetEndpoint.x = targetWidth
    absoluteTargetEndpoint.x = targetLeft + targetWidth
    connectionContainer.x = targetEndpoint.x
    
    # top left
    if isTop
      connectionContainer.y = targetEndpoint.y
    else
      connectionContainer.y = absoluteSourceEndpoint.y - absoluteTargetEndpoint.y + targetEndpoint.y

  connectionContainer.y -= strokeWidth
      
  connectionContainer.width = Math.abs(absoluteSourceEndpoint.x - absoluteTargetEndpoint.x)
  connectionContainer.height = Math.abs(absoluteSourceEndpoint.y - absoluteTargetEndpoint.y) + strokeWidth

  connection.endX = connectionContainer.width
  if (isRight and !isTop) or (!isRight and isTop)
    connection.endY = connectionContainer.height - strokeWidth
  else
    connection.startY = connectionContainer.height - strokeWidth

    

  if isRight
    $connectionContainer.css(  'left', """#{connectionContainer.x}px""")
  else
    $connectionContainer.css(  'right', """-#{connectionContainer.width}px""")
  $connectionContainer.css(   'top', """#{connectionContainer.y}px""")
  $connectionContainer.css( 'width', """#{connectionContainer.width}px""")
  $connectionContainer.css( 'height', """#{Math.max(connectionContainer.height, 15)}px""")
  
  middleX = connection.endX/2
  if (isRight and !isTop) or (!isRight and isTop)
    middleY = connection.endY/2
    control1Y = connection.startY + Math.min(middleX, middleY)
    control2Y = connection.endY - Math.min(middleX, middleY)
  else
    middleY = connection.startY/2
    control1Y = connection.startY - Math.min(middleX, middleY)
    control2Y = connection.endY + Math.min(middleX, middleY)
  
  if Raphael.svg
    $($connectionContainer).svg('destroy');
    $($connectionContainer).svg()
    svg = $($connectionContainer).svg('get'); 
    path = svg.createPath();
    
    bezierControls = [[middleX, connection.startY, middleX, connection.startY, middleX, control1Y],[middleX, control1Y, middleX, control2Y, middleX, control2Y],[middleX, connection.endY, middleX, connection.endY, connection.endX, connection.endY]]
    pathNode = svg.path(null, path.move(connection.startX, connection.startY).curveC(bezierControls), {fill: 'none', stroke: strokeColor, strokeWidth: strokeWidth}, true)
  else
    $($connectionContainer).empty()
    pathString = """M#{connection.startX},#{connection.startY}C#{middleX},#{connection.startY} #{middleX},#{connection.startY} #{middleX},#{control1Y} #{middleX},#{control1Y} #{middleX},#{control2Y} #{middleX},#{control2Y} #{middleX},#{connection.endY} #{middleX},#{connection.endY} #{connection.endX},#{connection.endY}"""
    
    # even if size of $connectionContainer is 1 DOM element must be passed via ".get(0)"
    paper = Raphael($($connectionContainer).get(0), connectionContainer.width, connectionContainer.height)
    pathNode = paper.path(pathString).attr({
      "stroke" : strokeColor
      "stroke-width" : strokeWidth
      "fill" : "None"
      "fill-opacity": "0"
    })  
    
updateTree = ($node, currentZoom)->
  $parent = $($node).parent().closest('.node')
  connectNodes($parent, $node, currentZoom)
  