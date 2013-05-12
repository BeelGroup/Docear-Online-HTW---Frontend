require ['preloader', 'views/templates.pre.min', 'NodeFactory','feedbackForm', 'logger', 'views/MapView','routers/DocearRouter', 'views/RootNodeView', 'views/CanvasView', 'views/MinimapView', 'views/ZoomPanelView', 'models/RootNode', 'config', 'features'],  (preloader, templates, NodeFactory, FeedbackForm, Logger, MapView, DocearRouter, RootNodeView, CanvasView, MinimapView, ZoomPanelView, RootNodeModel, config, features) -> 

  # load user maps for dropdown menu
  loadUserMaps = ->
      $.ajax({
        type: 'GET',
        url: jsRoutes.controllers.User.mapListFromDB().url,
        dataType: 'json',
        success: (data)->
          $selectMinmap = $('#select-mindmap')
          
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
              $selectMinmap.append """<li><a class="dropdown-toggle" href="#{jsRoutes.controllers.Application.index().url}#map/#{value.map.mmIdOnServer}"> #{value.map.fileName} (#{date})</a></li>"""
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