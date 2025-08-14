package com.bober.autcsv.domain.repository

import com.bober.autcsv.domain.model.CvAnalysis

interface LlmRepository {
    suspend fun analyzeCV(cvContent: String): Result<CvAnalysis>
} 