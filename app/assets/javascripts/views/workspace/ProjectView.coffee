define ['logger', 'views/workspace/ResourceView', 'views/workspace/UserView', 'views/workspace/WorkspaceView'], (logger, ResourceView, UserView, WorkspaceView) ->
  module = () ->

  class Project extends Backbone.View
  
    tagName  : 'li'
    className: 'project'
    template : Handlebars.templates['Project']

    constructor:(@model, @workspaceView)->
      @id = @model.get('id')
      super()
      
      
    initialize : ()->
      @resourceViews = []
      @userViews = []
      @resourceViews.push(new ResourceView(@model.resource, @, @$el))
        
      @model.users.each (user)=>
        @userViews.push(new UserView(user))
        
    element:-> @$el

    getId:->
      @id
    
    refresh: ($node = -1)=>
      @workspaceView.refreshNode $node

    render:()->
      @$el.html @template @model.toJSON()
      
      $resourcesContainer = $(@el).children('ul')
      for resourceView in @resourceViews
        $($resourcesContainer).append $(resourceView.render().el)

      $usersContainer = $(@el).find('ul > li > ul.users')
      for userView in @userViews
        $($usersContainer).append $(userView.render().el)
      @

  module.exports = Project