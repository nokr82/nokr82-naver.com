package com.neung.playkok

import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import android.webkit.WebView.WebViewTransport
import androidx.annotation.RequiresApi
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.neung.playkok.base.PrefUtils
import com.neung.playkok.base.RootActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : RootActivity() {
    private val url = "http://play-kok.com?token="



    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val settings = webView.settings


        var intent = getIntent();

        webView.clearCache(true)
        webView.clearHistory()
        if (intent.getStringExtra("url") != null && !intent.getStringExtra("url").equals("")) {
            //푸쉬클릭이벤트
            webView.loadUrl(intent.getStringExtra("url"))
        }else {
            webView.loadUrl(url+getTokenId(this))
        }
        // Enable java script in web view
        settings.javaScriptEnabled = true

        // Enable and setup web view cache
        settings.setAppCacheEnabled(true)
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.setAppCachePath(cacheDir.path)


        // Enable zooming in web view
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false


        // Zoom web view text
        settings.textZoom = 100


        // Enable disable images in web view
        settings.blockNetworkImage = false
        // Whether the WebView should load image resources
        settings.loadsImagesAutomatically = true




        // More web view settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.safeBrowsingEnabled = true  // api 26
        }
        //settings.pluginState = WebSettings.PluginState.ON
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.mediaPlaybackRequiresUserGesture = false


        // More optional settings, you can enable it by yourself
        settings.domStorageEnabled = true
        settings.setSupportMultipleWindows(true)
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.loadWithOverviewMode = true
        settings.allowContentAccess = true
        settings.setGeolocationEnabled(true)
        settings.allowUniversalAccessFromFileURLs = true
        settings.allowFileAccess = true
        settings.mediaPlaybackRequiresUserGesture = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // https 이미지.
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        // WebView settings
        webView.fitsSystemWindows = true


        /*
            if SDK version is greater of 19 then activate hardware acceleration
            otherwise activate software acceleration
        */
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)


//        // Set web view client
        webView.webViewClient = object : WebViewClient() {

        }

        webView.webChromeClient = MyWebChromeClient()


    }





    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private val FILECHOOSER_RESULTCODE = 1

    internal inner class MyWebChromeClient : WebChromeClient() {
        override fun getDefaultVideoPoster(): Bitmap? {
            val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            // Use whatever color you want here. You could probably use a transparent color
            canvas.drawARGB(255, 0, 0, 0)
            return bitmap
        }

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            mFilePathCallback = filePathCallback
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(
                Intent.createChooser(intent, "Image Browser"),
                FILECHOOSER_RESULTCODE
            )
            return true
        }

        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            val newWebView = WebView(this@MainActivity)
            newWebView.settings.setJavaScriptEnabled(true);
            newWebView.settings.setSupportZoom(true);
            newWebView.settings.setBuiltInZoomControls(true);
            newWebView.settings.setPluginState(WebSettings.PluginState.ON);
            newWebView.settings.setSupportMultipleWindows(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // https 이미지.
                newWebView.settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
            val dialog = Dialog(this@MainActivity)
            dialog.setContentView(newWebView)

            val params: ViewGroup.LayoutParams = dialog.getWindow()!!.getAttributes()
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.getWindow()!!.setAttributes(params as WindowManager.LayoutParams)
            dialog.show()
            newWebView.setWebChromeClient(object : WebChromeClient() {
                override fun onCloseWindow(window: WebView) {
                    dialog.dismiss()
                }

                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    Log.d("콘솔","${consoleMessage.message()}".trimIndent())
                    return super.onConsoleMessage(consoleMessage)
                }

            })

            (resultMsg!!.obj as WebViewTransport).webView = newWebView
            resultMsg!!.sendToTarget()
            return true
        }

        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            Log.d("콘솔","${consoleMessage.message()}".trimIndent())
            return super.onConsoleMessage(consoleMessage)
        }
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILECHOOSER_RESULTCODE) {
            mFilePathCallback?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(
                    resultCode,
                    data
                )
            )
            mFilePathCallback = null
        }
    }


    // Method to show app exit dialog
    private fun showAppExitDialog() {
        val builder = AlertDialog.Builder(this)

        builder.setTitle("확인")
        builder.setMessage("PlayKok를 정말 종료하시겠습니까?")
        builder.setCancelable(true)

        builder.setPositiveButton("네", { _, _ ->
            super@MainActivity.onBackPressed()
        })

        builder.setNegativeButton("아니요", { _, _ ->
        })

        val dialog = builder.create()

        dialog.show()
    }


    // Handle back button press in web view
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            // If web view have back history, then go to the web view back history
            webView.goBack()
        } else {
            // Ask the user to exit the app or stay in here
            showAppExitDialog()
        }
    }

    //토큰가져오기
    private fun getTokenId(context: Context): String? {
        val addOnCompleteListener = FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(
                        ContentValues.TAG,
                        "getInstanceId failed",
                        task.exception
                    )

                    return@OnCompleteListener
                }
                // Get new Instance ID token
                PrefUtils.setPreference(context, "token", task.result!!.token)
//                System.out.println("놉444444 " + task.result!!.token);
            })
        var token = PrefUtils.getStringPreference(context, "token")
        return token
    }

    override fun onDestroy() {
        super.onDestroy()

        //브로드캐스트 비활성화
//        if (pushReceiver != null) {
//            this.unregisterReceiver(pushReceiver)
//        }

//        progressDialog!!.dismiss()
    }

}





