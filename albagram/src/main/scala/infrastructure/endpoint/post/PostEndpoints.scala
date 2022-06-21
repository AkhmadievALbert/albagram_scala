package infrastructure.endpoint.post

import cats.effect.Sync
import cats.syntax.all._
import domain.authentification.Authentification
import domain.error.PostAlreadyExistsError
import domain.posts.PostService
import domain.users.User
import infrastructure.endpoint.{AuthEndpoint, AuthService, Pagination, QueryParam}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.{EntityDecoder, HttpRoutes}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import tsec.authentication._
import tsec.jwt.algorithms.JWTMacAlgo

class PostEndpoints[F[_] : Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {

  import Pagination._
  import QueryParam._

  implicit val createPostReqDecoder: EntityDecoder[F, CreatePostRequest] = jsonOf

  private def createPostEndpoint(postService: PostService[F]): AuthEndpoint[F, Auth] = {
    case req@POST -> Root asAuthed user =>
      user.id match {
        case Some(id) =>
          val action = for {
            post <- req.request.as[CreatePostRequest].map(_.asPost(id))
            result <- postService.createPost(post).value
          } yield result
          action.flatMap {
            case Right(saved) => Ok(saved.asJson)
            case Left(PostAlreadyExistsError(post)) => NotFound(s"The post with title ${post.title} already exists")
          }
        case None =>
          NotFound("userId must exist")
      }
  }

  private def deleteThemeEndpoint(postService: PostService[F]): AuthEndpoint[F, Auth] = {
    case DELETE -> Root :? PostIdMatcher(id) asAuthed _ =>
      for {
        _ <- postService.deletePost(id)
        resp <- Ok()
      } yield resp
  }

  private def listEndpoint(postService: PostService[F]): AuthEndpoint[F, Auth] = {
    case GET -> Root :? OptionalPageSizeMatcher(pageSize) :? OptionalOffsetMatcher(
    offset,
    ) asAuthed _ =>
      for {
        retrieved <- postService.list(pageSize.getOrElse(10), offset.getOrElse(0))
        resp <- Ok(retrieved.asJson)
      } yield resp
  }

  private def listByThemeIdEndpoint(postService: PostService[F]): AuthEndpoint[F, Auth] = {
    case GET -> Root :? ThemeIdMatcher(themeId) :? OptionalPageSizeMatcher(pageSize) :? OptionalOffsetMatcher(
    offset,
    ) asAuthed _ =>
      for {
        retrieved <- postService.listByThemeId(themeId, pageSize.getOrElse(10), offset.getOrElse(0))
        resp <- Ok(retrieved.asJson)
      } yield resp
  }

  def endpoints(
                 postService: PostService[F],
                 auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]]
               ): HttpRoutes[F] = {
    val authEndpoints: AuthService[F, Auth] = {
      val onlyAdmin = deleteThemeEndpoint(postService)
      val allRoles = createPostEndpoint(postService)
        .orElse(listByThemeIdEndpoint(postService))
        .orElse(listEndpoint(postService))
      Authentification.allRolesHandler(allRoles)(Authentification.adminOnly(onlyAdmin))
    }
    auth.liftService(authEndpoints)
  }
}

object PostEndpoints {
  def endpoints[F[_] : Sync, Auth: JWTMacAlgo](
                                                postService: PostService[F],
                                                auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
                                              ): HttpRoutes[F] =
    new PostEndpoints[F, Auth].endpoints(postService, auth)
}

