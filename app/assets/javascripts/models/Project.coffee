define ['logger', 'collections/Files', 'collections/Users', 'models/User'], (logger, Files, Users, User)->
  module = () ->

  class Project extends Backbone.Model 

    constructor: (data)->
      super()
      @set 'id', data.id
      @set 'name', data.name
      @set 'revision', data.revision
      
      @setUsers data.authorizedUsers
      @set 'path', '/'
    
    setUsers: (authorizedUsers)->
      me = @
      for userName in authorizedUsers
        user = new User(userName)
        me.users.add(user)
      
    initialize : ()->
      if @files is undefined
        @files = new Files()
      if @users is undefined
        @users = new Users()
      
    loadFiles: ()->
      params = {
        url: jsRoutes.controllers.ProjectController.metadata(@id, "/").url
        type: 'GET'
        cache: false
        success: (data)->
          document.log "files data received"
        dataType: 'json' 
      }
      $.ajax(params)
      
  module.exports = Project