package com.bober.autcsv.core.pdf

import com.bober.autcsv.domain.model.Resume
import java.io.File

interface PdfTemplate {
    fun generate(resume: Resume, outputFile: File): String
}