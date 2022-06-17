package jp.techacademy.yuki.uemoto.japaneseproofreading

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.INVISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_result.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        title = getString(R.string.before_tittle)
        button.setOnClickListener {
            if (editText.text.isEmpty()){
                errorText1.text = "文字が入力されていません"
            }else{ startRequest()}
        }
        editText.doOnTextChanged { text, start, count, after ->
            if(editText.text.length > 500){
                 errorText1.text = "文字数が制限を超えています"
                button.isClickable = false
            }else {
                errorText1.text = ""
                button.isClickable = true
            }
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
            .connectTimeout(10000.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(10000.toLong(), TimeUnit.MILLISECONDS)
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
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.findItem(R.id.action_share).isVisible = false
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

