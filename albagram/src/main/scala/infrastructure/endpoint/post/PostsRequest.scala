package infrastructure.endpoint.post

import domain.posts.Post

final case class CreatePostRequest(
                               title: String,
                               description: Option[String] = None,
                               themeId: Long,
                               userId: Long
                             ) {
  def asPost(): Post = Post(
    title = title,
    description = description,
    themeId = themeId,
    userId = userId
  )
}