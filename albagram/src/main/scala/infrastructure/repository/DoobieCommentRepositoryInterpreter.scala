package infrastructure.repository

import cats.effect.Bracket
import cats.implicits.catsSyntaxOptionId
import doobie.implicits.toSqlInterpolator
import domain.comment.{Comment, CommentRepository}
import doobie.{Query0, Transactor, Update0}
import infrastructure.repository.SQLPagination.paginate
import doobie.implicits._

object CommentSQL {
  def insert(comment: Comment): Update0 = sql"""
      INSERT INTO COMMENTS (TITLE, USER_ID, POST_ID)
    VALUES (${comment.title}, ${comment.userId}, ${comment.postId})
  """.update

  def select(id: Long): Query0[Comment] =
    sql"""
    SELECT ID, TITLE, USER_ID, POST_ID
    FROM COMMENTS
    WHERE POST_ID = $id
  """.query
}

class DoobieCommentRepositoryInterpreter[F[_] : Bracket[*[_], Throwable]](val xa: Transactor[F])
  extends CommentRepository[F] {

  import CommentSQL._

  override def create(comment: Comment): F[Comment] =
    insert(comment)
      .withUniqueGeneratedKeys[Long]("id")
      .map(id => comment.copy(id = id.some))
      .transact(xa)

  def list(postId: Long, pageSize: Int, offset: Int): F[List[Comment]] =
    paginate(pageSize, offset)(select(postId))
      .to[List]
      .transact(xa)
}

object DoobieCommentRepositoryInterpreter {
  def apply[F[_] : Bracket[*[_], Throwable]](xa: Transactor[F]): CommentRepository[F] =
    new DoobieCommentRepositoryInterpreter(xa)
}