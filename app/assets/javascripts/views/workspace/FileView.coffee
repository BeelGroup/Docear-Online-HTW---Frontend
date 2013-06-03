define ['logger'], (logger) ->
  module = () ->

  class File extends Backbone.View
  
    tagName  : 'li'
    className: 'file'
    template : Handlebars.templates['File']

    constructor:(@model)->
      super()

    element:-> @$el

    render:()->
      @$el.html @template @model.toJSON()
      @


  module.exports = File