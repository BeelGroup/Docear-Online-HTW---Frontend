define ['logger'], (logger)->
  module = () ->

  class File extends Backbone.Model 

    constructor: (name)->
      super()
      @set 'name', name
      
  module.exports = File