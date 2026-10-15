package com.widgets.mysystem

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = "Add the MySystem widget from your home screen."; setPadding(48, 96, 48, 48) })
    }
}
