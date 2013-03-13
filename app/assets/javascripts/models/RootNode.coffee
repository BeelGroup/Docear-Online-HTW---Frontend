define ['models/AbstractNode'],  (AbstractNode) ->  
  module = ->
  
  class RootNode extends AbstractNode
    constructor: (id, folded, nodeText, containerID, isHTML, xPos, yPos, hGap, shiftY, locked, mapId, rootNodeView = @) ->
      super id, folded, nodeText, isHTML, xPos, yPos, hGap, shiftY, locked, rootNodeView
      @set 'containerID', containerID
      @set 'leftChildren', []
      @set 'rightChildren', []
      @set 'mapId', mapId

      @bind 'change:selectedNode', => 
        if @get('selectedNode') != null
          $("##{@get 'id' }").trigger 'newSelectedNode', @get 'selectedNode'

          
    
    # overwriting getter @get 'children' since we RootNode does not have a children attr
    get: (attr)->
      if attr == 'children'
        return @getChildren()
      Backbone.Model.prototype.get.call(this, attr);
  
      
    getChildren: ->
      children = []
      children = $.merge(children, @get('leftChildren').slice()  )
      children = $.merge(children, @get('rightChildren').slice()  )
      children
      
    getNextLeftChild: ->
      @getNextChild @get 'leftChildren'
    
    getNextRightChild: ->
      @getNextChild @get 'rightChildren'

    addLeftChild: (child)->
      @get('leftChildren').push(child)
      
    addRightChild: (child)->
      @get('rightChildren').push(child)
    
    addChild: (child, treeSide)->
      if treeSide == 'left'
        addLeftChild child
      else
        addRightChild child

  module.exports = RootNode