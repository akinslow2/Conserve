package com.gemini.energy.presentation.audit.dialog

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import com.gemini.energy.R
import com.gemini.energy.domain.entity.Audit
import com.gemini.energy.domain.interactor.AuditGetUseCase
import com.gemini.energy.domain.interactor.AuditSaveUseCase
import com.gemini.energy.domain.interactor.AuditUpdateUseCase
import com.gemini.energy.internal.util.BaseAndroidViewModel
import com.gemini.energy.internal.util.SingleLiveData
import com.gemini.energy.presentation.audit.list.model.AuditModel
import com.gemini.energy.presentation.util.Utils
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import timber.log.Timber
import java.util.*

class AuditCreateViewModel(context: Context,
                           private val auditSaveUseCase: AuditSaveUseCase,
                           private val auditGetUseCase: AuditGetUseCase,
                           private val auditUpdateCaseCase: AuditUpdateUseCase) :
        BaseAndroidViewModel(context.applicationContext as Application) {

    private val _result = SingleLiveData<Boolean>()
    val result = _result

    private val _error = SingleLiveData<String>()
    val error = _error

    fun createAudit(tag: String) {
        val date = Date()
        addDisposable(save(Audit(Utils.intNow(), tag, -1, "", date, date)))
    }

    @SuppressLint("CheckResult")
    fun updateAudit(auditModel: AuditModel, auditTag: String) {
        auditGetUseCase.execute(auditModel.id)
                .subscribe {
                    it.name = auditTag
                    it.updatedAt = Date()
                    // it.usn = -1 ToDo - Need to implement this later
                    update(it)
                }
    }

    private fun save(audit: Audit): Disposable {
        return auditSaveUseCase.execute(audit)
                .subscribeWith(object : DisposableObserver<Unit>() {

                    override fun onNext(t: Unit) {
                        Log.d(TAG, "ON-NEXT !!! \\m/")
                        result.value = true
                    }

                    override fun onError(e: Throwable) {
                        error.value = e.localizedMessage ?: e.message ?:
                                context.getString(R.string.unknown_error)
                    }

                    override fun onComplete() {

                        Timber.d("*** YOHO YOHO YOHO YOHO YOHO ***")
                        Timber.d("<< AUDIT CREATE - ON COMPLETE >>")

                        //ToDo -- Update the Collection from here

                    }
                })
    }

    private fun update(audit: Audit): Disposable? {
        return auditUpdateCaseCase.execute(audit)
                .subscribeWith(object : DisposableObserver<Unit>() {
                    override fun onNext(t: Unit) {
                        Timber.d("ON-NEXT !!! \\m/ -- [[ Audit Update ]]")
                        result.value = true
                    }
                    override fun onError(e: Throwable) { e.printStackTrace() }
                    override fun onComplete() {

                        Timber.d("*** YOHO YOHO YOHO YOHO YOHO ***")
                        Timber.d("<< AUDIT UPDATE - ON COMPLETE >>")

                        //ToDo -- Update the Collection from here

                    }
                })
    }

    companion object {
        private const val TAG = "AuditCreateViewModel"
    }
}