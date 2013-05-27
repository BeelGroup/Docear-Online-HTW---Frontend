define ['routers/DocearRouter'],  (DocearRouter) ->  
  module = () ->
  
  class PersistenceHandler extends Backbone.Model

    constructor: (mapId)->
      super()
      @mapId = mapId
      
      @persistenceApi = {
        'change': {
          'Node': jsRoutes.controllers.MindMap.changeNode(mapId).url
        },
        'create': {
          'Node': jsRoutes.controllers.MindMap.createNode(mapId).url
        },
        'delete': {
          'Node': jsRoutes.controllers.MindMap.deleteNode(mapId).url
        },
        'lock': {
          'Node': jsRoutes.controllers.MindMap.requestLock(mapId).url
        },
        'unlock': {
          'Node': jsRoutes.controllers.MindMap.releaseLock(mapId).url
        }
      }
      
    getObjectName: (object)->
      objectName = object.constructor.name
      if objectName == 'RootNode' 
        objectName = 'Node'
      objectName

    persistChanges: (object, changes, callback)->
      params = {'nodeId': object.get('id')}
      changesToPersist = false
      
      $.each changes, (index, attr)->
        params[attr] = object.get attr
        changesToPersist = true
      if changesToPersist
        $.post(@persistenceApi.change.Node, params, callback)

    persistNew: (object, params)->
      $.post(@persistenceApi.create.Node, params)
      
    lock: (node)->
      if $.inArray('LOCK_NODE', document.features) > -1
        params = {
            url: @persistenceApi.lock.Node
            type: 'POST'
            data: {'nodeId': node.get('id')}
            dataType: 'json'
            cache: false
            statusCode: {
              200: (response)->
                document.log "node "+node.get('id')+" locked"
              412: (response)->
                # hide and destroy edit container 
                $editNodeContainer = $('.node-edit-container')
                $('#'+node.get('id')).children('.inner-node').animate({opacity: 1.0}, 0)
                $editNodeContainer.addClass('close-and-destroy').hide()
                document.log "error while locking node "+node.get('id')
            }
            complete: (jqXHR, textStatus)->
              document.log textStatus
        }
        $.ajax(params)
      
    unlock: (node)->
      if $.inArray('LOCK_NODE', document.features) > -1
        params = {
            url: @persistenceApi.unlock.Node
            type: 'POST'
            data: {'nodeId': node.get('id')}
            cache: false
            statusCode: {
              200: (response)->
                document.log "node "+node.get('id')+" unlocked"
                # this is also done by the UpdateHandler when recieving updates,
                # but calling it here is for usability
                node.unlock()
              412: (response)->
                document.log "error while unlocking node "+node.get('id')
            }
        }
        $.ajax(params)

  module.exports = PersistenceHandler