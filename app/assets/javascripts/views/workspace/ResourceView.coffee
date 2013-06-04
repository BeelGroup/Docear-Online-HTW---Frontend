define ['logger'], (logger) ->
  module = () ->

  class ResourceView extends Backbone.View
  
    tagName  : 'li'
    className: 'resource'
    template : Handlebars.templates['Resource']

    constructor:(@model)->
      super()
      @model.resources.bind "add", @add , @   
      @_rendered = false

    initialize : ()->
      @resourceViews = []
      @model.resources.each (resource)=>
        @resourceViews.push(new ResourceView(resource))
      
    add: (resource)->
      resourceView = new ResourceView(resource)
      @resourceViews.push(resourceView)
      
      if @_rendered
        $(@el).find('ul.resources:first').append $(resourceView.render().el)
      
    element:-> @$el

    render:()->
      @_rendered = true
      @$el.html @template @model.toJSON()
      
      $resourcesContainer = $(@el).find('ul.resources:first')
      for resourceView in @resourceViews
        $($resourcesContainer).append $(resourceView.render().el)
      
      @


  module.exports = ResourceView