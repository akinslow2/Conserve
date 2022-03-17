package com.gemini.energy.service.device

import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.service.IComputable
import io.reactivex.Flowable
import io.reactivex.Observable

class General : IComputable {

    override fun compute(): Observable<Computable<*>> {
        return Observable.empty()
    }

}
