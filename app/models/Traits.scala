package models

/**
 * Has Primary ID Trait
 */
trait HasPrimaryId {
  val id: Option[Long]
}

/**
 * Has Name Trait
 */
trait HasName {
  val name: String
}

/**
 * Has Slug Trait
 */
trait HasSlug {
  val slug: String
}

/**
 * Has Description Trait
 */
trait HasDescription {
  val description: String
}

/**
 * Has Terms Trait
 */
trait HasTerms {
  val terms: Set[TermLike]
}

/**
 * Has Created Trait
 */
trait HasCreated {
  val createdAt: DateTime
}

/**
 * Has Creator Trait
 */
trait HasCreator extends HasCreated {
  val createdBy: UserLike
}
