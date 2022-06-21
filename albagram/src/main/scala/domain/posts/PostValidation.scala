package domain.posts

import cats.data.EitherT
import domain.error.{PostAlreadyExistsError, PostNotFoundError}

trait PostValidation[F[_]] {
  def doesNotExist(post: Post): EitherT[F, PostAlreadyExistsError, Unit]

  def exists(existId: Option[Long]): EitherT[F, PostNotFoundError.type, Unit]
}
