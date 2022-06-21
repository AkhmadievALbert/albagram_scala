package infrastructure.endpoint.theme

import cats.effect.Sync
import cats.syntax.all._
import domain.authentification.Authentification
import domain.error.ThemeAlreadyExistsError
import domain.themes._
import domain.users.User
import infrastructure.endpoint.{AuthEndpoint, AuthService, Pagination, QueryParam}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import tsec.authentication._
import tsec.jwt.algorithms.JWTMacAlgo

class ThemeEndpoints[F[_] : Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {

  import Pagination._
  import QueryParam._

  private def createThemeEndpoint(themeService: ThemeService[F]): AuthEndpoint[F, Auth] = {
    case req@POST -> Root asAuthed _ =>
      val action = for {
        title <- req.request.as[String]
        result <- themeService.createTheme(Theme(title = title)).value
      } yield result
      action.flatMap {
        case Right(saved) => Ok(saved.asJson)
        case Left(ThemeAlreadyExistsError(theme)) => NotFound(s"The theme with title ${theme.title} already exists")
      }
  }

  private def deleteThemeEndpoint(themeService: ThemeService[F]): AuthEndpoint[F, Auth] = {
    case DELETE -> Root :? ThemeTitleMatcher(title) asAuthed _ =>
      for {
        _ <- themeService.deleteByTitle(title)
        resp <- Ok()
      } yield resp
  }

  private def listEndpoint(themeService: ThemeService[F]): AuthEndpoint[F, Auth] = {
    case GET -> Root :? OptionalPageSizeMatcher(pageSize) :? OptionalOffsetMatcher(
    offset,
    ) asAuthed _ =>
      for {
        retrieved <- themeService.list(pageSize.getOrElse(10), offset.getOrElse(0))
        resp <- Ok(retrieved.asJson)
      } yield resp
  }

  def endpoints(
                 themeService: ThemeService[F],
                 auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]]
               ): HttpRoutes[F] = {
    val authEndpoints: AuthService[F, Auth] = {
      val onlyAdmin = createThemeEndpoint(themeService)
        .orElse(deleteThemeEndpoint(themeService))
        .orElse(listEndpoint(themeService))

      Authentification.adminOnly(onlyAdmin)
    }

    auth.liftService(authEndpoints)
  }
}

object ThemeEndpoints {
  def endpoints[F[_]: Sync, Auth: JWTMacAlgo](
                                                  themeService: ThemeService[F],
                                                  auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
                                                ): HttpRoutes[F] =
    new ThemeEndpoints[F, Auth].endpoints(themeService, auth)
}
