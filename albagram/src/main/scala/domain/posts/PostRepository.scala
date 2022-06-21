package domain.posts

import cats.data.OptionT

trait PostRepository[F[_]] {
  def create(post: Post): F[Post]

  def findByTitle(title: String): OptionT[F, Post]

  def get(postId: Long): OptionT[F, Post]

  def delete(postId: Long): OptionT[F, Post]

  def deleteByTitle(title: String): OptionT[F, Post]

  def list(pageSize: Int, offset: Int): F[List[Post]]

  def listByThemeId(themeId: Long, pageSize: Int, offset: Int): F[List[Post]]

  def listByThemeTitle(themeTitle: String, pageSize: Int, offset: Int): F[List[Post]]
}
