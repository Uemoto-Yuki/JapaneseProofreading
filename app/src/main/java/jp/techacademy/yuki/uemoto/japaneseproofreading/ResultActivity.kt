package jp.techacademy.yuki.uemoto.japaneseproofreading

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_result.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit


class ResultActivity : AppCompatActivity() {


    private val handler = Handler(Looper.getMainLooper())
    private var choiceItem = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        title = getString(R.string.after_tittle)

        val intent = intent
        val data1 = intent.getSerializableExtra("EXTRA_DATA") as ApiResponse
        var str = data1.checkedSentence
        var index = 0

        Log.d("check1", str.split(' ').toString())

        val str2 = str.split(' ') //文字列をスペースで分けてリスト化する。"AAA <<B>> C" が→ [AAA, <<B>>, C]
            .map { //strがリストになったのでList.map{}でstrの値を以下に変換して返す
                val rawValue =
                    it.replace("<<", "").replace(">>", "") //itは要素の文字列をさす。<<>>を削除した要素を定義
                if (it.indexOf("<<") == 0) { //もし、前から<<を検索して0番目の場合、
                    index++ //indexに1を足して
                    "<font color=\"#e63946\"><a href=\"dialog_page?index=${index - 1}\">$rawValue</a></font>" //strにhtmタグをくっつける
                } else it //それ以外なら元に戻りまたチェック
            }.joinToString(separator = "") //array型の文字列を全てくっつけて返す

        Log.d("check2", str2)

        var csHtml = HtmlCompat.fromHtml(str2, FROM_HTML_MODE_COMPACT)
        resulttext.text = csHtml //textView
        ResultEditText.setText(csHtml)

        resulttext.setLinkClickListenable(str2) { url ->
            Log.d("check3", url)
            showDialog(url)
            true
        }





        ResultEditText.doOnTextChanged { text, start, count, after ->
            if (ResultEditText.text.isNotEmpty()) {
                var validation4 = ResultEditText.text.toString().indexOf("""<""")
                var validation5 = ResultEditText.text.toString().indexOf(""">""")
                var validation6 = ResultEditText.text.toString().indexOf(""" """)

                if (ResultEditText.text.length > 500) {
                    errorText2.text = "文字数が制限を超えています"
                    button2.isClickable = false

                } else if (validation4 > -1 || validation5 > -1 || validation6 > -1) {
                    errorText2.text = "不等号(<,>)や半角スペースは使用できません"
                    button2.isClickable = false

                } else {
                    errorText2.text = ""
                    button2.isClickable = true
                }

            } else if (ResultEditText.text.isNullOrBlank()) {
                errorText2.text = "スペースが入力されている場合や\n" +
                        "文字数が0の時はチェックできません"
                button2.isClickable = false

            } else {
                errorText2.text = ""
                button2.isClickable = true
            }
        }

