package com.jijith.alexa.service.managersimpl

import android.content.Context
import android.os.Environment
import android.provider.Settings
import com.amazon.aace.alexa.AlexaProperties
import com.amazon.aace.alexa.config.AlexaConfiguration
import com.amazon.aace.alexa.config.AlexaConfiguration.TemplateRuntimeTimeout
import com.amazon.aace.carControl.CarControlAssets
import com.amazon.aace.carControl.CarControlConfiguration
import com.amazon.aace.core.CoreProperties
import com.amazon.aace.core.Engine
import com.amazon.aace.core.config.ConfigurationFile
import com.amazon.aace.core.config.EngineConfiguration
import com.amazon.aace.storage.config.StorageConfiguration
import com.amazon.aace.vehicle.config.VehicleConfiguration
import com.amazon.aace.vehicle.config.VehicleConfiguration.VehicleProperty
import com.amazon.sampleapp.core.FileUtils
import com.jijith.alexa.service.handlers.*
import com.jijith.alexa.service.handlers.carcontrol.CarControlDataProvider
import com.jijith.alexa.service.handlers.carcontrol.CarControlHandler
import com.jijith.alexa.service.interfaces.managers.AlexaEngineManager
import com.jijith.alexa.service.interfaces.managers.DatabaseManager
import com.jijith.alexa.service.interfaces.managers.NetworkManager
import com.jijith.alexa.utils.*
import org.json.JSONException
import timber.log.Timber
import java.io.File
import java.util.*

class AlexaEngineManagerImpl(private var context: Context) : AlexaEngineManager {

    // Core
    private lateinit var mEngine: Engine


    private lateinit var alexaClientHandler: AlexaClientHandler
    private lateinit var audioInputProviderHandler: AudioInputProviderHandler
    private lateinit var audioOutputProviderHandler: AudioOutputProviderHandler
    private lateinit var locationProviderHandler: LocationProviderHandler
    private lateinit var phoneCallControllerHandler: PhoneCallControllerHandler
    private lateinit var playbackControllerHandler: PlaybackControllerHandler
    private lateinit var speechRecognizer: SpeechRecognizerHandler
    private lateinit var audioPlayerHandler: AudioPlayerHandler
    private lateinit var speechSynthesizerHandler: SpeechSynthesizerHandler
    private lateinit var templateRuntimeHandler: TemplateRuntimeHandler
    private lateinit var alexaSpeakerHandler: AlexaSpeakerHandler
    private lateinit var alertsHandler: AlertsHandler
    private lateinit var networkInfoProvider: NetworkInfoProviderHandler
    private lateinit var cblHandler: CBLHandler
    private lateinit var notificationsHandler: NotificationsHandler

    private val databaseManger: DatabaseManager
    private var networkManager: NetworkManager

    private var mEngineStarted = false

    private var clientId = ""
    private var productId = ""
    private var productDsn = ""

    init {

        databaseManger = DatabaseMangerImpl(context)

        startEngine(null)

        networkManager = NetworkMangerImpl(context, networkInfoProvider)


    }

