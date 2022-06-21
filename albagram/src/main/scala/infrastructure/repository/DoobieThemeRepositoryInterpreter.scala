package infrastructure.repository

import cats.data.OptionT
import cats.effect.Bracket
import cats.syntax.all._
import doobie._
import doobie.implicits._
import io.circe.parser.decode
import io.circe.syntax._
import domain.themes.{Theme, ThemeRepository}
import infrastructure.repository.SQLPagination.paginate
import tsec.authentication.IdentityStore

private object ThemeSQL {

  def insert(theme: Theme): Update0 = sql"""
    INSERT INTO THEMES (TITLE)
    VALUES (${theme.title})
  """.update

  def update(theme: Theme, id: Long): Update0 = sql"""
    UPDATE THEMES
    SET TITLE = ${theme.title}
    WHERE ID = $id
  """.update

  def select(title: String): Query0[Theme] = sql"""
    SELECT ID, TITLE
    FROM THEMES
    WHERE TITLE = $title
  """.query

  def select(id: Long): Query0[Theme] = sql"""
    SELECT ID, TITLE
    FROM THEMES
    WHERE ID = $id
  """.query

  def delete(id: Long): Update0 = sql"""
    DELETE FROM THEMES WHERE ID = $id
  """.update

  def deleteByTitle(title: String): Update0 = sql"""
    DELETE FROM THEMES WHERE TITLE = $title
  """.update

  val selectAll: Query0[Theme] = sql"""
    SELECT ID, TITLE
    FROM THEMES
  """.query
}

class DoobieThemeRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
    extends ThemeRepository[F]
    with IdentityStore[F, Long, Theme] { self =>
  import ThemeSQL._


  def create(theme: Theme): F[Theme] = {
    val qq = insert(theme).withUniqueGeneratedKeys[Long]("id").map(id => theme.copy(id = id.some))
    qq.transact(xa)
  }

  def get(themeId: Long): OptionT[F, Theme] = OptionT(select(themeId).option.transact(xa))

  def findByTitle(title: String): OptionT[F, Theme] = {
    OptionT(select(title).option.transact(xa))
  }

  def deleteByTitle(title: String): OptionT[F, Theme] = {
    findByTitle(title).mapFilter(_.id).flatMap(delete)
  }

  override def delete(themeId: Long): OptionT[F, Theme] =
    get(themeId).semiflatMap(theme => ThemeSQL.deleteByTitle(theme.title).run.transact(xa).as(theme))

  def list(pageSize: Int, offset: Int): F[List[Theme]] =
    paginate(pageSize, offset)(selectAll).to[List].transact(xa)
}

object DoobieThemeRepositoryInterpreter {
  def apply[F[_]: Bracket[*[_], Throwable]](xa: Transactor[F]): DoobieThemeRepositoryInterpreter[F] =
    new DoobieThemeRepositoryInterpreter(xa)
}
