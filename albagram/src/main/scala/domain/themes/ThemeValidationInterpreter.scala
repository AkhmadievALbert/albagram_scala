package domain.themes

import cats.Applicative
import cats.data.EitherT
import domain.error.ThemeAlreadyExistsError

class ThemeValidationInterpreter[F[_]: Applicative](themeRepository: ThemeRepository[F])
  extends ThemeValidation[F] {

  override def doesNotExist(theme: Theme): EitherT[F, ThemeAlreadyExistsError, Unit] =
    themeRepository
      .findByTitle(theme.title)
      .map(ThemeAlreadyExistsError)
      .toLeft()
}

object ThemeValidationInterpreter {
  def apply[F[_]: Applicative](repo: ThemeRepository[F]): ThemeValidation[F] =
    new ThemeValidationInterpreter[F](repo)
}