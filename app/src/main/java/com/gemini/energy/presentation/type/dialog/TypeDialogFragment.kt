package com.gemini.energy.presentation.type.dialog

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.gemini.energy.R
import com.gemini.energy.presentation.type.list.model.TypeModel
import com.gemini.energy.presentation.util.EApplianceType
import com.gemini.energy.presentation.util.ERefrigerationType
import com.gemini.energy.presentation.util.ELightingType
import com.gemini.energy.presentation.util.EZoneType
import com.mobsandgeeks.saripaar.ValidationError
import com.mobsandgeeks.saripaar.Validator
import com.mobsandgeeks.saripaar.annotation.NotEmpty


class TypeDialogFragment : DialogFragment(), Validator.ValidationListener {

    @NotEmpty
    private lateinit var scopeName: EditText

    @NotEmpty
    private lateinit var scopeType: String

    @NotEmpty
    private lateinit var scopeSubType: String

    private lateinit var scopeSpinner: Spinner
    private lateinit var validator: Validator

    private var typeId: Int? = null
    var type: TypeModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        validator = Validator(this)
        validator.setValidationListener(this)

        typeId = arguments?.getInt("typeId")
        scopeType = getType(typeId ?: 0)!!.value
        scopeSubType = "none"

        type?.let {
            scopeType = it.type!!
            scopeSubType = it.subType!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_type_parent_dialog, container, false)
        dialog.setTitle(R.string.menu_create_audit_scope_parent)

        view.findViewById<Button>(R.id.btn_cancel_scope).setOnClickListener { dismiss() }
        view.findViewById<Button>(R.id.btn_save_scope).setOnClickListener { validator.validate() }

        scopeName = view.findViewById(R.id.edt_create_audit_scope_parent_name)
        scopeSpinner = view.findViewById(R.id.spin_audit_scope) as Spinner

        val scopeOptions  = getScopeOptions()
        scopeSpinner.adapter = ArrayAdapter(activity, R.layout.support_simple_spinner_dropdown_item, scopeOptions)

        type?.let {
            scopeName.setText(it.name)
            scopeSpinner.setSelection(getScopeOptions().indexOf(it.subType))
        }

        scopeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                Toast.makeText(activity, getString(R.string.selected_item) + " " + scopeOptions[position], Toast.LENGTH_SHORT).show()
                scopeSubType = scopeOptions[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        return view
    }

    override fun onValidationFailed(errors: MutableList<ValidationError>?) {
        Toast.makeText(activity, "Audit Entity Create - Validation Failed", Toast.LENGTH_SHORT).show()
    }

    override fun onValidationSucceeded() {
        val args = Bundle().apply {
            this.putString("typeTag", scopeName.text.toString())
            this.putString("type", scopeType)
            this.putString("subType", scopeSubType)
        }

        if (parentFragment != null) {
            val callbacks = parentFragment as OnTypeCreateListener?
            if (type == null) { callbacks?.onTypeCreate(args) }
            else { callbacks?.onTypeUpdate(args, type!!, scopeName.text.toString()) }
        }

        dismiss()
    }

    /*
    * Options depend on the Type | Parent or Child | Zone
    * */
    private fun getScopeOptions(): List<String> {

        return (when(getType(typeId!!)) {

            EZoneType.Lighting -> ELightingType.options()
            EZoneType.Plugload -> EApplianceType.options()
            EZoneType.Refrigeration -> ERefrigerationType.options()
            else -> listOf()

        })
    }

    private fun getType(pagerIndex: Int) = EZoneType.get(pagerIndex)

    companion object {

        fun newInstance(typeId: Int): TypeDialogFragment {
            val fragment = TypeDialogFragment()
            fragment.arguments = Bundle().apply {
                this.putInt("typeId", typeId)
            }

            return fragment
        }

        private const val TAG = "TypeDialogFragment"
    }

    interface OnTypeCreateListener {
        fun onTypeCreate(args: Bundle)
        fun onTypeUpdate(args: Bundle, type: TypeModel, scopeName: String)
    }

}