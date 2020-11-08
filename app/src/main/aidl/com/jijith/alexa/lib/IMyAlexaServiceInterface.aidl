// IMyAlexaServiceInterface.aidl
package com.jijith.alexa.lib;

import com.jijith.alexa.lib.IMyAlexaCallbackInterface;

// Declare any non-default types here with import statements

interface IMyAlexaServiceInterface {

    void registerCallback(IMyAlexaCallbackInterface iMyAlexaCallbackInterface);

    void unregisterCallback(IMyAlexaCallbackInterface iMyAlexaCallbackInterface);

    void startCBL();
}