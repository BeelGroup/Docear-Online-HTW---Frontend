###
abstract class
###

define [], ()->
  module = () ->

  class AbstractNode extends Backbone.Model 

    constructor:->
      super()    


    activateListeners:->
      @bind 'change:selected', =>
        if(@get('selected'))
          currentlySelected = @get('rootNodeModel').get 'selectedNode'
          if (typeof(currentlySelected) isnt 'undefined') and currentlySelected isnt null
            currentlySelected.set 'selected', false
          @get('rootNodeModel').set 'selectedNode', @

      @bind 'change:folded', =>
        rootID = @get('rootNodeModel').get 'id'
        # is catched in mapview to update mininodes in minimap
        $("##{rootID}").trigger 'newFoldAction'


      @bind 'change',(changes)->
        if $.inArray('SERVER_SYNC', document.features) > -1
          attributesToPersist = @get 'attributesToPersist'
          persistenceHandler = @get 'persistenceHandler'
          
          persistenceHandler.persistChanges @, changes
          @

    lock: (lockedBy) ->
      @set 'lockedBy', lockedBy
      @set 'locked', true
 
 
    unlock: ->
      @set 'locked', false

    # status messages for update
    saveOptions:
      success: (model) ->
        document.log "Node with id '#{model.id}' was updated to the server."
      error: (model, response) ->
        document.log "Error while saving Node with id '#{model.id}': #{response.status} #{response.statusText} (path was #{model.urlRoot})"

    fetchOptions:
      success: (model) ->
        document.log "Node with id '#{model.id}' was fetched from the server."
      error: (model, response) ->
        document.log "Error while fetching Node with id '#{model.id}': #{response.status} #{response.statusText} (path was #{model.urlRoot})"
    
    destroyOptions:
      success: ->
        document.log "Node has been deleted from the permanent record."
      error: (model, response) ->
        document.log "Error: #{response.status} #{response.statusText}"

    getNextChild: (children = (@get('children')))->
      nextChild = null
      getNext = false
      for child in (children)
        if nextChild == null
          nextChild = child
        else if child.get 'previouslySelected'
          nextChild = child
      nextChild
      
    getSelectedNode: ->
      nodes = []
      nodes = $.merge(nodes, @get('children').slice()  )
      
      if @get 'selected'
        return @
      else
        # used to be recursive via child.getSelectedNode() but could create mem problems
        while node = nodes.shift()
          if node.get 'selected'
            return node
          else 
            nodes = $.merge(nodes, node.get('children').slice()  )
      return null
        
    findById: (id)->
      nodes = []
      nodes = $.merge(nodes, @get('children').slice()  )
      if @get('id') == id
        return @
      else 
        while node = nodes.shift()
          if node.get('id') == id
            return node
          else
            nodes = $.merge(nodes, node.get('children').slice())
      return null
    
    getRoot: ()->
      currentNode = @
      while currentNode.constructor.name != 'RootNode'
        currentNode = currentNode.get 'parent'
      currentNode

    getCurrentMapId: ()->
      root = @getRoot()
      root.get 'mapId'
 
    updateConnection: ()->
      @set 'connectionUpdated', (@get('connectionUpdated')+1)
    
    removeCild: (child)->
      document.log 'removing '+child.get('id')+' from '+@get('id')
      children = []
      $.each(@get('children'), (index, node)->
        if node != child
          children.push(node)
      @set 'children', children
      
  module.exports = AbstractNode