package com.dscvit.gidget.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dscvit.gidget.R
import com.dscvit.gidget.adapters.FeedPageAdapter
import com.dscvit.gidget.animations.BounceEdgeEffectFactory
import com.dscvit.gidget.common.*
import com.dscvit.gidget.interfaces.RetroFitService
import com.dscvit.gidget.models.activity.feedPage.FeedPageModel
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FeedActivity : AppCompatActivity() {
    private lateinit var mService: RetroFitService
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: FeedPageAdapter
    private val signUp = SignUp()
    private lateinit var security: Security
    private lateinit var utils: Utils
    private lateinit var recyclerView: RecyclerView
    private lateinit var profilePhoto: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var searchButton: CardView
    private lateinit var emptyTextView: TextView
    private lateinit var pullRefresh: SwipeRefreshLayout
    private lateinit var swapFollowing: ImageButton
    private lateinit var swapMe: Button
    private lateinit var feedTypeTextView: TextView
    private lateinit var paToken: ImageButton
    private lateinit var prefs: SharedPreferences
    private lateinit var listener: SharedPreferences.OnSharedPreferenceChangeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed)

        this.mService = Common.retroFitService
        this.security = Security(this)
        this.utils = Utils(this)
        this.recyclerView = findViewById(R.id.feedPageRecyclerView)
        this.profilePhoto = findViewById(R.id.feedPageProfilePhoto)
        this.progressBar = findViewById(R.id.feedpageProgressBar)
        this.searchButton = findViewById(R.id.feedPageSearchButton)
        this.emptyTextView = findViewById(R.id.feedPageEmptyTextView)
        this.pullRefresh = findViewById(R.id.feedPagePullRefresh)
        this.swapFollowing = findViewById(R.id.feedPageSwapBtnFollowing)
        this.swapMe = findViewById(R.id.feedPageSwapBtnMe)
        this.feedTypeTextView = findViewById(R.id.feedPageFeedTypeTV)
        this.paToken = findViewById(R.id.feedPagePATokenBtn)
        this.recyclerView.setHasFixedSize(true)

        this.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        this.recyclerView.layoutManager = layoutManager
        this.recyclerView.edgeEffectFactory = BounceEdgeEffectFactory()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val signedUpUserMap = signUp.getSignedUpUserDetails(this)

        getFeedList(signedUpUserMap)
        getProfilePhoto(signedUpUserMap)
        navigateToSearchPage()
        pullRefresh.setOnRefreshListener { refreshPage(signedUpUserMap) }
        swapButtonClicked(signedUpUserMap)
        onPressPATokenBtn(paToken)
        this.listener = getSharedPrefListener(signedUpUserMap)
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(this.listener)
    }

    private fun refreshPage(signedUpUserMap: MutableMap<String, String>) {
        pullRefresh.isRefreshing = true
        recyclerView.visibility = View.GONE
        getFeedList(signedUpUserMap)
        pullRefresh.isRefreshing = false
    }

    @SuppressLint("SetTextI18n")
    private fun getFeedList(signedUpUserMap: MutableMap<String, String>) {
        progressBar.visibility = View.VISIBLE
        val feedType: String? = utils.getFeedType()
        if (feedType == null) {
            utils.saveFeedType(FeedType.Following)
            feedTypeTextView.text = "(${FeedType.Following.name})"
            getFeedFollowing(signedUpUserMap)
        } else {
            when (feedType) {
                FeedType.Following.name -> {
                    feedTypeTextView.text = "(${FeedType.Following.name})"
                    swapFollowing.backgroundTintList =
                        ContextCompat.getColorStateList(this, R.color.feedPageRecyclerItemColor)
                    swapMe.backgroundTintList =
                        ContextCompat.getColorStateList(this, R.color.darkestBlue)
                    getFeedFollowing(signedUpUserMap)
                }
                FeedType.Me.name -> {
                    feedTypeTextView.text = "(${FeedType.Me.name})"
                    swapFollowing.backgroundTintList =
                        ContextCompat.getColorStateList(this, R.color.darkestBlue)
                    swapMe.backgroundTintList =
                        ContextCompat.getColorStateList(this, R.color.feedPageRecyclerItemColor)
                    getFeedMe(signedUpUserMap)
                }
            }
        }
    }

    private fun getFeedFollowing(signedUpUserMap: MutableMap<String, String>) {
        mService.getFeedFollowing(
            signedUpUserMap["username"]!!,
            "token ${security.getToken()}"
        )
            .enqueue(object : Callback<MutableList<FeedPageModel>> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onResponse(
                    call: Call<MutableList<FeedPageModel>>,
                    response: Response<MutableList<FeedPageModel>>
                ) {
                    if (response.body()!!.isEmpty()) {
                        progressBar.visibility = View.GONE
                        emptyTextView.visibility = View.VISIBLE
                    } else {
                        emptyTextView.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        adapter = FeedPageAdapter(
                            this@FeedActivity,
                            response.body() as MutableList<FeedPageModel>
                        )

                        adapter.notifyDataSetChanged()
                        recyclerView.adapter = adapter

                        progressBar.visibility = View.GONE
                    }
                }

                override fun onFailure(call: Call<MutableList<FeedPageModel>>, t: Throwable) {
                    println("Error occurred - $t")
                    progressBar.visibility = View.GONE
                    emptyTextView.visibility = View.VISIBLE
                }
            })
    }

    private fun getFeedMe(signedUpUserMap: MutableMap<String, String>) {
        mService.getFeedMe(
            signedUpUserMap["username"]!!,
            "token ${security.getToken()}"
        )
            .enqueue(object : Callback<MutableList<FeedPageModel>> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onResponse(
                    call: Call<MutableList<FeedPageModel>>,
                    response: Response<MutableList<FeedPageModel>>
                ) {
                    if (response.body()!!.isEmpty()) {
                        progressBar.visibility = View.GONE
                        emptyTextView.visibility = View.VISIBLE
                    } else {
                        emptyTextView.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        adapter = FeedPageAdapter(
                            this@FeedActivity,
                            response.body() as MutableList<FeedPageModel>
                        )

                        adapter.notifyDataSetChanged()
                        recyclerView.adapter = adapter

                        progressBar.visibility = View.GONE
                    }
                }

                override fun onFailure(call: Call<MutableList<FeedPageModel>>, t: Throwable) {
                    println("Error occurred - $t")
                    progressBar.visibility = View.GONE
                    emptyTextView.visibility = View.VISIBLE
                }
            })
    }

    private fun getProfilePhoto(signedUpUserMap: MutableMap<String, String>) {
        val photoUrl = signedUpUserMap["photoUrl"]
        Picasso.get().load(photoUrl).into(profilePhoto)
        profilePhoto.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("username", signedUpUserMap["username"])
            intent.putExtra("owner", true)
            startActivity(intent)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun swapButtonClicked(signedUpUserMap: MutableMap<String, String>) {
        swapFollowing.setOnClickListener {
            recyclerView.visibility = View.GONE
            swapFollowing.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.feedPageRecyclerItemColor)
            swapMe.backgroundTintList = ContextCompat.getColorStateList(this, R.color.darkestBlue)
            utils.saveFeedType(FeedType.Following)
            feedTypeTextView.text = "(${FeedType.Following.name})"
            getFeedList(signedUpUserMap)
        }
        swapMe.setOnClickListener {
            recyclerView.visibility = View.GONE
            swapFollowing.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.darkestBlue)
            swapMe.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.feedPageRecyclerItemColor)
            utils.saveFeedType(FeedType.Me)
            feedTypeTextView.text = "(${FeedType.Me.name})"
            getFeedList(signedUpUserMap)
        }
    }

    private fun navigateToSearchPage() {
        searchButton.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
    }

    private fun onPressPATokenBtn(paTokenBtn: ImageButton) {
        paTokenBtn.setOnClickListener {
            utils.onPressPATokenButton()
        }
    }

    private fun getSharedPrefListener(signedUpUserMap: MutableMap<String, String>): SharedPreferences.OnSharedPreferenceChangeListener {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            println(key)
            if (key == Constants.paTokenKey) {
                refreshPage(signedUpUserMap)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        return listener
    }
}
