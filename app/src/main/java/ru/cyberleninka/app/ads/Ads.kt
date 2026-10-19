package ru.cyberleninka.app.ads

import android.app.Activity
import android.content.Context
import com.appodeal.ads.Appodeal
import ru.cyberleninka.app.BuildConfig
import ru.cyberleninka.app.R

/**
 * Тонкая обёртка над Appodeal. Если ключ не задан — реклама полностью отключена,
 * приложение работает как обычно (сборка и запуск без ключей).
 */
object Ads {

    private const val PREFS = "ads_prefs"
    private const val KEY_OPENS = "article_opens"
    private const val INTERSTITIAL_EVERY = 4

    private val enabled: Boolean get() = BuildConfig.APPODEAL_KEY.isNotBlank()

    fun init(activity: Activity) {
        if (!enabled) return
        if (Appodeal.isInitialized(Appodeal.BANNER) || Appodeal.isInitialized(Appodeal.INTERSTITIAL)) return
        Appodeal.setBannerViewId(R.id.adBanner)
        Appodeal.initialize(activity, BuildConfig.APPODEAL_KEY, Appodeal.BANNER or Appodeal.INTERSTITIAL)
    }

    fun showBanner(activity: Activity) {
        if (!enabled) return
        if (Appodeal.isInitialized(Appodeal.BANNER)) Appodeal.show(activity, Appodeal.BANNER_VIEW)
    }

    fun hideBanner(activity: Activity) {
        if (!enabled) return
        Appodeal.hide(activity, Appodeal.BANNER_VIEW)
    }

    /** Вызывать при открытии статьи: показывает межстраничный не чаще, чем раз в N открытий. */
    fun onArticleOpened(activity: Activity) {
        if (!enabled) return
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val opens = prefs.getInt(KEY_OPENS, 0) + 1
        prefs.edit().putInt(KEY_OPENS, opens).apply()
        if (opens % INTERSTITIAL_EVERY == 0 && Appodeal.isLoaded(Appodeal.INTERSTITIAL)) {
            Appodeal.show(activity, Appodeal.INTERSTITIAL)
        }
    }
}
