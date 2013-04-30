require [],  () ->  

  # tanslate infos from feedback formular to json object
  formToJson = ($form)->
    result =  {}
    $.each($($form).find('input, textarea, select'), (index, $field)->
      name = $($field).attr('name')
      if name != ''
        result[name] = $($field).val()
    )
    result
    
  # reset fedback form
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
    
    $footer = $($form).find('.modal-footer:first')
    
    $ajaxLoader = $footer.find('.loader:first')
    
    $footer.children('.btn-primary').hide()
    $ajaxLoader.show()
    
    formData = {}
    formData = formToJson($(this))
    $form.find('.control-group.error').removeClass('error')
    
    # reset messages
    $messageContainer = $($form).find('.alert:first')
    $messages = $($messageContainer).find('.form-warning:first ul.message')
    $messages.empty()
    $messageContainer.fadeOut()
    
    $.ajax({
      type: formType,
      url: formURL,
      data: formData,
      success: (data)->
        $($form).find('.close:first').click()
        resetForm $($form)
        $($form).find('.alert:first').hide()
        $ajaxLoader.hide()
        $footer.children().show()
      ,
      statusCode: {
        400: (xhr, textStatus, errorThrown)->
          $ajaxLoader.hide()
          $footer.children('.btn-primary').show()
          
          messages = jQuery.parseJSON(xhr.responseText);
          
          $($messageContainer).find('.form-warning:first .type').text('ERROR!')
          $.each(messages, (id, message)->
            $(""":input[name=#{id}]""").closest('.control-group').addClass('error')
            $messages.append("""<li>#{message}</li>""")
          )
          
          $($messageContainer).fadeIn()
      }
    }, 'json')
    false