define ['routers/DocearRouter'],  (DocearRouter) ->  
  module = () ->
  
  class UpdateHandler extends Backbone.Model

    constructor: (mapId, rootNode)->
      super()
      @mapId = mapId
      @rootNode = rootNode
      
      @updateApi = {
        'listenForUpdate': {
          'Node': jsRoutes.controllers.MindMap.listenForUpdates(mapId).url
        }
      }
      @listen()
      
    listen: (delay = 0)->
      me = @
      params = {
        url: @updateApi.listenForUpdate.Node
        type: 'GET'
        cache: false
        complete: (jqXHR, textStatus )->
          # document.log textStatus
        statusCode: {
          200: ()->
            document.log "changed -> calling getChanges()"
            me.getChanges()
            me.listenIfMapIsOpen(0)
          304: ()->
            document.log "no changes -> listen()"
            me.listenIfMapIsOpen(0)
          401: ()->
            document.log "user is not logged in -> stop listening"
          503: ()->
            document.log "Service Temporarily Unavailable"
            me.listenIfMapIsOpen(1000)
          0: ()->
            document.log "Unecpected response code 0"
            me.listenIfMapIsOpen(1000)
            
        }
        dataType: 'json' 
      }
      setTimeout(->
        if $.inArray('LISTEN_FOR_UPDATES', document.features) > -1
          document.log "listen for updates"
          $.ajax(params)
      , delay)
        
    listenIfMapIsOpen: (delay = 0)->
      if $(".node.root .map-id[value=#{@mapId}]").size() > 0
        @listen(delay)
      else
        document.log "map: #{@mapId} seems to be closed, stop listening"
    
    listenWithDelay: (delay)->
      setTimeout(->
        @listen()
      , delay)
    
    getChanges: ()->
      me = @
      rootNode = @rootNode

      params = {
        url: jsRoutes.controllers.MindMap.fetchUpdatesSinceRevision(@mapId, @rootNode.get('revision')).url
        type: 'GET'
        cache: false
        success: (data, textStatus, xhr)->
          for update in data.orderedUpdates
            switch update.type
              when "ChangeNodeAttribute" then me.updateNode(update)
          document.log "set current revision to "+data.currentRevision
          rootNode.set 'revision', data.currentRevision
        dataType: 'json' 
      }
      $.ajax(params)
    
    updateNode: (update)->
      node = @rootNode.findById update.nodeId
      if update.attribute is 'locked'
        if update.value isnt null
          node.lock update.value
          document.log "UPDATE: node #{node.id} SET locked"
        else
          node.unlock()
          document.log "UPDATE: node #{node.id} SET unlocked"
      else if update.attribute is 'isHtml'
        document.log "UPDATE: node #{node.id} SET #{update.attribute} = #{update.value} "
        node.setAttributeWithoutPersist 'isHtml', update.value
      else if update.attribute is 'nodeText'
        document.log "UPDATE: node #{node.id} SET #{update.attribute} = #{update.value}"
        node.setAttributeWithoutPersist 'nodeText', update.value
      
      
    
  module.exports = UpdateHandler