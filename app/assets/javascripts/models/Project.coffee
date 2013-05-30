define ['logger', 'collections/Files'], (logger, Files)->
  module = () ->

  class Project extends Backbone.Model 

    constructor: (name, @files = null)->
      super()
      @set 'name',  name
      
    initialize : ()->
      if @files is null
        @files = new Files()
      
      
      
    
    createProject: (name)->
      params = {
        url: jsRoutes.controllers.ProjectController.createProject().url
        type: 'POST'
        cache: false
        data: {'name': name}
        success: (data)->
          console.log data
        dataType: 'json' 
      }
      $.ajax(params)
      
  module.exports = Project