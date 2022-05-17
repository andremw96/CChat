package com.example.andre.cchat

class TextHelper {
    private var helper: TextHelper? = null

    fun getInstance(): TextHelper? {
        if (helper == null) {
            helper = TextHelper()
        }
        return helper
    }

    fun getText(progress: Int) : String {
        return if (progress in 0..49) {
            "low"
        } else {
            "high"
        }
    }
}
