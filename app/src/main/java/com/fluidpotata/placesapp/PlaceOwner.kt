package com.fluidpotata.placesapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "my_places")
data class PlaceOwner(
    @PrimaryKey val placeId: Int
)