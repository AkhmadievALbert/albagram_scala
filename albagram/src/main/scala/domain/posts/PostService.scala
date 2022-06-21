package domain.posts

import cats.{Functor, Monad}
import cats.data.EitherT
import cats.implicits.toFunctorOps
import domain.error.{PostAlreadyExistsError, PostNotFoundError}

class PostService[F[_]](postRepository: PostRepository[F], validator: PostValidation[F]) {
  def createPost(post: Post)(implicit M: Monad[F]): EitherT[F, PostAlreadyExistsError, Post] =
    for {
      _ <- validator.doesNotExist(post)
      saved <- EitherT.liftF(postRepository.create(post))
    } yield saved

  def getTheme(id: Long)(implicit F: Functor[F]): EitherT[F, PostNotFoundError.type, Post] =
    postRepository.get(id).toRight(PostNotFoundError)

  def getPostByTitle(title: String)(implicit F: Functor[F]): EitherT[F, PostNotFoundError.type, Post] =
    postRepository.findByTitle(title).toRight(PostNotFoundError)

  def deletePost(id: Long)(implicit F: Functor[F]): F[Unit] =
    postRepository.delete(id).value.void

  def deleteByTitle(title: String)(implicit F: Functor[F]): F[Unit] =
    postRepository.deleteByTitle(title).value.void

  def list(page: Int, size: Int): F[List[Post]] =
    postRepository.list(page, size)

  def listByThemeId(id: Long, page: Int, size: Int): F[List[Post]] =
    postRepository.listByThemeId(id, page, size)

  def listByTheme(themeTitle: String, page: Int, size: Int): F[List[Post]] =
    postRepository.listByThemeTitle(themeTitle, page, size)
}

object PostService {
  def apply[F[_]](
                   repository: PostRepository[F],
                   validator: PostValidation[F]
                 ): PostService[F] =
    new PostService[F](repository, validator)
}
