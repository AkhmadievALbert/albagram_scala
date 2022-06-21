package domain.users

import cats.Applicative
import cats.data.EitherT
import cats.syntax.all._
import domain.error.{UserAlreadyExistsError, UserNotFoundError}
import domain.users.State.Active

class UserValidationInterpreter[F[_]: Applicative](userRepo: UserRepository[F])
  extends UserValidation[F] {
  def doesNotExist(user: User): EitherT[F, UserAlreadyExistsError, Unit] =
    userRepo
      .findByUserName(user.userName)
      .map(UserAlreadyExistsError)
      .toLeft(())

  def exists(userId: Option[Long]): EitherT[F, UserNotFoundError.type, Unit] =
    EitherT {
      userId match {
        case Some(id) =>
          userRepo
            .get(id).value
            .map {
              case Some(value) => value.state match {
                case Active => Right(())
                case _ => Left[UserNotFoundError.type, Unit](UserNotFoundError)
              }
              case None => Left[UserNotFoundError.type, Unit](UserNotFoundError)
            }
        case None =>
          Either.left[UserNotFoundError.type, Unit](UserNotFoundError).pure[F]
      }
    }
}

object UserValidationInterpreter {
  def apply[F[_]: Applicative](repo: UserRepository[F]): UserValidation[F] =
    new UserValidationInterpreter[F](repo)
}
