package com.dscvit.gidget.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.dscvit.gidget.R
import com.dscvit.gidget.activities.ProfileActivity
import com.dscvit.gidget.common.Common
import com.dscvit.gidget.common.RoundedTransformation
import com.dscvit.gidget.common.Utils
import com.dscvit.gidget.interfaces.RetroFitService
import com.dscvit.gidget.models.searchPage.ItemsRepo
import com.squareup.picasso.Picasso

class SearchPageRepoAdapter(
    private val context: Context,
    private val searchPageDataList: MutableList<ItemsRepo>,
) : RecyclerView.Adapter<SearchPageRepoAdapter.SearchPageRepoViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchPageRepoViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_page_recycler_item, parent, false)
        return SearchPageRepoViewHolder(itemView)
    }

    override fun onBindViewHolder(holderRepo: SearchPageRepoViewHolder, position: Int) {
        val currentItem = searchPageDataList[position]
        val username = "@${currentItem.owner.login}"

        Picasso.get().load(currentItem.owner.avatar_url).error(R.drawable.github_logo).transform(
            RoundedTransformation(300, 0)
        ).into(holderRepo.profilePhoto)

        val type = if (currentItem.private)
            "Private"
        else
            "Public"

        holderRepo.name.text = currentItem.name
        holderRepo.username.text = username
        holderRepo.location.text = type

        // Add to widget
        holderRepo.addToWidgetButton.setOnClickListener {
            addToWidget(currentItem)
        }

        // onClick
        holderRepo.currentView.setOnClickListener { navigateToExternal(currentItem.html_url) }
        holderRepo.profilePhoto.setOnClickListener { openProfilePage(currentItem) }
    }

    override fun getItemCount(): Int = searchPageDataList.size

    private fun addToWidget(currentItem: ItemsRepo) {
        val mService: RetroFitService = Common.retroFitService
        Utils(context).addToWidget(
            mService = mService,
            isUser = false,
            username = "${currentItem.owner.login},false",
            name = currentItem.name,
            ownerAvatarUrl = currentItem.owner.avatar_url,
        )
    }

    private fun navigateToExternal(url: String?) {
        val uri: Uri = Uri.parse(url)
        val clickIntent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(clickIntent)
    }

    private fun openProfilePage(currentItem: ItemsRepo) {
        val intent = Intent(context, ProfileActivity::class.java)
        intent.putExtra("username", currentItem.owner.login)
        intent.putExtra("owner", false)
        context.startActivity(intent)
    }

    class SearchPageRepoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePhoto: ImageView = itemView.findViewById(R.id.searchPageRecyclerItemProfilePhoto)
        val name: TextView = itemView.findViewById(R.id.searchPageRecyclerItemNameText)
        val username: TextView = itemView.findViewById(R.id.searchPageRecyclerItemUsernameText)
        val location: TextView = itemView.findViewById(R.id.searchPageRecyclerItemLocationText)
        val addToWidgetButton: Button = itemView.findViewById(R.id.searchPageAddToHomeButton)
        val currentView: CardView = itemView.findViewById(R.id.searchPageCardView)
    }
}
