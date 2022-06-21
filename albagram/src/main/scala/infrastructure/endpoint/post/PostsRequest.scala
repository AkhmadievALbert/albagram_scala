package infrastructure.endpoint.post

import domain.posts.Post

final case class CreatePostRequest(
                               title: String,
                               description: Option[String] = None,
                               themeId: Long
                             ) {
  def asPost(userId: Long): Post = Post(
    title = title,
    description = description,
    themeId = themeId,
    userId = userId
  )
}