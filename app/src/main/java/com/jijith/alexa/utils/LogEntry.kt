package com.jijith.alexa.utils

import org.json.JSONObject

class LogEntry(type: Int, json: JSONObject?) {

    private var mType = type
    private var mJson: JSONObject? = json

    fun getType(): Int {
        return mType
    }

    fun getJSON(): JSONObject? {
        return mJson
    }
}