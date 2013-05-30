define ['logger', 'views/ProjectView'], (logger, ProjectView) ->
  module = () ->

  class Workspace extends Backbone.View
  
    tagName  : 'div'
    className: 'workspace-container'
    template : Handlebars.templates['Workspace']

    constructor:(@model)->
      super()
      
    initialize : ()->
      @projectViews = []
      @model.each (project)=>
        @projectViews.push(new ProjectView(project))
    
    events:
      "click .add-project-toggle" : -> 
        $(this).find('workspace-controls').hide()
        $(this).find('add-project').show()
        

    element:-> @$el


    render:()->
      @$el.html @template
      $projectsContainer = $(@el).children('ul.projects')
      for projectView in @projectViews
        $($projectsContainer).append $(projectView.render().el)
      @


  module.exports = Workspace