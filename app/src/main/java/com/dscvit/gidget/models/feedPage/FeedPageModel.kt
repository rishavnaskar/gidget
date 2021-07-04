package com.dscvit.gidget.models.feedPage

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

data class Payload(
    @SerializedName("action") val action: String? = null,
    @SerializedName("commits") val commits: Array<Commits>? = null,
    @SerializedName("forkee") val forkee: Forkee? = null,
    @SerializedName("issue") val issue: Issue? = null,
    @SerializedName("pull_request") val pull_request: PullRequest? = null,
    @SerializedName("review") val review: Review? = null,
    @SerializedName("comment") val comment: Comment? = null,
    @SerializedName("release") val release: Release? = null,
)

// Payload subclasses

data class Commits(
    @SerializedName("sha") val sha: String? = null,
    @SerializedName("message") val message: String? = null
)

data class Forkee(
    @SerializedName("html_url") val html_url: String? = null,
)

data class Issue(
    @SerializedName("html_url") val html_url: String? = null,
    @SerializedName("title") val title: String? = null
)

data class PullRequest(
    @SerializedName("html_url") val html_url: String? = null,
    @SerializedName("title") val title: String? = null
)

data class Review(
    @SerializedName("html_url") val html_url: String? = null
)

data class Comment(
    @SerializedName("html_url") val html_url: String? = null,
    @SerializedName("body") val body: String? = null
)

data class Release(
    @SerializedName("html_url") val html_url: String? = null,
    @SerializedName("name") val name: String? = null
)
