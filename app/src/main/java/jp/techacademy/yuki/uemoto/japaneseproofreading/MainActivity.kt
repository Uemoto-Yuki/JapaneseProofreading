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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.widget.doOnTextChanged
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {


    private val handler = Handler(Looper.getMainLooper())
    private var choiceItem = 0
    private var changeStr = ""
    private var str2 = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = getString(R.string.after_tittle)

        ResultEditText.doOnTextChanged { text, start, count, after ->
            if (ResultEditText.text.isNotEmpty()) {
                var validation1 = ResultEditText.text.toString().indexOf("""<""")
                var validation2 = ResultEditText.text.toString().indexOf(""">""")
                var validation3 = ResultEditText.text.toString().indexOf(""" """)

                if (ResultEditText.text.length > 500) {
                    errorText2.text = "文字数が制限を超えています"
                    button2.isClickable = false

                } else if (validation1 > -1 || validation2 > -1 || validation3 > -1) {
                    errorText2.text = "不等号(<,>)や半角スペースは使用できません"
                    button2.isClickable = false

                } else {
                    errorText2.text = ""
                    button2.isClickable = true
                }

            } else if (ResultEditText.text.isNullOrBlank()) {
                errorText2.text = "スペースのみの入力や文字数0はチェックできません"
                button2.isClickable = false

            } else {
                errorText2.text = ""
                button2.isClickable = true
            }
        }

        button2.setOnClickListener {
            button2.isClickable = false
            progress2.visibility = ProgressBar.VISIBLE
            changeStr = ""

            if (ResultEditText.text.isEmpty()) {
                progress2.visibility = ProgressBar.INVISIBLE
                errorText2.text = "文字が入力されていません"
                button2.isClickable = true
            } else {
                progress2.visibility = ProgressBar.VISIBLE
                startRequest()


            }
        }

        resulttext.setLinkClickListenable(str2) { url -> // この urlはaタグのhrefに指定された文字列
            showDialog(url)
            true
        }

    }


    private fun startRequest() {

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
                        var str2 =
                            str.split(' ') //文字列をスペースで分けてリスト化する。"AAA <<B>> C" が→ [AAA, <<B>>, C]
                                .map { //strがリストになったのでList.map{}でstrの値を以下に変換して返す
                                    val rawValue = it.replace("<<", "")
                                        .replace(">>", "") //itは要素の文字列をさす。<<>>を削除した要素を定義
                                    if (it.indexOf("<<") == 0) { //もし、前から<<を検索して0番目の場合、
                                        index++ //indexに1を足して
                                        "<font color=\"#ff8c00\"><a href=\"dialog_page?index=${index - 1}\">$rawValue</a></font>" //strにhtmタグをくっつける
                                    } else it //それ以外なら元に戻りまたチェック
                                }.joinToString(separator = "")

                        Log.d("check6", str2)

                        val csHtml = HtmlCompat.fromHtml(str2, HtmlCompat.FROM_HTML_MODE_COMPACT)
                        resulttext.text = csHtml
                        ResultEditText.setText(csHtml)
                        progress2.visibility = ProgressBar.INVISIBLE
                        button2.isClickable = true

                    }

                }
            }

            override fun onFailure(call: Call, e: IOException) {
                handler.post {
                    Toast.makeText(this@MainActivity, "時間をおいてもう一度お試しください", Toast.LENGTH_LONG)
                        .show()

                }
                progress2.visibility = ProgressBar.INVISIBLE
                button2.isClickable = true
            }
            // 必要に応じてCallback
        })


    }


    private fun showDialog(url: String) {
        var array = url.split("index=")
        val indexnum = array[1].toInt() //index=x を示す

        val data1 = intent.getSerializableExtra("EXTRA_DATA") as ApiResponse
        val alertsList = data1.alerts

        val suggest = alertsList[indexnum].suggestion
        val suggestList: Array<CharSequence> = suggest.toTypedArray()
        var index = 0

        var str = data1.checkedSentence

        Log.d("検証1", str)

        var str2 =
            if (changeStr.isNullOrEmpty()) {
                str.split(' ') //文字列をスペースで分けてリスト化する。"AAA <<B>> C" が→ [AAA, <<B>>, C]
                    .map { //strがリストになったのでList.map{}でstrの値を以下に変換して返す
                        val rawValue = it.replace("<<", "")
                            .replace(">>", "") //itは要素の文字列をさす。<<>>を削除した要素を定義
                        if (it.indexOf("<<") == 0) { //もし、前から<<を検索して0番目の場合、
                            index++ //indexに1を足して
                            "<font color=\"#ff8c00\"><a href=\"dialog_page?index=${index - 1}\">$rawValue</a></font>" //strにhtmタグをくっつける
                        } else it //それ以外なら元に戻りまたチェック
                    }.joinToString(separator = "")
            } else {
                changeStr
            }

        AlertDialog.Builder(this)
            .setTitle("訂正候補を選択")
            .setSingleChoiceItems(suggestList, 0) { dialog, which ->
                choiceItem = which

            }
            .setPositiveButton("OK") { dialog, which ->

                when (choiceItem) {
                    0 -> {
                        changeStr = str2.replace(
                            "<a href=\"dialog_page?index=${indexnum}\">${alertsList[indexnum].word}</a>",
                            "<a href=\"dialog_page?index=${indexnum}\">${suggest[0]}</a>"
                        )
                        Log.d(
                            "testold",
                            "<a href=\"dialog_page?index=${indexnum}\">${alertsList[indexnum].word}</a>"
                        )
                        Log.d(
                            "testnew",
                            "<a href=\"dialog_page?index=${indexnum}\">${suggest[0]}</a>"
                        )

                    }

                    1 -> {
                        changeStr = str2.replace(
                            "<a href=\"dialog_page?index=${indexnum}\">${alertsList[indexnum].word}</a>",
                            "<a href=\"dialog_page?index=${indexnum}\">${suggest[1]}</a>"

                        )
                        Log.d(
                            "testold",
                            "<a href=\"dialog_page?index=${indexnum}\">${alertsList[indexnum].word}</a>"
                        )
                        Log.d(
                            "testnew",
                            "<a href=\"dialog_page?index=${indexnum}\">${suggest[1]}</a>"
                        )
                    }

                    2 -> {
                        changeStr = str2.replace(
                            "<a href=\"dialog_page?index=${indexnum}\">${alertsList[indexnum].word}</a>",
                            "<a href=\"dialog_page?index=${indexnum}\">${suggest[2]}</a>"
                        )
                        Log.d(
                            "testold",
                            "<a href=\"dialog_page?index=${indexnum}\">${alertsList[indexnum].word}</a>"
                        )
                        Log.d(
                            "testnew",
                            "<a href=\"dialog_page?index=${indexnum}\">${suggest[2]}</a>"
                        )
                    }
                    else -> {
                    }

                }
                Log.d("test", str2)
                Log.d("testchangeStr", changeStr)
                val csHtml = HtmlCompat.fromHtml(changeStr, HtmlCompat.FROM_HTML_MODE_COMPACT)
                ResultEditText.setText(csHtml)
                resulttext.text = csHtml
                Log.d("check12", changeStr)

            }.setNegativeButton("NO") { dialog, which ->
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
