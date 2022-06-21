package domain.users

import cats.Applicative
import tsec.authorization.AuthorizationInfo

case class User(
                 id: Option[Long] = None,
                 userName: String,
                 hashPassword: String,
                 role: Role,
                 state: State
               )

object User {
  implicit def authRole[F[_]](implicit F: Applicative[F]): AuthorizationInfo[F, Role, User] =
    (u: User) => F.pure(u.role)
}
