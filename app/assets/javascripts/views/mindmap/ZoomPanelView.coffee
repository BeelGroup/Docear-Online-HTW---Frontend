define ->
  module = () ->

  class ZoomPanel extends Backbone.View

    tagName: 'div'
    className: 'zoom-panel'
    template: Handlebars.templates['ZoomPanel']

    events:
      "click #zoom-in"     : -> @canvas.zoomIn()
      "click #zoom-out"    : -> @canvas.zoomOut()
      "click #zoom-center" : -> @canvas.zoomCenter()
 
    constructor:(@id, @canvas)->
      super()    

    renderAndAppendTo:($element)->
      $element.append(@render().el)


    render:->
      attributes=
        plusImg: jsRoutes.controllers.Assets.at('images/plus.png').url
        minusImg: jsRoutes.controllers.Assets.at('images/minus.png').url
        centerImg: jsRoutes.controllers.Assets.at('images/centroid.png').url
        simpleTooltip: ($.inArray('SIMPLE_TOOLTIP', document.features) > -1)

      @$el.html @template attributes
      @

  module.exports = ZoomPanel