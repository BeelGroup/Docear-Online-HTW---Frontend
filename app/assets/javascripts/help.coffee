require [],  () -> 
  $helpMenu = $('#help-menu ul.menu')
  id = 1
  
  $.each($('#help-content .paragraph'), (index, paragraph)->
    currentLevel = 0
    $currentSubMenu = $helpMenu
    
    $.each($(paragraph).children('h1, h2, h3, h4, h5'), (index2, headline)->
      level = parseInt($(headline).prop("tagName").replace(/[^0-9]/g, ""))
      helpId = """help_#{id++}"""
      $(headline).attr('id', helpId)
      
      $newSub = undefined
      if currentLevel < level
        if currentLevel != 0
          $newSub = $('<ul></ul>')
          $currentSubMenu.append($newSub)
          $currentSubMenu = $newSub
        currentLevel = level
      else if currentLevel > level
        while currentLevel > level
          currentLevel--
          $currentSubMenu = $currentSubMenu.parent().closest('ul')

      $currentSubMenu.append("""<li><a href="##{helpId}">#{$(headline).text()}</a></li>""")
        
    )
    
  )
  