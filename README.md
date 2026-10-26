# Android

[GitHub repo](https://github.com/reteno-com/reteno-mobile-android-sdk)

[You can find full documentation here](https://docs.reteno.com/reference/android-sdk-setup)

The Reteno Android SDK for Mobile Customer Engagement and Analytics solutions.

---

## Overview

`Reteno` is a lightweight SDK for Android that helps mobile teams integrate Reteno into their mobile apps. The server-side library makes it easy to call the `Reteno API`

##### The SDK supports

- Native Android applications written in **Java**/**Kotlin**

- Android 8.0 or later (minSdk = 26)

### Getting started with Reteno SDK for Android

1. Add mavenCentral repository in your project level `build.gradle`:

```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  ...
}
```

2. Add `reteno` and `firebase` dependencies in application level `build.gradle`:

```groovy
dependencies {
  implementation 'com.reteno:fcm:(latest_version_here)'
  ...
  implementation "com.google.firebase:firebase-messaging:23.1.0"
  implementation "com.google.firebase:firebase-messaging-ktx:23.1.0"
}
```

| **Library**                     | **Description**                                                       |
| ------------------------------- | --------------------------------------------------------------------- |
| com.reteno:fcm                  | FCM enables push notifications through SDK and all core functionality |
| firebase:firebase-messaging     | Firebase cloud messaging                                              |
| firebase:firebase-messaging-ktx | Firebase cloud messaging Kotlin extensions                            |

###### License​ :

`Reteno Android SDK` is released under the MIT license. See [LICENSE](LICENSE) for details.

## 

## Setting up SDK

Follow our setup guide to integrate the Reteno SDK with your app.

### Step1: Enable androidx in your gradle.properties file

```groovy
android.useAndroidX=true
```

### Step2: Determine Compiler for Java Code

Add `com.reteno:fcm` and `firebase` dependencies in build.gradle

> Note:
>
> Java 1.8 compiler is required. In app level `build.gradle`:

```groovy
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
```

### Step3: Initialize the SDK with your access key in your Application class.

> Note:
>
> To set up SDK you need an SDK_ACCESS_KEY, visit [Managing Mobile SDK Access Keys](https://docs.reteno.com/reference/managing-mobile-sdk-access-keys) to get it.

The SDK creates its instance automatically when your app starts. Finish the initialization in `Application.onCreate()` by passing a `RetenoConfig` with your access key. You may need to create a new class that extends `Application` on this step; don't forget to point your manifest to it.

Keep the access key out of source control: for example, read it from `local.properties` or your CI secrets into a `BuildConfig` field instead of writing it into the code.

```kotlin
package [com.YOUR_PACKAGE]

import android.app.Application
import com.reteno.core.Reteno
import com.reteno.core.RetenoConfig

class CustomApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Reteno.initWithConfig(
            RetenoConfig.Builder()
                .accessKey(BuildConfig.RETENO_ACCESS_KEY)
                .build()
        )
    }
}
```

For example, in your app level `build.gradle`, with `reteno.access-key=...` in your git-ignored `local.properties`:

```groovy
android {
    defaultConfig {
        def localProperties = new Properties()
        localProperties.load(rootProject.file('local.properties').newDataInputStream())
        buildConfigField "String", "RETENO_ACCESS_KEY", "\"${localProperties['reteno.access-key']}\""
    }
}
```

**Manifest.xml**

```xml
<application
        android:name=".CustomApplication"
        ...
        >
...
</application>
```

> Note:
>
> `RetenoImpl(application, accessKey)` and the `RetenoApplication` interface are deprecated. Use `Reteno.initWithConfig(...)` and `Reteno.instance` instead.

### Step4: Use SDK via `Reteno.instance`.

Access the SDK anywhere in your application through `Reteno.instance`. E.g. in Activity:

```kotlin
Reteno.instance.logScreenView("Home")
```

### Step5: Handle Runtime Permissions.

Since Android 13 was released you have to make sure you are handling [Notification runtime permissions](https://developer.android.com/develop/ui/views/notifications/notification-permission).

The simplest way is to let the SDK ask for the permission. `RetenoNotifications.requestNotificationPermission()` shows the system dialog when needed and tells the SDK about the user's answer:

```kotlin
lifecycleScope.launch {
    val granted = RetenoNotifications.requestNotificationPermission()
}
```

From Java, use `RetenoNotifications.requestNotificationPermissionFuture()`, which returns a `CompletableFuture<Boolean>`.

If you request the permission yourself, call `Reteno.instance.updatePushPermissionStatus()` once the user grants it:

```kotlin
val requestPermissionLauncher = registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            Reteno.instance.updatePushPermissionStatus()
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
        } else {                    
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
        }
    }
    
private fun checkPermissions() {
    if (ContextCompat.checkSelfPermission(this, permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
        return
    } else if (shouldShowRequestPermissionRationale(permission.POST_NOTIFICATIONS)) {
        AlertDialog.Builder(this)
            .setTitle("Notifications blocked")
            .setMessage("Please allow receiving notifications from this app in your device settings")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Go to Settings") { dialog, which ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val uri = Uri.fromParts("package", getPackageName(), null)
                    intent.data = uri
                    startActivity(intent)
                }.show()
    } else {
        requestPermissionLauncher.launch(permission.POST_NOTIFICATIONS)
    }
}
```

### Step6: Set up your Firebase application for Firebase Cloud Messaging:

- Download your `google-services.json` config file (see how [here](https://support.google.com/firebase/answer/7015592?hl=en)).

- Add the above file to your root `app/` folder.

  ![](assets/google-services-json.png)

- Copy your FCM Server Key. In the [Firebase console](https://console.firebase.google.com/), click the gear icon next to Overview, then click Project Settings->Cloud Messaging -> Manage Service Accounts. Go to Service accounts to download FirebaseAdminSdk account's json key.
  ![](assets/FirebaseConsole.png)

![](assets/CloudConsole1.png)

![](assets/CloudConsole2.png)

- Follow this manual to [set up Reteno admin panel](https://docs.reteno.com/docs/connect-your-mobile-app) with your Firebase key.


### Please check optional items below.

Now you are ready to run your app and send a marketing push notification to your application.

Run your app on a physical Android device to make sure it builds correctly.


> **Optional.** If you use own FCM pushes along with Reteno

If you use your custom FCM service extended from `FirebaseMessagingService` don't extend it directly. Extend `RetenoFirebaseMessagingService` instead and call super methods for `onCreate`, `onNewToken`, `onMessageReceived`.
E.g.:

```kotlin
class CustomFcmService: RetenoFirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        // Your code here
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Your code here
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Your code here
    }
}
```

> **Optional.** You may add your default icon and color for all Reteno notifications via AndroidManifest.xml

```xml
<meta-data
            android:name="@string/notification_icon"
            android:resource="@drawable/ic_notification" />

<meta-data
            android:name="@string/notification_icon_color"
            android:resource="@color/red_dark" />
```

> **Optional.** Additionally, you can further configure the handling of [Deeplinks](https://docs.reteno.com/reference/android-push-handling#deeplinks), [Custom Data](https://docs.reteno.com/reference/android-push-handling#custom-data), and/or [Notification Events (Push Received, Notification Clicked)](https://docs.reteno.com/reference/android-push-handling#notification-events-push-received-notification-clicked).
>To learn more, please visit the [Android Push Handling](https://docs.reteno.com/reference/android-push-handling) page.
