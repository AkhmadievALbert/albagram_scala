package domain.comment

import cats.Monad
import cats.syntax.all._
import cats.data.EitherT
import domain.error.PostNotFoundError
import domain.posts.PostValidation

class CommentService[F[_]](
                            repository: CommentRepository[F],
                            validation: PostValidation[F]
                          ) {
  def create(comment: Comment)(implicit M: Monad[F]): F[Comment] =
    for {
      saved <- repository.create(comment)
    } yield saved

  def list(postId: Long, pageSize: Int, offset: Int)(implicit M: Monad[F]): EitherT[F, PostNotFoundError.type, List[Comment]] = {
    for {
      _ <- validation.exists(Some(postId))
      result <- EitherT.right(repository.list(postId, pageSize, offset))
    } yield result
  }
}

object CommentService {
  def apply[F[_]](
                   repository: CommentRepository[F],
                   validation: PostValidation[F])
  : CommentService[F] =
    new CommentService(repository, validation)
}
