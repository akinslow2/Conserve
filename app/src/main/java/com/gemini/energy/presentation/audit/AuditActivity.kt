package com.gemini.energy.presentation.audit

import CompanyCamAPI.CompanyCamServiceFactory
import android.Manifest
import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.gemini.energy.R
import com.gemini.energy.data.local.system.AuditDatabase
import com.gemini.energy.databinding.ActivityHomeDetailBinding
import com.gemini.energy.presentation.audit.detail.adapter.DetailPagerAdapter
import com.gemini.energy.presentation.audit.detail.preaudit.PreAuditFragment
import com.gemini.energy.presentation.audit.detail.zone.list.ZoneListFragment
import com.gemini.energy.presentation.audit.dialog.AuditDialogFragment
import com.gemini.energy.presentation.audit.list.AuditListFragment
import com.gemini.energy.presentation.audit.list.model.AuditModel
import com.gemini.energy.presentation.base.BaseActivity
import com.gemini.energy.presentation.util.Navigator
import com.gemini.energy.service.EnergyService
import com.gemini.energy.service.OutgoingRows
import com.gemini.energy.service.sync.Collection
import com.gemini.energy.service.sync.Connection
import com.gemini.energy.service.sync.Syncer
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import kotlinx.android.synthetic.main.activity_home_detail.*
import timber.log.Timber
import java.io.File
import java.util.*
import javax.inject.Inject

class AuditActivity : BaseActivity(), AuditListFragment.OnAuditSelectedListener {

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var energyService: EnergyService

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var auditListFragment: AuditListFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedViewModel = ViewModelProviders.of(this).get(SharedViewModel::class.java)
        sharedViewModel.getAudit().observe(this,
                Observer<AuditModel> { })

