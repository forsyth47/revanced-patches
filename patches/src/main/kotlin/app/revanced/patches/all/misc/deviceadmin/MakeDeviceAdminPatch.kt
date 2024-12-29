package app.revanced.patches.all.misc.deviceadmin

import app.revanced.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.io.File

@Suppress("unused")
val makeDeviceAdminPatch = resourcePatch(
    name = "Make Device Admin App",
    description = "Modifies an app to request Device Admin privileges by adding a DeviceAdminReceiver and necessary XML.",
    use = false,
) {
    execute {
        val resXmlDirectory = get("res/xml")
        
        // Modify AndroidManifest.xml to include DeviceAdminReceiver
        document("AndroidManifest.xml").use { document ->
            val applicationNode = document.getElementsByTagName("application").item(0) as Element
            
            // Add the DeviceAdminReceiver inside the <application> tag
            val receiverNode = document.createElement("receiver")
            receiverNode.setAttribute("android:name", ".MyDeviceAdminReceiver")
            receiverNode.setAttribute("android:label", "@string/app_name")
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

        // Fetch package name from AndroidManifest.xml and create the smali path
        val smaliDirectory = get("smali")
        document("AndroidManifest.xml").use { document ->
            val manifestElement = document.documentElement // Get the root element
            val packageName = manifestElement.getAttribute("package")
            
            // Convert the package name into a path for smali
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
}
