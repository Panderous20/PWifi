package com.example.pwifi.data.repository

import com.example.pwifi.data.model.SpeedTestUpdate
import kotlinx.coroutines.flow.Flow

interface SpeedTestRepository {
    fun runSpeedTestFlow(): Flow<SpeedTestUpdate>
}