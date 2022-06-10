package jp.techacademy.yuki.uemoto.japaneseproofreading

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_result.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException


class ResultActivity : AppCompatActivity() {

    val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        title = getString(R.string.after_tittle)

        val intent = intent
        val data1 = intent.getSerializableExtra("EXTRA_DATA") as ApiResponse
        var str = data1.checkedSentence
        var str1 = str.replace(" <<", "<font color=\"red\"><a href=\"dialog_page\">")
        str1 = str1.replace(">> ", "</font></a>")
        var csHtml = Html.fromHtml(str1)

        resulttext2.setLinkClickListenable(str1) { url ->
            // このurlがaタグのhrefに指定された文字列
            when (url) {
                "dialog_page" -> {
                    showDialog(); true
                }
                else -> false // false を返すとデフォルトの処理(http://example.com の表示)が実行される。(trueを返せば握りつぶせる)
            }
        }
        resulttext2.text = csHtml
        ResultEditText.setText(csHtml)

        button2.setOnClickListener {
            restartRequest()

        }
    }

    fun restartRequest() {

        val url = StringBuilder()
            .append(getString(R.string.base_url))
            .append("?apikey=").append(getString(R.string.api_key)) // Apiを使うためのApiKey
            .append("&sentence=").append(ResultEditText.text.toString())
            .toString()
        Log.e("ここ", url)

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
                    var str = apiResponse.checkedSentence

                    handler.post {
                        var str =
                            str.replace(" <<", "<font color=\"red\"><a href=\"dialog_page\">")
                        str = str.replace(">> ", "</font></a>")
                        val csHtml = Html.fromHtml(str)


                        resulttext2.setLinkClickListenable(str) { url ->
                            // この url が a タグの href に指定された文字列
                            when (url) {
                                "dialog_page" -> {
                                    showDialog(); true
                                }
                                else -> false // false を返すとデフォルトの処理(http://example.com の表示)が実行される。(trueを返せば握りつぶせる)
                            }
                        }
                        resulttext2.text = csHtml
                        ResultEditText.setText(csHtml)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("Error", e.toString())
                // 必要に応じてCallback
            }
            // 必要に応じてCallback
        })


    }


    private fun showDialog() {
        val data1 = intent.getSerializableExtra("EXTRA_DATA") as ApiResponse
        val suggest = data1.alerts
        val sug = suggest[0].suggestion.toString()

        AlertDialog.Builder(this)
            .setTitle("訂正候補")
            .setMessage(sug)
            .setPositiveButton("OK") { _, _ ->
                // TODO:Yesが押された時の挙動
            }
            .show()

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