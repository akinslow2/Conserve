package com.gemini.energy.service

import io.reactivex.Observable

interface IUploader {

    fun upload(outgoingRows: List<OutgoingRows>): Observable<Unit>

}