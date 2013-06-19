define ['logger', 'models/workspace/Resource', 'collections/workspace/Resources', 'collections/workspace/Users', 'models/workspace/User'], (logger, Resource, Resources, Users, User)->
  module = () ->

  class Project extends Backbone.Model 

    constructor: (data)->
      super()
      @fillFromData(data)
      @resource.update()
    
    fillFromData: (data)->
      @set 'id', data.id
      @set 'name', data.name
      @set 'revision', data.revision
      
      @setUsers data.authorizedUsers
    
    update: ()->
      me = @
      params = {
        url: jsRoutes.controllers.ProjectController.getProject(@get('id')).url
        type: 'GET'
        cache: false
        success: (projectData)=>
          me.fillFromData(projectData)
          document.log "Project #{me.get('id')} updated"
          
        dataType: 'json' 
      }
      $.ajax(params)
      
    setUsers: (authorizedUsers)->
      for userName in authorizedUsers
        if !@users.get(userName)
          @users.add(new User(userName))
          document.log "added user: #{userName} to project: #{@get('name')}"
      
      @users.each (user)=>
        if user.get('name') not in authorizedUsers
          @users.remove user.get('id')
          document.log "removed user: #{user.get('name')} from project: #{@get('name')}"
      
          
    
    initialize : ()->
      @set 'path', '/'
      if @users is undefined
        @users = new Users()
      if @resource is undefined
        @resource = new Resource(@, @get('path'), true)
        @resource.set 'dir', true
    

    ###
    # this function look for a resource in the tree. If the parent folder exists, 
    # it is created and added to the model (also rendered)
    # if the parent node doesn't exist, we assume that this part of the 
    # tree hasn't been loaded yet, so undefined is returned
    ###
    getResourceByPath: (path, createNonExistence = false)->
      resourcePaths = path.split("/")
      levels = resourcePaths.length
      
      parent = @resource
      
      currentResource = undefined

      resources = new Resources()
      resources.add(@resource);
      
      if levels > 1
        currentPath = '/'
        for path in resourcePaths
          currentPath = "#{currentPath}#{path}"
          currentResource = resources.get(currentPath)
          
          if currentResource isnt undefined
            resources = currentResource.resources
          else
            if createNonExistence
              currentResource = new Resource(@, currentPath, false, parent)
              parent = currentResource;
              document.log "creating new resource: #{path}"
            else
              document.log "resource '#{path}' does not exist"
              parent = undefined
              return undefined
          parent = currentResource
          
          if path isnt ''
            currentPath = "#{currentPath}/"
      parent
      
      
  module.exports = Project