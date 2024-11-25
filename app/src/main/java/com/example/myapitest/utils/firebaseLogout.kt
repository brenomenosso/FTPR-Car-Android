package com.example.myapitest.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.myapitest.LoginActivity
import com.google.firebase.auth.FirebaseAuth

fun firebaseLogout(context: Context): Intent {
    Toast.makeText(
        context,
        "Efetuando Logout",
        Toast.LENGTH_SHORT
    ).show()
    FirebaseAuth.getInstance().signOut()
    return LoginActivity.newIntent(context)
}