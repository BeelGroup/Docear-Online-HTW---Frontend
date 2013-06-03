define ['logger', 'models/workspace/User'], (logger, User)->
  module = () ->

  class Users extends Backbone.Collection 
    model: User

    
  module.exports = Users