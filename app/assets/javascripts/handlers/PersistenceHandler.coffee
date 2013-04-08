define ['routers/DocearRouter'],  (DocearRouter) ->  
  module = ->
  
  class PersistenceHandler

    persistenceApi: {
      'change': {
        'Node': jsRoutes.controllers.MindMap.changeNode().url
      },
      'create': {
        'Node': jsRoutes.controllers.MindMap.createNode().url
      },
      'delete': {
        'Node': jsRoutes.controllers.MindMap.deleteNode().url
      }
    }

    persistChanges: (object, d)->
      objectName = object.constructor.name
      values = {}
      if @persistenceApi.change[objectName] != undefined
        #TODO: only transfer changed values
        for attribute in object.get('attributesToPersist')
          values[attribute] = object.get attribute

        params = {'json': $.toJSON(values)}
        $.post(@persistenceApi.change[objectName], params)

    persistNew: (object, params)->
      objectName = object.constructor.name
      params = {'json': $.toJSON(params)}
      $.post(@persistenceApi.create[objectName], params, (data)->
        object.addChild data.id, data.nodeText
      , 'json')

  module.exports = PersistenceHandler