            button2.setOnClickListener {
                progress2.visibility = ProgressBar.VISIBLE

                if (ResultEditText.text.isEmpty()) {
                    progress2.visibility = ProgressBar.INVISIBLE
                    errorText2.text = "文字が入力されていません"
                } else {
                    progress2.visibility = ProgressBar.VISIBLE
                    restartRequest()
                }
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
                        intent.putExtra("EXTRA_DATA", apiResponse)

                        var str = apiResponse.checkedSentence
                        var index = 0
                        handler.post {
                            val str2 =
                                str.split(' ') //文字列をスペースで分けてリスト化する。"AAA <<B>> C" が→ [AAA, <<B>>, C]
                                    .map { //strがリストになったのでList.map{}でstrの値を以下に変換して返す
                                        val rawValue = it.replace("<<", "")
                                            .replace(">>", "") //itは要素の文字列をさす。<<>>を削除した要素を定義
                                        if (it.indexOf("<<") == 0) { //もし、前から<<を検索して0番目の場合、
                                            index++ //indexに1を足して
                                            "<font color=\"#e63946\"><a href=\"dialog_page?index=${index - 1}\">$rawValue</a></font>" //strにhtmタグをくっつける
                                        } else it //それ以外なら元に戻りまたチェック
                                    }.joinToString(separator = "")
                            Log.d("check6", str2)

                            resulttext.setLinkClickListenable(str2) { url ->
                                // この url が a タグの href に指定された文字列
                                Log.d("check5", url)
                                showDialog(url)
                                true
                            }
                            val csHtml = HtmlCompat.fromHtml(str2, FROM_HTML_MODE_COMPACT)
                            resulttext.text = csHtml
                            ResultEditText.setText(csHtml)


                        }

                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    handler.post {
                        Toast.makeText(this@ResultActivity, "時間をおいてもう一度お試しください", Toast.LENGTH_LONG)
                            .show()
                        // 必要に応じてCallback
                    }
                }
                // 必要に応じてCallback
            })


        }

        private fun showDialog(url: String) {
            var array = url.split("index=")
            val indexnum = array[1].toInt() //index=x を示す
            Log.d("check4", url)
            Log.d("check8", indexnum.toString())

            val data1 = intent.getSerializableExtra("EXTRA_DATA") as ApiResponse
            val alertsList = data1.alerts
            Log.d("checkalert", alertsList.toString())

            val suggest = alertsList[indexnum].suggestion
            val suggestList: Array<CharSequence> = suggest.toTypedArray()
            var index = 0

            var str = data1.checkedSentence
            var str2 =
                str.split(' ') //文字列をスペースで分けてリスト化する。"AAA <<B>> C" が→ [AAA, <<B>>, C]
                    .map { //strがリストになったのでList.map{}でstrの値を以下に変換して返す
                        val rawValue = it.replace("<<", "")
                            .replace(">>", "") //itは要素の文字列をさす。<<>>を削除した要素を定義
                        if (it.indexOf("<<") == 0) { //もし、前から<<を検索して0番目の場合、
                            index++ //indexに1を足して
                            "<font color=\"#e63946\"><a href=\"dialog_page?index=${index - 1}\">$rawValue</a></font>" //strにhtmタグをくっつける
                        } else it //それ以外なら元に戻りまたチェック
                    }.joinToString(separator = "")

            AlertDialog.Builder(this)
                .setTitle("訂正候補を選択")
                .setSingleChoiceItems(suggestList, 0) { dialog, which ->
                    choiceItem = which

                }
                .setPositiveButton("OK") { dialog, which ->

                    when (choiceItem) {
                        0 -> {
                            str2 = str2.replace(
                                "<font color=\"#e63946\"><a href=\"dialog_page?index=${index - 1}\">${alertsList[indexnum].word}</a></font>",
                                "<a href=\"dialog_page?index=${index - 1}\">${suggestList[0]}</a>"
                            )
                            Log.d("test0", str2)
                        }

                        1 -> {
                            str2 = str2.replace(
                                "<font color=\"#e63946\"><a href=\"dialog_page?index=${index - 1}\">${alertsList[indexnum].word}</a></font>",
                                "<a href=\"dialog_page?index=${index - 1}\">${suggestList[1]}</a>"
                            )
                            Log.d("test1", str2)
                        }

                        2 -> {
                            str2 = str2.replace(
                                "<font color=\"#e63946\"><a href=\"dialog_page?index=${index - 1}\">${alertsList[indexnum].word}</a></font>",
                                "<a href=\"dialog_page?index=${index - 1}\">${suggestList[2]}</a>"
                            )
                            Log.d("test2", str2)
                        }
                        else -> {
                        }
                    }
                    Log.d("test", str2)
                    val csHtml = HtmlCompat.fromHtml(str2, FROM_HTML_MODE_COMPACT)
                    ResultEditText.setText(csHtml)

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
            } else if (id == R.id.action_share) {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    val string = resulttext.text.toString()
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
