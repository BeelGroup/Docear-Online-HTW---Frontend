define ['models/AbstractNode'],  (AbstractNode) ->  
  module = ->

  class Node extends AbstractNode
    constructor: (id, folded, nodeText, isHTML, xPos, yPos, hGap, shiftY, locked, parent, rootNodeModel) ->
      super id, folded, nodeText, isHTML, xPos, yPos, hGap, shiftY, locked, rootNodeModel  
      @set 'children', []
      @set 'parent', parent

    fold:()->
      $nodeToFold = $('#'+(@get 'id'))
      @set 'folded', $nodeToFold.children('.children').is(':visible')
      
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