package de.mintware.barcode_scan

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.ImageView
import android.widget.TextView
import me.dm7.barcodescanner.zbar.BarcodeFormat
import me.dm7.barcodescanner.zbar.Result
import me.dm7.barcodescanner.zbar.ZBarScannerView
import java.util.ArrayList

class BarCodeScanPerfectActivity : Activity(), ZBarScannerView.ResultHandler {

    private lateinit var config: Protos.Configuration
    private var scannerView: ZBarScannerView? = null
    private var ivBack : ImageView? = null
    private var tvTitle : TextView? = null
    private var tvLight : TextView? = null

    companion object {
        const val TOGGLE_FLASH = 200
        const val CANCEL = 300
        const val EXTRA_CONFIG = "config"
        const val EXTRA_RESULT = "scan_result"
        const val EXTRA_ERROR_CODE = "error_code"

    }

    // region Activity lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        config = Protos.Configuration.parseFrom(intent.extras!!.getByteArray(EXTRA_CONFIG))

        setupScannerView()
        scannerView?.setResultHandler(this)
    }

    private fun setupScannerView() {
        setContentView(R.layout.activity_scan_perfect)
        ivBack = findViewById<ImageView>(R.id.ivBack)
        tvTitle = findViewById<TextView>(R.id.tvTitle)
        tvLight = findViewById<TextView>(R.id.tvLight)
        scannerView = findViewById<ZBarScannerView>(R.id.scannerView)

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
            val formats: MutableList<BarcodeFormat> = ArrayList()
            val mSelectedIndices = ArrayList<Int>()
            for (i in BarcodeFormat.ALL_FORMATS.indices) {
                mSelectedIndices.add(i)
            }
            for (index in mSelectedIndices) {
                formats.add(BarcodeFormat.ALL_FORMATS[index])
            }
            setFormats(formats)

            // this parameter will make your HUAWEI phone works great!
            setAspectTolerance(config.android.aspectTolerance.toFloat())
            if (config.autoEnableFlash) {
                flash = config.autoEnableFlash
            }
        }

    }

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

//            val format = (formatMap.filterValues { it == result.barcodeFormat }.keys.firstOrNull()
//                    ?: Protos.BarcodeFormat.unknown)
//
//            var formatNote = ""
//            if (format == Protos.BarcodeFormat.unknown) {
//                formatNote = result.barcodeFormat.toString()
//            }

            builder.let {
//                it.format = format
//                it.formatNote = formatNote
                it.rawContent = result.contents
                it.type = Protos.ResultType.Barcode
            }
        }
        val res = builder.build()
        intent.putExtra(EXTRA_RESULT, res.toByteArray())
        setResult(RESULT_OK, intent)
        finish()
    }

}
