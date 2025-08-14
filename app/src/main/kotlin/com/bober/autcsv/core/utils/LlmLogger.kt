package com.bober.autcsv.core.utils

import android.util.Log
import com.bober.autcsv.BuildConfig

object LlmLogger {
    private const val TAG_PREFIX = "AutLLM"
    private const val TAG_ANALYSIS = "$TAG_PREFIX.Analysis"
    private const val TAG_API = "$TAG_PREFIX.API"
    private const val TAG_ERROR = "$TAG_PREFIX.Error"
    private const val TAG_UI = "$TAG_PREFIX.UI"
    private const val TAG_FORM = "$TAG_PREFIX.Form"
    private const val TAG_LIST = "$TAG_PREFIX.List"
    private const val TAG_PREVIEW = "$TAG_PREFIX.Preview"
    private const val TAG_NAVIGATION = "$TAG_PREFIX.Navigation"
    private const val TAG_DATABASE = "$TAG_PREFIX.Database"

    // Analysis logging
    fun logAnalysisStart(contentLength: Int) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG_ANALYSIS, "Начинаем анализ резюме, длина контента: $contentLength")
        }
    }

    fun logAnalysisState(state: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG_ANALYSIS, "Состояние анализа изменилось на: $state")
        }
    }

    fun logAnalysisScreenEvent(event: String, resumeId: String? = null) {
        if (BuildConfig.DEBUG) {
            val message = if (resumeId != null) {
                "Экран анализа: $event для резюме $resumeId"
            } else {
                "Экран анализа: $event"
            }
            Log.d(TAG_ANALYSIS, message)
        }
    }

    fun logAnalysisResult(analysisType: String, result: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG_ANALYSIS, "Результат анализа [$analysisType]: $result")
        }
    }

    // API logging
    fun logApiRequest(model: String, promptLength: Int) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG_API, "Отправляем запрос к модели: $model, длина промпта: $promptLength")
        }
    }

    fun logApiResponse(responseLength: Int) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG_API, "Получен ответ, длина: $responseLength")
        }
    }

    // UI logging
    fun logUiEvent(screen: String, event: String, details: String? = null) {
        if (BuildConfig.DEBUG) {
            val message = if (details != null) {
                "UI [$screen]: $event - $details"
            } else {
                "UI [$screen]: $event"
            }
            Log.d(TAG_UI, message)
        }
    }

    fun logFormEvent(event: String, field: String? = null, value: String? = null) {
        if (BuildConfig.DEBUG) {
            val message = if (field != null && value != null) {
                "Form: $event - field: $field, value: $value"
            } else if (field != null) {
                "Form: $event - field: $field"
            } else {
                "Form: $event"
            }
            Log.d(TAG_FORM, message)
        }
    }

    fun logListEvent(event: String, resumeId: String? = null) {
        if (BuildConfig.DEBUG) {
            val message = if (resumeId != null) {
                "List: $event for resume $resumeId"
            } else {
                "List: $event"
            }
            Log.d(TAG_LIST, message)
        }
    }

    fun logPreviewEvent(event: String, resumeId: String? = null) {
        if (BuildConfig.DEBUG) {
            val message = if (resumeId != null) {
                "Preview: $event for resume $resumeId"
            } else {
                "Preview: $event"
            }
            Log.d(TAG_PREVIEW, message)
        }
    }

    fun logNavigationEvent(from: String, to: String, resumeId: String? = null) {
        if (BuildConfig.DEBUG) {
            val message = if (resumeId != null) {
                "Navigation: $from -> $to (resume: $resumeId)"
            } else {
                "Navigation: $from -> $to"
            }
            Log.d(TAG_NAVIGATION, message)
        }
    }

    // Database logging
    fun logDatabaseOperation(operation: String, table: String, id: String? = null) {
        if (BuildConfig.DEBUG) {
            val message = if (id != null) {
                "Database [$table]: $operation - ID: $id"
            } else {
                "Database [$table]: $operation"
            }
            Log.d(TAG_DATABASE, message)
        }
    }

    fun logDatabaseQuery(query: String, params: Map<String, Any>? = null) {
        if (BuildConfig.DEBUG) {
            val message = if (params != null) {
                "Database query: $query with params: $params"
            } else {
                "Database query: $query"
            }
            Log.d(TAG_DATABASE, message)
        }
    }

    // Error logging
    fun logError(message: String, error: Throwable? = null) {
        Log.e(TAG_ERROR, message, error)
    }

    fun logWarning(message: String) {
        Log.w(TAG_ERROR, message)
    }

    fun logDebug(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG_ANALYSIS, message)
        }
    }

    // Performance logging
    fun logPerformance(operation: String, duration: Long) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG_PREFIX, "Performance [$operation]: ${duration}ms")
        }
    }

    // User action logging
    fun logUserAction(action: String, screen: String, details: String? = null) {
        if (BuildConfig.DEBUG) {
            val message = if (details != null) {
                "User action [$screen]: $action - $details"
            } else {
                "User action [$screen]: $action"
            }
            Log.d(TAG_UI, message)
        }
    }
} 