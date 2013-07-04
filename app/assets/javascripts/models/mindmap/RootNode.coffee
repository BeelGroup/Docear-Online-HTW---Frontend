define ['logger', 'models/mindmap/AbstractNode'],  (logger, AbstractNode) ->  
  module = ->
  
  class RootNode extends AbstractNode
    constructor:->
      super()
      @allNodes = new Array()
      @parentsToLoad = new Array()
      @unfinishedNodes = {}
      @typeName = 'rootModel'
      @sup = RootNode.__super__

    addNodeToList:(node)->
      @allNodes.push node

    getNodeList:->
      @allNodes

    addNodetoUnfinishedList:(id, parentNode)->
      @unfinishedNodes[id] = parentNode

    addParentToParentToLoadList:(parentNode)->
      @parentsToLoad.push parentNode

    getParentsToLoadSize:->
      @parentsToLoad.length

    getNextParentToLoad:->
      @parentsToLoad.shift()

    getUnfinishedNodes:->
      @unfinishedNodes

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
      
    addRightChild: (child)->
      @get('rightChildren').push(child)
    
    addChild: (child, treeSide)->
      if treeSide is 'Left'
        @addLeftChild child
      else
        @addRightChild child

      @set 'lastAddedChildSide', treeSide
      @set 'lastAddedChild', child

    createAndAddChild: (treeSide = "Left")->
      values = {}
      values['mapId'] = @getCurrentMapId 'id'
      values['parentNodeId'] = @get 'id'
      values['side'] = treeSide
      @get('persistenceHandler').persistNew(@, values)
      
    updateAllConnections: ->
      for node in @allNodes
        node.updateConnection()
    
    removeChildBySide: (child, sideIdentifier)->
      children = []
      for node in @get(sideIdentifier)
        if node.get('id') != child.get('id')
          children.push(node)
        else
          document.log "removing #{child.get("id")} from root #{sideIdentifier} (#{@get("id")})"
      @set sideIdentifier, children
    
    removeChild: (child)->
      @removeChildBySide(child, 'leftChildren')
      @removeChildBySide(child, 'rightChildren')
      
  module.exports = RootNode