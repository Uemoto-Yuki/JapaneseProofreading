package jp.techacademy.yuki.uemoto.japaneseproofreading

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = getString(R.string.before_tittle)
        button.setOnClickListener {
            startRequest()
        }
    }

    // OkHttpClientを作成
    fun startRequest() {

        val url = StringBuilder()
            .append(getString(R.string.base_url))
            .append("?apikey=").append(getString(R.string.api_key)) // Apiを使うためのApiKey
            .append("&sentence=").append(editText.text.toString())
            .toString()

        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        // Requestを作成
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) { // 成功時の処理、これ参考
                 response.body?.string()?.also {
                     val apiResponse = Gson().fromJson(it, ApiResponse::class.java)
                     val intent = Intent(application, ResultActivity::class.java)
                     intent.putExtra("EXTRA_DATA", apiResponse)
                     startActivity(intent)
                 }

            }
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Error", e.toString())
                // 必要に応じてCallback
            }
            // 必要に応じてCallback
        })
        //テスト


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_help) {
            val intent = Intent(applicationContext, HelpActivity::class.java)
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

