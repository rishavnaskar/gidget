package com.dscvit.gidget.widget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.dscvit.gidget.R
import com.dscvit.gidget.activities.DeleteUserFromGidgetActivity
import com.dscvit.gidget.activities.MainActivity
import com.dscvit.gidget.common.*
import com.dscvit.gidget.services.WidgetRepoRemoteService
import com.dscvit.gidget.models.activity.widget.AddToWidget
import com.dscvit.gidget.models.activity.widget.WidgetRepoModel
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GidgetWidget : AppWidgetProvider() {
    private var utils: Utils? = null
    private var context: Context? = null
    private var security: Security? = null
    private val mService = Common.retroFitService
    private val phoneState = PhoneState()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        initialiseObjects(context)
        for (appWidgetId in appWidgetIds)
            updateAppWidget(context, appWidgetManager, appWidgetId, utils ?: Utils(context))
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null)
            initialiseObjects(context)

        if (intent != null && context != null) {
            if (intent.action == Constants.appWidgetUpdateAction)
                widgetActionUpdate(context, utils ?: Utils(context))
            if (intent.extras != null && intent.action == Constants.onWidgetItemClicked)
                onItemClicked(intent = intent, context = context)
            if (intent.action == Constants.onRefreshButtonClicked || intent.action == Constants.updateWidgetWithDatasource)
                onWidgetRefresh(context, intent)
            if (intent.action == Constants.deleteWidgetWithDatasource)
                deleteWidgetData(context)
            if (intent.action == Constants.clearWidgetItems)
                clearWidgetItems(context, utils ?: Utils(context))
            super.onReceive(context, intent)
        }
    }

    override fun onEnabled(context: Context) {
        initialiseObjects(context)
        if (!utils!!.isEmpty()) {
            onWidgetRefresh(context, Intent(Constants.onRefreshButtonClicked))
        }
    }

    override fun onDisabled(context: Context) {}

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {}

    private fun initialiseObjects(context: Context) {
        if (this.context == null)
            this.context = context
        if (this.utils == null)
            this.utils = Utils(context)
        if (this.security == null)
            this.security = Security(context)
    }

    private fun onWidgetRefresh(context: Context, intent: Intent) {
        initialiseObjects(context)
        val userMap: MutableMap<String, MutableMap<String, String>>? = utils?.getUserDetails()
        if (!userMap.isNullOrEmpty() && phoneState.isPhoneActive(context) && phoneState.isInternetConnected(
                context
            )
        )
            addToWidget(context, userMap)
        else if (userMap.isNullOrEmpty() && intent.action == Constants.onRefreshButtonClicked)
            Toast.makeText(context, "Cannot refresh empty widget", Toast.LENGTH_SHORT).show()
    }

    private fun addToWidget(
        context: Context,
        userMap: MutableMap<String, MutableMap<String, String>>,
        i: Int = 0,
        dataSource: ArrayList<AddToWidget> = arrayListOf()
    ) {
        initialiseObjects(context)
        val views = RemoteViews(context.packageName, R.layout.gidget_widget)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds =
            appWidgetManager.getAppWidgetIds(ComponentName(context, GidgetWidget::class.java))

        views.setViewVisibility(R.id.appwidgetProgressBar, View.VISIBLE)
        appWidgetManager.updateAppWidget(appWidgetIds, views)

        try {
            val key: String = userMap.keys.elementAt(i)
            val map: MutableMap<String, String> = userMap[key]!!
            val name: String = map["name"]!!
            val isUser: Boolean = map["isUser"].toBoolean()

            if (isUser) {
                mService.widgetUserEvents(
                    key.substring(0, key.indexOf(",")),
                    "token ${Security(context).getToken()}"
                )
                    .enqueue(object : Callback<MutableList<WidgetRepoModel>> {
                        override fun onResponse(
                            call: Call<MutableList<WidgetRepoModel>>,
                            response: Response<MutableList<WidgetRepoModel>>
                        ) {
                            if (response.body() != null) {
                                for (res in response.body()!!) {
                                    val addToWidget = AddToWidget()
                                    val eventsList: List<String> =
                                        utils?.getEventMessageAndIcon(res)
                                            ?: Utils(context).getEventMessageAndIcon(res)
                                    addToWidget.username = res.actor.login
                                    addToWidget.name = res.repo.name
                                    addToWidget.avatarUrl = res.actor.avatar_url
                                    addToWidget.icon = eventsList[1].toInt()
                                    addToWidget.message = eventsList[0]
                                    addToWidget.details = utils?.getEventDetails(res)
                                    addToWidget.date = utils?.getDate(res)
                                    addToWidget.dateISO = res.created_at
                                    addToWidget.htmlUrl = utils?.getHtmlUrl(res)

                                    dataSource.add(addToWidget)
                                }

                                if (userMap.keys.elementAtOrNull(i + 1) != null)
                                    addToWidget(context, userMap, i + 1, dataSource)
                                else if (key == userMap.keys.last()) {
                                    refreshWidgetUpdateSharedPref(
                                        context,
                                        dataSource,
                                        appWidgetManager,
                                        appWidgetIds,
                                        views
                                    )
                                }
                            } else throw Exception("Unable to get data")
                        }

                        override fun onFailure(
                            call: Call<MutableList<WidgetRepoModel>>,
                            t: Throwable
                        ) {
                            println("ERROR - ${t.message}")
                            views.setViewVisibility(R.id.appwidgetProgressBar, View.GONE)
                            views.setTextViewText(R.id.appwidgetDate, utils?.getTime())
                            appWidgetManager.updateAppWidget(appWidgetIds, views)
                            println("Failed to fetch data")
                        }
                    })
            } else {
                mService.widgetRepoEvents(
                    key.substring(0, key.indexOf(",")),
                    name,
                    "token ${Security(context).getToken()}"
                )
                    .enqueue(object : Callback<MutableList<WidgetRepoModel>> {
                        override fun onResponse(
                            call: Call<MutableList<WidgetRepoModel>>,
                            response: Response<MutableList<WidgetRepoModel>>
                        ) {
                            if (response.body() != null) {
                                for (res in response.body()!!) {
                                    val addToWidget = AddToWidget()
                                    val eventsList: List<String> =
                                        utils?.getEventMessageAndIcon(res)
                                            ?: Utils(context).getEventMessageAndIcon(res)

                                    addToWidget.username = res.actor.login
                                    addToWidget.name = res.repo.name
                                    addToWidget.avatarUrl = res.actor.avatar_url
                                    addToWidget.icon = eventsList[1].toInt()
                                    addToWidget.message = eventsList[0]
                                    addToWidget.details = utils?.getEventDetails(res)
                                    addToWidget.date = utils?.getDate(res)
                                    addToWidget.dateISO = res.created_at
                                    addToWidget.htmlUrl = utils?.getHtmlUrl(res)

                                    dataSource.add(addToWidget)
                                }

                                if (userMap.keys.elementAtOrNull(i + 1) != null)
                                    addToWidget(context, userMap, i + 1, dataSource)
                                else if (key == userMap.keys.last()) {
                                    refreshWidgetUpdateSharedPref(
                                        context,
                                        dataSource,
                                        appWidgetManager,
                                        appWidgetIds,
                                        views
                                    )
                                }
                            } else throw Exception("Unable to get data")
                        }

                        override fun onFailure(
                            call: Call<MutableList<WidgetRepoModel>>,
                            t: Throwable
                        ) {
                            println("ERROR - ${t.message}")
                            views.setViewVisibility(R.id.appwidgetProgressBar, View.GONE)
                            views.setTextViewText(R.id.appwidgetDate, utils?.getTime())
                            appWidgetManager.updateAppWidget(appWidgetIds, views)
                            println("Failed to fetch data")
                        }
                    })
            }
        } catch (e: Exception) {
            println(e.message)
            Toast.makeText(context, "Error refreshing Gidget", Toast.LENGTH_SHORT).show()
            views.setViewVisibility(R.id.appwidgetProgressBar, View.GONE)
            views.setTextViewText(R.id.appwidgetDate, utils?.getTime())
            appWidgetManager.updateAppWidget(appWidgetIds, views)
        }
    }

    private fun refreshWidgetUpdateSharedPref(
        context: Context,
        dataSource: ArrayList<AddToWidget>,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        views: RemoteViews
    ) {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor: SharedPreferences.Editor = prefs.edit()
        val gson = Gson()
        dataSource.sortWith(SortByDate())
        if (dataSource.size > 50) dataSource.subList(51, dataSource.size).clear()
        editor.putString("dataSource", gson.toJson(dataSource))
        editor.apply()
        views.setViewVisibility(R.id.appwidgetProgressBar, View.GONE)
        views.setTextViewText(R.id.appwidgetDate, utils?.getTime())
        appWidgetManager.updateAppWidget(appWidgetIds, views)
        widgetActionUpdate(context, utils ?: Utils(context))
    }

    private fun deleteWidgetData(context: Context) {
        AppWidgetAlarm.stopGidgetRefresh(context)
        utils?.deleteAllData()
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds =
            appWidgetManager.getAppWidgetIds(ComponentName(context, GidgetWidget::class.java))
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.appwidgetListView)
        widgetActionUpdate(context, utils ?: Utils(context))
    }
}

