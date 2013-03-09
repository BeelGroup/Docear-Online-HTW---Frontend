define ['models/AbstractNode'],  (AbstractNode) ->  
  module = ->

  class Node extends AbstractNode
    constructor: (id, folded, nodeText, isHTML, xPos, yPos, hGap, shiftY, locked, parent, rootNodeModel) ->
      super id, folded, nodeText, isHTML, xPos, yPos, hGap, shiftY, locked, rootNodeModel  
      @set 'children', []
      @set 'parent', parent
      
    addChild: (id, text)->
      newChild = new Node(id, false, text, false,0,0,0,0,false, @)
      children = @get('children').slice()
      children.push(newChild)
      @set 'children', children
      
    createAndAddChild: ()->
      values = {}
      values['mapId'] = @getCurrentMapId 'id'
      values['parentNodeId'] = @get('parent').get 'id'
      @get('persistenceHandler').persistNew(@, values)
  module.exports = Node