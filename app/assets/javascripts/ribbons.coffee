  initRibbons = ()->
    $( "#ribbons .ribbon-tabs li.tab a" ).on("click", ()->
      ribbonId = $(this).attr('href')
      $('#ribbons .ribbon-tabs li.tab').removeClass('active')
      
      $(this).parent().addClass('active')
      if $(ribbonId).is(':visible')
        $(ribbonId).hide()
      else
        $('.ribbon').hide()
        $(ribbonId).show()
      
      false
    )

    $firstTab = $( "#ribbons .ribbon-tabs li.tab:first a" )
    if  $firstTab.size() > 0
      $active = $( "#ribbons .ribbon-tabs li.tab.active:first a" )
      if $active.size() > 0
        $active.click()
      else 
        $firstTab.click()