define ['routers/DocearRouter', 'collections/workspace/Workspace', 'models/workspace/Resource', 'models/workspace/Project'],  (DocearRouter, Workspace, Resource, Project) ->  
  module = () ->
  
  class WorkspaceUpdateHandler extends Backbone.Model

    constructor: (@workspace)->
      @runningRequests = {}
      super()
      
    listen: ()->
      if $.inArray('LISTEN_FOR_UPDATES', document.features) > -1 and $.inArray('WORKSPACE', document.features) > -1
        document.log "listen for updates on workspace"
        me = @
        
        projects = {}
        if @workspace.length > 0
          @workspace.each (project)=>
            if project.get('revision') > -1
              projects[project.id] = project.get('revision');
            else
              projects[project.id] = 0;

        apiUrl = jsRoutes.controllers.ProjectController.listenForUpdates().url
        if apiUrl.indexOf('?') < 0
          apiUrl +=  "?"
        else
          apiUrl +=  "&"
        apiUrl += "_=#{Math.floor(Math.random()*100000)}"
        params = {
          url: apiUrl
          type: 'POST'
          cache: false
          data: projects
          success: (projectData)=>
            for projectId, revision of projectData.updatedProjects
              project = me.workspace.get(projectId)
              me.getChangesByProject(project)
            
            for projectId, revision of projectData.newProjects
              me.getProject(projectId)
            
            for projectId in projectData.deletedProjects
              me.workspace.remove(projectId)
          statusCode: {
            200: ()->
              me.listen()
            304: ()->
              me.listen()
            401: ()->
              document.log "user is not logged in -> stop listening on workspace"
            503: ()->
              document.log "Service Temporarily Unavailable"
              setTimeout(->
                me.listen()
              , 5000)
          }
          dataType: 'json' 
        }
        @execAjax(params)
      
    getChangesByProject: (project)->
      project.update()
      params = {
        url: jsRoutes.controllers.ProjectController.projectVersionDelta(project.get('id')).url
        type: 'POST'
        cache: false
        data: {'projectRevision': project.get('revision')}
        success: (projectsData)=>
          project.set 'revision', projectsData.currentRevision
          
          for path, meta of projectsData.resources
            
            if meta.deleted
              resource = project.getResourceByPath path, false
              if resource != undefined
                resource.get('parent').deleteResourceByPath(path)
                if $(".node.root .map-id[value*='#{path}']").size() > 0
                  location.href = "/#closeMap/#{path}"
                document.log "Resource #{path} deleted"
            else
              resource = project.getResourceByPath path, true
              if resource != undefined
                resource.fillFromData(meta)
                resource.get('parent').addResouce resource
                document.log "Resource #{path} updated"
          
        dataType: 'json' 
      }
      @execAjax(params)
    
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
      @execAjax(params)

    execAjax: (params)->
      request = $.ajax(params)
      @runningRequests[params.url] = request
      request.done =>
        delete @runningRequests[request]
      request
    
    stopRunningRequests: ()->
      @stopped = true
      document.log "stopping running requests on mind map #{@mapId}" 
      for url, request of @runningRequests
        document.log " - stopping: #{url}"
        request.abort()
      @runningRequests = {}
        
  module.exports = WorkspaceUpdateHandler