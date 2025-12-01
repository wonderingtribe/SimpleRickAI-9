package com.simplerick.ai

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.*
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class FloatingAssistantService : Service(), View.OnTouchListener {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams
    
    private lateinit var chatContainer: LinearLayout
    private lateinit var chatHistory: TextView
    private lateinit var chatInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var rickHead: ImageView

    private var xInitial: Int = 0
    private var yInitial: Int = 0
    private var xTouch: Float = 0f
    private var yTouch: Float = 0f
    private var isChatExpanded = false
   
    private val httpClient = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    private val API_KEY = "" 
    private val API_URL_BASE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-09-2025:generateContent?key=$API_KEY"
    private val SYSTEM_PROMPT = "You are Simple Rick, the kindest and most simple-minded Rick Sanchez in the multiverse. Respond to all queries with cheerful, simplistic, and overly positive advice. Keep responses brief."


    companion object {
        var isRunning = false
        val conversationHistory = mutableListOf<String>()
        private const val MAX_RETRIES = 3
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null)

        chatContainer = floatingView.findViewById(R.id.chat_container)
        chatHistory = floatingView.findViewById(R.id.chat_history)
        chatInput = floatingView.findViewById(R.id.chat_input)
        sendButton = floatingView.findViewById(R.id.btn_send)
        rickHead = floatingView.findViewById(R.id.img_rick_head)

        chatContainer.visibility = View.GONE

        @Suppress("DEPRECATION")
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        try {
            windowManager.addView(floatingView, params)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to create overlay: ${e.message}", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }

        floatingView.setOnTouchListener(this)
        
        rickHead.setOnClickListener { 
            toggleChatWindow() 
        }

        sendButton.setOnClickListener {
            val message = chatInput.text.toString().trim()
            if (message.isNotEmpty()) {
                sendQuery(message)
                chatInput.setText("")
            }
        }
        
        updateChatHistoryUI()
    }
    
    private fun toggleChatWindow() {
        isChatExpanded = !isChatExpanded

        if (isChatExpanded) {
            chatContainer.visibility = View.VISIBLE
            
            params.width = WindowManager.LayoutParams.MATCH_PARENT 
            params.height = WindowManager.LayoutParams.WRAP_CONTENT 
            
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv() 

            params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            params.y = 0 
        } else {
            chatContainer.visibility = View.GONE
            
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            
            params.gravity = Gravity.TOP or Gravity.START
        }

        windowManager.updateViewLayout(floatingView, params)
    }
    
    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        if (isChatExpanded) {
            return false 
        }
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                xInitial = params.x
                yInitial = params.y
                xTouch = event.rawX
                yTouch = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                params.x = xInitial + (event.rawX - xTouch).toInt()
                params.y = yInitial + (event.rawY - yTouch).toInt()
                windowManager.updateViewLayout(floatingView, params)
                return true
            }
            MotionEvent.ACTION_UP -> {
                val deltaX = event.rawX - xTouch
                val deltaY = event.rawY - yTouch
                val isClick = Math.abs(deltaX) < 10 && Math.abs(deltaY) < 10

                if (isClick) {
                    rickHead.performClick()
                }
                return true
            }
        }
        return false
    }
    
    private fun sendQuery(query: String) = scope.launch(Dispatchers.IO) {
        conversationHistory.add("You: $query")
        withContext(Dispatchers.Main) { 
            updateChatHistoryUI()
            chatInput.hint = "Rick is thinking..." 
            sendButton.isEnabled = false
        }

        try {
            val payloadJson = buildApiPayload(query).toString()
            
            var attempt = 0
            var geminiResponseText: String? = null

            while (attempt < MAX_RETRIES && geminiResponseText == null) {
                try {
                    val requestBody = payloadJson.toRequestBody(JSON)
                    val request = Request.Builder()
                        .url(API_URL_BASE)
                        .post(requestBody)
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw IOException("API call failed with code ${response.code}")

                        val responseJson = JSONObject(response.body?.string() ?: "{}")
                        
                        val text = responseJson.optJSONArray("candidates")
                            ?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.optJSONObject(0)
                            ?.optString("text")

                        if (text != null) {
                            geminiResponseText = text
                        } else {
                            throw IOException("Response was successful but text content was missing.")
                        }
                    }
                } catch (e: Exception) {
                    attempt++
                    if (attempt < MAX_RETRIES) {
                        val delayTime = (1L shl attempt) * 1000L 
                        delay(delayTime)
                    } else {
                        throw e 
                    }
                }
            }

            val finalResponse = geminiResponseText ?: "Uh oh, Morty! The response was empty after all retries."
            conversationHistory.add("Rick: $finalResponse")

        } catch (e: Exception) {
            conversationHistory.add("Rick: Oh gosh, Morty! I ran into a major error: ${e.localizedMessage}")
        } finally {
            withContext(Dispatchers.Main) {
                updateChatHistoryUI()
                chatInput.hint = "Ask Simple Rick anything..."
                sendButton.isEnabled = true
            }
        }
    }
    
    private fun buildApiPayload(query: String): JSONObject {
        val contentsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", query)
                    })
                })
            })
        }

        return JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", SYSTEM_PROMPT)
                    })
                })
            })
        }
    }

    private fun updateChatHistoryUI() {
        chatHistory.text = conversationHistory.joinToString("\n\n")
        (chatHistory.parent as? ScrollView)?.post {
            (chatHistory.parent as? ScrollView)?.fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        scope.cancel() 
        if (::floatingView.isInitialized && floatingView.isAttachedToWindow) {
            windowManager.removeView(floatingView)
        }
        Toast.makeText(this, "Simple Rick AI dismissed.", Toast.LENGTH_SHORT).show()
    }
}
