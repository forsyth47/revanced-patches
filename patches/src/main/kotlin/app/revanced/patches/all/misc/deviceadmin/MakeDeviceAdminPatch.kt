package app.revanced.patches.all.misc.deviceadmin

import app.revanced.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.io.File

@Suppress("unused")
val makeDeviceAdminPatch = resourcePatch(
    name = "Make Device Admin App",
    description = "Modifies an app to request Device Admin privileges by adding a DeviceAdminReceiver and necessary XML.",
    use = true,
) {
    execute {
        val resXmlDirectory = get("res/xml")
        val resValuesDirectory = get("res/values")

        // Fetch the package name and determine fallback name
        val smaliDirectory = get("smali")
        var fallbackAppName: String? = null

        val packageName = document("AndroidManifest.xml").use { document ->
            val manifestElement = document.documentElement
            val pkgName = manifestElement.getAttribute("package")
            fallbackAppName = pkgName.substringAfterLast('.')
            pkgName
        }

        // Check if @string/app_name exists
        val appName = File(resValuesDirectory, "strings.xml").let { stringsFile ->
            if (stringsFile.exists()) {
                val stringsContent = stringsFile.readText()
                if (stringsContent.contains("name=\"app_name\"")) {
                    "@string/app_name"
                } else {
                    fallbackAppName
                }
            } else {
                fallbackAppName
            }
        } ?: fallbackAppName

        // Modify AndroidManifest.xml to include DeviceAdminReceiver
        document("AndroidManifest.xml").use { document ->
            val applicationNode = document.getElementsByTagName("application").item(0) as Element
            
            // Add the DeviceAdminReceiver inside the <application> tag
            val receiverNode = document.createElement("receiver")
            receiverNode.setAttribute("android:name", ".MyDeviceAdminReceiver")
            receiverNode.setAttribute("android:label", appName)
            receiverNode.setAttribute("android:permission", "android.permission.BIND_DEVICE_ADMIN")

            // Add meta-data tag
            val metaDataNode = document.createElement("meta-data")
            metaDataNode.setAttribute("android:name", "android.app.device_admin")
            metaDataNode.setAttribute("android:resource", "@xml/device_admin")

            // Add intent-filter tag
            val intentFilterNode = document.createElement("intent-filter")
            val actionNode = document.createElement("action")
            actionNode.setAttribute("android:name", "android.app.action.DEVICE_ADMIN_ENABLED")
            intentFilterNode.appendChild(actionNode)

            receiverNode.appendChild(metaDataNode)
            receiverNode.appendChild(intentFilterNode)
            applicationNode.appendChild(receiverNode)
        }

        // Create device_admin.xml in res/xml directory
        File(resXmlDirectory, "device_admin.xml").apply {
            writeText(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <device-admin xmlns:android="http://schemas.android.com/apk/res/android">
                        <uses-policies>
                            <limit-password />
                            <watch-login />
                        </uses-policies>
                    </device-admin>
                """.trimIndent(),
            )
        }

        // Create the smali file for DeviceAdminReceiver
        val packagePath = packageName.replace('.', '/')
        val smaliFile = File(smaliDirectory, "$packagePath/MyDeviceAdminReceiver.smali")

        smaliFile.apply {
            parentFile.mkdirs()
            writeText(
                """
                    .class public L${packagePath}/MyDeviceAdminReceiver;
                    .super Landroid/app/admin/DeviceAdminReceiver;

                    .method public constructor <init>()V
                        .locals 0
                        .prologue
                        invoke-direct {p0}, Landroid/app/admin/DeviceAdminReceiver;-><init>()V
                        return-void
                    .end method
                """.trimIndent(),
            )
        }
    }
}