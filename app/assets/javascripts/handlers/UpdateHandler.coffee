define ['routers/DocearRouter', 'models/Node'],  (DocearRouter, Node) ->  
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
        success: (data)->
          for update in data.orderedUpdates
            switch update.type
              when "ChangeNodeAttribute" then me.updateNode(update)
              when "AddNode" then me.addNode(update)
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
    
    addNode: (update)->
      parentNode = @rootNode.findById update.parentNodeId
      ###
      
      mapLoader = new MapLoader()
      maploader.appendNodesToParent([update], parentNode)
      ###
      node = new Node()
      node.set 'children', []
      node.set 'parent', parentNode
      
      node.set 'id', update.nodeAsJson.id
      node.set 'nodeText', update.nodeAsJson.nodeText
      node.set 'isHtml', update.nodeAsJson.isHtml
      node.set 'folded', update.nodeAsJson.folded

      node.set 'xPos', 0
      node.set 'yPos', 0
      node.set 'hGap', update.nodeAsJson.hGap
      node.set 'shiftY', update.nodeAsJson.shiftY

      node.set 'rootNodeModel', @rootNode
      node.set 'selected', false
      node.set 'previouslySelected', false
      node.set 'foldable', ($.inArray('FOLD_NODE', document.features) > -1)
      node.set 'lastAddedChild', 'undefined'
      node.set 'connectionUpdated', 0

      node.set 'persistenceHandler', parentNode.get('persistenceHandler')
      node.set 'attributesToPersist', ['folded', 'nodeText', 'isHtml']
      node.set 'autoPersist', false

      node.set 'foldedShow', false
      node.set 'minusIcon', jsRoutes.controllers.Assets.at('images/icon_minus.svg').url
      node.set 'plusIcon', jsRoutes.controllers.Assets.at('images/icon_plus.svg').url
      node.set 'loadingIcon', jsRoutes.controllers.Assets.at('images/ajax-loader.gif').url
      node.set 'edgeStyle', parentNode.get('edgeStyle')

      if parentNode.get('id') is @rootNode.get('id')
        if update.side isnt null
          document.log "TODO: add node to root using side: #{update.side}"
      else
        parentNode.addChild(node)
      
      node
      
      
      
      
    
  module.exports = UpdateHandler