define ->
  module = () ->
  
  class HtmlView extends Backbone.View

    tagName: 'div',
    className: 'htmlView' 
    template: Handlebars.templates['Container']

    getRenderData: ->
    # if the model is already set, parse it to json
      if @model?
        @model.toJSON()
    # otherwise pass an empty JSON
      else
        {}

    # a.k.a. constructor
    initialize: (@model) ->

    render: ->
      @$el.html @template @getRenderData()
      @


  module.exports = HtmlView