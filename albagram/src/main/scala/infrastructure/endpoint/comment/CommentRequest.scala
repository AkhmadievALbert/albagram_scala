package infrastructure.endpoint.comment

case class CommentRequest(
                           title: String,
                           postId: Long
                         )
