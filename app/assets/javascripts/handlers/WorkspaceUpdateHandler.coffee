define ['routers/DocearRouter', 'collections/workspace/Workspace', 'models/workspace/Resource', 'models/workspace/Project'],  (DocearRouter, Workspace, File, Project) ->  
  module = () ->
  
  class MindMapUpdateHandler extends Backbone.Model

    constructor: (@workspace)->
      super()
      @workspace = workspace
      
    listen: ()->
      if $.inArray('LISTEN_FOR_UPDATES', document.features) > -1
        document.log "listen for updates on workspace"
        me = @
        
        if @workspace.length > 0
          projects = {}
          @workspace.each (project)=>
            if project.get('revision') > -1
              projects[project.id] = project.get('revision');
            else
              projects[project.id] = 0;
              
          
          params = {
              url: jsRoutes.controllers.ProjectController.listenForUpdates().url
              type: 'POST'
              cache: false
              data: projects
              success: (projectData)=>
                #project = new Project(projectData)
                #me.add(project)
                for projectId, revision of projectData.updatedProjects
                  project = me.workspace.get(projectId)
                  me.getChangesByProjectId.getChangesByProject(project)
                
                for projectId, revision of projectData.newProjects
                  me.getProject(projectId)
                
                for projectId in projectData.deletedProjects
                  me.workspace.remove(projectId)
                me.listen()
              error: ()->
                me.listen()
              dataType: 'json' 
            }
            $.ajax(params)
        else
          setTimeout(=>
            @listen()
          , 2000)
      
    getChangesByProject: (project)->
      params = {
        url: jsRoutes.controllers.ProjectController.projectVersionDelta(project.get('id')).url
        type: 'POST'
        cache: false
        data: {'projectRevision': project.get('revision')}
        success: (projectsData)=>
          project.set 'revision', projectsData.currentRevision
          
          for path, meta of projectsData.resources
            file = project.createOrRecieveRecursiveByPath path
            if file != undefined
              file.fillFromData(meta)
            document.log "File #{path} updated"
          
        dataType: 'json' 
      }
      $.ajax(params)
    
    getProject: (projectId)->
      me = @
      params = {
        url: jsRoutes.controllers.ProjectController.getProject(projectId).url
        type: 'GET'
        cache: false
        success: (projectData)=>
          project = new Project(projectData)
          me.workspace.add(project)
          document.log "Project #{project.get('id')} added"
          
        dataType: 'json' 
      }
      $.ajax(params)
    
  module.exports = MindMapUpdateHandler