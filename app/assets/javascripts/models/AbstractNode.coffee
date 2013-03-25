###
abstract class
###

define ['handlers/PersistenceHandler'], (PersistenceHandler)->
  module = () ->

  class AbstractNode extends Backbone.Model 

    constructor: (id, folded, nodeText, isHTML, xPos, yPos, hGap, shiftY, locked, rootNodeModel) ->
      super()    
      @set 'id', id
      @set 'folded', folded
      @set 'nodeText', nodeText
      @set 'isHTML', isHTML
      @set 'xPos', xPos
      @set 'yPos', yPos
      @set 'hGap', hGap
      @set 'shiftY', shiftY
      @set 'locked', locked

      @set 'rootNodeModel', rootNodeModel
      
      @set 'selected', false
      @set 'previouslySelected', false
      @set 'foldable', ($.inArray('FOLD_NODE', document.features) > -1)
      @set 'lastAddedChild', 'undefined'
      
      ## THROW events on all (also possible: save/update/change)
      #@on 'all', (event) -> console.log "Event: " + event
      @sup = AbstractNode.__super__

      @set 'persistenceHandler', (new PersistenceHandler())
      @set 'attributesToPersist', ['folded', 'nodeText', 'isHTML', 'locked']
      
      @bind 'change:selected', =>
        if(@get('selected'))
          currentlySelected = @get('rootNodeModel').get 'selectedNode'
          if(typeof(currentlySelected) != 'undefined')
            currentlySelected.set 'selected', false
          @get('rootNodeModel').set 'selectedNode', @

      @bind 'change:folded', =>
        rootID = @get('rootNodeModel').get 'id'
        $("##{rootID}").trigger 'newFoldAction'  


      @bind 'change',(node, changes)->
        if $.inArray('SERVER_SYNC', document.features) > -1
          attributesToPersist = @get 'attributesToPersist'
          persistenceHandler = @get 'persistenceHandler'
        
          $.each changes.changes, (attr, value)->
            if attr in attributesToPersist
              persistenceHandler.persistChanges node, changes

    # will be set to /map/json/id, when fetch() or update() will be called
    urlRoot: '/map/json/' #TODO replace with jsRoutes command

    lock: (lockedBy) ->
      @set 'lockedBy', lockedBy
      @set 'locked', true
 

    unlock: ->
      @set 'locked', false

    # status messages for update
    saveOptions:
      success: (model) ->
        console.log "Node with id '#{model.id}' was updated to the server."
      error: (model, response) ->
        console.log "Error while saving Node with id '#{model.id}': #{response.status} #{response.statusText} (path was #{model.urlRoot})"

    fetchOptions:
      success: (model) ->
        console.log "Node with id '#{model.id}' was fetched from the server."
      error: (model, response) ->
        console.log "Error while fetching Node with id '#{model.id}': #{response.status} #{response.statusText} (path was #{model.urlRoot})"
    
    destroyOptions:
      success: ->
        editor.log "Node has been deleted from the permanent record."
      error: (model, response) ->
        editor.log "Error: #{response.status} #{response.statusText}"

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
 
  module.exports = AbstractNode