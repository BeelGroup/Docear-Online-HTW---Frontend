define ['routers/DocearRouter'],  (DocearRouter) ->  
  module = () ->
  
  class PersistenceHandler extends Backbone.Model

    constructor: (@mapId, @projectId, @sourceId, @disabled)->
      super()
      @persistenceApi = {
        'change': {
          'Node': document.addURLParam(jsRoutes.controllers.MindMap.changeNode(@projectId, encodeURIComponent(@mapId)).url, 'source', @sourceId)
        },
        'create': {
          'Node': document.addURLParam(jsRoutes.controllers.MindMap.createNode(@projectId, encodeURIComponent(@mapId)).url, 'source', @sourceId)
        },
        'delete': {
          'Node': document.addURLParam(jsRoutes.controllers.MindMap.deleteNode(@projectId, encodeURIComponent(@mapId)).url, 'source', @sourceId)
        },
        'lock': {
          'Node': document.addURLParam(jsRoutes.controllers.MindMap.requestLock(@projectId, encodeURIComponent(@mapId)).url, 'source', @sourceId)
        },
        'unlock': {
          'Node': document.addURLParam(jsRoutes.controllers.MindMap.releaseLock(@projectId, encodeURIComponent(@mapId)).url, 'source', @sourceId)
        }
      }
      
    getObjectName: (object)->
      objectName = object.constructor.name
      if objectName == 'RootNode' 
        objectName = 'Node'
      objectName

    persistChanges: (object, changes, callback)->
      if !@disabled
        params = {'nodeId': object.get('id')}
        changesToPersist = false
        
        $.each changes, (index, attr)->
          params[attr] = object.get attr
          changesToPersist = true
        if changesToPersist
          $.post(@persistenceApi.change.Node, params, callback)

    persistNew: (object, params)->
      if !@disabled
        $.post(@persistenceApi.create.Node, params)

    deleteNode: (node, errorCallback)->
      if !@disabled
        params = {
            url: @persistenceApi.delete.Node
            type: 'DELETE'
            data: {'nodeId': node.get('id')}
            dataType: 'json'
            cache: false
            statusCode: {
              200: (response)->
                document.log "node "+node.get('id')+" deleted OK"
                
              412: (response)->
                document.log "node "+node.get('id')+" could not be deleted"
                errorCallback()
            }
        }
        $.ajax(params)
      
    lock: (node)->
      if !@disabled
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
      if !@disabled
        if $.inArray('LOCK_NODE', document.features) > -1
          params = {
              url: @persistenceApi.unlock.Node
              type: 'POST'
              data: {'nodeId': node.get('id')}
              cache: false
              statusCode: {
                200: (response)->
                  document.log "node "+node.get('id')+" unlocked"
                  # this is also done by the MindMapUpdateHandler when recieving updates,
                  # but calling it here is for usability
                  node.unlock()
                412: (response)->
                  document.log "error while unlocking node "+node.get('id')
              }
          }
          $.ajax(params)

  module.exports = PersistenceHandler