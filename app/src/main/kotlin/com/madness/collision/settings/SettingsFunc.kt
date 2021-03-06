package com.madness.collision.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.text.HtmlCompat
import com.madness.collision.BuildConfig
import com.madness.collision.R
import com.madness.collision.main.MainActivity
import com.madness.collision.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import java.net.URL
import java.util.*

/**
 * For changing languages
 * And more...
 */

object SettingsFunc {
    const val AUTO = "auto"
    const val requestWriteStorage = 0x11
    const val TAG = "SettingsFun"
    private const val TAG_LANG = "Language"
    private const val urlUpdate = "https://www.coolapk.com/apk/com.madness.collision/"

    fun updateLanguage( context: Context){
        if (!mainApplication.debug) {
            Log.i(TAG_LANG, "not in advanced mode")
            return
        }
        if (getLanguage(context) == AUTO) {
            Log.i(TAG_LANG, AUTO)
            return
        }
        val settingsPreferences = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        val langPref: String = settingsPreferences.getString(P.SETTINGS_LANGUAGE, "") ?: ""
        val langLocale = SystemUtil.getLocaleApp().toString()
        if (langPref != langLocale) {
            switchLanguage(context, langPref)
            Log.i(TAG_LANG, "Current: $langLocale, switching to: " + settingsPreferences.getString(P.SETTINGS_LANGUAGE, AUTO))
        }
    }

    fun settingsPreferencesLanguage(context: Context,  language: String) {
        context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE).edit { putString(P.SETTINGS_LANGUAGE, language) }
    }

    fun getLanguage(context: Context): String {
        return context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE).getString(P.SETTINGS_LANGUAGE, AUTO) ?: AUTO
    }

    fun getLocale(localeString: String): Locale{
        if (localeString == AUTO) return SystemUtil.getLocaleApp()
        val regex = "(.+)_(.+)".toRegex()
        return if (localeString.matches(regex)) {
            val (lang, country) = regex.find(localeString)!!.destructured
            Locale(lang, country)
        } else Locale(localeString)
    }

    fun switchLanguage( context: Context,  languageCode: String) {
        val resources = context.resources
        val configuration = resources.configuration
        configuration.setLocale(getLocale(languageCode))
        // new technique works complicated, so this feature is only used for debug
        resources.updateConfiguration(configuration, resources.displayMetrics)
//        context.createConfigurationContext(configuration)
    }

    fun check4Update(context: Context,  checking: CollisionDialog?,  permissionRequest: PermissionRequestHandler){
        GlobalScope.launch {
            val prefSettings = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
            if (!prefSettings.getBoolean(P.SETTINGS_UPDATE_NOTIFY, true)) return@launch
            val verWeb: String
            val infoWeb: String
            try {
                val url = URL(urlUpdate)
                val document = Jsoup.parse(url, 10000)
                val etsVer = document.getElementsByClass("list_app_info")
                val etsInfo = document.getElementsByClass("apk_left_title_info")
                verWeb = etsVer[0].text()
                if (BuildConfig.VERSION_NAME == verWeb) {
                    updateNone(context, prefSettings, checking)
                }else {
                    infoWeb = etsInfo[0].html()
                    updateInfo(context, prefSettings, checking, verWeb, infoWeb, permissionRequest)
                }
            } catch ( e: Exception) {
                e.printStackTrace()
                updateNone(context, prefSettings, checking)
            }
        }
    }

    private fun updateInfo(context: Context, prefSettings: SharedPreferences, checking: CollisionDialog?,
                           version: String, content: String, permissionRequest: PermissionRequestHandler){
        val updateDetail = HtmlCompat.fromHtml(content, HtmlCompat.FROM_HTML_MODE_COMPACT)
        if (checking == null){
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            putExtra(MainActivity.LAUNCH_ITEM, MainActivity.ITEM_API)
            }
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
            val updateIntent = Intent(context, NotificationActions::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(NotificationActions.ACTION, NotificationActions.ACTION_APP_UPDATE)
            }
            val updatePendingIntent = PendingIntent.getService(context, 0, updateIntent, 0)
            val localeContext = SystemUtil.getLocaleContextSys(context)
            val color = X.getColor(context, if (ThemeUtil.getIsNight(context)) R.color.primaryABlack else R.color.primaryAWhite)
            val builder = NotificationsUtil.Builder(context, "")
                    .setSmallIcon(R.drawable.ic_notify_logo)
                    .setColor(color)
                    .setContentTitle("")
                    .setContentText(updateDetail)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(updateDetail).setBigContentTitle(""))
