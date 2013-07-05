define ['logger', 'views/workspace/ProjectView'], (logger, ProjectView) ->
  module = () ->

  class Workspace extends Backbone.View
  
    tagName  : 'div'
    className: 'workspace-container'
    id: "workspace-container"
    template : Handlebars.templates['Workspace']

    constructor:(@model)->
      super()
      @model.bind "add", @add , @
      @model.bind "remove", @remove , @   
      @_rendered = false

    resize:(widthAndHeight)->
      @$el.css
        height: widthAndHeight.height

    initialize : ()->
      @projectViews = {}
      @model.each (project)=>
        @projectViews[project.get('id')] = new ProjectView(project, @)

    remove: (project)->
      $objToDelete = $("li##{project.get('id')}")
      delete @projectViews[project.get('id')]
      if $objToDelete.size() > 0
        $('#workspace-tree').jstree("delete_node", $objToDelete)
              
    add: (project)->
      projectView = new ProjectView(project, @)
      @projectViews[project.get('id')] = projectView
      
      if @_rendered        
        $objToDelete = $(".temp-project.delete-me-on-update")
        if $objToDelete.size() > 0
          $('#workspace-tree').jstree("delete_node", $objToDelete)
        $(@el).find('#workspace-tree ul:first').append $(projectView.render().el)

        @$workspaceTree.jstree({
          plugins: ["themes", "html_data", "ui", "crrm", "contextmenu","dnd", "sort"],
          sort: (a,b)-> 
            aIsNotFolder = not $(a).hasClass('folder')
            bIsNotFolder = not $(b).hasClass('folder')

            valTextA = @.get_text(a).toLowerCase()
            valTextB = @.get_text(b).toLowerCase()

            aIsBigger = valTextA > valTextB

            if aIsBigger
              if bIsNotFolder
                if aIsNotFolder
                  1
                else
                  -1
              else
                1
            else
              if aIsNotFolder
                if bIsNotFolder
                  -1
                else
                  1
              else
                -1

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
                    
        }).bind("move_node.jstree rename_node.jstree create_node.jstree dblclick.jstree", (event, data)=>
          type = event.type
          if type is 'move_node'
            # rollback movement
            $.jstree.rollback data.rlbk
            # send move request to the server
            @requestMoveResource event, data
          else if type is 'rename_node'
            if $(data.args[0]).hasClass 'temp-project'
              @requestCreateProject(data.args[0], data.args[1])
            else
              @moveResource()
          else if type is 'dblclick'
            console.log $(event.target)
            $target = $(event.target)
            if $target.hasClass 'jstree-icon'
              $obj = $(event.target).parent().parent()
            else
              $obj = $(event.target).parent()

            if ($obj.hasClass('users') or $obj.hasClass('project') or $obj.hasClass('folder'))
              if $obj.hasClass 'jstree-open'
                $('#workspace-tree').jstree('close_node', $obj)
              else
                $('#workspace-tree').jstree('open_node', $obj)
            else if $obj.hasClass 'mindmap-file'
              @openMindmap $obj
            
          else
            document.log "Action for event type \'"+type+"\' not implemented jet"
        )


    refreshNode: ($node) =>
      @$workspaceTree.jstree 'refresh', $node


    customMenu:(node) =>
      @$workspaceTree.jstree 'deselect_all'
      @$workspaceTree.jstree "select_node", node, true

      #add default items
      items = new Object()

      if ($(node).hasClass("mindmap-file"))
        items.addFile =  # upload
          label: "Open mind map",
          action: @openMindmap

      if ($(node).hasClass("folder")) 
        items.createMapItem = 
          label: "Create new mind map"
          action: @requestCreateMapItem
        items.addFile =  # upload
          label: "Upload &amp; add file",
          action: @requestAddFile
        items.addFolder =  
          label: "Add folder",
          action: @requestAddFolder
        if not ($(node).hasClass("resources"))
          items.deleteItem =
            separator_before: true
            label: "Delete folder"
            action: @requestRemoveFolderOrFile

      if($(node).hasClass("users"))
        items.addUserItem =
          label: "Add user",
          action: @requestAddUser

      if($(node).hasClass("file"))
        items.downloadItem = 
          label: "Download file"
          action: @requestDownloadItem
        items.deleteItem = 
          separator_before: true
          label: "Delete file"
          action: @requestRemoveFolderOrFile

      if($(node).hasClass("user") and not $(node).hasClass("users") )
        items.deleteItem =
          separator_before: true
          label: "Delete user"
          action:  @requestRemoveUser

      items


    openMindmap:(obj)->
      #projectId
      #mapId
      $project = $(obj).closest('li.project')
      path = obj.attr('id').substr(obj.attr('id').indexOf("_PATH_")+6)

      itemData = 
        projectId : $project.attr('id')
        # get text from node and remove whitespaces
        name : obj.text().replace /\s/g, ''
        path: path

      document.log "Trying to open mindmap \'"+itemData.name+"\' from project \'"+itemData.projectId+"\' (WorkspaceView.openMindmap()"
      location.href = "/#project/#{itemData.projectId}/map/#{itemData.path}"

    requestCreateMapItem: (liNode, a, b)->
      $parent = $('#workspace-tree').jstree('get_selected')
      $('#workspace-tree').jstree('open_node', $parent)
      newNode = { attr: {class: 'resource file mindmap-file delete-me-on-update'}, state: "leaf", data: "new_mindmap.mm" }
      obj = $('#workspace-tree').jstree("create_node", $parent, 'inside', newNode, false, false)

      # instant renaming
      # own implementation of @.rename(obj)
      obj = @._get_node(obj)
      @.__rollback()
      f = @.__callback
      @._show_input(obj, (obj, new_name, old_name)-> 
        f.call(@, { "obj" : obj, "new_name" : new_name, "old_name" : old_name })

        nameEnding = new_name.substring(new_name.lastIndexOf('.')+1)
        if nameEnding isnt 'mm'
          new_name += '.mm'
        $("#workspace-tree").jstree('rename_node', obj[0] , new_name)
        
        $parent  = $('#workspace-tree').jstree('get_selected')
        $project = $($parent).closest('li.project')

        currentPath = $parent.attr('id')
        currentPath = currentPath.substr(currentPath.indexOf("_PATH_")+6)

        if currentPath isnt "/"
          $path = currentPath+"/"+new_name 
        else
          $path = currentPath+new_name 

        # set path as id -> so it will be found and can be removed on update from server
        $(obj).attr("id", $path)
        # build path
        if currentPath[currentPath.length-1] isnt '/'
          currentPath += "/"
        currentPath += new_name

        projectId   = $project.attr('id')
        # set id in dom
        obj[0].id = currentPath

        params = {
          url: jsRoutes.controllers.MindMap.createNewMap(projectId).url
          type: 'POST'
          cache: false
          data: {"path": currentPath}
          success:(data)=>
            # create new model and add to parent
            document.log "mind map created: "+currentPath+" to project "+projectId

          error:()=>
            document.log "error while adding mind map with path : "+currentPath+" to project "+projectId
            # remove mm file from view
            $('#workspace-tree').jstree("delete_node", obj)

          dataType: 'json' 
        }
        $.ajax(params)
      )
        
    requestAddFile:(liNode, a,b)=>
      if $.inArray('WORKSPACE_UPLOAD', document.features) > -1
        $('#file-to-upload').click()
      false


    requestAddFolder:(liNode, a, b)->
      $parent = $('#workspace-tree').jstree('get_selected')

      $('#workspace-tree').jstree('open_node', $parent)
      newNode = { attr: {class: 'folder delete-me-on-update', id:'fuadadeimuada'}, state: "closed", data: "New folder" }
      obj = $('#workspace-tree').jstree("create_node", $parent, 'inside', newNode, false, false)


      # instant renaming
      # own implementation of @.rename(obj)
      obj = @._get_node(obj)
      @.__rollback()
      f = @.__callback
      @._show_input(obj, (obj, new_name, old_name)-> 

        f.call(@, { "obj" : obj, "new_name" : new_name, "old_name" : old_name })
        #$('#workspace-tree').jstree('deselect_all')
        #$('#workspace-tree').jstree("select_node", "#fuadadeimuada")
        $parent  = $(obj).parent().parent()

        $project = $($parent).closest('li.project')
        currentPath = $parent.attr('id')
        currentPath = currentPath.substr(currentPath.indexOf("_PATH_")+6)


        if currentPath isnt "/"
          $path = $parent.attr("id")+"/"+new_name 
        else
          $path = $parent.attr("id")+new_name 

        # set path as id -> so it will be found and can be removed on update from server
        $(obj).attr("id", $path)

        # build path
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

        $(obj).children('a').attr('id', new_name)
        
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

    requestDownloadItem: ()=>
      itemData = @getSelectedItemData()
      currentPath = itemData.path.substr(itemData.path.indexOf("_PATH_")+6)
      window.open(jsRoutes.controllers.ProjectController.getFile(itemData.projectId, currentPath).url);

    requestRemoveFolderOrFile:()=>
      itemData = @getSelectedItemData()
      currentPath = itemData.path.substr(itemData.path.indexOf("_PATH_")+6)

      params = 
        url: jsRoutes.controllers.ProjectController.deleteFile(itemData.projectId).url
        type: 'DELETE'
        cache: false
        data: "path": currentPath
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
      oldPath = oldPath.substr(oldPath.indexOf("_PATH_")+6)

      name = oldPath.substring(oldPath.lastIndexOf('/')+1)

      newParent = $(data.args[0].np).attr('id')
      newParent = newParent.substr(newParent.indexOf("_PATH_")+6)

      # from http://stackoverflow.com/questions/280634/endswith-in-javascript
      if newParent.indexOf('/', newParent.length - 1) is -1
        newParent += '/'
      newPath = newParent + name

      projectId = $(data.args[0].o).closest('li.project').attr('id')
      params = 
        url: jsRoutes.controllers.ProjectController.moveFile(projectId).url
        type: 'POST'
        cache: false
        data: "currentPath": oldPath, "moveToPath": newPath

        success:()=>
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
    
    requestCreateProject: (obj, projectName)=>
      params = {
          url: jsRoutes.controllers.ProjectController.createProject().url
          type: 'POST'
          cache: false
          data: {"name": projectName}
          success:(data)=>
            # create new model and add to parent
            document.log "new project created : "+projectName
          error:()=>
            document.log "error while creating project : "+projectName
            # remove folder from view
            $('#workspace-tree').jstree("delete_node", obj)
          dataType: 'json' 
        }
        $.ajax(params)
      
        
    newProject: ()->
      new_name = "project"
      obj = $("#workspace-tree").jstree("create","#workspace-tree","last","new_name", false, true)
      $(obj).addClass('project temp-project delete-me-on-update')
      $("#workspace-tree").jstree("rename",obj)


    events:
      "click .add-project-toggle" : "newProject"

    element:-> @$el


    render:()->
      @_rendered = true
      options = {}
      options.upload_enabled = $.inArray('WORKSPACE_UPLOAD', document.features) > -1
      @$el.html @template options
      @$workspaceTree = $(@el).children('#workspace-tree')
      
      $projectsContainer = $(@$workspaceTree).children('ul.projects')
      for projectId, projectView in @projectViews
        $($projectsContainer).append $(projectView.render().el)
      @bindEvents()
      @
      
    bindEvents: ()=>
      @$el.find('#file-to-upload').change (evt)=>
        if evt.target.files.length > 0
          $parent = $('#workspace-tree').jstree('get_selected')
          parentPath = $parent.attr('id')
          if $parent.hasClass 'file'
            $parent.closest('.folder')
          
          $project = $($parent).closest('li.project')
          projectId = $project.attr('id')
          projectModel = @model.get projectId

          parentPath = parentPath.substr(parentPath.indexOf("_PATH_")+6)
          
          path = "/"
          if (parentPath isnt projectId) and (parentPath isnt path)
            path = parentPath+'/'
          
  
          files = evt.target.files
          fileInfos = []
          for f in files
            #tempFunc is necessary to make sure file reader nows "filename"
            tempFunc = (f)=>
              filename = escape(f.name)
              filepath = path+filename
  
              fileInfos.push({
                "name": escape(f.name)
                'filepath': filepath
                "type": f.type
                "size": f.size
                "modified": f.lastModifiedDate.toLocaleDateString()
              })
              
              $('#workspace-tree').jstree('open_node', $parent)
              
              resource = projectModel.getResourceByPath(filepath)
              revision = -1
              if !!resource
                revision = resource.get('revision')
                $treeItem = $("li.file[id*='#{filepath}']")
                $treeItem.addClass('loading')
              else
                newNode = { attr: {class: 'loading delete-me-on-update'}, state: "leaf", data: filename }
                obj = $('#workspace-tree').jstree("create_node", $parent, 'inside', newNode, false, true)
                obj.attr('id', filepath)
                
              reader = new FileReader()
              reader.onload = (event)=>
                $.ajax({
                  url: jsRoutes.controllers.ProjectController.putFile(projectId, filepath, false, revision).url
                  type: 'PUT'
                  processData: false
                  contentType: 'application/octet-stream'
                  data: event.target.result
                  dataType: 'json'
                  error: ()->
                    $objToDelete = $(".loading.delete-me-on-update")
                    if $objToDelete.size() > 0
                      $('#workspace-tree').jstree("delete_node", $objToDelete)
                    $('.loading').removeClass('loading')
                    document.log 'error while uploading file.'
                  statusCode:
                    401: ()->
                      $objToDelete = $(".loading.delete-me-on-update")
                      if $objToDelete.size() > 0
                        $('#workspace-tree').jstree("delete_node", $objToDelete)
                        
                      $('li.loading').removeClass('loading')
                      document.log 'Error while uploading file. Unauthorized.'
                  success: ()->
                    $('li.loading').removeClass('loading')
                    
                    
                })
              reader.readAsArrayBuffer(f);
            tempFunc(f)
          # in case user wants to upload the same file again after changing it
          # http://stackoverflow.com/questions/1043957/clearing-input-type-file-using-jquery
          $('#file-to-upload').wrap('<form>').closest('form').get(0).reset();
          $('#file-to-upload').unwrap();
      
  module.exports = Workspace