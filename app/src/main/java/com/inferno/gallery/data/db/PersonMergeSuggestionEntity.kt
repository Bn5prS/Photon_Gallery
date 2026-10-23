package com.inferno.gallery.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Relation

/**
 * Represents a proactively detected merge suggestion between two person clusters
 * whose centroid similarity falls in the ambiguous band (e.g. 0.65 to 0.77).
 *
 * Canonical invariant: personIdA < personIdB.
 */
@Entity(
    tableName = "person_merge_suggestions",
    primaryKeys = ["personIdA", "personIdB"],
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personIdA"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personIdB"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("personIdA"),
        Index("personIdB"),
        Index("status")
    ]
)
data class PersonMergeSuggestionEntity(
    val personIdA: Long,
    val personIdB: Long,
    val similarity: Float,
    val status: String = "PENDING", // PENDING, DISMISSED, ACCEPTED
    val createdAt: Long = System.currentTimeMillis()
)

data class MergeSuggestionWithPersons(
    @Embedded val suggestion: PersonMergeSuggestionEntity,
    @Relation(
        parentColumn = "personIdA",
        entityColumn = "id"
    )
    val personA: PersonEntity,
    @Relation(
        parentColumn = "personIdB",
        entityColumn = "id"
    )
    val personB: PersonEntity
)
