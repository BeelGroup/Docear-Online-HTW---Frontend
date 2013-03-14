require [],  () ->   
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
            $selectMinmap.append """<li><a class="dropdown-toggle" href="#loadMap/#{value.map.mmIdOnServer}"> #{value.map.fileName} (#{date})</a></li>"""
          )
    })
    
  formToJson = ($form)->
    result =  {}
    $.each($($form).find('input, textarea, select'), (index, $field)->
      name = $($field).attr('name')
      if name != ''
        result[name] = $($field).val()
    )
    result
    
  resetForm = ($form)->
    $($form).find(':input').each(
      ->
        if this.type in ['password', 'select-multiple', 'select-one', 'text', 'textarea']
          $(this).val('')
        else if this.type in ['checkbox', 'radio']
          this.checked = false
    )
    
  $('form.feedback-form').submit ->
    formURL = $(this).attr('action')
    formType = $(this).attr('method')
    
    $form = $(this)
    formData = {}
    formData = formToJson($(this))
    $.ajax({
      type: formType,
      url: formURL,
      data: formData,
      success: (data)->
        $($form).find('.close:first').click()
        resetForm $($form)
        $($form).find('.alert:first').hide()
      ,
      statusCode: {
        400: (data)->
          $messageContainer = $($form).find('.alert:first')
          $($messageContainer).find('.form-warning:first .type').text('ERROR!')
          $($messageContainer).find('.form-warning:first .message').text('Feedback could not be saved!')
          $($messageContainer).fadeIn()
      }
    })
    false
  
  if $("body").hasClass("login-page")
    $("#username").focus()
  else if $("body").hasClass('is-authenticated')
    loadUserMaps()