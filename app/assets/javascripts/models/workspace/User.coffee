define ['logger'], (logger)->
  module = () ->

  class User extends Backbone.Model 

    constructor: (name)->
      super()
      @set 'name', name
      
  module.exports = User