package domain.comment

import cats.implicits.none
import infrastructure.endpoint.comment.CommentRequest

final case class Comment(
                          id: Option[Long] = none,
                          title: String,
                          postId: Long,
                          userId: Long
                        )

object Comment {
  def apply(comment: CommentRequest, userId: Long): Comment =
    new Comment(
      title = comment.title,
      postId = comment.postId,
      userId = userId
    )
}
