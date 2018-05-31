package com.gemini.energy.presentation.list.adapter

import android.databinding.BindingAdapter
import android.support.v7.widget.RecyclerView
import com.gemini.energy.presentation.list.model.AuditModel

object ViewBindingAdapter {

    @JvmStatic
    @BindingAdapter("auditList", "auditListCallback", requireAll = false)
    fun setAuditListAdapter(recyclerView: RecyclerView, auditList: List<AuditModel>?,
                        auditListCallback: AuditListAdapter.Callbacks?) {

        auditList?.let {
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = AuditListAdapter(it, auditListCallback)
        }

    }
}