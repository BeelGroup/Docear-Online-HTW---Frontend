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
        @projectViews.push(new ProjectView(project, @))
    
    add: (project)->
      projectView = new ProjectView(project, @)
      @projectViews.push(projectView)
      
      if @_rendered
        $(@el).find('#workspace-tree ul:first').append $(projectView.render().el)
        @$workspaceTree.jstree({
          "plugins": ["themes", "html_data", "ui", "crrm", "contextmenu" ],
          contextmenu: {items: @customMenu}
        })


    refreshNode: ($node) =>
      @$workspaceTree.jstree 'refresh', $node
        
    customMenu:(node) =>
      @$workspaceTree.jstree 'deselect_all'
      @$workspaceTree.jstree "select_node", node, true

      #add default items      
      items = 
        deleteItem:
          label: "Remove",
          separator_before  : false,
          separator_after : true,
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


    addFolderToJsTree:(liNode, a, b)->
      $parent = $('#workspace-tree').jstree('get_selected')

      $('#workspace-tree').jstree('open_node', $parent)
      newNode = { attr: {class: 'folder'}, state: "open", data: "New folder" }
      obj = $('#workspace-tree').jstree("create_node", $parent, 'inside', newNode, false, false)

      # instant renaming
      # own implementation of @.rename(obj)
      obj = @._get_node(obj)
      @.__rollback()
      f = @.__callback
      @._show_input(obj, (obj, new_name, old_name)-> 
        f.call(@, { "obj" : obj, "new_name" : new_name, "old_name" : old_name })

        $parent  = $('#workspace-tree').jstree('get_selected')
        $project = $($parent).closest('li.project')

        # build path
        currentPath = $parent.attr('id')
        if currentPath[currentPath.length-1] isnt '/'
          currentPath += "/"
        currentPath += new_name

        projectId   = $project.attr('id')

        # set id in dom
        obj[0].id = currentPath

        params = {
          url: jsRoutes.controllers.ProjectController.createFolder(projectId).url
          type: 'POST'
          cache: false
          data: {"path": currentPath}
          success:(data)=>
            # create new model and add to parent
            document.log "folder with path : "+currentPath+" to project "+projectId
          error:()=>
            document.log "error on folder adding with path : "+currentPath+" to project "+projectId
          dataType: 'json' 
        }

        $.ajax(params)
      )

    newFolderToServer:()->


    deleteNodeInJsTree:(node)->
      if this.is_selected(node)
        this.remove()
      else 
        this.remove(node)

    addUserInJsTree:()->
      position = 'inside'
      parent = $('#workspace-tree').jstree('get_selected')
      $('#workspace-tree').jstree('open_node', parent)
      newNode = { attr: {class: 'user'}, state: "close", data: "New user"}
      obj = $('#workspace-tree').jstree("create_node", parent, position, newNode, false, false);

      # instant renaming
      @.rename(obj)



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
      options = {}
      options.upload_enabled = $.inArray('WORKSPACE_UPLOAD', document.features) > -1
      @$el.html @template options
      @$workspaceTree = $(@el).children('#workspace-tree')
      
      $projectsContainer = $(@$workspaceTree).children('ul.projects')
      for projectView in @projectViews
        $($projectsContainer).append $(projectView.render().el)
      @
      
  module.exports = Workspace