        super.binder?.let {
            setupContent(it)
            setupAuditList()
        }

        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions, 101)
        }

        // capture auth code for companycam api
        val data = intent?.data
        if (data != null)
            CompanyCamServiceFactory.setToken(data)

    }


    /*
    * Option Menu Management
    * */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_create_audit -> consume { showCreateAudit() }
        R.id.menu_energy_calculation -> consume {
            linlaHeaderProgress.visibility = View.VISIBLE
            energyService.run(callback = {
                Log.d(TAG, "\\m/ End of Computation \\m/ - (${Thread.currentThread().name})")
                linlaHeaderProgress.visibility = View.GONE
                val message = if (it) {
                    "Success"
                } else {
                    "Failed"
                }
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            })
        }
        R.id.menu_sync -> consume { sync() }
        R.id.menu_preference -> consume {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        R.id.menu_upload_photo -> consume {
            // these all return null :-(
            val audit = sharedViewModel.getAudit().value
            val zone = sharedViewModel.getZone().value
            val type = sharedViewModel.getType().value
            val subtype = sharedViewModel.getSubType().value

            // this gets the tag for the preaudit fragment
            val tag = "${ANDROID_SWITCHER}:${view_pager.id}:${PREAUDIT_FRAGMENT_INDEX}"
            // this gets the preaudit fragment
            val fragment = supportFragmentManager.findFragmentByTag(tag) as PreAuditFragment?
            // this gets the selected audit id from the preaudit fragment
            val auditId = fragment?.getAuditId()

            val address = if (auditId != null) getAddressFromAuditId(auditId) else ""

            val projectName = findViewById<TextView>(R.id.txt_header_audit).text.toString()
            startPhotoUploadToCompanyCam(projectName, address, listOf())
        }
        else -> super.onOptionsItemSelected(item)
    }

    private inline fun consume(f: () -> Unit): Boolean {
        f()
        return true
    }


    /*
    * View Pager Main Content
    * */
    private fun setupContent(binder: ActivityHomeDetailBinding) {
        binder.viewPager.adapter = DetailPagerAdapter(supportFragmentManager)
    }


    /*
    * Side Panel Content Setup
    *
    * 1. Audit List
    * 2. Audit Selection Listener - Which gets activated from the [Adapter - Fragment - Activity]
    * 3. Set the Top Right Header to the current Active Audit Id
    * 4. Refresh the Zone View Model - As we need to fetch the Zone for the selected Audit
    * 5. Dispose the Subscribed Get Audit Channel when the Activity dies off
    *
    * */
    private fun setupAuditList() {
        val auditListFragment = AuditListFragment.newInstance()
        this.auditListFragment = auditListFragment

        supportFragmentManager
                .beginTransaction()
                .add(R.id.side_bar, auditListFragment, FRAG_AUDIT_LIST)
                .commit()
    }

    override fun onAuditSelected(observable: Observable<AuditModel>) {
        disposables.add(observable.subscribeWith(object : DisposableObserver<AuditModel>() {
            override fun onComplete() {}
            override fun onNext(t: AuditModel) {
                setAuditHeader(t)
                refreshZoneViewModel(t)
                refreshPreAudit(t)
            }

            override fun onError(e: Throwable) {}
        }))
    }

    private fun setAuditHeader(audit: AuditModel) {
        findViewById<TextView>(R.id.txt_header_audit).text = audit.name
    }

    private fun refreshZoneViewModel(auditModel: AuditModel) {
        val tag = "${ANDROID_SWITCHER}:${view_pager.id}:${ZONE_LIST_FRAGMENT_INDEX}"
        val fragment = supportFragmentManager.findFragmentByTag(tag) as ZoneListFragment?
        fragment?.let {
            fragment.setAuditModel(auditModel)
        }
    }

    private fun refreshPreAudit(auditModel: AuditModel) {
        val tag = "${ANDROID_SWITCHER}:${view_pager.id}:${PREAUDIT_FRAGMENT_INDEX}"
        val fragment = supportFragmentManager.findFragmentByTag(tag) as PreAuditFragment?
        fragment?.let {
            fragment.setAuditModel(auditModel)
        }
    }

    override fun onDestroy() {
        disposables.dispose()
        super.onDestroy()
    }

    private fun showCreateAudit() {
        val dialogFragment = AuditDialogFragment()
        dialogFragment.audit = null
        dialogFragment.show(supportFragmentManager, FRAG_DIALOG)
    }

    /**
     * UtilityRate Methods to Write Log to External Storage
     * ToDo: Move these out of Activity
     * */
    private fun getPid(): String? {
        val ps = "ps"

        try {
            val process = Runtime.getRuntime().exec(ps)
            process.waitFor()
            val output = process.inputStream.bufferedReader().use { it.readLines() }
            output.forEach {
                if (it.matches(".*com\\.gemini\\.energy.*".toRegex())) {
                    Timber.d(it)
                    val pid = "[0-9]{5}".toRegex().find(it)?.value
                    Timber.d("PID :: $pid")
                    return pid
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    private fun sync() = Connection().sync(mListener)

    private val mListener: Syncer.Listener = object: Syncer.Listener {
        override fun onPreExecute() { linlaHeaderProgress.visibility = View.VISIBLE }
        override fun onPostDownload(sync: Syncer) {

            val mColListenerUpload: Collection.Listener = object: Collection.Listener {
                override fun onPreExecute() {}
                override fun onPostExecute(col: Collection?) {
                    col?.let {
                        sync.refreshCollection(it)
                        sync.gUpload()
                    }
                }
            }

            Collection.create(mColListenerUpload)

        }
        override fun onPostExecute() { auditListFragment.refresh(); linlaHeaderProgress.visibility = View.GONE }
    }

    private fun writeLog() {
        val writer = OutgoingRows(applicationContext)
        val directory = writer.getDocumentFolderPath(OutgoingRows.DEBUG)!!
        val logFile = File(directory.toString(), "${Date().time}.txt")

        val pid = getPid()
        val logcat = "logcat -f $logFile"
        Timber.d(logcat)

        try {
            Runtime.getRuntime().exec(logcat)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Toast.makeText(applicationContext, "Log Created", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "AuditActivity"

        private const val FRAG_DIALOG = "AuditDialogFragment"
        private const val FRAG_AUDIT_LIST = "AuditListFragment"

        private const val ANDROID_SWITCHER = "android:switcher"
        private const val ZONE_LIST_FRAGMENT_INDEX = 1
        private const val PREAUDIT_FRAGMENT_INDEX = 0

        private var disposables = CompositeDisposable()
    }

}
