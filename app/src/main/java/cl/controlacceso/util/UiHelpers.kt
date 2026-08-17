package cl.controlacceso.util

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cl.controlacceso.R

object UiHelpers {

    private const val BOTTOM_CLEARANCE_PX = 5

    fun downloadsDocumentUri(): Uri {
        return DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            "primary:${Environment.DIRECTORY_DOWNLOADS}"
        )
    }

    fun applyOpenDocumentDefaults(intent: Intent) {
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, downloadsDocumentUri())
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun liftContentAboveNavBar(root: View) {
        val original = (root.getTag(R.id.original_bottom_padding) as? Int) ?: root.paddingBottom.also {
            root.setTag(R.id.original_bottom_padding, it)
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                original + navBottom + BOTTOM_CLEARANCE_PX
            )
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }
}
