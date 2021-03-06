package com.neung.playkok

import android.R.attr.scheme
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import androidx.annotation.RequiresApi
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.neung.playkok.base.PrefUtils
import com.neung.playkok.base.RootActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : RootActivity() {
    private val url = "http://play-kok.com?token="

/*
internal var pushReceiver: BroadcastReceiver? = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent != null) {
//                    앱이 실행되고 있을 시 broadcast 활성화
//                    url : 해당 페이지로 이동

                var url = intent.getStringExtra("url");
//                webView.loadUrl(url)
                webView.loadUrl(url)
            }
        }
    }
*/

    @SuppressLint("JavascriptInterface")
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        //브로드캐스트 활성화
//        val filter1 = IntentFilter("PUSH");
//        registerReceiver(pushReceiver, filter1);

        val settings = webView.settings


        var intent = getIntent();

        webView.clearCache(true)
        webView.clearHistory()
        if (intent.getStringExtra("url") != null && !intent.getStringExtra("url").equals("")) {
            //푸쉬클릭이벤트
            webView.loadUrl(intent.getStringExtra("url"))
        } else {
            webView.loadUrl(url + getTokenId(this))
        }

        settings.setAllowFileAccess(false)
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

//        webView.addJavascriptInterface(object : Any() {
//            @JavascriptInterface
//            fun performClick(url:String) {
//                var intent = Intent(this@MainActivity, ImageActivity::class.java)
//                intent.putExtra("url", url)
//                this@MainActivity.startActivity(intent)
//            }
//        }, "ok")

        // WebView settings
        webView.fitsSystemWindows = true


        /*
            if SDK version is greater of 19 then activate hardware acceleration
            otherwise activate software acceleration
        */
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)


//        // Set web view client
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (scheme != null && url!!.contains("kakao")) {
                    val INTENT_PROTOCOL_START = "intent:"
                    val INTENT_PROTOCOL_INTENT = "#Intent;"
                    val INTENT_PROTOCOL_END = ";end;"
                    return if (Build.VERSION.SDK_INT >= 19) {
                        if (url!!.startsWith(INTENT_PROTOCOL_START)) {
                            val customUrlStartIndex = INTENT_PROTOCOL_START.length
                            val customUrlEndIndex = url!!.indexOf(INTENT_PROTOCOL_INTENT)
                            if (customUrlEndIndex < 0) {
                                false
                            } else {
                                val customUrl =
                                    url!!.substring(customUrlStartIndex, customUrlEndIndex)
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                try {
                                    intent.data = Uri.parse(customUrl)
                                    baseContext.startActivity(intent)
                                } catch (e: ActivityNotFoundException) {
                                    val packageStartIndex =
                                        customUrlEndIndex + INTENT_PROTOCOL_INTENT.length
                                    val packageEndIndex = url!!.indexOf(INTENT_PROTOCOL_END)
                                    val packageName = url!!.substring(
                                        packageStartIndex,
                                        if (packageEndIndex < 0) url!!.length else packageEndIndex
                                    )
                                    //                                  intent.setData(Uri.parse(GOOGLE_PLAY_STORE_PREFIX   + packageName));
                                    intent.data =
                                        Uri.parse("https://play.google.com/store/apps/details?id=com.kakao.talk&hl=ko")
                                    baseContext.startActivity(intent)
                                }
                                true
                            }
                        } else {
                            false
                        }
                    } else {
                        if (url!!.startsWith("intent:") || url!!.startsWith("kakaolink:") || url!!.startsWith(
                                "market:"
                            )
                        ) {
                            val intent =
                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(intent)
                        } else {
                            view!!.loadUrl(url)
                        }
                        super.shouldOverrideUrlLoading(view, url)
                    }
                }
                return false
            }

        }

        webView.webChromeClient = MyWebChromeClient()


    }


    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private val FILECHOOSER_RESULTCODE = 1

    internal inner class MyWebChromeClient : WebChromeClient() {

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            mFilePathCallback = filePathCallback
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.type = "*/*"
            startActivityForResult(
                Intent.createChooser(intent, "Image Browser"),
                FILECHOOSER_RESULTCODE
            )
            return true
        }


        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
//            Log.d("콘솔", "${consoleMessage.message()}".trimIndent())
            return super.onConsoleMessage(consoleMessage)
        }
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILECHOOSER_RESULTCODE) {
            var params: Array<Uri>? = null
            if (data!!.clipData != null) { // handle multiple-selected files
                val list = mutableListOf<Uri>()
                val numSelectedFiles = data.clipData!!.itemCount
                for (i in 0 until numSelectedFiles) {
                    list.add(data.clipData!!.getItemAt(i).uri)
                }
                params = list.toTypedArray()
            } else if (data!!.data != null) { // handle single-selected file
                params = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            } else {  // 이미지 없음 꺼져
                return
            }

            mFilePathCallback?.onReceiveValue(params)
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





