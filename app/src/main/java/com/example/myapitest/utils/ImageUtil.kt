package com.example.myapitest.ui

import android.widget.ImageView
import com.example.myapitest.R
import com.squareup.picasso.Picasso

fun ImageView.loadUrl(imageUrl: String) {
    Picasso.get()
        .load(imageUrl)
        .placeholder(R.drawable.placeholder)
        .error(R.drawable.placeholder)
        .into(this)
}