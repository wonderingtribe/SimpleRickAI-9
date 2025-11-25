package com.simplerick.ai

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*
import androidx.core.view.isVisible

class FloatingAssistantService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams
    
    private lateinit var collapsedView: View
    private lateinit var expandedView: View
    private lateinit var chatContainer: LinearLayout
    private lateinit var inputField: EditText
    private var isExpanded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_overlay, null)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView, params)

        collapsedView = floatingView.findViewById(R.id.collapse_view)
        expandedView = floatingView.findViewById(R.id.expanded_container)
        chatContainer = floatingView.findViewById(R.id.chat_history_container)
        inputField = floatingView.findViewById(R.id.input_query)
        val closeBtn = floatingView.findViewById<ImageView>(R.id.close_btn)
        val sendBtn = floatingView.findViewById<ImageView>(R.id.send_btn)
        val rootContainer = floatingView.findViewById<View>(R.id.root_container)

        rootContainer.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val Xdiff = (event.rawX - initialTouchX).toInt()
                        val Ydiff = (event.rawY - initialTouchY).toInt()
                        if (Xdiff < 10 && Ydiff < 10) {
                            if (!isExpanded) expandView()
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                }
                return false
            }
        })

        closeBtn.setOnClickListener { collapseView() }
        sendBtn.setOnClickListener {
            val query = inputField.text.toString()
            if (query.isNotEmpty()) {
                addMessageToChat("User: $query", true)
                inputField.setText("")
                processAIResponse(query)
            }
        }

        inputField.setOnTouchListener { v, event ->
            v.onTouchEvent(event)
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            windowManager.updateViewLayout(floatingView, params)
            true
        }
    }

    private fun processAIResponse(query: String) {
        val rickLines = listOf(
            "Ugh.. Morty—look at this. The code is barely holding together.",
            "Listen, Morty, that function is like—*urp*—a dimensional mistake waiting to happen.",
            "Okay, alright—here's the deal: your logic is fine but your execution? Ehh, needs a portal gun.",
            "Morty, Morty, Morty... this is what happens when you nest loops like a drunken Gromflomite.",
            "Relax. I’ve seen worse. Once saw a universe crash because someone forgot a semicolon."
        )

        val mortyLines = listOf(
            "A-are you sure this is gonna work, Rick?",
            "Jeez Rick, that code looks kinda… uh… dangerous.",
            "W-wait—shouldn’t we, like, test this or something?",
            "R-rick I don’t know if the compiler’s gonna like that, man.",
            "Aw geez, that’s gonna throw an exception, isn’t it?"
        )

        val response = if (Math.random() < 0.7) {
            "Rick: ${rickLines.random()}"
        } else {
            "Morty: ${mortyLines.random()}"
        }

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            addMessageToChat(response, false)
        }, 600)
    }

    private fun addMessageToChat(text: String, isUser: Boolean) {
        val textView = TextView(this)
        textView.text = text
        textView.textSize = 14f
        textView.setPadding(16, 8, 16, 8)
        textView.setTextColor(if (isUser) 0xFFFFFFFF.toInt() else 0xFF00FF00.toInt())
        textView.typeface = android.graphics.Typeface.MONOSPACE
        chatContainer.addView(textView)
    }

    private fun expandView() {
        collapsedView.visibility = View.GONE
        expandedView.visibility = View.VISIBLE
        isExpanded = true

        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        params.width = (resources.displayMetrics.widthPixels * 0.8).toInt()
        params.height = 800
        windowManager.updateViewLayout(floatingView, params)
    }

    private fun collapseView() {
        collapsedView.visibility = View.VISIBLE
        expandedView.visibility = View.GONE
        isExpanded = false

        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        windowManager.updateViewLayout(floatingView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
    }
}
