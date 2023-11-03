# Android

[GitHub repo](https://github.com/reteno-com/reteno-mobile-android-sdk)
[Documentation](https://docs.reteno.com/reference/android-sdk-setup)

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

`Reteno Android SDK` is released under the MIT license. See [LICENSE](https://docs.reteno.com/reference/android-sdk-setup) for details.

## 

## Setting up SDK

Follow our setup guide to integrate the Reteno SDK with your app.

### ### Step1: Enable androidx in your gradle.properties file

```groovy
android.useAndroidX=true
android.enableJetifier=true
```

### Step2: Add `com.reteno:fcm` and firebase dependencies in build.gradle

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

### Step3: Edit your custom Application class and provider API Access-Key at SDK initialization.

Below is sample code you can add to your application class which gets you started with `RetenoSDK`. You may need to create a new class that extends the `Application` on this step. Don't forget to edit your manifest file to use the custom Application class. Also make sure to provide the **access-key** in the constructor. You may store Reteno access key the way you wish based on your preferences:

```java
package [com.YOUR_PACKAGE];

import android.app.Application;

import androidx.annotation.NonNull;

import com.reteno.core.Reteno;
import com.reteno.core.RetenoApplication;
import com.reteno.core.RetenoImpl;

public class CustomApplication extends Application implements RetenoApplication {

    private Reteno retenoInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        retenoInstance = new RetenoImpl(this, "your_access_key_here");
    }

    @NonNull
    @Override
    public Reteno getRetenoInstance() {
        return retenoInstance;
    }
}
```

```kotlin
package [com.YOUR_PACKAGE];

import android.app.Application
import com.reteno.core.Reteno
import com.reteno.core.RetenoApplication
import com.reteno.core.RetenoImpl

class CustomApplication: Application(), RetenoApplication {

    private lateinit var retenoInstance: Reteno

    override fun onCreate() {
        super.onCreate()
        retenoInstance = RetenoImpl(this, "your_access_key_here")
    }

    override fun getRetenoInstance(): Reteno {
        return retenoInstance
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

### Step4: Use SDK via `Reteno` interface.

Not `RetenoImpl` implementation. You may now access Reteno SDK across your application via your app instance. E.g. in Activity:

```java
Reteno reteno = ((CustomApplication)getApplication()).getRetenoInstance();
```

```kotlin
val reteno = (application as CustomApplication).getRetenoInstance()
```

> **Optional.** You may add your default icon for all Reteno notifications via AndroidManifest.xml

```xml
<meta-data
            android:name="@string/notification_icon"
            android:resource="@drawable/ic_notification" />
```

> **Note:** If your app is running on Android 13 or later make sure to handle [Notification runtime permissions](https://developer.android.com/develop/ui/views/notifications/notification-permission)

### Step5: Set up your Firebase application for Firebase Cloud Messaging:

- Download your `google-services.json` config file (see how [here](https://support.google.com/firebase/answer/7015592?hl=en)).

- Add the above file to your root `app/` folder.

    ![](assets/google-services-json.png)

- Copy your FCM Server Key. In the [Firebase console](https://console.firebase.google.com/), click the gear icon next to Overview, then click Project Settings->Cloud Messaging -> Manage Service Accounts. Go to Service accounts to download FirebaseAdminSdk account's json key.
  ![](assets/FirebaseConsole.png)

![](assets/CloudConsole1.png)

![](assets/CloudConsole2.png)

- Follow this manual to [set up Reteno admin panel](https://docs.reteno.com/docs/connect-your-mobile-app) with your Firebase key.

### 6. If you use custom FCM Service extended from `FirebaseMessagingService`

If you use your custom FCM service extended from `FirebaseMessagingService` don't extend it directly. Extend `RetenoFirebaseMessagingService` instead and call super methods for `onCreate`, `onNewToken`, `onMessageReceived`. 
E.g.:

```java
public class CustomFcmService extends RetenoFirebaseMessagingService {

    @Override
    public void onCreate() {
        super.onCreate();
        // Your code here
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Your code here
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        // Your code here
    }
}
```

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

Now you are ready to run your app and send a marketing push notification to your application.

Run your app on a physical Android device to make sure it builds correctlas
