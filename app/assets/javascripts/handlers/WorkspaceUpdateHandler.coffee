define ['routers/DocearRouter', 'collections/Workspace', 'models/File', 'models/Project'],  (DocearRouter, Workspace, File, Project) ->  
  module = () ->
  
  class MindMapUpdateHandler extends Backbone.Model

    constructor: (@workspace)->
      super()
      @workspace = workspace
      
    listen: (delay = 0)->
      me = @
      @workspace.each (project)=>
        params = {
          url: jsRoutes.controllers.ProjectController.projectVersionDelta(project.get('id')).url
          type: 'POST'
          cache: false
          data: {'cursor': project.get('revision')}
          success: (data)=>
            #project = new Project(projectData)
            #me.add(project)
          dataType: 'json' 
        }
        if $.inArray('LISTEN_FOR_UPDATES', document.features) > -1
          document.log "listen for updates on workspace"
          $.ajax(params)
      setTimeout(=>
        @listen(delay)
      , delay)
      
    getChanges: ()->
      me = @
      rootNode = @rootNode
      params = {
        url: jsRoutes.controllers.MindMap.fetchUpdatesSinceRevision(-1, @mapId, @rootNode.get('revision')).url
        type: 'GET'
        cache: false
        success: (data)->
          for update in data.orderedUpdates
            switch update.type
              when "ChangeNodeAttribute" then me.updateNode(update)
              when "AddNode" then me.addNode(update)
              when "DeleteNode" then me.deleteNode(update)
          document.log "set current revision to "+data.currentRevision
          rootNode.set 'revision', data.currentRevision
        dataType: 'json' 
      }
      $.ajax(params)
    
    
  module.exports = MindMapUpdateHandler