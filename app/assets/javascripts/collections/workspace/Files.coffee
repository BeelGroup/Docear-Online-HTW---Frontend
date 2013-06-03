define ['logger', 'models/workspace/File'], (logger, File)->
  module = () ->

  class Files extends Backbone.Collection 
    model: File

    
  module.exports = Files