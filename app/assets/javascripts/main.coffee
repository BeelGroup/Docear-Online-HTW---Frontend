require ['logger', 'views/templates.pre.min', 'NodeFactory','feedbackForm', 'views/mindmap/MapView','routers/DocearRouter', 'views/mindmap/RootNodeView', 'views/mindmap/CanvasView', 'views/mindmap/MinimapView', 'views/mindmap/ZoomPanelView', 'models/mindmap/RootNode', 'config', 'features', 'ribbons', 'collections/workspace/Workspace', 'collections/workspace/Resources', 'models/workspace/Project', 'models/workspace/Resource', 'views/workspace/WorkspaceView'],  (logger, templates, NodeFactory, FeedbackForm, MapView, DocearRouter, RootNodeView, CanvasView, MinimapView, ZoomPanelView, RootNodeModel, config, features, ribbons, Workspace, Files, Project, File, WorkspaceView) -> 

  document.addURLParam = (url, param, value, urlEncode = false)->
    result = url
    if result.indexOf('?') < 0
      result +=  "?"
    else
      result +=  "&"
    result += "#{param}="
    if urlEncode
      result += encodeURIComponent(value)
    else
      result += value
    result

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
  
  $('.alert .close').live("click", (e)-> 
    $($(@).parent()).hide()
  )
