define ['models/AbstractNode'],  (AbstractNode) ->  
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

      

    addChild: (id, text)->
      newChild = new Node(id, false, text, false,0,0,0,0,false, @)
      @get('children').push(newChild)
      @set 'lastAddedChild', newChild
      
    createAndAddChild: ()->
      values = {}
      values['mapId'] = @getCurrentMapId 'id'
      values['parentNodeId'] = @get('parent').get 'id'
      @get('persistenceHandler').persistNew(@, values)
      
      
    move: (newParent, position = -1)->
      document.log 'moving '+@get('id')+' to parent '+newParent.get('id')
      #@get('parent').removeCild(@)
      #newParent.addChild(@)
      
      
  module.exports = Node