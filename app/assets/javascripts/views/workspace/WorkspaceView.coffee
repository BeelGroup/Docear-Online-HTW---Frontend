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
      @model._editMode = false

    resize:(widthAndHeight)->
      @$el.css
        height: widthAndHeight.height+"px"
      tollbarHeight = @$el.children('.toolbar').outerHeight()+10
      @$el.children('.scroll-container').css
        height: (widthAndHeight.height-tollbarHeight)+'px'

    initialize : ()->
      @projectViews = {}
      @model.each (project)=>
        @projectViews[project.get('id')] = new ProjectView(project, @)

    remove: (project)->
      $objToDelete = $("li##{project.get('id')}")
      $objToDelete.addClass('deleted').addClass('temp-project delete-me-on-update')
      delete @projectViews[project.get('id')]
      if $objToDelete.size() > 0
        $('#workspace-tree').jstree("remove", $objToDelete)
       
    initJsTree: ->
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
          else if $(data.args[0]).hasClass 'temp-mindmap-file'
            @requestCreateMindMap(data.args[0], data.args[1])
          else if $(data.args[0]).hasClass 'to-be-renamed'
            @requestRenameResource(data.args[0], data.args[1])
          else if $(data.args[0]).hasClass 'temp-user'
            @requestAddUser(data.args[0], data.args[1])
          else
            @moveResource()
        else if type is 'dblclick'
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
      $deleted = $('.project.deleted')
      if $deleted.size() > 0
        for $del in $deleted
          $('#workspace-tree').jstree("delete_node", $deleted)
      
    add: (project)->
      projectView = new ProjectView(project, @)
      @projectViews[project.get('id')] = projectView
      
      if @_rendered        
        $objToDelete = $(".temp-project.delete-me-on-update")
        if $objToDelete.size() > 0
          $('#workspace-tree').jstree("delete_node", $objToDelete)
        $(@el).find('#workspace-tree ul:first').append $(projectView.render().el)
        @initJsTree()


    refreshNode: ($node) =>
      @$workspaceTree.jstree 'refresh', $node


    customMenu:(node) =>
      @$workspaceTree.jstree 'deselect_all'
      @$workspaceTree.jstree "select_node", node, true

      #add default items
      items = new Object()

      if ($(node).hasClass("mindmap-file"))
        items.openMindmap = 
          label: "Open mind map",
          action: @openMindmap

      if ($(node).hasClass("folder")) 
        items.createMapItem = 
          label: "Create new mind map"
          action: @newMindMap
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
          action: @addUser

      if($(node).hasClass("file") && !$(node).hasClass("delete-me-on-update"))
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
      
      if $(node).hasClass("resource")
        items.renameItem =
          label: "Rename"
          action:  @renameResource

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
        
    requestAddFile:(liNode, a,b)=>
      if $.inArray('WORKSPACE_UPLOAD', document.features) > -1
        $('#file-to-upload').click()
      false


    requestAddFolder:(liNode, a, b)->
      $parent = $('#workspace-tree').jstree('get_selected')

      $('#workspace-tree').jstree('open_node', $parent)
      newNode = { attr: {class: 'resource loading folder delete-me-on-update', id:'fuadadeimuada'}, state: "closed", data: "New folder" }
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

        dirtyPath = projectId+'_PATH_'+currentPath
        cleanPath = dirtyPath.replace new RegExp("/", "g"), "\\/"
        cleanPath = cleanPath.replace new RegExp("\\.", "g"), "\\."
        cleanPath = cleanPath.replace new RegExp(" ", "g"), "\\ "
        competingObjects = $('#'+cleanPath)        

        if competingObjects.size() < 1
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
        else
          firstObj = $(competingObjects[0])
          pos = $(firstObj).position()
          pos.top = pos.top
          $("#multiplename-error").css 'top', pos.top
          $("#multiplename-error").find('.message').html('Sorry, but this name is already in use.')
          $("#multiplename-error").show()
          $('#workspace-tree').jstree("delete_node", obj)
      )


    addUser: (userName = "new_user")=>
      @model._editMode = true
      if typeof userName isnt "string"
        userName = "new_user"
      $parent = $('#workspace-tree').jstree('get_selected')
      $('#workspace-tree').jstree('open_node', $parent)
      obj = $('#workspace-tree').jstree("create_node", $parent, 'inside', userName, false, true)
      $(obj).addClass('delete-me-on-update')
      $(obj).addClass('temp-user')
      $(obj).addClass('user')
      $("#workspace-tree").jstree("rename",obj)
      
      
    # jstree functions are required, so dont use a fatarrow here
    requestAddUser:(obj, userName)=>
      @model._editMode = false
      if !userName or userName is "new_user" or userName.replace(/\s+/g, '') is ""
        $('#workspace-tree').jstree("delete_node", obj)
      else
        $parent  = $('#workspace-tree').jstree('get_selected')
        $project = $($parent).closest('li.project')

        projectId   = $project.attr('id')
        competingObjects = $('#'+projectId).find(".user a[id='"+userName+"']")

        if competingObjects.size() < 1
          $(obj).children('a').attr('id', userName)
          params = {
            url: jsRoutes.controllers.ProjectController.addUserToProject(projectId).url
            type: 'POST'
            cache: false
            data: {"username": userName}
            success:(data)=>
              # create new model and add to parent
              document.log "user \'"+userName+"\' was added to project "+projectId
            error:()=>
              document.log "Error while adding user : "+userName+" to project "+projectId
              # remove folder from view
              #$('#workspace-tree').jstree("delete_node", obj)
            statusCode:
              403: ()->
                # remove user from view
                $('#workspace-tree').jstree("delete_node", obj)
                document.log 'User already in project.'
            dataType: 'json' 
          }
          $.ajax(params)
        else
          firstObj = $(competingObjects[0])
          pos = $(firstObj).position() 
          pos.top = pos.top + $(firstObj).outerHeight()
          $("#multiplename-error").css 'top', pos.top
          $("#multiplename-error").find('.message').html('This user already exists.')
          $("#multiplename-error").show()
          $('#workspace-tree').jstree("delete_node", obj)

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

      if oldPath isnt newPath
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
      $selectedItem = $('#workspace-tree').jstree('get_selected') 
      userName = $selectedItem.children('a').attr('id')
      if $selectedItem.hasClass('user')
        params = {
          url: jsRoutes.controllers.ProjectController.removeUserFromProject(itemData.projectId).url
          type: 'POST'
          cache: false
          data: {"username": userName}
          success:(data)=>
            document.log "SUCCESS: The user \'"+userName+"\' was removed from project \'"+itemData.projectId+"\'"
          error:()=>
            document.log "ERROR: The user \'"+userName+"\' wasnt removed from project \'"+itemData.projectId+"\'"
          dataType: 'json' 
        }
        $.ajax(params)  


    getSelectedItemData:->
      $selectedItem = $('#workspace-tree').jstree('get_selected')    
      $project = $($selectedItem).closest('li.project')

      itemData = 
        projectId : $project.attr('id')
        # get text from node and remove whitespaces
        name : $selectedItem.text()
        path: $selectedItem.attr('id')

      itemData
    
    isValidFilename: (fileName, $currentObject = null)=>
      illegalCharRegex = new RegExp("[#{document.illegalFilenameCharacter}]+")
      isValid = !fileName.match(illegalCharRegex) and fileName.replace(/\s+/g, '') isnt ""
      $message = $('#illegal-character-error').hide()
      
      if $currentObject isnt null and !isValid
        pos = $($currentObject).position() 
        pos.top = pos.top + $($currentObject).outerHeight()
        if $message.size() > 0
          $message.css 'top', pos.top
          $message.show()
      isValid
      
    requestCreateProject: (obj, projectName)=>
      @model._editMode = false
      if !@isValidFilename(projectName, obj)
        $('#workspace-tree').jstree("delete_node", obj)
        @newProject(projectName)
      else
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
        
    newProject: (projectName = "new project")=>
      @model._editMode = true
      if typeof projectName isnt "string"
        projectName = "new project"
      obj = $("#workspace-tree").jstree("create","#workspace-tree","last",projectName, false, true)
      $(obj).addClass('project temp-project delete-me-on-update')
      $("#workspace-tree").jstree("rename",obj)
    
    requestCreateMindMap: (obj, fileName)=>
      @model._editMode = false
      if !@isValidFilename(fileName, obj)        
        $parent  = $('#workspace-tree').jstree('get_selected')
        $('#workspace-tree').jstree("delete_node", obj)
        @newMindMap(fileName)
      else
        indexOfDot = fileName.lastIndexOf('.')
        nameEnding = fileName.substring(indexOfDot+1)
        if indexOfDot < 0 or nameEnding isnt 'mm'
          fileName += '.mm'
        
        $parent  = $('#workspace-tree').jstree('get_selected')
        $project = $($parent).closest('li.project')
  
        currentPath = $parent.attr('id')
        currentPath = currentPath.substr(currentPath.indexOf("_PATH_")+6)
  
        if currentPath isnt "/"
          $path = currentPath+"/"+fileName 
        else
          $path = currentPath+fileName 
  
        # set path as id -> so it will be found and can be removed on update from server
        $(obj).attr("id", $path)
        $(obj).removeClass('temp-mindmap-file')
        $("#workspace-tree").jstree('rename_node', obj , fileName)
        # build path
        if currentPath[currentPath.length-1] isnt '/'
          currentPath += "/"
        currentPath += fileName
  
        projectId   = $project.attr('id')
        # set id in dom
        obj[0].id = currentPath
  
        competingObjects = $('#'+projectId).find(".file[id*='"+currentPath+"']").not('.delete-me-on-update')
        if competingObjects.size() < 1
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
        else
          firstObj = $(competingObjects[0])
          pos = $(firstObj).position()
          pos.top = pos.top
          $("#multiplename-error").css 'top', pos.top
          $("#multiplename-error").find('.message').html('Sorry, but this name is already in use.')
          $("#multiplename-error").show()
          $('#workspace-tree').jstree("delete_node", obj)
    
    newMindMap: (filename = "new_mindmap.mm")=>
      @model._editMode = true
      if typeof filename isnt "string"
        filename = "new_mindmap.mm"
      $parent = $('#workspace-tree').jstree('get_selected')
      
      if $parent.size() > 0 and ($parent.hasClass('resource') or $parent.hasClass('resources'))
        if $parent.hasClass('file')
          $('#workspace-tree').jstree("deselect_node", $parent)
          $parent = $parent.closest('.folder')
          $('#workspace-tree').jstree("select_node", $parent)
        obj = $("#workspace-tree").jstree("create",$parent,"last",filename, false, true)
        $(obj).addClass('resource loading file delete-me-on-update temp-mindmap-file')
        $("#workspace-tree").jstree("rename",obj)
      else
        $firstObj = $("#workspace-container").find('.add-mindmap-toggle:first')
        pos = $firstObj.position()
        pos.top = pos.top + $firstObj.outerHeight()
        $("#no-selection-error").css 'top', pos.top
        $("#no-selection-error").show()
    
    renameResource: ($obj = null)=>
      @model._editMode = true
      if $obj is null
        $obj = $('#workspace-tree').jstree('get_selected')
      $obj = $('#workspace-tree').jstree('get_selected')
      $($obj).addClass('to-be-renamed').addClass('loading')
      $("#workspace-tree").jstree("rename", $obj)
    
    requestRenameResource: (obj, newName)=>
      @model._editMode = false
      oldPath = $(obj).attr('id')
      oldPath = oldPath.substr(oldPath.indexOf("_PATH_")+6)
      
      lastSlashPos = oldPath.lastIndexOf('/')
      parentPath = oldPath.substring(0, lastSlashPos)
      oldName = oldPath.substring(lastSlashPos+1)
      newPath = parentPath+"/"+newName
      
      if !@isValidFilename(newName, obj)
        $(obj).removeClass('to-be-renamed').removeClass('loading')
        $("#workspace-tree").jstree('set_text', obj , oldName )
      else
  
        #if oldPath isnt newPath
        if !@isValidFilename(newName, obj)
          $('#workspace-tree').jstree("rollback", obj)
          @renameResource(obj)
        else if oldPath isnt newPath
          projectId = $(obj).closest('li.project').attr('id')
          params = 
            url: jsRoutes.controllers.ProjectController.moveFile(projectId).url
            type: 'POST'
            cache: false
            data: "currentPath": oldPath, "moveToPath": newPath
  
            success:()=>
              document.log "SUCCESS: Resource \'"+oldName+"\' was be moved from "+oldPath+" to "+newPath
            error:()=>
              document.log "ERROR: resource \'"+oldName+"\' could not be moved from "+oldPath+" to "+newPath
            dataType: 'json' 
          $.ajax(params)  
    
    events:
      "click .add-project-toggle" : "newProject"
      "click .add-mindmap-toggle" : "newMindMap"

    element:-> @$el

    getCleanedPath:(dirtyPath)->
      cleanPath = dirtyPath.replace new RegExp("/", "g"), "\\/"
      cleanPath = cleanPath.replace new RegExp("\\.", "g"), "\\."
      cleanPath = cleanPath.replace new RegExp(" ", "g"), "\\ "

    render:()->
      @_rendered = true
      options = {}
      options.upload_enabled = $.inArray('WORKSPACE_UPLOAD', document.features) > -1
      @$el.html @template options
      @$workspaceTree = $(@el).find('#workspace-tree')
          
      $projectsContainer = $(@$workspaceTree).children('ul.projects')
      for projectId, projectView in @projectViews
        $($projectsContainer).append $(projectView.render().el)
      @bindEvents()
      
      setTimeout(=>
        @initJsTree()
      , 500)
      
      $(@$el).resizable({
        handles: 'e'
        start: (event, ui)->
          $('#workspace-container').find('.toggle-workspace-sidebar.link i').removeClass('icon-double-angle-right').addClass('icon-double-angle-left')
      });
      $(@$el).find('.ui-resizable-handle').addClass('toggle-workspace-sidebar')
      $('body').on 'click', '.toggle-workspace-sidebar', ->
        workspaceWidth = $('#workspace-container').width()
        newWidth = workspaceWidth
        
        if workspaceWidth <= 0
          newWidth = -$('#workspace-container').attr('data-prev-width')
          $('#workspace-container').find('.toggle-workspace-sidebar.link i').removeClass('icon-double-angle-right').addClass('icon-double-angle-left')
        
        $('#workspace-container').attr('data-prev-width', workspaceWidth)
        
        $('#workspace-container').animate
          'width': '-='+newWidth+'px'
        , document.fadeDuration, =>
          if workspaceWidth > 0
            $('#workspace-container').find('.toggle-workspace-sidebar.link i').removeClass('icon-double-angle-left').addClass('icon-double-angle-right')
        $('#mindmap-container, #mindmap-viewport').animate
          'width': '+='+newWidth+'px'
        , duration: document.fadeDuration
            
      
      @
      
    uploadFile: (file, projectId, filepath, revision)=>
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
            document.log 'error while uploading file.'
          statusCode:
            401: ()->
              $objToDelete = $(".loading.delete-me-on-update")
              if $objToDelete.size() > 0
                $('#workspace-tree').jstree("delete_node", $objToDelete)
              document.log 'Error while uploading file. Unauthorized.'
        })
      reader.readAsArrayBuffer(file);
      
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
            filename = escape(f.name)
            if @isValidFilename(filename, $parent)
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
                
                resource.update null, =>
                  @uploadFile(f, projectId, filepath, revision)
              else
                newNode = { attr: {class: 'loading delete-me-on-update'}, state: "leaf", data: filename }
                obj = $('#workspace-tree').jstree("create_node", $parent, 'inside', newNode, false, true)
                obj.attr('id', filepath)
                @uploadFile(f, projectId, filepath, revision)
          
          # in case user wants to upload the same file again after changing it
          # http://stackoverflow.com/questions/1043957/clearing-input-type-file-using-jquery
          $('#file-to-upload').wrap('<form>').closest('form').get(0).reset();
          $('#file-to-upload').unwrap();
      
  module.exports = Workspace