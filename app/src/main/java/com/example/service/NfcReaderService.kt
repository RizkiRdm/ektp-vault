package com.example.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Bundle
import com.example.domain.model.NdefParsedRecord
import com.example.domain.model.NfcTagData
import com.example.domain.repository.INfcReaderService
import java.io.IOException
import java.nio.charset.Charset
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// NFC reader service implementing ReaderCallback for e-KTP contactless smart card detection
class NfcReaderService(
    private val context: Context
) : INfcReaderService, NfcAdapter.ReaderCallback {

    private val nfcAdapter: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(context) }
    private val _tagDiscoveryFlow = MutableStateFlow<NfcTagData?>(null)
    override val tagDiscoveryFlow: StateFlow<NfcTagData?> = _tagDiscoveryFlow.asStateFlow()

    private var onTagDiscoveredListener: ((NfcTagData) -> Unit)? = null

    override val isNfcAvailable: Boolean
        get() = nfcAdapter != null

    override val isNfcEnabled: Boolean
        get() = nfcAdapter?.isEnabled == true

    fun setOnTagDiscoveredListener(listener: ((NfcTagData) -> Unit)?) {
        this.onTagDiscoveredListener = listener
    }

    override fun enableReaderMode(activity: Activity) {
        val adapter = nfcAdapter ?: return
        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

        val options = Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
        }

        // Pass this as the NfcAdapter.ReaderCallback
        adapter.enableReaderMode(activity, this, flags, options)
    }

    override fun disableReaderMode(activity: Activity) {
        nfcAdapter?.disableReaderMode(activity)
    }

    // Callback invoked on background thread when NFC hardware detects a tag
    override fun onTagDiscovered(tag: Tag) {
        val tagData = parseTag(tag)
        _tagDiscoveryFlow.value = tagData
        onTagDiscoveredListener?.invoke(tagData)
    }

    override fun processIntent(intent: Intent): NfcTagData? {
        val action = intent.action ?: return null
        if (action != NfcAdapter.ACTION_TAG_DISCOVERED &&
            action != NfcAdapter.ACTION_TECH_DISCOVERED &&
            action != NfcAdapter.ACTION_NDEF_DISCOVERED
        ) {
            return null
        }

        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        } ?: return null

        val parsed = parseTag(tag)
        _tagDiscoveryFlow.value = parsed
        onTagDiscoveredListener?.invoke(parsed)
        return parsed
    }

    // Securely extract identification metadata and e-KTP attributes from physical tag
    override fun parseTag(tag: Tag): NfcTagData {
        val rawUid = tag.id?.copyOf() ?: ByteArray(0)
        val hexUid = formatHexUid(rawUid)
        val techList = tag.techList?.map { it.substringAfterLast(".") } ?: emptyList()

        val isIsoDep = techList.contains("IsoDep")
        var historicalBytesHex: String? = null

        // Inspect ISO 14443-4 IsoDep smart card parameters (e-KTP hardware signature)
        val isoDep = IsoDep.get(tag)
        if (isoDep != null) {
            try {
                isoDep.timeout = 1500
                isoDep.connect()
                val hist = isoDep.historicalBytes
                val hiLayer = isoDep.hiLayerResponse
                if (hist != null && hist.isNotEmpty()) {
                    historicalBytesHex = hist.joinToString(" ") { "%02X".format(it) }
                } else if (hiLayer != null && hiLayer.isNotEmpty()) {
                    historicalBytesHex = hiLayer.joinToString(" ") { "%02X".format(it) }
                }
            } catch (_: IOException) {
                // Ignore transient contactless disconnect
            } catch (_: Exception) {
                // Safeguard against platform-specific NFC driver exceptions
            } finally {
                try {
                    if (isoDep.isConnected) {
                        isoDep.close()
                    }
                } catch (_: IOException) {
                    // Safe cleanup
                }
            }
        }

        val ndefRecords = parseNdefMessages(tag)
        val isEktpCompatible = verifyEktpSignatureInternal(isIsoDep, techList, rawUid)

        return NfcTagData(
            rawUid = rawUid,
            hexUid = hexUid,
            techList = techList,
            isIsoDep = isIsoDep,
            historicalBytesHex = historicalBytesHex,
            ndefRecords = ndefRecords,
            isEktpCompatible = isEktpCompatible
        )
    }

    private fun parseNdefMessages(tag: Tag): List<NdefParsedRecord> {
        val records = mutableListOf<NdefParsedRecord>()
        val ndef = Ndef.get(tag) ?: return records

        try {
            ndef.connect()
            val message: NdefMessage? = ndef.cachedNdefMessage ?: ndef.ndefMessage
            message?.records?.forEach { record ->
                records.add(convertNdefRecord(record))
            }
        } catch (_: Exception) {
            // NDEF payload reading is optional for identity smart cards
        } finally {
            try {
                if (ndef.isConnected) {
                    ndef.close()
                }
            } catch (_: Exception) {
                // Safe cleanup
            }
        }
        return records
    }

    private fun convertNdefRecord(record: NdefRecord): NdefParsedRecord {
        val tnf = record.tnf
        val type = String(record.type, Charset.forName("US-ASCII"))
        val id = if (record.id.isNotEmpty()) String(record.id, Charset.forName("UTF-8")) else null
        var textPayload: String? = null
        var uriPayload: String? = null
        var mime: String? = null

        when (tnf) {
            NdefRecord.TNF_WELL_KNOWN -> {
                if (record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                    textPayload = parseTextRecordPayload(record.payload)
                } else if (record.type.contentEquals(NdefRecord.RTD_URI)) {
                    uriPayload = parseUriRecordPayload(record.payload)
                }
            }
            NdefRecord.TNF_MIME_MEDIA -> {
                mime = type
                textPayload = String(record.payload, Charset.forName("UTF-8"))
            }
            else -> {
                textPayload = record.payload.joinToString("") { "%02X".format(it) }
            }
        }

        return NdefParsedRecord(
            tnf = tnf,
            type = type,
            id = id,
            payloadText = textPayload,
            payloadUri = uriPayload,
            mimeType = mime
        )
    }

    private fun parseTextRecordPayload(payload: ByteArray): String {
        if (payload.isEmpty()) return ""
        val statusByte = payload[0].toInt()
        val isUtf16 = (statusByte and 0x80) != 0
        val langCodeLength = statusByte and 0x3F
        val charset = if (isUtf16) Charset.forName("UTF-16") else Charset.forName("UTF-8")
        val textLength = payload.size - 1 - langCodeLength
        return if (textLength > 0) String(payload, 1 + langCodeLength, textLength, charset) else ""
    }

    private fun parseUriRecordPayload(payload: ByteArray): String {
        if (payload.isEmpty()) return ""
        val prefixCode = payload[0].toInt()
        val prefixes = arrayOf(
            "", "http://www.", "https://www.", "http://", "https://", "tel:", "mailto:",
            "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://", "sftp://", "smb://", "nfs://",
            "ftp://", "dav://", "news:", "telnet://", "imap:", "rtsp://", "urn:", "pop:",
            "sip:", "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://", "tcpobex://",
            "irdaobex://", "file://", "urn:epc:id:", "urn:epc:tag:", "urn:epc:pat:", "urn:epc:raw:",
            "urn:epc:", "urn:nfc:"
        )
        val prefix = if (prefixCode in prefixes.indices) prefixes[prefixCode] else ""
        val uriBody = String(payload, 1, payload.size - 1, Charset.forName("UTF-8"))
        return "$prefix$uriBody"
    }

    override fun verifyEktpSignature(tagData: NfcTagData): Boolean {
        return verifyEktpSignatureInternal(tagData.isIsoDep, tagData.techList, tagData.rawUid)
    }

    // Verify e-KTP compliance: ISO 14443 Type A/B smart card with IsoDep framing
    private fun verifyEktpSignatureInternal(isIsoDep: Boolean, techList: List<String>, uid: ByteArray): Boolean {
        val hasSmartCardTech = isIsoDep || techList.contains("IsoDep") || techList.contains("NfcA") || techList.contains("NfcB")
        val validUidLength = uid.size == 4 || uid.size == 7 || uid.size == 10
        return hasSmartCardTech && validUidLength
    }

    private fun formatHexUid(bytes: ByteArray): String {
        return bytes.joinToString(":") { "%02X".format(it) }
    }

    fun computeSha256Uid(rawUid: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(rawUid)
        return hash.joinToString("") { "%02x".format(it) }
    }
}
