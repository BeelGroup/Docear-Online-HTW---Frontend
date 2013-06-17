define ['logger', 'models/workspace/Project', 'views/workspace/ProjectView', 'views/workspace/UploadView'], (logger, Project, ProjectView, UploadView) ->
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
          plugins: ["themes", "html_data", "ui", "crrm", "contextmenu","dnd"],
          contextmenu: {items: @customMenu},
          ui: 
            select_limit: 1
          ,  
          crrm:
            move:
              check_move: (m)-> # http://www.jstree.com/documentation/core (for m)
                checkresult = false
                # .o - the node being moved
                # .np - the new parent
                sourceProjectId = $(m.o).closest('li.project').attr('id')
                targetProjectId = $(m.np).closest('li.project').attr('id')
                if sourceProjectId is targetProjectId
                  if $(m.np).hasClass('folder') and not $(m.np).hasClass('users')
                    checkresult = true

                checkresult          
                    
        }).bind("move_node.jstree rename_node.jstree create_node.jstree", (event, data)=>
          type = event.type
          if(type is 'move_node')
            # rollback movement
            $.jstree.rollback data.rlbk
            # send move request to the server
            @requestMoveResource event, data
          else
            document.log "Action for event type \'"+type+"\' not implemented jet"
          if(type is 'rename_node')
            @moveResource()
        )


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
          action: -> document.log 'Please implement a remove function for this type of node'

      if ($(node).hasClass("folder")) 
        items.addFile =  # upload
          label: "Add file",
          action: @reQuestAddFile
        items.addFolder =  
          label: "Add folder",
          action: @requestAddFolder
        items.deleteItem.label = "Delete folder"
        items.deleteItem.action = @requestRemoveFolderOrFile

      if ($(node).hasClass("mindmap-file"))
        items.addFile =  # upload
          label: "Open mindmap",
          action: @openMindmap
        items.deleteItem.label = "Delete mindmap"

      if($(node).hasClass("users"))
        items.addUserItem =
          label: "Add user",
          action: @addUser
        # Users directory should not be deleted!
        delete items.deleteItem

      if($(node).hasClass("resources"))
        delete items.deleteItem

      if($(node).hasClass("file"))
        items.deleteItem.action = @requestRemoveFolderOrFile
        items.deleteItem.label = "Delete file"

      if($(node).hasClass("user"))
        items.deleteItem.action = @requestRemoveUser
        items.deleteItem.label = "Delete user"

      items


    openMindmap:(obj)->
      #projectId
      #mapId
      $project = $(obj).closest('li.project')

      itemData = 
        projectId : $project.attr('id')
        # get text from node and remove whitespaces
        name : obj.text().replace /\s/g, ''
        path: obj.attr('id')

      document.log "Trying to open mindmap \'"+itemData.name+"\' from project \'"+itemData.projectId+"\' (WorkspaceView.openMindmap()"
      location.href = "http://127.0.0.1:9000/#project/#{itemData.projectId}/map/#{itemData.name}"


    requestAddFile:(a,b)=>
      document.log 'add file'


    requestAddFolder:(liNode, a, b)->
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
            # remove folder from view
            $('#workspace-tree').jstree("delete_node", obj)

          dataType: 'json' 
        }

        $.ajax(params)
      )

    # jstree functions are required, so dont use a fatarrow here
    requestAddUser:()->      
      $parent = $('#workspace-tree').jstree('get_selected')

      $('#workspace-tree').jstree('open_node', $parent)
      newNode = { attr: {class: 'user'}, state: "leaf", data: "New user" }
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

        projectId   = $project.attr('id')
        params = {
          url: jsRoutes.controllers.ProjectController.addUserToProject(projectId).url
          type: 'POST'
          cache: false
          data: {"username": new_name}
          success:(data)=>
            # create new model and add to parent
            document.log "user \'"+new_name+"\' was added to project "+projectId
          error:()=>
            document.log "error on folder adding with path : "+new_name+" to project "+projectId
            # remove folder from view
            $('#workspace-tree').jstree("delete_node", obj)
          dataType: 'json' 
        }

        $.ajax(params)
      )


    requestRemoveFolderOrFile:()=>
      itemData = @getSelectedItemData()

      params = 
        url: jsRoutes.controllers.ProjectController.deleteFile(itemData.projectId).url
        type: 'DELETE'
        cache: false
        data: "path": itemData.path
        success:(data)=>
          document.log "SUCCESS: The file \'"+itemData.name+"\' was removed from project \'"+itemData.projectId+"\'"
        error:()=>
          document.log "ERROR: The file \'"+itemData.name+"\' wasnt removed from project \'"+itemData.projectId+"\'"
        dataType: 'json' 
      
      $.ajax(params)


    removeFolderOrFile:(path)->
      cleanPath = path.replace new RegExp(".", "g"), "\\."
      cleanPath = cleanPath.replace new RegExp("/", "g"), "\\/"      
      obj = $("#"+cleanPath)
      $('#workspace-tree').jstree("delete_node", obj)


    requestMoveResource:(event, data)=>
      # get node informations
      oldPath = $(data.args[0].o).attr('id')
      name = data.args[0].o.text().replace /\s/g, ''
      newParent = $(data.args[0].np).attr('id')
      if newParent is "/"
        newParent =  ""
      newPath = newParent+"/"+name

      projectId = $(data.args[0].o).closest('li.project').attr('id')

      params = 
        url: jsRoutes.controllers.ProjectController.moveFile(projectId).url
        type: 'POST'
        cache: false
        data: "currentPath": oldPath, "moveToPath": newPath, "name": name

        success:(data)=>

          document.log "SUCCESS: Resource \'"+name+"\' was be moved from "+oldPath+" to "+newPath
        error:()=>
          document.log "ERROR: resource \'"+name+"\' could not be moved from "+oldPath+" to "+newPath
        dataType: 'json' 
      
      $.ajax(params)  


    ###
     old path might be something like "\\/README\\.md"
     use:
      cleanPath = path.replace new RegExp(".", "g"), "\\."
      cleanPath = cleanPath.replace new RegExp("/", "g"), "\\/"
    ###
    moveResource:(oldPath, newPath, newParentsPath)->
      obj = $("#"+oldPath)
      parent = $("#"+newParentsPath)

      @$workspaceTree.jstree('cut', obj)
      @$workspaceTree.jstree('paste', parent)
      @$workspaceTree.jstree 'refresh', obj

      # alter id to new path
      obj.attr('id', newPath)



    requestRemoveUser:()=>
      itemData = @getSelectedItemData()

      params = {
        url: jsRoutes.controllers.ProjectController.removeUserFromProject(itemData.projectId).url
        type: 'POST'
        cache: false
        data: {"username": itemData.name, "itemData" : itemData}
        success:(data)=>
          document.log "SUCCESS: The user \'"+itemData.name+"\' was removed from project \'"+itemData.projectId+"\'"
        error:()=>
          document.log "ERROR: The user \'"+itemData.name+"\' wasnt removed from project \'"+itemData.projectId+"\'"
        dataType: 'json' 
      }

      $.ajax(params)  


    getSelectedItemData:->
      $selectedItem = $('#workspace-tree').jstree('get_selected')    
      $project = $($selectedItem).closest('li.project')

      itemData = 
        projectId : $project.attr('id')
        # get text from node and remove whitespaces
        name : $selectedItem.text().replace /\s/g, ''
        path: $selectedItem.attr('id')

      itemData


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
      "click .upload-file" : "actionUpload"

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

      
    actionUpload: (event)->
      if $.inArray('WORKSPACE_UPLOAD', document.features) > -1
        uploadView = new UploadView("507f191e810c19729de860ea", "/");
        uploadView.appendAndRender @$el
      false
      
  module.exports = Workspace