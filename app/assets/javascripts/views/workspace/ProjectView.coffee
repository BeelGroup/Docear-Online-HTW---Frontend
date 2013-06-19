define ['logger', 'views/workspace/ResourceView', 'views/workspace/UserView', 'views/workspace/WorkspaceView'], (logger, ResourceView, UserView, WorkspaceView) ->
  module = () ->

  class Project extends Backbone.View
  
    tagName  : 'li'
    className: 'project'
    template : Handlebars.templates['Project']

    constructor:(@model, @workspaceView)->
      @id = @model.get('id')
      @model.users.bind "add", @addUser , @   
      @model.users.bind "remove", @removeUser , @
      @_rendered = false
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

      
    addUser: (user)=>
      document.log "adding user #{user.get('name')} to view"
      
      
      # $objToDelete = $("ul.users a##{user.get('name')}")
      # $('#workspace-tree').jstree("delete_node", $objToDelete)
      
      if @_rendered
        $parent = $('#workspace-tree').find("li.users")
        newNode = { attr: {class: "resource file", id: user.get('id')}, state: "leaf", data: user.get('name') }
        obj = $('#workspace-tree').jstree("create_node", $parent, 'inside', newNode, false, false)
      else
        userView = new UserView(user)
        @userViews.push(userView)
      
    removeUser: (user)=>
      document.log "removing user #{user.get('name')} from view"
      $objToDelete = $("ul.users a##{user.get('name')}")
      $('#workspace-tree').jstree("delete_node", $objToDelete)
      
    render:()->
      @$el.html @template @model.toJSON()
      
      $resourcesContainer = $(@el).children('ul')
      for resourceView in @resourceViews
        resourceView.render()

      $usersContainer = $(@el).find('ul > li > ul.users')
      for userView in @userViews
        $($usersContainer).append $(userView.render().el)
      @_rendered = true
      @

  module.exports = Project