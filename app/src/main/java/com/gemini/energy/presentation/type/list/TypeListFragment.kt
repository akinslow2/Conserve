package com.gemini.energy.presentation.type.list

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gemini.energy.App
import com.gemini.energy.R
import com.gemini.energy.databinding.FragmentZoneTypeListBinding
import com.gemini.energy.internal.util.lazyThreadSafetyNone
import com.gemini.energy.presentation.audit.detail.zone.list.model.ZoneModel
import com.gemini.energy.presentation.audit.list.model.AuditModel
import com.gemini.energy.presentation.base.BaseActivity
import com.gemini.energy.presentation.type.SharedViewModel
import com.gemini.energy.presentation.type.TypeActivity
import com.gemini.energy.presentation.type.dialog.TypeCreateViewModel
import com.gemini.energy.presentation.type.dialog.TypeDialogFragment
import com.gemini.energy.presentation.type.list.adapter.TypeListAdapter
import com.gemini.energy.presentation.type.list.model.TypeModel
import com.gemini.energy.presentation.util.EAction
import com.gemini.energy.presentation.util.EZoneType
import dagger.android.support.DaggerFragment
import timber.log.Timber
import javax.inject.Inject

class TypeListFragment : DaggerFragment(),

        TypeListAdapter.OnTypeClickListener,
        TypeDialogFragment.OnTypeCreateListener,

        View.OnClickListener {


    interface OnTypeSelectedListener {
        fun onTypeSelected(type: TypeModel, position: Int)
    }


    /*
    * View Model Setup - [ TypeListViewModel ]
    * */
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val typeListViewModel by lazyThreadSafetyNone {
        ViewModelProviders.of(this, viewModelFactory).get(TypeListViewModel::class.java)
    }

    private val typeCreateViewModel by lazyThreadSafetyNone {
        ViewModelProviders.of(this, viewModelFactory).get(TypeCreateViewModel::class.java)
    }


    /*
    * Binder - fragment_zone_list.xml
    * */
    private lateinit var binder: FragmentZoneTypeListBinding

    private var auditModel: AuditModel? = null
    private var zoneModel: ZoneModel? = null
    private var typeModel: TypeModel? = null
    private var position = -1


    /*
    * Type Id is used to query the specific Type
    * [Plugload - Motors - HVAC - Lighting - Others]
    * */
    private var typeId: Int? = null

    private lateinit var sharedViewModel: SharedViewModel

    /*
    * Fragment Life Cycle Methods
    * */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupArguments()
        setupListeners()

        sharedViewModel = ViewModelProviders.of(activity!!).get(SharedViewModel::class.java)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binder = DataBindingUtil.inflate(inflater,
                R.layout.fragment_type_list, container, false)

        binder.viewModel = typeListViewModel
        binder.callbacks = this
        binder.fabClick = this
        binder.app = app
        binder.showCreate = app.isParent()

        binder.recyclerView.layoutManager = LinearLayoutManager(context)

        return binder.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.d("PIT STOP !! PIT STOP -- DEFAULT TYPE POSITION -- [$position]")
        if (position == -1) return
        val handler = Handler()
        handler.postDelayed({
            try {
                binder.recyclerView.scrollToPosition(position)
                val vh = binder.recyclerView.findViewHolderForAdapterPosition(position)
                vh.itemView.findViewById<CardView>(R.id.card_view_type).performClick()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 200)
    }

    private fun showCreateZoneType(type: TypeModel? = null) {
        typeId?.let {
            val fragment = TypeDialogFragment.newInstance(it)
            fragment.show(childFragmentManager, FRAG_DIALOG)
            fragment.type = type
        }

    }

    private fun refreshViewModel() {
        zoneModel?.let {zone ->
            typeId?.let { typeId ->
                typeListViewModel.loadZoneTypeList(
                        zone.id!!,
                        getType(typeId)?.value!!
                )
            }
        }
    }

    private fun setupArguments() {

        val zone = arguments?.getParcelable<ZoneModel>(PARCEL_ZONE)
        val audit = arguments?.getParcelable<AuditModel>(PARCEL_AUDIT)
        val position = arguments?.getInt(PARCEL_POSITION, -1)

        setAuditModel(audit)
        setZoneModel(zone)
        setTypeId(arguments?.getInt("typeId"))

        if (app.isChild()) { refreshViewModel() }

        position?.let { this.position = it }

        Timber.d(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
        Timber.d("POSITION -- DEFAULT TYPE !! DEFAULT TYPE !! -- POSITION")
        Timber.d("$position")
        Timber.d(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")

    }

    /*
    * Listeners | Observers
    * */
    override fun onClick(v: View?) {
        showCreateZoneType()
    }

    // *** Navigator : Audit Activity <> Type Activity *** //
    override fun onTypeClick(view: View, typeModel: TypeModel, position: Int) {

        // Step 1 : Check if the Type has a Child
        // Step 2 : If it has a Child load the Type Activity with the proper set of Dialog
        // Step 3 : If it has no Child - Load the Feature Form

        if (app.isParent()) {

            // 1. Pass a message to the Type Activity that this is a Child View for Plugload
            // 2. Side List -> Has Plugload Types
            // 3. View Pager -> Has only one Element the PlugLoad Child Elements

            val intent = Intent(activity, TypeActivity::class.java)

            intent.putExtra(PARCEL_AUDIT, auditModel)
            intent.putExtra(PARCEL_ZONE, zoneModel)
            intent.putExtra(PARCEL_TYPE, typeModel)
            intent.putExtra(PARCEL_POSITION, position)
            intent.putExtra("typeId", typeId as Int)

            context?.let {
                ActivityCompat.startActivity(it, intent, null)
            }

            app.setCounter(EAction.Push, typeModel)

        } else {

            setTypeModel(typeModel)
            val callbacks = activity as OnTypeSelectedListener
            callbacks.onTypeSelected(typeModel, position)

            Log.d(TAG, "Setting Up Shared View Model")
            Log.d(TAG, typeModel.toString())
            sharedViewModel.setType(typeModel)

            Timber.d("******************************************************************************")
            Timber.d("TYPE POSITION : $position")
            Timber.d("******************************************************************************")

        }

    }

    override fun onEditClick(view: View, typeModel: TypeModel) {
        showCreateZoneType(typeModel)
    }

    override fun onDeleteClick(view: View, typeModel: TypeModel) {
        typeListViewModel.deleteZoneType(typeModel)
    }

    override fun onTypeCreate(args: Bundle) {

        val typeTag = args.getString("typeTag")
        val type= args.getString("type")
        val subType: String? = args.getString("subType")

        Log.d(TAG, "Type Tag :: $typeTag")
        Log.d(TAG, "Type :: $type")
        Log.d(TAG, "Sub Type :: $subType")

        zoneModel?.let {zone ->
            if (zone.id != null) {
                typeCreateViewModel.createZoneType(
                        zone.id,
                        type,
                        subType,
                        typeTag,
                        zone.auditId
                )
            }
        }
    }

    override fun onTypeUpdate(args: Bundle, type: TypeModel, scopeName: String) {
        typeCreateViewModel.updateZoneType(type, scopeName)
    }

    private fun setupListeners() {
        typeCreateViewModel.result.observe(this, Observer {
            refreshViewModel()
        })
    }

    private fun setAuditModel(audit: AuditModel?) {
        Log.d(TAG, "Setting Audit Model -- ${audit.toString()}")
        this.auditModel = audit
    }

    fun setZoneModel(zone: ZoneModel?) {
        Log.d(TAG, "Setting Zone Model -- ${zone.toString()}")
        this.zoneModel = zone
        refreshViewModel()
    }

    private fun setTypeModel(type: TypeModel?) {
        Log.d(TAG, "Setting Type Model -- ${type.toString()}")
        this.typeModel = type
    }

    private fun setTypeId(typeId: Int?) {
        this.typeId = typeId
    }

    companion object {
        private fun getType(pagerIndex: Int) = EZoneType.get(pagerIndex)

        fun newInstance(type: Int, zone: ZoneModel, audit: AuditModel, position: Int): TypeListFragment {
            val fragment = TypeListFragment()

            fragment.arguments = Bundle().apply {
                this.putParcelable(PARCEL_ZONE, zone)
                this.putParcelable(PARCEL_AUDIT, audit)
                this.putInt(PARCEL_POSITION, position)
                this.putInt("typeId", type)
            }

            return fragment
        }

        private val app = App.instance
        private const val FRAG_DIALOG = "TypeDialogFragment"
        private const val TAG = "TypeListFragment"

        private const val PARCEL_AUDIT      = "EXTRA.AUDIT"
        private const val PARCEL_ZONE       = "EXTRA.ZONE"
        private const val PARCEL_TYPE       = "EXTRA.TYPE"
        private const val PARCEL_POSITION   = "EXTRA.POSITION.TYPE"

    }

}
