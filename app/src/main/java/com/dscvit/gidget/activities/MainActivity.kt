package com.dscvit.gidget.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dscvit.gidget.R
import com.dscvit.gidget.common.Common
import com.dscvit.gidget.common.FeedType
import com.dscvit.gidget.common.Security
import com.dscvit.gidget.common.SignUp
import com.dscvit.gidget.common.Utils
import com.dscvit.gidget.models.authModel.AccessToken
import com.dscvit.gidget.models.profilePage.ProfilePageModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var loginButton: Button
    private lateinit var security: Security
    private lateinit var utils: Utils
    private lateinit var clientID: String
    private lateinit var clientSecret: String
    private val redirectUrl: String = "gidget://auth"
    private val signUp = SignUp()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.security = Security(this)
        this.utils = Utils(this)
        clientID = security.getClientId()
        clientSecret = security.getClientSecret()

        if (signUp.isUserSignedUp(this)) {
            startActivity(Intent(this, FeedActivity::class.java))
            finish()
        } else
            loginButtonOnTap()
    }

    private fun loginButtonOnTap() {
        progressBar = findViewById(R.id.mainPageProgressBar)
        loginButton = findViewById(R.id.buLogin)

        loginButton.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            loginButton.visibility = View.INVISIBLE

            try {
                Toast.makeText(this, "Please open in browser", Toast.LENGTH_SHORT).show()
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/login/oauth/authorize?client_id=$clientID&scope=user:email&redirect_uri=$redirectUrl")
                )
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Check your Internet connection!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        progressBar.visibility = View.INVISIBLE
        loginButton.visibility = View.VISIBLE
        try {
            val uri: Uri? = intent.data
            if (uri != null && uri.toString().startsWith(redirectUrl)) {
                val code: String? = uri.getQueryParameter("code")

                if (code != null) {
                    progressBar.visibility = View.VISIBLE
                    loginButton.visibility = View.INVISIBLE

                    Common.authService.getAccessToken(clientID, clientSecret, code, redirectUrl)
                        .enqueue(object : Callback<AccessToken> {
                            override fun onResponse(
                                call: Call<AccessToken>,
                                response: Response<AccessToken>
                            ) {
                                if (response.body()?.access_token != null) {
                                    val accessToken: String = response.body()!!.access_token!!
                                    Common.retroFitService.getAuthenticatedUser("token $accessToken")
                                        .enqueue(object : Callback<ProfilePageModel> {
                                            override fun onResponse(
                                                call: Call<ProfilePageModel>,
                                                response: Response<ProfilePageModel>
                                            ) {
                                                try {
                                                    val user = response.body()
                                                    if (user != null) {
                                                        if (signUp.signUpUser(
                                                                this@MainActivity,
                                                                user.login
                                                                    ?: throw Exception("Gidget is unable access your username"),
                                                                user.name
                                                                    ?: "...",
                                                                user.avatar_url
                                                                    ?: throw Exception("Gidget is unable access your avatar")
                                                            )
                                                        ) {
                                                            utils.saveFeedType(FeedType.Following)
                                                            Toast.makeText(
                                                                this@MainActivity,
                                                                "Logged in",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            startActivity(
                                                                Intent(
                                                                    this@MainActivity,
                                                                    FeedActivity::class.java
                                                                )
                                                            )
                                                            finish()
                                                        } else
                                                            throw Exception()
                                                    }
                                                } catch (e: Exception) {
                                                    progressBar.visibility = View.INVISIBLE
                                                    loginButton.visibility = View.VISIBLE
                                                    if (e.message.isNullOrEmpty()) Toast.makeText(
                                                        this@MainActivity,
                                                        "We ran into some error!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    else Toast.makeText(
                                                        this@MainActivity,
                                                        e.message,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }

                                            override fun onFailure(
                                                call: Call<ProfilePageModel>,
                                                t: Throwable
                                            ) {
                                                progressBar.visibility = View.INVISIBLE
                                                loginButton.visibility = View.VISIBLE
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Could not fetch user",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                println(t.message)
                                            }
                                        })
                                } else throw Exception("Null access token")
                            }

                            override fun onFailure(call: Call<AccessToken>, t: Throwable) {
                                progressBar.visibility = View.INVISIBLE
                                loginButton.visibility = View.VISIBLE
                                Toast.makeText(
                                    this@MainActivity,
                                    "Access token error",
                                    Toast.LENGTH_SHORT
                                ).show()
                                println(t.message)
                            }
                        })
                } else if (uri.getQueryParameter("error") != null) {
                    progressBar.visibility = View.INVISIBLE
                    loginButton.visibility = View.VISIBLE
                    Toast.makeText(
                        this@MainActivity,
                        "We ran into some error!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            progressBar.visibility = View.INVISIBLE
            loginButton.visibility = View.VISIBLE
            Toast.makeText(
                this,
                "Something went wrong. Please check your GitHub settings",
                Toast.LENGTH_SHORT
            ).show()
        }
        super.onResume()
    }
}
