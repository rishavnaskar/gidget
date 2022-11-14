package com.dscvit.gidget.common

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.*
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.*
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.dscvit.gidget.R
import com.dscvit.gidget.interfaces.RetroFitService
import com.dscvit.gidget.models.activity.widget.AddToWidget
import com.dscvit.gidget.models.activity.widget.WidgetRepoModel
import com.dscvit.gidget.models.profilePage.ProfilePageModel
import com.dscvit.gidget.widget.GidgetWidget
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.picasso.Transformation
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.reflect.Type
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.min


enum class FeedType {
    Following, Me
}

class Utils(val context: Context) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun addToWidget(
        mService: RetroFitService,
        isUser: Boolean,
        username: String,
        name: String,
        ownerAvatarUrl: String,
    ) {
        val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
        val appwidgetIDs: IntArray = appWidgetManager
            .getAppWidgetIds(ComponentName(context, GidgetWidget::class.java))
        val alertDialog: AlertDialog = addToWidgetAlertDialog()
        if (appwidgetIDs.isNotEmpty()) {
            if (isUser)
                mService.widgetUserEvents(
                    username.substring(0, username.indexOf(",")),
                    "token ${Security(context).getToken()}"
                )
                    .enqueue(object : Callback<MutableList<WidgetRepoModel>> {
                        override fun onResponse(
                            call: Call<MutableList<WidgetRepoModel>>,
                            response: Response<MutableList<WidgetRepoModel>>
                        ) {
                            if (response.body() != null) {
                                val dataSource: ArrayList<AddToWidget> = arrayListOf()
                                for (res in response.body()!!) {
                                    val addToWidget = AddToWidget()
                                    val eventsList: List<String> = getEventMessageAndIcon(res)

                                    addToWidget.username = res.actor.login
                                    addToWidget.name = res.repo.name
                                    addToWidget.avatarUrl = res.actor.avatar_url
                                    addToWidget.icon = eventsList[1].toInt()
                                    addToWidget.message = eventsList[0]
                                    addToWidget.details = getEventDetails(res)
                                    addToWidget.date = getDate(res)
                                    addToWidget.dateISO = res.created_at
                                    addToWidget.htmlUrl = getHtmlUrl(res)

                                    dataSource.add(addToWidget)
                                }
                                if (dataSource.isEmpty())
                                    Toast.makeText(
                                        context,
                                        "No activity found for this user",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                else {
                                    saveArrayList(
                                        dataSource = dataSource,
                                        username = username,
                                        name = name,
                                        photoUrl = ownerAvatarUrl,
                                        isUser = isUser
                                    )
                                    val widgetIntent = Intent(context, GidgetWidget::class.java)
                                    widgetIntent.action = Constants.updateWidgetWithDatasource
                                    context.sendBroadcast(widgetIntent)
                                }

                                if (alertDialog.isShowing)
                                    alertDialog.dismiss()
                                Toast.makeText(context, "Added to Gidget", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }

                        override fun onFailure(
                            call: Call<MutableList<WidgetRepoModel>>,
                            t: Throwable
                        ) {
                            if (alertDialog.isShowing) {
                                Toast.makeText(context, "Could not add Gidget", Toast.LENGTH_LONG)
                                    .show()
                                alertDialog.dismiss()
                            }
                            println("ERROR - ${t.message}")
                        }
                    })
            else
                mService.widgetRepoEvents(
                    username.substring(0, username.indexOf(",")),
                    name,
                    "token ${Security(context).getToken()}"
                )
                    .enqueue(object : Callback<MutableList<WidgetRepoModel>> {
                        override fun onResponse(
                            call: Call<MutableList<WidgetRepoModel>>,
                            response: Response<MutableList<WidgetRepoModel>>
                        ) {
                            if (response.body() != null) {
                                val dataSource: ArrayList<AddToWidget> = arrayListOf()
                                for (res in response.body()!!) {
                                    val addToWidget = AddToWidget()
                                    val eventsList: List<String> = getEventMessageAndIcon(res)

                                    addToWidget.username = res.actor.login
                                    addToWidget.name = res.repo.name
                                    addToWidget.avatarUrl = res.actor.avatar_url
                                    addToWidget.icon = eventsList[1].toInt()
                                    addToWidget.message = eventsList[0]
                                    addToWidget.details = getEventDetails(res)
                                    addToWidget.date = getDate(res)
                                    addToWidget.dateISO = res.created_at
                                    addToWidget.htmlUrl = getHtmlUrl(res)

                                    dataSource.add(addToWidget)
                                }

                                if (dataSource.isEmpty())
                                    Toast.makeText(
                                        context,
                                        "No activity found for this repo",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                else {
                                    saveArrayList(
                                        dataSource = dataSource,
                                        username = username,
                                        name = name,
                                        photoUrl = ownerAvatarUrl,
                                        isUser = isUser
                                    )
                                    val widgetIntent = Intent(context, GidgetWidget::class.java)
                                    widgetIntent.action = Constants.updateWidgetWithDatasource
                                    context.sendBroadcast(widgetIntent)
                                }

                                if (alertDialog.isShowing)
                                    alertDialog.dismiss()
                                Toast.makeText(context, "Added to Gidget", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }

                        override fun onFailure(
                            call: Call<MutableList<WidgetRepoModel>>,
                            t: Throwable
                        ) {
                            if (alertDialog.isShowing) {
                                Toast.makeText(context, "Could not add Gidget", Toast.LENGTH_LONG)
                                    .show()
                                alertDialog.dismiss()
                            }
                            println("ERROR - ${t.message}")
                        }
                    })
        } else {
            if (alertDialog.isShowing)
                alertDialog.dismiss()
            Toast.makeText(context, "Please add Gidget to home screen", Toast.LENGTH_LONG).show()
        }
    }

    fun saveArrayList(
        dataSource: ArrayList<AddToWidget>,
        username: String,
        name: String,
        photoUrl: String,
        isUser: Boolean
    ) {
        try {
            val editor: SharedPreferences.Editor = prefs.edit()
            val gson = Gson()
            if (prefs.contains(Constants.dataSource)) {
                val userDetailsMap: MutableMap<String, MutableMap<String, String>>? =
                    getUserDetails()
                if (!userDetailsMap.isNullOrEmpty()) {
                    var userDataSource: ArrayList<AddToWidget>? = getArrayList()

                    if (!userDataSource.isNullOrEmpty()) {
                        if (userDetailsMap.containsKey(username)) {
                            userDetailsMap.remove(username)
                            userDataSource =
                                userDataSource.filter { it.username!! == username } as ArrayList<AddToWidget>
                        }

                        userDetailsMap[username] = mutableMapOf(
                            Constants.name to name,
                            Constants.photoUrl to photoUrl,
                            Constants.isUser to isUser.toString()
                        )
                        userDataSource.addAll(dataSource)
                        userDataSource.sortWith(SortByDate())
                        if (userDataSource.size > 50) userDataSource.subList(
                            51,
                            userDataSource.size
                        )
                            .clear()
                        editor.putString(Constants.dataSource, gson.toJson(userDataSource))
                        editor.putString(Constants.userDetails, gson.toJson(userDetailsMap))
                        editor.apply()
                    }
                }
            } else {
                val userDetails: MutableMap<String, MutableMap<String, String>> = mutableMapOf(
                    username to mutableMapOf(
                        Constants.name to name,
                        Constants.photoUrl to photoUrl,
                        Constants.isUser to isUser.toString()
                    )
                )
                editor.putString(Constants.dataSource, gson.toJson(dataSource))
                editor.putString(Constants.userDetails, gson.toJson(userDetails))
                editor.apply()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to save details", Toast.LENGTH_LONG).show()
            println(e.message)
        }
    }

    fun getArrayList(): ArrayList<AddToWidget>? {
        return try {
            val gson = Gson()
            val json: String = prefs.getString(Constants.dataSource, null).toString()
            val type: Type = object : TypeToken<ArrayList<AddToWidget?>?>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    fun deleteAllData() = prefs.edit().clear().apply()

    fun deleteArrayList() {
        if (prefs.contains(Constants.dataSource)) {
            val editor: SharedPreferences.Editor = prefs.edit()
            editor.remove(Constants.dataSource)
            editor.remove(Constants.userDetails)
            editor.apply()
        }
    }

    fun getUserDetails(): MutableMap<String, MutableMap<String, String>>? {
        return if (prefs.contains(Constants.userDetails)) {
            val gson = Gson()
            val jsonUserDetails: String = prefs.getString(Constants.userDetails, null).toString()
            val jsonUserDetailsType: Type =
                object : TypeToken<MutableMap<String?, MutableMap<String?, String?>>?>() {}.type
            gson.fromJson(jsonUserDetails, jsonUserDetailsType)
        } else
            null
    }

    fun getEventDetails(currentItem: WidgetRepoModel): String? {
        try {
            return when (currentItem.type) {
                "CommitCommentEvent" -> {
                    if (currentItem.payload?.comment?.body.isNullOrEmpty()) null
                    else "${currentItem.payload?.comment?.body}"
                }
                "CreateEvent" -> null
                "ForkEvent" -> null
                "DeleteEvent" -> null
                "GollumEvent" -> null
                "IssueCommentEvent" -> {
                    if (currentItem.payload?.action.isNullOrEmpty()) null
                    else when (currentItem.payload?.action) {
                        "created" -> "${currentItem.payload.comment?.body}"
                        else -> null
                    }
                }
                "IssuesEvent" -> {
                    if (currentItem.payload?.action.isNullOrEmpty()) null
                    else "${currentItem.payload?.issue?.title}"
                }
                "MemberEvent" -> {
                    if (currentItem.payload?.action.isNullOrEmpty()) null
                    else "${currentItem.payload?.member?.login}"
                }
                "PublicEvent" -> null
                "PullRequestEvent" -> {
                    if (currentItem.payload?.action.isNullOrEmpty() || currentItem.payload?.pull_request?.title.isNullOrEmpty()) null
                    else "${currentItem.payload?.pull_request?.title}"
                }
                "PullRequestReviewEvent" -> {
                    if (currentItem.payload?.pull_request?.title.isNullOrEmpty()) null
                    else "${currentItem.payload?.pull_request?.title}"
                }
                "PullRequestReviewCommentEvent" -> {
                    if (currentItem.payload?.comment?.body.isNullOrEmpty()) null
                    else "${currentItem.payload?.comment?.body}"
                }
                "PushEvent" -> {
                    var message = ""
                    currentItem.payload?.commits?.forEach {
                        if (!it.message.isNullOrEmpty()) {
                            message += if (currentItem.payload.commits.last() != it)
                                "${it.message}, "
                            else it.message
                        }
                    }
                    if (currentItem.payload?.commits.isNullOrEmpty()) null
                    else message
                }
                "ReleaseEvent" -> {
                    if (currentItem.payload?.release?.name.isNullOrEmpty()) null
                    else "Release - ${currentItem.payload?.release?.name}"
                }
                "SponsorshipEvent" -> null
                "WatchEvent" -> null
                else -> null
            }
        } catch (e: Throwable) {
            return null
        }
    }

    fun getEventMessageAndIcon(currentItem: WidgetRepoModel): List<String> {
        try {
            return when (currentItem.type) {
                "CommitCommentEvent" -> listOf(
                    "User commented on a commit",
                    R.drawable.ic_baseline_comment_24.toString()
                )
                "CreateEvent" -> listOf(
                    "User created a branch / tag",
                    R.drawable.ic_git_branch.toString()
                )
                "ForkEvent" -> listOf(
                    "User forked this repository",
                    R.drawable.ic_github_fork.toString()
                )
                "DeleteEvent" -> listOf(
                    "User deleted a branch / tag",
                    R.drawable.ic_baseline_delete_24.toString()
                )
                "GollumEvent" -> listOf(
                    "User created / updated a wiki page",
                    R.drawable.github_gollum.toString()
                )
                "IssueCommentEvent" -> listOf(
                    if (currentItem.payload?.action.isNullOrEmpty()) "User commented on an issue"
                    else when (currentItem.payload?.action) {
                        "edited" -> "User edited a comment"
                        "deleted" -> "User deleted a comment"
                        else -> "User commented on an issue"
                    },
                    R.drawable.ic_baseline_comment_24.toString()
                )
                "IssuesEvent" -> listOf(
                    if (currentItem.payload?.action.isNullOrEmpty()) "Activity related to an issue"
                    else "User ${currentItem.payload?.action} a issue",
                    R.drawable.ic_github_issue.toString()
                )
                "MemberEvent" -> listOf(
                    if (currentItem.payload?.action.isNullOrEmpty()) "A collaborator was added or removed"
                    else "A collaborator was ${currentItem.payload?.action}",
                    R.drawable.ic_baseline_group_24.toString()
                )
                "PublicEvent" -> listOf(
                    "Repository was made public",
                    R.drawable.ic_baseline_public_24.toString()
                )
                "PullRequestEvent" -> listOf(
                    if (currentItem.payload?.action.isNullOrEmpty())
                        "User made a pull request"
                    else "User ${currentItem.payload?.action} a pull request",
                    R.drawable.ic_github_pull_request.toString()
                )
                "PullRequestReviewEvent" -> listOf(
                    "User reviewed a pull request",
                    R.drawable.pull_request_review_event.toString()
                )
                "PullRequestReviewCommentEvent" -> listOf(
                    "User commented on a pull request review",
                    R.drawable.ic_baseline_comment_24.toString()
                )
                "PushEvent" -> listOf(
                    "User made a push request",
                    R.drawable.ic_baseline_cloud_upload_24.toString()
                )
                "ReleaseEvent" -> listOf(
                    "User made a new release",
                    R.drawable.ic_baseline_new_releases_24.toString()
                )
                "SponsorshipEvent" -> listOf(
                    "User started sponsoring",
                    R.drawable.ic_baseline_monetization_on_24.toString()
                )
                "WatchEvent" -> listOf(
                    "User starred this repo",
                    R.drawable.github_star.toString()
                )
                else -> listOf("Unidentified event", R.drawable.github_logo.toString())
            }
        } catch (e: Throwable) {
            return listOf("Unidentified event", R.drawable.github_logo.toString())
        }
    }

    fun getDate(currentItem: WidgetRepoModel): String {
        val dateTimePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val createDate = LocalDateTime.parse(currentItem.created_at, dateTimePattern)
        val currentDate = LocalDateTime.now(ZoneId.of("Etc/UTC"))
        val differenceTime = Duration.between(currentDate, createDate).abs()
        return when {
            differenceTime.seconds < 60 -> "${differenceTime.seconds} secs ago"
            differenceTime.toMinutes().toInt() == 1 -> "${differenceTime.toMinutes()} min ago"
            differenceTime.toMinutes() < 60 -> "${differenceTime.toMinutes()} mins ago"
            differenceTime.toHours().toInt() == 1 -> "${differenceTime.toHours()} hr ago"
            differenceTime.toHours() < 24 -> "${differenceTime.toHours()} hrs ago"
            differenceTime.toDays().toInt() == 1 -> "${differenceTime.toDays()} day ago"
            else -> "${differenceTime.toDays()} days ago"
        }
    }

    fun getTime(): String {
        val localTime: LocalTime = LocalTime.now()
        val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
        return localTime.format(dateTimeFormatter)
    }

    fun getHtmlUrl(currentItem: WidgetRepoModel): String {
        return try {
            when (currentItem.type) {
                "CommitCommentEvent" -> "https://github.com/${currentItem.repo.name}"
                "CreateEvent" -> "https://github.com/${currentItem.repo.name}"
                "ForkEvent" -> currentItem.payload!!.forkee!!.html_url!!
                "DeleteEvent" -> "https://github.com/${currentItem.repo.name}"
                "GollumEvent" -> "https://github.com/${currentItem.repo.name}"
                "IssueCommentEvent" -> currentItem.payload!!.issue!!.html_url!!
                "IssuesEvent" -> currentItem.payload!!.issue!!.html_url!!
                "MemberEvent" -> "https://github.com/${currentItem.repo.name}"
                "PublicEvent" -> "https://github.com/${currentItem.repo.name}"
                "PullRequestEvent" -> currentItem.payload!!.pull_request!!.html_url!!
                "PullRequestReviewEvent" -> currentItem.payload!!.review!!.html_url!!
                "PullRequestReviewCommentEvent" -> currentItem.payload!!.comment!!.html_url!!
                "PushEvent" -> try {
                    "https://github.com/${currentItem.repo.name}/commit/${
                        currentItem.payload?.commits?.get(
                            0
                        )?.sha
                    }"
                } catch (e: Exception) {
                    "https://github.com/${currentItem.repo.name}/commit"
                }
                "ReleaseEvent" -> currentItem.payload!!.release!!.html_url!!
                "SponsorshipEvent" -> "https://github.com/${currentItem.repo.name}"
                "WatchEvent" -> "https://github.com/${currentItem.repo.name}"
                else -> "https://github.com/${currentItem.repo.name}"
            }
        } catch (e: Exception) {
            "https://github.com/${currentItem.repo.name}"
        }
    }

    fun isEmpty(): Boolean = !prefs.contains(Constants.userDetails)

    fun saveFeedType(feedType: FeedType) =
        prefs.edit { this.putString(Constants.feedType, feedType.name) }

    fun getFeedType(): String? = if (prefs.contains(Constants.feedType))
        prefs.getString(Constants.feedType, null)
    else
        null

    private fun addToWidgetAlertDialog(): AlertDialog {
        val alertDialogView =
            LayoutInflater.from(context).inflate(R.layout.loading_alertdialog, null)
        val alertDialogBuilder =
            AlertDialog.Builder(context).setView(alertDialogView).setCancelable(false)
        return alertDialogBuilder.show()
    }

    private fun enterPATokenAlertDialog(): AlertDialog {
        val alertDialogView =
            LayoutInflater.from(context).inflate(R.layout.pa_token_alertdialog, null)
        val alertDialogBuilder =
            AlertDialog.Builder(context).setView(alertDialogView).setCancelable(true)
        val paEditText = alertDialogView.findViewById<EditText>(R.id.enterPATokenEditText)
        val paTokenSubmitBtn = alertDialogView.findViewById<ImageButton>(R.id.paTokenSubmitBtn)
        val paTokenDisplayText = alertDialogView.findViewById<TextView>(R.id.paTokenDisplayText)
        val paTokenCopyBtn = alertDialogView.findViewById<ImageButton>(R.id.paTokenCopyBtn)
        val paTokenDeleteBtn = alertDialogView.findViewById<ImageButton>(R.id.paTokenDeleteBtn)

        val alertDialog = alertDialogBuilder.create()

        paTokenSubmitBtn.setOnClickListener {
            addUserPAToken(paEditText.text.toString())
        }
        paEditText.setOnEditorActionListener(OnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addUserPAToken(paEditText.text.toString())
                return@OnEditorActionListener true
            }
            false
        })
        paTokenCopyBtn.setOnClickListener { copyPAToken() }
        paTokenDeleteBtn.setOnClickListener { deletePAToken() }
        handleSharedPrefChange(paTokenDisplayText, alertDialog)

        return alertDialog
    }

    private fun copyPAToken() {
        try {
            val paToken = prefs.getString(Constants.paTokenKey, Security.defaultToken)
            if (paToken.isNullOrEmpty())
                throw Exception("Empty token")
            else if (paToken == Security.defaultToken)
                throw Exception("Default token cannot copied")

            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            val clip = ClipData.newPlainText("GitHub Personal Access Token", paToken)
            clipboard!!.setPrimaryClip(clip)
            Toast.makeText(context, "Token Copied", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            println(e.message)
            Toast.makeText(context, "Failed to copy: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun deletePAToken() {
        try {
            when (prefs.getString(Constants.paTokenKey, Security.defaultToken)) {
                Security.defaultToken -> throw Exception("Default token")
                null -> throw Exception("Null token")
                else -> {
                    val editor = prefs.edit()
                    editor.remove(Constants.paTokenKey)
                    editor.apply()
                    Toast.makeText(context, "Deleted successfully", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            println(e.message)
            Toast.makeText(context, "Cannot delete: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun onPressPATokenButton() = enterPATokenAlertDialog().show()

    private fun addUserPAToken(paToken: String?) {
        hideKeyboardFrom()
        if (paToken.isNullOrEmpty()) {
            Toast.makeText(context, "Cannot Add Empty PA Token", Toast.LENGTH_LONG).show()
        } else if (!paToken.startsWith("ghp_")) {
            Toast.makeText(context, "Invalid PA Token", Toast.LENGTH_LONG).show()
        } else {
            Common.retroFitService.getAuthenticatedUser("token $paToken").enqueue(
                object : Callback<ProfilePageModel> {
                    override fun onResponse(
                        call: Call<ProfilePageModel>,
                        response: Response<ProfilePageModel>
                    ) {
                        val user = response.body()
                        if (user == null || user.login.isNullOrEmpty()) {
                            Toast.makeText(context, "Invalid PA Token", Toast.LENGTH_LONG)
                                .show()
                        } else {
                            val editor: SharedPreferences.Editor = prefs.edit()
                            editor.putString(Constants.paTokenKey, paToken)
                            editor.apply()
                            Toast.makeText(context, "Token added successfully", Toast.LENGTH_LONG)
                                .show()
                        }
                    }

                    override fun onFailure(call: Call<ProfilePageModel>, t: Throwable) {
                        println(t.message)
                        Toast.makeText(context, "Invalid PA Token", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    fun getPAToken(): String? = prefs.getString(Constants.paTokenKey, Security.defaultToken)

    @SuppressLint("SetTextI18n")
    private fun handleSharedPrefChange(
        paTokenDisplayTextView: TextView,
        alertDialog: AlertDialog
    ) {
        val currentPAToken = getPAToken()
        paTokenDisplayTextView.text =
            if (currentPAToken == Security.defaultToken) "Default Token" else (currentPAToken?.substring(
                0,
                min(currentPAToken.length, 20)
            ) + "...")

        val listener = OnSharedPreferenceChangeListener { prefs, key ->
            if (key == Constants.paTokenKey) {
                val paToken = prefs.getString(key, Security.defaultToken)
                if (paToken == Security.defaultToken)
                    paTokenDisplayTextView.text = "Default Token"
                else
                    paTokenDisplayTextView.text = paToken
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)

        alertDialog.setOnCancelListener {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
        alertDialog.setOnDismissListener {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    private fun hideKeyboardFrom() {
        (context as Activity).currentFocus?.let { view ->
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}

class SortByDate : Comparator<AddToWidget> {
    override fun compare(first: AddToWidget?, second: AddToWidget?): Int {
        val firstDate = first!!.dateISO!!
        val secondDate = second!!.dateISO!!

        val dateTimePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val createFirstDate = LocalDateTime.parse(firstDate, dateTimePattern)
        val createSecondDate = LocalDateTime.parse(secondDate, dateTimePattern)
        val result = createFirstDate.compareTo(createSecondDate)
        return when {
            result < 0 -> 1
            result > 0 -> -1
            else -> 0
        }
    }
}

class RoundedTransformation(
    private val radius: Int, // dp
    private var margin: Int
) : Transformation {
    override fun transform(source: Bitmap): Bitmap {
        val paint = Paint()
        paint.isAntiAlias = true
        paint.shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawRoundRect(
            RectF(
                margin.toFloat(),
                margin.toFloat(),
                (source.width - margin).toFloat(),
                (source.height - margin).toFloat()
            ),
            radius.toFloat(), radius.toFloat(), paint
        )
        if (source != output) {
            source.recycle()
        }
        return output
    }

    override fun key(): String {
        return "rounded"
    }
}
