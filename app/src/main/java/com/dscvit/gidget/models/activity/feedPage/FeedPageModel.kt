package com.dscvit.gidget.models.activity.feedPage

import com.dscvit.gidget.models.activity.commonDataClasses.Payload
import com.google.gson.annotations.SerializedName

data class FeedPageModel(
    @SerializedName("id") val id: Long,
    @SerializedName("type") val type: String,
    @SerializedName("actor") val actor: Actor,
    @SerializedName("repo") val repo: Repo,
    @SerializedName("payload") val payload: Payload? = null,
    @SerializedName("public") val public: Boolean,
    @SerializedName("created_at") val created_at: String,
    @SerializedName("org") val org: Org
)

data class Actor(
    @SerializedName("id") val id: Long,
    @SerializedName("login") val login: String,
    @SerializedName("display_login") val display_login: String,
    @SerializedName("gravatar_id") val gravatar_id: String,
    @SerializedName("url") val url: String,
    @SerializedName("avatar_url") val avatar_url: String
)

data class Org(
    @SerializedName("id") val id: Long,
    @SerializedName("login") val login: String,
    @SerializedName("gravatar_id") val gravatar_id: String,
    @SerializedName("url") val url: String,
    @SerializedName("avatar_url") val avatar_url: String
)

data class Repo(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("url") val url: String
)
