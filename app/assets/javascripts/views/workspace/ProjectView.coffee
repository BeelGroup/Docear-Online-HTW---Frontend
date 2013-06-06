define ['logger', 'views/workspace/ResourceView', 'views/workspace/UserView'], (logger, ResourceView, UserView) ->
  module = () ->

  class Project extends Backbone.View
  
    tagName  : 'li'
    className: 'project'
    template : Handlebars.templates['Project']

    constructor:(@model)->
      @id = @model.get('id')
      super()
      
      
    initialize : ()->
      @resourceViews = []
      @userViews = []
      @resourceViews.push(new ResourceView(@model.resource))
        
      @model.users.each (user)=>
        @userViews.push(new UserView(user))
        
    element:-> @$el


    render:()->
      @$el.html @template @model.toJSON()
      
      $resourcesContainer = $(@el).find('ul > li > ul.resources')
      for resourceView in @resourceViews
        $($resourcesContainer).append $(resourceView.render().el)

      $usersContainer = $(@el).find('ul > li > ul.users')
      for userView in @userViews
        $($usersContainer).append $(userView.render().el)
      @

  module.exports = Project