define ['logger', 'models/workspace/Project', 'views/workspace/ProjectView'], (logger, Project, ProjectView) ->
  module = () ->

  class Workspace extends Backbone.View
  
    tagName  : 'div'
    className: 'workspace-container'
    id: "workspace-container"
    template : Handlebars.templates['Workspace']

    constructor:(@model)->
      super()
      @model.bind "add", @add , @   
      @_rendered = false
      
    initialize : ()->
      @projectViews = []
      @model.each (project)=>
        @projectViews.push(new ProjectView(project))
    
    add: (project)->
      projectView = new ProjectView(project)
      @projectViews.push(projectView)
      
      if @_rendered
        $(@el).find('ul.projects:first').append $(projectView.render().el)

    #http://liquidmedia.org/blog/2011/02/backbone-js-part-3/
    remove: (model)->
      viewToRemove = @projectViews.select((cv)-> return cv.model is model )[0]
      @projectViews = @projectViews.without(viewToRemove)
      
      if @_rendered
        $(viewToRemove.el).remove()
        
    events:
      "click .add-project-toggle" : ->
        numProjects = @$el.find('.projects li').size()
        params = {
          url: jsRoutes.controllers.ProjectController.createProject().url
          type: 'POST'
          cache: false
          data: {"name": "Project_#{numProjects}"}
          success: (data)=>
            project = new Project(data)
            @model.add(project)
            document.log "project added"
          dataType: 'json' 
        }
        $.ajax(params)
        

    element:-> @$el


    render:()->
      @_rendered = true
      @$el.html @template
      $workspaceTree = $(@el).children('#workspace-tree')
      
      $projectsContainer = $($workspaceTree).children('ul.projects')
      for projectView in @projectViews
        $($projectsContainer).append $(projectView.render().el)
      
      @
      

  module.exports = Workspace