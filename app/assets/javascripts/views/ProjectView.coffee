define ['logger', 'views/FileView'], (logger, FileView) ->
  module = () ->

  class Project extends Backbone.View
  
    tagName  : 'li'
    className: 'project'
    template : Handlebars.templates['Project']

    constructor:(@model)->
      super()
      
      
    initialize : ()->
      @fileViews = []
      @model.files.each (file)=>
        @fileViews.push(new FileView(file))
        
    element:-> @$el


    render:()->
      @$el.html @template @model.toJSON()
      $filesContainer = $(@el).children('ul.files')
      for fileView in @fileViews
        $($filesContainer).append $(fileView.render().el)
      @

    renderAndAppendToNode:($target)->
      $($target).append(@render().el)
      @

  module.exports = Project