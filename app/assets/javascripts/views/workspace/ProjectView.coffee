define ['logger', 'views/workspace/FileView', 'views/workspace/UserView'], (logger, FileView, UserView) ->
  module = () ->

  class Project extends Backbone.View
  
    tagName  : 'li'
    className: 'project'
    template : Handlebars.templates['Project']

    constructor:(@model)->
      @id = @model.get('id')
      super()
      
      
    initialize : ()->
      @fileViews = []
      @userViews = []
      @model.files.each (file)=>
        @fileViews.push(new FileView(file))
        
      @model.users.each (user)=>
        @userViews.push(new UserView(user))
        
    element:-> @$el


    render:()->
      @$el.html @template @model.toJSON()
      $filesContainer = $(@el).find('ul > li > ul.files')
      $usersContainer = $(@el).find('ul > li > ul.users')
      for fileView in @fileViews
        $($filesContainer).append $(fileView.render().el)
        
      for userView in @userViews
        $($usersContainer).append $(userView.render().el)
      @

  module.exports = Project