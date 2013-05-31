define ['logger'], (logger) ->
  module = () ->

  class User extends Backbone.View
  
    tagName  : 'li'
    className: 'user'
    template : Handlebars.templates['User']

    constructor:(@model)->
      super()

    element:-> @$el

    render:()->
      @$el.html @template @model.toJSON()
      @


  module.exports = User