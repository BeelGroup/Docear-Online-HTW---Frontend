define ['logger', 'models/workspace/Resource', 'collections/workspace/Resources', 'collections/workspace/Users', 'models/workspace/User'], (logger, Resource, Resources, Users, User)->
  module = () ->

  class Project extends Backbone.Model 

    constructor: (data)->
      super()
      @set 'id', data.id
      @set 'name', data.name
      @set 'revision', data.revision
      
      @setUsers data.authorizedUsers

      @resource.update()
    
    setUsers: (authorizedUsers)->
      me = @
      for userName in authorizedUsers
        user = new User(userName)
        me.users.add(user)
      
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