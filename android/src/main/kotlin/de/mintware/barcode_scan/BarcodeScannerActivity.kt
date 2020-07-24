package de.mintware.barcode_scan

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView

class BarcodeScannerActivity : Activity(), ZXingScannerView.ResultHandler {

    private lateinit var config: Protos.Configuration
    private var scannerView: ZXingScannerView? = null
    private var ivBack : ImageView? = null
    private var tvTitle : TextView? = null
    private var tvLight : TextView? = null

    companion object {
        const val TOGGLE_FLASH = 200
        const val CANCEL = 300
        const val EXTRA_CONFIG = "config"
        const val EXTRA_RESULT = "scan_result"
        const val EXTRA_ERROR_CODE = "error_code"

        private val formatMap: Map<Protos.BarcodeFormat, BarcodeFormat> = mapOf(
                Protos.BarcodeFormat.aztec to BarcodeFormat.AZTEC,
                Protos.BarcodeFormat.code39 to BarcodeFormat.CODE_39,
                Protos.BarcodeFormat.code93 to BarcodeFormat.CODE_93,
                Protos.BarcodeFormat.code128 to BarcodeFormat.CODE_128,
                Protos.BarcodeFormat.dataMatrix to BarcodeFormat.DATA_MATRIX,
                Protos.BarcodeFormat.ean8 to BarcodeFormat.EAN_8,
                Protos.BarcodeFormat.ean13 to BarcodeFormat.EAN_13,
                Protos.BarcodeFormat.interleaved2of5 to BarcodeFormat.ITF,
                Protos.BarcodeFormat.pdf417 to BarcodeFormat.PDF_417,
                Protos.BarcodeFormat.qr to BarcodeFormat.QR_CODE,
                Protos.BarcodeFormat.upce to BarcodeFormat.UPC_E
        )

    }

    // region Activity lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        config = Protos.Configuration.parseFrom(intent.extras!!.getByteArray(EXTRA_CONFIG))

        setupScannerView()
        scannerView?.setResultHandler(this)
    }

    private fun setupScannerView() {
        setContentView(R.layout.activity_scan)
        ivBack = findViewById<ImageView>(R.id.ivBack)
        tvTitle = findViewById<TextView>(R.id.tvTitle)
        tvLight = findViewById<TextView>(R.id.tvLight)
        scannerView = findViewById<ZXingAutofocusScannerView>(R.id.scannerView)

        ivBack?.setOnClickListener{
            finish()
        }

        val title = config.stringsMap["title"]
        if (!TextUtils.isEmpty(title)){
            tvTitle?.text = title
        }

        val flashEnable = config.stringsMap["flash_enable"]
        if (TextUtils.equals(flashEnable, "1")) {
            var buttonText = config.stringsMap["flash_on"]
            if (scannerView?.flash == true) {
                buttonText = config.stringsMap["flash_off"]
            }
            tvLight?.text = buttonText
            tvLight?.setOnClickListener {
                scannerView?.toggleFlash()
                var buttonText = config.stringsMap["flash_on"]
                if (scannerView?.flash == true) {
                    buttonText = config.stringsMap["flash_off"]
                }
                tvLight?.text = buttonText
            }
        }

        scannerView?.apply {
            setAutoFocus(config.android.useAutoFocus)
            val restrictedFormats = mapRestrictedBarcodeTypes()
            if (restrictedFormats.isNotEmpty()) {
                setFormats(restrictedFormats)
            }

            // this parameter will make your HUAWEI phone works great!
            setAspectTolerance(config.android.aspectTolerance.toFloat())
            if (config.autoEnableFlash) {
                flash = config.autoEnableFlash
//                invalidateOptionsMenu()
            }
        }

    }

    // region AppBar menu
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        var buttonText = config.stringsMap["flash_on"]
//        if (scannerView?.flash == true) {
//            buttonText = config.stringsMap["flash_off"]
//        }
//        val flashButton = menu.add(0, TOGGLE_FLASH, 0, buttonText)
//        flashButton.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
//
//        val cancelButton = menu.add(0, CANCEL, 0, config.stringsMap["cancel"])
//        cancelButton.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
//
//        return super.onCreateOptionsMenu(menu)
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        if (item.itemId == TOGGLE_FLASH) {
//            scannerView?.toggleFlash()
//            this.invalidateOptionsMenu()
//            return true
//        }
//        if (item.itemId == CANCEL) {
//            setResult(RESULT_CANCELED)
//            finish()
//            return true
//        }
//        return super.onOptionsItemSelected(item)
//    }

    override fun onPause() {
        super.onPause()
        scannerView?.stopCamera()
    }

    override fun onResume() {
        super.onResume()
        if (config.useCamera > -1) {
            scannerView?.startCamera(config.useCamera)
        } else {
            scannerView?.startCamera()
        }
    }
    // endregion

    override fun handleResult(result: Result?) {
        val intent = Intent()

        val builder = Protos.ScanResult.newBuilder()
        if (result == null) {

            builder.let {
                it.format = Protos.BarcodeFormat.unknown
                it.rawContent = "No data was scanned"
                it.type = Protos.ResultType.Error
            }
        } else {

            val format = (formatMap.filterValues { it == result.barcodeFormat }.keys.firstOrNull()
                    ?: Protos.BarcodeFormat.unknown)

            var formatNote = ""
            if (format == Protos.BarcodeFormat.unknown) {
                formatNote = result.barcodeFormat.toString()
            }

            builder.let {
                it.format = format
                it.formatNote = formatNote
                it.rawContent = result.text
                it.type = Protos.ResultType.Barcode
            }
        }
        val res = builder.build()
        intent.putExtra(EXTRA_RESULT, res.toByteArray())
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun mapRestrictedBarcodeTypes(): List<BarcodeFormat> {
        val types: MutableList<BarcodeFormat> = mutableListOf()

        this.config.restrictFormatList.filterNotNull().forEach {
            if (!formatMap.containsKey(it)) {
                print("Unrecognized")
                return@forEach
            }

            types.add(formatMap.getValue(it))
        }

        return types
    }
}
