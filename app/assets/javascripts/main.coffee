require ['raphael-min', 'json2', 'mousetrap.min', 'jquery.mousewheel.min', 'jquery.hotkeys', 'jquery.transformAnimate', 'jquery.json-2.4.min', 'jquery.svg.min', 'jquery.svganim.min', 'bootstrap-wysiwyg', 'views/templates.pre.min', 'NodeFactory','feedbackForm', 'logger', 'views/MapView','routers/DocearRouter', 'views/RootNodeView', 'views/CanvasView', 'views/MinimapView', 'views/ZoomPanelView', 'models/RootNode', 'config', 'features'],  (raphael, json2, mousetrap, mousewheel, JQHotkeys, JQTransformAnimate, JQJson, JSSvg ,JQSvgAnim, bootstrapWysiwyg, templates, NodeFactory, FeedbackForm, Logger, MapView, DocearRouter, RootNodeView, CanvasView, MinimapView, ZoomPanelView, RootNodeModel, config, features) -> 

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