package com.example.whiteboardcaptureandroid

enum class ScanMode(
    val displayName: String,
    val description: String
) {
    ML_KIT("ML Kit Scanner", "Google-hosted document scanner UI"),
    WHITEBOARD_BETA("Whiteboard Beta Scanner", "Experimental in-app whiteboard capture");

    companion object {
        fun fromOrdinal(ordinal: Int): ScanMode {
            return values()[ordinal]
        }
    }
}