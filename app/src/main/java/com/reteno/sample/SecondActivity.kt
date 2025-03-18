package com.reteno.sample

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.reteno.core.RetenoInternalImpl

class SecondActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
        RetenoInternalImpl.instance.pausePushInAppMessages(true)
        Handler().postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2000L)
    }

    override fun onStop() {
        RetenoInternalImpl.instance.pausePushInAppMessages(false)
        super.onStop()
    }
}