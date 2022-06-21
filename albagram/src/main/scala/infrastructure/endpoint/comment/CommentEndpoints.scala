package infrastructure.endpoint.comment

import cats.effect.Sync
import cats.syntax.all._
import infrastructure.endpoint.{AuthEndpoint, AuthService}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes}
import tsec.authentication._
import tsec.jwt.algorithms.JWTMacAlgo
import org.http4s.circe.jsonOf
import domain.authentification.Authentification
import domain.comment.CommentService
import domain.comment.Comment
import domain.error.PostNotFoundError
import domain.users.User
import infrastructure.endpoint.QueryParam.PostIdMatcher

class CommentEndpoints[F[_] : Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  import infrastructure.endpoint.Pagination._

  implicit val commentDecoder: EntityDecoder[F, Comment] = jsonOf
  implicit val commentModelDecoder: EntityDecoder[F, CommentRequest] = jsonOf

  private def createCommentEndpoint(commentService: CommentService[F]): AuthEndpoint[F, Auth] = {
    case req@POST -> Root asAuthed user =>
      user.id match {
        case Some(id) =>
          for {
            model <- req.request.as[CommentRequest]
            comment <- Comment(model, id).pure[F]
            result <- commentService.create(comment)
            resp <- Ok(result.asJson)
          } yield resp
        case None =>
          NotFound("userId must exist")
      }
  }

  private def listForPostId(commentService: CommentService[F]): AuthEndpoint[F, Auth] = {
    case GET -> Root :? PostIdMatcher(id) :? OptionalPageSizeMatcher(pageSize) :? OptionalOffsetMatcher(offset) asAuthed _ =>
      val action = for {
        result <- commentService.list(id, pageSize.getOrElse(10), offset.getOrElse(0)).value
      } yield result

      action.flatMap {
        case Right(saved) => Ok(saved.asJson)
        case Left(PostNotFoundError) => NotFound("Post with id - \"" + s"$id" + "\" - not found")
      }
  }

  def endpoints(
                 commentService: CommentService[F],
                 auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
               ): HttpRoutes[F] = {
    val authEndpoints: AuthService[F, Auth] = {
      Authentification.allRoles {
        createCommentEndpoint(commentService)
          .orElse(listForPostId(commentService))
      }
    }
    auth.liftService(authEndpoints)
  }
}

object CommentEndpoints {
  def endpoints[F[_] : Sync, Auth: JWTMacAlgo](
                                                commentService: CommentService[F],
                                                auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
                                              ): HttpRoutes[F] =
    new CommentEndpoints[F, Auth].endpoints(commentService, auth)
}