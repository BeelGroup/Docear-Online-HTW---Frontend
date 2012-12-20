//TODO this can be done in Java too

import play.api.{Application, Logger, Play, GlobalSettings}
import play.api.mvc.RequestHeader

object Global extends GlobalSettings {
  //-Dbasic.auth.enabled=true
  lazy val basicAuthEnabled: Boolean = {
    val enabled = Play.current.configuration.getBoolean("basic.auth.enabled").getOrElse(false)
    Logger.info("basic auth enabled: " + enabled)
    enabled
  }

  val credentialSource = new CredentialsFromConfCheck

  override def onRouteRequest(request: RequestHeader) = {
    if(Logger.isDebugEnabled && !request.path.startsWith("/assets")) {
      Logger.debug(request.path)
    }

    if (basicAuthEnabled) {
      requireBasicAuthentication(request, credentialSource) {
        super.onRouteRequest(request)
      }
    } else {
      super.onRouteRequest(request)
    }
  }

  override def onStart(app: Application) {
    Logger.info("using configuration " + Play.configuration(app).getString("application.conf.name").get)
    super.onStart(app)
  }
}