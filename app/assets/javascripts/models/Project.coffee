define ['logger', 'collections/Files'], (logger, Files)->
  module = () ->

  class Project extends Backbone.Model 

    constructor: (data, @files = null)->
      super()
      @set 'id', data.id
      @set 'name', data.name
      @set 'revision', data.revision
      @set 'authorizedUsers', data.authorizedUsers
    
    initialize : ()->
      if @files is null
        @files = new Files()
      
  module.exports = Project