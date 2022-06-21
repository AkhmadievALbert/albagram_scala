package domain.error

import domain.posts.Post
import domain.themes.Theme
import domain.users.User

sealed trait ValidationError extends Product with Serializable

case object UserNotFoundError extends ValidationError
case class UserAlreadyExistsError(user: User) extends ValidationError
case class UserAuthenticationFailedError(userName: String) extends ValidationError

case class ThemeAlreadyExistsError(theme: Theme) extends ValidationError

case class PostAlreadyExistsError(post: Post) extends ValidationError
case object PostNotFoundError extends  ValidationError

