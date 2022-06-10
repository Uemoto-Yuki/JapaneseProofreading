package jp.techacademy.yuki.uemoto.japaneseproofreading

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ApiResponse(
    @SerializedName("resultID")
    val resultID: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("inputSentence")
    val inputSentence: String,
    @SerializedName("normalizedSentence")
    val normalizedSentence: String,
    @SerializedName("checkedSentence")
    val checkedSentence: String,
    @SerializedName("alerts")
    val alerts: List<Alerts>
):Serializable

data class Alerts(
    @SerializedName("pos")
    val pos: Int,
    @SerializedName("word")
    val word: String,
    @SerializedName("score")
    val score: Float,
    @SerializedName("suggestions")
    val suggestion: List<String>

):Serializable
