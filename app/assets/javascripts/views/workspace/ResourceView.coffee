define ['logger'], (logger) ->
  module = () ->

  class ResourceView extends Backbone.View
  
    tagName  : 'li'
    className: 'resource'
    template : Handlebars.templates['Resource']

    constructor:(@model, @projectView, @parentView)->
      super()
      @model.resources.bind "add", @add , @   
      @_rendered = false

    initialize : ()->
      @resourceViews = []
      @model.resources.each (resource)=>
        @resourceViews.push(new ResourceView(resource, @projectView, @$el))
      
    add: (resource)->
      resourceView = new ResourceView(resource, @projectView, @$el)
      @resourceViews.push(resourceView)
      
      if @_rendered
        $resources = $(@el).find('ul.resources:first')
        $resources.append $(resourceView.render().el)
    
    element:-> @$el

    render:()->
      @_rendered = true
      @$el.html @template @model.toJSON()
      @$el.attr 'id', @model.get('id')
      if @model.dir
        $(@el).addClass('folder')  
      
      $resourcesContainer = $(@el).find('ul.resources:first')
      for resourceView in @resourceViews
        $($resourcesContainer).append $(resourceView.render().el)
      @projectView.refresh($(@el))
      @


  module.exports = ResourceView