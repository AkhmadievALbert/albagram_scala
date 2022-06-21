package domain.themes

import cats.data.EitherT
import domain.error.ThemeAlreadyExistsError

trait ThemeValidation[F[_]] {
  def doesNotExist(theme: Theme): EitherT[F, ThemeAlreadyExistsError, Unit]
}
