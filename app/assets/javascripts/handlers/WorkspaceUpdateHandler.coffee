define ['routers/DocearRouter', 'collections/Workspace', 'models/File', 'models/Project'],  (DocearRouter, Workspace, File, Project) ->  
  module = () ->
  
  class MindMapUpdateHandler extends Backbone.Model

    constructor: (@workspace)->
      super()
      @workspace = workspace
      
    listen: (delay = 0)->
      if $.inArray('LISTEN_FOR_UPDATES', document.features) > -1
        document.log "listen for updates on workspace"
        me = @
        @workspace.each (project)=>
          me.getChangesByProject(project)
        
        setTimeout(=>
          @listen(delay)
        , delay)
      
    getChangesByProject: (project)->
      params = {
          url: jsRoutes.controllers.ProjectController.projectVersionDelta(project.get('id')).url
          type: 'POST'
          cache: false
          data: {'cursor': project.get('revision')}
          success: (projectData)=>
            #project = new Project(projectData)
            #me.add(project)
            document.log "Project #{project.get('id')} updates recieved"
            
          dataType: 'json' 
        }
        $.ajax(params)
          
          
    
    
  module.exports = MindMapUpdateHandler