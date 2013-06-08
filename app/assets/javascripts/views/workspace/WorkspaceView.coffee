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
        $(@el).find('#workspace-tree ul:first').append $(projectView.render().el)
        @$workspaceTree.jstree({
        "plugins": ["themes", "html_data", "ui", "crrm", "contextmenu" ],
        contextmenu: {items: @customMenu}
        }).bind("rename_node.jstree create_node.jstree", (event, data)-> 
          type = event.type
          if(type is 'move_node')
            document.log 'moving a node is currently not implemented!'
          else if (type is 'rename_node')
            document.log 'rename node'
          else if (type is 'create_node')
            document.log 'create node'
        )

    customMenu:(node) =>

      @$workspaceTree.jstree 'deselect_all'
      @$workspaceTree.jstree("select_node", node, true); 

      #add default items      
      items = 
        deleteItem:
          label: "Remove",
          action: @deleteNodeInJsTree
         
      if ($(node).hasClass("folder")) 
        items.addFile =  # upload
          label: "Add file",
          action: @addFileToJsTree
        items.addFolder =  
          label: "Add Folder",
          action: @addFolderToJsTree

      if($(node).hasClass("users"))
        items.addUserItem =
          label: "Add user",
          action: @addUserInJsTree
        # Users directory should not be deleted!
        delete items.deleteItem

      if($(node).hasClass("resources"))
        delete items.deleteItem

      items


    addFileToJsTree:(a,b)=>
      document.log 'add file'


    addFolderToJsTree:()=>
      console.log @$workspaceTree.jstree('get_selected')
      position = 'inside'
      parent = @$workspaceTree.jstree('get_selected')
      newNode = { state: "open", data: "New nooooode!" }
      @$workspaceTree.jstree("create_node", parent, position, newNode, false, false);


    deleteNodeInJsTree:(node)=>
      document.log 'delete node'
      
    addUserInJsTree:()=>
      document.log 'add user'



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
      @$workspaceTree = $(@el).children('#workspace-tree')
      
      $projectsContainer = $(@$workspaceTree).children('ul.projects')
      for projectView in @projectViews
        $($projectsContainer).append $(projectView.render().el)
      
      @$workspaceTree.jstree()
      @
      

  module.exports = Workspace