package domain.themes

import cats.data._
import cats.Functor
import cats.Monad
import cats.syntax.functor._
import domain.error.{ThemeAlreadyExistsError, UserAlreadyExistsError, UserNotFoundError}
import domain.users.{User, UserRepository, UserValidation}

class ThemeService[F[_]](themeRepository: ThemeRepository[F], validator: ThemeValidation[F]) {
  def createTheme(theme: Theme)(implicit M: Monad[F]): EitherT[F, ThemeAlreadyExistsError, Theme] =
    for {
      _ <- validator.doesNotExist(theme)
      saved <- EitherT.liftF(themeRepository.create(theme))
    } yield saved

  def getTheme(id: Long)(implicit F: Functor[F]): EitherT[F, UserNotFoundError.type, Theme] =
    themeRepository.get(id).toRight(UserNotFoundError)

  def getThemeByName(
                     title: String,
                   )(implicit F: Functor[F]): EitherT[F, UserNotFoundError.type, Theme] =
    themeRepository.findByTitle(title).toRight(UserNotFoundError)

  def deleteTheme(id: Long)(implicit F: Functor[F]): F[Unit] =
    themeRepository.delete(id).value.void

  def deleteByTitle(title: String)(implicit F: Functor[F]): F[Unit] =
    themeRepository.deleteByTitle(title).value.void

  def list(page: Int, size: Int): F[List[Theme]] =
    themeRepository.list(page, size)
}

object ThemeService {
  def apply[F[_]](
                   repository: ThemeRepository[F],
                   validator: ThemeValidation[F]
                 ): ThemeService[F] =
    new ThemeService[F](repository, validator)
}

