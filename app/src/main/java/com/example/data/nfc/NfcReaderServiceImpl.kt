package com.example.data.nfc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import com.example.domain.model.NfcTagData
import com.example.domain.repository.INfcReaderService
import com.example.service.NfcReaderService
import kotlinx.coroutines.flow.StateFlow

// Facade delegating to the unified NfcReaderService
class NfcReaderServiceImpl(
    context: Context
) : INfcReaderService, NfcAdapter.ReaderCallback {

    private val delegate = NfcReaderService(context)

    override val tagDiscoveryFlow: StateFlow<NfcTagData?> = delegate.tagDiscoveryFlow
    override val nfcScanState: StateFlow<com.example.domain.model.NfcScanState> = delegate.nfcScanState
    override val isNfcAvailable: Boolean get() = delegate.isNfcAvailable
    override val isNfcEnabled: Boolean get() = delegate.isNfcEnabled

    override fun resetScanState() = delegate.resetScanState()
    override fun setScanState(state: com.example.domain.model.NfcScanState) = delegate.setScanState(state)

    override fun enableReaderMode(activity: Activity) = delegate.enableReaderMode(activity)
    override fun disableReaderMode(activity: Activity) = delegate.disableReaderMode(activity)
    override fun processIntent(intent: Intent): NfcTagData? = delegate.processIntent(intent)
    override fun parseTag(tag: Tag): NfcTagData = delegate.parseTag(tag)
    override fun verifyEktpSignature(tagData: NfcTagData): Boolean = delegate.verifyEktpSignature(tagData)
    override fun onTagDiscovered(tag: Tag) = delegate.onTagDiscovered(tag)
}
