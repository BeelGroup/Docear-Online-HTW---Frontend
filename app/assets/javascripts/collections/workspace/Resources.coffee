define ['logger', 'models/workspace/Resource'], (logger, Resource)->
  module = () ->

  class Resources extends Backbone.Collection 
    model: Resource

    
  module.exports = Resources