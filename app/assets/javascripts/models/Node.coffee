define ['models/AbstractNode'],  (AbstractNode) ->  
  module = ->

  class Node extends AbstractNode
    constructor: (id, folded, nodeText, isHTML, xPos, yPos, hGap, shiftY, locked, parent, rootNodeModel, edgeStyle) ->
      super id, folded, nodeText, isHTML, xPos, yPos, hGap, shiftY, locked, rootNodeModel  
      @set 'children', []
      @set 'parent', parent

      @setEdgestyle edgeStyle
      
    setEdgestyle: (edgeStyle) ->
      # get values from parent if no new values were passed
      if edgeStyle is undefined or edgeStyle is null
        edgeStyle = (@get 'parent').get 'edgeStyle'

      #document.log 'toDo: translate color in Node.coffee: 18'
      #edgeStyle.color = document.graph.defaultColor
      @set 'edgeStyle', edgeStyle

      

    addChild: (id, text)->
      newChild = new Node(id, false, text, false,0,0,0,0,false, @)
      @get('children').push(newChild)
      @set 'lastAddedChild', newChild
      
    createAndAddChild: ()->
      values = {}
      values['mapId'] = @getCurrentMapId 'id'
      values['parentNodeId'] = @get('parent').get 'id'
      @get('persistenceHandler').persistNew(@, values)
  module.exports = Node