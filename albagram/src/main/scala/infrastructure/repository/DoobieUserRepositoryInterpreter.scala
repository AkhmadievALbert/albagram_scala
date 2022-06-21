package infrastructure.repository

import cats.data.OptionT
import cats.effect.Bracket
import cats.syntax.all._
import doobie._
import doobie.implicits._
import io.circe.parser.decode
import io.circe.syntax._
import domain.users.{Role, User, UserRepository}
import infrastructure.repository.SQLPagination.paginate
import tsec.authentication.IdentityStore

private object UserSQL {

  implicit val roleMeta: Meta[Role] =
    Meta[String].imap(decode[Role](_).leftMap(throw _).merge)(_.asJson.toString)

  def insert(user: User): Update0 = sql"""
    INSERT INTO USERS (USERNAME, HASH_PASSWORD, ROLE, STATE)
    VALUES (${user.userName}, ${user.hashPassword}, ${user.role}, ${user.state})
  """.update

  def update(user: User, id: Long): Update0 = sql"""
    UPDATE USERS
    SET USERNAME = ${user.userName},
        HASH_PASSWORD = ${user.hashPassword},
        ROLE = ${user.role},
        STATE = ${user.state}
    WHERE ID = $id
  """.update

  def select(userId: Long): Query0[User] = sql"""
    SELECT ID, USERNAME, HASH_PASSWORD, ROLE, STATE
    FROM USERS
    WHERE ID = $userId
  """.query

  def byUserName(userName: String): Query0[User] = sql"""
    SELECT ID, USERNAME, HASH_PASSWORD, ROLE, STATE
    FROM USERS
    WHERE USERNAME = $userName
  """.query[User]

  def delete(userId: Long): Update0 = sql"""
    DELETE FROM USERS WHERE ID = $userId
  """.update

  val selectAll: Query0[User] = sql"""
    SELECT ID, USERNAME, HASH_PASSWORD, ROLE, STATE
    FROM USERS
  """.query
}

class DoobieUserRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
    extends UserRepository[F]
    with IdentityStore[F, Long, User] { self =>
  import UserSQL._

  def create(user: User): F[User] = {
    val qq = insert(user).withUniqueGeneratedKeys[Long]("id").map(id => user.copy(id = id.some))
    qq.transact(xa)
  }

  def update(user: User): OptionT[F, User] =
    OptionT.fromOption[F](user.id).semiflatMap { id =>
      UserSQL.update(user, id).run.transact(xa).as(user)
    }

  def get(userId: Long): OptionT[F, User] = OptionT(select(userId).option.transact(xa))

  def findByUserName(userName: String): OptionT[F, User] =
    OptionT(byUserName(userName).option.transact(xa))

  def delete(userId: Long): OptionT[F, User] =
    get(userId).semiflatMap(user => UserSQL.delete(userId).run.transact(xa).as(user))

  def deleteByUserName(userName: String): OptionT[F, User] =
    findByUserName(userName).mapFilter(_.id).flatMap(delete)

  def list(pageSize: Int, offset: Int): F[List[User]] =
    paginate(pageSize, offset)(selectAll).to[List].transact(xa)
}

object DoobieUserRepositoryInterpreter {
  def apply[F[_]: Bracket[*[_], Throwable]](xa: Transactor[F]): DoobieUserRepositoryInterpreter[F] =
    new DoobieUserRepositoryInterpreter(xa)
}
