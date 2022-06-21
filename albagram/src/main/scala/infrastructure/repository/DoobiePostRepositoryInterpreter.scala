package infrastructure.repository

import cats.data.OptionT
import cats.effect.Bracket
import cats.implicits.{catsSyntaxOptionId, toFunctorOps}
import domain.posts.{Post, PostRepository}
import doobie.implicits._
import doobie.{Query0, Transactor, Update0}
import infrastructure.repository.SQLPagination.paginate

object PostSQL {
  def insert(post: Post): Update0 = sql"""
    INSERT INTO POSTS (TITLE, DESCRIPTION, THEME_ID, USER_ID)
    VALUES (${post.title}, ${post.description}, ${post.themeId}, ${post.userId})
  """.update

  def select(id: Long): Query0[Post] = sql"""
    SELECT ID, TITLE, DESCRIPTION, THEME_ID, USER_ID
    FROM POSTS
    WHERE ID = $id
  """.query

  def selectByTitle(title: String): Query0[Post] = sql"""
    SELECT ID, TITLE, DESCRIPTION, THEME_ID, USER_ID
    FROM POSTS
    WHERE TITLE = $title
  """.query

  def selectAll(): Query0[Post] = sql"""
    SELECT ID, TITLE, DESCRIPTION, THEME_ID, USER_ID
    FROM POSTS
  """.query

  def delete(id: Long): Update0 =  sql"""
     DELETE FROM POSTS WHERE ID = $id
  """.update

  def deleteByTitle(title: String): Update0 =  sql"""
     DELETE FROM POSTS WHERE TITLE = $title
  """.update

  def selectAllByThemeId(themeId: Long): Query0[Post] = sql"""
     SELECT P.ID, P.TITLE, P.DESCRIPTION, P.THEME_ID, P.USER_ID
     FROM POSTS as P
     INNER JOIN THEMES as T ON P.THEME_ID=T.ID
     WHERE T.ID=$themeId
  """.query

  def selectAllByThemeTitle(title: String): Query0[Post] = sql"""
     SELECT P.ID, P.TITLE, P.DESCRIPTION, P.THEME_ID, P.USER_ID
     FROM POSTS as P
     INNER JOIN THEMES as T ON P.THEME_ID=T.ID
     WHERE T.id=$title
  """.query
}

object DoobiePostRepositoryInterpreter {
  def apply[F[_]: Bracket[*[_], Throwable]](xa: Transactor[F]): PostRepository[F] =
    new DoobiePostRepositoryInterpreter(xa)
}

class DoobiePostRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
  extends PostRepository[F] {
  import PostSQL._

  override def create(post: Post): F[Post] =
    insert(post)
      .withUniqueGeneratedKeys[Long]("id")
      .map(id => post.copy(id = id.some))
      .transact(xa)

  override def get(postId: Long): OptionT[F, Post] =
    OptionT(select(postId).option.transact(xa))

  override def findByTitle(title: String): OptionT[F, Post] =
    OptionT(selectByTitle(title).option.transact(xa))

  override def deleteByTitle(title: String): OptionT[F, Post] =
    findByTitle(title).semiflatMap(post => PostSQL.deleteByTitle(title).run.transact(xa).as(post))

  override def list(pageSize: Int, offset: Int): F[List[Post]] =
    paginate(pageSize, offset)(selectAll()).to[List].transact(xa)

  override def listByThemeId(themeId: Long, pageSize: Int, offset: Int): F[List[Post]] =
    paginate(pageSize, offset)(selectAllByThemeId(themeId)).to[List].transact(xa)

  override def listByThemeTitle(themeTitle: String, pageSize: Int, offset: Int): F[List[Post]] =
    paginate(pageSize, offset)(selectAllByThemeTitle(themeTitle)).to[List].transact(xa)

  override def delete(postId: Long): OptionT[F, Post] = {
    get(postId).semiflatMap(post => PostSQL.delete(postId).run.transact(xa).as(post))
  }
}