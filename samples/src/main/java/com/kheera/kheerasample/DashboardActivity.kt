package com.kheera.kheerasample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * A screen that is shown after the user logs in
 */
class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
    }

}
