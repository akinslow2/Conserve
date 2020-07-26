package com.gemini.energy.presentation.type

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.gemini.energy.App
import com.gemini.energy.R
import com.gemini.energy.databinding.ActivityHomeDetailBinding
import com.gemini.energy.presentation.audit.detail.zone.list.ZoneListFragment
import com.gemini.energy.presentation.audit.detail.zone.list.model.ZoneModel
import com.gemini.energy.presentation.audit.list.model.AuditModel
import com.gemini.energy.presentation.base.BaseActivity
import com.gemini.energy.presentation.type.adapter.FeaturePagerAdapter
import com.gemini.energy.presentation.type.adapter.TypePagerAdapter
import com.gemini.energy.presentation.type.list.TypeListFragment
import com.gemini.energy.presentation.type.list.model.TypeModel
import com.gemini.energy.presentation.util.EAction
import kotlinx.android.synthetic.main.activity_home_detail.*
import timber.log.Timber

class TypeActivity : BaseActivity(),
        ZoneListFragment.OnZoneSelectedListener,
        TypeListFragment.OnTypeSelectedListener {


    /*
    * Case 1 : Set via ZoneListFragment - On Zone Click - Within different Activity
    * Case 2 : Set via ZoneListFragment - On Zone Click - Within the same Activity
    * */
    private var auditModel: AuditModel? = null
    private var zoneModel: ZoneModel? = null
    private var typeModel: TypeModel? = null

    private var typeId: Int? = null

    private var defaultZonePosition = -1
    private var defaultTypePosition = -1

    /*
    * Fragment Lifecycle Methods
    * Type Counter - 0 :: Parent Type Activity
    * Type Counter - 1 :: Child Type Activity
    *
    * Note : Only Plugload, Refrigeration, and Lighting have Parent | Child rest Types only have Parent
    *
    * */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupArguments()

        super.binder?.let {
            setupContent(it)
            setupSideMenu()
        }

    }

    override fun onResume() {
        super.onResume()

        when (app.getCount()) {
            0 -> Timber.d("*** PARENT TYPE ACTIVITY ***")
            1 -> Timber.d("*** CHILD TYPE ACTIVITY ***")
        }
    }

    private fun setupArguments() {

        setAudit(intent.getParcelableExtra(PARCEL_AUDIT))
        setZone(intent.getParcelableExtra(PARCEL_ZONE))
        setZonePosition(intent.getIntExtra(PARCEL_POSITION_ZONE, -1))

        setType(intent.getParcelableExtra(PARCEL_TYPE))
        setTypePosition(intent.getIntExtra(PARCEL_POSITION_TYPE, -1))
        setTypeId(intent.getIntExtra("typeId", 0))

        zoneModel?.let {
            setHeader(it, typeModel)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_upload, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_upload_photo -> consume {

            val projectName = auditModel?.name
            val zoneName = zoneModel?.name
            val equipmentName = typeModel?.name
            val equipmentType = typeModel?.type
            val equipmentSubtype = typeModel?.subType

            val tags = arrayOf(zoneName, equipmentName, equipmentType, equipmentSubtype)

            startPhotoUploadToCompanyCam(projectName, getAddressFromAuditId(auditModel?.id), tags.filterNotNull())
        }
        else -> super.onOptionsItemSelected(item)
    }

    private inline fun consume(f: () -> Unit): Boolean {
        f()
        return true
    }


    /*
    * View Pager Main Content
    * Morphs it's behaviour based on the Type - [Parent or Child]
    * */
    private fun setupContent(binder: ActivityHomeDetailBinding) {

        if (zoneModel == null || auditModel == null) {
            Timber.d("Null - Zone or Audit")
            return
        }

        if (app.isParent()) {
            binder.viewPager.adapter = TypePagerAdapter(
                    supportFragmentManager, zoneModel!!, auditModel!!, defaultTypePosition)
        }

        if (app.isChild()) {
            binder.viewPager.adapter = FeaturePagerAdapter(
                    supportFragmentManager, typeId ?: 0)
        }

    }


    private fun setupSideMenu() {
        if (app.isParent()) { setupZoneList() }
        if (app.isChild()) { setupTypeList() }
    }


    /*
    * Side Panel Content Setup
    * Loading the Audit List Fragment
    * */
    private fun setupZoneList() {
        val zoneListFragment =
                ZoneListFragment.newInstance(zoneModel?.auditId!!, "n/a", defaultZonePosition)

        supportFragmentManager
                .beginTransaction()
                .add(R.id.side_bar, zoneListFragment, FRAG_ZONE_LIST)
                .commit()

    }

    private fun setupTypeList() {

        Timber.d("Type Id -- $typeId")
        Timber.d("Zone Model -- ${zoneModel.toString()}")
        Timber.d("Audit Model -- ${auditModel.toString()}")

        if (typeId != null && zoneModel != null && auditModel != null) {
            val typeListFragment = TypeListFragment.newInstance(
                    typeId!!, zoneModel!!, auditModel!!, defaultTypePosition
            )

            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.side_bar, typeListFragment, FRAG_TYPE_LIST)
                    .commit()
        }

    }

    /*
    * This gets called when the Zone is selected
    * Should reload the specific Fragment that is currently being selected
    * */
    private fun refreshTypeViewModel(zone: ZoneModel) {

        for (index in 0..4) {
            val tag = "$ANDROID_SWITCHER:${view_pager.id}:$index"

            val fragment = supportFragmentManager
                    .findFragmentByTag(tag) as TypeListFragment?

            fragment?.let {
                fragment.setZoneModel(zone)
            }
        }
    }


    /*
    * Listeners Setup
    * */
    override fun onZoneSelected(zone: ZoneModel, position: Int) {
        setZone(zone)
        setZonePosition(position)
        setHeader(zone)
        refreshTypeViewModel(zone)
    }

    override fun onTypeSelected(type: TypeModel, positon: Int) {
        if (zoneModel != null) {
            setHeader(zoneModel!!, type)
        }
    }


    /*
    * Data Setup
    * */
    private fun setAudit(audit: AuditModel?) {
        this.auditModel = audit
    }

    private fun setZone(zone: ZoneModel?) {
        this.zoneModel = zone
    }

    private fun setZonePosition(position: Int) {
        this.defaultZonePosition = position
    }

    private fun setTypePosition(position: Int) {
        this.defaultTypePosition = position
    }

    private fun setType(type: TypeModel?) {
        this.typeModel = type
    }

    private fun setTypeId(id: Int) {
        this.typeId = id
    }

    private fun setHeader(zone: ZoneModel, type: TypeModel? = null) {
        var header = "${auditModel?.name} > ${zone.name}"
        if (type != null) {
            header = "$header > ${type.name}"
        }

        findViewById<TextView>(R.id.txt_header_audit).text = header
    }

    override fun setupToolbar() {
        super.setupToolbar()
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()

        if (App.instance.getCount() > 0) {
            App.instance.setCounter(EAction.Pop)
        }

        return true
    }

    companion object {
        private var app: App = App.instance

        private const val FRAG_ZONE_LIST    = "TypeActivityZoneListFragment"
        private const val FRAG_TYPE_LIST    = "TypeActivityTypeListFragment"

        private const val PARCEL_AUDIT      = "EXTRA.AUDIT"
        private const val PARCEL_ZONE       = "EXTRA.ZONE"
        private const val PARCEL_TYPE       = "EXTRA.TYPE"
        private const val PARCEL_POSITION_ZONE      = "EXTRA.POSITION.ZONE"
        private const val PARCEL_POSITION_TYPE      = "EXTRA.POSITION.TYPE"

        private const val ANDROID_SWITCHER = "android:switcher"
    }

}