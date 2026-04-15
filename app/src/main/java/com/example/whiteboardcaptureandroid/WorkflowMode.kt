package com.example.whiteboardcaptureandroid

enum class WorkflowMode(
    val displayName: String,
    val description: String
) {
    CALENDAR_ENTRY("Calendar Entry", "Default workflow for calendar capture and approval"),
    VECTOR_CONVERSION("Vector Conversion", "Local canvas to vector workflow");
}