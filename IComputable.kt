package com.gemini.energy.service

import com.gemini.energy.domain.entity.Computable
import io.reactivex.Observable

interface IComputable {
    fun compute(): Observable<Computable<*>>
}