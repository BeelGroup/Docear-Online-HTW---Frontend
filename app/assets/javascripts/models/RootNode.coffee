define ['models/AbstractNode'],  (AbstractNode) ->  
  module = ->
  
  class RootNode extends AbstractNode
    constructor: (id, folded, nodeText, containerID, isHTML, xPos, yPos, hGap, shiftY, locked, mapId, edgeStyle,rootNodeView = @) ->
      super id, folded, nodeText, isHTML, xPos, yPos, hGap, shiftY, locked, rootNodeView
      @set 'containerID', containerID
      @set 'leftChildren', []
      @set 'rightChildren', []
      @set 'mapId', mapId

      @setEdgestyle edgeStyle

      # will be catched in canvasView
      @bind 'change:selectedNode', => 
        if @get('selectedNode') isnt null and (typeof(@get('selectedNode')) isnt 'undefined')
          # catched in canvasview -> center to selected node if necessary
          #console.log @get('selectedNode')
          $("##{@get 'id' }").trigger 'newSelectedNode', @get 'selectedNode'


    setEdgestyle: (edgeStyle) ->
      # set default values in root
      if edgeStyle is undefined or edgeStyle is null
        edgeStyle = {width: document.graph.defaultWidth, color: document.graph.defaultColor}

      document.log 'toDo: translate color in RootNode.coffee: 27'
      edgeStyle.color = document.graph.defaultColor
      @set 'edgeStyle', edgeStyle
 

    # overwriting getter @get 'children' since we RootNode does not have a children attr
    get: (attr)->
      if attr == 'children'
        return @getChildren()
      Backbone.Model.prototype.get.call(this, attr);

    selectNone:->
      currentlySelected = @.get 'selectedNode'

      if (typeof(currentlySelected) isnt 'undefined') and currentlySelected isnt null
        currentlySelected.set 'selected', false
      @set 'selectedNode', null    

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
      @set 'lastAddedChild', newChild
      
    addRightChild: (child)->
      @get('rightChildren').push(child)
      @set 'lastAddedChild', newChild
    
    addChild: (child, treeSide)->
      if treeSide == 'left'
        addLeftChild child
      else
        addRightChild child

    updateAllConnections: ->
      nodes = []
      nodes = $.merge(nodes, @get('children').slice()  )
      # used to be recursive via child.getSelectedNode() but could create mem problems
      while node = nodes.shift()
        node.updateConnection()
        nodes = $.merge(nodes, node.get('children').slice()  )
      
  module.exports = RootNode