package domain.themes

import cats.data.OptionT

trait ThemeRepository[F[_]] {
  def create(theme: Theme): F[Theme]

  def findByTitle(title: String): OptionT[F, Theme]

  def get(themeId: Long): OptionT[F, Theme]

  def delete(themeId: Long): OptionT[F, Theme]

  def deleteByTitle(title: String): OptionT[F, Theme]

  def list(pageSize: Int, offset: Int): F[List[Theme]]
}