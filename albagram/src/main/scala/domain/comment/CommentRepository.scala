package domain.comment

trait CommentRepository[F[_]] {
  def create(comment: Comment): F[Comment]
  def list(postId: Long, pageSize: Int, offset: Int): F[List[Comment]]
}
