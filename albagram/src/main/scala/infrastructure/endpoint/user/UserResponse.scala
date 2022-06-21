package infrastructure.endpoint.user

import cats.Applicative
import domain.users.{Role, State, User}
import tsec.authorization.AuthorizationInfo


case class UserResponse(
                         id: Option[Long] = None,
                         userName: String,
                         role: Role,
                         state: State
                       )

object UserResponse {
  def apply(user: User): UserResponse = UserResponse(
    user.id,
    user.userName,
    user.role,
    user.state
  )
}