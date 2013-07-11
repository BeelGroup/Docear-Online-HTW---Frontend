define ['logger', 'models/workspace/Project'], (logger, Project)->
  module = () ->

  class Workspace extends Backbone.Collection 
    model: Project

    loadAllUserProjects: (callback = null)->
      me = @
      params = {
        url: jsRoutes.controllers.User.projectListFromDB().url
        type: 'GET'
        cache: false
        success: (data)->
          $.each(data, (index, projectData)->
            project = new Project(projectData)
            me.add(project)
          )
          if !!callback
            callback()
        dataType: 'json' 
      }
      $.ajax(params)
      
    getResourceByPath: (path)->
      result = null
      @each (project)=>
        resource = project.getResourceByPath(path)
        if resource isnt null
          result = resource
          return result
      result
      
  module.exports = Workspace