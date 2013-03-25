test 'MindMap.append left', ->
  mindMap = new MindMap
  equal(0, mindMap.leftChildren.length, 'no children should be there')
  mindMap.appendLeft new Node "left 1"
  equal(1, mindMap.leftChildren.length, 'child should be there')

test 'MindMap.append right', ->
  mindMap = new MindMap
  equal(0, mindMap.rightChildren.length, 'no children should be there')
  mindMap.appendRight new Node "right 1"
  equal(1, mindMap.rightChildren.length, 'child should be there')

test 'load css properties from element not existing but with CSS declarations', ->
  ok(cssForClass("css-test-element","color") in ["rgb(255, 170, 204)", "#ffffcc"], "there should not be an existing element in the template but function gets the correct value anyway")
  ok(cssForClass("css-test-element-control","color") in ["rgb(255, 255, 204)", "#ffffcc"], "old jquery function should stil work as getter")
  $(".css-test-element-control").css("color", "#000000")
  ok(cssForClass(".css-test-element-control","color") in [ "rgb(0, 0, 0)", "#000000"], "old jquery function should stil work as setter")