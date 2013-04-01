define ->
  module = () ->

  class SyncedView extends Backbone.View
      

    constructor:()->
      super()
      

    delegateEvents: (events) ->
      if @model?
        @fieldMap = @fieldMap or {}
        for selector, field of @fieldMap
          # bind selector change to the model
          @delegateModelSync(selector, field)
          # bind form element change events to sync up to the model
          @delegateFormSync selector, field


    delegateFormSync: (selector, field) ->
      $(@el).delegate selector, "change", (e) =>
        @syncFormToModel e, selector, field


    delegateModelSync: (selector, field) ->
      delegateMethod = () ->
        @syncModelToForm(selector, field)

      if field.field?
        @model.bind "change:#{ field.field }",delegateMethod , @ 
      else
        @model.bind "change:#{ field }", delegateMethod, @ 


    syncModelToForm: (selector, field) ->
        # check if toForm was defined (complex map entry)
        if field.toForm?
          do @[field.toForm]
        else
          value = @model.get field
        # !here is still work to do!
        #if @$(selector)[0].nodeName == 'INPUT'
        #  @$(selector).val(value)
        #else  
        @$(selector).val value


    syncFormToModel: (jqEvent, selector, field) ->
      setOb = {}
      # check if toModel was defined (complex map entry)
      if field.toModel?
        value = do @[field.toModel]
        field = field.field
      else
        value = @$(selector).val()

      @model.set field, value, (error) -> console.log error


  module.exports = SyncedView