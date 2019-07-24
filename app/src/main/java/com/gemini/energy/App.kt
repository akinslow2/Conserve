package com.gemini.energy

import android.content.Context
import android.support.multidex.MultiDex
import com.crashlytics.android.Crashlytics
import com.gemini.energy.internal.injection.DaggerApplication
import com.gemini.energy.presentation.audit.DropBox
import com.gemini.energy.presentation.util.EAction
import com.gemini.energy.presentation.type.list.model.TypeModel
import io.fabric.sdk.android.Fabric
import io.reactivex.plugins.RxJavaPlugins
import timber.log.Timber


class App : DaggerApplication() {

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        Fabric.with(this, Crashlytics())
        Timber.plant(Timber.DebugTree())
        RxJavaPlugins.setErrorHandler({Timber.e(it)})

        if (DropBox.hasToken()) DropBox.captureAuthToken()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    // TODO: Maybe we would want to initialize Collection here
    // test to see if it updates

    /**
     * Type Counter for Parent - Child
     * */
    private var counter: ArrayList<TypeModel> = arrayListOf<TypeModel>()

    fun getCount() = counter.size
    fun setCounter(action: EAction, type: TypeModel? = null) {
        when(action) {
            EAction.Push -> counter.add(type!!)
            EAction.Pop -> counter.removeAt(counter.size - 1)
        }
    }
    fun isParent(): Boolean = counter.size == 0
    fun isChild(): Boolean = !(isParent())
}