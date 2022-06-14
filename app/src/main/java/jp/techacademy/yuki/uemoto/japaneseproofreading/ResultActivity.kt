package jp.techacademy.yuki.uemoto.japaneseproofreading

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_result.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException


class ResultActivity : AppCompatActivity() {


    private val handler = Handler(Looper.getMainLooper())
    private var index = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        title = getString(R.string.after_tittle)

        val intent = intent
        val data1 = intent.getSerializableExtra("EXTRA_DATA") as ApiResponse
        var str = data1.checkedSentence
        var str1 = str.replace(" <<", "<font color=\"red\"><a href=\"dialog_page\">")
        str1 = str1.replace(">> ", "</font></a>")
        val uriString = "dialog_page"

        for (data in data1.alerts.indices){
            var uri = Uri.parse(uriString)
                .buildUpon()
                .appendQueryParameter("index", index.toString())
                .build()
            index++
            Log.d("テスト", uri.toString())

        }



        // dialog_page?index=0
        // dialog_page?index=1
        // dialog_page?index=2

        var csHtml = HtmlCompat.fromHtml(str1, FROM_HTML_MODE_COMPACT)

        resulttext.setLinkClickListenable(str1) { url ->

            // このurlがaタグのhrefに指定された文字列
            when (url) {
                "dialog_page"-> {
                    showDialog(); true
                }
                else -> false
            }
        }
        resulttext.text = csHtml //textView
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

                        val csHtml = HtmlCompat.fromHtml(str, FROM_HTML_MODE_COMPACT)

                        resulttext.setLinkClickListenable(str) { url ->
                            // この url が a タグの href に指定された文字列
                            when (url) {
                                "dialog_page" -> {
                                    showDialog(); true
                                }
                                else -> false
                            }
                        }
                        resulttext.text = csHtml
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
        var uri = Uri.parse("dialog_page?index=$index")
        Log.d("テスト2", uri.toString())
        val indexnum = uri.getQueryParameter("index")!!.toInt()
        val data1 = intent.getSerializableExtra("EXTRA_DATA") as ApiResponse
        val alert = data1.alerts[indexnum-1]
        val suggest = alert.suggestion.toString()
        AlertDialog.Builder(this)
            .setTitle("訂正候補")
            .setMessage(suggest)
            .setPositiveButton("OK") { _, _ ->
                // TODO:Yesが押された時の挙動
            }
            .show()

//        val alertsList = data1.alerts
//        val suggest = alertsList[0].suggestion.toString()


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
        } else if (id == R.id.action_share) {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                val string=  resulttext.text.toString()
                putExtra(Intent.EXTRA_TEXT, string)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }

        true
        return super.onOptionsItemSelected(item)
    }


}
