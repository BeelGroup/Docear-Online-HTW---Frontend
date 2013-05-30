require ['logger', 'views/templates.pre.min', 'NodeFactory','feedbackForm', 'views/MapView','routers/DocearRouter', 'views/RootNodeView', 'views/CanvasView', 'views/MinimapView', 'views/ZoomPanelView', 'models/RootNode', 'config', 'features', 'ribbons', 'collections/Workspace', 'collections/Files', 'models/Project', 'models/File', 'views/WorkspaceView'],  (logger, templates, NodeFactory, FeedbackForm, MapView, DocearRouter, RootNodeView, CanvasView, MinimapView, ZoomPanelView, RootNodeModel, config, features, ribbons, Workspace, Files, Project, File, WorkspaceView) -> 

  # load user maps for dropdown menu
  loadUserMaps = ->
    $.ajax({
      type: 'GET',
      url: jsRoutes.controllers.User.mapListFromDB().url,
      dataType: 'json',
      success: (data)->
        $selectMinmap = $('.select-mindmap')
        mapLatestRevision = {}
        if data.length> 0
          $.each(data, (index,value)->
            if typeof mapLatestRevision[value.mmIdInternal] == "undefined" or mapLatestRevision[value.mmIdInternal].revision < value.revision
              dateRevision = new Date(value.revision)
              mapLatestRevision[value.mmIdInternal] = {}
              mapLatestRevision[value.mmIdInternal].map = value
              mapLatestRevision[value.mmIdInternal].revision = dateRevision.getTime()
          )
          $selectMinmap.empty()
          $.each(mapLatestRevision, (id,value)->
            date = $.datepicker.formatDate("dd.mm.yy", new Date(value.map.revision))
            link = """<li><a class="dropdown-toggle" href="#{jsRoutes.controllers.Application.index().url}#map/#{value.map.mmIdOnServer}"> #{value.map.fileName} (#{date})</a></li>"""
            $selectMinmap.append link
          )
    })

  if document.body.className.match("login-page")
    $("#username").focus()
  else if document.body.className.match('is-authenticated')
    loadUserMaps()
  
  initialLoad = ->
    if $('#mindmap-container').size() > 0

      $('#mindmap-container').fadeIn()
      
      mapView = new MapView('mindmap-viewport')
      mapView.renderAndAppendTo($('#mindmap-container'))
      router = new  DocearRouter(mapView)

      if typeof mapView.mapId == 'undefined'
        mapView.loadMap("welcome")

  initialLoad()
  
  if($.inArray('RIBBONS', document.features) > -1)
    initRibbons()
  
    ###
  files = new Files([new File('file1'), new File('file2'), new File('file3')])
  project1 = new Project("Project1", files)
  project2 = new Project("Project2", files)
  workspace = new Workspace([project1, project2])
  
  workspaceView = new WorkspaceView(workspace)
  $('#mindmap-container').before(workspaceView.render().element())
  ###
  
  workspaceVisible = false
  $('.toggle-workspace-sidebar').live('click', (event)=>
    width = $('.workspace-container:first').outerWidth()
    if !workspaceVisible
      $('#mindmap-container').animate({
        "margin-left": "#{width}px"
        "width": "-=#{width}"
      })
    else
      $('#mindmap-container').animate({
        "margin-left": "0px"
        "width": "+=#{width}"
      })
    workspaceVisible = !workspaceVisible
    false
  )
  
  
  $("#add-new-project").live('click', ->
    name = $(this).parent().find('input.project-name').val();

    params = {
      url: jsRoutes.controllers.ProjectController.createProject().url
      type: 'POST'
      cache: false
      data: {"name": name}
      success: (data)->
        console.log data
      dataType: 'json' 
    }
    $.ajax(params)
  )