package jp.techacademy.yuki.uemoto.japaneseproofreading

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_result.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit


private val handler = Handler(Looper.getMainLooper())


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        title = getString(R.string.before_tittle)
        button.setOnClickListener {
            progress.visibility = ProgressBar.VISIBLE
            if (edit_Text.text.isEmpty()) {
                errorText1.text = "文字が入力されていません"
                progress.visibility = ProgressBar.INVISIBLE
            } else {
                startRequest()
                progress.visibility = ProgressBar.INVISIBLE
            }
        }





        edit_Text.doOnTextChanged { text, start, count, after ->
            if (edit_Text.text.isNotEmpty()) {
                var validation1 = edit_Text.text.toString().indexOf("""<""")
                var validation2 = edit_Text.text.toString().indexOf(""">""")
                var validation3 = edit_Text.text.toString().indexOf(""" """)

                if (edit_Text.text.length > 500) {
                    errorText1.text = "文字数が制限を超えています"
                    button.isClickable = false

                } else if (validation1 > -1 || validation2 > -1 || validation3 > -1) {
                    errorText1.text = "不等号(<,>)や半角スペースは使用できません"
                    button.isClickable = false
                }else {
                    errorText1.text = ""
                    button.isClickable = true
                }

            } else if (edit_Text.text.isNullOrBlank()) {
                errorText1.text = "スペースが入力されている場合や\n" +
                        "文字数が0の時はチェックできません"
                button.isClickable = false
            } else {
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
            .append("&sentence=").append(edit_Text.text.toString())
            .toString()

        val client =
            OkHttpClient.Builder()
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
                handler.post {
                    Toast.makeText(this@MainActivity, "時間をおいてもう一度お試しください", Toast.LENGTH_LONG).show()
                }
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