@SuppressLint("UnspecifiedImmutableFlag")
internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    utils: Utils,
) {
    val views = RemoteViews(context.packageName, R.layout.gidget_widget)

    // Button Intent
    val buttonIntent =
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    val buttonPendingIntent = PendingIntent.getActivity(context, 0, buttonIntent, 0)
    views.setOnClickPendingIntent(R.id.appWidgetLogo, buttonPendingIntent)
    views.setOnClickPendingIntent(R.id.appwidgetTitle, buttonPendingIntent)

    val refreshIntent =
        Intent(context, GidgetWidget::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    refreshIntent.action = Constants.onRefreshButtonClicked
    val refreshPendingIntent =
        PendingIntent.getBroadcast(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    views.setOnClickPendingIntent(R.id.appwidgetRefreshButton, refreshPendingIntent)

    val deleteIntent = Intent(
        context,
        DeleteUserFromGidgetActivity::class.java
    ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    val deletePendingIntent =
        PendingIntent.getActivity(context, 0, deleteIntent, 0)
    views.setOnClickPendingIntent(R.id.appwidgetDeleteButton, deletePendingIntent)

    // Removing ProgressBar
    views.setViewVisibility(R.id.appwidgetProgressBar, View.GONE)

    // Main Widget
    val clickIntent = Intent(context, GidgetWidget::class.java)
    val clickPendingIntent = PendingIntent.getBroadcast(context, 0, clickIntent, 0)
    views.setPendingIntentTemplate(R.id.appwidgetListView, clickPendingIntent)

    if (utils.isEmpty()) {
        views.setEmptyView(R.id.appwidgetListView, R.id.appwidgetEmptyViewText)
        views.setOnClickPendingIntent(R.id.appwidgetEmptyViewText, buttonPendingIntent)
    } else {
        // Date Widget
        views.setTextViewText(R.id.appwidgetDate, utils.getTime())

        // Widget Service Intent
        val serviceIntent = Intent(context, WidgetRepoRemoteService::class.java)
        views.setRemoteAdapter(R.id.appwidgetListView, serviceIntent)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.appwidgetListView)
    }
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

internal fun onItemClicked(intent: Intent, context: Context) {
    if (intent.extras!!.containsKey("dataSource") || intent.hasExtra("dataSource")) {
        val clickedItem: AddToWidget = intent.getParcelableExtra("dataSource")!!
        val uri: Uri = Uri.parse(clickedItem.htmlUrl)
        val clickIntent =
            Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        Toast.makeText(context, clickedItem.name, Toast.LENGTH_SHORT).show()
        context.startActivity(clickIntent)
    }
}

internal fun widgetActionUpdate(context: Context, utils: Utils) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val appWidgetIds =
        appWidgetManager.getAppWidgetIds(ComponentName(context, GidgetWidget::class.java))
    if (appWidgetIds.isNotEmpty()) {
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.appwidgetListView)
        updateAppWidget(context, appWidgetManager, appWidgetIds.first(), utils)
        if (!utils.isEmpty())
            AppWidgetAlarm.startGidgetRefresh(context)
    }
}

internal fun clearWidgetItems(context: Context, utils: Utils) {
    AppWidgetAlarm.stopGidgetRefresh(context)
    widgetActionUpdate(context, utils)
}
