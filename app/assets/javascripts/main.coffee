require [],  () ->   
  loadUserMaps = ->
    $.ajax({
      type: 'GET',
      url: jsRoutes.controllers.MindMap.mapListFromDB().url,
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
            $selectMinmap.append """<li><a class="dropdown-toggle" href="#loadMap/#{id}"> #{value.map.fileName}</a></li>"""
          )
    })
    
  if $("body").hasClass("login-page")
    $("#username").focus()
  else if $('.logoutButton').size() > 0
    loadUserMaps()