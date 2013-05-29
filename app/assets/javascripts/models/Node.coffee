define ['logger', 'models/AbstractNode'],  (logger, AbstractNode) ->  
  module = ->

  class Node extends AbstractNode
    constructor:->
      super() 
      @typeName = 'nodeModel'

      
    setEdgestyle: (edgeStyle) ->
      # get values from parent if no new values were passed
      if edgeStyle is undefined or edgeStyle is null
        edgeStyle = (@get 'parent').get 'edgeStyle'

      @set 'edgeStyle', edgeStyle

      

    addChild: (newChild)->
      if @get('children') is undefined
        @set 'children', []
      @get('children').push(newChild)
      newChild.set 'parent', @
      @set 'lastAddedChild', newChild
      
    createAndAddChild: ()->
      values = {}
      values['mapId'] = @getCurrentMapId 'id'
      values['parentNodeId'] = @get 'id'
      @get('persistenceHandler').persistNew(@, values)
      
      
    move: (newParent, position = -1)->
      document.log 'moving '+@get('id')+' to parent '+newParent.get('id')
      
      @get('parent').removeChild(@)
      newParent.addChild(@)
      
      $.each(newParent.get('children'), (index, child)->
        child.updateConnection()
      )
      @get('rootNodeModel').trigger 'refreshDomConnectionsAndBoundaries'
      @updateConnection()

      
    removeChild: (child)->
      currentChildren = @get('children')
      children = []
      
      for node in currentChildren
        if node.get('id') != child.get('id')
          children.push(node)
        else
          document.log 'removing '+child.get('id')+' from '+@get('id')
      @set 'children', children
      
  module.exports = Node