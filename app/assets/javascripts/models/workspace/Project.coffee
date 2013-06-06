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
        @resource = new Resource(@, @get('path'))
    
    getFileByPath: (path)->
      resources = new Resources()
      resources.add(@resource); 
      
      result = undefined
      while result == undefined and files.length > 0
        files.each (file)=>
          if file.path is path
            result = file
            return result
          else if file.isFolder
            files.add file
      result
      
    ###
    # this function look for a file in the tree. If the parent folder exists, 
    # it is created and added to the model (also rendered)
    # if the parent node doesn't exist, we assume that this part of the 
    # tree hasn't been loaded yet, so undefined is returned
    ###
    createOrRecieveRecursiveByPath: (path)->
      filePaths = path.split("/")
      levels = filePaths.length
      
      result = undefined
      currentFile = undefined

      files = @files
      if levels > 1
        currentPath = ''
        for i, path in filePaths
          if files isnt undefined and i+1 < levels
            currentPath = "{currentPath}/#{path}"
            file = files.get currentPath
            if file isnt undefined
              files = file.get 'files'
            else
              files = undefined
              return undefined
          else
            result = files.get currentPath
            if result is undefined
              result = new File(currentPath)
      else
        result = @files.get path
        if result is undefined
          result = new File(path)
        
  module.exports = Project