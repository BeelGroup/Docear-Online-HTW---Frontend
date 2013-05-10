  initRibbons = ()->
    $( "#ribbons .nav-tabs li.tab a" ).live("click", ()->
      ribbonId = $(this).attr('href')
      $('#ribbons .nav-tabs li.tab').removeClass('active')
      
      $(this).parent().addClass('active')
      if $(ribbonId).is(':visible')
        $(ribbonId).hide()
      else
        $('.ribbon').hide()
        $(ribbonId).show()
      
      false
    )
    
    $active = $( "#ribbons .nav-tabs li.tab.active:first a" )
    if $active.size() > 0
      $active.click()
    else
      $( "#ribbons .nav-tabs li.tab:first a" ).click()