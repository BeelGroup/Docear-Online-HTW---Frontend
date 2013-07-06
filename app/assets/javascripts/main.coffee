require ['logger', 'views/templates.pre.min', 'NodeFactory','feedbackForm', 'views/mindmap/MapView','routers/DocearRouter', 'views/mindmap/RootNodeView', 'views/mindmap/CanvasView', 'views/mindmap/MinimapView', 'views/mindmap/ZoomPanelView', 'models/mindmap/RootNode', 'config', 'features', 'ribbons', 'collections/workspace/Workspace', 'collections/workspace/Resources', 'models/workspace/Project', 'models/workspace/Resource', 'views/workspace/WorkspaceView'],  (logger, templates, NodeFactory, FeedbackForm, MapView, DocearRouter, RootNodeView, CanvasView, MinimapView, ZoomPanelView, RootNodeModel, config, features, ribbons, Workspace, Files, Project, File, WorkspaceView) -> 

  if document.body.className.match("login-page")
    $("#username").focus()
  
  initialLoad = ->
    if $('#mindmap-container').size() > 0

      $('#mindmap-container').fadeIn()
      
      mapView = new MapView('mindmap-viewport')
      mapView.renderAndAppendTo($('#mindmap-container'))
      router = new  DocearRouter(mapView)

      if typeof mapView.mapId == 'undefined'
        mapView.loadMap '-1', 'welcome'

  initialLoad()
  
  if($.inArray('RIBBONS', document.features) > -1)
    initRibbons()
  
  workspaceVisible = false
  $('.toggle-workspace-sidebar').live('click', (event)=>
    $workspaceContainer = $('#workspace-container')
    $workspaceContainer.show()
    width = $workspaceContainer.outerWidth()
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
  $('.alert .close').live("click", (e)-> 
    $($(@).parent()).hide()
  )
