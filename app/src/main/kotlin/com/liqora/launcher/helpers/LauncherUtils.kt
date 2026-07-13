package com.liqora.launcher.helpers

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.app.Activity
import com.liqora.launcher.compose.launcher.LauncherConfig
import com.liqora.launcher.compose.launcher.LauncherItem

object LauncherUtils {

    /**
     * True if this app is currently set as the device's default Home/Launcher app.
     */
    fun isDefaultLauncher(context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfo = context.packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            resolveInfo?.activityInfo?.packageName == context.packageName
        } catch (e: Exception) {
            false
        }
    }

    /**
     * On Android 10+ (API 29+), returns an Intent that launches the system's
     * "Set as Home app" role picker for RoleManager.ROLE_HOME, or null if the
     * role isn't available/already held/API too low — in which case callers
     * should fall back to [defaultLauncherFallbackIntent].
     */
    fun getDefaultLauncherRoleIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Fallback for Android 9 and below (or when the Role API path isn't usable):
     * opens the system's "Home app" chooser settings screen.
     */
    fun defaultLauncherFallbackIntent(): Intent {
        return Intent(Settings.ACTION_HOME_SETTINGS)
    }

    fun launchApp(context: Context, packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                context.startActivity(intent)
                if (context is Activity) {
                    context.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            }
        } catch (e: Exception) {
            // Ignore failures
        }
    }

    fun loadDrawableFromUri(context: Context, uriString: String): Drawable? {
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use {
                Drawable.createFromStream(it, uriString)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resolves the four "essential" apps (Phone, Messages, Chrome, Camera) to
     * concrete installed package names for the first-launch default Home Screen
     * + Dock setup. Each lookup prefers the exact app when installed and falls
     * back to whatever the system currently treats as the default handler for
     * that role (e.g. the device's default browser/SMS/dialer/camera app) so
     * the layout is never left pointing at something that isn't installed.
     *
     * Returns package names in a stable order: Phone, Messages, Chrome, Camera.
     * Apps that can't be resolved at all (rare — e.g. a tablet with no dialer)
     * are simply omitted rather than inserting a broken shortcut.
     */
    fun resolveDefaultDockApps(context: Context): List<String> {
        val pm = context.packageManager

        fun installed(pkg: String): String? = try {
            pm.getPackageInfo(pkg, 0)
            pkg
        } catch (e: Exception) {
            null
        }

        fun defaultHandlerFor(intent: Intent): String? = try {
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
        } catch (e: Exception) {
            null
        }

        val phone = installed("com.google.android.dialer")
            ?: defaultHandlerFor(Intent(Intent.ACTION_DIAL))
            ?: defaultHandlerFor(Intent(Intent.ACTION_CALL_BUTTON))

        val messages = installed("com.google.android.apps.messaging")
            ?: defaultHandlerFor(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")))

        val chrome = installed("com.android.chrome")
            ?: defaultHandlerFor(Intent(Intent.ACTION_VIEW, Uri.parse("https://")))

        val camera = installed("com.google.android.GoogleCamera")
            ?: defaultHandlerFor(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE))

        return listOfNotNull(phone, messages, chrome, camera).distinct()
    }

    /**
     * True only when this is genuinely the first run: no saved schematic and
     * the current in-memory config is still the untouched empty default.
     * Used to gate the one-time Home Screen + Dock defaults so a user who
     * deliberately empties their layout later never has it silently restored.
     */
    fun isFreshInstall(config: LauncherConfig): Boolean {
        return !config.defaultLayoutApplied && config.items.isEmpty() && config.dockApps.isEmpty()
    }

    fun findEmptyCell(config: LauncherConfig): Pair<Int, Int> {
        val occupied = config.items.flatMap { item ->
            (0 until item.spanX).flatMap { dx ->
                (0 until item.spanY).map { dy ->
                    (item.gridX + dx) to (item.gridY + dy)
                }
            }
        }.toSet()

        for (y in 0 until config.gridRows) {
            for (x in 0 until config.gridColumns) {
                if ((x to y) !in occupied) return x to y
            }
        }
        return 0 to 0
    }
}
