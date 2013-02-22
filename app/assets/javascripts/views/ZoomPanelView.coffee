define ->
  module = () ->

  class ZoomPanel extends Backbone.View

    constructor:(@canvas)->
      super()

    id: 'zoomPanel'
    tagName: 'div'
    className: 'zoomPanel'
    template: Handlebars.templates['ZoomPanel']

    events:
      "click #zoom-in"     : -> @canvas.zoomIn()
      "click #zoom-out"    : -> @canvas.zoomOut()
      "click #zoom-center" : -> @canvas.zoomCenter()
      

    renderAndAppendTo:($element)->
      $element.append(@render().el)


    render:->
      @$el.html @template {zoomFactor: 0}

      @$el.css 
        'position' : 'absolute'
        'left'     : '1%'
        'top'      : '1%'
      @

  module.exports = ZoomPanel