define ['routers/DocearRouter'],  (DocearRouter) ->  
  module = () ->
  
  class PersistenceHandler extends Backbone.Model

    constructor: (mapId)->
      super()
      @mapId = mapId
      console.log mapId
      
      @persistenceApi = {
        'change': {
          'Node': jsRoutes.controllers.MindMap.changeNode(mapId).url
        },
        'create': {
          'Node': jsRoutes.controllers.MindMap.createNode(mapId).url
        },
        'delete': {
          'Node': jsRoutes.controllers.MindMap.deleteNode(mapId).url
        }
      }

    persistChanges: (object, changes)->
      objectName = object.constructor.name
      values = {}
      if @persistenceApi.change[objectName] != undefined
        
        changesToPersist = false
        $.each changes.changed, (attr, value)->
          if attr in object.get('attributesToPersist')
            values[attr] = object.get attr
            changesToPersist = true
        if changesToPersist
          params = {'nodeId': object.get('id'), 'attributeValueMapJson': $.toJSON(values)}
          $.post(@persistenceApi.change[objectName], params)

    persistNew: (object, params)->
      objectName = object.constructor.name
      params = {'json': $.toJSON(params)}
      $.post(@persistenceApi.create[objectName], params, (data)->
        object.addChild data.id, data.nodeText
      , 'json')

  module.exports = PersistenceHandler