package jp.techacademy.yuki.uemoto.japaneseproofreading

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
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
    private var choiceItem = 0 //radiobuttonの選択したアイテムの値保持用
    private var changeStr = "" //
    private var str2 = "" //


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "Text Checker"

        //メイン画面起動時のアプリ説明用ダイアログ
        val dialogFragment = Tutorial()
        dialogFragment.show(supportFragmentManager, "dialog")

        //テキストウォッチャー。入力制限用。
        ResultEditText.doOnTextChanged { _, _, _, _ ->
            if (ResultEditText.text.isNotEmpty()) {
                var validation1 = ResultEditText.text.toString().indexOf("""<""")
                var validation2 = ResultEditText.text.toString().indexOf(""">""")
                var validation3 = ResultEditText.text.toString().indexOf(""" """)

                if (ResultEditText.text.length > 500) {
                    errorText2.text = "文字数が制限を超えています"
                    button.isClickable = false

                } else if (validation1 > -1 || validation2 > -1 || validation3 > -1) {
                    errorText2.text = "不等号(<,>)や半角スペースは使用できません"
                    button.isClickable = false

                } else {
                    errorText2.text = ""
                    button.isClickable = true
                }

            } else if (ResultEditText.text.isNullOrBlank()) {
                errorText2.text = "スペースのみの入力や文字数0はチェックできません"
                button.isClickable = false

            } else {
                errorText2.text = ""
                button.isClickable = true
            }
        }

        //checkボタンが押された時の挙動。
        button.setOnClickListener {
            button.isClickable = false //連打で同じリクエストを送らないように制御
            progress2.visibility = ProgressBar.VISIBLE //読み込み中の表示
            changeStr = ""

            if (ResultEditText.text.isEmpty()) {
                //何も入力されていない場合は読み込み中を消してエラーメッセージを出す
                progress2.visibility = ProgressBar.INVISIBLE
                errorText2.text = "文字が入力されていません"
                button.isClickable = true
            } else {
                //入力されている場合はリクエストを送信
                startRequest()

            }
        }
        //訂正候補表示するために、リンクを押せるようにする
        resulttext.setLinkClickListenable(str2) { url -> // この urlはaタグのhrefに指定された文字列
            showDialog(url)
            true
        }

    }


    private fun startRequest() {

        //APIに文字列を送るためのurlを作成
        val url = StringBuilder()
            .append(getString(R.string.base_url))
            .append("?apikey=").append(getString(R.string.api_key)) // Apiを使うためのApiKey
            .append("&sentence=").append(ResultEditText.text.toString())
            .toString()

        //クライアント作成
        val client = OkHttpClient.Builder()
            //LogChatに、通信したURLや、GET/POST、ステータスコードや受け取ったレスポンスなどを表示させる
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(10000.toLong(), TimeUnit.MILLISECONDS) //接続までに10秒までまつ
            .readTimeout(10000.toLong(), TimeUnit.MILLISECONDS) //データ取得に10秒までまつ
            .build()

        // Requestを作成
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) { // 成功時の処理
                response.body?.string()?.also {
                    val apiResponse = Gson().fromJson(it, ApiResponse::class.java)
                    intent.putExtra("EXTRA_DATA", apiResponse)

                    var resStatus = apiResponse.status //レスポンスのステータスを取得。int型。
                    var str = apiResponse.checkedSentence //checkedSentenceはチェック後の文。指摘箇所を<<>>で示す。
                    var index = 0

                    //指摘がある時
                    if (resStatus == 1) {
                        handler.post {
                            var str2 =
                                str.split(' ') //文字列をスペースで分けてリスト化する。"AAA <<B>> C" が→ [AAA, <<B>>, C]
                                    .map { //strがリストになったのでList.map{}でstrの値を以下に変換して返す
                                        val rawValue = it.replace("<<", "")
                                            .replace(">>", "") //itは要素の文字列をさす。<<>>を削除した要素を定義
                                        if (it.indexOf("<<") == 0) { //もし、前から<<を検索して0番目の場合、
                                            index++ //indexに1を足して
                                            "<font color=\"#FF4500\"><a href=\"dialog_page?index=${index - 1}\">$rawValue</a></font>" //strにhtmタグをくっつける
                                        } else it //それ以外なら元に戻りまたチェック
                                    }.joinToString(separator = "")

                            val csHtml =
                                HtmlCompat.fromHtml(str2, HtmlCompat.FROM_HTML_MODE_COMPACT) //HtmlタグでTextViewを装飾
                            resulttext.text = csHtml
                            ResultEditText.setText(csHtml)

                        }
                    } else if (resStatus == 0) { //指摘がない時
                        handler.post {
                            Toast.makeText(this@MainActivity, "指摘はありません！", Toast.LENGTH_LONG)
                                .show()

                        }
                    } else {
                        //通信には成功しているが何らかのエラーが生じた場合。ひとまず何が起こっているか知らせる。
                        var resMessgage = apiResponse.message
                        handler.post {
                            Toast.makeText(this@MainActivity, resMessgage, Toast.LENGTH_LONG)
                                .show()

                        }
                    }
                    progress2.visibility = ProgressBar.INVISIBLE
                    button.isClickable = true

                }
            }

            //通信失敗した場合の処理
            override fun onFailure(call: Call, e: IOException) {
                handler.post {
                    Toast.makeText(this@MainActivity, "時間をおいてもう一度お試しください", Toast.LENGTH_LONG)
                        .show()

                }
                progress2.visibility = ProgressBar.INVISIBLE
                button.isClickable = true
            }
            // 必要に応じてCallback
        })


    }

    //訂正候補表示のための処理
    private fun showDialog(url: String) {

        var array = url.split("index=") //渡ってきたurlを"index="で区切ってリストに入れる
        val indexnum = array[1].toInt() //index=x を示す

        val data1 = intent.getSerializableExtra("EXTRA_DATA") as ApiResponse
        val alertsList = data1.alerts //alertsは指定内容を格納した配列
        val suggest = alertsList[indexnum].suggestion //suggestionは指摘箇所を置き換える候補。
        val suggestList: Array<CharSequence> =
            suggest.toTypedArray() //選択肢に表示する際にCharSequenceでないとエラーが表示されるため
        var str = data1.checkedSentence //チェック後の文。指摘箇所を<<>>で示す。

        var index = 0
        var str2 =
            //校正後の文字列では無い場合は、changeStrに何も入っていないのでstr2を使用する
            if (changeStr.isNullOrEmpty()) {
                str.split(' ') //文字列をスペースで分けてリスト化する。"AAA <<B>> C" が→ [AAA, <<B>>, C]
                    .map { //strがリストになったのでList.map{}でstrの値を以下に変換して返す
                        val rawValue = it.replace("<<", "")
                            .replace(">>", "") //itは要素の文字列をさす。<<>>を削除した要素を定義
                        if (it.indexOf("<<") == 0) { //もし、前から<<を検索して0番目の場合、
                            index++ //indexに1を足して
                            "<font color=\"#FF4500\"><a href=\"dialog_page?index=${index - 1}\">$rawValue</a></font>" //strにhtmタグをくっつける
                        } else it //それ以外なら元に戻りまたチェック
                    }.joinToString(separator = "")
            } else {
                //check押下後、1回校正後の文字列の場合は、changeStrを使用する
                changeStr
            }

        AlertDialog.Builder(this) //訂正候補表示のため
            .setTitle("訂正候補を選択")
            .setSingleChoiceItems(suggestList, 0) { dialog, which ->
                choiceItem = which //そのままwhichを使用しても.setPositiveButtonには渡らないため

            }
            .setPositiveButton("OK") { dialog, which ->
                //OKを押した場合、str2において特定の指摘箇所に相当する順番の訂正候補に置き換えてchangeStrに代入する
                when (choiceItem) {
                    0 -> {
                        changeStr = str2.replace(
                            "<font color=\"#FF4500\"><a href=\"dialog_page?index=${indexnum}\">${alertsList[indexnum].word}</a></font>",
                            "${suggest[0]}"
                        )
                    }

                    1 -> {
                        changeStr = str2.replace(
                            "<font color=\"#FF4500\"><a href=\"dialog_page?index=${indexnum}\">${alertsList[indexnum].word}</a></font>",
                            "${suggest[1]}"

                        )
                    }

                    2 -> {
                        changeStr = str2.replace(
                            "<font color=\"#FF4500\"><a href=\"dialog_page?index=${indexnum}\">${alertsList[indexnum].word}</a></font>",
                            "${suggest[2]}"
                        )
                    }
                    else -> {
                    }

                }
                val csHtml = HtmlCompat.fromHtml(changeStr, HtmlCompat.FROM_HTML_MODE_COMPACT)
                ResultEditText.setText(csHtml)
                resulttext.text = csHtml
                choiceItem = 0

            }.setNegativeButton("CANCEL") { dialog, which ->
                //CANCELボタンを押下した場合はダイアログを閉じるだけ(何もしなし)
            }
            .show()
    }


    //メニューを生成
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        //ヘルプマークを押下するとヘルプ画面に飛ぶ
        if (id == R.id.action_help) {
            val intent = Intent(applicationContext, HelpActivity::class.java)
            startActivity(intent)
            return true
        } else if (id == R.id.action_share) {
            //共有マークを押すと共有を呼び出す。
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
