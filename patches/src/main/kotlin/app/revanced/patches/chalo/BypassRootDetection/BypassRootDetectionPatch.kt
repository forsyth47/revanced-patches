package app.revanced.patches.all.misc.disableRootDetection

import app.revanced.patcher.patch.resourcePatch
import java.io.File

@Suppress("unused")
val disableRootDetectionPatch = resourcePatch(
    name = "Disable Root Detection",
    description = "Modifies the isRooted() method in the Smali file to always return false.",
    use = true,
) {
    compatibleWith("com.instagram.android")
    
    execute {
        val smaliDirectory = get("smali_classes6")
        val targetFilePath = "$smaliDirectory/com/google/firebase/crashlytics/internal/common/CommonUtils.smali"
        val targetMethodName = ".method public static isRooted(Landroid/content/Context;)Z"

        // Load the Smali file
        val smaliFile = File(targetFilePath)
        if (!smaliFile.exists()) {
            throw Exception("The specified Smali file does not exist: $targetFilePath")
        }

        val smaliContent = smaliFile.readText()

        // Locate the isRooted method
        val startIndex = smaliContent.indexOf(targetMethodName)
        if (startIndex == -1) {
            throw IllegalStateException("The isRooted() method was not found in the specified Smali file.")
        }

        // Extract everything before and after the method to preserve the file's structure
        val methodEndIndex = smaliContent.indexOf(".end method", startIndex) + ".end method".length
        val beforeMethod = smaliContent.substring(0, startIndex)
        val afterMethod = smaliContent.substring(methodEndIndex)

        // Replace the method content with the modified code
        val modifiedMethod = """
            .method public static isRooted(Landroid/content/Context;)Z
                .registers 1

                const/4 p0, 0x0

                return p0
            .end method
        """.trimIndent()

        // Save the modified content back to the Smali file
        smaliFile.writeText(beforeMethod + modifiedMethod + afterMethod)
    }
}