    /**
     * Configure the Engine and register platform interface instances
     * @param json JSON string with LVC config if LVC is supported, null otherwise.
     * @throws RuntimeException
     */
    @Throws(RuntimeException::class)
    private fun startEngine(json: String?) {

        // Create an "appdata" subdirectory in the cache directory for storing application data
        val cacheDir: File = context?.getCacheDir()
        val appDataDir = File(cacheDir, "appdata")
        val sampleDataDir = File(cacheDir, "sampledata")

        // Copy certs from assets to certs subdirectory of cache directory
        val certsDir = File(appDataDir, "certs")
        FileUtils.copyAllAssets(context?.getAssets(), "certs", certsDir, false)

        // Copy models from assets to certs subdirectory of cache directory.
        // Force copy the models on every start so that the models on device cache are always the latest
        // from the APK
        val modelsDir = File(appDataDir, "models")
        FileUtils.copyAllAssets(context?.getAssets(), "models", modelsDir, true)
        copyAsset("Contacts.json", File(sampleDataDir, "Contacts.json"), false)
        copyAsset(
            "NavigationFavorites.json",
            File(sampleDataDir, "NavigationFavorites.json"),
            false
        )

        // Create AAC engine
        mEngine = Engine.create(context)
        val configuration =
            getEngineConfigurations(json, appDataDir, certsDir, modelsDir)

        // Get extra module factories and add configurations
        /*val data: MutableMap<String, String> =
            HashMap()
        data[SampleAppContext.CERTS_DIR] = certsDir.path
        data[SampleAppContext.MODEL_DIR] = modelsDir.path
        data[SampleAppContext.PRODUCT_DSN] = productDsn
        data[SampleAppContext.APPDATA_DIR] = appDataDir.path
        data[SampleAppContext.JSON] = json!!*/
        /*val sampleAppContext = SampleAppContext(context, null, data)
        val extraFactories: List<ModuleFactoryInterface> =
            getExtraModuleFactory()
        configExtraModules(sampleAppContext, extraFactories, configuration)*/
        val configurationArray = configuration!!.toTypedArray()
        val configureSucceeded = mEngine.configure(configurationArray)
        if (!configureSucceeded) throw RuntimeException("Engine configuration failed")

        // Create the platform implementation handlers and register them with the engine
        // Logger
        if (!mEngine?.registerPlatformInterface(LoggerHandler()))
            throw RuntimeException("Could not register Logger platform interface")

        // AlexaClient
        alexaClientHandler = AlexaClientHandler(context)
        if (!mEngine.registerPlatformInterface(alexaClientHandler))
            throw RuntimeException("Could not register AlexaClient platform interface")

        // AudioInputProvider
        audioInputProviderHandler = AudioInputProviderHandler(context)
        if (!mEngine.registerPlatformInterface(audioInputProviderHandler))
            throw RuntimeException("Could not register AudioInputProvider platform interface")

        // AudioOutputProvider
        audioOutputProviderHandler = AudioOutputProviderHandler(context, alexaClientHandler)
        if (!mEngine.registerPlatformInterface(audioOutputProviderHandler))
            throw RuntimeException("Could not register AudioOutputProvider platform interface")

        // LocationProvider
        locationProviderHandler = LocationProviderHandler(context)
        if (!mEngine.registerPlatformInterface(locationProviderHandler))
            throw RuntimeException("Could not register LocationProvider platform interface")

        // PhoneCallController
        phoneCallControllerHandler = PhoneCallControllerHandler(context)
        if (!mEngine.registerPlatformInterface(phoneCallControllerHandler))
            throw RuntimeException("Could not register PhoneCallController platform interface")

        // PlaybackController
        playbackControllerHandler = PlaybackControllerHandler(context)
        if (!mEngine.registerPlatformInterface(playbackControllerHandler))
            throw RuntimeException("Could not register PlaybackController platform interface")

        // SpeechRecognizer
        speechRecognizer = SpeechRecognizerHandler(context, true)
        if (!mEngine.registerPlatformInterface(speechRecognizer))
            throw RuntimeException("Could not register SpeechRecognizer platform interface")

        // AudioPlayer
        audioPlayerHandler = AudioPlayerHandler(audioOutputProviderHandler, playbackControllerHandler)
        if (!mEngine.registerPlatformInterface(audioPlayerHandler))
            throw RuntimeException("Could not register AudioPlayer platform interface")

        // SpeechSynthesizer
        speechSynthesizerHandler = SpeechSynthesizerHandler()
        if (!mEngine.registerPlatformInterface(speechSynthesizerHandler))
            throw RuntimeException("Could not register SpeechSynthesizer platform interface")

        // TemplateRuntime
        templateRuntimeHandler = TemplateRuntimeHandler(playbackControllerHandler)
        if (!mEngine.registerPlatformInterface(templateRuntimeHandler))
            throw RuntimeException("Could not register TemplateRuntime platform interface")

        // AlexaSpeaker
        alexaSpeakerHandler = AlexaSpeakerHandler(context)
        if (!mEngine.registerPlatformInterface(alexaSpeakerHandler))
            throw java.lang.RuntimeException("Could not register AlexaSpeaker platform interface")

        // Alerts
        alertsHandler = AlertsHandler(context)
        if (!mEngine.registerPlatformInterface(alertsHandler))
            throw java.lang.RuntimeException("Could not register Alerts platform interface")

        // NetworkInfoProvider
        networkInfoProvider = NetworkInfoProviderHandler(context)
        if (!mEngine.registerPlatformInterface(networkInfoProvider))
            throw RuntimeException("Could not register NetworkInfoProvider platform interface")

        // CBL
        cblHandler = CBLHandler(context, databaseManger)
        if (!mEngine.registerPlatformInterface(cblHandler))
            throw RuntimeException("Could not register CBL platform interface")
        //mAlexaClient.registerAuthStateObserver(mCBLHandler)

        // Notifications
        notificationsHandler = NotificationsHandler()
        if (!mEngine.registerPlatformInterface(notificationsHandler))
            throw RuntimeException("Could not register Notifications platform interface")

        // LVC Handlers
        if (!mEngine.registerPlatformInterface(CarControlHandler(context )))
            throw RuntimeException("Could not register Car Control platform interface")

        // Set Output Audio provider now that it is registered
        /*ifsampleAppContext.audioOutputProvider = mAudioOutputProvider

        */


        /*// Navigation
        if (!mEngine.registerPlatformInterface(
                NavigationHandler(this, mLogger).also({
                    mNavigation = it
                })
            )
        ) throw RuntimeException("Could not register Navigation platform interface")*/

        /*// DoNotDisturb
        if (!mEngine.registerPlatformInterface(
                DoNotDisturbHandler(this, mLogger).also({
                    mDoNotDisturb = it
                })
            )
        ) throw RuntimeException("Could not register DoNotDisturb platform interface") else mAlexaClient.registerAuthStateObserver(
            mDoNotDisturb
        )*/

        /*if (!mEngine.registerPlatformInterface(
                AddressBookHandler(
                    this,
                    mLogger,
                    sampleContactsDataPath,
                    sampleNavigationFavoritesDataPath
                ).also({
                    mAddressBook = it
                })
            )
        ) throw RuntimeException("Could not register AddressBook platform interface")

        // EqualizerController
        if (!mEngine.registerPlatformInterface(
                EqualizerControllerHandler(this, mLogger).also({
                    mEqualizerControllerHandler = it
                })
            )
        ) throw RuntimeException("Could not register EqualizerController platform interface")

        // AlexaComms Handler


        mMACCPlayer = MACCPlayer(this, mLogger, mPlaybackController)
        if (!mEngine.registerPlatformInterface(mMACCPlayer)) {
            Log.i("MACC", "registration failed")
            throw RuntimeException("Could not register external media player platform interface")
        } else {
            Log.i("MACC", "registration succeeded")
        }
        mMACCPlayer.runDiscovery()

        // Mock CD platform handler
        if (!mEngine.registerPlatformInterface(
                CDLocalMediaSource(this, mLogger).also({
                    mCDLocalMediaSource = it
                })
            )
        ) throw RuntimeException("Could not register Mock CD player Local Media Source platform interface")

        // Mock DAB platform handler
        if (!mEngine.registerPlatformInterface(
                DABLocalMediaSource(this, mLogger).also({
                    mDABLocalMediaSource = it
                })
            )
        ) throw RuntimeException("Could not register Mock DAB player Local Media Source platform interface")

        // Mock AM platform handler
        if (!mEngine.registerPlatformInterface(
                AMLocalMediaSource(this, mLogger).also({
                    mAMLocalMediaSource = it
                })
            )
        ) throw RuntimeException("Could not register Mock AM radio player Local Media Source platform interface")*/

        // Mock SIRIUSXM platform handler
        /*if ( !mEngine.registerPlatformInterface(
                mSIRUSXMLocalMediaSource = new SiriusXMLocalMediaSource(this, mLogger)
        ) ) throw new RuntimeException( "Could not register Mock SIRIUSXM player Local Media Source platform interface" );*/

        // Mock FM platform handler
        /* if (!mEngine.registerPlatformInterface(
                 FMLocalMediaSource(this, mLogger).also({
                     mFMLocalMediaSource = it
                 })
             )
         ) throw RuntimeException("Could not register Mock FM radio player Local Media Source platform interface")*/

        // Mock Bluetooth platform handler
        /* if (!mEngine.registerPlatformInterface(
                 BluetoothLocalMediaSource(this, mLogger).also({
                     mBTLocalMediaSource = it
                 })
             )
         ) throw RuntimeException("Could not register Mock Bluetooth player Local Media Source platform interface")

         // Mock Line In platform handler
         if (!mEngine.registerPlatformInterface(
                 LineInLocalMediaSource(this, mLogger).also({
                     mLILocalMediaSource = it
                 })
             )
         ) throw RuntimeException("Could not register Mock Line In player Local Media Source platform interface")

         // Mock Satellite Radio platform handler
         if (!mEngine.registerPlatformInterface(
                 SatelliteLocalMediaSource(this, mLogger).also({
                     mSATRADLocalMediaSource = it
                 })
             )
         ) throw RuntimeException("Could not register Mock Satellite radio player Local Media Source platform interface")

         // Mock USB platform handler
         if (!mEngine.registerPlatformInterface(
                 USBLocalMediaSource(this, mLogger).also({
                     mUSBLocalMediaSource = it
                 })
             )
         ) throw RuntimeException("Could not register Mock USB player Local Media Source platform interface")

         // Mock global preset
         if (!mEngine.registerPlatformInterface(
                 GlobalPresetHandler(this, mLogger).also({
                     mGlobalPresetHandler = it
                 })
             )
         ) throw RuntimeException("Could not register Mock Global Preset platform interface")

         // Mock apl
         if (!mEngine.registerPlatformInterface(
                 APLHandler(this, mLogger).also({
                     mAplHandler = it
                 })
             )
         ) throw RuntimeException("Could not register Mock Global Preset platform interface")

         // Register extra modules
         loadPlatformInterfacesAndLoadUI(mEngine, extraFactories, sampleAppContext)*/

        // Alexa Locale
        val supportedLocales = mEngine.getProperty(AlexaProperties.SUPPORTED_LOCALES)
        Timber.d("supported locale %s", supportedLocales)
        /*val localesArray = supportedLocales.split(",").toTypedArray()
        val localeAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item, localesArray
        )
        val spinnerView = findViewById(R.id.locale_spinner) as Spinner
        spinnerView.adapter = localeAdapter*/
        val defaultLocale = mEngine.getProperty(AlexaProperties.LOCALE)
        Timber.d("default locale %s", defaultLocale)
        /*var localePosition = localeAdapter.getPosition(defaultLocale)
        if (localePosition < 0) {
            Timber.e("$defaultLocale is not in the Supported Locales")
            localePosition = 0
        }
        spinnerView.setSelection(localePosition)
        spinnerView.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                arg0: AdapterView<*>?, arg1: View,
                position: Int, arg3: Long
            ) {
                val s = localesArray[position]
                if (mEngine.getProperty(AlexaProperties.LOCALE) != s) {
                    Toast.makeText(
                        context, "Switching Alexa locale to $s",
                        Toast.LENGTH_SHORT
                    ).show()
                    mEngine.setProperty(AlexaProperties.LOCALE, s)
                }
            }

            override fun onNothingSelected(arg0: AdapterView<*>?) {
                // TODO Auto-generated method stub
            }
        }*/

        // Start the engine
        if (!mEngine.start()) throw RuntimeException("Could not start engine")
        mEngineStarted = true

        // Check if Amazonlite is supported
        if (mEngine.getProperty(AlexaProperties.WAKEWORD_SUPPORTED) == "true") {
            speechRecognizer.enableWakeWord()
        }

        Timber.d("Wakeword supported: %s", mEngine.getProperty(AlexaProperties.WAKEWORD_SUPPORTED))

        //log whether LocationProvider gave a supported country
        Timber.d("Country Supported: %s", mEngine.getProperty(AlexaProperties.COUNTRY_SUPPORTED))

        // Initialize AutoVoiceChrome
/*        mAddressBook.onInitialize()
        initTapToTalk()
        initEarconsSettings()*/
    }


