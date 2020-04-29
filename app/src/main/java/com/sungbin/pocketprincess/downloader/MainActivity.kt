package com.sungbin.pocketprincess.downloader

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.content_main.*
import android.view.WindowManager
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.thin.downloadmanager.DownloadRequest
import com.thin.downloadmanager.DownloadStatusListenerV1
import java.io.File
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.thin.downloadmanager.ThinDownloadManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.os.*
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.sungbin.sungbintool.ToastUtils
import kotlinx.android.synthetic.main.layout_dialog.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL


class MainActivity : AppCompatActivity() {

    private val CHECK_URI = "http://sungbin.josbar.io/PPC_DW_PW.txt"
    private val appPackage = "com.neowizgames.imo.ppclientu_gp"
    private val apkPath =
        "${Environment.getExternalStorageDirectory().absolutePath}/Download/PocketPrincess.apk"
    private val dataPath =
        "${Environment.getExternalStorageDirectory().absolutePath}/Android/obb/com.neowizgames.imo.ppclientu_gp/main.12.com.neowizgames.imo.ppclientu_gp.obb"
    private var downManager: ThinDownloadManager? = null
    private var downTaskApp: DownloadRequest? = null
    private var downTaskData: DownloadRequest? = null
    private var alert: AlertDialog? = null
    private var isInstallApp = true
    private var layout: LinearLayout? = null
    private var isInstallData = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.content_main)

        //showCheckDialog()

        isInstallData = File(dataPath).exists()
        isInstallApp = try {
            this.packageManager.getPackageInfo(appPackage, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
        downManager = ThinDownloadManager()

        val permissionlistener = object : PermissionListener {
            override fun onPermissionGranted() {
            }

            override fun onPermissionDenied(deniedPermissions: List<String>) {
                finish()
            }
        }

        TedPermission.with(applicationContext)
            .setRationaleTitle(getString(R.string.need_permission))
            .setRationaleMessage(getString(R.string.why_use_permission))
            .setPermissionListener(permissionlistener)
            .setDeniedMessage(getString(R.string.you_cant_use))
            .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
            .check()

        ppcChatRoom.setOnClickListener {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://open.kakao.com/o/gDJsinEb"))
            )
        }

        downTaskApp = DownloadRequest(Uri.parse(getString(R.string.apk_link)))
            .setDestinationURI(Uri.parse(
                apkPath
            ))
            .setPriority(DownloadRequest.Priority.HIGH)
            .setStatusListener(object : DownloadStatusListenerV1{
                @SuppressLint("SetTextI18n")
                override fun onDownloadComplete(downloadRequest: DownloadRequest?) {
                    isInstallApp = true
                    layout!!.findViewById<TextView>(R.id.title).text = "게임 다운로드 완료!"
                    layout!!.findViewById<TextView>(R.id.content).text = "게임 다운로드가 완료되었습니다.\n밑에 확인 버튼을 눌러주세요."
                    layout!!.findViewById<TextView>(R.id.close).visibility = View.VISIBLE
                    layout!!.findViewById<TextView>(R.id.close).setOnClickListener {
                        checkInstall()
                        alert!!.cancel()
                        installApp.text = getString(R.string.install_app_done)
                        ToastUtils.show(applicationContext,
                            "앱 다운로드가 완료 되었습니다.\n설치를 진행합니다.",
                            ToastUtils.SHORT, ToastUtils.INFO)
                        val builder = StrictMode.VmPolicy.Builder()
                        StrictMode.setVmPolicy(builder.build())
                        val intent = Intent(Intent.ACTION_VIEW)
                        if (Build.VERSION.SDK_INT >= 24) {
                            val uri = FileProvider.getUriForFile(applicationContext,
                                "${applicationContext.packageName}.provider",
                                File(apkPath))
                            intent.setDataAndType(uri, "application/vnd.android.package-archive")
                            intent.flags = FLAG_GRANT_READ_URI_PERMISSION
                            startActivity(intent)
                        } else {
                            intent.setDataAndType(
                                Uri.fromFile(
                                    File(apkPath)
                                ),
                                "application/vnd.android.package-archive")
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                    }
                }

                @SuppressLint("SetTextI18n")
                override fun onDownloadFailed(
                    downloadRequest: DownloadRequest?,
                    errorCode: Int,
                    errorMessage: String?
                ) {
                    installApp.performClick()
                }

                @SuppressLint("SetTextI18n")
                override fun onProgress(
                    downloadRequest: DownloadRequest?,
                    totalBytes: Long,
                    downloadedBytes: Long,
                    progress: Int
                ) {
                    val string = "다운로드중...\n<${downloadedBytes/1000000}" +
                            " / ${totalBytes/1000000} MB> (${progress}%)"
                    layout!!.findViewById<TextView>(R.id.title).text = "게임 다운로드 진행중..."
                    layout!!.findViewById<TextView>(R.id.content).text = string
                }

            })

        downTaskData = DownloadRequest(Uri.parse(getString(R.string.obb_link)))
            .setDestinationURI(Uri.parse(
                dataPath
            ))
            .setPriority(DownloadRequest.Priority.HIGH)
            .setStatusListener(object : DownloadStatusListenerV1{
                override fun onDownloadComplete(downloadRequest: DownloadRequest?) {
                    isInstallData = true
                    layout!!.findViewById<TextView>(R.id.title).text = "설치 완료!"
                    layout!!.findViewById<TextView>(R.id.content).text = "라이선스 우회파일 설치가 완료되었습니다.\n밑에 확인 버튼을 눌러주세요."
                    layout!!.findViewById<TextView>(R.id.close).visibility = View.VISIBLE
                    layout!!.findViewById<TextView>(R.id.close).setOnClickListener {
                        alert!!.cancel()
                        checkInstall()
                    }
                }

                @SuppressLint("SetTextI18n")
                override fun onDownloadFailed(
                    downloadRequest: DownloadRequest?,
                    errorCode: Int,
                    errorMessage: String?
                ) {
                    installData.performClick()
                }

                @SuppressLint("SetTextI18n")
                override fun onProgress(
                    downloadRequest: DownloadRequest?,
                    totalBytes: Long,
                    downloadedBytes: Long,
                    progress: Int
                ) {
                    val string = "다운로드중...\n<${downloadedBytes/1000000}" +
                            " / ${totalBytes/1000000} MB> (${progress}%)"
                    layout!!.findViewById<TextView>(R.id.title).text = "라이선스 우회파일 설치중..."
                    layout!!.findViewById<TextView>(R.id.content).text = string
                }

            })

        installApp.setOnClickListener {
            installApp.text = "게임 설치 진행중..."
            downManager!!.add(downTaskApp)
            val dialog = AlertDialog.Builder(this@MainActivity)
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            layout = LinearLayout(this@MainActivity)
            inflater.inflate(R.layout.layout_dialog, layout, true)
            layout!!.findViewById<TextView>(R.id.title).text = "다운로드 준비중..."
            layout!!.findViewById<TextView>(R.id.content).text = "게임 다운로드 준비중 입니다."
            layout!!.findViewById<TextView>(R.id.close).visibility = View.INVISIBLE
            dialog.setView(layout)
            dialog.setCancelable(false)
            alert = dialog.create()
            alert!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alert!!.show()
        }

        installData.setOnClickListener {
            downManager!!.add(downTaskData)
            val dialog = AlertDialog.Builder(this@MainActivity)
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            layout = LinearLayout(this@MainActivity)
            inflater.inflate(R.layout.layout_dialog, layout, true)
            layout!!.findViewById<TextView>(R.id.title).text = "설치 준비중..."
            layout!!.findViewById<TextView>(R.id.content).text = "라이선스 우회파일 설치 준비중 입니다."
            layout!!.findViewById<TextView>(R.id.close).visibility = View.INVISIBLE
            dialog.setView(layout)
            dialog.setCancelable(false)
            alert = dialog.create()
            alert!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alert!!.show()
        }

        installDone.setOnClickListener {
            val intent = applicationContext.getPackageManager().getLaunchIntentForPackage(appPackage);
            intent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

       checkInstall()
    }

    private fun checkInstall(){
        if(isInstallApp){
            installApp.text = "이미 게임이 설치되있습니다."
            installApp.isClickable = false
        }
        if(isInstallData){
            installData.text = "이미 라이선스 우회 파일이 설치되있습니다."
            installData.isClickable = false
        }
        if(isInstallApp && isInstallData){
            installApp.visibility = View.GONE
            installData.visibility = View.GONE
            installDone.visibility = View.VISIBLE
        }
    }

    private fun showCheckDialog(){
        val key = getData()
        val dialog = AlertDialog.Builder(this@MainActivity)
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        layout = LinearLayout(this@MainActivity)
        inflater.inflate(R.layout.layout_input_dialog, layout, true)
        layout!!.findViewById<TextView>(R.id.close).setOnClickListener {
            val input = layout!!.findViewById<EditText>(R.id.input).text.toString()
            if(input == key){
                ToastUtils.show(this, "환영합니다 :)",
                    ToastUtils.SHORT, ToastUtils.SUCCESS)
                alert!!.cancel()
            }
            else {
                ToastUtils.show(this, "인증번호가 일치하지 않습니다 :(",
                    ToastUtils.SHORT, ToastUtils.WARNING)
                alert!!.cancel()
                finish()
            }
        }
        dialog.setView(layout)
        dialog.setCancelable(false)
        alert = dialog.create()
        alert!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alert!!.show()
    }

    private fun getData(): String {
        try {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy);

            val url = URL(CHECK_URI)
            val con = url.openConnection()
            if (con != null) {
                con.connectTimeout = 5000
                con.useCaches = false
                val isr = InputStreamReader(con.getInputStream())
                val br = BufferedReader(isr)
                var str = br.readLine()
                var line: String? = ""
                while ({ line = br.readLine(); line }() != null) {
                    str += "\n" + line
                }
                br.close()
                isr.close()
                Log.d("PW", str)
                return str
            }
            return "연결 실패"
        } catch (e: Exception) {
            return "연결 실패"
        }
    }
}