//                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
//                    .addAction(R.drawable.ic_update_24, localeContext.getString(R.string.SettingsSync_Update_AlertDialog_PositiveButtonText), updatePendingIntent)
            with(NotificationManagerCompat.from(context)){
                notify(0, builder.build())// todo id, update action & update interface
            }
        }else{
            updateLanguage(context)
            GlobalScope.launch(Dispatchers.Main) {
                val dialogUpdate = CollisionDialog(context,
                        R.string.SettingsSync_Update_AlertDialog_NegativeButtonText,
                        R.string.SettingsSync_Update_AlertDialog_PositiveButtonText,
                        R.string.SettingsSync_Update_AlertDialog_NeutralButtonText, true)
                dialogUpdate.makeAgainst(dialogUpdate.buttonIndifferent)
                dialogUpdate.setTitleCollision(R.string.SettingsSync_Update_AlertDialog_Title_UpdateInfo, 0, 0)
                dialogUpdate.setContent(0)
                dialogUpdate.setCustomContent(R.layout.update)

                dialogUpdate.findViewById<TextView>(R.id.updateVer).text = version
                dialogUpdate.findViewById<TextView>(R.id.updateContent).text = updateDetail

                dialogUpdate.setListener( { dialogUpdate.dismiss() }, {
                    dialogUpdate.dismiss()
                    prefSettings.edit { putBoolean(P.SETTINGS_UPDATE_NOTIFY, true) }
                    updateLink(context, permissionRequest)
                }, {
                    dialogUpdate.dismiss()
                    prefSettings.edit { putBoolean(P.SETTINGS_UPDATE_NOTIFY, false) }
                })
                if (!(context as Activity).isFinishing) dialogUpdate.show()
                checking.dismiss()
            }
        }
    }

    private fun updateLink(context: Context, permissionRequest: PermissionRequestHandler){
        var popLoading: CollisionDialog? = null
        GlobalScope.launch(Dispatchers.Main){
            popLoading = CollisionDialog.loading(context, ProgressBar(context))
            popLoading?.show()
        }
        obtainUpdateUrl(context) { url, userAgent ->
            updateLinkPop(context, permissionRequest, url, userAgent)
            popLoading?.dismiss()
        }
    }

    /**
     * For from service
     */
    fun update(context: Context){
        obtainUpdateUrl(context) { url, userAgent ->
            actionUpdateViaDownload(context, null, url, userAgent)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun obtainUpdateUrl(context: Context, callback: (String, String?) -> Unit){
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = true
        webView.settings.loadsImagesAutomatically = false
        val downloadListener = DownloadListener { url, userAgent, _, _, _ ->
            webView.destroy()
            callback.invoke(url, userAgent)
        }
        webView.setDownloadListener(downloadListener)
        webView.loadUrl(urlUpdate)
        webView.webViewClient = object : WebViewClient() {
            var accessed = false
            override fun onPageFinished(view: WebView, url: String) {
                if (accessed) return
                view.evaluateJavascript("onDownloadApk(1);", null)
                accessed = true
            }
        }
    }

    private fun updateLinkPop(context: Context, permissionRequest: PermissionRequestHandler, url: String, userAgent: String?){
        updateLanguage(context)
        GlobalScope.launch(Dispatchers.Main){
            val dialogUpdate = CollisionDialog(context, R.string.text_cancel, R.string.updateDownload, true)
            dialogUpdate.setTitleCollision(R.string.SettingsSync_Update_AlertDialog_PositiveButtonText, 0, 0)
            dialogUpdate.setContent(0)
            dialogUpdate.setCustomContent(R.layout.update_link)

            // make link button
            val colorLink = ThemeUtil.getColor(context, R.attr.colorAPrimary)
            val radius = X.size(context, context.resources.getDimension(R.dimen.radius), X.DP)
            val textViaBrowser: TextView = dialogUpdate.findViewById(R.id.updateLink)
            textViaBrowser.background = RippleUtil.getSelectableDrawablePure(colorLink, radius)
            textViaBrowser.setOnClickListener{
                dialogUpdate.dismiss()
                actionUpdateViaBrowser(context, url)
            }
            textViaBrowser.setOnLongClickListener{
                actionCopyLink(context, url)
                true
            }

            dialogUpdate.setListener( { dialogUpdate.dismiss() }, {
                dialogUpdate.dismiss()
                actionUpdateViaDownload(context, permissionRequest, url, userAgent)
            })
            if (!(context as Activity).isFinishing) dialogUpdate.show()
        }
    }

    private fun actionUpdateViaDownload(context: Context, permissionRequest: PermissionRequestHandler?, url: String, userAgent: String?){
        if (X.belowOff(X.Q)){
            val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (PermissionUtil.check(context, permissions).isNotEmpty()) {
                if (permissionRequest != null && X.aboveOn(X.M)) {
                    permissionRequest.resumeJob = Runnable{ downloadUpdate(context, url, userAgent) }
                    permissionRequest.requestPermission(permissions, requestWriteStorage)
                } else {
                    X.toast(context, R.string.toast_permission_storage_denied, Toast.LENGTH_SHORT)
                }
                return
            }
        }
        downloadUpdate(context, url, userAgent)
    }

    private fun actionUpdateViaBrowser(context: Context, url: String){
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun actionCopyLink(context: Context, url: String){
        //todo unable to do it on Android 9
        X.copyText2Clipboard(context, url, R.string.text_copy_link)
    }

    private fun updateNone(context: Context, prefSettings: SharedPreferences, checking: CollisionDialog?){
        if (!prefSettings.getBoolean(P.SETTINGS_UPDATE_VIA_SETTINGS, false)) return
        updateLanguage(context)
        GlobalScope.launch(Dispatchers.Main) {
            CollisionDialog.alert(context, R.string.SettingsSync_UpdateFalse_AlertDialog_Title).show()
            checking?.dismiss()
        }
        prefSettings.edit { putBoolean(P.SETTINGS_UPDATE_VIA_SETTINGS, false) }
    }

    private fun downloadUpdate(context: Context, url: String,  userAgent: String?){
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager? ?: return
        val request = DownloadManager.Request(Uri.parse(url))

        request.addRequestHeader("cookie", CookieManager.getInstance().getCookie(url))
        request.addRequestHeader("User-Agent", userAgent)

        request.setDescription(context.getString(R.string.settingsfunction_downloaddescription))
        request.setTitle(context.getString(R.string.app_name))
        request.allowScanningByMediaScannerCompat()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Boundo.apk")
        request.setMimeType("application/vnd.android.package-archive")
        downloadManager.enqueue(request)
    }

    @Suppress("DEPRECATION")
    private fun DownloadManager.Request.allowScanningByMediaScannerCompat() {
        if (X.belowOff(X.Q)) allowScanningByMediaScanner()
    }

}
