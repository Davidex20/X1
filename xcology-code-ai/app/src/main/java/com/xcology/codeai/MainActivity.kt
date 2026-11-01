package com.xcology.codeai

import android.app.Activity
import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var output: TextView
    private lateinit var prompt: EditText
    private lateinit var send: Button
    private val history = JSONArray()
    private val prefs by lazy { getSharedPreferences("config", MODE_PRIVATE) }
    private var lastAnswer = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 24, 20, 20); setBackgroundColor(0xff101827.toInt()) }
        fun label(s: String, size: Float = 15f) = TextView(this).apply { text=s; textSize=size; setTextColor(0xffe2e8f0.toInt()); setPadding(0,8,0,8) }
        fun field(hintText: String, value: String = "") = EditText(this).apply { hint=hintText; setText(value); setSingleLine(true); setTextColor(-1); setHintTextColor(0xff94a3b8.toInt()) }
        root.addView(label("XCOLOGY  /  CODE AI", 23f))
        root.addView(label("Code generation • debugging • refactoring"))
        val endpoint = field("HTTPS API URL (chat completions)", prefs.getString("endpoint", "https://api.openai.com/v1/chat/completions") ?: "")
        val model = field("Model", prefs.getString("model", "gpt-4.1-mini") ?: "")
        val key = field("API key", prefs.getString("key", "") ?: "").apply { inputType = 129 }
        root.addView(endpoint); root.addView(model); root.addView(key)
        val scroll = ScrollView(this)
        output = label("Describe what you want to build. Your generated code will appear here.", 15f).apply { typeface=Typeface.MONOSPACE; setTextIsSelectable(true) }
        scroll.addView(output)
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))
        prompt = EditText(this).apply { hint="e.g. Build a Kotlin login screen..."; setTextColor(-1); setHintTextColor(0xff94a3b8.toInt()); minLines=2; maxLines=5 }
        root.addView(prompt)
        val actions = LinearLayout(this)
        send = Button(this).apply { text="GENERATE CODE" }
        val copy = Button(this).apply { text="COPY"; setOnClickListener { (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("code",lastAnswer)); Toast.makeText(this@MainActivity,"Copied",Toast.LENGTH_SHORT).show() } }
        val clear = Button(this).apply { text="NEW"; setOnClickListener { while(history.length()>0) history.remove(0); output.text="New coding session"; lastAnswer="" } }
        actions.addView(send,LinearLayout.LayoutParams(0,-2,2f)); actions.addView(copy,LinearLayout.LayoutParams(0,-2,1f)); actions.addView(clear,LinearLayout.LayoutParams(0,-2,1f))
        root.addView(actions)
        setContentView(root)
        send.setOnClickListener {
            val userText=prompt.text.toString().trim()
            val url=endpoint.text.toString().trim()
            val modelName=model.text.toString().trim()
            val token=key.text.toString().trim()
            if(userText.isBlank() || !url.startsWith("https://") || modelName.isBlank()) { Toast.makeText(this,"Enter a prompt, HTTPS endpoint and model",Toast.LENGTH_LONG).show(); return@setOnClickListener }
            prefs.edit().putString("endpoint",url).putString("model",modelName).putString("key",token).apply()
            send.isEnabled=false; output.text="Generating..."
            val messages=JSONArray().put(JSONObject().put("role","system").put("content","You are Xcology Code AI, a specialized coding assistant. Prioritize functional, complete, well-structured code, file paths, setup steps, tests, and concise technical explanations. Ask clarifying questions only when essential. Never claim code was executed or verified unless it was."))
            for(i in 0 until history.length()) messages.put(history.getJSONObject(i))
            messages.put(JSONObject().put("role","user").put("content",userText))
            thread {
                try {
                    val conn=(URL(url).openConnection() as HttpURLConnection).apply { requestMethod="POST"; connectTimeout=20000; readTimeout=120000; doOutput=true; setRequestProperty("Content-Type","application/json"); if(token.isNotEmpty()) setRequestProperty("Authorization","Bearer $token") }
                    val request=JSONObject().put("model",modelName).put("messages",messages).put("stream",false)
                    conn.outputStream.use { it.write(request.toString().toByteArray(Charsets.UTF_8)) }
                    val status=conn.responseCode
                    val body=(if(status in 200..299) conn.inputStream else conn.errorStream).bufferedReader().use { it.readText() }
                    if(status !in 200..299) throw IllegalStateException("HTTP $status: ${body.take(700)}")
                    val result=JSONObject(body).getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content")
                    history.put(JSONObject().put("role","user").put("content",userText))
                    history.put(JSONObject().put("role","assistant").put("content",result))
                    runOnUiThread { lastAnswer=result; output.text=result; prompt.setText(""); send.isEnabled=true }
                    conn.disconnect()
                } catch(e:Exception) { runOnUiThread { output.text="Request failed: ${e.message}"; send.isEnabled=true } }
            }
        }
    }
}
