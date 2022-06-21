package domain.posts

import cats.Applicative
import cats.data.EitherT
import cats.implicits.{catsSyntaxApplicativeId, toFunctorOps}
import domain.error.{PostAlreadyExistsError, PostNotFoundError}

class PostValidationInterpreter[F[_] : Applicative](postRepository: PostRepository[F])
  extends PostValidation[F] {

  override def doesNotExist(post: Post): EitherT[F, PostAlreadyExistsError, Unit] =
    postRepository
      .findByTitle(post.title)
      .map(PostAlreadyExistsError)
      .toLeft()

  def exists(existId: Option[Long]): EitherT[F, PostNotFoundError.type, Unit] =
    existId match {
      case Some(id) =>
        postRepository
          .get(id)
          .toRight(PostNotFoundError)
          .void
      case None =>
        EitherT.left[Unit](PostNotFoundError.pure[F])
    }
}

object PostValidationInterpreter {
  def apply[F[_] : Applicative](repo: PostRepository[F]): PostValidation[F] =
    new PostValidationInterpreter[F](repo)
}
