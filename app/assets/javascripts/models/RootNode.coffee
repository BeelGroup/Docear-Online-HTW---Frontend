define ['logger', 'models/AbstractNode'],  (logger, AbstractNode) ->  
  module = ->
  
  class RootNode extends AbstractNode
    constructor:->
      super()
      @typeName = 'rootModel'
      @sup = RootNode.__super__


    activateListeners:->
      # will be catched in canvasView
      #  -> center to selected node if necessary
      @bind 'change:selectedNode', => 
        if @get('selectedNode') isnt null and (typeof(@get('selectedNode')) isnt 'undefined')
          $("##{@get 'id' }").trigger 'newSelectedNode', @get 'selectedNode'

      # call function in super class      
      @sup.activateListeners.apply @


    setEdgestyle: (edgeStyle) ->
      # set default values in root
      if edgeStyle is undefined or edgeStyle is null
        edgeStyle = {width: document.graph.defaultWidth, color: document.graph.defaultColor}

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