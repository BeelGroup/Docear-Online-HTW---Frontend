require [],  () ->   
   
  if $("body").hasClass("login-page")
    $("#username").focus()
  
  loadUserMaps = ->
    $.ajax({
      type: 'GET',
      url: jsRoutes.controllers.MindMap.mapListFromDB().url,
      dataType: 'json',
      success: (data)->
        $selectMinmap = $('#select-mindmap')
        
        mapLatestRevision = {}
        $.each(data, (index,value)->
          if typeof mapLatestRevision[value.mmIdOnServer] == "undefined" or mapLatestRevision[value.mmIdOnServer].revision < value.revision
            mapLatestRevision[value.mmIdOnServer] = {}
            mapLatestRevision[value.mmIdOnServer].map = value
            mapLatestRevision[value.mmIdOnServer].revision = value.revision
        )
        $.each(mapLatestRevision, (id,value)->
          $selectMinmap.append """<li><a class="dropdown-toggle" href="#loadMap/#{id}"> #{value.map.fileName} #{value.map.revision}</a></li>"""
        )
    })
  loadUserMaps()