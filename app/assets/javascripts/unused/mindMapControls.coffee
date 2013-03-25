
  
  changeZoom = (direction)->
    ## global variable?? :P
    ###
    zoomFactor = 0;
    if direction > 0
      zoom = zoom / 1.5
      zoomFactor--
    else
      zoom = zoom * 1.5
      zoomFactor++
    
    $("#zoom-factor").text(zoomFactor);
    
    $("#mindmap").html("")
    current_map_drawer = new MindMapDrawer(current_mindmap, $("#mindmap"))
    current_map_drawer.setZoom(zoom)
    current_map_drawer.draw()
    ###
    #current_map_drawer.refreshDom() 
    
  $('#mindmap').mousewheel (e, delta)-> 
    changeZoom(e.originalEvent.detail)
    false
  
  $('body').on("mouseenter", ".node", 
    -> 
      $(this).children('i.fold:first, .controls').show()
      false
  )
  $('body').on("mouseleave", ".node",
    -> 
      if not $(this).hasClass('selected')
        $(this).children('i.fold:first, .controls').hide()
      false
  )
    
  $('body').on("click", ".node i.fold", 
    ->
      if $(this).hasClass('icon-minus-sign')
        $(this).parent().children('.children').fadeOut('fast')
      else
        $(this).parent().children('.children').fadeIn('fast')
      $(this).toggleClass('icon-minus-sign')
      $(this).toggleClass('icon-plus-sign')
    )

  $('body').on("click", ".node .action-edit",
    -> 
      innerNode = $(this).closest('.node').children('.inner-node')  
      if $(this).hasClass('action-save')
        editArea = innerNode.find('textarea:first')
        innerNode.html(editArea.text())
      else
        editArea = $('<textarea class="changeable"></textarea>')
        editArea.css('width', (innerNode.width()+10)+'px')
        editArea.css('height', (innerNode.height()+10)+'px')
        editArea.text(innerNode.html())
        innerNode.empty().append(editArea)
      $(this).toggleClass('icon-ok')
      $(this).toggleClass('icon-edit')
      $(this).toggleClass('action-save')
      false
  )