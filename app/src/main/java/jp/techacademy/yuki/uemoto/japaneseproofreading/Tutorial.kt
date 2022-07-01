package jp.techacademy.yuki.uemoto.japaneseproofreading

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import kotlinx.android.synthetic.main.activity_tutorial.*
import kotlinx.coroutines.NonCancellable.cancel

class Tutorial : DialogFragment() {

    //カスタムダイアログを生成する
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {

            val builder = AlertDialog.Builder(it)
            // inflaterレイアウトを取得
            val inflater = requireActivity().layoutInflater
            builder.setView(inflater.inflate(R.layout.activity_tutorial, null))

                .setPositiveButton(
                    R.string.tutorial_dialog
                ) { dialog, id ->
                    dialog.cancel()
                }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

