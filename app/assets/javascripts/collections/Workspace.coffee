define ['logger', 'models/Project'], (logger, Project)->
  module = () ->

  class Workspace extends Backbone.Collection 
    model: Project

    
  module.exports = Workspace