package com.dscvit.gidget.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dscvit.gidget.R
import com.dscvit.gidget.adapters.DeleteGidgetItemAdapter
import com.dscvit.gidget.animations.BounceEdgeEffectFactory
import com.dscvit.gidget.common.Utils

class DeleteUserFromGidgetActivity : AppCompatActivity() {
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: DeleteGidgetItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delete_user_from_gidget)

        val progressBar: ProgressBar = findViewById(R.id.deleteUserFromGidgetProgressBar)
        val emptyTextView: TextView = findViewById(R.id.deleteUserFromGidgetEmptyText)
        val recyclerView: RecyclerView = findViewById(R.id.deleteUserFromGidgetRecyclerView)
        val backButton: ImageView = findViewById(R.id.deleteUserFromGidgetBackButton)

        recyclerView.setHasFixedSize(true)
        layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recyclerView.layoutManager = layoutManager
        recyclerView.edgeEffectFactory = BounceEdgeEffectFactory()

        populateData(progressBar, emptyTextView, recyclerView)
        backButton.setOnClickListener { finish() }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun populateData(
        progressBar: ProgressBar,
        emptyTextView: TextView,
        recyclerView: RecyclerView
    ) {
        recyclerView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        val utils = Utils(this)
        val userMap: MutableMap<String, MutableMap<String, String>>? = utils.getUserDetails()

        if (utils.isEmpty() || userMap.isNullOrEmpty()) {
            progressBar.visibility = View.GONE
            emptyTextView.visibility = View.VISIBLE
        } else {
            emptyTextView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter = DeleteGidgetItemAdapter(this, userMap)
            adapter.notifyDataSetChanged()
            recyclerView.adapter = adapter
            progressBar.visibility = View.GONE
        }
    }
}
