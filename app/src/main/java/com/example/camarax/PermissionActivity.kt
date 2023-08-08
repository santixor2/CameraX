package com.example.camarax

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.camarax.util.Permission

class PermissionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)
        checkRequestPermission()
    }

    private fun checkRequestPermission(){
        if(Permission.hasPermissions(this)) startAct()
        else Permission.requestPermission(this,this)
    }
    private fun startAct(){
        Intent(this,MainActivity::class.java).also {
            startActivity(it)
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 1){
            if(grantResults [0] == PackageManager.PERMISSION_GRANTED
                && grantResults [1] == PackageManager.PERMISSION_GRANTED
                && grantResults [2] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Permission Granted", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this,"Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }

        if(Permission.hasPermissions(this))
            startAct()
    }
}