package domain.authentication

import domain.users.{Role, User, State}
import tsec.passwordhashers.PasswordHash

final case class LoginRequest(
                               username: String,
                               password: String,
                             )

final case class SignupRequest(
                                username: String,
                                password: String
                              ) {
  def asUser[A](hashedPassword: PasswordHash[A]): User = User(
    userName = username,
    hashPassword = hashedPassword,
    role = Role.User,
    state = State.Active
  )
}
