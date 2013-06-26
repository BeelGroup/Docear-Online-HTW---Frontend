package views

import play.api.i18n.{Lang, Messages}
import controllers.secured.SecuredBase
import SecuredBase.SESSION_KEY_USERNAME

object TemplateUtil {
  def i(key: String, args:String*)(implicit lang: Lang): String = Messages(key, args.toArray:_*)

  def isAuthenticated(implicit session: play.mvc.Http.Session): Boolean = session.get(SESSION_KEY_USERNAME) != null

  def usernameOption(implicit session: play.mvc.Http.Session): Option[String] = Option(session.get(SESSION_KEY_USERNAME))

  def username(implicit session: play.mvc.Http.Session): String = session.get(SESSION_KEY_USERNAME)
}