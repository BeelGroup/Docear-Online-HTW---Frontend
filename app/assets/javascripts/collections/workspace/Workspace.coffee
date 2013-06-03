define ['logger', 'models/Project'], (logger, Project)->
  module = () ->

  class Workspace extends Backbone.Collection 
    model: Project

    loadAllUserProjects: ()->
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
        dataType: 'json' 
      }
      $.ajax(params)
      
  module.exports = Workspace