    /**
     * Get the configurations to start the Engine
     * @param json JSON string with LVC config if LVC is supported, null otherwise.
     * @param appDataDir path to app's data directory
     * @param certsDir path to certificates directory
     * @return List of Engine configurations
     */
    private fun getEngineConfigurations(
        json: String?,
        appDataDir: File,
        certsDir: File,
        modelsDir: File
    ): ArrayList<EngineConfiguration>? {
        // Retrieve device config from config file and update preferences

        val config = FileUtils.getConfigFromFile(context.assets, APP_CONFIG, CONFIG)
        if (config != null) {
            try {
                clientId = config.getString(CLIENT_ID)
                productId = config.getString(PRODUCT_ID)
            } catch (e: JSONException) {
                Timber.w("Missing device info in app_config.json")
            }
            try {
                productDsn = config.getString("productDsn")
            } catch (e: JSONException) {
                try {
                    // set Android ID as product DSN
                    productDsn = Settings.Secure.getString(
                        context?.getContentResolver(),
                        Settings.Secure.ANDROID_ID
                    )
                    Timber.i("android id for DSN: $productDsn")
                } catch (error: Error) {
                    productDsn = UUID.randomUUID().toString()
                    Timber.w("android id not found, generating random DSN: $productDsn")
                }
            }
        }

        Timber.d("clientId: %s \n productId: %s \n productDsn:%s", clientId, productId, productDsn)

        val carControlConfiguration = CarControlConfiguration.create()
        carControlConfiguration.createControl("car", CarControlConfiguration.Zone.ALL)
            .addAssetId(CarControlAssets.Device.CAR)
            .addToggleController("recirculate", TRUE)
            .addAssetId(CarControlAssets.Setting.AIR_RECIRCULATION)
            .addToggleController("climate.sync", TRUE)
            .addAssetId(CarControlAssets.Setting.CLIMATE_SYNC)

        // Configure the engine

        val timeoutList = arrayOf(
            TemplateRuntimeTimeout(
                AlexaConfiguration.TemplateRuntimeTimeoutType.DISPLAY_CARD_TTS_FINISHED_TIMEOUT,
                8000
            ),
            TemplateRuntimeTimeout(
                AlexaConfiguration.TemplateRuntimeTimeoutType.DISPLAY_CARD_AUDIO_PLAYBACK_FINISHED_TIMEOUT,
                8000
            ),
            TemplateRuntimeTimeout(
                AlexaConfiguration.TemplateRuntimeTimeoutType.DISPLAY_CARD_AUDIO_PLAYBACK_STOPPED_PAUSED_TIMEOUT,
                1800000
            )
        )

        val configuration =
            ArrayList(
                Arrays.asList( //AlexaConfiguration.createCurlConfig( certsDir.getPath(), "wlan0" ), Uncomment this line to specify the interface name to use by AVS.
                    AlexaConfiguration.createCurlConfig(certsDir.path),
                    AlexaConfiguration.createDeviceInfoConfig(
                        productDsn,
                        clientId,
                        productId,
                        "Alexa Auto SDK",
                        "Android Sample App"
                    ),
                    AlexaConfiguration.createMiscStorageConfig(appDataDir.path + "/miscStorage.sqlite"),
                    AlexaConfiguration.createCertifiedSenderConfig(appDataDir.path + "/certifiedSender.sqlite"),
                    AlexaConfiguration.createCapabilitiesDelegateConfig(appDataDir.path + "/capabilitiesDelegate.sqlite"),
                    AlexaConfiguration.createAlertsConfig(appDataDir.path + "/alerts.sqlite"),
                    AlexaConfiguration.createNotificationsConfig(appDataDir.path + "/notifications.sqlite"),
                    AlexaConfiguration.createDeviceSettingsConfig(appDataDir.path + "/deviceSettings.sqlite"),
                    /*AlexaConfiguration.createEqualizerControllerConfig(
                        EqualizerConfiguration.getSupportedBands(),
                        EqualizerConfiguration.getMinBandLevel(),
                        EqualizerConfiguration.getMaxBandLevel(),
                        EqualizerConfiguration.getDefaultBandLevels()
                    ), */ // Uncomment the below line to specify the template runtime values
                    AlexaConfiguration.createTemplateRuntimeTimeoutConfig( timeoutList ),
                    StorageConfiguration.createLocalStorageConfig(appDataDir.path + "/localStorage.sqlite"),  // Example Vehicle Config
                    VehicleConfiguration.createVehicleInfoConfig(
                        arrayOf(
                            VehicleProperty(
                                VehicleConfiguration.VehiclePropertyType.MAKE,
                                "Amazon"
                            ),
                            VehicleProperty(
                                VehicleConfiguration.VehiclePropertyType.MODEL,
                                "AmazonCarOne"
                            ),
                            VehicleProperty(
                                VehicleConfiguration.VehiclePropertyType.TRIM,
                                "Advance"
                            ),
                            VehicleProperty(VehicleConfiguration.VehiclePropertyType.YEAR, "2025"),
                            VehicleProperty(
                                VehicleConfiguration.VehiclePropertyType.GEOGRAPHY,
                                "US"
                            ),
                            VehicleProperty(
                                VehicleConfiguration.VehiclePropertyType.VERSION,
                                String.format(
                                    "Vehicle Software Version 1.0 (Auto SDK Version %s)",
                                    mEngine.getProperty(CoreProperties.VERSION)
                                )
                            ),
                            VehicleProperty(
                                VehicleConfiguration.VehiclePropertyType.OPERATING_SYSTEM,
                                "Android 8.1 Oreo API Level 23"
                            ),
                            VehicleProperty(
                                VehicleConfiguration.VehiclePropertyType.HARDWARE_ARCH,
                                "Armv8a"
                            ),
                            VehicleProperty(
                                VehicleConfiguration.VehiclePropertyType.LANGUAGE,
                                "en-US"
                            ),
                            VehicleProperty(
                                VehicleConfiguration.VehiclePropertyType.MICROPHONE,
                                "Single, roof mounted"
                            ),  // If this list is left blank, it will be fetched by the engine using amazon default endpoint
                            VehicleProperty(
                                VehicleConfiguration.VehiclePropertyType.COUNTRY_LIST,
                                "US,GB,IE,CA,DE,AT,IN,JP,AU,NZ,FR"
                            ),
                            VehicleProperty(
                                VehicleConfiguration.VehiclePropertyType.VEHICLE_IDENTIFIER,
                                "123456789a"
                            )
                        )
                    ),
                    carControlConfiguration
                )
            )
        
        return configuration
    }

    private fun copyAsset(
        assetPath: String,
        destFile: File,
        force: Boolean
    ) {
        FileUtils.copyAsset(context.getAssets(), assetPath, destFile, force)
    }

    override fun startCBL() {
        if (cblHandler != null) {
            cblHandler.startCBL()
        } else {
            Timber.d("error")
        }
    }
}