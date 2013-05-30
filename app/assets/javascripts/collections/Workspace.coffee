define ['logger', 'models/Project'], (logger, Project)->
  module = () ->

  class Workspace extends Backbone.Collection 
    model: Project

    loadAllUserProjects: ()->
      params = {
        url: jsRoutes.controllers.User.projectListFromDB().url
        type: 'GET'
        cache: false
        success: (data)->
          console.log data
        dataType: 'json' 
      }
      $.ajax(params)
      @add 
    
  module.exports = Workspace