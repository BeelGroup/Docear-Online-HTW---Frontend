define ['logger'], (logger)->
  module = () ->

  class User extends Backbone.Model 

    constructor: (name, projectId)->
      super()
      @set 'id', name
      @set 'name', name
      @set 'projectId', projectId
      
  module.exports = User