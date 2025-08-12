package com.fluidpotata.placesapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlaceOwnerDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlace(placeOwner: PlaceOwner)

    @Query("SELECT placeId FROM my_places")
    suspend fun getAllOwnedIds(): List<Int>
}
