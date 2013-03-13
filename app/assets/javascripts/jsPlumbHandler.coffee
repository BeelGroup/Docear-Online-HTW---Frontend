isTest = $("body").hasClass("test-mode")

connectWrapper = ->
  if !isTest
    jsPlumb.connect.apply(this, arguments)

initializeJsPlumb = ->
  STROKE_COLOR = "#487698" #TODO style information should only be in style.less, idea $("#not-visible-example-element").css('color')

  jsPlumb.Defaults.PaintStyle =
    lineWidth: 3,
    strokeStyle: STROKE_COLOR
  jsPlumb.Defaults.Endpoint = ["Dot", { radius:1 }]
  jsPlumb.Defaults.EndpointStyle = { fillStyle:STROKE_COLOR }
  jsPlumb.Defaults.Anchor = ["RightMiddle","LeftMiddle"]
  jsPlumb.Defaults.PaintStyle = { lineWidth: 2, strokeStyle:STROKE_COLOR }
  jsPlumb.Defaults.Connector = [ "StateMachine", { curviness:10 } ] # Bezier causes drawing errors on Firefox 16.0.2 ubuntu


connectNodes = (sourceIdentifier, targetIdentifier) -> 
  $container = $(targetIdentifier).parent()
  connectWrapper({ source:$(sourceIdentifier), target:$(targetIdentifier), container:$container })