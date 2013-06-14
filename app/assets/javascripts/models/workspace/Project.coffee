define ['logger', 'models/workspace/Resource', 'collections/workspace/Users', 'models/workspace/User'], (logger, Resource, Users, User)->
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
    
    getResourceByPath: (path)->
      resources = new Resources()
      resources.add(@resource); 
      
      result = undefined
      while result == undefined and resources.length > 0
        resources.each (resource)=>
          if resource.path is path
            result = resource
            return result
          else if resource.isFolder
            resources.add resource
      result
      
    ###
    # this function look for a resource in the tree. If the parent folder exists, 
    # it is created and added to the model (also rendered)
    # if the parent node doesn't exist, we assume that this part of the 
    # tree hasn't been loaded yet, so undefined is returned
    ###
    createOrRecieveResourceByPath: (path)->
      resourcePaths = path.split("/")
      levels = resourcePaths.length
      
      result = undefined
      currentResource = undefined

      resources = new Resources()
      resources.add(@resource); 
      if levels > 1
        currentPath = ''
        for i, path in resourcePaths
          if i+1 < levels
            currentPath = "{currentPath}/#{path}"
            resource = resources.get currentPath
            if resource isnt undefined
              resources = resource.get 'resources'
            else
              resources = undefined
              return undefined
          else
            result = resources.get currentPath
            if result is undefined
              result = new Resource(currentPath)
      else if result is undefined
        result = new Resource(path)
        
  module.exports = Project